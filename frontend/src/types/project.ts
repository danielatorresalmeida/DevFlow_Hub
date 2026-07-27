export interface Project {
  id: number
  name: string
  description: string | null
  status: string
  startDate: string | null
  endDate: string | null
  managerId: number | null
}

export interface ProjectListItem extends Project {
  managerName: string
}