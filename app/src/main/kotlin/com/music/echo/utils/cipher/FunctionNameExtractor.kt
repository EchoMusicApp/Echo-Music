package echo.music.iad1tya.utils.cipher

import timber.log.Timber
import java.security.MessageDigest

object FunctionNameExtractor {
    private const val TAG = "echomusic_CipherFnExtract"

    
    
    
    private val PLAYER_HASH_PATTERNS = listOf(
        Regex("""jsUrl['":\s]+[^"']*?/player/([a-f0-9]{8})/"""),
        Regex("""player_ias\.vflset/[^/]+/([a-f0-9]{8})/"""),
        Regex("""/s/player/([a-f0-9]{8})/""")
    )

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
        val constantArg: Int?,
        val jsExpression: String? = null,
        val isHardcoded: Boolean = false
    )

    data class NFunctionInfo(
        val name: String,
        val arrayIndex: Int?,
        val jsExpression: String? = null,
        val isHardcoded: Boolean = false
    )

    data class HardcodedPlayerConfig(
        val sigFuncName: String,
        val sigConstantArg: Int?,
        val sigJsExpression: String? = null,
        val nFuncName: String,
        val nArrayIndex: Int?,
        val nConstantArgs: List<Int>? = null,
        val nJsExpression: String? = null,
        val signatureTimestamp: Int
    )

    fun extractPlayerHash(playerJs: String): String? {
        for ((index, pattern) in PLAYER_HASH_PATTERNS.withIndex()) {
            val match = pattern.find(playerJs)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        val contentToHash = playerJs.take(10000)
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(contentToHash.toByteArray())
        return digest.take(4).joinToString("") { "%02x".format(it) }
    }

    fun getHardcodedConfig(playerHash: String): HardcodedPlayerConfig? {
        val config = PlayerConfigStore.get(playerHash)
        if (config != null) {
            Timber.tag(TAG).d("Found config for hash $playerHash")
        } else {
            Timber.tag(TAG).w("No config for hash: $playerHash")
        }
        return config
    }

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
