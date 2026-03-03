import { STATUS, type StatusType } from '@/types/flow'

/**
 * 状态图标映射
 */
export const statusIconMap: Record<StatusType, string> = {
  [STATUS.SUCCEED]: 'check-circle', // 0 成功（最终态）
  [STATUS.FAILED]: 'close-circle', // 1 失败（最终态）
  [STATUS.CANCELED]: 'abort', // 2 取消（最终态）
  [STATUS.RUNNING]: 'circle-2-1', // 3 运行中（中间状态）
  [STATUS.TERMINATE]: 'abort', // 4 终止（Task最终态）待作废
  [STATUS.REVIEWING]: 'reviewing', // 5 审核中（Task中间状态）
  [STATUS.REVIEW_ABORT]: 'review-abort', // 6 审核驳回（Task最终态）
  [STATUS.REVIEW_PROCESSED]: 'reviewed', // 7 审核通过（Task最终态）
  [STATUS.HEARTBEAT_TIMEOUT]: 'abort', // 8 心跳超时（最终态）
  [STATUS.PREPARE_ENV]: 'circle-2-1', // 9 准备环境中（中间状态）
  [STATUS.UNEXEC]: 'circle', // 10 从未执行（最终态）
  [STATUS.SKIP]: 'redo-arrow', // 11 跳过（最终态）
  [STATUS.QUALITY_CHECK_FAIL]: 'close-circle', // 12 质量红线检查失败（最终态）
  [STATUS.QUEUE]: 'hourglass', // 13 排队（初始状态）
  [STATUS.QUEUE_CACHE]: 'hourglass', // 19 队列待处理，瞬态。只在启动和取消过程中存在（中间状态）
  [STATUS.LOOP_WAITING]: 'circle-2-1', // 14 轮循等待中 互斥组抢锁轮循 （中间状态）
  [STATUS.CALL_WAITING]: 'circle-2-1', // 15 等待回调 用于启动构建环境插件等待构建机回调启动结果（中间状态）
  [STATUS.TRY_FINALLY]: 'circle-2-1', // 16 不可见的后台状态（未使用）
  [STATUS.QUEUE_TIMEOUT]: 'abort', // 17 排队超时（最终态）
  [STATUS.EXEC_TIMEOUT]: 'abort', // 18 执行超时（最终态）
  [STATUS.RETRY]: 'retry', // 20 重试（中间状态）
  [STATUS.PAUSE]: 'play-circle-shape', // 21 暂停执行，等待事件 （Stage/Job/Task中间态）
  [STATUS.STAGE_SUCCESS]: 'flag', // 22 当Stage人工审核取消运行时，成功（Stage/Pipeline最终态）
  [STATUS.QUOTA_FAILED]: 'close-circle', // 23 失败 (未使用）
  [STATUS.DEPENDENT_WAITING]: 'circle-2-1', // 24 依赖等待 等待依赖的job完成才会进入准备环境（Job中间态）
  [STATUS.QUALITY_CHECK_PASS]: 'circle-2-1', // 25 质量红线检查通过
  [STATUS.QUALITY_CHECK_WAIT]: 'circle-2-1', // 26 质量红线等待把关
  [STATUS.UNKNOWN]: 'circle', // 99
}

export const statusAlias = {
  SUCCEED: 'SUCCEED', // 0 成功（最终态）
  FAILED: 'FAILED', // 1 失败（最终态）
  CANCELED: 'CANCELED', // 2 取消（最终态）
  RUNNING: 'RUNNING', // 3 运行中（中间状态）
  TERMINATE: 'TERMINATE', // 4 终止（Task最终态）待作废
  REVIEWING: 'REVIEWING', // 5 审核中（Task中间状态）
  REVIEW_ABORT: 'REVIEW_ABORT', // 6 审核驳回（Task最终态）
  REVIEW_PROCESSED: 'REVIEW_PROCESSED', // 7 审核通过（Task最终态）
  HEARTBEAT_TIMEOUT: 'HEARTBEAT_TIMEOUT', // 8 心跳超时（最终态）
  PREPARE_ENV: 'PREPARE_ENV', // 9 准备环境中（中间状态）
  UNEXEC: 'UNEXEC', // 10 从未执行（最终态）
  SKIP: 'SKIP', // 11 跳过（最终态）
  QUALITY_CHECK_FAIL: 'QUALITY_CHECK_FAIL', // 12 质量红线检查失败（最终态）
  QUEUE: 'QUEUE', // 13 排队（初始状态）
  LOOP_WAITING: 'LOOP_WAITING', // 14 轮循等待中 互斥组抢锁轮循 （中间状态）
  CALL_WAITING: 'CALL_WAITING', // 15 等待回调 用于启动构建环境插件等待构建机回调启动结果（中间状态）
  TRY_FINALLY: 'TRY_FINALLY', // 16 不可见的后台状态（未使用）
  QUEUE_TIMEOUT: 'QUEUE_TIMEOUT', // 17 排队超时（最终态）
  EXEC_TIMEOUT: 'EXEC_TIMEOUT', // 18 执行超时（最终态）
  QUEUE_CACHE: 'QUEUE_CACHE', // 19 队列待处理，瞬态。只在启动和取消过程中存在（中间状态）
  RETRY: 'RETRY', // 20 重试（中间状态）
  PAUSE: 'PAUSE', // 21 暂停执行，等待事件 （Stage/Job/Task中间态）
  STAGE_SUCCESS: 'STAGE_SUCCESS', // 22 当Stage人工审核取消运行时，成功（Stage/Pipeline最终态）
  QUOTA_FAILED: 'QUOTA_FAILED', // 23 失败 (未使用）
  DEPENDENT_WAITING: 'DEPENDENT_WAITING', // 24 依赖等待 等待依赖的job完成才会进入准备环境（Job中间态）
  QUALITY_CHECK_PASS: 'QUALITY_CHECK_PASS', // 25 质量红线检查通过
  QUALITY_CHECK_WAIT: 'QUALITY_CHECK_WAIT', // 26 质量红线等待把关
  UNKNOWN: 'UNKNOWN', // 99
}

