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

export const hashID = () => {
    const uuid = uuidv4().replace(/-/g, '')
    return uuid
}

export const randomString = (len) => {
    const chars = 'ABCDEFGHJKLMNPQRSTWXYZabcdefhijklmnprstwxyz012345678'
    const tempLen = chars.length
    let tempStr = ''
    for (let i = 0; i < len; ++i) {
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
    return (containerType) => containerType === typeName
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
        const { status, jobControlOption: { dependOnType, dependOnId, dependOnName } } = job
        if (status !== STATUS_MAP.DEPENDENT_WAITING) return ''
        let val = ''
        if (dependOnType === 'ID') {
            val = dependOnId || []
        } else if (dependOnType) {
            val = dependOnName || ''
        }
        return val
    } catch (e) {
        console.log(e)
        return ''
    }
}

export function isObject (o) {
    return o !== null && typeof o === 'object' && !Array.isArray(o)
}
