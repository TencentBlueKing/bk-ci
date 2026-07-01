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

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreComponentVersionItem
import com.tencent.devops.store.pojo.common.version.StoreShowVersionInfo
import com.tencent.devops.store.pojo.common.version.StoreVersionLogInfo
import com.tencent.devops.store.pojo.common.version.StoreVersionSizeInfo
import com.tencent.devops.store.pojo.common.version.VersionInfo

/**
 * 组件版本相关查询服务(按业务维度从 StoreComponentQueryService 拆分)。
 */
interface StoreComponentVersionQueryService {

    /**
     * 根据组件标识获取组件版本列表
     * @param storeStatusList 版本状态过滤列表，null表示不过滤(返回全部状态，兼容老逻辑)
     */
    @Suppress("LongParameterList")
    fun getComponentVersionsByCode(
        userId: String,
        storeType: String,
        storeCode: String,
        page: Int,
        pageSize: Int,
        checkPermissionFlag: Boolean = true,
        storeStatusList: List<String>? = null
    ): Page<StoreComponentVersionItem>

    /**
     * 根据组件标识获取组件回显版本信息
     */
    fun getComponentShowVersionInfo(
        userId: String,
        storeType: String,
        storeCode: String
    ): StoreShowVersionInfo

    /**
     * 获取组件升级版本信息
     */
    fun getComponentUpgradeVersionInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        projectCode: String = "",
        instanceId: String? = null,
        osName: String? = null,
        osArch: String? = null
    ): VersionInfo?

    fun getStoreUpgradeStatusInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        version: String
    ): Result<String?>

    /**
     * 根据组件id获取组件版本发布日志
     */
    fun getStoreVersionLogs(
        storeCode: String,
        storeType: StoreTypeEnum,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreVersionLogInfo>>

    /**
     * 根据组件Code和版本号获取组件的大小
     */
    fun getStoreVersionSize(
        storeCode: String,
        storeType: StoreTypeEnum,
        version: String,
        osName: String?,
        osArch: String?
    ): StoreVersionSizeInfo

    /**
     * 获取满足条件的组件版本信息
     */
    fun getComponentVersionList(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        storeStatus: StoreStatusEnum? = null
    ): List<VersionInfo>
}
