export interface AuthenticatedCollaborator {
  id: number
  name: string
  email: string
  role: string
  active: boolean
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  collaborator: AuthenticatedCollaborator
}

export interface ApiError {
  timestamp: string
  status: number
  message: string
  validationErrors: Record<string, string>
}

export interface AuthSession {
  accessToken: string
  tokenType: string
  expiresAt: number
  collaborator: AuthenticatedCollaborator
}
