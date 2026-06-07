package iad1tya.echo.music.recognition


object VibraSignature {

    const val REQUIRED_SAMPLE_RATE = 16_000

    
    @JvmStatic
    fun fromI16(samples: ByteArray): String = ShazamSignatureGenerator.fromI16(samples)
}
