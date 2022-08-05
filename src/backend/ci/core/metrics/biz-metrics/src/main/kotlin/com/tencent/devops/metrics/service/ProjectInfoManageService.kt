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
import com.tencent.devops.metrics.pojo.`do`.PipelineErrorTypeInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineLabelInfo
import com.tencent.devops.metrics.pojo.dto.QueryProjectAtomListDTO
import com.tencent.devops.metrics.pojo.dto.QueryProjectPipelineLabelDTO

interface ProjectInfoManageService {

    /**
     * 获取项目下插件列表
     * @param queryProjectInfoDTO 获取项目下信息列表信息传输对象
     */
    fun queryProjectAtomList(queryProjectInfoDTO: QueryProjectAtomListDTO): Page<AtomBaseInfoDO>

    /**
     * 获取项目下流水线标签列表
     */
    fun queryProjectPipelineLabels(queryProjectPipelineLabelDTO: QueryProjectPipelineLabelDTO): Page<PipelineLabelInfo>

    /**
     * 获取项目下流水线异常类型列表
     */
    fun queryPipelineErrorTypes(page: Int, pageSize: Int, keyword: String?): Page<PipelineErrorTypeInfoDO>

    /**
     * 同步流水线标签数据
     * @return 同步的数据量
     */
    fun syncPipelineLabelData(userId: String): Boolean
}
