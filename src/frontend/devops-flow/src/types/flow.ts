/**
 * Flow Model Type Definitions
 * Unified type definitions for flow orchestration system
 */

import type { PluginOutputVariable } from '@/types/variable'

// ============================================
// Status Constants and Types
// ============================================

/**
 * 状态常量定义
 * 统一管理所有状态值，避免 Magic string
 */
export const STATUS = {
  // 成功状态
  SUCCEED: 'SUCCEED',
  STAGE_SUCCESS: 'STAGE_SUCCESS',
  REVIEW_PROCESSED: 'REVIEW_PROCESSED',

  // 失败状态
  FAILED: 'FAILED',
  TERMINATE: 'TERMINATE',
  HEARTBEAT_TIMEOUT: 'HEARTBEAT_TIMEOUT',
  QUALITY_CHECK_FAIL: 'QUALITY_CHECK_FAIL',
  QUEUE_TIMEOUT: 'QUEUE_TIMEOUT',
  EXEC_TIMEOUT: 'EXEC_TIMEOUT',
  QUOTA_FAILED: 'QUOTA_FAILED',

  // 取消/警告状态
  CANCELED: 'CANCELED',
  REVIEW_ABORT: 'REVIEW_ABORT',

  // 运行中状态
  RUNNING: 'RUNNING',
  QUEUE: 'QUEUE',
  PREPARE_ENV: 'PREPARE_ENV',
  REVIEWING: 'REVIEWING',
  LOOP_WAITING: 'LOOP_WAITING',
  CALL_WAITING: 'CALL_WAITING',
  QUEUE_CACHE: 'QUEUE_CACHE',
  RETRY: 'RETRY',
  PAUSE: 'PAUSE',
  DEPENDENT_WAITING: 'DEPENDENT_WAITING',
  QUALITY_CHECK_PASS: 'QUALITY_CHECK_PASS',
  QUALITY_CHECK_WAIT: 'QUALITY_CHECK_WAIT',

  // 其他状态
  UNEXEC: 'UNEXEC',
  SKIP: 'SKIP',
  TRY_FINALLY: 'TRY_FINALLY',
  UNKNOWN: 'UNKNOWN',
} as const

/**
 * 状态类型定义
 * 基于 STATUS 常量生成类型
 */
export type StatusType = (typeof STATUS)[keyof typeof STATUS]

// ============================================
// Enums
// ============================================

export enum RunLockType {
  MULTIPLE = 'MULTIPLE',
  GROUP_LOCK = 'GROUP_LOCK',
}

export enum BuildCancelPolicy {
  EXECUTE_PERMISSION = 'EXECUTE_PERMISSION',
  RESTRICTED = 'RESTRICTED',
}

// ============================================
// Common Types
// ============================================

/**
 * 自定义变量
 */
export interface CustomVariable {
  key: string
  value: string
}

/**
 * 时间成本
 */
export interface TimeCost {
  systemCost: number // 系统耗时（毫秒）
  executeCost: number // 执行耗时（毫秒）
  waitCost: number // 等待耗时（毫秒）
  queueCost: number // 排队耗时（毫秒）
  totalCost: number // 总耗时（毫秒）
}

// ============================================
// Element (Plugin/Atom) Related Types
// ============================================

/**
 * 附加选项 - 元素的执行控制配置
 */
export interface AdditionalOptions {
  enable: boolean
  continueWhenFailed: boolean
  manualSkip: boolean
  retryWhenFailed: boolean
  retryCount: number
  manualRetry: boolean
  timeout: number
  timeoutVar: string
  runCondition: string
  pauseBeforeExec: boolean
  subscriptionPauseUser: string
  otherTask: string
  customVariables: CustomVariable[]
  customCondition: string
  enableCustomEnv: boolean
  failControl?: string[] // 失败控制选项数组
}

/**
 * 插件元素 (Element/Atom/Plugin)
 * 统一的元素类型定义，包含编排配置和运行时状态
 */
