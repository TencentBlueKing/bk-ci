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

import { handleNoPermission } from '../../../common-lib/permission/permission'
import ajax from './request'
import * as BKUI from 'bk-magic-vue'

// 处理流水线无权限的情况
export const handlePipelineNoPermission = (query) => {
    return handleNoPermission(
        BKUI,
        {
            resourceType: 'pipeline',
            ...query
        },
        ajax,
        global.pipelineVue.$createElement
    )
}

// 流水线权限动作
export const RESOURCE_ACTION = {
    CREATE: 'pipeline_create',
    EXECUTE: 'pipeline_execute',
    LIST: 'pipeline_list',
    VIEW: 'pipeline_view',
    EDIT: 'pipeline_edit',
    DOWNLOAD: 'pipeline_download',
    DELETE: 'pipeline_delete'
}

export const PROJECT_RESOURCE_ACTION = {
    MANAGE: 'project_manage',
    VISIT: 'project_visit',
    VIEW: 'project_view',
    VIEWS_MANAGE: 'project_views_manage',
    CREATE: 'project_create',
    EDIT: 'project_edit',
    ENABLE: 'project_enable'
}
