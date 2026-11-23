#pragma once

#include <cstdint>

namespace echo::music::usbaudio {

// USB Class, Subclass & Protocol Codes
inline constexpr uint8_t USB_CLASS_AUDIO = 0x01;
inline constexpr uint8_t USB_SUBCLASS_AUDIOCONTROL = 0x01;
inline constexpr uint8_t USB_SUBCLASS_AUDIOSTREAMING = 0x02;

inline constexpr uint8_t UAC_VERSION_1 = 0x00; // UAC 1.0 bInterfaceProtocol
inline constexpr uint8_t UAC_VERSION_2 = 0x20; // UAC 2.0 bInterfaceProtocol

// Descriptor Types
inline constexpr uint8_t USB_DT_DEVICE = 0x01;
inline constexpr uint8_t USB_DT_CONFIG = 0x02;
inline constexpr uint8_t USB_DT_STRING = 0x03;
inline constexpr uint8_t USB_DT_INTERFACE = 0x04;
inline constexpr uint8_t USB_DT_ENDPOINT = 0x05;
inline constexpr uint8_t USB_DT_CS_INTERFACE = 0x24;
inline constexpr uint8_t USB_DT_CS_ENDPOINT = 0x25;

// Audio Class-Specific AC Interface Descriptor Subtypes
inline constexpr uint8_t UAC_HEADER = 0x01;
inline constexpr uint8_t UAC_INPUT_TERMINAL = 0x02;
inline constexpr uint8_t UAC_OUTPUT_TERMINAL = 0x03;
inline constexpr uint8_t UAC_MIXER_UNIT = 0x04;
inline constexpr uint8_t UAC_SELECTOR_UNIT = 0x05;
inline constexpr uint8_t UAC_FEATURE_UNIT = 0x06;

// UAC2 specific AC Interface Subtypes
inline constexpr uint8_t UAC2_EFFECT_UNIT = 0x07;
inline constexpr uint8_t UAC2_PROCESSING_UNIT = 0x08;
inline constexpr uint8_t UAC2_EXTENSION_UNIT = 0x09;
inline constexpr uint8_t UAC2_CLOCK_SOURCE = 0x0A;
inline constexpr uint8_t UAC2_CLOCK_SELECTOR = 0x0B;
inline constexpr uint8_t UAC2_CLOCK_MULTIPLIER = 0x0C;

// Audio Class-Specific AS Interface Descriptor Subtypes
inline constexpr uint8_t UAC_AS_GENERAL = 0x01;
inline constexpr uint8_t UAC_FORMAT_TYPE = 0x02;

// Format Types
inline constexpr uint8_t UAC_FORMAT_TYPE_I = 0x01;
inline constexpr uint8_t UAC_FORMAT_TYPE_II = 0x02;
inline constexpr uint8_t UAC_FORMAT_TYPE_III = 0x03;

#pragma pack(push, 1)

struct StandardConfigDescriptor {
    uint8_t bLength;
    uint8_t bDescriptorType;
    uint16_t wTotalLength;
    uint8_t bNumInterfaces;
    uint8_t bConfigurationValue;
    uint8_t iConfiguration;
    uint8_t bmAttributes;
    uint8_t bMaxPower;
};

struct StandardInterfaceDescriptor {
    uint8_t bLength;
    uint8_t bDescriptorType;
    uint8_t bInterfaceNumber;
    uint8_t bAlternateSetting;
    uint8_t bNumEndpoints;
    uint8_t bInterfaceClass;
    uint8_t bInterfaceSubClass;
    uint8_t bInterfaceProtocol;
    uint8_t iInterface;
};

struct StandardEndpointDescriptor {
    uint8_t bLength;
    uint8_t bDescriptorType;
    uint8_t bEndpointAddress;
    uint8_t bmAttributes;
    uint16_t wMaxPacketSize;
    uint8_t bInterval;
};

struct Uac2ClockSourceDescriptor {
    uint8_t bLength;
    uint8_t bDescriptorType;
    uint8_t bDescriptorSubtype;
    uint8_t bClockID;
    uint8_t bmAttributes;
    uint8_t bmControls;
    uint8_t bAssocTerminal;
    uint8_t iClockSource;
};

struct Uac1FormatTypeIDescriptor {
    uint8_t bLength;
    uint8_t bDescriptorType;
    uint8_t bDescriptorSubtype;
    uint8_t bFormatType;
    uint8_t bNrChannels;
    uint8_t bSubFrameSize;
    uint8_t bBitResolution;
    uint8_t bSamFreqType;
    // Followed by tSamFreq bytes: 3 bytes each if bSamFreqType > 0, or 6 bytes (min/max) if 0
};

struct Uac2FormatTypeIDescriptor {
    uint8_t bLength;
    uint8_t bDescriptorType;
    uint8_t bDescriptorSubtype;
    uint8_t bFormatType;
    uint8_t bSubslotSize;
    uint8_t bBitResolution;
};

struct Uac2AsHeaderDescriptor {
    uint8_t bLength;
    uint8_t bDescriptorType;
    uint8_t bDescriptorSubtype;
    uint8_t bTerminalLink;
    uint8_t bmControls;
    uint8_t bFormatType;
    uint32_t bmFormats;
    uint8_t bNrChannels;
    uint32_t bmChannelConfig;
    uint8_t iChannelNames;
};

#pragma pack(pop)

} // namespace echo::music::usbaudio
