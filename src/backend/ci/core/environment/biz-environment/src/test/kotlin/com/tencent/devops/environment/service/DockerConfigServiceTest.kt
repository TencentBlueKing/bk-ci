package com.tencent.devops.environment.service

import com.tencent.devops.common.pipeline.type.agent.AgentDockerConfig
import com.tencent.devops.common.pipeline.type.agent.DockerOptions
import com.tencent.devops.environment.dao.DockerConfigDao
import com.tencent.devops.environment.pojo.DockerConfigRequest
import com.tencent.devops.environment.pojo.DockerConfigScope
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DockerConfigServiceTest {
    @Test
    fun `environment selection never inherits node limits`() {
        val dao = mockk<DockerConfigDao>()
        every { dao.list(any(), DockerConfigScope.NODE, "owner", listOf(1L)) } returns
            mapOf(1L to AgentDockerConfig("8", "4g", "podman"))
        every { dao.list(any(), DockerConfigScope.ENV, "owner", listOf(2L)) } returns emptyMap()
        val service = DockerConfigService(mockk(), dao, mockk(), mockk(), mockk())
        assertEquals(AgentDockerConfig(null, null, "podman"), service.forAgents("owner", listOf(1L), 2L)[1L])
        assertEquals("8", service.forAgents("owner", listOf(1L))[1L]?.cpus)
    }

    @Test
    fun `job fields independently override defaults and may be higher`() {
        val options = DockerOptions(null, null, null, null, null, null, "16", "")
        val merged = AgentDockerConfig("2", "512m").applyTo(options)
        assertEquals("16", merged.cpus)
        assertEquals("512m", merged.memory)
        assertEquals("", options.memory)
    }

    @Test
    fun `CPU and memory boundaries and runtime whitelist`() {
        listOf("0.5", "1", "4.25").forEach { DockerConfigRequest(cpus = it).validate() }
        listOf("6m", "512m", "4g", "").forEach { DockerConfigRequest(memory = it).validate() }
        listOf("0", "-1", "NaN", "Infinity").forEach {
            assertThrows(Exception::class.java) { DockerConfigRequest(cpus = it).validate() }
        }
        listOf("5m", "0g", "1.5g", "4 g", "4k").forEach {
            assertThrows(Exception::class.java) { DockerConfigRequest(memory = it).validate() }
        }
        assertThrows(Exception::class.java) { DockerConfigRequest(containerRuntime = "sh").validate() }
        DockerConfigRequest(containerRuntime = "podman").validate()
    }
}
