import type { LyricLine, LyricWord } from '../../types/lyrics.ts'

export function parseTTMLTime(timeStr: string): number {
  if (!timeStr) return 0
  const trimmed = timeStr.trim()
  if (trimmed.endsWith('ms')) {
    return parseFloat(trimmed.slice(0, -2))
  }
  if (trimmed.endsWith('s')) {
    return parseFloat(trimmed.slice(0, -1)) * 1000
  }
  if (trimmed.includes(':')) {
    const parts = trimmed.split(':')
    if (parts.length === 3) {
      const hours = parseFloat(parts[0] || '0')
      const minutes = parseFloat(parts[1] || '0')
      const seconds = parseFloat(parts[2] || '0')
      return Math.round((hours * 3600 + minutes * 60 + seconds) * 1000)
    }
    if (parts.length === 2) {
      const minutes = parseFloat(parts[0] || '0')
      const seconds = parseFloat(parts[1] || '0')
      return Math.round((minutes * 60 + seconds) * 1000)
    }
  }
  const numeric = parseFloat(trimmed)
  return isNaN(numeric) ? 0 : Math.round(numeric * 1000)
}

function cleanXmlEntities(raw: string): string {
  return raw
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
}

export class TTMLParser {
  public static parse(ttmlXml: string): LyricLine[] {
    if (!ttmlXml || typeof ttmlXml !== 'string') {
      return []
    }

    if (typeof DOMParser !== 'undefined') {
      try {
        const parsed = this.parseWithDOMParser(ttmlXml)
        if (parsed.length > 0) return parsed
      } catch {
        // Fall back to robust regex token parser
      }
    }

    return this.parseWithRegex(ttmlXml)
  }

  private static parseWithDOMParser(ttmlXml: string): LyricLine[] {
    const parser = new DOMParser()
    const doc = parser.parseFromString(ttmlXml, 'text/xml')
    const pElements = doc.getElementsByTagName('p')
    const result: LyricLine[] = []

    for (let i = 0; i < pElements.length; i++) {
      const p = pElements[i]
      if (!p) continue

      const beginAttr = p.getAttribute('begin')
      const endAttr = p.getAttribute('end')
      const agentAttr =
        p.getAttribute('ttm:agent') ||
        p.getAttribute('agent') ||
        p.getAttributeNS('http://www.w3.org/ns/ttml#metadata', 'agent') ||
        undefined

      const spanElements = p.getElementsByTagName('span')
      const words: LyricWord[] = []

      for (let j = 0; j < spanElements.length; j++) {
        const span = spanElements[j]
        if (!span) continue

        const role =
          span.getAttribute('ttm:role') ||
          span.getAttribute('role') ||
          span.getAttributeNS('http://www.w3.org/ns/ttml#metadata', 'role')

        if (role === 'x-translation' || role === 'x-roman') {
          continue
        }

        const spanBegin = span.getAttribute('begin')
        const spanEnd = span.getAttribute('end')
        const spanText = (span.textContent || '').trim()

        if (spanText && spanBegin && spanEnd) {
          words.push({
            text: spanText,
            startTimeMs: parseTTMLTime(spanBegin),
            endTimeMs: parseTTMLTime(spanEnd),
          })
        }
      }

      const lineStart = beginAttr
        ? parseTTMLTime(beginAttr)
        : words.length > 0 && words[0]
          ? words[0].startTimeMs
          : 0

      const lineEnd = endAttr
        ? parseTTMLTime(endAttr)
        : words.length > 0 && words[words.length - 1]
          ? words[words.length - 1]!.endTimeMs
          : undefined

      let lineText = ''
      if (words.length > 0) {
        lineText = words.map((w) => w.text).join(' ')
      } else {
        lineText = (p.textContent || '').trim()
      }

      if (lineText) {
        const line: LyricLine = {
          startTimeMs: lineStart,
          text: lineText,
        }
        if (lineEnd !== undefined) {
          line.endTimeMs = lineEnd
        }
        if (words.length > 0) {
          line.words = words
        }
        if (agentAttr) {
          line.agent = agentAttr
        }
        result.push(line)
      }
    }

    return result
  }