export const statusColorMap = {
  SUCCEED: '#2DCB56', // 0 成功（最终态）
  FAILED: '#EA3636', // 1 失败（最终态）
  CANCELED: '#FF9C01', // 2 取消（最终态）
  RUNNING: '#699DF4', // 3 运行中（中间状态）
  TERMINATE: '#FF9C01', // 4 终止（Task最终态）待作废
  REVIEWING: '#699DF4', // 5 审核中（Task中间状态）
  REVIEW_ABORT: '#FF9C01', // 6 审核驳回（Task最终态）
  REVIEW_PROCESSED: '#699DF4', // 7 审核通过（Task最终态）
  HEARTBEAT_TIMEOUT: '#FF9C01', // 8 心跳超时（最终态）
  PREPARE_ENV: '#699DF4', // 9 准备环境中（中间状态）
  UNEXEC: '#DCDEE5', // 10 从未执行（最终态）
  SKIP: '#DCDEE5', // 11 跳过（最终态）
  QUALITY_CHECK_FAIL: '#EA3636', // 12 质量红线检查失败（最终态）
  QUEUE: '#699DF4', // 13 排队（初始状态）
  QUEUE_CACHE: '#699DF4', // 19 队列待处理，瞬态。只在启动和取消过程中存在（中间状态）
  LOOP_WAITING: '#699DF4', // 14 轮循等待中 互斥组抢锁轮循 （中间状态）
  CALL_WAITING: '#699DF4', // 15 等待回调 用于启动构建环境插件等待构建机回调启动结果（中间状态）
  TRY_FINALLY: '#699DF4', // 16 不可见的后台状态（未使用）
  QUEUE_TIMEOUT: '#FF9C01', // 17 排队超时（最终态）
  EXEC_TIMEOUT: '#FF9C01', // 18 执行超时（最终态）
  RETRY: '#EA3636', // 20 重试（中间状态）
  PAUSE: '#FF9C01', // 21 暂停执行，等待事件 （Stage/Job/Task中间态）
  STAGE_SUCCESS: '#2DCB56', // 22 当Stage人工审核取消运行时，成功（Stage/Pipeline最终态）
  QUOTA_FAILED: '#EA3636', // 23 失败 (未使用）
  DEPENDENT_WAITING: '#699DF4', // 24 依赖等待 等待依赖的job完成才会进入准备环境（Job中间态）
  QUALITY_CHECK_PASS: '#699DF4', // 25 质量红线检查通过
  QUALITY_CHECK_WAIT: '#699DF4', // 26 质量红线等待把关
  UNKNOWN: '#699DF4', // 99
}

/**
 * 将状态映射为主题色
 * @param status 状态类型
 * @returns 主题色名称
 */
export function mapThemeOfStatus(
  status: StatusType,
): 'danger' | 'success' | 'warning' | 'info' | '' {
  switch (status) {
    case STATUS.CANCELED:
    case STATUS.REVIEW_ABORT:
      return 'warning'
    case STATUS.SUCCEED:
    case STATUS.REVIEW_PROCESSED:
    case STATUS.STAGE_SUCCESS:
      return 'success'
    case STATUS.FAILED:
    case STATUS.TERMINATE:
    case STATUS.HEARTBEAT_TIMEOUT:
    case STATUS.QUALITY_CHECK_FAIL:
    case STATUS.QUEUE_TIMEOUT:
    case STATUS.EXEC_TIMEOUT:
      return 'danger'
    case STATUS.QUEUE:
    case STATUS.RUNNING:
    case STATUS.REVIEWING:
    case STATUS.PREPARE_ENV:
    case STATUS.LOOP_WAITING:
    case STATUS.CALL_WAITING:
      return 'info'
    default:
      return ''
  }
}

/**
 * 判断状态是否为跳过状态
 * @param status 状态类型
 * @returns 是否为跳过状态
 */
export function isSkip(status: string | undefined): boolean {
  return status === STATUS.SKIP
}

/**
 * 判断状态是否为运行中状态
 * @param status 状态类型
 * @returns 是否为运行中状态
 */
export function isRunning(status: StatusType | string | undefined): boolean {
  return status === STATUS.RUNNING || status === STATUS.QUEUE || status === STATUS.PREPARE_ENV
}
