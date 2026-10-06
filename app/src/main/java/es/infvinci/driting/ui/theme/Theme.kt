package es.infvinci.driting.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF176B52), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF3E9), onPrimaryContainer = Color(0xFF123E32),
    secondary = Color(0xFF52675D),
    secondaryContainer = Color(0xFFDDF3E9), onSecondaryContainer = Color(0xFF123E32),
    background = Color(0xFFF5F7F3), surface = Color.White,
    onBackground = Color(0xFF172D25), onSurface = Color(0xFF172D25),
    surfaceVariant = Color(0xFFEDF1EB), onSurfaceVariant = Color(0xFF56675E),
    outlineVariant = Color(0xFFDCE4DA)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8CD7B7), onPrimary = Color(0xFF103B2D),
    primaryContainer = Color(0xFF214F3F), onPrimaryContainer = Color(0xFFB7F1D8),
    secondaryContainer = Color(0xFF214F3F), onSecondaryContainer = Color(0xFFB7F1D8),
    background = Color(0xFF101D17), surface = Color(0xFF192820),
    onBackground = Color(0xFFE4EFE7), onSurface = Color(0xFFE4EFE7),
    surfaceVariant = Color(0xFF293B31), onSurfaceVariant = Color(0xFFBCCBC0)
)

@Composable
fun DritingTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography, content = content)
}
