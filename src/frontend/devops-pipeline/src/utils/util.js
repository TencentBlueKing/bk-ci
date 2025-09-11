/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import {
    ALL_PIPELINE_VIEW_ID
} from '@/store/constants'
import { v4 as uuidv4 } from 'uuid'
import { isFileParam } from '@/store/modules/atom/paramsConfig'
import { VAR_MAX_LENGTH } from '@/store/constants'

export function isVNode (node) {
    return typeof node === 'object' && Object.prototype.hasOwnProperty.call(node, 'componentOptions')
}

export function urlJoin (...args) {
    return args.filter(arg => arg).join('/').replace(/([^:]\/)\/+/g, '$1')
}

export function isShallowEqual (obj1, obj2) {
    if (!isObject(obj1) || !isObject(obj2)) {
        return false
    }
    const obj1Keys = Object.keys(obj1)
    const obj2Keys = Object.keys(obj2)
    if (obj1Keys.length !== obj2Keys.length) {
        return false
    }

    return obj1Keys.every(key => obj1[key] === obj2[key])
}

export function isInArray (ele, array) {
    for (const item of array) {
        if (item === ele) {
            return true
        }
    }

    return false
}

export function isInlineElment (node) {
    const inlineElements = ['a', 'abbr', 'acronym', 'b', 'bdo', 'big', 'br', 'cite', 'code', 'dfn', 'em', 'font', 'i', 'img', 'input', 'kbd', 'label', 'q', 's', 'samp', 'select', 'small', 'span', 'strike', 'strong', 'sub', 'sup', 'textarea', 'tt', 'u', 'var']
    const tag = (node.tagName).toLowerCase()
    const display = getComputedStyle(node).display

    if ((isInArray(tag, inlineElements) && display === 'index') || display === 'inline') {
        console.warn('Binding node is displayed as inline element. To avoid some unexpected rendering error, please set binding node displayed as block element.')

        return true
    }

    return false
}

/**
 *  获取元素相对于页面的高度
 *  @param node {NodeElement} 指定的DOM元素
 */
export function getActualTop (node) {
    let actualTop = node.offsetTop
    let current = node.offsetParent

    while (current !== null) {
        actualTop += current.offsetTop
        current = current.offsetParent
    }

    return actualTop
}

/**
 *  获取元素相对于页面左侧的宽度
 *  @param node {NodeElement} 指定的DOM元素
 */
export function getActualLeft (node) {
    let actualLeft = node.offsetLeft
    let current = node.offsetParent

    while (current !== null) {
        actualLeft += current.offsetLeft
        current = current.offsetParent
    }

    return actualLeft
}

/**
 *  对元素添加样式类
 *  @param node {NodeElement} 指定的DOM元素
 *  @param className {String} 类名
 */
export function addClass (node, className) {
    const classNames = className.split(' ')
    if (node.nodeType === 1) {
        if (!node.className && classNames.length === 1) {
            node.className = className
        } else {
            let setClass = ' ' + node.className + ' '
            classNames.forEach((cl) => {
                if (setClass.indexOf(' ' + cl + ' ') < 0) {
                    setClass += cl + ' '
                }
            })
            const rtrim = /^\s+|\s+$/
            node.className = setClass.replace(rtrim, '')
        }
    }
}

/**
 *  对元素删除样式类
 *  @param node {NodeElement} 指定的DOM元素
 *  @param className {String} 类名
 */
export function removeClass (node, className) {
    const classNames = className.split(' ')
    if (node.nodeType === 1) {
        let setClass = ' ' + node.className + ' '
        classNames.forEach((cl) => {
            setClass = setClass.replace(' ' + cl + ' ', ' ')
        })
        const rtrim = /^\s+|\s+$/
        node.className = setClass.replace(rtrim, '')
    }
}

/**
 *  将传入的配置项转成本地的对象
 *  @param config {Object} 传入的对象
 *  @return obj {Object} 本地化之后的对象
 */
export function localizeConfig (config) {
    const obj = {}

    for (const key in config) {
        obj[key] = config[key]
    }

    return obj
}

/**
 *  在一个元素为对象的数组中，根据oldKey: oldValue找到指定的数组元素，并返回该数组元素中指定key的value
 *  @param arr - 元素为对象的数组
 *  @param oldKey - 查找的key
 *  @param oldValue - 查找的value
 *  @param key - 需要返回的value的指定的key
 *  @return result - 找到的value值，未找到返回undefined
 */
