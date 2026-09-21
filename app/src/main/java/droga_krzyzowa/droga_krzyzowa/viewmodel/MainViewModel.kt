package droga_krzyzowa.droga_krzyzowa.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _isLoading = mutableStateOf(true)
    val isLoading: MutableState<Boolean> = _isLoading
    var isAppReadyForResumeAds = false

    fun finishLoading() {
        _isLoading.value = false
        isAppReadyForResumeAds = true
    }
}