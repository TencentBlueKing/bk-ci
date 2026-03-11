import {
  type FlowModel,
  type FlowPermissions,
  type FlowSettings,
  type ModelAndSetting,
  type Stage,
  type StageStatusInfo,
  type StatusType,
  type VersionStatus,
} from '@/types/flow'
import {
  ENVIRONMENT_API_URL_PREFIX,
  PROCESS_API_URL_PREFIX,
  STORE_API_URL_PREFIX,
} from '@/utils/apiUrlPrefix'
import { del, get, post, put } from '@/utils/http'

/**
 * 创作流首页内容表格相关 API
 */
// 统一从 types/flow 导入状态类型（已在上方导入）
export type { StatusType }

type TriggerType = 'manualTrigger' | 'timerTrigger' | 'codeGitWebHookTrigger' | 'remoteTrigger'
export type SortType = 'NAME' | 'CREATE_DATE' | 'LATEST_BUILD_START_DATA' | 'UPDATE_TIME'
export type Collation = 'ASC' | 'DESC' | 'null' | 'DEFAULT'
type CanUpdate = 'INTERNAL' | 'TRUE' | 'FALSE'

export interface ContentTableItem {
  latestBuildRoute?: object
  latestBuildStartDate?: string
  duration?: string
  progress?: string
  flowAction?: MenuItem[]
  disabled?: boolean
  tooltips?: string | { disabled: boolean }
  updateDate?: string
  released?: boolean
  onlyDraftVersion?: boolean
  onlyBranchVersion?: boolean
  createDate?: string
  model?: FlowModel
  projectId: string
  pipelineId: string
  pipelineName: string
  pipelineDesc: string
  taskCount: number
  buildCount: number
  lock: boolean
  canManualStartup: boolean
  latestBuildStartTime: number
  latestBuildEndTime: number
  latestBuildStatus?: StatusType
  latestBuildNum: number
  latestBuildTaskName?: string
  latestBuildEstimatedExecutionSeconds: number
  latestBuildId?: string
  deploymentTime: number
  createTime: number
  updateTime: number
  pipelineVersion: number
  currentTimestamp: number
  runningBuildCount: number
  hasPermission: boolean
  hasCollect: boolean
  latestBuildUserId: string
  instanceFromTemplate: boolean
  templateId?: string
  versionName?: string
  version?: number
  updater: string
  creator: string
  groupLabel?: [
    {
      groupName: string
      labelName: string[]
    },
  ]
  latestBuildNumAlias?: string
  buildNumRule?: string
  viewNames?: string[]
  lastBuildMsg?: string
  lastBuildTotalCount?: number
  lastBuildFinishCount?: number
  startType?: TriggerType
  trigger?: string
  webhookAliasName?: string
  webhookMessage?: string
  webhookRepoUrl?: string
  webhookType?: string
  delete: boolean
  latestVersionStatus?: VersionStatus
  permissions?: FlowPermissions
  yamlExist: boolean
  archivingFlag: boolean
  latestBuildStageStatus?: StageStatusInfo[]
}

export interface ContentTableResponse {
  count: number
  page: number
  pageSize: number
  totalPages: number
  records: ContentTableItem[]
}

export interface ContentTableParams {
  projectId: string
  page?: number
  pageSize?: number
  sortType?: SortType
  filterByPipelineName?: string // 创作流名称
  filterByViewIds?: string // 创作流组id
  viewId: string // 当前所在视图ID
  collation?: Collation
  status?: string // 状态过滤
}

export interface CreateContentFormData {
  baseInfo: {
    pipelineName: string
    pipelineDesc: string
    envHashId: string
  }
  templateInfo: {
    activeTemplate: TemplateObject
    currentModel: string
    cloneTemplateSet: string[]
    activeMenuItem: string
  }
}

export interface CreateContentParams {
  projectId: string
  templateId: string
  templateVersion: number
  pipelineName: string
  useSubscriptionSettings?: boolean // 是否使用通知配置
  useLabelSettings?: boolean // 是否使用标签配置
  useConcurrencyGroup?: boolean // 是否使用并发组配置
  instanceType?: string // 创建实例的模式
  emptyTemplate?: boolean // 是否为空模板
  pipelineDesc?: string
  envHashId?: string
}

