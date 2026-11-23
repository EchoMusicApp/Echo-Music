export class ApiError extends Error {
  public status: number
  public details?: unknown

  constructor(message: string, status: number, details?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

export async function apiFetch<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const globalProcess = (
    globalThis as unknown as { process?: { env?: Record<string, string | undefined> } }
  ).process
  const baseUrl =
    typeof window === 'undefined'
      ? globalProcess?.env?.['API_BASE_URL'] || 'http://localhost:3000'
      : ''
  const fullPath = endpoint.startsWith('/') ? endpoint : `/${endpoint}`
  const url = endpoint.startsWith('http') ? endpoint : `${baseUrl}${fullPath}`

  const headers: Record<string, string> = {
    Accept: 'application/json',
    ...(options.headers as Record<string, string> | undefined),
  }

  if (options.body && typeof options.body === 'string' && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }

  const response = await fetch(url, {
    ...options,
    headers,
  })

  if (!response.ok) {
    let errorDetails: unknown
    const text = await response.text().catch(() => '')
    try {
      errorDetails = text ? JSON.parse(text) : undefined
    } catch {
      errorDetails = text
    }

    const message =
      typeof errorDetails === 'object' && errorDetails !== null && 'error' in errorDetails
        ? String((errorDetails as { error: unknown }).error)
        : typeof errorDetails === 'string' &&
            errorDetails.trim().length > 0 &&
            !errorDetails.startsWith('<!DOCTYPE')
          ? errorDetails
          : `Request failed with status ${response.status}: ${response.statusText}`

    throw new ApiError(message, response.status, errorDetails)
  }

  return (await response.json()) as T
}