export function findValByKeyValue (arr, oldKey, oldValue, key) {
    let result

    for (const obj of arr) {
        for (const _key in obj) {
            if (_key === oldKey && obj[_key] === oldValue) {
                result = obj[key]

                break
            }
        }
    }

    return result
}

/**
 *  在一个元素为对象的数组中，根据oldKey: oldValue找到指定的数组元素，并返回该数组的index
 *  @param arr - 元素为对象的数组
 *  @param oldKey - 查找的key
 *  @param oldValue - 查找的value
 *  @return result - 找到的index值，未找到返回-1
 */
export function findIndexByKeyValue (arr, oldKey, oldValue) {
    return arr.findIndex((v, i) => {
        return Object.prototype.hasOwnProperty.call(v, oldKey) && v[oldKey] === oldValue
    })
}

/**
 *  将字符串去掉指定内容之后转成数字
 *  @param {String} str - 需要转换的字符串
 *  @param {String} indicator - 需要被去掉的内容
 */
export function converStrToNum (str, indicator) {
    const reg = new RegExp(indicator, 'g')
    const $str = str.replace(reg, '')

    return ~~$str
}

/**
 *  将字符串根据indicator转成数组
 */
export function converStrToArr (str, indicator) {
    return str.length ? str.split(indicator) : []
}

/**
 * 将字符串根据indicator转成数组并将内容都转成Number类型（仅限数组内容均为数字的字符串）
 */
export function convertStrToNumArr (str, indicator) {
    return converStrToArr(str, indicator).map(item => {
        return ~~item
    })
}

/**
 *  将毫秒值转换成x时x分x秒的形式
 *  @param {Number} time - 时间的毫秒形式
 *  @return {String} str - 转换后的字符串
 */
export function convertMStoString (time) {
    function getSeconds (sec) {
        return `${sec}${window.pipelineVue.$i18n.t('timeMap.seconds')}`
    }

    function getMinutes (sec) {
        if (sec / 60 >= 1) {
            return `${Math.floor(sec / 60)}${window.pipelineVue.$i18n.t('timeMap.minutes')}${getSeconds(sec % 60)}`
        } else {
            return getSeconds(sec)
        }
    }

    function getHours (sec) {
        if (sec / 3600 >= 1) {
            return `${Math.floor(sec / 3600)}${window.pipelineVue.$i18n.t('timeMap.hours')}${getMinutes(sec % 3600)}`
        } else {
            return getMinutes(sec)
        }
    }

    function getDays (sec) {
        if (sec / 86400 >= 1) {
            return `${Math.floor(sec / 86400)}${window.pipelineVue.$i18n.t('timeMap.days')}${getHours(sec % 86400)}`
        } else {
            return getHours(sec)
        }
    }

    return time ? getDays(Math.floor(time / 1000)) : `0${window.pipelineVue.$i18n.t('timeMap.seconds')}`
}

export function convertMillSec (ms, full = false) {
    if (!Number.isInteger(ms)) return '--'
    const millseconds = ms % 1000 > 0 ? `.${`${ms % 1000}`.padStart(3, '0')}` : ''
    const day = Math.floor(ms / (24 * 60 * 60 * 1000))
    if (day > 0) {
        // 先减去天数，再计算小时
        ms -= day * (24 * 60 * 60 * 1000)
    }
    const seconds = Math.floor(ms / 1000) % 60
    const minutes = Math.floor(ms / 1000 / 60) % 60
    const hours = Math.floor(ms / 1000 / 60 / 60) % 24

    return `${day ? day + window.pipelineVue.$i18n.t('timeMap.days') : ''} ${[
        ...(hours > 0 ? [hours] : [full ? '00' : '']),
        minutes,
        seconds
    ].map(prezero).join(':')}${full ? '' : millseconds}`
}

/**
 *  将毫秒值转换成x时x分x秒的形式并使用格式化规则
 *  @param {Number} time - 时间的毫秒形式
 *  @return {String} str - 转换后的字符串
 */
export function convertMStoStringByRule (time) {
    if (time < 0) {
        return '--'
    }
    let res = ''
    if (window.pipelineVue.$i18n && window.pipelineVue.$i18n.locale === 'en-US') {
        res = convertToEn(time)
    } else {
        res = convertToCn(time)
    }
    return res
}

