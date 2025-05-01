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

// 安装/导入模板类型
export const INSTALL_TYPE_STORE = 'INSTALL_TYPE_STORE'
export const INSTALL_TYPE_REPOSITORY = 'INSTALL_TYPE_REPOSITORY'
export const INSTALL_TYPE_LOCAL = 'INSTALL_TYPE_LOCAL'

// 模板列表表格缓存(用于记录表格列宽\隐藏列\视图ID等)
export const CACHE_TEMPLATE_TABLE_WIDTH_MAP = 'CACHE_TEMPLATE_TABLE_WIDTH_MAP'
export const TEMPLATE_TABLE_COLUMN_CACHE = 'TEMPLATE_TABLE_COLUMN_CACHE'
export const TEMPLATE_VIEW_ID_CACHE = 'TEMPLATE_VIEW_ID_CACHE'

// 模板操作权限类型
export const TEMPLATE_ACTION_MAP = {
    MANAGE: 'MANAGE',
    CREATE: 'CREATE',
    EDIT: 'EDIT',
    DELETE: 'DELETE',
    VIEW: 'VIEW'
}

// 模板列表视图ID
export const ALL_TEMPLATE_VIEW_ID = 'allTemplate'
export const PIPELINE_TEMPLATE_VIEW_ID = 'pipelineTemplate'
export const STAGE_TEMPLATE_VIEW_ID = 'stageTemplate'
export const JOB_TEMPLATE_VIEW_ID = 'jobTemplate'
export const STEP_TEMPLATE_VIEW_ID = 'stepTemplate'
export const TEMPLATE_VIEW_ID_MAP = {
    [ALL_TEMPLATE_VIEW_ID]: 'ALL',
    [PIPELINE_TEMPLATE_VIEW_ID]: 'PIPELINE',
    [STAGE_TEMPLATE_VIEW_ID]: 'STAGE',
    [JOB_TEMPLATE_VIEW_ID]: 'JOB',
    [STEP_TEMPLATE_VIEW_ID]: 'STEP'
}

export const ALL_SOURCE = 'allSource'
export const CUSTOM_SOURCE = 'customSource'
export const MARKET_SOURCE = 'marketSource'

export const TEMPLATE_MODE = {
    CUSTOMIZE: 'CUSTOMIZE',
    CONSTRAINT: 'CONSTRAINT',
    PUBLIC: 'PUBLIC'
}

export const SET_INSTANCE_LIST = 'SET_INSTANCE_LIST'
export const SET_TEMPLATE_DETAIL = 'SET_TEMPLATE_DETAIL'
export const UPDATE_INSTANCE_LIST = 'UPDATE_INSTANCE_LIST'
export const UPDATE_USE_TEMPLATE_SETTING = 'UPDATE_USE_TEMPLATE_SETTING'
export const SET_RELEASE_ING = 'SET_RELEASE_ING'
export const SET_RELEASE_BASE_ID = 'SET_RELEASE_BASE_ID'

// 实例化发布状态
export const RELEASE_STATUS = {
    INIT: 'INIT',
    INSTANCING: 'INSTANCING',
    SUCCESS: 'SUCCESS',
    FAILED: 'FAILED'
}
