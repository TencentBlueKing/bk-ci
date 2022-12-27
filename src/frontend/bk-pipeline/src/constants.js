export const VM_CONTAINER_TYPE = 'vmBuild'
export const TRIGGER_CONTAINER_TYPE = 'trigger'
export const NORMAL_CONTAINER_TYPE = 'normal'

export const QUALITY_IN_ATOM_CODE = 'qualityGateInTask'
export const QUALITY_OUT_ATOM_CODE = 'qualityGateOutTask'

export const COPY_EVENT_NAME = 'copy'
export const DELETE_EVENT_NAME = 'delete'
export const CLICK_EVENT_NAME = 'click'

export const ATOM_QUALITY_CHECK_EVENT_NAME = 'atom-quality-check'
export const ATOM_REVIEW_EVENT_NAME = 'atom-review'
export const ATOM_CONTINUE_EVENT_NAME = 'atom-continue'
export const ATOM_EXEC_EVENT_NAME = 'atom-exec'

export const ATOM_ADD_EVENT_NAME = 'add-atom'

export const ADD_STAGE = 'add-stage'
export const STAGE_CHECK = 'stage-check'
export const STAGE_RETRY = 'stage-retry'
export const DEBUG_CONTAINER = 'debug-container'

export const DOCKER_BUILD_TYPE = 'DOCKER'
export const PUBLIC_DEVCLOUD_BUILD_TYPE = 'PUBLIC_DEVCLOUD'
export const PUBLIC_BCS_BUILD_TYPE = 'PUBLIC_BCS'

export const TOGGLE_POST_ACTION_VISIBLE = 'toggle-post-action-visible'

export const STATUS_MAP = {
    WAITING: 'WAITING',
    RUNNING: 'RUNNING',
    SKIP: 'SKIP',
    DEPENDENT_WAITING: 'DEPENDENT_WAITING',
    PREPARE_ENV: 'PREPARE_ENV',
    SUCCEED: 'SUCCEED',
    FAILED: 'FAILED',
    REVIEWING: 'REVIEWING',
    QUALITY_CHECK_FAIL: 'QUALITY_CHECK_FAIL',
    QUALITY_CHECK_PASS: 'QUALITY_CHECK_PASS',
    QUALITY_CHECK_WAIT: 'QUALITY_CHECK_WAIT',
    REVIEW_ABORT: 'REVIEW_ABORT',
    REVIEW_PROCESSED: 'REVIEW_PROCESSED',
    DISABLED: 'DISABLED',
    QUEUE: 'QUEUE',
    LOOP_WAITING: 'LOOP_WAITING',
    CALL_WAITING: 'CALL_WAITING',
    UNEXEC: 'UNEXEC',
    TERMINATE: 'TERMINATE',
    CANCELED: 'CANCELED',
    PAUSE: 'PAUSE',
    HEARTBEAT_TIMEOUT: 'HEARTBEAT_TIMEOUT',
    QUEUE_TIMEOUT: 'QUEUE_TIMEOUT',
    EXEC_TIMEOUT: 'EXEC_TIMEOUT'
}
