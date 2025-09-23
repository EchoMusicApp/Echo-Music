package iad1tya.echo.music.ui.screen.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import iad1tya.echo.music.R
import iad1tya.echo.music.ui.component.SonicBoomAnimation
import iad1tya.echo.music.ui.theme.typo

@Composable
fun UserNameScreen(
    onNameEntered: (String) -> Unit,
    onSkip: () -> Unit
) {
    var userName by remember { mutableStateOf("") }
    var showContent by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
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
        
        // Focus on text field
        delay(500)
        focusRequester.requestFocus()
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
            // App Icon
            if (showContent) {
                AsyncImage(
                    model = R.mipmap.ic_launcher_round,
                    contentDescription = "Echo Music Logo",
                    modifier = Modifier.size(80.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // Title
            if (showContent) {
                Text(
                    text = "What's your name?",
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
                    text = "We'd love to personalize your experience",
                    style = typo.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(titleAlpha.value)
                )
                
                Spacer(modifier = Modifier.height(40.dp))
            }
            
            // Name Input
            if (showContent) {
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { 
                        Text(
                            text = "Your name",
                            color = Color.White.copy(alpha = 0.7f)
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Person",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .alpha(contentAlpha.value),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            if (userName.isNotBlank()) {
                                onNameEntered(userName.trim())
                            }
                        }
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Continue Button
                Button(
                    onClick = {
                        if (userName.isNotBlank()) {
                            onNameEntered(userName.trim())
                        }
                    },
                    enabled = userName.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(contentAlpha.value),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Continue",
                        style = typo.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Skip Button
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.alpha(contentAlpha.value)
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
