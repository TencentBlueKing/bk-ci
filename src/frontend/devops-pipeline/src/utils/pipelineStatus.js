export const statusIconMap = {
    SUCCEED: 'check-circle', // 0 成功（最终态）
    FAILED: 'close-circle', // 1 失败（最终态）
    CANCELED: 'abort', // 2 取消（最终态）
    RUNNING: 'circle-2-1', // 3 运行中（中间状态）
    TERMINATE: 'abort', // 4 终止（Task最终态）待作废
    REVIEWING: 'reviewing', // 5 审核中（Task中间状态）
    REVIEW_ABORT: 'review-abort', // 6 审核驳回（Task最终态）
    REVIEW_PROCESSED: 'reviewed', // 7 审核通过（Task最终态）
    HEARTBEAT_TIMEOUT: 'abort', // 8 心跳超时（最终态）
    PREPARE_ENV: 'circle-2-1', // 9 准备环境中（中间状态）
    UNEXEC: 'circle', // 10 从未执行（最终态）
    SKIP: 'redo-arrow', // 11 跳过（最终态）
    QUALITY_CHECK_FAIL: 'close-circle', // 12 质量红线检查失败（最终态）
    QUEUE: 'circle-2-1', // 13 排队（初始状态）
    LOOP_WAITING: 'circle-2-1', // 14 轮循等待中 互斥组抢锁轮循 （中间状态）
    CALL_WAITING: 'circle-2-1', // 15 等待回调 用于启动构建环境插件等待构建机回调启动结果（中间状态）
    TRY_FINALLY: 'circle-2-1', // 16 不可见的后台状态（未使用）
    QUEUE_TIMEOUT: 'abort', // 17 排队超时（最终态）
    EXEC_TIMEOUT: 'abort', // 18 执行超时（最终态）
    QUEUE_CACHE: 'circle-2-1', // 19 队列待处理，瞬态。只在启动和取消过程中存在（中间状态）
    RETRY: 'retry', // 20 重试（中间状态）
    PAUSE: 'play-circle-shape', // 21 暂停执行，等待事件 （Stage/Job/Task中间态）
    STAGE_SUCCESS: 'flag', // 22 当Stage人工审核取消运行时，成功（Stage/Pipeline最终态）
    QUOTA_FAILED: 'close-circle', // 23 失败 (未使用）
    DEPENDENT_WAITING: 'circle-2-1', // 24 依赖等待 等待依赖的job完成才会进入准备环境（Job中间态）
    QUALITY_CHECK_PASS: 'circle-2-1', // 25 质量红线检查通过
    QUALITY_CHECK_WAIT: 'circle-2-1', // 26 质量红线等待把关
    UNKNOWN: 'placeholder' // 99
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
    UNKNOWN: 'UNKNOWN' // 99
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
    LOOP_WAITING: '#699DF4', // 14 轮循等待中 互斥组抢锁轮循 （中间状态）
    CALL_WAITING: '#699DF4', // 15 等待回调 用于启动构建环境插件等待构建机回调启动结果（中间状态）
    TRY_FINALLY: '#699DF4', // 16 不可见的后台状态（未使用）
    QUEUE_TIMEOUT: '#FF9C01', // 17 排队超时（最终态）
    EXEC_TIMEOUT: '#FF9C01', // 18 执行超时（最终态）
    QUEUE_CACHE: '#699DF4', // 19 队列待处理，瞬态。只在启动和取消过程中存在（中间状态）
    RETRY: '#EA3636', // 20 重试（中间状态）
    PAUSE: '#FF9C01', // 21 暂停执行，等待事件 （Stage/Job/Task中间态）
    STAGE_SUCCESS: '#2DCB56', // 22 当Stage人工审核取消运行时，成功（Stage/Pipeline最终态）
    QUOTA_FAILED: '#EA3636', // 23 失败 (未使用）
    DEPENDENT_WAITING: '#699DF4', // 24 依赖等待 等待依赖的job完成才会进入准备环境（Job中间态）
    QUALITY_CHECK_PASS: '#699DF4', // 25 质量红线检查通过
    QUALITY_CHECK_WAIT: '#699DF4', // 26 质量红线等待把关
    UNKNOWN: '#699DF4' // 99
}

export function mapThemeOfStatus (status) {
    switch (status) {
        case 'CANCELED':
        case 'REVIEW_ABORT':
            return 'warning'
        case 'SUCCEED':
        case 'REVIEW_PROCESSED':
        case 'STAGE_SUCCESS':
            return 'success'
        case 'FAILED':
        case 'TERMINATE':
        case 'HEARTBEAT_TIMEOUT':
        case 'QUALITY_CHECK_FAIL':
        case 'QUEUE_TIMEOUT':
        case 'EXEC_TIMEOUT':
            return 'danger'
        case 'QUEUE':
        case 'RUNNING':
        case 'REVIEWING':
        case 'PREPARE_ENV':
        case 'LOOP_WAITING':
        case 'CALL_WAITING':
            return 'info'
        default:
            return ''
    }
}
