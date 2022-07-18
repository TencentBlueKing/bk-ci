package com.tencent.devops.process.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.mock
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.store.StoreImageHelper
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.JobPipelineExportV2YamlConflictMapBaseItem
import com.tencent.devops.process.pojo.MarketBuildAtomElementWithLocation
import com.tencent.devops.process.pojo.PipelineExportV2YamlConflictMapItem
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.scm.ScmProxyService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

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

    @Test
    fun testReplaceMapWithDoubleCurlybraces1() {
        val inputMap: MutableMap<String, Any> = mutableMapOf()

        val resultMap = txPipelineExportService.replaceMapWithDoubleCurlyBraces(
            inputMap = inputMap,
            output2Elements = mutableMapOf(),
            variables = mutableMapOf(),
            outputConflictMap = mutableMapOf(),
            pipelineExportV2YamlConflictMapItem = PipelineExportV2YamlConflictMapItem(),
            exportFile = true
        )
        val result = jacksonObjectMapper().writeValueAsString(resultMap)
        Assertions.assertEquals(result, "null")
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
        val resultMap = txPipelineExportService.replaceMapWithDoubleCurlyBraces(
            inputMap = inputMap,
            output2Elements = output2Elements,
            variables = variables,
            outputConflictMap = mutableMapOf(),
            pipelineExportV2YamlConflictMapItem = PipelineExportV2YamlConflictMapItem(),
            exportFile = true
        )
        val result = jacksonObjectMapper().writeValueAsString(resultMap)
        println(result)
        Assertions.assertEquals(
            result, "{\"key1\":\"value\",\"key2\":\"\${{ variables.haha }}\",\"key3\":\"abcedf\${{ variables.haha }}" +
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
        val resultMap = txPipelineExportService.replaceMapWithDoubleCurlyBraces(
            inputMap = inputMap,
            output2Elements = output2Elements,
            variables = variables,
            outputConflictMap = mutableMapOf(),
            pipelineExportV2YamlConflictMapItem = PipelineExportV2YamlConflictMapItem(),
            exportFile = true
        )
        println(resultMap)
        val result = jacksonObjectMapper().writeValueAsString(resultMap)
        Assertions.assertEquals(
            result, "{\"key1\":\"value\",\"key2\":[\"\${{ variables.haha }}\"," +
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
        val resultMap = txPipelineExportService.formatScriptOutput(
            script = inputString,
            output2Elements = output2Elements,
            variables = variables,
            outputConflictMap = mutableMapOf(),
            pipelineExportV2YamlConflictMapItem = PipelineExportV2YamlConflictMapItem(),
            exportFile = true
        )
        val result = jacksonObjectMapper().writeValueAsString(resultMap)
        println(result)
        Assertions.assertEquals(
            resultMap, "# 您可以通过setEnv函数设置插件间传递的参数\n# echo \"::set-output " +
                "name=FILENAME::package.zip\"\n# 然后在后续的插件的表单中使用\${{ FILENAME }}引用这个变量\n\n#" +
                " 您可以在质量红线中创建自定义指标，然后通过setGateValue函数设置指标值\n# setGateValue \"CodeCoverage\" " +
                "\$myValue\n# 然后在质量红线选择相应指标和阈值。若不满足，流水线在执行时将会被卡住\n\n# cd \${{ ci.workspace }} " +
                "可进入当前工作空间目录\n\nset -x\n\n# 编译镜像\necho " +
                "\"::set-output name=compile_img_str::trpc-golang-compile" +
                ":0.1.2:tlinux:common\"\n# 运行镜像\necho \"::set-output" +
                " name=img_str::trpc-golang-runtime:0.1.0\"\necho " +
                "\"::set-output name=img_str2::trpc-golang-runtime:0.1.1\"\n# something\necho \"::set-output " +
                "name=TestDir::src/go-test\"\n# something\nrm \${{ TestDir }} -rf\n\necho \"::set-output " +
                "name=user::\${{ default_user }}\"\n"
        )
    }
}
