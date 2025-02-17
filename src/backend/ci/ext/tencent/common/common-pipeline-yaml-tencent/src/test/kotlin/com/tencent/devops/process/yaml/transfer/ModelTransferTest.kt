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

package com.tencent.devops.process.yaml.transfer

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.CommonPipelineAutoConfiguration
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.transfer.ElementInsertBody
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.process.pojo.classify.PipelineLabel
import com.tencent.devops.process.yaml.pojo.TemplatePath
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.transfer.aspect.IPipelineTransferAspect
import com.tencent.devops.process.yaml.transfer.aspect.IPipelineTransferAspectTrigger
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectLoader
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectWrapper
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferJoinPoint
import com.tencent.devops.process.yaml.transfer.inner.TransferCreator
import com.tencent.devops.process.yaml.transfer.inner.TransferCreatorImpl
import com.tencent.devops.process.yaml.transfer.pojo.ModelTransferInput
import com.tencent.devops.process.yaml.transfer.pojo.YamlTransferInput
import com.tencent.devops.process.yaml.v3.models.IPreTemplateScriptBuildYamlParser
import com.tencent.devops.process.yaml.v3.models.job.JobRunsOnPoolType
import com.tencent.devops.process.yaml.v3.parsers.template.YamlTemplate
import com.tencent.devops.process.yaml.v3.parsers.template.YamlTemplateConf
import com.tencent.devops.process.yaml.v3.parsers.template.models.GetTemplateParam
import com.tencent.devops.repository.pojo.CodeGitRepository
import io.mockk.every
import io.mockk.mockk
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.LinkedList
import org.json.JSONObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [SpringContextUtil::class, CommonConfig::class, CommonPipelineAutoConfiguration::class])
@Disabled
@Suppress("MaxLineLength")
internal class ModelTransferTest : BkCiAbstractTest() {
    private val client: Client = mockk()
    private val transferCache: TransferCacheService = mockk()
    private val creator: TransferCreator = TransferCreatorImpl(transferCache)
    private val triggerTransfer: TriggerTransfer = TriggerTransfer(
        client = client, creator = creator, transferCache = transferCache
    )
    private val dispatchTransfer: DispatchTransfer = TXDispatchTransfer(
        client = client, objectMapper = objectMapper, inner = creator
    )
    private val modelContainer: ContainerTransfer = ContainerTransfer(
        client = client,
        objectMapper = objectMapper,
        transferCache = transferCache,
        dispatchTransfer = dispatchTransfer,
        inner = creator
    )
    private val elementTransfer: ElementTransfer = ElementTransfer(
        client = client, creator = creator, transferCache = transferCache, triggerTransfer = triggerTransfer
    )

    private val yamlIndexService: YamlIndexService = YamlIndexService(dispatchTransfer, elementTransfer)
    private val variableTransfer: VariableTransfer = VariableTransfer()

    private val stageTransfer: StageTransfer = StageTransfer(
        client = client,
        objectMapper = objectMapper,
        containerTransfer = modelContainer,
        elementTransfer = elementTransfer,
        variableTransfer = variableTransfer,
        transferCacheService = transferCache,
        transferCreator = creator
    )
    private val modelTransfer: ModelTransfer = ModelTransfer(
        client = client,
        modelStage = stageTransfer,
        elementTransfer = elementTransfer,
        variableTransfer = variableTransfer,
        transferCache = transferCache
    )

    private val pipelineInfo = PipelineInfo(
        projectId = "",
        pipelineId = "",
        templateId = "",
        pipelineName = "",
        pipelineDesc = "",
        version = 1,
        createTime = 1,
        updateTime = 1,
        creator = "",
        lastModifyUser = "",
        channelCode = ChannelCode.BS,
        canManualStartup = true,
        canElementSkip = true,
        taskCount = 1,
        versionName = "",
        id = 1,
        viewNames = emptyList()
    )

