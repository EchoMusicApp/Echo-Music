#pragma once

#include "uac_defs.h"
#include <cstdint>
#include <cstddef>
#include <vector>
#include <optional>

namespace echo::music::usbaudio {

struct ParsedFormat {
    uint8_t interfaceNumber{0};
    uint8_t altSetting{0};
    uint8_t endpointAddress{0};
    std::optional<uint8_t> syncEndpointAddress;
    uint8_t bitDepth{16};
    uint8_t subslotBytes{2};
    uint8_t channels{2};
    std::vector<uint32_t> sampleRates;
};

struct ParsedDacCapabilities {
    int uacVersion{1};
    std::vector<ParsedFormat> supportedFormats;
    std::vector<uint32_t> supportedSampleRates;
    std::optional<uint8_t> clockSourceId;
    bool hasHardwareVolume{false};
    std::optional<uint8_t> volumeFeatureUnitId;
    float minVolumeDb{0.0f};
    float maxVolumeDb{0.0f};
    float volumeResDb{0.0f};
};

class DescriptorParser {
public:
    static ParsedDacCapabilities parse(const uint8_t* data, size_t size);
};

} // namespace echo::music::usbaudio
