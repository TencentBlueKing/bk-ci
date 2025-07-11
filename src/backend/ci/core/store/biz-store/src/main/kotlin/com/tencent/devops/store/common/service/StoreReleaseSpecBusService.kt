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

package com.tencent.devops.store.common.service

import com.tencent.devops.store.pojo.common.QueryComponentPkgEnvInfoParam
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.publication.StoreCreateRequest
import com.tencent.devops.store.pojo.common.publication.StorePkgEnvInfo
import com.tencent.devops.store.pojo.common.publication.StoreRunPipelineParam
import com.tencent.devops.store.pojo.common.publication.StoreUpdateRequest

interface StoreReleaseSpecBusService {

    /**
     * 执行新增组件请求前置业务
     * @param storeCreateRequest 新增组件请求报文
     */
    fun doStoreCreatePreBus(
        storeCreateRequest: StoreCreateRequest
    )

    /**
     * 执行更新组件请求前置业务
     * @param storeUpdateRequest 更新组件请求报文
     */
    fun doStoreUpdatePreBus(
        storeUpdateRequest: StoreUpdateRequest
    )

    /**
     * 对更新组件请求参数进行国际化转换个性化逻辑
     * @param storeUpdateRequest 更新组件请求报文
     */
    fun doStoreI18nConversionSpecBus(
        storeUpdateRequest: StoreUpdateRequest
    )

    /**
     * 处理检查组件升级参数个性化逻辑
     * @param storeUpdateRequest 更新组件请求报文
     */
    fun doCheckStoreUpdateParamSpecBus(
        storeUpdateRequest: StoreUpdateRequest
    )

    /**
     * 获取组件升级时组件状态
     * @return 组件状态
     */
    fun getStoreUpdateStatus(): StoreStatusEnum

    /**
     * 获取组件运行流水线启动参数
     * @param storeRunPipelineParam 运行流水线参数
     * @return 启动参数
     */
    fun getStoreRunPipelineStartParams(storeRunPipelineParam: StoreRunPipelineParam): MutableMap<String, String>

    /**
     * 获取组件运行流水线组件状态
     * @param buildId 构建ID
     * @param startFlag 启动标识
     * @return 组件状态
     */
    fun getStoreRunPipelineStatus(buildId: String? = null, startFlag: Boolean = true): StoreStatusEnum?

    /**
     * 获取组件包环境信息
     * @param userId 流水线ID
     * @param storeType 组件类型
     * @param storeCode 组件标识
     * @param version 组件版本
     * @param osName 操作系统名称
     * @param osArch 操作系统架构
     * @return 包环境信息列表
     */
    @Suppress("LongParameterList")
    fun getComponentPkgEnvInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        osName: String? = null,
        osArch: String? = null
    ): List<StorePkgEnvInfo>

    /**
     * 获取组件包环境信息
     * @param userId 流水线ID
     * @param storeType 组件类型
     * @param storeCode 组件标识
     * @param version 组件版本
     * @param queryComponentPkgEnvInfoParam 获取组件包环境信息查询参数
     * @return 包环境信息列表
     */
    fun getComponentPkgEnvInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        queryComponentPkgEnvInfoParam: QueryComponentPkgEnvInfoParam
    ): List<StorePkgEnvInfo>

    /**
     * 获取组件发布过程信息列表
     * @param userId 流水线ID
     * @param isNormalUpgrade 是否为普通升级
     * @param status 组件状态
     * @return 发布过程信息列表
     */
    fun getReleaseProcessItems(
        userId: String,
        isNormalUpgrade: Boolean,
        status: StoreStatusEnum
    ): List<ReleaseProcessItem>

    /**
     * 执行组件环境信息业务
     * @param userId 流水线ID
     * @param storeType 组件类型
     * @param storeCode 组件标识
     * @param version 组件版本
     * @param releaseType 发布类型
     */
    fun doStoreEnvBus(
        storeCode: String,
        storeType: StoreTypeEnum,
        version: String,
        userId: String,
        releaseType: ReleaseTypeEnum? = null
    )

    /**
     * 执行新增组件请求后置业务
     */
    fun doStorePostCreateBus(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum
    )
}