function convertToCn (time) {
    const str = convertMStoString(time)
    let res = str
    const arr = str.match(/^\d{1,}([\u4e00-\u9fa5]){1,}/)
    if (arr.length) {
        switch (arr[1]) {
            case '秒':
                res = '1分钟内'
                break
            case '天':
                res = `大于${arr[0]}`
                break
            case '时':
                res = str.replace(/\d{1,}秒/, '')
                break
        }
    }
    return res
}

function convertToEn (time) {
    const sec = time / 1000
    let res = ''
    if (sec <= 60) {
        res = 'less than 1 minute'
    } else if (sec <= 60 * 60) {
        res = `${Math.floor(sec / 60)}m and ${(Math.floor(sec % 60))}s`
    } else if (time <= 60 * 60 * 24) {
        res = `${Math.floor(sec / 3600)}h and ${Math.floor(sec % 60 / 60)}m`
    } else {
        res = `more than ${Math.floor(sec / 86400)} days`
    }
    return res
}

function prezero (num) {
    num = Number(num)

    if (num < 10) {
        return '0' + num
    }

    return num
}

export function convertTime (ms) {
    if (!ms) return '--'
    const time = new Date(ms)

    return `${time.getFullYear()}-${prezero(time.getMonth() + 1)}-${prezero(time.getDate())} ${prezero(time.getHours())}:${prezero(time.getMinutes())}:${prezero(time.getSeconds())}`
}

export function coverStrTimer (ms) {
    const time = new Date(ms)

    return `${time.getFullYear()}-${time.getMonth() + 1}-${time.getDate()} ${time.getHours()}:${time.getMinutes()}:${time.getSeconds()}`
}

export function convertMiniTime (ms) {
    const time = new Date(ms)

    return `${prezero(time.getMonth() + 1)}-${prezero(time.getDate())} ${prezero(time.getHours())}:${prezero(time.getMinutes())}`
}

/**
 *  转换文件大小
 */
export function convertFileSize (size, unit) {
    const arr = ['B', 'KB', 'MB', 'GB', 'TB']
    const calcSize = size / 1024
    let index

    arr.some((item, _index) => {
        if (unit === item) {
            index = _index
            return true
        }
        return false
    })

    const next = arr[index + 1]

    if (calcSize > 1024) {
        if (!next) {
            return `${calcSize.toFixed(2)}${unit}`
        } else {
            return convertFileSize(calcSize, next)
        }
    } else {
        return `${calcSize.toFixed(2)} ${next || unit}`
    }
}

export function isObject (o) {
    return o !== null && typeof o === 'object' && !Array.isArray(o)
}

export function mergeModules (target, ...modules) {
    return modules.reduce((merged, mod) => {
        Object.keys(mod).map(key => {
            if (isObject(merged[key]) && isObject(mod[key])) {
                merged[key] = {
                    ...merged[key],
                    ...mod[key]
                }
            }
            return key
        })

        return merged
    }, target)
}

/**
 * @param {String} selector
 * @param {DOM element} parent
 */
export const getOuterHeight = (selector, parent) => {
    const { marginTop, height, marginBottom } = getDOMRect(selector, parent)
    return parseFloat(marginTop) + parseFloat(height) + parseFloat(marginBottom)
}

/**
 * @param {String} selector
 * @param {DOM element} parent
 */
export const getOuterWidth = (selector, parent) => {
    const { marginLeft, width, marginRight } = getDOMRect(selector, parent)
    return parseFloat(marginLeft) + parseFloat(width) + parseFloat(marginRight)
}

/**
 * @param {String} selector
 * @param {DOM element} parent
 */
export const getInnerWidth = (selector, parent) => {
    const { width } = getDOMRect(selector, parent)
    return parseFloat(width)
}

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

const isDOMElement = obj => {
    return (
        typeof HTMLElement === 'object' ? obj instanceof HTMLElement : obj && typeof obj === 'object' && obj !== null && obj.nodeType === 1 && typeof obj.nodeName === 'string'
    )
}

export const deepCopy = obj => {
    return JSON.parse(JSON.stringify(obj))
}

