package droga_krzyzowa.droga_krzyzowa.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import droga_krzyzowa.droga_krzyzowa.R

/** News arrive as Firebase Cloud Messaging pushes on the "news_all" topic, like in the previous app. */
object NewsNotifications {
    const val CHANNEL_ID = "dk_notifications_channel"
    private const val TOPIC = "news_all"
    private const val TAG = "NewsNotifications"

    /** Same file and key as the previous app, so users who already answered are not asked again. */
    private const val PREFS = "dk_notif_prefs"
    private const val KEY_HANDLED = "handled_permission"

    fun isPermissionGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun isPromptHandled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_HANDLED, false)

    fun markPromptHandled(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_HANDLED, true) }
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_news), NotificationManager.IMPORTANCE_HIGH)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun subscribe(context: Context) {
        // Null-safe when google-services.json was missing at build time, so Firebase is not initialized.
        if (FirebaseApp.getApps(context).isEmpty()) return
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC).addOnCompleteListener { task ->
            if (!task.isSuccessful) Log.w(TAG, "Topic subscription failed", task.exception)
        }
    }
}