export interface ImportContentParams {
  projectId: string
  version?: number
  pipelineId?: string
  actionType?: 'FULL_YAML2MODEL' | 'FULL_MODEL2YAML'
  modelAndSetting?: ModelAndSetting
  oldYaml?: string
  yamlFileName?: string
}

export interface ImportContentResponse {
  modelAndSetting?: ModelAndSetting
  newYaml?: string
  yamlSupported?: boolean
  yamlInvalidMsg?: string
}

export interface MenuItem<T = any> {
  text: string
  disable?: boolean
  hasPermission?: boolean
  disablePermissionApi?: boolean
  permissionData?: any
  tooltips?: string
  handler: (data: T, item: MenuItem) => void
}

export interface SaveAsTemplateParams {
  templateName: string
  copySetting: boolean
  pipelineId?: string
}

export interface CopyFlowParams {
  pipelineId?: string
  projectId?: string
  name: string
  desc?: string
  labels?: string[]
  staticView?: string[]
  dynamicGroup?: string[]
}

export interface DynamicParamLables {
  groupId: string
  labelIds: string[]
}

export interface MatchDynamicViewParams {
  labels: DynamicParamLables[]
  pipelineName: string
}

/**
 * 创作环境信息
 */
export interface AuthoringEnvItem {
  envHashId: string
  name: string
  desc: string
  envType: string
  envNodeType: string
  nodeCount: number
  tags: [
    {
      tagKeyId: number
      tagKeyName: string
      tagAllowMulValue: boolean
      canUpdate: CanUpdate
      tagValues: [
        {
          tagValueId: number
          tagValueName: string
          nodeCount: number
          canUpdate: CanUpdate
        },
      ]
    },
  ]
  envVars: [
    {
      name: string
      value: string
      secure: true
      lastUpdateUser: string
      lastUpdateTime: number
    },
  ]
  createdUser: string
  createdTime: number
  updatedUser: string
  updatedTime: number
  canEdit: boolean
  canDelete: boolean
  canUse: boolean
  projectName: string
}

/**
 * 创作节点信息
 */
export interface AuthoringNodeItem {
  nodeHashId: string
  nodeId: string
  name: string
  ip: string
  nodeStatus: string
  agentStatus: boolean
  nodeType: string
  osName: string
  createdUser: string
  operator: string
  bakOperator: string
  gateway: string
  displayName: string
  bizId: number
  envEnableNode: boolean
  lastModifyTime: number
  nodeName: string
  size: string
  agentHashId: string
  agentId: number
}

export interface AuthoringNodeResponse {
  count: number
  page: number
  pageSize: number
  totalPages: number
  records: AuthoringNodeItem[]
}

/**
 * 保存基础设置参数
 */
export interface SaveBaseInfoParams {
  flowName: string
  desc: string
  authoringEnv: string
  projectId: string
}

export interface DeleteContentParams {
  projectId: string
  pipelineIds: string[]
}

/**
 * 获取已选中的tree数据接口返回格式
 */
export interface SelectedTreeDataResponse {
  id?: string
  projectId?: string
  name?: string
  projected?: boolean
  createTime?: number
  updateTime?: number
  creator?: string
  top?: boolean
  viewType?: number
  pipelineCount?: number
  pac?: boolean
}

export interface AddToFlowGroupParams {
  pipelineIds: string[]
  viewIds: string[]
}

export interface TemplateObject {
  name?: string
  templateId?: string
  projectId?: string
  version?: number
  versionName?: string
  templateType?: string
  templateTypeDesc?: string
  category?: string[]
  logoUrl?: string
  stages?: Stage[]
  cloneTemplateSettingExist?: {
    notifySettingExist: boolean
    concurrencySettingExist: boolean
    labelSettingExist: boolean
    inheritedDialect: boolean
    pipelineDialect: string
  }
  desc?: string
  srcTemplateId?: string
}

