package droga_krzyzowa.droga_krzyzowa.store

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google Play flexible in-app updates: the update downloads in the background while the app is used,
 * then [updateReady] turns true and the UI offers a restart via [completeUpdate].
 * Must be created in Activity.onCreate (it registers an activity result launcher).
 */
class InAppUpdates(private val activity: ComponentActivity) : DefaultLifecycleObserver {
    private val manager = AppUpdateManagerFactory.create(activity)
    private val _updateReady = MutableStateFlow(false)
    val updateReady: StateFlow<Boolean> = _updateReady.asStateFlow()

    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) _updateReady.value = true
    }

    init {
        activity.lifecycle.addObserver(this)
    }

    /** Asks Play whether a newer version exists and, if so, starts downloading it in the background. */
    fun checkForUpdate() {
        manager.registerListener(listener)
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                when {
                    info.installStatus() == InstallStatus.DOWNLOADED -> _updateReady.value = true
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ->
                        manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE))
                }
            }
            .addOnFailureListener { Log.w(TAG, "Update check failed", it) }
    }

    fun completeUpdate() {
        manager.completeUpdate()
    }

    override fun onResume(owner: LifecycleOwner) {
        // An update may have finished downloading while the app was in the background.
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) _updateReady.value = true
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        manager.unregisterListener(listener)
    }

    private companion object {
        const val TAG = "InAppUpdates"
    }
}
