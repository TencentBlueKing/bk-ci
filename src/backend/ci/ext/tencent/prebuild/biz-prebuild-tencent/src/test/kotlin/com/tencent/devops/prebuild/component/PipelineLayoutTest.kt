package com.tencent.devops.prebuild.component

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientErrorDecoder
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.devcloud.PublicDevCloudDispathcType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.prebuild.ServiceBaseTest
import com.tencent.devops.prebuild.pojo.CreateStagesRequest
import com.tencent.devops.prebuild.v2.component.PipelineLayout
import com.tencent.devops.prebuild.v2.component.PreCIYAMLValidatorV2
import com.tencent.devops.process.yaml.v2.models.PreTemplateScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.ScriptBuildYaml
import com.tencent.devops.process.yaml.v2.parsers.template.YamlTemplate
import com.tencent.devops.process.yaml.v2.parsers.template.models.GetTemplateParam
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient

@ExtendWith(MockitoExtension::class)
class PipelineLayoutTest : ServiceBaseTest() {
    @InjectMocks
    lateinit var preCIYAMLValidator: PreCIYAMLValidatorV2

    @Mock
    lateinit var serviceMarketAtomResource: ServiceMarketAtomResource

    fun setup(
        @Mock consulClient: CompositeDiscoveryClient,
        @Mock clientErrorDecoder: ClientErrorDecoder,
        @Mock commonConfig: CommonConfig,
        @Mock objectMapper: ObjectMapper,
        @Mock bkTag: BkTag
    ) {
        val theMock = Mockito.mockStatic(SpringContextUtil::class.java)
        theMock.`when`<Client> { SpringContextUtil.getBean(any(Client::class.java.javaClass)) }
            .thenReturn(Client(consulClient, clientErrorDecoder, commonConfig, bkTag, objectMapper))
    }

    @Test
    @DisplayName("测试流水线模板_本地构建机")
    fun testLocal() {
        val scriptBuildYaml = getYamlObject(getYamlForLocal())
        val createStagesRequest = CreateStagesRequest(
            userId = userId,
            startUpReq = startUpReq,
            scriptBuildYaml = scriptBuildYaml,
            agentInfo = agentInfo,
            channelCode = channelCode
        )

        val model = PipelineLayout.Builder()
            .pipelineName(pipelineName)
            .description(description)
            .creator(userId)
            .stages(createStagesRequest)
            .build()

        // 测试是否VM
        assertTrue(
            model.stages.stream().anyMatch { x ->
                x.containers.stream().anyMatch { y -> y.getClassType() == VMBuildContainer.classType }
            }
        )

        val vmContainer = model.stages.stream()
            .flatMap { x -> x.containers.stream() }
            .filter { y -> y.getClassType() == VMBuildContainer.classType }
            .map { z -> z }
            .findFirst()
            .orElse(null)

        assertNotNull(vmContainer)
        // 测试调度类型
        assertTrue((vmContainer as VMBuildContainer).dispatchType is ThirdPartyAgentIDDispatchType)

        // 本地构建机展示的名字取自agentId
        val dispatchType = vmContainer.dispatchType as ThirdPartyAgentIDDispatchType
        assertEquals(agentInfo.agentId, dispatchType.displayName)
    }

    @Test
    @DisplayName("测试流水线模板_docker_on_vm")
    @Disabled
    fun testDockerVM() {
        val scriptBuildYaml = getYamlObject(getYamlForDockerVM())
        val createStagesRequest = CreateStagesRequest(
            userId = userId,
            startUpReq = startUpReq,
            scriptBuildYaml = scriptBuildYaml,
            agentInfo = agentInfo,
            channelCode = channelCode
        )

        val model = PipelineLayout.Builder()
            .pipelineName(pipelineName)
            .description(description)
            .creator(userId)
            .stages(createStagesRequest)
            .build()

        assertTrue(
            model.stages.stream().anyMatch { x ->
                x.containers.stream().anyMatch { y -> y.getClassType() == VMBuildContainer.classType }
            }
        )

        val vmContainer = model.stages.stream()
            .flatMap { x -> x.containers.stream() }
            .filter { y -> y.getClassType() == VMBuildContainer.classType }
            .map { z -> z }
            .findFirst()
            .orElse(null)

        assertNotNull(vmContainer)
        assertTrue((vmContainer as VMBuildContainer).dispatchType is DockerDispatchType)
    }

    @Test
    @DisplayName("测试流水线模板_docker_on_devcloud")
    @Disabled
    fun testDevCloud() {
        val scriptBuildYaml = getYamlObject(getYamlForDevCloud())
        val createStagesRequest = CreateStagesRequest(
            userId = userId,
            startUpReq = startUpReq,
            scriptBuildYaml = scriptBuildYaml,
            agentInfo = agentInfo,
            channelCode = channelCode
        )

        val model = PipelineLayout.Builder()
            .pipelineName(pipelineName)
            .description(description)
            .creator(userId)
            .stages(createStagesRequest)
            .build()

        assertTrue(
            model.stages.stream().anyMatch { x ->
                x.containers.stream().anyMatch { y -> y.getClassType() == VMBuildContainer.classType }
            }
        )

        val vmContainer = model.stages.stream()
            .flatMap { x -> x.containers.stream() }
            .filter { y -> y.getClassType() == VMBuildContainer.classType }
            .map { z -> z }
            .findFirst()
            .orElse(null)

        assertNotNull(vmContainer)
        assertTrue((vmContainer as VMBuildContainer).dispatchType is PublicDevCloudDispathcType)
    }

    @Test
    @DisplayName("测试流水线模板_无编译环境")
    fun testAgentLess() {
        val scriptBuildYaml = getYamlObject(getYamlForAgentLess())
        val createStagesRequest = CreateStagesRequest(
            userId = userId,
            startUpReq = startUpReq,
            scriptBuildYaml = scriptBuildYaml,
            agentInfo = agentInfo,
            channelCode = channelCode
        )

        val model = PipelineLayout.Builder()
            .pipelineName(pipelineName)
            .description(description)
            .creator(userId)
            .stages(createStagesRequest)
            .build()

        // 无编译环境均为NormalContainer，非VM没有dispatchType
        assertTrue(
            model.stages.stream().anyMatch { x ->
                x.containers.stream().anyMatch { y ->
                    y.getClassType() == NormalContainer.classType && y.name.contains("无编译环境")
                }
            }
        )
    }

    @Test
    @DisplayName("测试流水线模板_非法构建机")
    fun testInvalidDispatchType() {
        val scriptBuildYaml = getYamlObject(getYamlForInvalidDispatchType())
        val createStagesRequest = CreateStagesRequest(
            userId = userId,
            startUpReq = startUpReq,
            scriptBuildYaml = scriptBuildYaml,
            agentInfo = agentInfo,
            channelCode = channelCode
        )

        // 公共构建资源池不存在的
        assertThrows<CustomException> {
            PipelineLayout.Builder()
                .pipelineName(pipelineName)
                .description(description)
                .creator(userId)
                .stages(createStagesRequest)
                .build()
        }
    }

    private fun getYamlObject(yamlStr: String): ScriptBuildYaml {
        return ScriptYmlUtils.normalizePreCiYaml(
            YamlTemplate(
                filePath = "",
                yamlObject = YamlUtil.getObjectMapper()
                    .readValue(yamlStr, PreTemplateScriptBuildYaml::class.java),
                extraParameters = null,
                getTemplateMethod = { p: GetTemplateParam<Any?> -> "" },
                nowRepo = null,
                repo = null,
                resourcePoolMapExt = null
            ).replace()
        )
    }
}