export interface AllTemplatesResponse {
  count: number
  page: number
  pageSize: number
  templates: Record<string, TemplateObject>
}

export interface CreateContentResponse {
  pipelineId: string
  pipelineName: string
  version: number
  versionNum?: number
  versionName?: string
  targetUrl?: string
  pullRequestId?: number
  yamlInfo?: {
    repoHashId?: string
    scmType?: string
    filePath?: string
    pathWithNamespace?: string
    webUrl?: string
    fileUrl?: string
    status?: string
  }
  updateBuildNo?: boolean
}

export interface GetStoreTemplatesParams {
  page: number
  pageSize: number
  projectCode: string
  keyword: string
}

export interface StoreTemplateItem {
  id: string
  name: string
  code: string
  srcProjectId: string
  version: string
  status: string
  type: string
  desc?: string
  rdType?: string
  classifyCode: string
  category?: string
  logoUrl?: string
  publisher: string
  os?: string[]
  downloads?: number
  score?: number
  summary?: string
  flag: boolean
  publicFlag: boolean
  buildLessRunFlag?: boolean
  docsLink?: string
  modifier: string
  updateTime: string
  recommendFlag?: boolean
  yamlFlag?: boolean
  installed?: boolean
  honorInfos?: Array<{
    honorId: string
    honorTitle: string
    honorName: string
  }>
  indexInfos?: Array<{
    indexCode: string
    iconUrl: string
    iconColor: string
    hover: string
  }>
  recentExecuteNum?: number
  hotFlag?: boolean
  updateFlag?: boolean
  ownerStoreCode?: string
  ownerStoreName?: string
  pipelineCnt?: number
}

export interface StoreTemplateResponse {
  count: number
  page: number
  pageSize: number
  records: StoreTemplateItem[]
}

export interface PluginPropertyItem {
  atomCode: string
  os: string[]
  logoUrl: string
  buildLessRunFlag: boolean
}

export interface PluginProperty {
  [key: string]: PluginPropertyItem
}

/**
 * 获取创作流内容表格数据
 */