export interface Element {
  '@type'?: string
  name: string
  id: string
  stepId?: string
  scriptType?: string
  script?: string
  continueNoneZero?: boolean
  enableArchiveFile?: boolean
  archiveFile?: string
  additionalOptions?: AdditionalOptions
  customEnv?: CustomVariable[] // 自定义环境变量
  executeCount?: number
  version?: string
  classType?: string
  atomCode?: string
  taskAtom?: string
  ownerStoreCode?: string
  canElementSkip?: boolean
  useLatestParameters?: boolean
  data?: {
    input: Record<string, unknown>
    output: PluginOutputVariable[]
  }
  // Runtime status fields
  status?: StatusType | ''
  [key: string]: unknown
}

// ============================================
// Container (Job) Related Types
// ============================================

/**
 * 调度类型配置
 */
export interface DispatchType {
  buildType: string
  value: string
  performanceUid?: string
  persistence?: boolean
  imageType?: string
  credentialId?: string
  credentialProject?: string
  imageCode?: string
  imageVersion?: string
  imageName?: string
  dockerBuildVersion?: string
  imagePublicFlag?: boolean
  imageRDType?: string
  recommendFlag?: boolean
}

/**
 * Job 控制选项
 */
export interface JobControlOption {
  enable: boolean
  prepareTimeout?: number
  timeout: number
  timeoutVar: string
  runCondition: string
  customVariables: CustomVariable[]
  customCondition: string
  dependOnType: string
  dependOnId: string[]
  dependOnName: string
  continueWhenFailed: boolean
}

/**
 * 矩阵 Job 控制选项
 */
export interface MatrixControlOption {
  strategyStr: string
  includeCaseStr: string
  excludeCaseStr: string
  fastKill: boolean
  maxConcurrency: number
}

/**
 * 互斥组配置
 */
export interface MutexGroup {
  enable: boolean
  mutexGroupName: string
  queueEnable: boolean
  timeoutVar: string
  queue: number
}

/**
 * 参数定义
 */
export interface Param {
  id: string
  name: string
  required: boolean
  constant: boolean
  type: string
  defaultValue: unknown
  options?: unknown[]
  desc?: string
  displayCondition?: Record<string, string>
  readOnly?: boolean
  valueNotEmpty?: boolean
  category?: string // User-defined group name for categorization
  order?: number // Order for sorting variables
  payload?: {
    [key: string]: unknown
  }
}

/**
 * 容器 (Container/Job)
 * 统一的容器类型定义，包含编排配置和运行时状态
 */
export interface Container {
  '@type': string
  id: string
  name: string
  elements: Element[]
  containerId: string
  containerHashId: string
  matrixGroupFlag?: boolean
  classType?: string
  jobId?: string
  // Build environment config
  baseOS?: string
  vmNames?: string[]
  maxQueueMinutes?: number
  maxRunningMinutes?: number
  buildEnv?: Record<string, string>
  dispatchType?: DispatchType
  showBuildResource?: boolean
  enableExternal?: boolean
  // Control options
  jobControlOption?: JobControlOption
  matrixControlOption?: MatrixControlOption
  mutexGroup?: MutexGroup
  nfsSwitch?: boolean
  params?: Param[]
  // Runtime status fields
  status?: StatusType
  startEpoch?: number
  systemElapsed?: number
  elementElapsed?: number
  canRetry?: boolean
  executeCount?: number
  timeCost?: TimeCost
  runContainer?: boolean // Preview skip state
  groupContainers?: Container[] // For matrix job group containers
  [key: string]: unknown
}

// ============================================
// Stage Related Types
// ============================================

/**
 * Stage 控制选项
 */
export interface StageControlOption {
  enable: boolean
  runCondition: string
  customVariables: CustomVariable[]
  customCondition: string
  timeout?: number
}

/**
 * 审核组
 */
