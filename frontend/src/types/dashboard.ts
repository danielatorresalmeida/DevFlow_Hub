export interface DashboardTaskItem {
  id: number
  title: string
  status: string
  priority: string
  updatedAt: string | null
  assigneeName: string
}

export interface DashboardProjectItem {
  id: number
  name: string
  status: string
  endDate: string
  managerName: string
}

export interface DashboardSummary {
  collaboratorCount: number
  projectCount: number
  taskCount: number
  programCount: number
  pendingTaskCount: number
  inProgressTaskCount: number
  reviewTaskCount: number
  completedTaskCount: number
  trackedTimeSeconds: number
  recentTasks: DashboardTaskItem[]
  upcomingProjects: DashboardProjectItem[]
}