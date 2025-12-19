export const statusIconMap = {
    SUCCEED: 'check-circle-color', // 0 成功（最终态）
    FAILED: 'close-circle-color', // 1 失败（最终态）
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
    QUALITY_CHECK_FAIL: 'close-circle-color', // 12 质量红线检查失败（最终态）
    QUEUE: 'hourglass', // 13 排队（初始状态）
    QUEUE_CACHE: 'hourglass', // 19 队列待处理，瞬态。只在启动和取消过程中存在（中间状态）
    LOOP_WAITING: 'circle-2-1', // 14 轮循等待中 互斥组抢锁轮循 （中间状态）
    CALL_WAITING: 'circle-2-1', // 15 等待回调 用于启动构建环境插件等待构建机回调启动结果（中间状态）
    TRY_FINALLY: 'circle-2-1', // 16 不可见的后台状态（未使用）
    QUEUE_TIMEOUT: 'abort', // 17 排队超时（最终态）
    EXEC_TIMEOUT: 'abort', // 18 执行超时（最终态）
    RETRY: 'retry', // 20 重试（中间状态）
    PAUSE: 'play-circle-shape', // 21 暂停执行，等待事件 （Stage/Job/Task中间态）
    STAGE_SUCCESS: 'flag', // 22 当Stage人工审核取消运行时，成功（Stage/Pipeline最终态）
    QUOTA_FAILED: 'close-circle-color', // 23 失败 (未使用）
    DEPENDENT_WAITING: 'circle-2-1', // 24 依赖等待 等待依赖的job完成才会进入准备环境（Job中间态）
    QUALITY_CHECK_PASS: 'circle-2-1', // 25 质量红线检查通过
    QUALITY_CHECK_WAIT: 'circle-2-1', // 26 质量红线等待把关
    UNKNOWN: 'placeholder' // 99
}
