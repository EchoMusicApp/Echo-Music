package iad1tya.echo.music.utils.cipher

import timber.log.Timber

object FunctionNameExtractor {
    private const val TAG = "echomusic_CipherFnExtract"

    
    
    
    private val SIG_FUNCTION_PATTERNS = listOf(
        
        Regex("""&&\s*\(\s*[a-zA-Z0-9$]+\s*=\s*([a-zA-Z0-9$]+)\s*\(\s*(\d+)\s*,\s*decodeURIComponent\s*\(\s*[a-zA-Z0-9$]+\s*\)"""),
        
        Regex("""\b[cs]\s*&&\s*[adf]\.set\([^,]+\s*,\s*encodeURIComponent\(([a-zA-Z0-9$]+)\("""),
        Regex("""\b[a-zA-Z0-9]+\s*&&\s*[a-zA-Z0-9]+\.set\([^,]+\s*,\s*encodeURIComponent\(([a-zA-Z0-9$]+)\("""),
        Regex("""\bm=([a-zA-Z0-9${'$'}]{2,})\(decodeURIComponent\(h\.s\)\)"""),
        Regex("""\bc\s*&&\s*d\.set\([^,]+\s*,\s*(?:encodeURIComponent\s*\()([a-zA-Z0-9$]+)\("""),
        Regex("""\bc\s*&&\s*[a-z]\.set\([^,]+\s*,\s*encodeURIComponent\(([a-zA-Z0-9$]+)\("""),
    )

    
    
    
    private val N_FUNCTION_PATTERNS = listOf(
        
        Regex("""\.get\("n"\)\)&&\(b=([a-zA-Z0-9$]+)(?:\[(\d+)\])?\(([a-zA-Z0-9])\)"""),
        
        Regex("""\.get\("n"\)\)\s*&&\s*\(([a-zA-Z0-9$]+)\s*=\s*([a-zA-Z0-9$]+)(?:\[(\d+)\])?\(\1\)"""),
        
        Regex("""\(\s*([a-zA-Z0-9$]+)\s*=\s*String\.fromCharCode\(110\)"""),
        
        Regex("""([a-zA-Z0-9$]+)\s*=\s*function\([a-zA-Z0-9]\)\s*\{[^}]*?enhanced_except_"""),
    )

    data class SigFunctionInfo(
        val name: String,
        val constantArg: Int? 
    )

    data class NFunctionInfo(
        val name: String,
        val arrayIndex: Int? 
    )

    fun extractSigFunctionInfo(playerJs: String): SigFunctionInfo? {
        for ((index, pattern) in SIG_FUNCTION_PATTERNS.withIndex()) {
            val match = pattern.find(playerJs)
            if (match != null) {
                val name = match.groupValues[1]
                val constArg = if (match.groupValues.size > 2) match.groupValues[2].toIntOrNull() else null
                Timber.tag(TAG).d("Sig function found with pattern $index: $name (constantArg=$constArg)")
                return SigFunctionInfo(name, constArg)
            }
        }
        Timber.tag(TAG).e("Could not find signature deobfuscation function name")
        return null
    }

    fun extractNFunctionInfo(playerJs: String): NFunctionInfo? {
        for ((index, pattern) in N_FUNCTION_PATTERNS.withIndex()) {
            val match = pattern.find(playerJs)
            if (match != null) {
                when (index) {
                    0 -> {
                        
                        val name = match.groupValues[1]
                        val arrayIdx = match.groupValues[2].toIntOrNull()
                        Timber.tag(TAG).d("N-function found with pattern $index: $name (arrayIndex=$arrayIdx)")
                        return NFunctionInfo(name, arrayIdx)
                    }
                    1 -> {
                        
                        val name = match.groupValues[2]
                        val arrayIdx = match.groupValues[3].toIntOrNull()
                        Timber.tag(TAG).d("N-function found with pattern $index: $name (arrayIndex=$arrayIdx)")
                        return NFunctionInfo(name, arrayIdx)
                    }
                    else -> {
                        val name = match.groupValues[1]
                        Timber.tag(TAG).d("N-function found with pattern $index: $name")
                        return NFunctionInfo(name, null)
                    }
                }
            }
        }
        Timber.tag(TAG).e("Could not find n-transform function name")
        return null
    }
}
