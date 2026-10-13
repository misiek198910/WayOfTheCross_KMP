package droga_krzyzowa.droga_krzyzowa.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import droga_krzyzowa.droga_krzyzowa.platform.LocalPlatformServices
import droga_krzyzowa.droga_krzyzowa.ui.theme.AppColors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import wayofthecross_kmp.shared.generated.resources.Res
import wayofthecross_kmp.shared.generated.resources.app_name
import wayofthecross_kmp.shared.generated.resources.emblem
import wayofthecross_kmp.shared.generated.resources.loading

private const val MIN_SPLASH_MILLIS = 1_200L

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val platform = LocalPlatformServices.current
    val currentOnFinished by rememberUpdatedState(onFinished)
    val intro = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            val ready = CompletableDeferred<Unit>()
            platform.prepareAppStart { ready.complete(Unit) }
            val minimumTime = async { delay(MIN_SPLASH_MILLIS) }
            intro.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            minimumTime.await()
            ready.await()
        }
        currentOnFinished()
    }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1_400), RepeatMode.Reverse),
        label = "glow",
    )

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(240.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse; alpha = intro.value }
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(AppColors.Gold.copy(alpha = 0.40f), Color.Transparent)))
            )
            Image(
                painterResource(Res.drawable.emblem),
                contentDescription = null,
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        alpha = intro.value
                        scaleX = 0.7f + 0.3f * intro.value
                        scaleY = 0.7f + 0.3f * intro.value
                    },
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = AppColors.Gold,
            modifier = Modifier.graphicsLayer {
                alpha = intro.value
                translationY = (1f - intro.value) * 24.dp.toPx()
            },
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(Res.string.loading),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            modifier = Modifier.graphicsLayer { alpha = intro.value },
        )
    }
}
