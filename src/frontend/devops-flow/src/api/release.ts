import { get, post } from '../utils/http'

/**
 * Release parameters interface
 */
export interface ReleaseParams {
  description: string
  enablePac?: boolean
  yamlInfo?: {
    scmType: string
    repoHashId: string
    filePath: string
  } | null
  targetAction?: string
}

/**
 * Release response interface
 */
export interface ReleaseResponse {
  versionName: string
  targetUrl?: string
  updateBuildNo?: boolean
}

/**
 * Prefetch release version info
 */
export interface PrefetchVersionInfo {
  newVersionNum: number
  newVersionName: string
  baseVersionName: string
  enablePac: boolean
  yamlInfo?: {
    scmType: string
    repoHashId: string
    filePath: string
  }
}

/**
 * Release draft flow version
 * @param projectId Project ID
 * @param flowId Flow ID (pipelineId)
 * @param version Version number
 * @param params Release parameters
 */
export async function releaseFlowVersion(
  projectId: string,
  flowId: string,
  version: number | string,
  params: ReleaseParams,
): Promise<ReleaseResponse> {
  const response = await post<ReleaseResponse>(
    `/process/api/user/version/projects/${projectId}/pipelines/${flowId}/releaseVersion/${version}`,
    params,
  )
  return response
}

/**
 * Prefetch release version info
 * @param projectId Project ID
 * @param flowId Flow ID (pipelineId)
 * @param version Version number
 */
export async function prefetchReleaseVersion(
  projectId: string,
  flowId: string,
  version: number | string,
): Promise<PrefetchVersionInfo> {
  const response = await get<PrefetchVersionInfo>(
    `/process/api/user/version/projects/${projectId}/pipelines/${flowId}/releaseVersion/${version}/prefetch`,
  )
  return response
}

/**
 * Get SCM branch list for PAC mode
 * @param projectId Project ID
 * @param repositoryHashId Repository hash ID
 * @param search Search keyword
 */
export async function getScmBranchList(
  projectId: string,
  repositoryHashId: string,
  search?: string,
): Promise<string[]> {
  const response = await get<string[]>(
    `/process/api/user/scm/${projectId}/${repositoryHashId}/branches`,
    { params: { search } },
  )
  return response
}
