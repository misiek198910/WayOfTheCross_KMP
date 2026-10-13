package droga_krzyzowa.droga_krzyzowa

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.core.content.edit
import com.russhwolf.settings.Settings
import droga_krzyzowa.droga_krzyzowa.billing.AdFreeBilling

/**
 * One-off import of what the previous native app (versionCode 45 and older) stored, so the update keeps
 * the user's state: the cached subscription (Room database) and the newest news item already read.
 */
object LegacyMigration {
    private const val KEY_DONE = "legacy_migration_done"
    private const val LEGACY_DATABASE = "way_of_the_cross_db"

    fun run(context: Context, settings: Settings) {
        if (settings.getBoolean(KEY_DONE, false)) return

        val lastReadNews = context.getSharedPreferences("news_prefs", Context.MODE_PRIVATE).getInt("last_read_id", 0)
        if (lastReadNews > 0) settings.putInt("news_last_read_id", lastReadNews)

        // A subscriber should not see the consent form and app open ad before Play confirms the subscription.
        if (legacyPremium(context)) {
            context.getSharedPreferences(AdFreeBilling.PREFS, Context.MODE_PRIVATE).edit { putBoolean(AdFreeBilling.KEY_ACTIVE, true) }
        }
        context.deleteDatabase(LEGACY_DATABASE)
        settings.putBoolean(KEY_DONE, true)
    }

    private fun legacyPremium(context: Context): Boolean {
        val file = context.getDatabasePath(LEGACY_DATABASE)
        if (!file.exists()) return false
        return try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("SELECT isPremium FROM subscription_status WHERE id = 1", null).use { cursor ->
                    cursor.moveToFirst() && cursor.getInt(0) != 0
                }
            }
        } catch (e: Exception) {
            Log.w("LegacyMigration", "Could not read the old subscription status", e)
            false
        }
    }
}