export const deepClone = obj => {
    if (typeof structuredClone === 'function') {
        return structuredClone(obj)
    }
    return JSON.parse(JSON.stringify(obj))
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

export function getServiceLogoByPath (link) {
    return link.replace(/\/?(devops\/)?(\w+)\S*$/, '$2')
}

export function getAtomPath (...args) {
    return args.join('-')
}

/**
 *  获取url参数值
 */
export function getQueryString (name) {
    const reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)', 'i')
    const r = window.location.search.substr(1).match(reg)
    if (r != null) return unescape(r[2]); return null
}

/**
 *  将毫秒值转换成xx:xx(分:秒)的形式
 *  @param {Number} time - 时间的毫秒形式
 *  @return {String} str - 转换后的字符串
 */
export function coverTimer (time, type) {
    let res = ''
    function getSeconds (sec, min) {
        const m = min / 60 >= 1 ? '' : '00:'
        if (type) {
            res = sec < 10 ? `${m}0${sec}${window.pipelineVue.$i18n.t('timeMap.seconds')}` : `${m}${sec}${window.pipelineVue.$i18n.t('timeMap.seconds')}`
        } else {
            res = sec < 10 ? `${m}0${sec}` : `${m}${sec}`
        }
        return res
    }

    function getMinutes (sec) {
        if (sec / 60 >= 1) {
            let res = ''
            let m = Math.floor(sec / 60)
            m = m < 10 ? `0${m}` : m
            if (type) {
                res = `${m}${window.pipelineVue.$i18n.t('timeMap.minutes')}${getSeconds(sec % 60, sec)}`
            } else {
                res = `${m}:${getSeconds(sec % 60, sec)}`
            }
            return res
        } else {
            return getSeconds(sec)
        }
    }

    function getHours (sec) {
        if (sec / 3600 >= 1) {
            let res = ''
            let h = Math.floor(sec / 3600)
            h = h < 10 ? `0${h}` : h
            if (type) {
                res = `${h}${window.pipelineVue.$i18n.t('timeMap.hours')}${getMinutes(sec % 3600)}`
            } else {
                res = `${h}:${getMinutes(sec % 3600)}`
            }
            return res
        } else {
            return getMinutes(sec)
        }
    }

    return time ? getHours(Math.floor(time / 1000)) : '00:00'
}

const DEFAULT_TIME_INTERVAL = 1000
export function debounce (fn, interval = DEFAULT_TIME_INTERVAL) {
    let timer = null

    return (...args) => {
        clearTimeout(timer)
        timer = setTimeout(() => {
            timer = null
            return fn(...args)
        }, interval)
    }
}

export function throttle (func, interval = DEFAULT_TIME_INTERVAL) {
    let lastFunc
    let lastRan
    return function () {
        const context = this
        const args = arguments
        if (!lastRan) {
            func.apply(context, args)
            lastRan = Date.now()
        } else {
            clearTimeout(lastFunc)
            lastFunc = setTimeout(function () {
                if ((Date.now() - lastRan) >= interval) {
                    func.apply(context, args)
                    lastRan = Date.now()
                }
            }, interval - (Date.now() - lastRan))
        }
    }
}

export function navConfirm ({ content, title, cancelText, ...restProps }) {
    return new Promise((resolve, reject) => {
        if (typeof window.globalVue.$leaveConfirm !== 'function') {
            reject(new Error('')); return
        }

        window.globalVue.$leaveConfirm({ content, title, cancelText, ...restProps })

        window.globalVue.$once('order::leaveConfirm', () => {
            resolve(true)
        })

        window.globalVue.$once('order::leaveCancel', () => {
            resolve(false)
        })
    })
}

export function getQueryParamList (arr = [], key) {
    if (Array.isArray(arr) && arr.length > 0) {
        const arrLen = arr.length
        return arr.reduce((result, item, index) => {
            result += `${key}=${encodeURIComponent(item)}`
            if (index < arrLen - 1) result += '&'
            return result
        }, '')
    } else if (typeof arr !== 'undefined') {
        return `${key}=${encodeURIComponent(arr)}`
    }
}

// 将vue-router的query参数转换成字符串
export function getQueryParamString (query) {
    const params = []
    for (const key in query) {
        if (Object.prototype.hasOwnProperty.call(query, key)) {
            const value = query[key].indexOf(',') > -1 ? query[key].split(',') : query[key]
            params.push(getQueryParamList(value, key))
        }
    }
    return params.join('&')
}

