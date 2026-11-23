#include "descriptor_parser.h"
#include <algorithm>
#include <cstring>
#include <set>

namespace echo::music::usbaudio {

ParsedDacCapabilities DescriptorParser::parse(const uint8_t* data, size_t size) {
    ParsedDacCapabilities caps;
    std::set<uint32_t> allSampleRates;

    if (!data || size < sizeof(StandardConfigDescriptor)) {
        return caps;
    }

    size_t offset = 0;
    int currentInterfaceClass = -1;
    int currentInterfaceSubClass = -1;
    int currentInterfaceProtocol = -1;
    uint8_t currentInterfaceNumber = 0;
    uint8_t currentAltSetting = 0;

    std::optional<ParsedFormat> currentFormat;

    while (offset + 2 <= size) {
        uint8_t descLen = data[offset];
        uint8_t descType = data[offset + 1];

        if (descLen == 0 || offset + descLen > size) {
            break;
        }

        const uint8_t* descData = &data[offset];

        if (descType == USB_DT_INTERFACE && descLen >= sizeof(StandardInterfaceDescriptor)) {
            const auto* iface = reinterpret_cast<const StandardInterfaceDescriptor*>(descData);
            currentInterfaceClass = iface->bInterfaceClass;
            currentInterfaceSubClass = iface->bInterfaceSubClass;
            currentInterfaceProtocol = iface->bInterfaceProtocol;
            currentInterfaceNumber = iface->bInterfaceNumber;
            currentAltSetting = iface->bAlternateSetting;

            if (currentInterfaceClass == USB_CLASS_AUDIO && currentInterfaceSubClass == USB_SUBCLASS_AUDIOCONTROL) {
                if (currentInterfaceProtocol == UAC_VERSION_2) {
                    caps.uacVersion = 2;
                } else if (currentInterfaceProtocol == UAC_VERSION_1) {
                    caps.uacVersion = 1;
                }
            }

            if (currentFormat.has_value()) {
                if (!currentFormat->sampleRates.empty() || caps.uacVersion == 2) {
                    caps.supportedFormats.push_back(*currentFormat);
                }
                currentFormat.reset();
            }

            if (currentInterfaceClass == USB_CLASS_AUDIO && currentInterfaceSubClass == USB_SUBCLASS_AUDIOSTREAMING) {
                if (currentAltSetting > 0) {
                    ParsedFormat fmt;
                    fmt.interfaceNumber = currentInterfaceNumber;
                    fmt.altSetting = currentAltSetting;
                    currentFormat = fmt;
                }
            }
        } else if (descType == USB_DT_CS_INTERFACE) {
            if (descLen >= 3) {
                uint8_t subtype = descData[2];

                if (currentInterfaceClass == USB_CLASS_AUDIO && currentInterfaceSubClass == USB_SUBCLASS_AUDIOCONTROL) {
                    if (caps.uacVersion == 2 && subtype == UAC2_CLOCK_SOURCE && descLen >= sizeof(Uac2ClockSourceDescriptor)) {
                        const auto* cs = reinterpret_cast<const Uac2ClockSourceDescriptor*>(descData);
                        caps.clockSourceId = cs->bClockID;
                        // In UAC2, sample rates are dynamically queried via CUR/RANGE requests on the clock source entity.
                        // Default to 48000 until runtime GET_RANGE query is performed.
                        allSampleRates.insert(48000);
                    } else if (subtype == UAC_FEATURE_UNIT && descLen >= 6) {
                        caps.hasHardwareVolume = true;
                        caps.volumeFeatureUnitId = descData[3];
                    }
                } else if (currentInterfaceClass == USB_CLASS_AUDIO && currentInterfaceSubClass == USB_SUBCLASS_AUDIOSTREAMING) {
                    if (subtype == UAC_AS_GENERAL && currentFormat.has_value()) {
                        if (caps.uacVersion == 2 && descLen >= 11) {
                            // In UAC2 AS General Descriptor, bNrChannels is at offset 10
                            currentFormat->channels = descData[10];
                        }
                    } else if (subtype == UAC_FORMAT_TYPE && currentFormat.has_value() && descLen >= 4) {
                        uint8_t formatType = descData[3];
                        if (formatType == UAC_FORMAT_TYPE_I) {
                            if (caps.uacVersion == 2 && descLen >= sizeof(Uac2FormatTypeIDescriptor)) {
                                const auto* f2 = reinterpret_cast<const Uac2FormatTypeIDescriptor*>(descData);
                                currentFormat->subslotBytes = f2->bSubslotSize;
                                currentFormat->bitDepth = f2->bBitResolution;
                            } else if (descLen >= sizeof(Uac1FormatTypeIDescriptor)) {
                                const auto* f1 = reinterpret_cast<const Uac1FormatTypeIDescriptor*>(descData);
                                currentFormat->channels = f1->bNrChannels;
                                currentFormat->subslotBytes = f1->bSubFrameSize;
                                currentFormat->bitDepth = f1->bBitResolution;

                                uint8_t samFreqType = f1->bSamFreqType;
                                if (samFreqType > 0 && descLen >= 8 + samFreqType * 3) {
                                    for (uint8_t i = 0; i < samFreqType; ++i) {
                                        size_t freqOffset = 8 + i * 3;
                                        uint32_t freq = static_cast<uint32_t>(descData[freqOffset]) |
                                                        (static_cast<uint32_t>(descData[freqOffset + 1]) << 8) |
                                                        (static_cast<uint32_t>(descData[freqOffset + 2]) << 16);
                                        currentFormat->sampleRates.push_back(freq);
                                        allSampleRates.insert(freq);
                                    }
                                } else if (samFreqType == 0 && descLen >= 14) {
                                    // Continuous frequency range: tLowerSamFreq (bytes 8..10), tUpperSamFreq (bytes 11..13)
                                    uint32_t minRate = static_cast<uint32_t>(descData[8]) |
                                                       (static_cast<uint32_t>(descData[9]) << 8) |
                                                       (static_cast<uint32_t>(descData[10]) << 16);
                                    uint32_t maxRate = static_cast<uint32_t>(descData[11]) |
                                                       (static_cast<uint32_t>(descData[12]) << 8) |
                                                       (static_cast<uint32_t>(descData[13]) << 16);
                                    static const uint32_t standardRates[] = {44100, 48000, 88200, 96000, 176400, 192000};
                                    for (uint32_t rate : standardRates) {
                                        if (rate >= minRate && rate <= maxRate) {
                                            currentFormat->sampleRates.push_back(rate);
                                            allSampleRates.insert(rate);
                                        }
                                    }
                                    if (currentFormat->sampleRates.empty() && minRate > 0) {
                                        currentFormat->sampleRates.push_back(minRate);
                                        allSampleRates.insert(minRate);
                                        if (maxRate != minRate) {
                                            currentFormat->sampleRates.push_back(maxRate);
                                            allSampleRates.insert(maxRate);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (descType == USB_DT_ENDPOINT && descLen >= sizeof(StandardEndpointDescriptor)) {
            const auto* ep = reinterpret_cast<const StandardEndpointDescriptor*>(descData);
            if (currentFormat.has_value()) {
                uint8_t epAttr = ep->bmAttributes & 0x03; // Transfer type (0x01 = isochronous)
                if (epAttr == 0x01) {
                    if ((ep->bEndpointAddress & 0x80) == 0) {
                        // OUT data endpoint
                        currentFormat->endpointAddress = ep->bEndpointAddress;
                    } else {
                        // IN sync/feedback endpoint
                        currentFormat->syncEndpointAddress = ep->bEndpointAddress;
                    }
                }
            }
        }

        offset += descLen;
    }

    if (currentFormat.has_value()) {
        if (!currentFormat->sampleRates.empty() || caps.uacVersion == 2) {
            caps.supportedFormats.push_back(*currentFormat);
        }
    }

    // For UAC2 formats that inherit sample rates from clock source:
    if (caps.uacVersion == 2) {
        for (auto& fmt : caps.supportedFormats) {
            if (fmt.sampleRates.empty()) {
                fmt.sampleRates.assign(allSampleRates.begin(), allSampleRates.end());
            }
        }
    }

    caps.supportedSampleRates.assign(allSampleRates.begin(), allSampleRates.end());
    return caps;
}

} // namespace echo::music::usbaudio