export interface ReviewGroup {
  id?: string
  name: string
  reviewers: string[]
  groups?: string[]
  operator?: string
  suggest?: string
  status?: 'PROCESS' | 'ABORT' | undefined
  params?: Record<string, unknown>[]
}

/**
 * 检查配置 (checkIn/checkOut)
 */
export interface CheckConfig {
  manualTrigger: boolean
  timeout: number
  markdownContent: boolean
  notifyType: string[]
  reviewGroups?: ReviewGroup[]
  reviewDesc?: string
  reviewParams?: Record<string, unknown>[]
  notifyGroup?: string[]
  status?: string
  isReviewError?: boolean
}

/**
 * Stage 阶段
 * 统一的阶段类型定义，包含编排配置和运行时状态
 */
export interface Stage {
  containers: Container[]
  id: string
  name: string
  tag?: string[]
  fastKill?: boolean
  finally?: boolean
  // Control options
  stageControlOption?: StageControlOption
  checkIn?: CheckConfig
  checkOut?: CheckConfig
  // Runtime status fields
  status?: StatusType
  elapsed?: number
  canRetry?: boolean
  executeCount?: number
  timeCost?: TimeCost
  startEpoch?: number
  runStage?: boolean // Preview skip state
  [key: string]: unknown
}

/**
 * Stage 状态信息 - 用于状态显示
 */
export interface StageStatusInfo {
  stageId: string
  name: string
  status: StatusType
  startEpoch?: number
  elapsed?: number
  tag?: string[]
  timeCost?: TimeCost
  showMsg?: string
}

// ============================================
// Flow Model and Settings
// ============================================

/**
 * 创作流模型
 */
export interface FlowModel {
  '@type'?: string
  name: string
  desc?: string
  stages: Stage[]
  labels?: string[]
  instanceFromTemplate?: boolean
  creator?: string
  events?: Record<string, unknown>
  staticViews?: unknown[]
  timeCost?: TimeCost
  latestVersion?: number
  [key: string]: unknown
}

/**
 * 订阅配置
 */
export interface Subscription {
  types: string[]
  groups: string[]
  users: string
  wechatGroupFlag: boolean
  wechatGroup: string
  wechatGroupMarkdownFlag: boolean
  content: string
}

/**
 * 创作流设置
 */
export interface FlowSettings {
  creator?: string
  createTime?: number
  updateTime?: number
  pipelineName: string
  desc: string
  // Cancel subscription list
  cancelSubscriptionList?: Subscription[]
  // Publish subscription list
  publishSubscriptionList?: Subscription[]
  projectId?: string
  pipelineId?: string
  version?: number
  labels?: string[]
  labelNames?: string[]
  buildNumRule?: string
  successSubscription?: Subscription
  failSubscription?: Subscription
  successSubscriptionList?: Subscription[]
  failSubscriptionList?: Subscription[]
  runLockType?: string
  waitQueueTimeMinute?: number
  maxQueueSize?: number
  concurrencyGroup?: string
  concurrencyCancelInProgress?: boolean
  maxConRunningQueueSize?: number
  failIfVariableInvalid?: boolean
  buildCancelPolicy?: 'EXECUTE_PERMISSION' | 'RESTRICTED'
  maxPipelineResNum?: number
  cleanVariablesWhenRetry?: boolean
  pipelineAsCodeSettings?: {
    enable: boolean
    projectDialect: string
    inheritedDialect: boolean
    pipelineDialect: string
  }
  updater?: string
  createdTime?: number
  envHashId?: string
}

// ============================================
// Execution Related Types
// ============================================

/**
 * 执行记录
 */
export interface ExecutionRecord {
  startUser: string
  timeCost: TimeCost
}

/**
 * 质量红线
 */
export interface ArtifactQuality {
  [key: string]: any
}

/**
 * TemplateInfo
 */
export interface TemplateInfo {
  templateId: string
  templateName: string
  version: number
  versionName: string
  instanceType: 'FREEDOM' | 'CONSTRAINT'
  desc: string
}

/**
 * 执行详情数据
 */
