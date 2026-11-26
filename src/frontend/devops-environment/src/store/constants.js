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
export const ENV_ACTIVE_NODE_TYPE = 'ENV_ACTIVE_NODE_TYPE'
export const ALLNODE = 'allNode'
export const RESIZE_LAYOUT_CONFIG_STORAGE_KEY = 'bk_devops_resize_layout_config' // ResizeLayout组件的配置存储key，存储不同页面的配置
export const RES_TYPE_STORAGE_KEY = 'bk_devops_environment_res_type'

export const SERVICE_RESOURCE_TYPE = {
    PIPELINE: 'pipeline',
    CREATE: 'creative-stream'
}

/**
 * 创作流节点 createWorkspaceSource / nodeSource 枚举
 * - REMOTEDEV / IEG_IMATE：同属「数字同事」，走同一套数字同事鉴权
 * - DEVCLOUD：TEG iMate Linux
 * - CLOUD_DESKTOP_NODE：创作流「云桌面」节点，权限与普通云桌面一致
 */
export const CREATE_WORKSPACE_TYPE = {
    REMOTEDEV: 'REMOTEDEV', // 个人项目数字同事（老创作流云桌面节点）
    DEVCLOUD: 'DEVCLOUD', // TEG iMate Linux
    IEG_IMATE: 'IEG_IMATE', // 团队项目数字同事（共享到团队项目时产生）
    CLOUD_DESKTOP_NODE: 'CLOUD_DESKTOP_NODE' // 创作流云桌面节点
}

/** 与 CREATE_WORKSPACE_TYPE 同值，创作流左侧分类路由 id 与此同名 */
export const NODE_SOURCE = {
    REMOTEDEV: 'REMOTEDEV',
    DEVCLOUD: 'DEVCLOUD',
    IEG_IMATE: 'IEG_IMATE',
    CLOUD_DESKTOP_NODE: 'CLOUD_DESKTOP_NODE'
}

/** 数字同事：个人 REMOTEDEV + 团队 IEG_IMATE，同一套鉴权 */
export const DIGITAL_COLLEAGUE_SOURCES = [
    NODE_SOURCE.REMOTEDEV,
    NODE_SOURCE.IEG_IMATE
]

/** 创作流左侧分类过滤 id（不含全部节点）；数字同事侧以 IEG_IMATE 为代表分类 */
export const CREATE_NODE_SOURCE_FILTERS = [
    NODE_SOURCE.IEG_IMATE,
    NODE_SOURCE.CLOUD_DESKTOP_NODE
]
export const ENV_TYPE_MAP = {
    ALL: 'ALL',
    BUILD: 'BUILD',
    PROD: 'PROD',
    DEV: 'DEV',
    DEVX: 'DEVX',
    CREATE: 'CREATE'
}

export const OS_LABEL_MAP = {
    LINUX: 'Linux',
    MACOS: 'macOS',
    WINDOWS: 'Windows'
}
