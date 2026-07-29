import { apiRequest } from './apiClient'
import type { AuthSession } from '../types/auth'
import type {
  ProjectOwnershipTransferResponse,
  TransferProjectOwnershipRequest,
} from '../types/projectMembership'

export function transferProjectOwnership(
  projectId: number,
  request: TransferProjectOwnershipRequest,
  session: AuthSession,
): Promise<ProjectOwnershipTransferResponse> {
  return apiRequest<ProjectOwnershipTransferResponse>(
    `/api/projects/${projectId}/ownership-transfer`,
    {
      method: 'POST',
      body: JSON.stringify(request),
    },
    session,
  )
}
