package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.type.agent.DockerOptions
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MountWorkspaceTest {
    @Test
    fun `JSON and kebab case round trip writes only disabled option`() {
        val mapper = JsonUtil.getObjectMapper(false).copy()
        val options = DockerOptions(null, null, null, null, null, null, null, null)
        assertFalse(mapper.writeValueAsString(options).contains("mountWorkspace"))
        assertFalse(mapper.writeValueAsString(options.copy(mountWorkspace = true)).contains("mountWorkspace"))
        val disabled = options.copy(mountWorkspace = false)
        assertTrue(mapper.writeValueAsString(disabled).contains("\"mountWorkspace\":false"))
        val yamlMapper = mapper.copy().setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
        val yamlShape = yamlMapper.writeValueAsString(disabled)
        assertTrue(yamlShape.contains("\"mount-workspace\":false"))
        assertEquals(false, yamlMapper.readValue(yamlShape, DockerOptions::class.java).mountWorkspace)
    }

    @Test
    fun `actual YAML mappers and v2 v3 parsers retain only false`() {
        val path = com.tencent.devops.process.yaml.pojo.TemplatePath("pipeline.yml")
        val mappers = listOf(
            com.tencent.devops.process.yaml.transfer.TransferMapper.getObjectMapper(),
            com.tencent.devops.process.yaml.v2.parsers.template.TemplateYamlMapper.getObjectMapper(),
            com.tencent.devops.process.yaml.v3.parsers.template.TemplateYamlMapper.getObjectMapper()
        )
        for (flag in listOf(null, true, false)) {
            val options = DockerOptions(null, null, null, null, null, null, null, null, flag)
            for (mapper in mappers) {
                val text = mapper.writeValueAsString(mapOf("image" to "alpine", "options" to options))
                assertEquals(flag == false, text.contains("mount-workspace: false"), text)
                assertFalse(text.contains("mountWorkspace"), text)
                val value = mapper.readValue(text, Map::class.java)
                val v2 = com.tencent.devops.process.yaml.v2.parsers.template.YamlObjects.getContainer(path.path, value)
                val v3 = com.tencent.devops.process.yaml.v3.parsers.template.YamlObjects.getContainer(path, value)
                assertEquals(if (flag == false) false else null, v2.options?.mountWorkspace)
                assertEquals(if (flag == false) false else null, v3.options?.mountWorkspace)
            }
        }
        for (invalid in listOf("false", 0, "yes")) {
            val value = mapOf("image" to "alpine", "options" to mapOf("mount-workspace" to invalid))
            assertThrows(Exception::class.java) {
                com.tencent.devops.process.yaml.v3.parsers.template.YamlObjects.getContainer(path, value)
            }
        }
    }
}
