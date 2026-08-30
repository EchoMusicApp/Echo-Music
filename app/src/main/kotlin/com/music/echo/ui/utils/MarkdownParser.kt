package echo.music.iad1tya.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

fun parseSimpleMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        var lastIndex = 0

        val matches = boldRegex.findAll(text)
        for (match in matches) {
            val normalText = text.substring(lastIndex, match.range.first)
            append(normalText)
            
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }
            
            lastIndex = match.range.last + 1
        }
        
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}
