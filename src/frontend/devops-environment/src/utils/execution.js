const STATUS_PENDING = 1
const STATUS_DOING = 2
const STATUS_SUCCESS = 3
const STATUS_FAIL = 4
const STATUS_PASS = 5
const STATUS_INGORE_ERROR = 6
const STATUS_MANUAL_CONFIRM = 7
const STATIS_MANUAL_END = 8
const STATUS_STATE_EXCEPTION = 9
const STATUS_FORCEDING = 10
const STATUS_FORCED_SUCCESS = 11

export const checkStatus = (status) => {
    if ([
        STATUS_SUCCESS,
        STATUS_PASS
    ].includes(status)) {
        return 'success'
    }
    if ([
        STATUS_INGORE_ERROR
    ].includes(status)) {
        return 'ingore'
    }
    if ([
        STATUS_FAIL,
        STATUS_STATE_EXCEPTION,
        STATIS_MANUAL_END
    ].includes(status)) {
        return 'fail'
    }
    if ([
        STATUS_FORCED_SUCCESS
    ].includes(status)) {
        return 'forced'
    }
    if ([
        STATUS_FORCEDING
    ].includes(status)) {
        return 'forceding'
    }
    if ([
        STATUS_DOING
    ].includes(status)) {
        return 'loading'
    }
    if ([
        STATUS_MANUAL_CONFIRM
    ].includes(status)) {
        return 'confirm'
    }
    if ([
        STATUS_PENDING
    ].includes(status)) {
        return 'disabled'
    }
    return 'disabled'
}

export const statusStyleMap = {
    success: 'success',
    ingore: 'ingore',
    fail: 'fail',
    forced: 'forced',
    forceding: 'loading',
    loading: 'loading',
    confirm: 'confirm',
    confirmForced: 'confirm-forced',
    disabled: 'disabled',
    evicted: 'fail'
}
