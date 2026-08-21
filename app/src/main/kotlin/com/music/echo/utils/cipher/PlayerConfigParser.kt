package echo.music.iad1tya.utils.cipher

import org.json.JSONException
import org.json.JSONObject

/**
 * Parses and validates the player-config JSON (bundled asset and remote copies).
 */
object PlayerConfigParser {
    const val SUPPORTED_SCHEMA_VERSION = 1

    private val SIG_RE = Regex("""^[A-Za-z0-9${'$'}_]{1,8}\(\d+,\d+,INPUT\)$""")
    private val NCLASS_RE = Regex("""^[A-Za-z0-9${'$'}_]{1,8}$""")
    private val HASH_RE = Regex("""^[a-f0-9]{8}$""")

    sealed class ParseResult {
        data class Success(
            val configs: Map<String, FunctionNameExtractor.HardcodedPlayerConfig>,
            val skippedEntries: List<String>,
        ) : ParseResult()

        data class Failure(val reason: String) : ParseResult()
    }

    fun buildNJsExpression(nClass: String): String =
        "(function(n){try{var u=new g.$nClass('https://x.googlevideo.com/videoplayback?n='+n,true);" +
            "var t=u.get('n');return(t&&t!==n)?t:n;}catch(e){return n;}})(INPUT)"

    fun parse(jsonText: String): ParseResult {
        val root = try {
            JSONObject(jsonText)
        } catch (e: JSONException) {
            return ParseResult.Failure("malformed JSON: ${e.message}")
        }

        if (!root.has("schemaVersion")) {
            return ParseResult.Failure("schemaVersion missing")
        }
        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion <= 0) return ParseResult.Failure("schemaVersion must be positive")
        if (schemaVersion > SUPPORTED_SCHEMA_VERSION) {
            return ParseResult.Failure("unsupported schemaVersion $schemaVersion (supported: $SUPPORTED_SCHEMA_VERSION)")
        }

        val players = root.optJSONObject("players")
            ?: return ParseResult.Failure("players missing or not an object")

        val configs = mutableMapOf<String, FunctionNameExtractor.HardcodedPlayerConfig>()
        val skipped = mutableListOf<String>()

        val iter = players.keys()
        while (iter.hasNext()) {
            val hash = iter.next()
            val entryElement = players.optJSONObject(hash)
            val entry = parseEntry(hash, entryElement)
            if (entry == null) {
                skipped += hash
                continue
            }
            val (config, aliases) = entry
            
            val keys = listOf(hash) + aliases
            val duplicate = keys.firstOrNull { it in configs }
                ?: keys.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }?.key
            if (duplicate != null) {
                return ParseResult.Failure("duplicate hash/alias '$duplicate' (entry $hash)")
            }
            configs[hash] = config
            for (alias in aliases) configs[alias] = config
        }

        return ParseResult.Success(configs, skipped)
    }

    private fun parseEntry(
        hash: String,
        obj: JSONObject?,
    ): Pair<FunctionNameExtractor.HardcodedPlayerConfig, List<String>>? {
        if (obj == null || !HASH_RE.matches(hash)) return null

        val sig = obj.optString("sig", "")
        if (sig.isEmpty() || !SIG_RE.matches(sig)) return null

        val nClass = obj.optString("nClass", "")
        if (nClass.isEmpty() || !NCLASS_RE.matches(nClass)) return null

        val sts = obj.optInt("sts", -1)
        if (sts <= 0) return null

        val aliases = mutableListOf<String>()
        val aliasesArray = obj.optJSONArray("aliases")
        if (aliasesArray != null) {
            for (i in 0 until aliasesArray.length()) {
                val alias = aliasesArray.optString(i, "")
                if (alias.isEmpty() || !HASH_RE.matches(alias)) return null
                aliases.add(alias)
            }
        }

        val config = FunctionNameExtractor.HardcodedPlayerConfig(
            sigFuncName = "_expr_sig",
            sigConstantArg = null,
            sigJsExpression = sig,
            nFuncName = "_expr_n",
            nArrayIndex = null,
            nConstantArgs = null,
            nJsExpression = buildNJsExpression(nClass),
            signatureTimestamp = sts,
        )
        return config to aliases
    }

    fun merge(
        bundled: Map<String, FunctionNameExtractor.HardcodedPlayerConfig>,
        remote: Map<String, FunctionNameExtractor.HardcodedPlayerConfig>,
    ): Map<String, FunctionNameExtractor.HardcodedPlayerConfig> = bundled + remote
}
