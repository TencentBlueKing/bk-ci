export interface ProjectMeta {
  projectCode?: string
  channelCode?: string
  [key: string]: unknown
}

export type AuthoringBaseOS = 'WINDOWS' | 'LINUX'

const PERSONAL_PROJECT_CHANNEL = 'PREBUILD'

interface WindowProjectContext {
  $currentProjectId?: string | null
  $projectList?: unknown
}

function getWindowProjectContext(): WindowProjectContext {
  if (typeof window === 'undefined') return {}
  return window as unknown as WindowProjectContext
}

export function getWindowProjectList(): ProjectMeta[] {
  const { $projectList } = getWindowProjectContext()
  return Array.isArray($projectList) ? ($projectList as ProjectMeta[]) : []
}

export function getCurrentProject(
  projectList: ProjectMeta[],
  projectId?: string | null,
): ProjectMeta | undefined {
  return projectList.find((project) => project.projectCode === projectId)
}

export function isPersonalProject(project?: ProjectMeta | null): boolean {
  return project?.channelCode === PERSONAL_PROJECT_CHANNEL
}

export function getAuthoringBaseOS(project?: ProjectMeta | null): AuthoringBaseOS {
  return isPersonalProject(project) ? 'WINDOWS' : 'LINUX'
}

export function getWindowAuthoringBaseOS(projectId?: string | null): AuthoringBaseOS {
  const context = getWindowProjectContext()
  const currentProject = getCurrentProject(
    getWindowProjectList(),
    projectId ?? context.$currentProjectId,
  )
  return getAuthoringBaseOS(currentProject)
}
