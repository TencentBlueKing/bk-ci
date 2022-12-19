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

import com.tencent.devops.store.pojo.common.StorePkgRunEnvInfo
import com.tencent.devops.store.pojo.common.StorePkgRunEnvRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum

interface StorePkgRunEnvInfoService {

    /**
     * 添加安装包运行时环境信息
     * @param userId userId
     * @param storePkgRunEnvRequest 安装包运行时环境信息请求报文
     * @return 布尔值
     */
    fun create(
        userId: String,
        storePkgRunEnvRequest: StorePkgRunEnvRequest
    ): Boolean

    /**
     * 更新安装包运行时环境信息
     * @param userId userId
     * @param id 主键ID
     * @param storePkgRunEnvRequest 安装包运行时环境信息请求报文
     * @return 布尔值
     */
    fun update(
        userId: String,
        id: String,
        storePkgRunEnvRequest: StorePkgRunEnvRequest
    ): Boolean

    /**
     * 更新安装包运行时环境信息
     * @param userId userId
     * @param id 主键ID
     * @return 布尔值
     */
    fun delete(
        userId: String,
        id: String
    ): Boolean

    /**
     * 获取安装包运行时环境信息
     * @param userId userId
     * @param storeType 组件类型
     * @param language 开发语言
     * @param osName 支持的操作系统名称
     * @param osArch 支持的操作系统架构
     * @param runtimeVersion 运行时版本
     * @return 安装包运行时环境信息
     */
    fun getStorePkgRunEnvInfo(
        userId: String,
        storeType: StoreTypeEnum,
        language: String,
        osName: String,
        osArch: String,
        runtimeVersion: String
    ): StorePkgRunEnvInfo?
}
