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

const tencentCI = {};

(function (win, exports) {
    const Vue = win.Vue
    win.TCI = {}
    win.TCI.__EVENT_BUS__ = Vue ? new Vue() : null
    const CREATE_DIALOG = 'createDialog'
    const CREATE_ASIDE_PANEL = 'createAsidePanel'
    const CLOSE_ASIDE_PANEL = 'closeAsidePanel'
    const CLOSE_EXT_DIALOG = 'closeExtDialog'
    const GO_TO_PAGE = 'goToPage'
    const SYNC_CUSTOME_DATA = 'syncCustomData'
    const SHOW_TIPS = 'showTips'

    function init () {
        if (win.addEventListener) {
            win.addEventListener('message', onMessage)
        } else if (win.attachEvent) {
            win.attachEvent('onmessage', onMessage)
        }

        for (const key in exports) {
            if (exports.hasOwnProperty(key)) {
                const cb = exports[key]
                addToGlobal(key, cb)
            }
        }
    }
    
    function onMessage (e) {
        parseMessage(e.data)
    }
    
    function parseMessage (data) {
        try {
            console.log('on message,', data)
            const fun = exports[data.action]
            if (typeof fun === 'function') {
                return fun(data.params)
            }
        } catch (e) {
            console.warn(e)
        }
    }

    function triggerEvent (eventName, payload, ele) {
        if (Vue) {
            win.TCI.__EVENT_BUS__.$emit(eventName, payload)
        } else { // 使用其它前端框架时，触发自定义事件
            const cs = new CustomEvent(eventName, { detail: payload })
            win.dispatchEvent(cs)
        }
    }
    
    function communicateOuter (data) {
        if (window.postMessage) {
            try {
                top.postMessage(data, '*')
            } catch (e) {
                console.warn('communicate fail', e)
            }
        }
    }

    //  globalVue = new win.Vue()
    function addToGlobal (prop, val) {
        if (Vue) {
            Vue.prototype['$' + prop] = val
            win.TCI.__EVENT_BUS__.$emit('', {
                [prop]: val
            })
        } else {
            win.TCI['$' + prop] = val
        }

        triggerEvent('change::$' + prop, {
            [prop]: val
        })
    }
    
    /**
     * 创建一个弹窗
     * @method createDialog
     * @param {弹窗选项} obj 弹窗选项
     *      header      string  弹窗标题，可不传，默认为（标题）
     *      width       int     弹窗宽度
     *      height      int     弹窗高度
     *      submitText  string  确定按钮文案
     *      cancelText  string  取消按钮方案
     *      customData  string  传给弹窗的数据
     */
    
    exports[CREATE_DIALOG] = function (params) {
        communicateOuter({
            action: CREATE_DIALOG,
            params
        })
    }

    /**
     * 关闭弹窗
     * @method closeExtDialog
     */
    
    exports[CLOSE_EXT_DIALOG] = function (params) {
        communicateOuter({
            action: CLOSE_EXT_DIALOG,
            params
        })
    }

    /**
     * 跳转页面
     * @method goToPage
     */
    
     exports[GO_TO_PAGE] = function (params) {
      communicateOuter({
          action: GO_TO_PAGE,
          params
      })
  }

    /**
     * 创建一个侧边栏
     * @method createAsidePanel
     * @param {侧边栏选项} obj 弹窗选项
     *      header      string  弹窗标题，可不传，默认为（标题）
     *      width       int     弹窗宽度
     */
    
    exports[CREATE_ASIDE_PANEL] = function (params) {
        communicateOuter({
            action: CREATE_ASIDE_PANEL,
            params
        })
    }

    /**
     * 关闭侧边栏
     * @method closeAsidePanel
     */
    
    exports[CLOSE_ASIDE_PANEL] = function (params) {
        communicateOuter({
            action: CLOSE_ASIDE_PANEL,
            params
        })
    }

    /**
     * 接收数据
     * @method syncCustomData
     */
    
    exports[SYNC_CUSTOME_DATA] = function (params) {
        console.log(params)
        try {
            const paramsObj = JSON.parse(params)
            window.BK_DEVOPS_MICRO_EXTENSION_DATA = paramsObj
            // window.data = paramsObj;
            triggerEvent('data:' + SYNC_CUSTOME_DATA, paramsObj)
        } catch (error) {
            console.error('micro extension: params error', error)
        }
    }

    /**
     * 弹出信息
     * @method showTips
     * @param {tips} object 提示信息对象, 传入$bkMessage
     */
    exports[SHOW_TIPS] = function (tips) {
        communicateOuter({
            action: SHOW_TIPS,
            params: tips
        })
    }
    init()
})(window, tencentCI)
