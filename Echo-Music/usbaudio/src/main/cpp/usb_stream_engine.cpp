#include "usb_stream_engine.h"
#include <sys/ioctl.h>
#include <unistd.h>
#include <pthread.h>
#include <android/log.h>
#include <cstring>
#include <algorithm>
#include <cmath>

#define LOG_TAG "UsbStreamEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace echo::music::usbaudio {

UsbStreamEngine::UsbStreamEngine() = default;

UsbStreamEngine::~UsbStreamEngine() {
    stopStream();
}

uint32_t UsbStreamEngine::calculateNominalPacketSamples(uint32_t sampleRate) {
    return sampleRate / MICROFRAMES_PER_SEC;
}

int UsbStreamEngine::startStream(
    int fd,
    int interfaceNumber,
    int altSetting,
    int dataEp,
    int syncEp,
    uint32_t sampleRate,
    uint32_t bitDepth,
    uint32_t channels
) {
    if (streaming_.load(std::memory_order_relaxed)) {
        stopStream();
    }

    fd_ = fd;
    interface_number_ = interfaceNumber;
    alt_setting_ = altSetting;
    data_ep_ = dataEp;
    sync_ep_ = syncEp;
    sample_rate_ = sampleRate;
    bit_depth_ = bitDepth;
    channels_ = channels;

    if (fd_ >= 0 && interfaceNumber >= 0) {
        // Detach kernel audio driver if active
        struct usbdevfs_ioctl ctl{};
        ctl.ifno = interfaceNumber;
        ctl.ioctl_code = USBDEVFS_DISCONNECT;
        ctl.data = nullptr;
        ioctl(fd_, USBDEVFS_IOCTL, &ctl);

        // Claim AudioStreaming interface
        int ifno = interfaceNumber;
        if (ioctl(fd_, USBDEVFS_CLAIMINTERFACE, &ifno) < 0 && errno != EBUSY) {
            LOGE("Failed to claim interface %d: %s", ifno, strerror(errno));
            return -errno;
        }

        // Select alternate setting with isochronous audio endpoint
        struct usbdevfs_setinterface setif{};
        setif.interface = static_cast<unsigned int>(interfaceNumber);
        setif.altsetting = static_cast<unsigned int>(altSetting);
        if (ioctl(fd_, USBDEVFS_SETINTERFACE, &setif) < 0) {
            LOGE("Failed to set alternate setting %d on interface %d: %s", altSetting, interfaceNumber, strerror(errno));
            return -errno;
        }

        // Configure sample rate via standard UAC control requests
        struct usbdevfs_ctrltransfer ctrl{};
        uint8_t rate_bytes3[3] = {
            static_cast<uint8_t>(sampleRate & 0xFF),
            static_cast<uint8_t>((sampleRate >> 8) & 0xFF),
            static_cast<uint8_t>((sampleRate >> 16) & 0xFF)
        };
        ctrl.bRequestType = 0x22; // Class, Endpoint, Host-to-Device
        ctrl.bRequest = 0x01;     // SET_CUR
        ctrl.wValue = 0x0100;     // SAMPLING_FREQ_CONTROL
        ctrl.wIndex = static_cast<uint16_t>(dataEp);
        ctrl.wLength = 3;
        ctrl.timeout = 1000;
        ctrl.data = rate_bytes3;
        if (ioctl(fd_, USBDEVFS_CONTROL, &ctrl) < 0) {
            // Fallback for UAC2 clock source SET_CUR
            uint8_t rate_bytes4[4] = {
                static_cast<uint8_t>(sampleRate & 0xFF),
                static_cast<uint8_t>((sampleRate >> 8) & 0xFF),
                static_cast<uint8_t>((sampleRate >> 16) & 0xFF),
                static_cast<uint8_t>((sampleRate >> 24) & 0xFF)
            };
            ctrl.bRequestType = 0x21; // Class, Interface, Host-to-Device
            ctrl.bRequest = 0x01;     // SET_CUR
            ctrl.wValue = 0x0100;     // CS_SAM_FREQ_CONTROL
            ctrl.wIndex = static_cast<uint16_t>(interfaceNumber);
            ctrl.wLength = 4;
            ctrl.timeout = 1000;
            ctrl.data = rate_bytes4;
            ioctl(fd_, USBDEVFS_CONTROL, &ctrl);
        }
    }

    bytes_per_sample_ = (bitDepth + 7) / 8;
    bytes_per_frame_ = bytes_per_sample_ * channels_;

    // Query bus speed: Full-Speed (1 ms frames = 1000 intervals/sec) vs High-Speed (125 µs = 8000 microframes/sec)
    uint32_t intervals_per_sec = MICROFRAMES_PER_SEC;
    if (fd_ >= 0) {
        int speed = ioctl(fd_, USBDEVFS_GET_SPEED);
        if (speed == 2 /* USB_SPEED_FULL */ || speed == 1 /* USB_SPEED_LOW */) {
            intervals_per_sec = 1000;
        }
    }
    intervals_per_sec_ = intervals_per_sec;

    // Pacing fraction in 16.16 fixed point: (sample_rate << 16) / intervals_per_sec
    fractional_step_ = static_cast<uint32_t>((static_cast<uint64_t>(sampleRate) << 16) / intervals_per_sec_);
    fractional_accum_ = 0;
    feedback_rate_q16_ = fractional_step_;

    ring_buffer_.flush();
    frames_played_.store(0, std::memory_order_release);
    flush_requested_.store(false, std::memory_order_release);

    streaming_.store(true, std::memory_order_release);
    stream_thread_ = std::thread(&UsbStreamEngine::streamLoop, this);

    LOGI("Started stream: SR=%u, bitDepth=%u, channels=%u, intervalsPerSec=%u, dataEp=0x%02x, syncEp=0x%02x",
         sampleRate, bitDepth, channels, intervals_per_sec_, dataEp, syncEp);
    return 0;
}

