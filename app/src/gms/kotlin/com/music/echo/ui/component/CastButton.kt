package echo.music.iad1tya.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.EnableGoogleCastKey
import echo.music.iad1tya.playback.CastConnectionHandler
import echo.music.iad1tya.utils.rememberPreference

/**
 * Cast button that shows a custom [CastDevicePickerSheet] bottom sheet.
 *
 * The picker discovers Cast devices via MediaRouter callbacks and connects
 * using the same internal mechanism the system MediaRouteChooserDialog uses,
 * avoiding the "Ignoring attempt to select removed route" crash on some OEM
 * MediaRouter implementations.
 */
@Composable
fun CastButton(
    modifier: Modifier = Modifier,
    tintColor: Color = MaterialTheme.colorScheme.onSurface,
    asMenuItem: Boolean = false,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val menuState = LocalMenuState.current

    val (enableGoogleCast) = rememberPreference(
        key = EnableGoogleCastKey,
        defaultValue = true
    )

    if (!enableGoogleCast) return

    // Check Cast availability once
    val castAvailable = remember(enableGoogleCast) {
        CastConnectionHandler.isCastAvailable(context)
    }
    if (!castAvailable) return

    // Cast state from the service
    val castHandler = playerConnection?.service?.castConnectionHandler
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castDeviceName by castHandler?.castDeviceName?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
    val deviceType by castHandler?.deviceType?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(CastDeviceType.UNKNOWN) }

    val showPicker: () -> Unit = {
        menuState.show {
            CastDevicePickerSheet(
                onDismiss = { menuState.dismiss() }
            )
        }
    }

    val showSession: () -> Unit = {
        menuState.show {
            CastSessionSheet(
                onDismiss = { menuState.dismiss() }
            )
        }
    }

    if (asMenuItem) {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isCasting) {
                            showSession()
                        } else {
                            showPicker()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                painter = painterResource(
                    if (isCasting) deviceType.connectedIcon else R.drawable.cast
                ),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(
                    if (isCasting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = if (isCasting) {
                    "Casting to ${castDeviceName ?: "Device"}"
                } else {
                    "Cast to..."
                },
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        Box(
            modifier = modifier
        ) {
            // Shadow background for cast button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Cast button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        if (isCasting) {
                            showSession()
                        } else {
                            showPicker()
                        }
                    }
            ) {
                Image(
                    painter = painterResource(
                        if (isCasting) deviceType.connectedIcon else R.drawable.cast
                    ),
                    contentDescription = if (isCasting) "Stop casting" else "Cast",
                    colorFilter = ColorFilter.tint(
                        if (isCasting) MaterialTheme.colorScheme.primary else tintColor
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
