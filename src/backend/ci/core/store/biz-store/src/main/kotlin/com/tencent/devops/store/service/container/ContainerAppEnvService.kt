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
import com.tencent.devops.store.pojo.app.ContainerAppEnv
import com.tencent.devops.store.pojo.app.ContainerAppEnvCreate

/**
 * 编译环境变量业务逻辑类
 *
 * since: 2018-12-20
 */
interface ContainerAppEnvService {

    /**
     * 根据编译环境ID查找该编译环境下的环境变量
     */
    fun listByAppId(appId: Int): Result<List<ContainerAppEnv>>

    /**
     * 根据id查找编译环境信息
     */
    fun getContainerAppEnv(id: Int): Result<ContainerAppEnv?>

    /**
     * 保存编译环境变量信息
     */
    fun saveContainerAppEnv(containerAppEnvRequest: ContainerAppEnvCreate): Result<Boolean>

    /**
     * 更新编译环境信息
     */
    fun updateContainerAppEnv(id: Int, containerAppEnvRequest: ContainerAppEnvCreate): Result<Boolean>

    /**
     * 删除编译环境变量信息
     */
    fun deleteContainerAppEnv(id: Int): Result<Boolean>
}
