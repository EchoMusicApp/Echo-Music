package echo.music.iad1tya.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.window.DialogProperties
import echo.music.iad1tya.ui.theme.seed
import echo.music.iad1tya.ui.theme.typo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import echomusic.composeapp.generated.resources.*

@Composable
@ExperimentalMaterial3Api
fun ReviewDialog(
    onDismissRequest: () -> Unit,
    onDoneReview: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        properties =
            DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
        onDismissRequest = {
            onDismissRequest.invoke()
        },
        confirmButton = {
            TextButton(onClick = {
                onDoneReview.invoke()
                uriHandler.openUri("https://github.com/iad1tya/echoMusic")
            }) {
                Text(
                    stringResource(Res.string.give_a_star),
                    style = typo().bodySmall,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismissRequest.invoke()
            }) {
                Text(
                    stringResource(Res.string.later),
                    style = typo().bodySmall,
                )
            }
        },
        icon = {
            Icon(painterResource(Res.drawable.mono), "App Icon")
        },
        title = {
            Text(
                stringResource(Res.string.enjoying_echomusic),
                style = typo().labelSmall,
            )
        },
        text = {
            Text(
                buildAnnotatedString {
                    append(stringResource(Res.string.if_you_enjoy_using_echomusic_star_echomusic_on_github_or_leave_a_review_on))
                    withLink(
                        LinkAnnotation.Url(
                            "https://www.producthunt.com/products/echomusic",
                            TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline, color = seed)),
                        ) {
                            onDoneReview.invoke()
                            onDismissRequest.invoke()
                            uriHandler.openUri("https://www.producthunt.com/products/echomusic")
                        },
                    ) {
                        append(" ProductHunt")
                    }
                    append("\n")
                    append(stringResource(Res.string.if_you_love_my_work_consider))
                    withLink(
                        LinkAnnotation.Url(
                            "https://buymeacoffee.com/iad1tya",
                            TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline, color = seed)),
                        ) {
                            onDoneReview.invoke()
                            onDismissRequest.invoke()
                            uriHandler.openUri("https://buymeacoffee.com/iad1tya")
                        },
                    ) {
                        append(stringResource(Res.string.buying_me_a_coffee))
                    }
                },
                textAlign = TextAlign.Center,
                style = typo().bodySmall,
            )
        },
    )
}