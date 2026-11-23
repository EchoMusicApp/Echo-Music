import type { LyricLine } from '../../types/lyrics.ts'

export type AIProvider =
  'openrouter' | 'deepl' | 'mistral' | 'openai-compatible' | 'anthropic' | 'gemini'

export interface AIKeysConfig {
  provider: AIProvider
  openRouterKey?: string
  openRouterModel?: string
  deepLKey?: string
  deepLPlan?: 'free' | 'pro'
  mistralKey?: string
  mistralModel?: string
  openAiBaseUrl?: string
  openAiKey?: string
  openAiModel?: string
  anthropicKey?: string
  anthropicModel?: string
  geminiKey?: string
  geminiModel?: string
  targetLanguage: string
}

const STORAGE_KEY = 'echo_ai_keys'

const DEFAULT_CONFIG: AIKeysConfig = {
  provider: 'openrouter',
  openRouterModel: 'google/gemini-2.0-flash-001',
  targetLanguage: 'English',
}

export class TranslationClient {
  private static instance: TranslationClient | null = null
  private translationCache = new Map<string, string[]>()

  public static getInstance(): TranslationClient {
    if (!TranslationClient.instance) {
      TranslationClient.instance = new TranslationClient()
    }
    return TranslationClient.instance
  }

  public getConfig(): AIKeysConfig {
    try {
      const stored = localStorage.getItem(STORAGE_KEY)
      if (stored) {
        return { ...DEFAULT_CONFIG, ...JSON.parse(stored) }
      }
    } catch {
      // Fallback to default config
    }
    return DEFAULT_CONFIG
  }