export interface ExecuteDetailData {
  id: string // 构建ID
  pipelineId: string // 流水线ID
  pipelineName: string // 流水线名称
  userId: string // 用户ID
  triggerUser: string // 触发用户
  trigger: string // 触发方式
  queueTime: number // 排队时间戳
  startTime: number // 开始时间戳
  queueTimeCost: number // 排队耗时
  endTime?: number // 结束时间戳
  status: StatusType // 构建状态
  model: FlowModel // 创作流模型
  currentTimestamp: number // 当前时间戳
  buildNum: number // 构建次数
  curVersion: number // 当前版本号
  curVersionName: string // 当前版本名称
  latestVersion: number // 最新版本号
  latestBuildNum: number // 最新构建次数
  lastModifyUser: string // 最后修改用户
  executeTime: number // 执行时间
  stageStatus: StageStatusInfo[] // Stage状态列表
  executeCount: number // 执行次数
  startUserList: string[] // 启动用户列表
  recordList: ExecutionRecord[] // 执行记录列表
  buildMsg: string // 构建消息
  debug: boolean // 是否调试模式
  artifactQuality?: ArtifactQuality // 制品质量信息
  versionChange: boolean // 版本是否变更
  webhookInfo?: Record<string, unknown> // Webhook信息
  materials?: Record<string, unknown>[] // 材料列表
  cancelBuildPerm: boolean // 是否有取消构建权限
  errorInfoList: Record<string, unknown>[] // 错误信息列表
  templateInfo?: TemplateInfo // 模板信息
  [key: string]: unknown
}

// ============================================
// Version and Permission Types
// ============================================

/**
 * 版本状态类型
 */
export type VersionStatus =
  | 'RELEASED'
  | 'COMMITTING'
  | 'BRANCH'
  | 'BRANCH_RELEASE'
  | 'DRAFT_RELEASE'
  | 'DELETE'
  | 'HIDDEN'

/**
 * 权限信息
 */
export interface FlowPermissions {
  canManage: boolean // 是否可管理
  canDelete: boolean // 是否可删除
  canView: boolean // 是否可查看
  canEdit: boolean // 是否可编辑
  canExecute: boolean // 是否可执行
  canDownload: boolean // 是否可下载
  canShare: boolean // 是否可分享
  canArchive: boolean // 是否可归档
}

/**
 * 创作流基本信息
 */
export interface FlowInfo {
  pipelineId: string // 流水线ID
  pipelineName: string // 流水线名称
  version: number // 版本号
  versionNum: number // 版本数量
  versionName: string // 版本名称
  pipelineVersion: number // 流水线版本
  pipelineCreator: string // 创建者
  createTime: number // 创建时间
  updateTime: number // 更新时间
  versionStatus: VersionStatus // 版本状态
  latestVersionStatus: VersionStatus // 最新版本状态
  releaseVersion: number // 发布版本
  releaseVersionName: string // 发布版本名称
  baseVersion: number // 基础版本
  baseVersionName: string // 基础版本名称
  locked: boolean // 是否锁定
  permissions: FlowPermissions // 权限信息
  hasCollect: boolean // 是否收藏
  canManualStartup: boolean // 是否可手动启动
  canDebug: boolean // 是否可调试
  instanceFromTemplate: boolean // 是否从模板创建
  yamlExist: boolean // 是否存在YAML配置
  latestVersion: number // 最新版本
  latestBuildNum: number // 最新构建次数
  buildCancelPolicy: BuildCancelPolicy // 取消构建策略
  description?: string
  [key: string]: unknown
}

/**
 * 版本列表项
 */
export interface FlowVersion {
  version: number // 版本号
  versionName: string // 版本名称
  creator: string // 创建者
  createTime: number // 创建时间
  versionStatus: VersionStatus // 版本状态
  isLatest: boolean // 是否最新版本
  [key: string]: unknown
}

export interface ModelAndSetting {
  model: FlowModel
  setting: FlowSettings
}
