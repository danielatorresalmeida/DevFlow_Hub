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

function executeTaskAction(
  taskId: number,
  action:
    | 'start-timer'
    | 'pause-timer'
    | 'resume-timer'
    | 'complete',
  session: AuthSession,
  signal?: AbortSignal,
): Promise<Task> {
  return apiRequest<Task>(
    `/api/tasks/${taskId}/${action}`,
    {
      method: 'POST',
      signal,
    },
    session,
  )
}

export function startTaskTimer(
  taskId: number,
  session: AuthSession,
  signal?: AbortSignal,
): Promise<Task> {
  return executeTaskAction(
    taskId,
    'start-timer',
    session,
    signal,
  )
}

export function pauseTaskTimer(
  taskId: number,
  session: AuthSession,
  signal?: AbortSignal,
): Promise<Task> {
  return executeTaskAction(
    taskId,
    'pause-timer',
    session,
    signal,
  )
}

export function resumeTaskTimer(
  taskId: number,
  session: AuthSession,
  signal?: AbortSignal,
): Promise<Task> {
  return executeTaskAction(
    taskId,
    'resume-timer',
    session,
    signal,
  )
}

export function completeTask(
  taskId: number,
  session: AuthSession,
  signal?: AbortSignal,
): Promise<Task> {
  return executeTaskAction(
    taskId,
    'complete',
    session,
    signal,
  )
}