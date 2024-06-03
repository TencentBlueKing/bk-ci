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

package com.tencent.devops.metrics.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.metrics.pojo.`do`.AtomBaseInfoDO
import com.tencent.devops.metrics.pojo.dto.AtomDisplayConfigDTO
import com.tencent.devops.metrics.pojo.vo.AtomDisplayConfigVO

interface AtomDisplayConfigManageService {

    /**
     * 新增项目下需要展示的插件配置
     * @param atomDisplayConfigDTO 保存项目下展示插件配置传输对象
     * @return 布尔值
     */
    fun addAtomDisplayConfig(
        atomDisplayConfigDTO: AtomDisplayConfigDTO
    ): Boolean

    /**
     * 更新项目下需要展示的插件配置
     * @return 布尔值
     */
    fun deleteAtomDisplayConfig(
        projectId: String,
        userId: String,
        atomCodes: List<AtomBaseInfoDO>
    ): Boolean

    /**
     * 获取项目下需要展示的插件
     * @return 项目下展示插件配置报文
     */
    fun getAtomDisplayConfig(projectId: String, userId: String, keyword: String?): AtomDisplayConfigVO

    /**
     * 获取项目下可供选择展示的插件
     * @return 项目下展示插件配置报文
     */
    fun getOptionalAtomDisplayConfig(
        projectId: String,
        userId: String,
        keyword: String?,
        page: Int,
        pageSize: Int
    ): Page<AtomBaseInfoDO>
}
