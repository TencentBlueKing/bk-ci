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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.auth.api

import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.code.AuthServiceCode

interface AuthProjectApi {
    /**
     * 获取项目成员 (需要对接的权限中心支持该功能才可以）
     * @param serviceCode 调用者的服务编码
     * @param projectCode 项目编码/ID
     * @param group 项目角色组,非必填写，如果不填，则默认取项目下的所有角色组的成员
     */
    fun getProjectUsers(serviceCode: AuthServiceCode, projectCode: String, group: BkAuthGroup? = null): List<String>

    /**
     * 拉取项目所有成员，并按项目角色组分组成员信息返回
     * @param serviceCode 调用者的服务编码
     * @param projectCode 项目编码/ID
     *
     */
    fun getProjectGroupAndUserList(serviceCode: AuthServiceCode, projectCode: String): List<BkAuthGroupAndUserList>

    /**
     * 获取用户有管理权限的项目Code
     * @param serviceCode 调用者的服务编码
     * @param userId 用户ID
     * @param supplier supplier函数，用于可能需要从外部加载资源的场景,可以不传
     */
    fun getUserProjects(
        serviceCode: AuthServiceCode,
        userId: String,
        supplier: (() -> List<String>)?
    ): List<String>

    /**
     * 获取用户有查看或管理权限的项目
     * @param serviceCode 调用者的服务编码
     * @param userId userId
     * @param supplier supplier函数，用于可能需要从外部加载资源的场景,可以不传
     * @return ProjectCode to ProjectName
     */
    fun getUserProjectsAvailable(
        serviceCode: AuthServiceCode,
        userId: String,
        supplier: (() -> List<String>)?
    ): Map<String, String>

    /**
     * 判断是否某个项目中某个组角色的成员
     * @param user 用户id
     * @param serviceCode 服务类型，比如PIPELINE
     * @param projectCode 项目编码
     * @param group 项目组角色
     */
    fun isProjectUser(user: String, serviceCode: AuthServiceCode, projectCode: String, group: BkAuthGroup?): Boolean
}