export function getParamsValuesMap (params = [], valueKey = 'defaultValue', initValues = {}) {
    if (!Array.isArray(params)) return {}
    return params.reduce((values, param) => {
        if (!param.id) return values

        if (isFileParam(param.type) && param.enableVersionControl) {
            values[param.id] = {
                directory: initValues[param.id] ?? param[valueKey],
                latestRandomStringInPath: (valueKey === 'defaultValue' ? param.randomStringInPath : param.latestRandomStringInPath) || ''
            }
        } else {
            values[param.id] = initValues[param.id] ?? param[valueKey]
        }
        return values
    }, {})
}

/**
 * 判断两个数组是否有交集
 * @param {Array} arr1
 * @param {Array} arr2
 */
export function hasIntersection (arr1, arr2) {
    try {
        return arr2.some(item => arr1.includes(item))
    } catch (e) {
        return false
    }
}

/**
 * 根据插件字段rely配置决定是否显示
 * @param {Object} fieldProps 插件字段配置
 * @param {Object} values   插件表单值
 */
export function rely (fieldProps, values) {
    try {
        const { rely: { expression = [], operation = 'AND' } } = fieldProps
        const cb = item => {
            const { key, value, regex } = item
            if (Array.isArray(value)) {
                if (Array.isArray(values[key])) {
                    return hasIntersection(value, values[key])
                }
                return typeof values[key] !== 'undefined' && value.includes(values[key])
            } else if (regex) {
                const reg = new RegExp(regex, 'i')
                return Array.isArray(values[key]) ? values[key].some(item => reg.test(item)) : reg.test(values[key])
            } else {
                return Array.isArray(values[key]) ? values[key].some(item => item === value) : values[key] === value
            }
        }
        switch (operation) {
            case 'AND':
                return expression.every(cb)
            case 'OR':
                return expression.length > 0 ? expression.some(cb) : true
            case 'NOT':
                return expression.length > 0 ? !expression.some(cb) : true
            default:
                return true
        }
    } catch (e) {
        return true
    }
}

export class HttpError extends Error {
    constructor (code = 500, message = 'http request error message') {
        super(message)
        this.code = code
    }
}

export function bkVarWrapper (name) {
    return '${{' + name + '}}'
}

export function bkVarRepoRefWrapper (name) {
    return '${{variables.' + name + '.repo-name}}@${{variables.' + name + '.branch}}'
}

export const toolbars = {
    bold: false, // 粗体
    italic: false, // 斜体
    header: false, // 标题
    underline: false, // 下划线
    strikethrough: false, // 中划线
    mark: false, // 标记
    superscript: false, // 上角标
    subscript: false, // 下角标
    quote: false, // 引用
    ol: false, // 有序列表
    ul: false, // 无序列表
    // link: false, // 链接
    imagelink: false, // 图片链接
    code: false, // code
    table: false, // 表格
    fullscreen: true, // 全屏编辑
    readmodel: false, // 沉浸式阅读
    htmlcode: false, // 展示html源码
    help: false, // 帮助
    /* 1.3.5 */
    undo: false, // 上一步
    redo: false, // 下一步
    trash: false, // 清空
    // save: false, // 保存（触发events中的save事件）
    /* 1.4.2 */
    navigation: false, // 导航目录
    /* 2.1.8 */
    alignleft: false, // 左对齐
    aligncenter: false, // 居中
    alignright: false, // 右对齐
    /* 2.2.1 */
    subfield: false, // 单双栏模式
    preview: true // 预览
}

export function cacheViewIdKey (projectId) {
    return `BK_DEVOPS_VIEW_ID_LS_PREFIX_${projectId}`
}
export function cacheViewId (projectId, viewId) {
    localStorage.setItem(cacheViewIdKey(projectId), viewId)
}

export function getCacheViewId (projectId) {
    return localStorage.getItem(cacheViewIdKey(projectId)) ?? ALL_PIPELINE_VIEW_ID
}

export function getMaterialIconByType (type) {
    const materialIconMap = {
        CODE_SVN: 'CODE_SVN',
        CODE_GIT: 'CODE_GIT',
        CODE_GITLAB: 'CODE_GITLAB',
        GITHUB: 'codeGithubWebHookTrigger',
        CODE_TGIT: 'CODE_GIT',
        CODE_P4: 'CODE_P4',
        CODE_REMOTE: 'remoteTrigger',
        CODE_SERVICE: 'openApi'
    }
    return materialIconMap[type] ?? 'CODE_GIT'
}

