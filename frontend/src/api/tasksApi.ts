import { apiRequest } from './apiClient'
import type { AuthSession } from '../types/auth'
import type { Task } from '../types/task'

export function getTasks(
  session: AuthSession,
  signal?: AbortSignal,
): Promise<Task[]> {
  return apiRequest<Task[]>(
    '/api/tasks',
    { method: 'GET', signal },
    session,
  )
}

export function getTaskById(
  taskId: number,
  session: AuthSession,
  signal?: AbortSignal,
): Promise<Task> {
  return apiRequest<Task>(
    `/api/tasks/${taskId}`,
    { method: 'GET', signal },
    session,
  )
}