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

package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.store.pojo.common.StoreDockingPlatformInfo
import com.tencent.devops.store.pojo.common.StoreDockingPlatformRequest

interface StoreDockingPlatformService {

    /**
     * 添加对接平台信息
     * @param userId 用户ID
     * @param storeDockingPlatformRequest 请求报文
     * @return 布尔值
     */
    fun create(
        userId: String,
        storeDockingPlatformRequest: StoreDockingPlatformRequest
    ): Boolean

    /**
     * 删除对接平台信息
     * @param userId 用户ID
     * @param id 主键ID
     * @return 布尔值
     */
    fun delete(
        userId: String,
        id: String
    ): Boolean

    /**
     * 更新对接平台信息
     * @param userId 用户ID
     * @param id 主键ID
     * @param storeDockingPlatformRequest 请求报文
     * @return 布尔值
     */
    fun update(
        userId: String,
        id: String,
        storeDockingPlatformRequest: StoreDockingPlatformRequest
    ): Boolean

    /**
     * 获取对接平台信息列表
     * @param userId 用户ID
     * @param platformName 平台名称
     * @param id 平台ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 对接平台信息列表
     */
    fun getStoreDockingPlatforms(
        userId: String,
        platformName: String? = null,
        id: String? = null,
        page: Int = 1,
        pageSize: Int = 10
    ): Page<StoreDockingPlatformInfo>?

    /**
     * 判断对接平台Code是否已注册
     * @param platformCode 平台Code
     */
    fun isPlatformCodeRegistered(platformCode: String): Boolean
}
