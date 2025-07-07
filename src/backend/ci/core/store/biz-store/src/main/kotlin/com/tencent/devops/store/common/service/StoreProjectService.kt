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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.store.pojo.common.InstallStoreReq
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.common.StoreProjectInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.test.StoreTestItem
import com.tencent.devops.store.pojo.common.test.StoreTestRequest

/**
 * store项目通用业务逻辑类
 *
 * since: 2019-03-22
 */
@Suppress("ALL")
interface StoreProjectService {

    /**
     * 根据商店组件标识获取已安装的项目列表
     */
    fun getInstalledProjects(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Result<List<InstalledProjRespItem>>

    /**
     * 安装商店组件
     */
    fun installStoreComponent(
        userId: String,
        storeId: String,
        installStoreReq: InstallStoreReq,
        publicFlag: Boolean,
        channelCode: ChannelCode
    ): Result<Boolean>

    /**
     * 校验安装权限
     */
    fun validateInstallPermission(
        publicFlag: Boolean,
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        projectCodeList: ArrayList<String>,
        channelCode: ChannelCode = ChannelCode.BS
    ): Result<Boolean>

    /**
     * 卸载商店组件
     */
    fun uninstall(
        storeType: StoreTypeEnum,
        storeCode: String,
        projectCode: String,
        instanceIdList: List<String>? = null
    ): Result<Boolean>

    /**
     * 判断组件是否被项目安装
     */
    fun isInstalledByProject(
        projectCode: String,
        storeCode: String,
        storeType: Byte,
        instanceId: String? = null
    ): Boolean

    /**
     * 获取项目下关联的组件信息
     * @return key:storeCode,value:version
     */
    fun getProjectComponents(
        projectCode: String,
        storeType: Byte,
        storeProjectTypes: List<Byte>,
        instanceId: String? = null
    ): Map<String, String?>?

    /**
     * 更新组件初始化项目信息
     */
    fun updateStoreInitProject(userId: String, storeProjectInfo: StoreProjectInfo): Boolean

    /**
     * 保存组件测试信息
     */
    fun saveStoreTestInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        storeTestRequest: StoreTestRequest
    ): Boolean

    /**
     * 获取组件测试信息
     */
    fun getStoreTestInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String
    ): Set<StoreTestItem>
}
