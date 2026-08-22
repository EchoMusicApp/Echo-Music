package echo.music.iad1tya.listentogether

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class ListenTogetherActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val client = ListenTogetherClient.getInstance() ?: return
        val notifId = intent.getIntExtra(ListenTogetherClient.EXTRA_NOTIFICATION_ID, 0)

        // Dismiss the notification that triggered this action
        NotificationManagerCompat.from(context).cancel(notifId)

        when (intent.action) {
            ListenTogetherClient.ACTION_APPROVE_JOIN -> {
                val userId = intent.getStringExtra(ListenTogetherClient.EXTRA_USER_ID) ?: return
                client.approveJoin(userId)
            }
            ListenTogetherClient.ACTION_REJECT_JOIN -> {
                val userId = intent.getStringExtra(ListenTogetherClient.EXTRA_USER_ID) ?: return
                client.rejectJoin(userId, null)
            }
            ListenTogetherClient.ACTION_APPROVE_SUGGESTION -> {
                val suggestionId = intent.getStringExtra(ListenTogetherClient.EXTRA_SUGGESTION_ID) ?: return
                client.approveSuggestion(suggestionId)
            }
            ListenTogetherClient.ACTION_REJECT_SUGGESTION -> {
                val suggestionId = intent.getStringExtra(ListenTogetherClient.EXTRA_SUGGESTION_ID) ?: return
                client.rejectSuggestion(suggestionId, null)
            }
        }
    }
}