  public saveConfig(config: Partial<AIKeysConfig>): void {
    try {
      const current = this.getConfig()
      const merged = { ...current, ...config }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(merged))
    } catch {
      // Ignore write errors
    }
  }

  public hasConfiguredKey(): boolean {
    const config = this.getConfig()
    switch (config.provider) {
      case 'openrouter':
        return Boolean(config.openRouterKey?.trim())
      case 'deepl':
        return Boolean(config.deepLKey?.trim())
      case 'mistral':
        return Boolean(config.mistralKey?.trim())
      case 'openai-compatible':
        return Boolean(config.openAiKey?.trim() && config.openAiBaseUrl?.trim())
      case 'anthropic':
        return Boolean(config.anthropicKey?.trim())
      case 'gemini':
        return Boolean(config.geminiKey?.trim())
      default:
        return false
    }
  }

  public async translateLyrics(
    lines: LyricLine[],
    overrideConfig?: Partial<AIKeysConfig>,
    signal?: AbortSignal,
  ): Promise<LyricLine[]> {
    if (!lines || lines.length === 0) return lines

    const config = { ...this.getConfig(), ...overrideConfig }
    const nonEmptyIndices: number[] = []
    const textsToTranslate: string[] = []

    lines.forEach((line, idx) => {
      if (line.text.trim()) {
        nonEmptyIndices.push(idx)
        textsToTranslate.push(line.text.trim())
      }
    })

    if (textsToTranslate.length === 0) return lines

    const cacheKey = `${config.targetLanguage}:${textsToTranslate.join('|')}`
    if (this.translationCache.has(cacheKey)) {
      const cached = this.translationCache.get(cacheKey)!
      return lines.map((line, idx) => {
        const itemIdx = nonEmptyIndices.indexOf(idx)
        if (itemIdx !== -1 && cached[itemIdx]) {
          return { ...line, translatedText: cached[itemIdx] }
        }
        return line
      })
    }

    let translatedStrings: string[] = []

    if (config.provider === 'deepl') {
      translatedStrings = await this.translateWithDeepL(textsToTranslate, config, signal)
    } else if (config.provider === 'mistral') {
      translatedStrings = await this.translateWithMistral(textsToTranslate, config, signal)
    } else if (config.provider === 'openai-compatible') {
      translatedStrings = await this.translateWithOpenAICompatible(textsToTranslate, config, signal)
    } else if (config.provider === 'anthropic') {
      translatedStrings = await this.translateWithAnthropic(textsToTranslate, config, signal)
    } else if (config.provider === 'gemini') {
      translatedStrings = await this.translateWithGemini(textsToTranslate, config, signal)
    } else {
      translatedStrings = await this.translateWithOpenRouter(textsToTranslate, config, signal)
    }

    if (translatedStrings.length > 0) {
      this.translationCache.set(cacheKey, translatedStrings)
      return lines.map((line, idx) => {
        const itemIdx = nonEmptyIndices.indexOf(idx)
        if (itemIdx !== -1 && translatedStrings[itemIdx]) {
          return { ...line, translatedText: translatedStrings[itemIdx] }
        }
        return line
      })
    }

    return lines
  }

  private async translateWithOpenRouter(
    texts: string[],
    config: AIKeysConfig,
    signal?: AbortSignal,
  ): Promise<string[]> {
    const key = config.openRouterKey?.trim()
    if (!key) throw new Error('OpenRouter API key is missing. Set your key in Lyrics Settings.')

    const indexedLines: Record<string, string> = {}
    texts.forEach((text, i) => {
      indexedLines[String(i)] = text
    })

    const model = config.openRouterModel || 'google/gemini-2.0-flash-001'
    const prompt = `Translate the following song lyric lines into ${config.targetLanguage}.
Maintain the poetic sentiment and rhythmic feel.
You MUST reply with a single valid JSON object mapping each line index to its translated string.
Format:
{
  "0": "Translated line 0",
  "1": "Translated line 1"
}
Do NOT wrap in markdown codeblocks if possible, or return raw JSON.

Lines to translate:
${JSON.stringify(indexedLines, null, 2)}`

    const res = await fetch('https://openrouter.ai/api/v1/chat/completions', {
      method: 'POST',
      ...(signal ? { signal } : {}),
      headers: {
        Authorization: `Bearer ${key}`,
        'Content-Type': 'application/json',
        'HTTP-Referer': window.location.origin,
        'X-Title': 'Echo Music Web',
      },
      body: JSON.stringify({
        model,
        messages: [
          {
            role: 'system',
            content:
              'You are a professional music and lyric translator. You respond only with valid JSON mappings.',
          },
          {
            role: 'user',
            content: prompt,
          },
        ],
        temperature: 0.3,
        response_format: { type: 'json_object' },
      }),
    })

    if (!res.ok) {
      const err = await res.text()
      throw new Error(`OpenRouter translation failed (${res.status}): ${err}`)
    }

    const data = (await res.json()) as {
      choices?: Array<{ message?: { content?: string } }>
    }
    const content = data.choices?.[0]?.message?.content || '{}'
    return this.parseIndexedJson(content, texts.length)
  }

  private async translateWithMistral(
    texts: string[],
    config: AIKeysConfig,
    signal?: AbortSignal,
  ): Promise<string[]> {
    const key = config.mistralKey?.trim()
    if (!key) throw new Error('Mistral API key is missing. Set your key in Lyrics Settings.')

    const indexedLines: Record<string, string> = {}
    texts.forEach((text, i) => {
      indexedLines[String(i)] = text
    })

    const model = config.mistralModel || 'mistral-small-latest'
    const prompt = `Translate the following song lyric lines into ${config.targetLanguage}.
Respond ONLY with a JSON object mapping string index to translation:
{
  "0": "Translated line 0"
}

Input:
${JSON.stringify(indexedLines, null, 2)}`

    const res = await fetch('https://api.mistral.ai/v1/chat/completions', {
      method: 'POST',
      ...(signal ? { signal } : {}),
      headers: {
        Authorization: `Bearer ${key}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model,
        messages: [{ role: 'user', content: prompt }],
        temperature: 0.3,
        response_format: { type: 'json_object' },
      }),
    })

    if (!res.ok) {
      const err = await res.text()
      throw new Error(`Mistral translation failed (${res.status}): ${err}`)
    }

    const data = (await res.json()) as {
      choices?: Array<{ message?: { content?: string } }>
    }
    const content = data.choices?.[0]?.message?.content || '{}'
    return this.parseIndexedJson(content, texts.length)
  }

  private async translateWithOpenAICompatible(
    texts: string[],
    config: AIKeysConfig,
    signal?: AbortSignal,
  ): Promise<string[]> {
    const key = config.openAiKey?.trim()
    const baseUrl = config.openAiBaseUrl?.trim().replace(/\/+$/, '')
    if (!key || !baseUrl)
      throw new Error(
        'OpenAI-compatible endpoint and key are required. Set them in Lyrics Settings.',
      )

    const indexedLines: Record<string, string> = {}
    texts.forEach((text, i) => {
      indexedLines[String(i)] = text
    })

    const prompt = `Translate the following song lyric lines into ${config.targetLanguage}.
Respond ONLY with a JSON object mapping string index to translation:
{
  "0": "Translated line 0"
}

Input:
${JSON.stringify(indexedLines, null, 2)}`

    const res = await fetch(`${baseUrl}/chat/completions`, {
      method: 'POST',
      ...(signal ? { signal } : {}),
      headers: {
        Authorization: `Bearer ${key}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model: config.openAiModel || 'gpt-4o-mini',
        messages: [{ role: 'user', content: prompt }],
        response_format: { type: 'json_object' },
      }),
    })

    if (!res.ok) {
      const err = await res.text()
      throw new Error(`OpenAI-compatible translation failed (${res.status}): ${err}`)
    }

    const data = (await res.json()) as {
      choices?: Array<{ message?: { content?: string } }>
    }
    const content = data.choices?.[0]?.message?.content || '{}'
    return this.parseIndexedJson(content, texts.length)
  }

  private async translateWithAnthropic(
    texts: string[],
    config: AIKeysConfig,
    signal?: AbortSignal,
  ): Promise<string[]> {
    const key = config.anthropicKey?.trim()
    if (!key) throw new Error('Anthropic API key is missing. Set your key in Lyrics Settings.')

    const indexedLines: Record<string, string> = {}
    texts.forEach((text, i) => {
      indexedLines[String(i)] = text
    })

    const model = config.anthropicModel || 'claude-haiku-4-5'
    const prompt = `Translate the following song lyric lines into ${config.targetLanguage}.
Respond ONLY with a JSON object mapping string index to translation:
{
  "0": "Translated line 0"
}

Input:
${JSON.stringify(indexedLines, null, 2)}`

    const res = await fetch('https://api.anthropic.com/v1/messages', {
      method: 'POST',
      ...(signal ? { signal } : {}),
      headers: {
        'x-api-key': key,
        'anthropic-version': '2023-06-01',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model,
        max_tokens: 8192,
        system:
          'You are a professional music and lyric translator. You respond only with valid JSON mappings.',
        messages: [{ role: 'user', content: prompt }],
      }),
    })

    if (!res.ok) {
      const err = await res.text()
      throw new Error(`Anthropic translation failed (${res.status}): ${err}`)
    }

    const data = (await res.json()) as {
      content?: Array<{ type: string; text?: string }>
    }
    const content =
      data.content
        ?.filter((b) => b.type === 'text')
        .map((b) => b.text || '')
        .join('') || '{}'
    return this.parseIndexedJson(content, texts.length)
  }

  private async translateWithGemini(
    texts: string[],
    config: AIKeysConfig,
    signal?: AbortSignal,
  ): Promise<string[]> {
    const key = config.geminiKey?.trim()
    if (!key) throw new Error('Gemini API key is missing. Set your key in Lyrics Settings.')

    const indexedLines: Record<string, string> = {}
    texts.forEach((text, i) => {
      indexedLines[String(i)] = text
    })

    const model = config.geminiModel || 'gemini-2.0-flash'
    const prompt = `Translate the following song lyric lines into ${config.targetLanguage}.
Respond ONLY with a JSON object mapping string index to translation:
{
  "0": "Translated line 0"
}

Input:
${JSON.stringify(indexedLines, null, 2)}`

    const res = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`,
      {
        method: 'POST',
        ...(signal ? { signal } : {}),
        headers: {
          'x-goog-api-key': key,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          contents: [{ role: 'user', parts: [{ text: prompt }] }],
          generationConfig: { responseMimeType: 'application/json' },
        }),
      },
    )

    if (!res.ok) {
      const err = await res.text()
      throw new Error(`Gemini translation failed (${res.status}): ${err}`)
    }

    const data = (await res.json()) as {
      candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>
    }
    const content = data.candidates?.[0]?.content?.parts?.map((p) => p.text || '').join('') || '{}'
    return this.parseIndexedJson(content, texts.length)
  }

  private async translateWithDeepL(
    texts: string[],
    config: AIKeysConfig,
    signal?: AbortSignal,
  ): Promise<string[]> {
    const key = config.deepLKey?.trim()
    if (!key) throw new Error('DeepL API key is missing. Set your key in Lyrics Settings.')

    const host =
      config.deepLPlan === 'pro'
        ? 'https://api.deepl.com/v2/translate'
        : 'https://api-free.deepl.com/v2/translate'

    // DeepL target language codes are uppercase 2-letter codes (e.g. EN, ES, DE, JA, FR)
    const targetLangCode = this.getDeepLLangCode(config.targetLanguage)

    const res = await fetch(host, {
      method: 'POST',
      ...(signal ? { signal } : {}),
      headers: {
        Authorization: `DeepL-Auth-Key ${key}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        text: texts,
        target_lang: targetLangCode,
      }),
    })

    if (!res.ok) {
      const err = await res.text()
      throw new Error(`DeepL translation failed (${res.status}): ${err}`)
    }

    const data = (await res.json()) as {
      translations?: Array<{ text: string }>
    }

    return (data.translations || []).map((t) => t.text)
  }

  private parseIndexedJson(content: string, count: number): string[] {
    try {
      const cleanJson = content
        .replace(/```json/gi, '')
        .replace(/```/g, '')
        .trim()
      const parsed = JSON.parse(cleanJson) as Record<string, string> | string[]

      if (Array.isArray(parsed)) {
        return parsed.map((s) => String(s || ''))
      }

      const result: string[] = []
      for (let i = 0; i < count; i++) {
        result.push(parsed[String(i)] || '')
      }
      return result
    } catch {
      return []
    }
  }

  private getDeepLLangCode(language: string): string {
    const map: Record<string, string> = {
      english: 'EN-US',
      spanish: 'ES',
      german: 'DE',
      french: 'FR',
      japanese: 'JA',
      chinese: 'ZH',
      italian: 'IT',
      portuguese: 'PT-BR',
      russian: 'RU',
      korean: 'KO',
    }
    return map[language.toLowerCase()] || 'EN-US'
  }
}
