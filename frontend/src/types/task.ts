export type TaskStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'REVIEW'
  | 'COMPLETED'

export type TaskPriority =
  | 'LOW'
  | 'MEDIUM'
  | 'HIGH'

export interface Task {
  id: number
  title: string
  description: string | null
  status: TaskStatus
  priority: TaskPriority
  projectId: number | null
  assigneeId: number | null
  totalTimeSeconds: number
  timerActive: boolean
  timerStartedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface TaskListItem extends Task {
  projectName: string
  assigneeName: string
}