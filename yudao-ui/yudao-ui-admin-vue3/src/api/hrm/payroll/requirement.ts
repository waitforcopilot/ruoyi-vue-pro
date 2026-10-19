import request from '@/config/axios'

export interface PayrollRequirement {
  id?: number
  scopeMode?: string
  scopeJson?: string
  scopeState?: string
  projectScopeVersion?: number
  templateItemId?: string
  templateVersion?: number
  projectId?: number
  permittedActions?: string[]
  code: string
  module: string
  description: string
  sourceApplicability?: string
  applicabilityReason?: string
  ruleVersion?: string
  semanticVersion?: number
  currentConfirmationValid?: boolean
  fieldMapping?: string
  sourceSystem?: string
  sourceOwnerId?: number
  reviewerId?: number
  priority: number
  status: string
  readiness: string
  acceptance?: string
  evidence?: string
  version?: number
  confirmedBy?: number
  confirmedAt?: string
}
export const listRequirements = (module?: string, projectId?: number) =>
  request.get<PayrollRequirement[]>({
    url: '/hrm/payroll/requirement/list',
    params: { module, projectId }
  })
export const saveRequirement = (data: PayrollRequirement) =>
  request.post<number>({ url: '/hrm/payroll/requirement/save', data })
export const initializeRequirements = (projectId?: number) =>
  request.post<number>({ url: '/hrm/payroll/requirement/initialize', params: { projectId } })
export const requirementHistory = (id: number, projectId?: number) =>
  request.get({ url: '/hrm/payroll/requirement/history', params: { id, projectId } })
export const exportRequirements = (module?: string, projectId?: number) =>
  request.download({ url: '/hrm/payroll/requirement/export', params: { module, projectId } })

export interface PayrollRequirementProject {
  id: number
  name: string
  projectKey?: string
  scopeJson?: string
  ownerId?: number
  metadataVersion: number
  projectScopeVersion?: number
}
export const getLegacyRequirementProject = () =>
  request.get<PayrollRequirementProject | null>({ url: '/hrm/payroll/requirement/legacy-project' })

export const confirmRequirement = (
  id: number,
  version: number,
  projectId?: number,
  projectScopeVersion?: number
) =>
  request.post<boolean>({
    url: '/hrm/payroll/requirement/confirm',
    data: { id, version, projectId, projectScopeVersion }
  })

export const assignRequirement = (
  id: number,
  version: number,
  reviewerId?: number,
  projectId?: number,
  projectScopeVersion?: number
) =>
  request.post<boolean>({
    url: '/hrm/payroll/requirement/assign',
    data: { id, version, reviewerId: reviewerId ?? null, projectId, projectScopeVersion }
  })

export const listRequirementProjects = () =>
  request.get<PayrollRequirementProject[]>({ url: '/hrm/payroll/requirement-project/list' })
export const createRequirementProject = (data: {
  projectKey: string
  name: string
  creationScopeJson?: string
}) => request.post<number>({ url: '/hrm/payroll/requirement-project/create', data })
export const renameRequirementProject = (data: {
  id: number
  projectKey: string
  name: string
  metadataVersion: number
}) => request.post({ url: '/hrm/payroll/requirement-project/metadata', data })
export const saveRequirementProjectScope = (data: {
  projectId: number
  expectedVersion?: number
  scopeJson: string
  reason: string
}) => request.post({ url: '/hrm/payroll/requirement-project/scope', data })
export const requirementTemplateDifferences = (projectId: number) =>
  request.get<any[]>({
    url: '/hrm/payroll/requirement-project/template-differences',
    params: { projectId }
  })
export const decideRequirementTemplate = (data: {
  projectId: number
  requirementId: number
  version: number
  templateVersion: number
  accept: boolean
}) => request.post({ url: '/hrm/payroll/requirement-project/template-decision', data })
export const requirementProjectEvents = (projectId: number) =>
  request.get<any[]>({ url: '/hrm/payroll/requirement-project/events', params: { projectId } })

export const requirementCapabilities = (projectId: number) =>
  request.get<Record<string, boolean>>({
    url: '/hrm/payroll/requirement-project/capabilities',
    params: { projectId }
  })
export const requirementMembers = (projectId: number) =>
  request.get<any[]>({ url: '/hrm/payroll/requirement-project/members', params: { projectId } })
export const grantRequirementMember = (data: any) =>
  request.post<number>({ url: '/hrm/payroll/requirement-project/member-grant', data })
export const revokeRequirementMember = (data: any) =>
  request.post({ url: '/hrm/payroll/requirement-project/member-revoke', data })
export const eligibleRequirementReviewers = (projectId: number, requirementId: number) =>
  request.get<number[]>({
    url: '/hrm/payroll/requirement-project/eligible-reviewers',
    params: { projectId, requirementId }
  })
