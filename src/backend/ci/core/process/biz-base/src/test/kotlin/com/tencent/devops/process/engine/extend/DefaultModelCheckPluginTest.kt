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

package com.tencent.devops.process.engine.extend

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.process.TestBase
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.config.JobCommonSettingConfig
import com.tencent.devops.process.pojo.config.PipelineCommonSettingConfig
import com.tencent.devops.process.pojo.config.StageCommonSettingConfig
import com.tencent.devops.process.pojo.config.TaskCommonSettingConfig
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.api.atom.ServiceAtomResource
import com.tencent.devops.store.api.atom.ServiceMarketAtomEnvResource
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.pojo.atom.AtomRunInfo
import com.tencent.devops.store.pojo.atom.AtomVersion
import com.tencent.devops.store.pojo.atom.InstalledAtom
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.StoreUserCommentInfo
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultModelCheckPluginTest : TestBase() {

    private val client: Client = mock()
    private val pipelineCommonSettingConfig: PipelineCommonSettingConfig = mock()
    private val stageCommonSettingConfig: StageCommonSettingConfig = mock()
    private val jobCommonSettingConfig: JobCommonSettingConfig = mock()
    private val taskCommonSettingConfig: TaskCommonSettingConfig = mock()
    private val checkPlugin = DefaultModelCheckPlugin(
        client = client,
        pipelineCommonSettingConfig = pipelineCommonSettingConfig,
        stageCommonSettingConfig = stageCommonSettingConfig,
        jobCommonSettingConfig = jobCommonSettingConfig,
        taskCommonSettingConfig = taskCommonSettingConfig
    )
    private val serviceMarketAtomResource: ServiceMarketAtomResource = mock()
    private val serviceAtomResource: ServiceAtomResource = mock()
    private val serviceMarketAtomEnvResource: ServiceMarketAtomEnvResource = mock()

    private fun genAtomVersion() = AtomVersion(
        atomId = "1",
        atomCode = "atomCode",
        name = "name",
        logoUrl = "logoUrl",
        classifyCode = "classifyCode",
        classifyName = "classifyLanName",
        category = "category",
        docsLink = "docsLink",
        htmlTemplateVersion = "htmlTemplateVersion",
        atomType = "atomType",
        jobType = "jobType",
        os = null,
        summary = "summary",
        description = "description",
        version = "version",
        atomStatus = "atomStatus",
        releaseType = null,
        versionContent = "versionContent",
        language = "language",
        codeSrc = "codeSrc",
        publisher = "publisher",
        modifier = "modifier",
        creator = "creator",
        createTime = "2020-01-01 01:01:01",
        updateTime = "2020-01-01 01:01:01",
        flag = false,
        repositoryAuthorizer = null,
        defaultFlag = null,
        projectCode = "",
        initProjectCode = "projectCode",
        labelList = null,
        userCommentInfo = StoreUserCommentInfo(true, ""),
        visibilityLevel = VisibilityLevelEnum.LOGIN_PUBLIC.name,
        privateReason = "privateReason",
        recommendFlag = null,
        frontendType = FrontendTypeEnum.NORMAL,
        // 开启插件yml显示
        yamlFlag = true,
        editFlag = false,
        dailyStatisticList = null
    )

    @BeforeEach
    fun setUp2() {
        whenever(client.get(ServiceMarketAtomResource::class)).thenReturn(serviceMarketAtomResource)
        whenever(
            client.get(ServiceMarketAtomResource::class).getAtomByCode(atomCode = atomCode, username = "")
        ).thenReturn(
            Result(genAtomVersion())
        )

        whenever(client.get(ServiceAtomResource::class)).thenReturn(serviceAtomResource)
        whenever(
            client.get(ServiceAtomResource::class).getInstalledAtoms(projectId)
        ).thenReturn(
            Result(genInstallAtomInfo())
        )
        whenever(client.get(ServiceMarketAtomEnvResource::class)).thenReturn(serviceMarketAtomEnvResource)
        whenever(
            serviceMarketAtomEnvResource.batchGetAtomRunInfos(
                projectCode = any(),
                atomVersions = any()
            )
        ).thenReturn(
            Result(
                mapOf(
                    "manualTrigger:1.*" to AtomRunInfo(
                        atomCode = "manualTrigger",
                        atomName = "手动触发",
                        version = "1.*",
                        initProjectCode = projectId,
                        jobType = JobTypeEnum.AGENT,
                        buildLessRunFlag = false,
                        inputTypeInfos = null
                    )
                )
            )
        )
        whenever(pipelineCommonSettingConfig.maxModelSize).thenReturn(16777215)
        whenever(pipelineCommonSettingConfig.maxStageNum).thenReturn(20)
        whenever(pipelineCommonSettingConfig.maxPipelineNameSize).thenReturn(255)
        whenever(pipelineCommonSettingConfig.maxPipelineDescSize).thenReturn(255)
        whenever(stageCommonSettingConfig.maxJobNum).thenReturn(20)
        whenever(jobCommonSettingConfig.maxTaskNum).thenReturn(20)
        whenever(taskCommonSettingConfig.maxInputNum).thenReturn(50)
        whenever(taskCommonSettingConfig.maxOutputNum).thenReturn(50)
        whenever(taskCommonSettingConfig.maxInputComponentSize).thenReturn(1024)
        whenever(taskCommonSettingConfig.maxTextareaComponentSize).thenReturn(4096)
        whenever(taskCommonSettingConfig.maxCodeEditorComponentSize).thenReturn(16384)
        whenever(taskCommonSettingConfig.maxDefaultInputComponentSize).thenReturn(1024)
        whenever(taskCommonSettingConfig.multipleInputComponents).thenReturn("dynamic-parameter")
        whenever(taskCommonSettingConfig.maxMultipleInputComponentSize).thenReturn(4000)
        whenever(taskCommonSettingConfig.maxDefaultOutputComponentSize).thenReturn(4000)
    }

    private fun genInstallAtomInfo(): List<InstalledAtom> {
        return listOf(
            InstalledAtom(
                atomId = "atomId",
                atomCode = atomCode,
                name = "atomName",
                logoUrl = "logoUrl",
                classifyCode = "classifyCode",
                classifyName = "classifyLanName",
                category = "category",
                summary = "summary",
                publisher = "publisher",
                installer = "installer",
                installTime = "2020-01-01 01:01:01",
                installType = StoreProjectTypeEnum.COMMON.name,
                pipelineCnt = 0,
                hasPermission = true
            )
        )
    }

    @Test
    fun checkTriggerContainer() {
        // trigger
        val triggerStage = genStages(1, 1, 1)
        checkPlugin.checkTriggerContainer(triggerStage[0])
    }

    @Test
    fun beforeDeleteElementInExistsModel() {
        val existsModel = genModel(stageSize = 4, jobSize = 2, elementSize = 2)
        checkPlugin.beforeDeleteElementInExistsModel(
            existsModel, null,
            BeforeDeleteParam(
                userId, projectId, pipelineId
            )
        )
    }

    @Test
    fun clearUpModel() {
        val model = genModel(stageSize = 3, jobSize = 3, elementSize = 0)
        checkPlugin.clearUpModel(model)
    }

    @Test
    fun checkJob() {
        // trigger
        val triggerContainers = genContainers(1, 1, 2)
        checkPlugin.checkJob(triggerContainers[0], projectId, pipelineId, userId, false)
        val allContainers = genContainers(2, 3, 2)
        val mac = allContainers[0]
        checkPlugin.checkJob(mac, projectId, pipelineId, userId, false)
        val win = allContainers[1]
        checkPlugin.checkJob(win, projectId, pipelineId, userId, false)
        val linux = allContainers[2]
        checkPlugin.checkJob(linux, projectId, pipelineId, userId, false)
    }

    private fun checkModelIntegrityEmptyElement() {
        val model = genModel(stageSize = 4, jobSize = 2, elementSize = 0)
        Assertions.assertThrows(ErrorCodeException::class.java) { checkPlugin.checkModelIntegrity(model, projectId) }
    }

    @Test
    fun checkModelIntegrity() {
        try {
            checkModelIntegrityEmptyElement()
        } catch (actual: ErrorCodeException) {
            Assertions.assertEquals(ProcessMessageCode.ERROR_EMPTY_JOB, actual.errorCode)
        }
    }

    @Test
    fun `checkModelJob&ElementSize`() {
        val fulModel = genModel(stageSize = 3, jobSize = 2, elementSize = 2)
        val expect = 2 /* TriggerStage */ + (3 /* stageSize */ * (2 /* JobSize */ * 2 /* element */ + 2 /* Job */))
        try {
            val actualSize = checkPlugin.checkModelIntegrity(fulModel, projectId)
            Assertions.assertEquals(expect, actualSize)
        } catch (actual: ErrorCodeException) {
            Assertions.assertEquals(ProcessMessageCode.ERROR_ATOM_RUN_BUILD_ENV_INVALID, actual.errorCode)
        }
    }
}
