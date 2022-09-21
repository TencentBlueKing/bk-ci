export function getPipelineStatusClass (status, isSkip = false) {
    const statusMap = {
        DEPENDENT_WAITING: 'waiting',
        WAITING: 'waiting',
        TRIGGER_REVIEWING: 'running',
        REVIEWING: 'running',
        CANCELED: 'canceled',
        REVIEW_ABORT: 'canceled',
        TRY_FINALLY: 'canceled',
        UNEXEC: 'skip',
        SKIP: 'skip',
        FAILED: 'danger',
        HEARTBEAT_TIMEOUT: 'danger',
        QUALITY_CHECK_FAIL: 'danger',
        QUEUE_TIMEOUT: 'danger',
        EXEC_TIMEOUT: 'danger',
        TERMINATE: 'danger',
        SUCCEED: 'success',
        REVIEW_PROCESSED: 'success',
        STAGE_SUCCESS: 'success',
        PAUSE: 'pause',
        RUNNING: 'running',
        PREPARE_ENV: 'running',
        QUEUE: 'running',
        LOOP_WAITING: 'running',
        CALL_WAITING: 'running',
        QUEUE_CACHE: 'running'
    }
    return isSkip ? 'skip' : statusMap[status]
}

export function getPipelineStatusShapeIconCls (status) {
    const iconName = 'bk-icon'
    const iconMap = {
        RUNNING: 'icon-circle-2-1 executing',
        PREPARE_ENV: 'icon-circle-2-1 executing',
        QUEUE: 'icon-circle-2-1 executing',
        LOOP_WAITING: 'icon-circle-2-1 executing',
        CALL_WAITING: 'icon-circle-2-1 executing',
        DEPENDENT_WAITING: 'icon-clock',
        WAITING: 'icon-clock',
        CANCELED: 'icon-exclamation-circle-shape',
        TERMINATE: 'icon-exclamation-circle-shape',
        TRIGGER_REVIEWING: 'icon-exclamation-triangle-shape',
        REVIEWING: 'icon-exclamation-triangle-shape',
        REVIEW_ABORT: 'icon-exclamation-triangle-shape',
        FAILED: 'icon-close-circle-shape',
        HEARTBEAT_TIMEOUT: 'icon-close-circle-shape',
        QUEUE_TIMEOUT: 'icon-close-circle-shape',
        EXEC_TIMEOUT: 'icon-close-circle-shape',
        SUCCEED: 'icon-check-circle-shape'
    }
    return [iconName, iconMap[status]]
}

export function getPipelineStatusCircleIconCls (status) {
    const iconMap = {
        RUNNING: 'bk-icon icon-circle-2-1 executing',
        PREPARE_ENV: 'bk-icon icon-circle-2-1 executing',
        LOOP_WAITING: 'bk-icon icon-circle-2-1 executing',
        CALL_WAITING: 'bk-icon icon-circle-2-1 executing',
        DEPENDENT_WAITING: 'bk-icon icon-clock',
        WAITING: 'bk-icon icon-clock',
        CANCELED: 'bk-icon icon-exclamation',
        TERMINATE: 'bk-icon icon-exclamation',
        TRIGGER_REVIEWING: 'stream-icon stream-reviewing-2',
        REVIEWING: 'stream-icon stream-reviewing-2',
        REVIEW_ABORT: 'bk-icon icon-exclamation-triangle',
        FAILED: 'bk-icon icon-close',
        HEARTBEAT_TIMEOUT: 'bk-icon icon-close',
        QUEUE_TIMEOUT: 'bk-icon icon-close',
        EXEC_TIMEOUT: 'bk-icon icon-close',
        SUCCEED: 'bk-icon icon-check-1',
        REVIEW_PROCESSED: 'stream-icon stream-flag',
        STAGE_SUCCESS: 'stream-icon stream-flag',
        QUEUE: 'stream-icon stream-hourglass executing',
        QUEUE_CACHE: 'stream-icon stream-hourglass executing'
    }
    return [iconMap[status]]
}

export function getPipelineStatusIconCls (status) {
    const iconName = 'bk-icon'
    const iconMap = {
        SUCCEED: 'icon-check-circle',
        FAILED: 'icon-close-circle',
        SKIP: 'icon-redo-arrow',
        RUNNING: 'icon-circle-2-1 executing'
    }
    return [iconName, iconMap[status]]
}
