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

package com.tencent.devops.auth.pojo.enum

/**
 * 项目权限重置类型
 */
enum class ProjectResetType(val description: String) {
    /**
     * 场景一：为已有资源增加新操作权限
     * 例如：为流水线增加归档权限
     * 参数：migrateResource=true, filterResourceTypes=非空, filterActions=非空
     * 操作：修改分级管理员范围 + 重置项目级用户组权限 + 重置资源级别组权限
     */
    ADD_RESOURCE_ACTION("为已有资源增加新操作权限"),

    /**
     * 场景二：新服务接入权限中心（无存量数据）
     * 例如：SCC任务首次接入
     * 参数：migrateResource=false, filterResourceTypes=非空, filterActions=空
     * 操作：修改分级管理员范围 + 重置项目级用户组权限
     */
    NEW_SERVICE_WITHOUT_DATA("新服务接入权限中心(无存量数据)"),

    /**
     * 场景三：已有服务接入权限中心（有存量数据）
     * 例如：流水线模板接入权限中心
     * 参数：migrateResource=true, filterResourceTypes=非空, filterActions=空
     * 操作：修改分级管理员范围 + 重置项目级用户组权限 + 迁移资源
     */
    EXISTING_SERVICE_WITH_DATA("已有服务接入权限中心(有存量数据)"),

    /**
     * 场景四：增加项目级别操作权限
     * 例如：project_manage-archived-pipeline, project_api-operate
     * 参数：migrateResource=false, filterResourceTypes=包含project, filterActions=非空
     * 操作：修改分级管理员范围 + 重置项目级用户组权限
     */
    ADD_PROJECT_ACTION("增加项目级别操作权限"),

    /**
     * 其他场景：不符合上述场景的重置操作
     */
    OTHER("其他重置场景")
}
