package droga_krzyzowa.droga_krzyzowa.push

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import droga_krzyzowa.droga_krzyzowa.MainActivity
import droga_krzyzowa.droga_krzyzowa.R

/**
 * Shows news pushes while the app is in the foreground (in the background the system shows them).
 * A push with data action=promote_parish opens the "Moja Parafia" store page on the next app start.
 */
class NewsMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["action"] == PROMOTE_PARISH) PendingStoreRedirect.set(this, PARISH_APP_ID)
        val notification = message.notification ?: return
        if (!NewsNotifications.isPermissionGranted(this)) return

        NewsNotifications.createChannel(this)
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val built = NotificationCompat.Builder(this, NewsNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(this, R.color.gold))
            .setContentTitle(notification.title ?: getString(R.string.app_name))
            .setContentText(notification.body.orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body.orEmpty()))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), built)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }

    private companion object {
        const val PROMOTE_PARISH = "promote_parish"
        const val PARISH_APP_ID = "mivs.mojaparafia"
    }
}

/** Same preferences as the previous app's promotion flow; consumed by MainActivity when it resumes. */
object PendingStoreRedirect {
    private const val PREFS = "promotion_prefs"
    private const val KEY_APP_ID = "pending_store_app_id"

    fun set(context: Context, appId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY_APP_ID, appId) }
    }

    fun take(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val appId = prefs.getString(KEY_APP_ID, null) ?: return null
        prefs.edit { remove(KEY_APP_ID) }
        return appId
    }
}
