package com.tencent.devops.prebuild.service

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.prebuild.ServiceBaseTest
import com.tencent.devops.prebuild.dao.PrebuildProjectDao
import com.tencent.devops.prebuild.v2.component.PipelineLayout
import com.tencent.devops.prebuild.v2.component.PreCIYAMLValidator
import com.tencent.devops.prebuild.v2.service.PreBuildV2Service
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.yaml.v2.models.PreScriptBuildYaml
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
@Disabled
class PreBuildV2ServiceTest : ServiceBaseTest() {
    @Mock
    lateinit var preCIYAMLValidator: PreCIYAMLValidator

    @Mock
    lateinit var client: Client

    @Mock
    lateinit var prebuildProjectDao: PrebuildProjectDao

    @Mock
    lateinit var pipelineLayoutBuilder: PipelineLayout.Builder

    @InjectMocks
    lateinit var preBuildV2Service: PreBuildV2Service

    @BeforeEach
    fun setup() {
        // 流水线创建远程调用
        Mockito.`when`(
            client.get(ServicePipelineResource::class)
                .create(anyString(), anyString(), any()!!, ChannelCode.BS, false)
        ).thenReturn(Result(PipelineId(PIPELINE_ID)))

        // 流水线修改远程调用
        Mockito.`when`(
            client.get(ServicePipelineResource::class)
                .edit(anyString(), anyString(), PIPELINE_ID, any()!!, ChannelCode.BS)
        ).thenReturn(Result(true))

        // 启动流水线远程调用
        Mockito.`when`(
            client.get(ServiceBuildResource::class).manualStartup(
                anyString(), anyString(), anyString(), mapOf(), channelCode
            )
        ).thenReturn(Result(BuildId(BUILD_ID)))

        // DB存储void
        Mockito.doNothing().`when`(
            prebuildProjectDao.createOrUpdate(
                any()!!,
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
            )
        )

        // 流水线编排
        Mockito.`when`(pipelineLayoutBuilder.build()).thenReturn(any())

        // yaml校验器
        Mockito.`when`(preCIYAMLValidator.validate(anyString())).thenReturn(Triple(true, any(), ""))
    }

    @Test
    @DisplayName("测试yaml校验通过")
    fun testCheckYamlSchema_success() {
        val successResp = preBuildV2Service.checkYamlSchema(YAML_CONTENT)
        assertTrue(successResp.isOk())
        assertNull(successResp.data)
        assertNull(successResp.message)
    }

    @Test
    @DisplayName("测试yaml校验失败")
    fun testCheckYamlSchema_fail() {
        val errorMsg = "error at line 22"
        val mockFailReturn: Triple<Boolean, PreScriptBuildYaml?, String> = Triple(false, null, errorMsg)
        Mockito.`when`(preCIYAMLValidator.validate(anyString())).thenReturn(mockFailReturn)
        val failResp = preBuildV2Service.checkYamlSchema(YAML_CONTENT)

        assertFalse(failResp.isOk())
        assertNotNull(failResp.message)
        assertTrue(failResp.message!!.contains(errorMsg))
    }

    @Test
    @DisplayName("测试流水线生成并启动成功")
    fun testStartBuild_success() {
        val resp = preBuildV2Service.startBuild(userId, anyString(), startUpReq, agentInfo)
        assertEquals(BUILD_ID, resp.id)
        verify(prebuildProjectDao, times(1)).createOrUpdate(
            any()!!,
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )
    }

    @Test
    @DisplayName("测试yaml非法导致流水线生成失败")
    fun testStartBuild_fail() {
        Mockito.`when`(preCIYAMLValidator.validate(anyString())).thenReturn(Triple(false, null, "error"))
        assertThrows<CustomException> { preBuildV2Service.startBuild(userId, anyString(), startUpReq, agentInfo) }
    }
}
