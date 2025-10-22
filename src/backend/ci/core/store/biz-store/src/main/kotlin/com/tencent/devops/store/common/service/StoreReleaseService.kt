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

import com.tencent.devops.store.pojo.common.StoreReleaseInfoUpdateRequest
import com.tencent.devops.store.pojo.common.publication.StoreCreateRequest
import com.tencent.devops.store.pojo.common.publication.StoreCreateResponse
import com.tencent.devops.store.pojo.common.publication.StoreOfflineRequest
import com.tencent.devops.store.pojo.common.publication.StoreProcessInfo
import com.tencent.devops.store.pojo.common.publication.StoreReleaseRequest
import com.tencent.devops.store.pojo.common.publication.StoreUpdateRequest
import com.tencent.devops.store.pojo.common.publication.StoreUpdateResponse

interface StoreReleaseService {

    /**
     * 新增组件
     * @param userId userId
     * @param storeCreateRequest 新增组件请求报文
     * @return 新增组件返回报文
     */
    fun createComponent(
        userId: String,
        storeCreateRequest: StoreCreateRequest
    ): StoreCreateResponse?

    /**
     * 更新组件
     * @param userId userId
     * @param storeUpdateRequest 更新组件请求报文
     * @return 更新组件返回报文
     */
    fun updateComponent(
        userId: String,
        storeUpdateRequest: StoreUpdateRequest
    ): StoreUpdateResponse?

    /**
     * 根据组件ID获取版本发布进度信息
     * @param userId userId
     * @param storeId 组件ID
     * @return 版本发布进度信息
     */
    fun getProcessInfo(userId: String, storeId: String): StoreProcessInfo

    /**
     * 取消发布
     * @param userId userId
     * @param storeId 组件ID
     * @return 布尔值
     */
    fun cancelRelease(userId: String, storeId: String): Boolean

    /**
     * 通过测试
     * @param userId userId
     * @param storeId 组件ID
     * @return 布尔值
     */
    fun passTest(userId: String, storeId: String): Boolean

    /**
     * 填写信息
     * @param userId userId
     * @param storeId 组件ID
     * @param storeReleaseInfoUpdateRequest 组件发布信息修改请求报文
     * @return 布尔值
     */
    fun editReleaseInfo(
        userId: String,
        storeId: String,
        storeReleaseInfoUpdateRequest: StoreReleaseInfoUpdateRequest
    ): Boolean

    /**
     * 处理发布
     * @param userId userId
     * @param storeReleaseRequest 发布请求报文
     * @return 布尔值
     */
    fun handleStoreRelease(
        userId: String,
        storeReleaseRequest: StoreReleaseRequest
    ): Boolean

    /**
     * 下线组件
     * @param userId userId
     * @param storeOfflineRequest 下线组件请求报文
     * @param checkPermissionFlag 是否检查权限
     * @return 布尔值
     */
    fun offlineComponent(
        userId: String,
        storeOfflineRequest: StoreOfflineRequest,
        checkPermissionFlag: Boolean = true
    ): Boolean

    /**
     * 重新构建
     * @param userId userId
     * @param storeId 组件ID
     * @return 布尔值
     */
    fun rebuild(
        userId: String,
        storeId: String
    ): Boolean

    /**
     * 返回上一步
     * @param userId userId
     * @param storeId 组件ID
     * @return 布尔值
     */
    fun back(
        userId: String,
        storeId: String
    ): Boolean
}
