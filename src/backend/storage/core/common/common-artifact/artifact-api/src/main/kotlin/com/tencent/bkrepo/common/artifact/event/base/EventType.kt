/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.common.artifact.event.base

/**
 * 事件类型
 */
enum class EventType(val nick: String) {
    // PROJECT
    PROJECT_CREATED("创建项目"),

    // REPOSITORY
    REPO_CREATED("创建仓库"),
    REPO_UPDATED("更新仓库"),
    REPO_DELETED("删除仓库"),
    // 主要针对代理仓库需要定时从远程将相关信息同步到本地
    REPO_REFRESHED("刷新仓库信息"),

    // NODE
    NODE_CREATED("创建节点"),
    NODE_RENAMED("重命名节点"),
    NODE_MOVED("移动节点"),
    NODE_COPIED("复制节点"),
    NODE_DELETED("删除节点"),
    NODE_DOWNLOADED("下载节点"),

    // METADATA
    METADATA_DELETED("删除元数据"),
    METADATA_SAVED("添加元数据"),

    // VERSION
    VERSION_CREATED("创建制品"),
    VERSION_DELETED("删除制品"),
    VERSION_DOWNLOAD("下载制品"),
    VERSION_UPDATED("更新制品"),
    VERSION_STAGED("晋级制品"),

    // ADMIN
    ADMIN_ADD("添加管理员"),
    ADMIN_DELETE("移除管理员"),

    // WebHook
    WEBHOOK_TEST("webhook测试")
}
