package com.tencent.devops.process.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.mock
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.DispatchSubInfoRegisterLoader
import com.tencent.devops.common.pipeline.DispatchSubTypeRegisterLoader
import com.tencent.devops.common.pipeline.ElementSubTypeRegisterLoader
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.store.StoreImageHelper
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.JobPipelineExportV2YamlConflictMapBaseItem
import com.tencent.devops.process.pojo.MarketBuildAtomElementWithLocation
import com.tencent.devops.process.pojo.PipelineExportContext
import com.tencent.devops.process.pojo.PipelineExportInfo
import com.tencent.devops.process.pojo.PipelineExportV2YamlConflictMapItem
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.pipelineExport.ExportCondition
import com.tencent.devops.process.service.pipelineExport.ExportStepRun
import com.tencent.devops.process.service.pipelineExport.TXPipelineExportService
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.store.pojo.atom.GetRelyAtom
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class TXPipelineExportServiceTest {
    private val stageTagService: StageTagService = mock()
    private val pipelineGroupService: PipelineGroupService = mock()
    private val pipelinePermissionService: PipelinePermissionService = mock()
    private val pipelineRepositoryService: PipelineRepositoryService = mock()
    private val storeImageHelper: StoreImageHelper = mock()
    private val scmProxyService: ScmProxyService = mock()
    private val client: Client = mock()

    private val txPipelineExportService = TXPipelineExportService(
        stageTagService = stageTagService,
        pipelineGroupService = pipelineGroupService,
        pipelinePermissionService = pipelinePermissionService,
        pipelineRepositoryService = pipelineRepositoryService,
        storeImageHelper = storeImageHelper,
        scmProxyService = scmProxyService,
        client = client
    )

    private val defaultExportInfo = PipelineExportInfo(
        userId = "test_user",
        pipelineInfo = PipelineInfo(
            projectId = "test_project",
            pipelineId = "pipelineId",
            templateId = "templateId",
            pipelineName = "pipelineName",
            pipelineDesc = "pipelineDesc",
            version = 0,
            createTime = 0,
            updateTime = 0,
            creator = "creator",
            lastModifyUser = "lastModifyUser",
            channelCode = ChannelCode.BS,
            canManualStartup = false,
            canElementSkip = false,
            taskCount = 0,
            versionName = "versionName",
            id = 0
        ),
        model = Model(name = "test", desc = "desc", stages = emptyList()),
        stageTags = emptyList(),
        labels = emptyList(),
        isGitCI = false,
        exportFile = true,
        getImageNameAndCredentials = this::getImageNameAndCredentials,
        getAtomRely = this::getAtomRely,
        getRepoInfo = this::getRepoInfo
    )

    private val defaultContext = PipelineExportContext()

    @Test
    fun doParseModel() {
        val baseModel = getStrFromResource("model.json")
        ElementSubTypeRegisterLoader.registerElement(null)
        DispatchSubTypeRegisterLoader.registerType()
        DispatchSubInfoRegisterLoader.registerInfo()

        val res = txPipelineExportService.doParseModel(
            defaultExportInfo.copy(model = JsonUtil.to(baseModel, Model::class.java)),
            PipelineExportContext()
        )
        println(res.first)
    }

    private fun getImageNameAndCredentials(
        userId: String,
        projectId: String,
        pipelineId: String,
        dispatchType: StoreDispatchType
    ): Pair<String, String?> = Pair("test", "test")

    private fun getAtomRely(elementInfo: GetRelyAtom): Map<String, Map<String, Any>>? = null

    private fun getRepoInfo(
        projectId: String,
        repositoryConfig: RepositoryConfig
    ): Repository? = null

    @Test
    fun testReplaceMapWithDoubleCurlybraces1() {
        val inputMap: MutableMap<String, Any> = mutableMapOf()

        val resultMap = ExportCondition.replaceMapWithDoubleCurlyBraces(
            defaultExportInfo,
            defaultContext.initAll(),
            inputMap = inputMap,
            pipelineExportV2YamlConflictMapItem = PipelineExportV2YamlConflictMapItem()
        )
        val result = jacksonObjectMapper().writeValueAsString(resultMap)
        Assertions.assertEquals(result, "null")
    }

    @Test
    fun testParseSetEnv() {
        val script = """
            setEnv "entryJs" ${'$'}(sed -n "1p" dist/entryfile.txt)
            setEnv "entryCss" ${'$'}(sed -n "2p" dist/entryfile.txt)
        """.trimIndent()
        val compare = """
            echo "::set-output name=entryJs::${'$'}(sed -n "1p" dist/entryfile.txt)"
            echo "::set-output name=entryCss::${'$'}(sed -n "2p" dist/entryfile.txt)"
        """.trimIndent()
        val res = ExportStepRun.parseSetEnv(script)
        Assertions.assertEquals(res, compare)
    }

    @Test
    fun testReplaceMapWithDoubleCurlybraces2() {
        val inputMap = mutableMapOf(
            "key1" to "value" as Any,
            "key2" to "\${{haha}}" as Any,
            "key3" to "abcedf\${{haha}}hijklmn" as Any,
            "key4" to "aaaaaa\${{haha}}hijklmn\${{aaaa}}" as Any,
            "key5" to "\${{123456}}aaaaaa\${{haha}}hijklmn\${{aaaa}}" as Any,
            "\${{key}}" to "\${{123456}}aaaaaa\${{haha}}hijklmn\${{aaaa}}" as Any
        )
        val variables = mapOf("haha" to "value")
        val output2Elements = mutableMapOf(
            "aaaa" to
                mutableListOf(
                    MarketBuildAtomElementWithLocation(
                        stageLocation = null,
                        jobLocation = JobPipelineExportV2YamlConflictMapBaseItem(
                            jobId = "job_1",
                            id = null,
                            name = null
                        ),
                        stepAtom = MarketBuildAtomElement(
                            name = "名称",
                            id = "stepId"
                        )
                    )
                )
        )
        val resultMap = ExportCondition.replaceMapWithDoubleCurlyBraces(
            defaultExportInfo,
            defaultContext.initAll().apply {
                this.variables = variables
                this.output2Elements = output2Elements
            },
            inputMap = inputMap,
            pipelineExportV2YamlConflictMapItem = PipelineExportV2YamlConflictMapItem()
        )
        val result = jacksonObjectMapper().writeValueAsString(resultMap)
        println(result)
        Assertions.assertEquals(
            result,
            "{\"key1\":\"value\",\"key2\":\"\${{ variables.haha }}\",\"key3\":\"abcedf\${{ variables.haha }}" +
                "hijklmn\",\"key4\":\"aaaaaa\${{ variables.haha }}hijklmn\${{ jobs.job_1.steps.stepId.outputs.aaaa " +
                "}}\",\"key5\":\"\${{ 123456 }}aaaaaa\${{ variables.haha }}hijklmn\${{ jobs.job_1.steps.stepId." +
                "outputs.aaaa }}\",\"\${{key}}\":\"\${{ 123456 }}aaaaaa\${{ variables.haha }}hijklmn" +
                "\${{ jobs.job_1.steps.stepId.outputs.aaaa }}\"}"
        )
    }

    @Test
    fun testReplaceMapWithDoubleCurlybraces3() {
        val inputMap = mutableMapOf(
            "key1" to "value" as Any,
            "key2" to listOf(
                "\${{haha}}",
                "abcedf\${{haha}}hijklmn",
                "\${{123456}}aaaaaa\${{haha}}hijklmn\${{aaaa}}",
                123
            ) as Any
        )
        val variables = mapOf("haha" to "value")
        val output2Elements = mutableMapOf(
            "aaaa" to mutableListOf(
                MarketBuildAtomElementWithLocation(
                    stageLocation = null,
                    jobLocation = JobPipelineExportV2YamlConflictMapBaseItem(jobId = "job_1", id = null, name = null),
                    stepAtom = MarketBuildAtomElement(
                        name = "名称",
                        id = "stepId"
                    )
                )
            )
        )
        val resultMap = ExportCondition.replaceMapWithDoubleCurlyBraces(
            defaultExportInfo,
            defaultContext.initAll().apply {
                this.variables = variables
                this.output2Elements = output2Elements
            },
            inputMap = inputMap,
            pipelineExportV2YamlConflictMapItem = PipelineExportV2YamlConflictMapItem()
        )
        println(resultMap)
        val result = jacksonObjectMapper().writeValueAsString(resultMap)
        Assertions.assertEquals(
            result,
            "{\"key1\":\"value\",\"key2\":[\"\${{ variables.haha }}\"," +
                "\"abcedf\${{ variables.haha }}hijklmn\",\"\${{ 123456 }}aaaaaa" +
                "\${{ variables.haha }}hijklmn\${{ jobs.job_1.steps.stepId.outputs.aaaa }}\",123]}"
        )
    }

    @Test
    fun testReplaceMapWithDoubleCurlybraces4() {
        val inputString = "# 您可以通过setEnv函数设置插件间传递的参数\n" +
            "# setEnv \"FILENAME\" \"package.zip\"\n" +
            "# 然后在后续的插件的表单中使用\${{FILENAME}}引用这个变量\n" +
            "\n" +
            "# 您可以在质量红线中创建自定义指标，然后通过setGateValue函数设置指标值\n" +
            "# setGateValue \"CodeCoverage\" \$myValue\n" +
            "# 然后在质量红线选择相应指标和阈值。若不满足，流水线在执行时将会被卡住\n" +
            "\n" +
            "# cd \${{WORKSPACE}} 可进入当前工作空间目录\n" +
            "\n" +
            "set -x\n" +
            "\n" +
            "# 编译镜像\n" +
            "setEnv      \"compile_img_str\"      \"trpc-golang-compile:0.1.2:tlinux:common\"        \n" +
            "# 运行镜像\n" +
            "setEnv \"img_str\" \"trpc-golang-runtime:0.1.0\"\n" +
            "setEnv img_str2 trpc-golang-runtime:0.1.1\n" +
            "# something\n" +
            "setEnv \"TestDir\" \"src/go-test\"\n" +
            "# something\n" +
            "rm \${{TestDir}} -rf\n" +
            "\n" +
            "setEnv \"user\" \${{default_user}}"
        val variables = mapOf("haha" to "value")
        val output2Elements = mutableMapOf(
            "aaaa" to mutableListOf(
                MarketBuildAtomElementWithLocation(
                    stageLocation = null,
                    jobLocation = null,
                    stepAtom = MarketBuildAtomElement(
                        name = "名称",
                        id = "stepId"
                    )
                )
            )
        )
        val resultMap = ExportStepRun.formatScriptOutput(
            defaultExportInfo,
            defaultContext.initAll(),
            script = inputString,
            pipelineExportV2YamlConflictMapItem = PipelineExportV2YamlConflictMapItem()
        )
        val result = jacksonObjectMapper().writeValueAsString(resultMap)
        println(result)
        Assertions.assertEquals(
            resultMap,
            "# 您可以通过setEnv函数设置插件间传递的参数\n# echo \"::set-output " +
                "name=FILENAME::package.zip\"\n# 然后在后续的插件的表单中使用\${{ FILENAME }}引用这个变量\n#" +
                " 您可以在质量红线中创建自定义指标，然后通过setGateValue函数设置指标值\n# setGateValue \"CodeCoverage\" " +
                "\$myValue\n# 然后在质量红线选择相应指标和阈值。若不满足，流水线在执行时将会被卡住\n# cd \${{ ci.workspace }} " +
                "可进入当前工作空间目录\nset -x\n# 编译镜像\necho " +
                "\"::set-output name=compile_img_str::trpc-golang-compile" +
                ":0.1.2:tlinux:common\"\n# 运行镜像\necho \"::set-output" +
                " name=img_str::trpc-golang-runtime:0.1.0\"\necho " +
                "\"::set-output name=img_str2::trpc-golang-runtime:0.1.1\"\n# something\necho \"::set-output " +
                "name=TestDir::src/go-test\"\n# something\nrm \${{ TestDir }} -rf\necho \"::set-output " +
                "name=user::\${{ default_user }}\""
        )
    }

    private fun getStrFromResource(testYaml: String): String {
        val classPathResource = ClassPathResource(testYaml)
        val inputStream: InputStream = classPathResource.inputStream
        val isReader = InputStreamReader(inputStream)

        val reader = BufferedReader(isReader)
        val sb = StringBuffer()
        var str: String?
        while (reader.readLine().also { str = it } != null) {
            sb.append(str).append("\n")
        }
        inputStream.close()
        return sb.toString()
    }
}