  private static parseWithRegex(ttmlXml: string): LyricLine[] {
    const result: LyricLine[] = []
    const pTagRegex = /<p\b([^>]*)>([\s\S]*?)<\/p>/gi
    let pMatch: RegExpExecArray | null

    while ((pMatch = pTagRegex.exec(ttmlXml)) !== null) {
      const pAttrs = pMatch[1] || ''
      const pContent = pMatch[2] || ''

      const beginMatch = /begin=["']([^"']+)["']/i.exec(pAttrs)
      const endMatch = /end=["']([^"']+)["']/i.exec(pAttrs)
      const agentMatch = /(?:ttm:)?agent=["']([^"']+)["']/i.exec(pAttrs)

      const words: LyricWord[] = []
      const spanRegex = /<span\b([^>]*)>([\s\S]*?)<\/span>/gi
      let spanMatch: RegExpExecArray | null

      while ((spanMatch = spanRegex.exec(pContent)) !== null) {
        const spanAttrs = spanMatch[1] || ''
        const spanRawText = (spanMatch[2] || '').replace(/<[^>]+>/g, '').trim()
        const spanText = cleanXmlEntities(spanRawText)

        const roleMatch = /(?:ttm:)?role=["']([^"']+)["']/i.exec(spanAttrs)
        const role = roleMatch ? roleMatch[1]?.toLowerCase() : ''
        if (role === 'x-translation' || role === 'x-roman') {
          continue
        }

        const spanBeginMatch = /begin=["']([^"']+)["']/i.exec(spanAttrs)
        const spanEndMatch = /end=["']([^"']+)["']/i.exec(spanAttrs)

        if (spanText && spanBeginMatch && spanBeginMatch[1] && spanEndMatch && spanEndMatch[1]) {
          words.push({
            text: spanText,
            startTimeMs: parseTTMLTime(spanBeginMatch[1]),
            endTimeMs: parseTTMLTime(spanEndMatch[1]),
          })
        }
      }

      const pBeginStr = beginMatch ? beginMatch[1] : undefined
      const pEndStr = endMatch ? endMatch[1] : undefined

      const lineStart = pBeginStr
        ? parseTTMLTime(pBeginStr)
        : words.length > 0 && words[0]
          ? words[0].startTimeMs
          : 0

      const lineEnd = pEndStr
        ? parseTTMLTime(pEndStr)
        : words.length > 0 && words[words.length - 1]
          ? words[words.length - 1]!.endTimeMs
          : undefined

      let lineText = ''
      if (words.length > 0) {
        lineText = words.map((w) => w.text).join(' ')
      } else {
        const stripped = pContent.replace(/<[^>]+>/g, '').trim()
        lineText = cleanXmlEntities(stripped)
      }

      if (lineText) {
        const line: LyricLine = {
          startTimeMs: lineStart,
          text: lineText,
        }
        if (lineEnd !== undefined) {
          line.endTimeMs = lineEnd
        }
        if (words.length > 0) {
          line.words = words
        }
        if (agentMatch && agentMatch[1]) {
          line.agent = agentMatch[1]
        }
        result.push(line)
      }
    }

    // If no <p> tags were present, test if root has direct spans
    if (result.length === 0) {
      const spanRegex = /<span\b([^>]*)>([\s\S]*?)<\/span>/gi
      let spanMatch: RegExpExecArray | null
      const standaloneWords: LyricWord[] = []

      while ((spanMatch = spanRegex.exec(ttmlXml)) !== null) {
        const spanAttrs = spanMatch[1] || ''
        const spanRawText = (spanMatch[2] || '').replace(/<[^>]+>/g, '').trim()
        const spanText = cleanXmlEntities(spanRawText)
        const spanBeginMatch = /begin=["']([^"']+)["']/i.exec(spanAttrs)
        const spanEndMatch = /end=["']([^"']+)["']/i.exec(spanAttrs)

        if (spanText && spanBeginMatch && spanBeginMatch[1] && spanEndMatch && spanEndMatch[1]) {
          standaloneWords.push({
            text: spanText,
            startTimeMs: parseTTMLTime(spanBeginMatch[1]),
            endTimeMs: parseTTMLTime(spanEndMatch[1]),
          })
        }
      }

      if (standaloneWords.length > 0) {
        result.push({
          startTimeMs: standaloneWords[0]!.startTimeMs,
          endTimeMs: standaloneWords[standaloneWords.length - 1]!.endTimeMs,
          text: standaloneWords.map((w) => w.text).join(' '),
          words: standaloneWords,
        })
      }
    }

    return result
  }
}
