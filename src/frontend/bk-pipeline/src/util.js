import { v4 as uuidv4 } from 'uuid'
import Vue from 'vue'
import {
    VM_CONTAINER_TYPE,
    NORMAL_CONTAINER_TYPE,
    TRIGGER_CONTAINER_TYPE,
    STATUS_MAP
} from './constants'

export const eventBus = new Vue()

/**
 *
 * @param {obj} 判断是否为DOM节点
 * @returns boolean
 */
const isDOMElement = obj => {
    return (
        typeof HTMLElement === 'object' ? obj instanceof HTMLElement : obj && typeof obj === 'object' && obj !== null && obj.nodeType === 1 && typeof obj.nodeName === 'string'
    )
}

/**
 *
 * @param {string} selector
 * @param {string} parent
 * @returns
 */
const getDOMRect = (selector, parent) => {
    const target = isDOMElement(selector) ? selector : parent ? parent.querySelector(selector) : document.querySelector(selector)

    if (!target) {
        return {}
    }

    const style = window.getComputedStyle(target)

    return {
        width: style.width,
        height: style.height,
        marginTop: style.marginTop,
        marginBottom: style.marginBottom,
        marginLeft: style.marginLeft,
        marginRight: style.marginRight
    }
}

/**
 * @param {String} selector
 * @param {DOM element} parent
 */
export const getOuterHeight = (selector, parent) => {
    const { marginTop, height, marginBottom } = getDOMRect(selector, parent)
    return parseFloat(marginTop) + parseFloat(height) + parseFloat(marginBottom)
}

export const hashID = (len = 32) => {
    const uuid = uuidv4().replace(/-/g, '')
    return uuid.substring(0, len)
}

export const randomString = (len) => {
    const chars = 'ABCDEFGHJKLMNPQRSTWXYZabcdefhijklmnprstwxyz_0123456789'
    let tempStr = ''
    for (let i = 0; i < len; ++i) {
        const tempLen = i === 0 ? chars.length - 10 : chars.length
        tempStr += chars.charAt(Math.floor(Math.random() * tempLen))
    }
    return tempStr
}

export function pad (n) {
    return ('00' + n).slice(-2)
}

/**
 *  将毫秒值转换成xx:xx(分:秒)的形式
 *  @param {Number} ms - 时间的毫秒形式
 *  @return {String}
 */
export function convertMStoString (ms) {
    const _1hour = 36e5
    const _1min = 6e4
    const _1day = 8.64e7
    const _1sec = 1000
    const day = Math.floor(ms / _1day)
    const hour = Math.floor((ms % _1day) / _1hour)
    const min = Math.floor((ms % _1hour) / _1min)
    const sec = Math.floor((ms % _1min) / _1sec)
    
    switch (true) {
        case day > 0:
            return `${day} ${pad(hour)}:${pad(min)}:${pad(sec)}`
        case hour > 0:
            return `${pad(hour)}:${pad(min)}:${pad(sec)}`
        default:
            return `${pad(min)}:${pad(sec)}`
    }
}

export function checkContainerType (typeName) {
    return (container) => container['@type'] === typeName
}

/**
 * 判断是否为触发器
 * @param {*} containerType contatiner类型
 */
export const isTriggerContainer = checkContainerType(TRIGGER_CONTAINER_TYPE)

/**
 * 判断是否为构建环境
 * @param {*} containerType contatiner类型
 */
export const isVmContainer = checkContainerType(VM_CONTAINER_TYPE)

/**
 * 判断是否为无编译环境
 * @param {*} containerType contatiner类型
 */
export const isNormalContainer = checkContainerType(NORMAL_CONTAINER_TYPE)

export function getDependOnDesc (job) {
    try {
        const { status, jobControlOption = {} } = job
        const { dependOnType, dependOnId, dependOnName } = jobControlOption
        if (status !== STATUS_MAP.DEPENDENT_WAITING) return ''
        let val = ''
        if (dependOnType === 'ID') {
            val = dependOnId || []
        } else if (dependOnType) {
            val = dependOnName || ''
        }
        return val
    } catch (e) {
        return ''
    }
}

export function isObject (o) {
    return o !== null && typeof o === 'object' && !Array.isArray(o)
}

export function getStatusCls (status) {
    switch (status) {
        case STATUS_MAP.QUEUE:
        case STATUS_MAP.RUNNING:
        case STATUS_MAP.REVIEWING:
        case STATUS_MAP.PREPARE_ENV:
        case STATUS_MAP.LOOP_WAITING:
        case STATUS_MAP.DEPENDENT_WAITING:
        case STATUS_MAP.CALL_WAITING:
            return 'DOING'
        case STATUS_MAP.UNEXEC:
        case STATUS_MAP.DISABLED:
        case STATUS_MAP.WAITING:
            return 'NORMAL'
        case STATUS_MAP.CANCELED:
        case STATUS_MAP.REVIEW_ABORT:
        case STATUS_MAP.TRY_FINALLY:
        case STATUS_MAP.QUEUE_CACHE:
        case STATUS_MAP.SKIP:
            return 'WARNING'
        case STATUS_MAP.FAILED:
        case STATUS_MAP.TERMINATE:
        case STATUS_MAP.HEARTBEAT_TIMEOUT:
        case STATUS_MAP.QUALITY_CHECK_FAIL:
        case STATUS_MAP.QUEUE_TIMEOUT:
        case STATUS_MAP.EXEC_TIMEOUT:
            return 'FAILED'
        case STATUS_MAP.SUCCEED:
        case STATUS_MAP.REVIEW_PROCESSED:
        case STATUS_MAP.STAGE_SUCCESS:
            return 'SUCCESS'
        case STATUS_MAP.PAUSE:
            return 'PAUSE'
        default:
            return ''
    }
}
