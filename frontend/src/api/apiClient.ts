import type { ApiError, AuthSession } from '../types/auth'

export class ApiClientError extends Error {
  readonly status: number
  readonly validationErrors: Record<string, string>

  constructor(
    status: number,
    message: string,
    validationErrors: Record<string, string> = {},
  ) {
    super(message)
    this.name = 'ApiClientError'
    this.status = status
    this.validationErrors = validationErrors
  }
}

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() ?? ''
const apiBaseUrl = configuredBaseUrl.replace(/\/+$/, '')

function buildUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${apiBaseUrl}${normalizedPath}`
}

async function createApiError(response: Response): Promise<ApiClientError> {
  try {
    const body = (await response.json()) as Partial<ApiError>

    return new ApiClientError(
      response.status,
      body.message ?? 'The request could not be completed.',
      body.validationErrors ?? {},
    )
  } catch {
    return new ApiClientError(
      response.status,
      'The request could not be completed.',
    )
  }
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
  session?: AuthSession | null,
): Promise<T> {
  const headers = new Headers(options.headers)

  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (session) {
    headers.set(
      'Authorization',
      `${session.tokenType} ${session.accessToken}`,
    )
  }

  const response = await fetch(buildUrl(path), {
    ...options,
    headers,
  })

  if (!response.ok) {
    throw await createApiError(response)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}
