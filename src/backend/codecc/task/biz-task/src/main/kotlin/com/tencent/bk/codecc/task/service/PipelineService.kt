/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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

package com.tencent.bk.codecc.task.service

import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.vo.BatchRegisterVO
import com.tencent.bk.codecc.task.vo.RepoInfoVO
import com.tencent.devops.common.api.exception.StreamException
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.repository.pojo.Repository

interface PipelineService {

    /**
     * 获取创建流水线参数
     *
     * @param registerVO
     * @param taskInfoEntity
     * @param defaultExecuteTime
     * @param defaultExecuteDate
     * @return
     */
    fun assembleCreatePipeline(registerVO: BatchRegisterVO, taskInfoEntity: TaskInfoEntity,
                               defaultExecuteTime: String, defaultExecuteDate: List<String>): Model

    /**
     * 获取代码库库原子
     */
    fun getCodeElement(registerVO: BatchRegisterVO, relPath: String?): Element


    /**
     * 更新流水线工具配置
     *
     * @param userName
     * @param taskId
     * @param toolList
     * @param taskInfoEntity
     * @param updateType
     * @param needUpdateRelPath
     * @param relPath
     * @return
     */
    fun updatePipelineTools(userName: String, taskId: Long, toolList: List<String>, taskInfoEntity: TaskInfoEntity?,
                            updateType: ComConstants.PipelineToolUpdateType, codeElement: Element?): Set<String>


    /**
     * 启动流水线
     */
    fun startPipeline(taskInfoEntity: TaskInfoEntity, toolName: List<String>, userName: String): String


    /**
     * 将devops的语言转换为codecc语言
     *
     * @param codeLang
     * @return
     * @throws StreamException
     */
    @Throws(StreamException::class)
    fun convertDevopsCodeLangToCodeCC(codeLang: String): Long?


    /**
     * 获取代码库路径清单
     *
     * @param projectId
     * @return
     */
    fun getRepositoryList(projCode: String): List<RepoInfoVO>


    /**
     * 获取代码库分支
     *
     * @param projCode
     * @param url
     * @param scmType
     * @return
     */
    fun getRepositoryBranches(projCode: String, url: String, scmType: String): List<String>?

    /**
     * 修改CODECC原子语言, 渠道为CODECC
     *
     * @param taskInfoEntity
     * @param userName
     */
    fun updateBsPipelineLang(taskInfoEntity: TaskInfoEntity, userName: String)


    /**
     * 修改codecc原子语言，渠道为BS
     *
     * @param taskInfoEntity
     * @param userName
     */
    fun updateBsPipelineLangBSChannelCode(taskInfoEntity: TaskInfoEntity, userName: String)

    /**
     * 更新定时任务触发信息
     *
     * @param taskInfoEntity
     * @param executeDate
     * @param executeTime
     * @param userName
     */
    fun modifyCodeCCTiming(taskInfoEntity: TaskInfoEntity, executeDate: List<String>, executeTime: String, userName: String)


    /**
     * 删除定时构建原子
     * @param userName
     * @param createFrom
     * @param pipelineId
     * @param projectId
     */
    fun deleteCodeCCTiming(userName: String, createFrom: String, pipelineId: String, projectId: String)


    /**
     * 获取代码库详细信息
     */
    fun getRepoDetail(projectId: String,
                      repoHashId: String): Repository?

    /**
     * 更新任务初始化信息
     */
    fun updateTaskInitStep(isFirstTrigger: String?, taskInfoEntity: TaskInfoEntity,
                           pipelineBuildId: String, toolName: String, userName: String)

}