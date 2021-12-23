package com.tencent.devops.prebuild.service

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.ci.v2.PreScriptBuildYaml
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
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class PreBuildV2ServiceTest : ServiceBaseTest() {
    @MockBean
    lateinit var preCIYAMLValidator: PreCIYAMLValidator

    @MockBean
    lateinit var client: Client

    @MockBean
    lateinit var prebuildProjectDao: PrebuildProjectDao

    @MockBean
    lateinit var pipelineLayoutBuilder: PipelineLayout.Builder

    @Autowired
    lateinit var preBuildV2Service: PreBuildV2Service

    @BeforeAll
    fun initMock() {
        // 流水线创建远程调用
        Mockito.`when`(
            client.get(ServicePipelineResource::class).create(anyString(), anyString(), any(), ChannelCode.BS)
        ).thenReturn(Result(PipelineId(PIPELINE_ID)))

        // 流水线修改远程调用
        Mockito.`when`(
            client.get(ServicePipelineResource::class)
                .edit(anyString(), anyString(), PIPELINE_ID, any(), ChannelCode.BS)
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
                any(),
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
    fun testCheckYamlSchema_success() {
        assertNotNull(prebuildProjectDao)
        val successResp = preBuildV2Service.checkYamlSchema(YAML_CONTENT)
        assertTrue(successResp.isOk())
        assertNull(successResp.data)
        assertNull(successResp.message)
    }

    @Test
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
    fun testStartBuild_success() {
        val resp = preBuildV2Service.startBuild(userId, anyString(), startUpReq, agentInfo)
        assertEquals(BUILD_ID, resp.id)
        verify(prebuildProjectDao, times(1)).createOrUpdate(
            any(),
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
    fun testStartBuild_fail() {
        Mockito.`when`(preCIYAMLValidator.validate(anyString())).thenReturn(Triple(false, null, "error"))
        assertThrows<CustomException> { preBuildV2Service.startBuild(userId, anyString(), startUpReq, agentInfo) }
    }
}
