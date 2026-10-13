package droga_krzyzowa.droga_krzyzowa.store

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

/** Shows the Google Play in-app rating dialog. Play applies its own quota, so the dialog may not appear. */
fun Activity.requestInAppReview() {
    val manager = ReviewManagerFactory.create(this)
    manager.requestReviewFlow().addOnCompleteListener { request ->
        if (request.isSuccessful) {
            manager.launchReviewFlow(this, request.result)
        } else {
            Log.w("InAppReview", "Review flow unavailable", request.exception)
        }
    }
}
