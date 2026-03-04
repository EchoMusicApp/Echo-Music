package iad1tya.echo.music.lyrics

import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

object TTMLParser {

    data class ParsedLine(
        val text: String,
        val startTime: Double,
        val words: List<ParsedWord>,
        val agent: String? = null,
        val isBackground: Boolean = false,
        val backgroundLines: List<ParsedLine> = emptyList()
    )

    data class ParsedWord(
        val text: String,
        val startTime: Double,
        val endTime: Double
    )

    private data class SpanInfo(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val hasTrailingSpace: Boolean
    )

    private fun Element.getAttributeByLocalName(localName: String): String {
        val nsValue = getAttributeNS("http://www.w3.org/ns/ttml#metadata", localName)
        if (nsValue.isNotEmpty()) return nsValue

        val prefixedValue = getAttribute("ttm:$localName")
        if (prefixedValue.isNotEmpty()) return prefixedValue

        val attrs = attributes
        for (i in 0 until attrs.length) {
            val attr = attrs.item(i)
            val attrName = attr.nodeName ?: continue
            if (attrName == localName || attrName.endsWith(":$localName")) {
                return attr.nodeValue ?: ""
            }
        }
        return ""
    }

    fun parseTTML(ttml: String): List<ParsedLine> {
        val lines = mutableListOf<ParsedLine>()

        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ttml.byteInputStream())

            val pElements = doc.getElementsByTagName("p")