export async function getContentTableData(
  params: ContentTableParams,
): Promise<ContentTableResponse> {
  const { projectId, ...query } = params
  try {
    const res = await get<ContentTableResponse>(
      `${PROCESS_API_URL_PREFIX}/user/pipelines/projects/${projectId}/listViewPipelines`,
      {
        params: query,
      },
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 删除创作流
 */
export async function deleteContent(params: DeleteContentParams): Promise<Record<string, boolean>> {
  try {
    const res = await del<Record<string, boolean>>(
      `${PROCESS_API_URL_PREFIX}/user/pipelines/batchDelete`,
      { data: params },
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 禁用创作流
 */
export async function disableContent(params: {
  pipelineId: string
  projectId: string
  enable: boolean
}): Promise<boolean> {
  try {
    const { pipelineId, projectId, enable } = params
    const res = await post<boolean>(
      `${PROCESS_API_URL_PREFIX}/user/pipelines/projects/${projectId}/pipelines/${pipelineId}/lock?enable=${enable}`,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 复制创作流
 */
export async function copyContent(params: CopyFlowParams): Promise<ContentTableItem> {
  try {
    const { projectId, pipelineId, ...otherParams } = params
    const res = await post<ContentTableItem>(
      `${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}/copy`,
      otherParams,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 另存为模板
 */
export async function saveAsTemplate(
  projectId: string,
  params: SaveAsTemplateParams,
): Promise<{ id: string }> {
  try {
    const res = await post<{ id: string }>(
      `${PROCESS_API_URL_PREFIX}/user/templates/projects/${projectId}/templates/saveAsTemplate`,
      params,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 添加至创作流组
 */
export async function addToFlowGroup(
  projectId: string,
  params: AddToFlowGroupParams,
): Promise<boolean> {
  try {
    const res = await post<boolean>(
      `${PROCESS_API_URL_PREFIX}/user/pipelineViews/projects/${projectId}/bulkAdd`,
      params,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 新建创作流
 */
export async function createContent(params: CreateContentParams): Promise<CreateContentResponse> {
  try {
    const { projectId, ...otherParams } = params
    const response = await post<CreateContentResponse>(
      `${PROCESS_API_URL_PREFIX}/user/version/projects/${projectId}/createPipelineWithTemplate`,
      otherParams,
    )
    return response
  } catch (error) {
    throw error
  }
}

/**
 * model与yaml转换
 */
export async function apiTransfer(
  importParams: ImportContentParams,
): Promise<ImportContentResponse> {
  try {
    const { projectId, pipelineId, actionType, ...params } = importParams
    const res = await post<ImportContentResponse>(
      `${PROCESS_API_URL_PREFIX}/user/transfer/projects/${projectId}`,
      params,
      {
        params: {
          pipelineId,
          actionType,
        },
      },
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 获取流水线下插件属性列表
 */
export async function getPluginProperties(
  importParams: ImportContentParams,
): Promise<PluginProperty> {
  try {
    const { projectId, pipelineId, actionType, ...params } = importParams
    const res = await get<PluginProperty>(
      `/${PROCESS_API_URL_PREFIX}/user/pipeline/projects/${projectId}/pipelines/${pipelineId}/atom/prop/list`,
      {
        params: params.version ? { version: params.version } : {},
      },
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 获取内容详情
 */
export async function getContentDetail(id: string): Promise<ContentTableItem> {
  // TODO: 调用实际接口
  // const response = await http.get(`/api/flow/content/${id}`);
  // return response.data;

  // 模拟数据
  return new Promise((resolve) => {
    setTimeout(() => {
      const mockData: ContentTableItem = {
        projectId: 'fayetest',
        pipelineId: 'p-68ae025a1a354136948596ab3d09073f',
        pipelineName: '0420-4',
        pipelineDesc: '',
        taskCount: 3,
        buildCount: 0,
        lock: false,
        canManualStartup: true,
        latestBuildStartTime: 0,
        latestBuildEndTime: 0,
        latestBuildNum: 0,
        latestBuildEstimatedExecutionSeconds: 1,
        deploymentTime: 1618922545000,
        createTime: 1618922545000,
        updateTime: 1618922545000,
        pipelineVersion: 1,
        currentTimestamp: 1766136363205,
        runningBuildCount: 0,
        hasPermission: true,
        hasCollect: false,
        latestBuildUserId: '',
        instanceFromTemplate: false,
        updater: 'fayewang',
        creator: 'fayewang',
        lastBuildTotalCount: 0,
        lastBuildFinishCount: 0,
        delete: false,
        latestVersionStatus: 'RELEASED',
        permissions: {
          canManage: true,
          canDelete: true,
          canView: true,
          canEdit: true,
          canExecute: true,
          canDownload: true,
          canShare: true,
          canArchive: true,
        },
        yamlExist: false,
        archivingFlag: false,
      }
      resolve(mockData)
    }, 300)
  })
}

/**
 * 获取动态流水线组数据
 */
export async function getMatchDynamicView(
  projectId: string,
  params: MatchDynamicViewParams,
): Promise<string[]> {
  try {
    const res = await post<string[]>(
      `${PROCESS_API_URL_PREFIX}/user/pipelineViews/projects/${projectId}/matchDynamicView`,
      params,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 获取已选中的tree数据
 */
export async function getSelectedTreeData(
  projectId: string,
  pipelineId: string,
): Promise<SelectedTreeDataResponse[]> {
  try {
    const res = await get<SelectedTreeDataResponse[]>(
      `${PROCESS_API_URL_PREFIX}/user/pipelineViews/projects/${projectId}/pipelines/${pipelineId}/listViews`,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 获取创作环境列表
 */
export async function apiGetAuthoringEnvList(params: {
  projectId: string
  envType: string
}): Promise<AuthoringEnvItem[]> {
  try {
    const res = await get<AuthoringEnvItem[]>(
      `${ENVIRONMENT_API_URL_PREFIX}/user/environment/${params.projectId}?envType=${params.envType}`,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 获取全部项目模板
 */
export async function apiGetProjectTemplates(projectId: string): Promise<AllTemplatesResponse> {
  try {
    const res = await get<AllTemplatesResponse>(
      `${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/allTemplates`,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 获取默认配置
 */
export async function apiGetDefaultSetting(): Promise<FlowSettings> {
  try {
    const res = await get<FlowSettings>(`${PROCESS_API_URL_PREFIX}/user/setting/default/get`)
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 获取商店模板列表
 */
export async function apiGetStoreTemplates(
  params: GetStoreTemplatesParams,
): Promise<StoreTemplateResponse> {
  try {
    const res = await get<StoreTemplateResponse>(
      `${STORE_API_URL_PREFIX}/user/market/template/list`,
      { params },
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 收藏/取消收藏创作流
 * @param projectId 项目ID
 * @param pipelineId 创作流ID
 * @param type 是否收藏（true: 收藏, false: 取消收藏）
 */
export async function toggleFlowFavorite(
  projectId: string,
  pipelineId: string,
  type: boolean,
): Promise<boolean> {
  try {
    const res = await put<boolean>(
      `${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}/favor?type=${type}`,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 重命名创作流
 * @param projectId 项目ID
 * @param pipelineId 创作流ID
 * @param name 新名称
 */
export async function renameFlow(
  projectId: string,
  pipelineId: string,
  name: string,
): Promise<boolean> {
  try {
    const res = await post<boolean>(
      `${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}/rename`,
      { name },
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 导出创作流 JSON，参考 devops-pipeline 的 download action 实现
 * @param projectId 项目ID
 * @param pipelineId 创作流ID
 * @param fileName 下载文件名
 */
export async function downloadFlowJson(projectId: string, pipelineId: string, fileName: string): Promise<void> {
  const blob = await get<Blob>(
    `${PROCESS_API_URL_PREFIX}/user/pipelines/${pipelineId}/projects/${projectId}/export`,
    {
      responseType: 'blob',
      headers: { Accept: '*/*' },
    },
  )
  const blobUrl = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = blobUrl
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(blobUrl)
}

/**
 * 导出创作流为 YAML - 获取 YAML 内容
 * @param projectId 项目ID
 * @param pipelineId 创作流ID
 * @param modelAndSetting 模型和设置
 */
export async function exportFlowAsYaml(
  projectId: string,
  pipelineId: string,
  modelAndSetting: ModelAndSetting,
): Promise<ImportContentResponse> {
  try {
    const res = await post<ImportContentResponse>(
      `${PROCESS_API_URL_PREFIX}/user/transfer/projects/${projectId}`,
      { modelAndSetting },
      {
        params: {
          pipelineId,
          actionType: 'FULL_MODEL2YAML',
        },
      },
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 简单流水线信息（用于下拉选择器）
 */
export interface SimplePipelineInfo {
  pipelineId: string
  pipelineName: string
}

/**
 * 按名称搜索创作流（简单列表，用于下拉选择器）
 * @param projectId 项目ID
 * @param pipelineName 创作流名称（搜索关键词）
 */
export async function searchFlowByName(
  projectId: string,
  pipelineName: string = '',
): Promise<SimplePipelineInfo[]> {
  try {
    const params = new URLSearchParams()
    params.append('pipelineName', pipelineName)

    const res = await get<SimplePipelineInfo[]>(
      `${PROCESS_API_URL_PREFIX}/user/pipelineInfos/${projectId}/searchByName?${params.toString()}`,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 获取创作流回收站表格数据
 */
export async function getRecycleTableData(
  params: ContentTableParams,
): Promise<ContentTableResponse> {
  const { projectId, ...query } = params
  try {
    const res = await get<ContentTableResponse>(
      `${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/pipelineRecycleList`,
      {
        params: query,
      },
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 恢复创作流
 */
export async function restoreContent(params: {
  pipelineId: string
  projectId: string
}): Promise<boolean> {
  try {
    const { pipelineId, projectId } = params
    const res = await put<boolean>(
      `${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}/restore`,
    )
    return res
  } catch (error) {
    throw error
  }
}
