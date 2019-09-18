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

const devopsUtil = {};

(function (win, exports) {
    const Vue = win.Vue
    win.globalVue = Vue ? new Vue() : null
    const SYNC_TOP_URL = 'syncUrl'
    const SYNC_TOP_PROJECT_ID = 'syncTopProjectId'
    const SYNC_PROJECT_LIST = 'syncProjectList'
    const SYNC_USER_INFO = 'syncUserInfo'
    const RECEIVE_PROJECT_ID = 'receiveProjectId'
    const TOGGLE_PROJECT_MENU = 'toggleProjectMenu'
    const POP_PROJECT_DIALOG = 'popProjectDialog'
    const SHOW_ASK_PERMISSION_DIALOG = 'showAskPermissionDialog'
    const TOGGLE_LOGIN_DIALOG = 'toggleLoginDialog'
    const LEAVE_CONFIRM = 'leaveConfirm'
    const LEAVE_CANCEL = 'leaveCancel'
    const LEAVE_CONFIRM_ORDER = 'leaveConfirmOrder'
    const LEAVE_CANCEL_ORDER = 'leaveCancelOrder'
    const SHOW_TIPS = 'showTips'
    const BACK_HOME = 'backHome'

    function init () {
        if (win.addEventListener) {
            win.addEventListener('message', onMessage)
        } else if (win.attachEvent) {
            win.attachEvent('onmessage', onMessage)
        }
    }
    
    function onMessage (e) {
        parseMessage(e.data)
    }
    
    function parseMessage (data) {
        try {
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
            win.globalVue.$emit(eventName, payload)
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
            win.globalVue.$emit('', {
                [prop]: val
            })
        } else {
            win['$' + prop] = val
        }

        triggerEvent('change::$' + prop, {
            [prop]: val
        })
    }
    
    /**
     * 同步父窗口URL
     * @method syncUrl
     * @param {url} str 要同步到父窗口的url
     * @param {refresh} boolean 是否刷新页面
     */
    exports[SYNC_TOP_URL] = function (url, refresh) {
        communicateOuter({
            action: SYNC_TOP_URL,
            params: {
                url: url,
                refresh
            }
        })
    }

    /**
     * 弹出权限弹窗
     * @method showAskPermissionDialog
     * @param {权限对象} obj 要申请的权限对象, 包含以下三个字段
     *      title               string  权限弹窗标题，可不传，默认为（无权限操作）
     *      noPermissionList    array  要申请的权限列表，[{resource: '流水线', option: '操作'}, ...]
     *      applyPermissionUrl  string 申请权限跳转地址
     */
    exports[SHOW_ASK_PERMISSION_DIALOG] = function (params) {
        communicateOuter({
            action: SHOW_ASK_PERMISSION_DIALOG,
            params
        })
    }

    /**
     * 确认是否离开
     * @method leaveConfirm
     * @param {} params
     * {
     *  tips: string
     * }
     */
    exports[LEAVE_CONFIRM] = function (params) {
        communicateOuter({
            action: LEAVE_CONFIRM,
            params
        })
    }

    /**
     * 弹出信息
     * @method showTips
     * @param {tips} object 提示信息对象, 传入$bkMessage
     */
    exports[SHOW_TIPS] = function (tips) {
        console.log(tips)
        communicateOuter({
            action: SHOW_TIPS,
            params: tips
        })
    }

    /**
     * 登录弹窗
     * @method toggleLoginDialog
     * @param {isShow} boolean 是否显示登录弹窗
     */
    exports[TOGGLE_LOGIN_DIALOG] = function (isShow) {
        communicateOuter({
            action: TOGGLE_LOGIN_DIALOG,
            params: isShow
        })
    }

    /**
     * 同步父窗口的ProjectId
     * @method syncTopProjectId
     * @param {projectId} str 要同步到父窗口的ProjectId
     */
    exports[SYNC_TOP_PROJECT_ID] = function (projectId) {
        communicateOuter({
            action: SYNC_TOP_PROJECT_ID,
            params: {
                projectId
            }
        })
    }

    /**
     * 关闭或显示项目下拉菜单
     * @method toggleProjectMenu
     * @param {show} boolean 是否显示
     */
    exports[TOGGLE_PROJECT_MENU] = function (show) {
        communicateOuter({
            action: TOGGLE_PROJECT_MENU,
            params: show
        })
    }

    /**
     * 弹出项目编辑窗口
     * @method popProjectDialog
     * @param {project} object 项目对象
     */
    exports[POP_PROJECT_DIALOG] = function (project) {
        communicateOuter({
            action: POP_PROJECT_DIALOG,
            params: project
        })
    }

    /**
     * 获取父窗口的ProjectId
     * @method receiveProjectId
     * @param {projectId} str 父窗口的ProjectId
     */
    exports[RECEIVE_PROJECT_ID] = function (projectId) {
        addToGlobal('currentProjectId', projectId)
    }

    /**
     * 接收父窗口的项目列表
     * @method syncProjectList
     * @param {projectList} str 父窗口的项目列表
     */
    exports[SYNC_PROJECT_LIST] = function (projectList) {
        addToGlobal('projectList', projectList)
    }

    /**
     * 获取父窗口的用户信息
     * @method syncUserInfo
     * @param {userInfo} str 父窗口的用户信息
     */
    exports[SYNC_USER_INFO] = function (userInfo) {
        addToGlobal('userInfo', userInfo)
    }

    /**
     * 触发返回首页事件
     * @method backHome
     */
    exports[BACK_HOME] = function () {
        triggerEvent('order::' + BACK_HOME)
    }

    /**
     * 页面确认离开——确定事件
     * @method leaveConfirmOrder
     */
    exports[LEAVE_CONFIRM_ORDER] = function () {
        triggerEvent('order::' + LEAVE_CONFIRM)
    }

    /**
     * 页面确认离开——取消事件
     * @method leaveCancelOrder
     */
    exports[LEAVE_CANCEL_ORDER] = function () {
        triggerEvent('order::' + LEAVE_CANCEL)
    }

    for (const key in exports) {
        if (exports.hasOwnProperty(key)) {
            const cb = exports[key]
            addToGlobal(key, cb)
        }
    }
    
    init()
})(window, devopsUtil)
