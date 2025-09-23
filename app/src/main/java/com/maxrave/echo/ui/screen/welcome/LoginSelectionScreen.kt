package iad1tya.echo.music.ui.screen.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iad1tya.echo.music.ui.component.SonicBoomAnimation
import iad1tya.echo.music.ui.theme.typo

@Composable
fun LoginSelectionScreen(
    userName: String? = null,
    isYouTubeLoggedIn: Boolean = false,
    isSpotifyLoggedIn: Boolean = false,
    onYouTubeLogin: () -> Unit,
    onSpotifyLogin: () -> Unit,
    onSkip: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    val titleAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        showContent = true
        
        // Animate title
        titleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
        
        // Animate content
        delay(300)
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Sonic Boom Animation (smaller)
            if (showContent) {
                SonicBoomAnimation(
                    modifier = Modifier.size(100.dp),
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(40.dp))
            }
            
            // Title
            if (showContent) {
                Text(
                    text = if (userName != null) "Hi, $userName!" else "Connect Your Music",
                    style = typo.headlineMedium.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(titleAlpha.value)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Connect your accounts for a personalized experience",
                    style = typo.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(titleAlpha.value)
                )
                
                Spacer(modifier = Modifier.height(40.dp))
            }
            
            // Login Options
            if (showContent) {
                Column(
                    modifier = Modifier.alpha(contentAlpha.value)
                ) {
                    // YouTube Login Card
                    LoginOptionCard(
                        title = "YouTube Music",
                        description = "Access your playlists and favorites",
                        icon = Icons.Default.PlayArrow,
                        isLoggedIn = isYouTubeLoggedIn,
                        onClick = onYouTubeLogin,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Spotify Login Card
                    LoginOptionCard(
                        title = "Spotify",
                        description = "Get lyrics, canvas, and enhanced features",
                        icon = Icons.Default.MusicNote,
                        isLoggedIn = isSpotifyLoggedIn,
                        onClick = onSpotifyLogin,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Continue Button
                    Button(
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Continue",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Continue to App",
                                style = typo.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Skip Button
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Skip for now",
                            color = Color.White.copy(alpha = 0.7f),
                            style = typo.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isLoggedIn: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = typo.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                
                Text(
                    text = if (isLoggedIn) "Connected" else description,
                    style = typo.bodySmall,
                    color = if (isLoggedIn) Color.Green else Color.White.copy(alpha = 0.7f)
                )
            }
            
            // Tick mark for logged in status
            if (isLoggedIn) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Connected",
                    tint = Color.Green,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
