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

package com.tencent.devops.project.service

import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.service.OPPServiceVO
import com.tencent.devops.project.pojo.service.ServiceCreateInfo
import com.tencent.devops.project.pojo.service.ServiceListVO
import com.tencent.devops.project.pojo.service.ServiceVO

/**
 * 用户服务列表中展示的业务服务的服务接口
 */
interface UserProjectServiceService {
    /**
     * OP接口
     */
    fun listOPService(userId: String): Result<List<OPPServiceVO>>

    /**
     * 拉取用户的服务列表
     */
    fun listService(userId: String, projectId: String?): Result<List<ServiceListVO>>

    /**
     * 收藏服务
     */
    fun updateCollected(userId: String, serviceId: Long, collector: Boolean): Result<Boolean>

    /**
     * 创建服务
     */
    fun createService(userId: String, serviceCreateInfo: ServiceCreateInfo): Result<OPPServiceVO>

    /**
     * 删除服务
     */
    fun deleteService(userId: String, serviceId: Long): Result<Boolean>

    /**
     * 更新服务信息
     */
    fun updateService(userId: String, serviceId: Long, serviceCreateInfo: ServiceCreateInfo): Result<Boolean>

    /**
     * 读取指定服务的信息
     */
    fun getService(userId: String, serviceId: Long): Result<ServiceVO>

    /**
     * 同步将服务类别下的服务注册进来
     */
    fun syncService(userId: String, services: List<ServiceListVO>)
}