export const prettyDateTimeFormat = (target) => {
    if (!target) {
        return ''
    }
    const formatStr = (str) => {
        if (String(str).length === 1) {
            return `0${str}`
        }
        return str
    }
    const d = new Date(target)
    const year = d.getFullYear()
    const month = formatStr(d.getMonth() + 1)
    const date = formatStr(d.getDate())
    const hours = formatStr(d.getHours())
    const minutes = formatStr(d.getMinutes())
    const seconds = formatStr(d.getSeconds())
    return `${year}-${month}-${date} ${hours}:${minutes}:${seconds}`
}

export function areDeeplyEqual (obj1, obj2) {
    const stack = [[obj1, obj2]]
    let current, left, right

    while (stack.length > 0) {
        current = stack.pop()
        left = current[0]
        right = current[1]

        if (left === right) {
            continue
        }

        if (typeof left !== 'object' || left === null
            || typeof right !== 'object' || right === null) {
            return false
        }
        const ignoreKeys = ['isError', 'id', 'pipelineCreator', 'containerHashId', 'containerId', 'executeCount']
        // 排除 isError 字段
        const leftKeys = Object.keys(left).filter(key => !ignoreKeys.includes(key))
        const rightKeys = Object.keys(right).filter(key => !ignoreKeys.includes(key))
        if (leftKeys.length !== rightKeys.length) {
            return false
        }

        for (let i = 0; i < leftKeys.length; i++) {
            const key = leftKeys[i]
            if (!Object.hasOwnProperty.call(right, key)) {
                return false
            }
            if (left[key] === right[key]) {
                continue
            }
            if (typeof left[key] === 'object' && typeof right[key] === 'object') {
                stack.push([left[key], right[key]])
            } else {
                return false
            }
        }
    }

    return true
}

export function generateDisplayName (version, versionName) {
    if (!version || !versionName) return '--'
    return `V${version} (${versionName})`
}

export function weekAgo () {
    // 获取当前日期
    const now = new Date()

    // 获取一周前的日期
    const oneWeekAgo = new Date()
    oneWeekAgo.setDate(now.getDate() - 7)

    // 创建开始和结束日期对象
    const start = new Date(oneWeekAgo.setHours(0, 0, 0))
    const end = new Date(now.setHours(23, 59, 59))
    return [start, end]
}

export function isEmptyObj (obj) {
    try {
        return Object.keys(obj).length === 0
    } catch (error) {
        console.error(error)
        return false
    }
}

export function flatSearchKey (searchKey) {
    return searchKey.reduce((searchMap, item) => {
        if (Array.isArray(item.values)) {
            if (typeof searchMap[item.id] === 'undefined') {
                searchMap[item.id] = Array.from(new Set(item.values.map(v => v.id)))
            } else {
                searchMap[item.id] = Array.from(new Set([
                    ...searchMap[item.id],
                    ...item.values.map(v => v.id)
                ]))
            }
        }
        return searchMap
    }, {})
}

export function parseErrorMsg (msg) {
    try {
        return JSON.parse(msg)
    } catch (e) {
        return {
            message: msg
        }
    }
}

export function showPipelineCheckMsg (showTooltips, message, h) {
    const errorInfo = parseErrorMsg(message)
    showTooltips({
        theme: 'error',
        delay: 0,
        ellipsisLine: 0,
        message: h('div', {
            class: 'pipeline-save-error-list-box'
        }, errorInfo.errors.map(item => h('div', {
            class: 'pipeline-save-error-list-item'
        }, [
            h('p', {}, item.errorTitle),
            h('ul', {
                class: 'pipeline-save-error-list'
            }, item.errorDetails.map(err => h('li', {
                domProps: {
                    innerHTML: err
                }
            })))
        ])))
    })
}

export async function copyToClipboard (text) {
    if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(text)
    } else {
        const textArea = document.createElement('textarea')
        textArea.value = text
        document.body.appendChild(textArea)
        textArea.select()
        document.execCommand('Copy')
        document.body.removeChild(textArea)
    }
}
