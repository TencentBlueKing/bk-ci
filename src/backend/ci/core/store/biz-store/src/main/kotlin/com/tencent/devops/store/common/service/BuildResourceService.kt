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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.store.pojo.container.BuildResource

/**
 * 构建资源逻辑类
 *
 * since: 2018-12-20
 */
interface BuildResourceService {

    /**
     * 获取默认构建资源信息
     */
    fun getDefaultBuildResource(buildType: BuildType): Any?

    /**
     * 获取所有构建资源信息
     */
    fun getAllPipelineBuildResource(): Result<List<BuildResource>>

    /**
     * 根据id获取构建资源信息
     */
    fun getPipelineBuildResource(id: String): Result<BuildResource?>

    /**
     * 根据构建资源编码查询数据库记录数
     */
    fun getCountByCode(buildResourceCode: String): Int

    /**
     * 保存构建资源信息
     */
    fun savePipelineBuildResource(
        defaultFlag: Boolean,
        buildResourceCode: String,
        buildResourceName: String
    ): Result<Boolean>

    /**
     * 更新构建资源信息
     */
    fun updatePipelineBuildResource(
        id: String,
        defaultFlag: Boolean,
        buildResourceCode: String,
        buildResourceName: String
    ): Result<Boolean>

    /**
     * 删除构建资源信息
     */
    fun deletePipelineBuildResource(id: String): Result<Boolean>
}
