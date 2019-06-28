/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

export function isVNode (node) {
    return typeof node === 'object' && node.hasOwnProperty('componentOptions')
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
    let result

    arr.some((v, i) => {
        for (const _key in v) {
            if (_key === oldKey && v[_key] === oldValue) {
                result = i

                break
            }
        }
    })

    return result
}

export function deepClone (obj) {
    const _obj = {}

    for (const key in obj) {
        if (obj[key].toString().toLowerCase() === '[object object]') {
            _obj[key] = deepClone(obj[key])
        } else {
            _obj[key] = key === 'text' ? '' : obj[key]
        }
    }

    return _obj
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
        return `${sec}秒`
    }

    function getMinutes (sec) {
        if (sec / 60 >= 1) {
            return `${Math.floor(sec / 60)}分${getSeconds(sec % 60)}`
        } else {
            return getSeconds(sec)
        }
    }

    function getHours (sec) {
        if (sec / 3600 >= 1) {
            return `${Math.floor(sec / 3600)}小时${getMinutes(sec % 3600)}`
        } else {
            return getMinutes(sec)
        }
    }

    function getDays (sec) {
        if (sec / 86400 >= 1) {
            return `${Math.floor(sec / 86400)}天${getHours(sec % 86400)}`
        } else {
            return getHours(sec)
        }
    }

    return time ? getDays(Math.floor(time / 1000)) : '0秒'
}

/**
 *  将毫秒值转换成x时x分x秒的形式并使用格式化规则
 *  @param {Number} time - 时间的毫秒形式
 *  @return {String} str - 转换后的字符串
 */
export function convertMStoStringByRule (time) {
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

function prezero (num) {
    num = Number(num)

    if (num < 10) {
        return '0' + num
    }

    return num
}

export function convertTime (ms) {
    const time = new Date(ms)

    return `${time.getFullYear()}-${prezero(time.getMonth() + 1)}-${prezero(time.getDate())} ${prezero(time.getHours())}:${prezero(time.getMinutes())}:${prezero(time.getSeconds())}`
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
        return `${calcSize.toFixed(2)}${next || unit}`
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

export const hashID = (length = 8) => {
    let pos = 0
    let result = ''
    while (pos < length) {
        const n = Math.round(Math.random() * 126) + 33
        result += String.fromCharCode(n)
        pos++
    }
    return result
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
 *  复制文本
 */
export const copyText = (text) => {
    const textarea = document.createElement('textarea')
    document.body.appendChild(textarea)
    textarea.value = text
    textarea.select()
    if (document.execCommand('copy')) {
        document.execCommand('copy')
        document.body.removeChild(textarea)
        return true
    } else {
        console.warn('浏览器不支持此功能，请使用谷歌浏览器。')
    }
    return false
}
