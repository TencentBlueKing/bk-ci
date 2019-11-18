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

package com.tencent.devops.store.service.container

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.app.BuildEnv
import com.tencent.devops.store.pojo.app.ContainerApp
import com.tencent.devops.store.pojo.app.ContainerAppCreate
import com.tencent.devops.store.pojo.app.ContainerAppEnvCreate
import com.tencent.devops.store.pojo.app.ContainerAppInfo
import com.tencent.devops.store.pojo.app.ContainerAppRequest
import com.tencent.devops.store.pojo.app.ContainerAppVersion
import com.tencent.devops.store.pojo.app.ContainerAppVersionCreate
import com.tencent.devops.store.pojo.app.ContainerAppWithVersion

/**
 * 编译环境业务逻辑类
 *
 * since: 2018-12-20
 */
interface ContainerAppService {

    /**
     * 根据操作系统查找编译环境信息
     */
    fun listApps(os: String): List<ContainerApp>

    /**
     * 根据编译环境id查找编译环境版本信息
     */
    fun listAppVersion(appId: Int): List<ContainerAppVersion>

    /**
     * 根据操作系统查找环境变量列表及版本列表
     */
    fun listAppsWithVersion(os: String): List<ContainerAppWithVersion>

    /**
     * 根据操作系统查找构建机环境变量
     */
    fun getApps(os: String): List<BuildEnv>

    /**
     * 添加编译环境信息
     */
    fun addApp(app: ContainerAppCreate): Int

    /**
     * 添加编译环境版本信息
     */
    fun addAppVersion(appVersion: ContainerAppVersionCreate)

    /**
     * 添加编译环境变量信息
     */
    fun addAppEnv(appEnvCreate: ContainerAppEnvCreate)

    /**
     * 添加编译环境信息
     */
    fun addContainerAppInfo(containerAppRequest: ContainerAppRequest): Result<Boolean>

    /**
     * 更新编译环境信息
     */
    fun updateContainerAppInfo(id: Int, containerAppRequest: ContainerAppRequest): Result<Boolean>

    /**
     * 删除编译环境信息
     */
    fun deleteContainerAppInfo(id: Int): Result<Boolean>

    /**
     * 根据id获取编译环境信息
     */
    fun getContainerAppInfo(id: Int): Result<ContainerAppInfo?>

    /**
     * 获取所用编译环境信息
     */
    fun getAllContainerAppInfos(): Result<List<ContainerApp>>

    fun getBuildEnv(name: String, version: String, os: String): BuildEnv?

    fun getAppVer(name: String, os: String): List<Map<String, String>>
}
