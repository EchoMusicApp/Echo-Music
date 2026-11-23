#pragma once

#include "spsc_ring_buffer.h"
#include <cstdint>
#include <cstddef>
#include <atomic>
#include <thread>
#include <vector>
#include <linux/usbdevice_fs.h>

namespace echo::music::usbaudio {

/**
 * Isochronous USB Streaming Engine managing URB allocation, submission, and reaping
 * with explicit feedback synchronization over Linux usbdevfs.
 */
class UsbStreamEngine {
public:
    static constexpr size_t NUM_URBS = 8;
    static constexpr size_t PACKETS_PER_URB = 8; // 8 microframes = 1 ms per URB on High-Speed USB 2.0
    static constexpr uint32_t MICROFRAMES_PER_SEC = 8000;

    UsbStreamEngine();
    ~UsbStreamEngine();

    // Disallow copy/move
    UsbStreamEngine(const UsbStreamEngine&) = delete;
    UsbStreamEngine& operator=(const UsbStreamEngine&) = delete;

    /**
     * Calculates the nominal integer number of samples per microframe (floor).
     */
    static uint32_t calculateNominalPacketSamples(uint32_t sampleRate);

    /**
     * Starts isochronous streaming loop.
     * @param fd Open Linux file descriptor from UsbDeviceConnection.getFileDescriptor()
     * @param interfaceNumber AudioStreaming interface number to claim
     * @param altSetting Alternate setting with active audio streaming endpoints
     * @param dataEp Endpoint address for data OUT
     * @param syncEp Endpoint address for explicit feedback IN (-1 if none)
     * @param sampleRate e.g. 44100, 48000, 96000, 192000
     * @param bitDepth e.g. 16, 24, 32
     * @param channels e.g. 2
     * @return 0 on success, negative errno on failure
     */
    int startStream(int fd, int interfaceNumber, int altSetting, int dataEp, int syncEp, uint32_t sampleRate, uint32_t bitDepth, uint32_t channels);

    int startStream(int fd, int dataEp, int syncEp, uint32_t sampleRate, uint32_t bitDepth, uint32_t channels) {
        return startStream(fd, 0, 1, dataEp, syncEp, sampleRate, bitDepth, channels);
    }

    /**
     * Stops streaming loop and releases URB resources.
     */
    int stopStream();

    /**
     * Enqueues PCM audio bytes into the lock-free ring buffer.
     * Called from producer thread.
     */
    size_t writeAudio(const uint8_t* buffer, size_t size);

    [[nodiscard]] size_t availableWrite() const { return ring_buffer_.availableWrite(); }

    void setVolumeMultiplier(double multiplier) {
        volume_multiplier_.store(multiplier, std::memory_order_relaxed);
    }

    [[nodiscard]] double getVolumeMultiplier() const {
        return volume_multiplier_.load(std::memory_order_relaxed);
    }

    void flush() {
        if (!streaming_.load(std::memory_order_relaxed)) {
            ring_buffer_.flush();
            fractional_accum_ = 0;
            frames_played_.store(0, std::memory_order_release);
        } else {
            flush_requested_.store(true, std::memory_order_release);
        }
    }

    [[nodiscard]] uint64_t getFramesPlayed() const {
        return frames_played_.load(std::memory_order_relaxed);
    }

    [[nodiscard]] bool isStreaming() const { return streaming_.load(std::memory_order_relaxed); }
    [[nodiscard]] SpscRingBuffer& ringBuffer() { return ring_buffer_; }

private:
    std::atomic<double> volume_multiplier_{1.0};
    std::atomic<uint64_t> frames_played_{0};
    std::atomic<bool> flush_requested_{false};
    void streamLoop();
    void submitInitialUrbs();
    void reapAndResubmitUrbs();

    std::atomic<bool> streaming_{false};
    std::thread stream_thread_;

    int fd_{-1};
    int interface_number_{0};
    int alt_setting_{1};
    int data_ep_{0};
    int sync_ep_{-1};
    uint32_t sample_rate_{44100};
    uint32_t bit_depth_{16};
    uint32_t channels_{2};
    uint32_t bytes_per_sample_{2};
    uint32_t bytes_per_frame_{4};
    uint32_t intervals_per_sec_{MICROFRAMES_PER_SEC};

    // Fractional sample pacing accumulator for asynchronous DACs without sync EP (fixed point 16.16)
    uint32_t fractional_step_{0};
    uint32_t fractional_accum_{0};

    // Feedback endpoint tracking
    uint32_t feedback_rate_q16_{0}; // 10.14 or 12.14 shifted to 16.16

    SpscRingBuffer ring_buffer_{SpscRingBuffer::DEFAULT_CAPACITY};

    // URBs and transfer buffers
    struct UrbContext {
        std::vector<uint8_t> urb_storage; // Holds usbdevfs_urb + trailing usbdevfs_iso_packet_desc[PACKETS_PER_URB]
        std::vector<uint8_t> buffer;
        bool submitted{false};

        usbdevfs_urb* urb() {
            return reinterpret_cast<usbdevfs_urb*>(urb_storage.data());
        }
    };

    std::vector<UrbContext> data_urbs_;
    std::vector<UrbContext> sync_urbs_;
};

} // namespace echo::music::usbaudio
