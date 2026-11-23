package echo.music.usbaudio

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BitPerfectPcmTest {

    @Test
    fun sourceBitsAndBytesMatchesStandardEncodings() {
        assertEquals(16, BitPerfectPcm.sourceBits(C.ENCODING_PCM_16BIT))
        assertEquals(24, BitPerfectPcm.sourceBits(C.ENCODING_PCM_24BIT))
        assertEquals(24, BitPerfectPcm.sourceBits(C.ENCODING_PCM_FLOAT))
        assertEquals(32, BitPerfectPcm.sourceBits(C.ENCODING_PCM_32BIT))
        assertNull(BitPerfectPcm.sourceBits(C.ENCODING_PCM_8BIT))

        assertEquals(2, BitPerfectPcm.bytesPerSample(C.ENCODING_PCM_16BIT))
        assertEquals(3, BitPerfectPcm.bytesPerSample(C.ENCODING_PCM_24BIT))
        assertEquals(4, BitPerfectPcm.bytesPerSample(C.ENCODING_PCM_FLOAT))
        assertEquals(4, BitPerfectPcm.bytesPerSample(C.ENCODING_PCM_32BIT))
    }

    @Test
    fun outputBitsSelectsNarrowestSufficientDepth() {
        assertEquals(16, BitPerfectPcm.outputBits(16, listOf(16, 24, 32)))
        assertEquals(24, BitPerfectPcm.outputBits(24, listOf(16, 24, 32)))
        assertEquals(32, BitPerfectPcm.outputBits(24, listOf(16, 32)))
        assertNull(BitPerfectPcm.outputBits(32, listOf(16, 24)))
    }

    @Test
    fun pcm16BitTo16BitExactConversion() {
        val input = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        input.putShort(12345.toShort())
        input.putShort((-12345).toShort())
        input.flip()

        val output = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        val exact = BitPerfectPcm.convert(input, C.ENCODING_PCM_16BIT, 2, 16, output)

        assertTrue(exact)
        output.flip()
        assertEquals(12345.toShort(), output.short)
        assertEquals((-12345).toShort(), output.short)
    }

    @Test
    fun pcm16BitTo24BitExactConversion() {
        val input = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
        input.putShort(0x1234.toShort())
        input.flip()

        val output = ByteBuffer.allocate(3).order(ByteOrder.LITTLE_ENDIAN)
        val exact = BitPerfectPcm.convert(input, C.ENCODING_PCM_16BIT, 1, 24, output)

        assertTrue(exact)
        output.flip()
        // 16-bit 0x1234 left-aligned into 24-bit container: lower byte 0, upper 2 bytes 0x1234
        val b0 = output.get().toInt() and 0xFF
        val b1 = output.get().toInt() and 0xFF
        val b2 = output.get().toInt() and 0xFF
        assertEquals(0, b0)
        assertEquals(0x34, b1)
        assertEquals(0x12, b2)
    }
}
