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

export const SET_TEMPLATE = 'SET_TEMPLATE'

export const FETCHING_ATOM_VERSION = 'FETCHING_ATOM_VERSION'
export const SET_ATOM_VERSION_LIST = 'SET_ATOM_VERSION_LIST'

export const FETCHING_ATOM_LIST = 'FETCHING_ATOM_LIST'
export const SET_ATOMS = 'SET_ATOMS'
export const SET_COMMEND_ATOM_COUNT = 'SET_COMMEND_ATOM_COUNT'
export const SET_ATOM_PAGE_OVER = 'SET_ATOM_PAGE_OVER'
export const CLEAR_ATOM_DATA = 'CLEAR_ATOM_DATA'
export const SET_REQUEST_ATOM_DATA = 'SET_REQUEST_ATOM_DATA'
export const FETCHING_ATOM_MORE_LOADING = 'FETCHING_ATOM_MORE_LOADING'
export const SET_ATOMS_CLASSIFY = 'SET_ATOMS_CLASSIFY'
export const SET_CONTAINER_DETAIL = 'SET_CONTAINER_DETAIL'
export const SET_ATOM_MODAL = 'SET_ATOM_MODAL'
export const SET_ATOM_MODAL_FETCHING = 'SET_FETCHING_ATOM_MODAL'
export const SET_CONTAINER_FETCHING = 'SET_CONTAINER_FETCHING'
export const SET_CONTAINER = 'SET_CONTAINER'

export const UPDATE_ATOM_TYPE = 'UPDATE_ATOM_TYPE'
export const INSERT_ATOM = 'INSERT_ATOM'
export const DELETE_ATOM = 'DELETE_ATOM'
export const UPDATE_ATOM = 'UPDATE_ATOM'
export const UPDATE_ATOM_INPUT = 'UPDATE_ATOM_INPUT'
export const UPDATE_WHOLE_ATOM_INPUT = 'UPDATE_WHOLE_ATOM_INPUT'
export const UPDATE_ATOM_OUTPUT = 'UPDATE_ATOM_OUTPUT'
export const UPDATE_ATOM_OUTPUT_NAMESPACE = 'UPDATE_ATOM_OUTPUT_NAMESPACE'
export const DELETE_ATOM_PROP = 'DELETE_ATOM_PROP'
export const SET_REMOTE_TRIGGER_TOKEN = 'SET_REMOTE_TRIGGER_TOKEN'

export const ADD_STAGE = 'ADD_STAGE'
export const DELETE_STAGE = 'DELETE_STAGE'
export const UPDATE_STAGE = 'UPDATE_STAGE'
export const SET_INSERT_STAGE_STATE = 'SET_INSERT_STAGE_STATE'

export const SET_STAGE_TAG_LIST = 'SET_STAGE_TAG_LIST'
export const SET_COMMON_SETTING = 'SET_COMMON_SETTING'

export const SET_DEFAULT_STAGE_TAG = 'SET_DEFAULT_STAGE_TAG'

export const ADD_CONTAINER = 'ADD_CONTAINER'
export const UPDATE_CONTAINER = 'UPDATE_CONTAINER'
export const DELETE_CONTAINER = 'DELETE_CONTAINER'

export const SET_PIPELINE = 'SET_PIPELINE'
export const SET_PIPELINE_EDITING = 'SET_PIPELINE_EDITING'
export const SET_EDIT_FROM = 'SET_EDIT_FROM'
export const PROPERTY_PANEL_VISIBLE = 'PROPERTY_PANEL_VISIBLE'
export const CONTAINER_TYPE_SELECTION_VISIBLE = 'CONTAINER_TYPE_SELECTION_VISIBLE'

export const VM_CONTAINER_TYPE = 'vmBuild'
export const TRIGGER_CONTAINER_TYPE = 'trigger'
export const NORMAL_CONTAINER_TYPE = 'normal'

export const SET_BUILD_PARAM = 'SET_BUILD_PARAM'

export const SET_PIPELINE_EXEC_DETAIL = 'SET_PIPELINE_EXEC_DETAIL'
export const SET_HIDE_SKIP_EXEC_TASK = 'SET_HIDE_SKIP_EXEC_TASK'

export const SET_GLOBAL_ENVS = 'SET_GLOBAL_ENVS'

export const TOGGLE_ATOM_SELECTOR_POPUP = 'TOGGLE_ATOM_SELECTOR_POPUP'

export const SET_STORE_LOADING = 'SET_STORE_LOADING'

export const SET_STORE_SEARCH = 'SET_STORE_SEARCH'

export const SET_EXECUTE_STATUS = 'SET_EXECUTE_STATUS'
export const SET_SAVE_STATUS = 'SET_SAVE_STATUS'

export const SET_PIPELINE_STAGE = 'SET_PIPELINE_STAGE'
export const SET_PIPELINE_CONTAINER = 'SET_PIPELINE_CONTAINER'
export const SET_COMMEND_ATOM_PAGE_OVER = 'SET_COMMEND_ATOM_PAGE_OVER'

export const buildNoRules = [
    {
        value: 'CONSISTENT',
        label: 'consistent'
    }, {
        value: 'SUCCESS_BUILD_INCREMENT',
        label: 'success_build_increment'
    }, {
        value: 'EVERY_BUILD_INCREMENT',
        label: 'every_build_increment',
        default: true
    }
]

// 构建平台类型列表
export const platformList = [
    {
        id: 'ANDROID',
        name: 'Android'
    }, {
        id: 'IPHONE',
        name: 'IOS'
    }
]

export const defaultBuildNo = {
    buildNo: '0',
    buildNoType: buildNoRules[2].value
}

export const TOGGLE_STAGE_REVIEW_PANEL = 'TOGGLE_STAGE_REVIEW_PANEL'
export const SET_IMPORTED_JSON = 'SET_IMPORTED_JSON'