int UsbStreamEngine::stopStream() {
    streaming_.store(false, std::memory_order_release);

    if (stream_thread_.joinable()) {
        stream_thread_.join();
    }

    // Discard any active submitted URBs on this file descriptor individually
    if (fd_ >= 0) {
        for (auto& ctx : data_urbs_) {
            if (ctx.submitted) {
                ioctl(fd_, USBDEVFS_DISCARDURB, ctx.urb());
            }
        }
        for (auto& ctx : sync_urbs_) {
            if (ctx.submitted) {
                ioctl(fd_, USBDEVFS_DISCARDURB, ctx.urb());
            }
        }

        // Reap all discarded URBs until drained so kernel holds no stale references
        size_t pending = 0;
        for (const auto& ctx : data_urbs_) if (ctx.submitted) ++pending;
        for (const auto& ctx : sync_urbs_) if (ctx.submitted) ++pending;

        while (pending > 0) {
            usbdevfs_urb* reaped_urb = nullptr;
            if (ioctl(fd_, USBDEVFS_REAPURB, &reaped_urb) != 0) {
                if (errno == EINTR) continue;
                break; // ENODEV: device gone or disconnected
            }
            if (reaped_urb && reaped_urb->usercontext) {
                static_cast<UrbContext*>(reaped_urb->usercontext)->submitted = false;
            }
            --pending;
        }

        // Restore USB interface: select altsetting 0, release interface, reconnect kernel driver
        if (interface_number_ >= 0) {
            struct usbdevfs_setinterface setif{};
            setif.interface = static_cast<unsigned int>(interface_number_);
            setif.altsetting = 0;
            ioctl(fd_, USBDEVFS_SETINTERFACE, &setif);

            int ifno = interface_number_;
            ioctl(fd_, USBDEVFS_RELEASEINTERFACE, &ifno);

            struct usbdevfs_ioctl ctl{};
            ctl.ifno = interface_number_;
            ctl.ioctl_code = USBDEVFS_CONNECT;
            ctl.data = nullptr;
            ioctl(fd_, USBDEVFS_IOCTL, &ctl);
        }
    }

    data_urbs_.clear();
    sync_urbs_.clear();

    LOGI("Stopped stream");
    return 0;
}

size_t UsbStreamEngine::writeAudio(const uint8_t* buffer, size_t size) {
    return ring_buffer_.write(buffer, size);
}