            for (i in 0 until pElements.length) {
                val pElement = pElements.item(i) as? Element ?: continue

                val begin = pElement.getAttribute("begin")
                if (begin.isNullOrEmpty()) continue

                val startTime = parseTime(begin)
                val spanInfos = mutableListOf<SpanInfo>()
                val backgroundLines = mutableListOf<ParsedLine>()

                val agent = pElement.getAttributeByLocalName("agent").ifEmpty { null }

                val childNodes = pElement.childNodes
                for (j in 0 until childNodes.length) {
                    val node = childNodes.item(j)

                    when (node.nodeType) {
                        Node.ELEMENT_NODE -> {
                            val span = node as? Element
                            if (span?.tagName?.lowercase() == "span") {
                                val role = span.getAttributeByLocalName("role")

                                when (role) {
                                    "x-bg" -> {
                                        val bgLine = parseBackgroundSpan(span, startTime)
                                        if (bgLine != null) backgroundLines.add(bgLine)
                                    }
                                    "x-translation", "x-roman" -> { /* skip */ }
                                    else -> {
                                        val wordBegin = span.getAttribute("begin")
                                        val wordEnd = span.getAttribute("end")
                                        val wordText = span.textContent?.trim() ?: ""

                                        if (wordText.isNotEmpty() && wordBegin.isNotEmpty() && wordEnd.isNotEmpty()) {
                                            val nextSibling = node.nextSibling
                                            val hasTrailingSpace = nextSibling?.nodeType == Node.TEXT_NODE &&
                                                nextSibling.textContent?.contains(Regex("\\s")) == true

                                            spanInfos.add(SpanInfo(wordText, parseTime(wordBegin), parseTime(wordEnd), hasTrailingSpace))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val words = mergeSpansIntoWords(spanInfos)
                val lineText = words.joinToString(" ") { it.text }

                val finalText = if (lineText.isEmpty()) {
                    getDirectTextContent(pElement).trim()
                } else {
                    lineText
                }

                if (finalText.isNotEmpty()) {
                    lines.add(ParsedLine(finalText, startTime, words, agent, false, backgroundLines))
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }

        return lines
    }

    private fun parseBackgroundSpan(span: Element, parentStartTime: Double): ParsedLine? {
        val bgBegin = span.getAttribute("begin")
        val bgStartTime = if (bgBegin.isNotEmpty()) parseTime(bgBegin) else parentStartTime

        val spanInfos = mutableListOf<SpanInfo>()
        val childNodes = span.childNodes

        for (j in 0 until childNodes.length) {
            val node = childNodes.item(j)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val innerSpan = node as? Element
                if (innerSpan?.tagName?.lowercase() == "span") {
                    val role = innerSpan.getAttributeByLocalName("role")
                    if (role == "x-translation" || role == "x-roman") continue

                    val wordBegin = innerSpan.getAttribute("begin")
                    val wordEnd = innerSpan.getAttribute("end")
                    val wordText = innerSpan.textContent?.trim() ?: ""

                    if (wordText.isNotEmpty() && wordBegin.isNotEmpty() && wordEnd.isNotEmpty()) {
                        val nextSibling = node.nextSibling
                        val hasTrailingSpace = nextSibling?.nodeType == Node.TEXT_NODE &&
                            nextSibling.textContent?.contains(Regex("\\s")) == true
                        spanInfos.add(SpanInfo(wordText, parseTime(wordBegin), parseTime(wordEnd), hasTrailingSpace))
                    }
                }
            }
        }

        val words = mergeSpansIntoWords(spanInfos)
        val lineText = words.joinToString(" ") { it.text }
        val finalText = if (lineText.isEmpty()) getDirectTextContent(span).trim() else lineText

        return if (finalText.isNotEmpty()) {
            ParsedLine(finalText, bgStartTime, words, null, true, emptyList())
        } else null
    }

    private fun getDirectTextContent(element: Element): String {
        val sb = StringBuilder()
        val childNodes = element.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node.nodeType == Node.TEXT_NODE) {
                sb.append(node.textContent)
            } else if (node.nodeType == Node.ELEMENT_NODE) {
                val el = node as? Element
                val role = el?.getAttributeByLocalName("role") ?: ""
                if (role != "x-bg" && role != "x-translation" && role != "x-roman") {
                    if (el?.tagName?.lowercase() == "span") {
                        sb.append(el.textContent ?: "")
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun mergeSpansIntoWords(spanInfos: List<SpanInfo>): List<ParsedWord> {
        if (spanInfos.isEmpty()) return emptyList()

        val words = mutableListOf<ParsedWord>()
        var currentText = StringBuilder()
        var currentStartTime = spanInfos[0].startTime
        var currentEndTime = spanInfos[0].endTime

        for ((index, span) in spanInfos.withIndex()) {
            if (index == 0) {
                currentText.append(span.text)
                currentStartTime = span.startTime
                currentEndTime = span.endTime
            } else {
                val prevSpan = spanInfos[index - 1]
                if (prevSpan.hasTrailingSpace) {
                    if (currentText.isNotEmpty()) {
                        words.add(ParsedWord(currentText.toString().trim(), currentStartTime, currentEndTime))
                    }
                    currentText = StringBuilder(span.text)
                    currentStartTime = span.startTime
                    currentEndTime = span.endTime
                } else {
                    currentText.append(span.text)
                    currentEndTime = span.endTime
                }
            }
        }

        if (currentText.isNotEmpty()) {
            words.add(ParsedWord(currentText.toString().trim(), currentStartTime, currentEndTime))
        }

        return words
    }

    fun toLRC(lines: List<ParsedLine>): String {
        return buildString {
            lines.forEach { line ->
                val timeMs = (line.startTime * 1000).toLong()
                val minutes = timeMs / 60000
                val seconds = (timeMs % 60000) / 1000
                val centiseconds = (timeMs % 1000) / 10

                val agentPrefix = if (!line.agent.isNullOrEmpty()) "{agent:${line.agent}}" else ""
                appendLine(String.format("[%02d:%02d.%02d]%s%s", minutes, seconds, centiseconds, agentPrefix, line.text))

                if (line.words.isNotEmpty()) {
                    val wordsData = line.words.joinToString("|") { word ->
                        "${word.text}:${word.startTime}:${word.endTime}"
                    }
                    appendLine("<$wordsData>")
                }

                line.backgroundLines.forEach { bgLine ->
                    val bgTimeMs = (bgLine.startTime * 1000).toLong()
                    val bgMinutes = bgTimeMs / 60000
                    val bgSeconds = (bgTimeMs % 60000) / 1000
                    val bgCentiseconds = (bgTimeMs % 1000) / 10
                    appendLine(String.format("[%02d:%02d.%02d]{bg}%s", bgMinutes, bgSeconds, bgCentiseconds, bgLine.text))

                    if (bgLine.words.isNotEmpty()) {
                        val bgWordsData = bgLine.words.joinToString("|") { word ->
                            "${word.text}:${word.startTime}:${word.endTime}"
                        }
                        appendLine("<$bgWordsData>")
                    }
                }
            }
        }
    }

    private fun parseTime(timeStr: String): Double {
        return try {
            when {
                timeStr.contains(":") -> {
                    val parts = timeStr.split(":")
                    when (parts.size) {
                        2 -> parts[0].toDouble() * 60 + parts[1].toDouble()
                        3 -> parts[0].toDouble() * 3600 + parts[1].toDouble() * 60 + parts[2].toDouble()
                        else -> timeStr.toDoubleOrNull() ?: 0.0
                    }
                }
                else -> timeStr.toDoubleOrNull() ?: 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }
}
