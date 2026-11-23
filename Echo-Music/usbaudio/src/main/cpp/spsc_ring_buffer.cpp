#include "spsc_ring_buffer.h"
#include <algorithm>
#include <cstring>

namespace echo::music::usbaudio {

static size_t nextPowerOfTwo(size_t val) {
    if (val <= 1) return 1;
    size_t power = 1;
    while (power < val) {
        power <<= 1;
    }
    return power;
}

SpscRingBuffer::SpscRingBuffer(size_t capacity)
    : capacity_(nextPowerOfTwo(capacity)),
      mask_(capacity_ - 1),
      buffer_(capacity_) {}

size_t SpscRingBuffer::write(const uint8_t* data, size_t size) {
    if (!data || size == 0) return 0;

    const size_t current_write = write_index_.load(std::memory_order_relaxed);
    const size_t current_read = read_index_.load(std::memory_order_acquire);

    const size_t occupied = current_write - current_read;
    const size_t free_space = capacity_ - occupied;
    const size_t bytes_to_write = std::min(size, free_space);

    if (bytes_to_write == 0) return 0;

    const size_t start_offset = current_write & mask_;
    const size_t first_chunk = std::min(bytes_to_write, capacity_ - start_offset);
    const size_t second_chunk = bytes_to_write - first_chunk;

    std::memcpy(&buffer_[start_offset], data, first_chunk);
    if (second_chunk > 0) {
        std::memcpy(&buffer_[0], data + first_chunk, second_chunk);
    }

    write_index_.store(current_write + bytes_to_write, std::memory_order_release);
    return bytes_to_write;
}

size_t SpscRingBuffer::read(uint8_t* dest, size_t size) {
    if (!dest || size == 0) return 0;

    const size_t current_read = read_index_.load(std::memory_order_relaxed);
    const size_t current_write = write_index_.load(std::memory_order_acquire);

    const size_t occupied = current_write - current_read;
    const size_t bytes_to_read = std::min(size, occupied);

    if (bytes_to_read == 0) return 0;

    const size_t start_offset = current_read & mask_;
    const size_t first_chunk = std::min(bytes_to_read, capacity_ - start_offset);
    const size_t second_chunk = bytes_to_read - first_chunk;

    std::memcpy(dest, &buffer_[start_offset], first_chunk);
    if (second_chunk > 0) {
        std::memcpy(dest + first_chunk, &buffer_[0], second_chunk);
    }

    read_index_.store(current_read + bytes_to_read, std::memory_order_release);
    return bytes_to_read;
}

size_t SpscRingBuffer::availableRead() const {
    const size_t current_read = read_index_.load(std::memory_order_relaxed);
    const size_t current_write = write_index_.load(std::memory_order_acquire);
    return current_write - current_read;
}

size_t SpscRingBuffer::availableWrite() const {
    const size_t current_write = write_index_.load(std::memory_order_relaxed);
    const size_t current_read = read_index_.load(std::memory_order_acquire);
    const size_t occupied = current_write - current_read;
    return capacity_ - occupied;
}

void SpscRingBuffer::flush() {
    const size_t current_write = write_index_.load(std::memory_order_relaxed);
    read_index_.store(current_write, std::memory_order_release);
}

} // namespace echo::music::usbaudio
