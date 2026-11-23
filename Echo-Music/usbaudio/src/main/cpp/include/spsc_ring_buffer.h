#pragma once

#include <cstddef>
#include <cstdint>
#include <atomic>
#include <vector>

namespace echo::music::usbaudio {

/**
 * Lock-free Single-Producer Single-Consumer (SPSC) FIFO ring buffer.
 *
 * Designed for low-latency PCM streaming from the producer thread (handleBuffer)
 * to the consumer real-time URB reaping/submission thread.
 *
 * Buffer capacity must be a power of two to allow bitwise masking index wraps.
 */
class SpscRingBuffer {
public:
    static constexpr size_t DEFAULT_CAPACITY = 128 * 1024; // 128 KB (~150ms of 16-bit 44.1kHz stereo)

    explicit SpscRingBuffer(size_t capacity = DEFAULT_CAPACITY);
    ~SpscRingBuffer() = default;

    // Disallow copy/move
    SpscRingBuffer(const SpscRingBuffer&) = delete;
    SpscRingBuffer& operator=(const SpscRingBuffer&) = delete;
    SpscRingBuffer(SpscRingBuffer&&) = delete;
    SpscRingBuffer& operator=(SpscRingBuffer&&) = delete;

    /**
     * Writes up to `size` bytes into the buffer from `data`.
     * Returns the actual number of bytes written (may be less than `size` if full).
     * Single producer thread only.
     */
    size_t write(const uint8_t* data, size_t size);

    /**
     * Reads up to `size` bytes from the buffer into `dest`.
     * Returns the actual number of bytes read (may be less than `size` if empty).
     * Single consumer thread only.
     */
    size_t read(uint8_t* dest, size_t size);

    /**
     * Returns the number of bytes available for reading.
     */
    [[nodiscard]] size_t availableRead() const;

    /**
     * Returns the number of bytes available for writing.
     */
    [[nodiscard]] size_t availableWrite() const;

    /**
     * Resets head and tail pointers.
     * Caution: Should only be called when streaming is stopped or synchronized.
     */
    void flush();

    [[nodiscard]] size_t capacity() const { return capacity_; }

private:
    const size_t capacity_;
    const size_t mask_;
    std::vector<uint8_t> buffer_;

    // Align atomic indices to avoid cache line false sharing (64-byte hardware cache line).
    alignas(64) std::atomic<size_t> write_index_{0};
    alignas(64) std::atomic<size_t> read_index_{0};
};

} // namespace echo::music::usbaudio
