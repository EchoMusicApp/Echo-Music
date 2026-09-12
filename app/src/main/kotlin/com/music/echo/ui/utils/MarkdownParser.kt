package echo.music.iad1tya.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

fun parseSimpleMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        
        for ((index, line) in lines.withIndex()) {
            if (line.startsWith("# ")) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                    appendMarkdownInline(line.substring(2).trim())
                }
            } else if (line.startsWith("## ")) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                    appendMarkdownInline(line.substring(3).trim())
                }
            } else if (line.startsWith("### ")) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
                    appendMarkdownInline(line.substring(4).trim())
                }
            } else if (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) {
                val indent = line.takeWhile { it.isWhitespace() }.length
                append(" ".repeat(indent))
                append("• ")
                appendMarkdownInline(line.trimStart().drop(2).trim())
            } else {
                appendMarkdownInline(line)
            }
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.appendMarkdownInline(text: String) {
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