    @BeforeEach
    fun setUp() {
        every {
            transferCache.getAtomDefaultValue(any())
        }.returns(JSONObject())
        every {
            transferCache.getAtomDefaultValue("CodeccCheckAtomDebug@4.*")
        }.returns("""{
    "beAutoLang" : false,
    "languages" : "",
    "checkerSetType" : "normal",
    "tools" : "",
    "asyncTask" : false,
    "asyncTaskId" : "",
    "goPath" : "",
    "pyVersion" : "py3",
    "bk_atom_del_hook_url_method" : "DELETE",
    "scriptType" : "SHELL",
    "script" : "# Coverity/Klocwork将通过调用编译脚本来编译您的代码，以追踪深层次的缺陷\n# 请使用依赖的构建工具如maven/cmake等写一个编译脚本build.sh\n# 确保build.sh能够编译代码\n# cd path/to/build.sh\n# sh build.sh",
    "languageRuleSetMap" : "{}",
    "C_CPP_RULE" : [],
    "checkerSetEnvType" : "prod",
    "multiPipelineMark" : "",
    "rtxReceiverType" : "1",
    "rtxReceiverList" : [],
    "botWebhookUrl" : "",
    "botRemindRange" : "2",
    "botRemindSeverity" : "7",
    "botRemaindTools" : [],
    "emailReceiverType" : "1",
    "emailReceiverList" : [],
    "emailCCReceiverList" : [],
    "instantReportStatus" : "2",
    "reportDate" : [],
    "reportTime" : "",
    "reportTools" : [],
    "toolScanType" : "1",
    "diffBranch" : "",
    "byFile" : false,
    "mrCommentEnable" : true,
    "prohibitIgnore" : false,
    "newDefectJudgeFromDate" : "",
    "transferAuthorList" : [],
    "path" : [],
    "customPath" : [],
    "scanTestSource" : false,
    "openScanPrj" : false,
    "issueSystem" : "TAPD",
    "issueSubSystem" : "",
    "issueResolvers" : [],
    "issueReceivers" : [],
    "issueFindByVersion" : "",
    "maxIssue" : "1000",
    "issueAutoCommit" : false,
    "issueTools" : [],
    "issueSeverities" : []
  }""".let { JSONObject(JsonUtil.to(it, object : TypeReference<Map<String, Any>>() {})) })
        every {
            transferCache.getAtomDefaultValue("manualReviewUserTask@1.*")
        }.returns(JSONObject())
        every {
            transferCache.getDockerResource(any(), any(), any())
        }.returns(null)
        every {
            transferCache.getAtomDefaultValue("checkout@1.*")
        }.returns("""{
    "repositoryType" : "ID",
    "repositoryHashId" : "",
    "repositoryName" : "",
    "repositoryUrl" : "",
    "authType" : "TICKET",
    "persistCredentials" : true,
    "pullType" : "BRANCH",
    "refName" : "master",
    "localPath" : "",
    "strategy" : "REVERT_UPDATE",
    "fetchDepth" : "",
    "fetchOnlyCurrentRef" : false,
    "enableFetchRefSpec" : false,
    "fetchRefSpec" : "",
    "enablePartialClone" : false,
    "includePath" : "",
    "excludePath" : "",
    "cachePath" : "",
    "enableGitLfs" : true,
    "enableSubmodule" : true,
    "submodulePath" : "",
    "enableSubmoduleRemote" : false,
    "enableSubmoduleRecursive" : true,
    "enableVirtualMergeBranch" : true,
    "enableGitClean" : true,
    "enableGitCleanIgnore" : true,
    "autoCrlf" : "false",
    "enableTrace" : false
  }""".let { JSONObject(JsonUtil.to(it, object : TypeReference<Map<String, Any>>() {})) })

        every {
            transferCache.getAtomDefaultValue("jobExecuteScriptForOA@1.*")
        }.returns("""{
    "scriptType" : "1"
  }""".let { JSONObject(JsonUtil.to(it, object : TypeReference<Map<String, Any>>() {})) })

        every {
            transferCache.getStoreImageDetail(any(), any(), any())
        }.returns(null)

        every {
            transferCache.getProjectGroupAndUsers(any())
        }.returns(
            listOf(
                BkAuthGroupAndUserList(
                    displayName = "管理员",
                    roleId = 1,
                    roleName = "manager",
                    userIdList = emptyList(),
                    type = ""
                ),
                BkAuthGroupAndUserList(
                    displayName = "开发人员",
                    roleId = 2,
                    roleName = "developer",
                    userIdList = emptyList(),
                    type = ""
                )
            )
        )

        every {
            transferCache.getPipelineLabel(any(), any())
        }.returns(
            listOf(
                PipelineGroup(
                    id = "qweqwe",
                    projectId = "a",
                    name = "a",
                    createTime = 1,
                    updateTime = 1,
                    createUser = "superD",
                    updateUser = "superD",
                    labels = listOf(
                        PipelineLabel(
                            id = "a1",
                            groupId = "aa",
                            name = "标签A",
                            createTime = 1,
                            uptimeTime = 1,
                            createUser = "b",
                            updateUser = "b"
                        ),
                        PipelineLabel(
                            id = "a2",
                            groupId = "aa",
                            name = "标签B",
                            createTime = 1,
                            uptimeTime = 1,
                            createUser = "b",
                            updateUser = "b"
                        )
                    )
                )
            )
        )

        every {
            transferCache.getGitRepository(any(), any(), any())
        }.returns(
            CodeGitRepository(
                aliasName = "aliasName/xxx",
                url = "https://git.com/XXX/XXX.git",
                credentialId = "credentialId",
                projectName = "projectName",
                userName = "userName",
                projectId = "projectId",
                repoHashId = "repoHashId",
                gitProjectId = 0
            )
        )

        every {
            transferCache.getPipelineRemoteToken(any(), any(), any())
        }.returns(
            "PipelineRemoteToken"
        )

        every {
            transferCache.getThirdPartyAgent(JobRunsOnPoolType.ENV_ID, any(), any(), any())
        }.returns(
            "构建机环境别名"
        )

        every {
            transferCache.getThirdPartyAgent(JobRunsOnPoolType.AGENT_ID, any(), any(), any())
        }.returns(
            "构建机节点别名"
        )
        ReflectionTestUtils.setField(creator, "marketRunTaskData", true)
        ReflectionTestUtils.setField(creator, "runPlugInAtomCodeData", "run")
        ReflectionTestUtils.setField(creator, "runPlugInVersionData", "1.*")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "model-yaml-001",
            "model-yaml-002"
        ]
    )
    fun model2Yaml(value: String) {
        val file = testReadResourceFile("transfer/$value/model.json")
        val yamlV3 = testReadResourceFile("transfer/$value/yamlV3.yaml")
        val modelAndSetting = JsonUtil.to(file, object : TypeReference<PipelineModelAndSetting>() {})

        val invalidElement = mutableListOf<String>()
        val defaultAspects = PipelineTransferAspectLoader.checkInvalidElement(invalidElement)
        PipelineTransferAspectLoader.sharedEnvTransfer(defaultAspects)
        val watcher = Watcher(id = "yaml and model transfer watcher")
        watcher.start("step_1|FULL_MODEL2YAML V3 start")
        val yml = modelTransfer.model2yaml(
            ModelTransferInput(
                userId = "test",
                model = modelAndSetting.model,
                setting = modelAndSetting.setting,
                pipelineInfo = pipelineInfo,
                version = YamlVersion.V3_0,
                aspectWrapper = PipelineTransferAspectWrapper(defaultAspects)
            )
        )
        if (!invalidElement.isNullOrEmpty()) {
            println(invalidElement)
            throw PipelineTransferException(
                CommonMessageCode.ELEMENT_NOT_SUPPORT_TRANSFER,
                arrayOf(invalidElement.joinToString("\n"))
            )
        }
        val newYaml = TransferMapper.toYaml(yml)
        Assertions.assertEquals(newYaml, TransferMapper.toYaml(TransferMapper.to(yamlV3)))
//        v2 暂不支持跳过检查
        /*val yamlV2 = testReadResourceFile("transfer/$value/yamlV2.yaml")
                watcher.start("step_2|FULL_MODEL2YAML V2 start")
                val ymlV2 = modelTransfer.model2yaml(
                    ModelTransferInput(
                        "test",
                        modelAndSetting.model,
                        modelAndSetting.setting,
                        YamlVersion.Version.V2_0
                    )
                )
                val newYamlV2 = TransferMapper.toYaml(ymlV2)
                Assertions.assertEquals(newYamlV2, TransferMapper.toYaml(TransferMapper.to(yamlV2)))
                watcher.stop()*/
        println(watcher.toString())
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "yaml-model-004-v3",
            "yaml-model-003-v3-template",
            "yaml-model-002-v3",
            "yaml-model-001-v3",
            "yaml-model-001-v2"
        ]
    )
    fun yaml2model(value: String) {
        val modelFile = testReadResourceFile("transfer/$value/model.json")
        val yaml = testReadResourceFile("transfer/$value/yaml.yaml")

        val watcher = Watcher(id = "yaml and model transfer watcher")
        watcher.start("parse PreScriptBuildYaml")
        val pYml = YamlUtil.getObjectMapper().readValue(
            yaml,
            object : TypeReference<IPreTemplateScriptBuildYamlParser>() {}
        )
        val aspects: LinkedList<IPipelineTransferAspect> = LinkedList()
        aspects.add(
            object : IPipelineTransferAspectTrigger {
                override fun before(jp: PipelineTransferJoinPoint): Any? {
                    if (jp.yamlTriggerOn() != null && jp.yamlTriggerOn()!!.repoName == null) {
                        jp.yamlTriggerOn()!!.repoName = "test/before"
                    }
                    return null
                }
            }
        )
        PipelineTransferAspectLoader.sharedEnvTransfer(aspects)
        watcher.start("normalize Yaml")
        pYml.replaceTemplate { templateFilter ->
            YamlTemplate(
                yamlObject = templateFilter,
                filePath = TemplatePath("TEMPLATE_ROOT_FILE"),
                extraParameters = this,
                getTemplateMethod = ::getTemplate,
                nowRepo = null,
                repo = null,
                resourcePoolMapExt = null,
                conf = YamlTemplateConf(
                    useOldParametersExpression = false // todo
                )
            ).replace()
        }
        watcher.start("yaml2Model")
        val input = YamlTransferInput(
            userId = "testUser",
            projectCode = "testProject",
            pipelineInfo = pipelineInfo,
            yaml = pYml,
            aspectWrapper = PipelineTransferAspectWrapper(aspects),
            yamlFileName = ""
        )
        val model = modelTransfer.yaml2Model(input)
        val setting = modelTransfer.yaml2Setting(input)
        watcher.start("last")
        val newModel = JsonUtil.toJson(PipelineModelAndSetting(model, setting))
        val compare = JsonUtil.toJson(JsonUtil.to(modelFile))
        if (!JSONObject(newModel).similar(JSONObject(compare))) {
            Assertions.assertEquals(newModel, compare)
        }
        watcher.stop()
        println(watcher.toString())
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "yaml,341,21 -> yaml-step",
            "yaml,459,24 -> yaml-job",
            "yaml,219,14 -> yaml-stage",
            "yaml,600,12 -> yaml-setting",
            "yaml,30,14 -> yaml-setting-2",
            "yaml_very_simple,1,1 -> yaml_very_simple-step",
            "yaml_muti_line,16,20 -> yaml_muti_line-step"
        ]
    )
    fun yamlElementInsert(value: String) {
        val (input, output) = value.split(" -> ")
        val (yamlPath, line, column) = input.split(",")
        val yaml = testReadResourceFile("transfer/yaml-element-insert-V3/$yamlPath.yaml")
        val compare = testReadResourceFile("transfer/yaml-element-insert-V3/$output.yaml")
        val res = yamlIndexService.modelTaskInsert(
            "testuesr", "test", "p-test", line.toInt(), column.toInt(), ElementInsertBody(
                yaml, MarketBuildAtomElement("yamlElementInsert test"), ElementInsertBody.ElementInsertType.INSERT
            )
        )
        println(res.mark)
        Assertions.assertEquals(compare, res.yaml)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "yaml,341,21 -> yaml-1",
            "yaml,381,20 -> yaml-2",
            "yaml,173,29 -> yaml-3",
            "yaml,340,12 -> yaml-4",
            "yaml,30,14 -> PipelineTransferException-ElementUpdateWrongPath"
        ]
    )
    fun yamlElementUpdate(value: String) {
        val (input, output) = value.split(" -> ")
        val (yamlPath, line, column) = input.split(",")
        val yaml = testReadResourceFile("transfer/yaml-element-update-V3/$yamlPath.yaml")
        val compare = testReadResourceFile("transfer/yaml-element-update-V3/$output.yaml")
        val res = kotlin.runCatching {
            yamlIndexService.modelTaskInsert(
                "testuesr", "test", "p-test", line.toInt(), column.toInt(), ElementInsertBody(
                    yaml, MarketBuildAtomElement("yamlElementInsert test"), ElementInsertBody.ElementInsertType.UPDATE
                )
            )
        }.onFailure {
            if (output.contains("PipelineTransferException")) {
                val error = it as PipelineTransferException
                Assertions.assertEquals("${it::class.simpleName}-${error.errorCode}", output)
                return
            }
            throw it
        }.getOrNull()
        println(res?.mark)
        Assertions.assertEquals(compare, res?.yaml)
    }

    fun getTemplate(param: GetTemplateParam<Any>): String {
        return testReadResourceFile("transfer/templates/${param.path.path}")
    }

    private fun testReadResourceFile(resourceName: String): String {
        val inputStream = javaClass.classLoader.getResourceAsStream(resourceName) ?: return ""
        val reader = BufferedReader(InputStreamReader(inputStream))
        return reader.readText().replace("\r\n", "\n")
    }
}