void UsbStreamEngine::streamLoop() {
    // Elevate thread priority to SCHED_FIFO for real-time isochronous pacing
    struct sched_param param{};
    param.sched_priority = 2; // Real-time priority
    pthread_setschedparam(pthread_self(), SCHED_FIFO, &param);

    const uint32_t max_samples_per_pkt = (sample_rate_ + intervals_per_sec_ - 1) / intervals_per_sec_ + 4;
    const uint32_t max_packet_size = max_samples_per_pkt * bytes_per_frame_;
    const uint32_t urb_buffer_size = max_packet_size * PACKETS_PER_URB;
    const size_t urb_storage_size = sizeof(usbdevfs_urb) + sizeof(usbdevfs_iso_packet_desc) * PACKETS_PER_URB;

    data_urbs_.resize(NUM_URBS);
    bool any_submitted = false;

    for (auto& ctx : data_urbs_) {
        ctx.urb_storage.assign(urb_storage_size, 0);
        ctx.buffer.assign(urb_buffer_size, 0);
        ctx.submitted = false;

        usbdevfs_urb* u = ctx.urb();
        u->type = USBDEVFS_URB_TYPE_ISO;
        u->endpoint = static_cast<unsigned char>(data_ep_);
        u->buffer = ctx.buffer.data();
        u->buffer_length = static_cast<int>(urb_buffer_size);
        u->number_of_packets = static_cast<int>(PACKETS_PER_URB);
        u->usercontext = &ctx;

        for (size_t p = 0; p < PACKETS_PER_URB; ++p) {
            u->iso_frame_desc[p].length = max_packet_size;
            u->iso_frame_desc[p].actual_length = 0;
            u->iso_frame_desc[p].status = 0;
        }

        if (fd_ >= 0) {
            if (ioctl(fd_, USBDEVFS_SUBMITURB, u) == 0) {
                ctx.submitted = true;
                any_submitted = true;
            }
        }
    }

    // Submit sync feedback URBs if explicit feedback IN endpoint exists
    if (sync_ep_ > 0) {
        const size_t sync_storage_size = sizeof(usbdevfs_urb) + sizeof(usbdevfs_iso_packet_desc);
        sync_urbs_.resize(2);
        for (auto& sctx : sync_urbs_) {
            sctx.urb_storage.assign(sync_storage_size, 0);
            sctx.buffer.assign(4, 0);
            sctx.submitted = false;

            usbdevfs_urb* su = sctx.urb();
            su->type = USBDEVFS_URB_TYPE_ISO;
            su->endpoint = static_cast<unsigned char>(sync_ep_ | 0x80);
            su->buffer = sctx.buffer.data();
            su->buffer_length = 4;
            su->number_of_packets = 1;
            su->usercontext = &sctx;
            su->iso_frame_desc[0].length = 4;
            su->iso_frame_desc[0].actual_length = 0;
            su->iso_frame_desc[0].status = 0;

            if (fd_ >= 0) {
                if (ioctl(fd_, USBDEVFS_SUBMITURB, su) == 0) {
                    sctx.submitted = true;
                }
            }
        }
    }

    if (fd_ >= 0 && !any_submitted) {
        LOGE("Failed to submit initial URBs on endpoint 0x%02x: %s", data_ep_, strerror(errno));
        streaming_.store(false, std::memory_order_release);
        return;
    }

    while (streaming_.load(std::memory_order_relaxed)) {
        if (flush_requested_.exchange(false, std::memory_order_acq_rel)) {
            ring_buffer_.flush();
            fractional_accum_ = 0;
            frames_played_.store(0, std::memory_order_release);
        }

        if (fd_ < 0) {
            usleep(1000);
            continue;
        }

        usbdevfs_urb* reaped_urb = nullptr;
        int ret = ioctl(fd_, USBDEVFS_REAPURBNDELAY, &reaped_urb);

        if (ret == 0 && reaped_urb != nullptr) {
            auto* ctx = static_cast<UrbContext*>(reaped_urb->usercontext);
            ctx->submitted = false;

            // Handle sync endpoint feedback packets
            if (sync_ep_ > 0 && reaped_urb->endpoint == static_cast<unsigned char>(sync_ep_ | 0x80)) {
                const uint8_t* sptr = ctx->buffer.data();
                uint32_t raw_fb = 0;
                if (intervals_per_sec_ == 1000) {
                    // Full-Speed: 10.14 format (3 bytes)
                    uint32_t fb1014 = static_cast<uint32_t>(sptr[0]) |
                                      (static_cast<uint32_t>(sptr[1]) << 8) |
                                      (static_cast<uint32_t>(sptr[2]) << 16);
                    if (fb1014 > 0) {
                        raw_fb = (fb1014 << 2); // Convert to 16.16 format
                    }
                } else {
                    // High-Speed: 16.16 format (4 bytes)
                    raw_fb = static_cast<uint32_t>(sptr[0]) |
                             (static_cast<uint32_t>(sptr[1]) << 8) |
                             (static_cast<uint32_t>(sptr[2]) << 16) |
                             (static_cast<uint32_t>(sptr[3]) << 24);
                }

                // Validate raw feedback against nominal rate (allow up to ±20% deviation)
                if (raw_fb > 0 && fractional_step_ > 0) {
                    uint32_t min_fb = fractional_step_ * 8 / 10;
                    uint32_t max_fb = fractional_step_ * 12 / 10;
                    if (raw_fb >= min_fb && raw_fb <= max_fb) {
                        feedback_rate_q16_ = raw_fb;
                    }
                }

                reaped_urb->iso_frame_desc[0].actual_length = 0;
                reaped_urb->iso_frame_desc[0].status = 0;
                if (ioctl(fd_, USBDEVFS_SUBMITURB, reaped_urb) == 0) {
                    ctx->submitted = true;
                }
                continue;
            }

            usbdevfs_urb* u = ctx->urb();

            // Fill next URB packets from ring buffer
            uint8_t* ptr = ctx->buffer.data();
            int total_length = 0;

            for (size_t p = 0; p < PACKETS_PER_URB; ++p) {
                // Determine sample count for this interval
                fractional_accum_ += feedback_rate_q16_;
                uint32_t samples_to_send = fractional_accum_ >> 16;
                fractional_accum_ &= 0xFFFF;
                samples_to_send = std::min(samples_to_send, max_samples_per_pkt);

                uint32_t bytes_to_send = samples_to_send * bytes_per_frame_;
                size_t read_bytes = ring_buffer_.read(ptr, bytes_to_send);

                if (bytes_per_frame_ > 0) {
                    frames_played_.fetch_add(read_bytes / bytes_per_frame_, std::memory_order_relaxed);
                }

                // Zero out any underrun deficit to keep isochronous clock lock intact
                if (read_bytes < bytes_to_send) {
                    std::memset(ptr + read_bytes, 0, bytes_to_send - read_bytes);
                }

                // Apply 64-bit float software volume attenuation if multiplier != 1.0
                double mult = volume_multiplier_.load(std::memory_order_relaxed);
                if (std::abs(mult - 1.0) > 1e-6) {
                    if (bytes_per_sample_ == 2) { // 16-bit PCM
                        auto* samples = reinterpret_cast<int16_t*>(ptr);
                        size_t sample_count = bytes_to_send / sizeof(int16_t);
                        for (size_t i = 0; i < sample_count; ++i) {
                            double scaled = static_cast<double>(samples[i]) * mult;
                            samples[i] = static_cast<int16_t>(std::clamp(scaled, -32768.0, 32767.0));
                        }
                    } else if (bytes_per_sample_ == 3) { // 24-bit packed PCM
                        size_t sample_count = bytes_to_send / 3;
                        for (size_t i = 0; i < sample_count; ++i) {
                            size_t idx = i * 3;
                            int32_t sample = static_cast<int32_t>(
                                (static_cast<uint32_t>(ptr[idx])) |
                                (static_cast<uint32_t>(ptr[idx + 1]) << 8) |
                                (static_cast<uint32_t>(ptr[idx + 2]) << 16)
                            );
                            if (sample & 0x00800000) {
                                sample |= 0xFF000000;
                            }
                            double scaled = static_cast<double>(sample) * mult;
                            int32_t clamped = static_cast<int32_t>(std::clamp(scaled, -8388608.0, 8388607.0));
                            ptr[idx] = static_cast<uint8_t>(clamped & 0xFF);
                            ptr[idx + 1] = static_cast<uint8_t>((clamped >> 8) & 0xFF);
                            ptr[idx + 2] = static_cast<uint8_t>((clamped >> 16) & 0xFF);
                        }
                    } else if (bytes_per_sample_ == 4) { // 32-bit PCM
                        auto* samples = reinterpret_cast<int32_t*>(ptr);
                        size_t sample_count = bytes_to_send / sizeof(int32_t);
                        for (size_t i = 0; i < sample_count; ++i) {
                            double scaled = static_cast<double>(samples[i]) * mult;
                            samples[i] = static_cast<int32_t>(std::clamp(scaled, -2147483648.0, 2147483647.0));
                        }
                    }
                }

                u->iso_frame_desc[p].length = bytes_to_send;
                u->iso_frame_desc[p].actual_length = 0;
                u->iso_frame_desc[p].status = 0;

                ptr += bytes_to_send;
                total_length += static_cast<int>(bytes_to_send);
            }

            u->buffer_length = total_length;
            u->number_of_packets = static_cast<int>(PACKETS_PER_URB);

            // Resubmit URB
            if (ioctl(fd_, USBDEVFS_SUBMITURB, u) == 0) {
                ctx->submitted = true;
            }
        } else {
            // Sleep 1 interval period if idle to avoid CPU pegging
            usleep(intervals_per_sec_ == 1000 ? 1000 : 125);
        }
    }
}

} // namespace echo::music::usbaudio
