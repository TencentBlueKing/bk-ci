import { get, post } from '@/utils/http'

const STORE_API_URL_PREFIX = '/store/api'

export enum JobType {
  CREATIVE_STREAM = 'CREATIVE_STREAM',
  CLOUD_TASK = 'CLOUD_TASK',
}

export enum JobCategory {
  TRIGGER = 'TRIGGER',
  TASK = 'TASK',
}

export interface AtomItem {
  atomCode: string
  name: string
  summary: string
  logoUrl?: string
  classifyCode: string
  category: JobCategory
  installed: boolean
  defaultFlag: boolean
  disabled: boolean
  recommendFlag?: boolean
  version?: string
  defaultVersion?: string // 插件的默认版本
  os?: string[]
  score?: number
  recentExecuteNum?: number
  publisher?: string
  installFlag?: boolean // 是否有权限安装
  docsLink?: string // 文档链接
  hotFlag?: boolean // 是否热门
  honorInfos?: Array<{
    // 荣誉信息
    honorId: string
    honorTitle: string
    honorName: string
  }>
  indexInfos?: Array<{
    // 索引信息
    indexCode: string
    iconUrl: string
    iconColor: string
    hover: string
  }>
}

export interface AtomClassify {
  id: string
  classifyCode: string
  classifyName: string
}

export interface FetchAtomsParams {
  projectCode: string
  category: JobCategory
  jobType?: JobType
  classifyId?: string
  os?: string
  keyword?: string
  queryProjectAtomFlag?: boolean
  queryFitAgentBuildLessAtomFlag?: boolean
  fitOsFlag?: boolean
  page?: number
  pageSize?: number
  ownerStoreCode?: string
}

export interface FetchAtomsResponse {
  records: AtomItem[]
  count: number
  page: number
  pageSize: number
}

/**
 * 获取插件列表
 */
export function fetchAtoms(params: FetchAtomsParams): Promise<FetchAtomsResponse> {
  return get<FetchAtomsResponse>(`${STORE_API_URL_PREFIX}/user/pipeline/atom`, {
    params: {
      serviceScope: 'CREATIVE_STREAM',
      ...params,
    },
  })
}

/**
 * 搜索模式专用：支持 installed 参数，分页加载已安装/未安装插件列表
 */
export interface FetchSearchAtomsParams {
  projectCode: string
  category: JobCategory
  searchKey: string
  installed?: boolean
  os?: string
  page?: number
  pageSize?: number
}

export interface FetchSearchAtomsResponse {
  records: AtomItem[]
  count: number
  page: number
  pageSize: number
  totalPages: number
}

/**
 * 搜索插件列表（搜索模式专用）
 */
export function fetchSearchAtoms(params: FetchSearchAtomsParams): Promise<FetchSearchAtomsResponse> {
  return get<FetchSearchAtomsResponse>(`${STORE_API_URL_PREFIX}/user/pipeline/atom`, {
    params: {
      serviceScope: 'CREATIVE_STREAM',
      page: params.page,
      pageSize: params.pageSize,
      projectCode: params.projectCode,
      jobType: undefined,
      category: params.category,
      classifyId: undefined,
      os: undefined,
      keyword: params.searchKey,
      queryProjectAtomFlag: false,
      fitOsFlag: false,
      installed: params.installed,
    },
  })
}

/**
 * 获取插件分类列表
 */
export function fetchAtomClassify({
  category,
  serviceScope = 'CREATIVE_STREAM',
}: {
  category: JobCategory
  serviceScope?: string
}): Promise<AtomClassify[]> {
  return get<AtomClassify[]>(`${STORE_API_URL_PREFIX}/user/pipeline/atom/classify`, {
    params: {
      category,
      serviceScope,
    },
  })
}

/**
 * 安装插件
 */
export function installAtom(params: {
  atomCode: string
  projectCode: string[]
  version?: string
}): Promise<void> {
  return post(`${STORE_API_URL_PREFIX}/user/market/atom/install`, params)
}

/**
 * 插件版本信息
 */
export interface AtomVersion {
  atomCode: string
  version: string
  versionName?: string
  releaseType?: string
  status?: string
  defaultFlag?: boolean
  recommendFlag?: boolean
  latestFlag?: boolean
  creator?: string
  createTime?: string
  versionValue: string
}

/**
 * 插件配置模型
 */
export interface AtomModal {
  atomCode: string
  name: string
  version: string
  classType: string
  os?: string[]
  input?: Record<string, any>
  output?: Record<string, any>
  props?: Record<string, any>
  [key: string]: any
}

/**
 * 获取插件版本列表
 */
export function fetchAtomVersionList(params: {
  projectCode: string
  atomCode: string
}): Promise<AtomVersion[]> {
  const { projectCode, atomCode } = params
  return get<AtomVersion[]>(
    `${STORE_API_URL_PREFIX}/user/pipeline/atom/projectCodes/${projectCode}/atomCodes/${atomCode}/version/list`,
  )
}

/**
 * 获取插件配置详情
 */
export function fetchAtomModal(params: {
  projectCode: string
  atomCode: string
  version: string
  queryOfflineFlag?: boolean
}): Promise<AtomModal> {
  const { projectCode, atomCode, version, queryOfflineFlag = false } = params
  return get<AtomModal>(
    `${STORE_API_URL_PREFIX}/user/pipeline/atom/${projectCode}/${atomCode}/${version}`,
    {
      params: { queryOfflineFlag },
    },
  )
}
