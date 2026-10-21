package echo.music.iad1tya.ui.screens.settings

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where bug reports are delivered. */
const val BUG_REPORT_EMAIL = "akshaybiju602@gmail.com"

/**
 * Free access key from https://web3forms.com (enter BUG_REPORT_EMAIL there; the key is safe to ship).
 * When set, reports go through Web3Forms, which needs no activation click. When blank, FormSubmit is used.
 */
const val WEB3FORMS_ACCESS_KEY = "0ff633cb-668b-4efa-aaec-a098c335066c"

/**
 * Sends a bug report straight to [BUG_REPORT_EMAIL] over HTTPS (FormSubmit relay), so the user never
 * has to open an email app. Returns true when the relay accepted the report.
 */
suspend fun sendBugReport(message: String, contact: String): Boolean =
  withContext(Dispatchers.IO) {
    runCatching {
      val body =
        listOf(
            "_subject" to "Pattukal bug report (v${BuildConfig.VERSION_NAME})",
            "_template" to "table",
            "_captcha" to "false",
            "bug" to message,
            "reply_to" to contact.ifBlank { "not provided" },
            "app_version" to BuildConfig.VERSION_NAME,
            "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "android" to "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
          )
          .joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
          }
      val useWeb3 = WEB3FORMS_ACCESS_KEY.isNotBlank()
      val endpoint = if (useWeb3) "https://api.web3forms.com/submit" else "https://formsubmit.co/ajax/$BUG_REPORT_EMAIL"
      val payload = if (useWeb3) "$body&access_key=${URLEncoder.encode(WEB3FORMS_ACCESS_KEY, "UTF-8")}&subject=" + URLEncoder.encode("Pattukal bug report (v${BuildConfig.VERSION_NAME})", "UTF-8") + "&message=" + URLEncoder.encode(message, "UTF-8") else body
      val conn = URL(endpoint).openConnection() as HttpURLConnection
      try {
        conn.requestMethod = "POST"
        conn.connectTimeout = 15_000
        conn.readTimeout = 20_000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.setRequestProperty("Accept", "application/json")
        conn.outputStream.use { it.write(payload.toByteArray()) }
        val code = conn.responseCode
        val reply = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        // the relays answer 200 even when they refuse (e.g. not activated), so read the JSON verdict
        code in 200..299 && !reply.contains("\"success\":false") && !reply.contains("\"success\": false") &&
          !reply.contains("\"success\":\"false\"") && !reply.contains("\"success\": \"false\"")
      } finally {
        conn.disconnect()
      }
    }.getOrDefault(false)
  }

@Composable
fun ReportBugDialog(onDismiss: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var message by rememberSaveable { mutableStateOf("") }
  var contact by rememberSaveable { mutableStateOf("") }
  var sending by rememberSaveable { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = { if (!sending) onDismiss() },
    shape = RoundedCornerShape(28.dp),
    title = { Text("Report a bug") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          "Describe what went wrong. It is sent to the developer directly - no email app needed.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = message,
          onValueChange = { message = it },
          label = { Text("What happened?") },
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
        )
        OutlinedTextField(
          value = contact,
          onValueChange = { contact = it },
          label = { Text("Your email (optional)") },
          singleLine = true,
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(
        enabled = message.isNotBlank() && !sending,
        onClick = {
          sending = true
          scope.launch {
            val ok = sendBugReport(message.trim(), contact.trim())
            sending = false
            Toast.makeText(
                context,
                if (ok) "Bug report sent. Thank you!" else "Could not send. Check your internet and try again.",
                Toast.LENGTH_LONG,
              )
              .show()
            if (ok) onDismiss()
          }
        },
      ) {
        Text(if (sending) "Sending..." else "Send")
      }
    },
    dismissButton = { TextButton(enabled = !sending, onClick = onDismiss) { Text("Cancel") } },
  )
}
