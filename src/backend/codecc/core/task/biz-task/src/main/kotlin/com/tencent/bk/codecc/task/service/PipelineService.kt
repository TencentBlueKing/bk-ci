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

import com.tencent.bk.codecc.task.model.GongfengPublicProjEntity
import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.pojo.ActiveProjParseModel
import com.tencent.bk.codecc.task.pojo.GongfengPublicProjModel
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO
import com.tencent.bk.codecc.task.vo.BatchRegisterVO
import com.tencent.bk.codecc.task.vo.BuildEnvVO
import com.tencent.bk.codecc.task.vo.RepoInfoVO
import com.tencent.bk.codecc.task.vo.checkerset.ToolCheckerSetVO
import com.tencent.devops.common.api.exception.StreamException
import com.tencent.devops.common.constant.ComConstants

interface PipelineService {

    /**
     * 获取创建流水线参数
     * @return 流水线ID
     */
    fun assembleCreatePipeline(
        registerVO: BatchRegisterVO,
        taskInfoEntity: TaskInfoEntity,
        defaultExecuteTime: String,
        defaultExecuteDate: List<String>,
        userName: String,
        relPath: String
    ): String

    /**
     * 获取代码库库原子
     */
    // fun getCodeElement(registerVO: BatchRegisterVO, relPath: String?): Element

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
    fun updatePipelineTools(
        userName: String,
        taskId: Long,
        toolList: List<String>,
        taskInfoEntity: TaskInfoEntity?,
        updateType: ComConstants.PipelineToolUpdateType,
        registerVO: BatchRegisterVO?,
        relPath: String?
    ): Set<String>

    /**
     * 为工蜂项目创建蓝盾项目
     */
    fun createGongfengDevopsProject(newProject: GongfengPublicProjModel): String

    /**
     * 为工蜂项安装插件
     */
    fun installAtom(userName: String, projectIds: ArrayList<String>, atomCode: String): Boolean

    /**
     * 为活跃项目创建蓝盾项目
     */
    fun createActiveProjDevopsProject(activeProjParseModel: ActiveProjParseModel): String

    /**
     * 为工蜂项目创建流水线
     */
    fun createGongfengDevopsPipeline(
        gongfengPublicProjModel: GongfengPublicProjModel,
        projectId: String
    ): String

    /**
     * 为活跃项目创建流水线
     */
    fun createGongfengActivePipeline(
        activeProjParseModel: ActiveProjParseModel,
        projectId: String,
        taskName: String,
        taskId: Long
    ): String

    /**
     * 启动流水线
     */
    fun startPipeline(
        pipelineId: String,
        projectId: String,
        nameEn: String,
        createFrom: String?,
        toolName: List<String>,
        userName: String
    ): String

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
     * 将devops语言切换为codecc语言，并且自动带其他语言
     */
    @Throws(StreamException::class)
    fun convertDevopsCodeLangToCodeCCWithOthers(codeLang: String): Long?

    /**
     * CodeCC的语言转流水线语言
     *
     * @param codeLang
     * @return
     */
    fun localConvertDevopsCodeLang(codeLang: Long): List<String>

    /**
     * CodeCC的语言转语言字符串
     *
     * @param codeLang
     * @return
     */
    fun convertCodeCCLangToString(codeLang: Long): Set<String>

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
     * 更新定时任务触发信息
     *
     * @param taskInfoEntity
     * @param executeDate
     * @param executeTime
     * @param userName
     */
    fun modifyCodeCCTiming(
        taskInfoEntity: TaskInfoEntity,
        executeDate: List<String>,
        executeTime: String,
        userName: String
    )

    /**
     * 删除定时构建原子
     * @param userName
     * @param createFrom
     * @param pipelineId
     * @param projectId
     */
    fun deleteCodeCCTiming(userName: String, taskInfoEntity: TaskInfoEntity)

    /**
     * 获取代码库详细信息
     */
    fun getRepoDetail(taskInfoEntity: TaskInfoEntity, analyzeConfigInfoVO: AnalyzeConfigInfoVO): AnalyzeConfigInfoVO

    /**
     * 更新任务初始化信息
     */
    fun updateTaskInitStep(
        isFirstTrigger: String?,
        taskInfoEntity: TaskInfoEntity,
        pipelineBuildId: String,
        toolName: String,
        userName: String
    )

    /**
     * 获取编译环境工具列表
     */
    fun getBuildEnv(os: String): List<BuildEnvVO>

    /**
     * 根据蓝盾项目ID列表批量(1W)获取代码库信息
     */
    fun getRepoUrlByBkProjects(projectIds: Set<String>): Map<String, RepoInfoVO>

    /**
     * 修改各工具关联的规则集
     */
    fun updateCheckerSets(
        userName: String,
        projectId: String,
        pipelineId: String,
        taskId: Long,
        checkerSets: List<ToolCheckerSetVO>
    ): Boolean

    /**
     * 更新流水线
     */
    fun updateExistsCommonPipeline(
        gongfengPublicProjEntity: GongfengPublicProjEntity,
        projectId: String,
        taskId: Long,
        pipelineId: String,
        owner: String,
        dispatchRoute: ComConstants.CodeCCDispatchRoute,
        commitId: String? = null
    ): Boolean

    /**
     * 更新流水线
     */
    fun updateExistsCommonPipeline(
        gongfengId: Int,
        url: String,
        projectId: String,
        taskId: Long,
        pipelineId: String,
        owner: String,
        dispatchRoute: ComConstants.CodeCCDispatchRoute,
        commitId: String?
    ): Boolean

    /**
     * 更新流水线代码库配置
     *
     * @param userName
     * @param registerVO
     * @return
     */
    fun updateCodeLibrary(
        userName: String,
        registerVO: BatchRegisterVO,
        taskEntity: TaskInfoEntity
    ): Boolean

    /**
     * 更新流水线插件版本
     */
    fun updatePluginVersion(userId: String, projectId: String, pipelineId: String)
}
