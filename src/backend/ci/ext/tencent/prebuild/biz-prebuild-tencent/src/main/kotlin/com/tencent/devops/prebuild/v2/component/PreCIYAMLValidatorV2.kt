package com.tencent.devops.prebuild.v2.component

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.prebuild.PreBuildMessageCode.ALPHABET_NUMBER_UNDERSCORE
import com.tencent.devops.prebuild.PreBuildMessageCode.STAGES_JOBS_STEPS
import com.tencent.devops.process.yaml.v2.enums.TemplateType
import com.tencent.devops.process.yaml.v2.exception.YamlFormatException
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

@Component
class PreCIYAMLValidatorV2 {

    companion object {
        private const val CI_SCHEMA = "ci"
        private const val TEMPLATE_EXTEND_SCHEMA = "template-extends"
        private const val TEMPLATE_STAGE_SCHEMA = "template-stages"
        private const val TEMPLATE_JOB_SCHEMA = "template-jobs"
        private const val TEMPLATE_STEP_SCHEMA = "template-steps"
        private const val TEMPLATE_VARIABLE_SCHEMA = "template-variables"
        private const val TEMPLATE_GATE_SCHEMA = "template-gates"
        private val logger = LoggerFactory.getLogger(PreCIYAMLValidatorV2::class.java)
        private val schemaCacheMap = ConcurrentHashMap<String, JsonSchema>()
    }

    fun check(originYaml: String, templateType: TemplateType?, isCiFile: Boolean) {
        checkYamlSchemaCore(originYaml, templateType, isCiFile)
    }

    private fun checkYamlSchemaCore(originYaml: String, templateType: TemplateType? = null, isCiFile: Boolean) {
        val loadYaml = try {
            YamlUtil.toYaml(Yaml().load(originYaml))
        } catch (ignored: Throwable) {
            throw YamlFormatException("There may be a problem with your yaml syntax ${ignored.message}")
        }

        val yamlJson = YamlUtil.getObjectMapper().readTree(YamlUtil.toYaml(loadYaml)).replaceOn()

        // 若是校验顶级整体
        if (isCiFile) {
            getSchema(CI_SCHEMA).check(yamlJson)
            // 校验schema后有一些特殊的校验
            yamlJson.checkCiRequired()
            yamlJson.checkVariablesFormat()
        }

        when (templateType) {
            TemplateType.EXTEND -> {
                getSchema(TEMPLATE_EXTEND_SCHEMA).check(yamlJson)
                yamlJson.checkExtendsRequired()
                yamlJson.checkVariablesFormat()
            }
            TemplateType.VARIABLE -> {
                getSchema(TEMPLATE_VARIABLE_SCHEMA).check(yamlJson)
                yamlJson.checkVariablesFormat()
            }
            TemplateType.STAGE -> getSchema(TEMPLATE_STAGE_SCHEMA).check(yamlJson)
            TemplateType.GATE -> getSchema(TEMPLATE_GATE_SCHEMA).check(yamlJson)
            TemplateType.JOB -> getSchema(TEMPLATE_JOB_SCHEMA).check(yamlJson)
            TemplateType.STEP -> getSchema(TEMPLATE_STEP_SCHEMA).check(yamlJson)
            else -> {
                return
            }
        }
    }

    private fun getSchema(file: String): JsonSchema {
        return schemaCacheMap.getOrPut(file) {
            JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
                    .objectMapper(YamlUtil.getObjectMapper())
                    .build()
                    .getSchema(
                        getStrFromResource("schema/$file.json").ifBlank {
                            val msg = "yaml schema is blank from resources file"
                            logger.error(msg)
                            throw RuntimeException(msg)
                        }
                    )
        }
    }

    private fun getStrFromResource(path: String): String {
        val classPathResource = ClassPathResource(path)
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

private fun JsonSchema.check(yaml: JsonNode) {
    validate(yaml).let {
        if (!it.isNullOrEmpty()) {
            throw YamlFormatException(it.toString())
        }
    }
}

// Yaml规则下会将on当成true在消除锚点时会将On替换为true
private fun JsonNode.replaceOn(): JsonNode {
    val realOn = get("true") ?: return this
    val node = this as ObjectNode
    node.set<JsonNode>("on", realOn)
    node.remove("true")
    return this
}

private fun JsonNode.checkV1(): Boolean {
    return get("version")?.textValue() != "v2.0"
}

private fun JsonNode.checkVariablesFormat() {
    val vars = get("variables") ?: return
    val keyRegex = Regex("^[0-9a-zA-Z_]+$")
    vars.fields().forEach {
        if (!keyRegex.matches(it.key)) {
            throw YamlFormatException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ALPHABET_NUMBER_UNDERSCORE
                )
            )
        }
    }
}

private fun JsonNode.checkCiRequired() {
    val requiredList = listOf("stages", "jobs", "steps", "extends")
    requiredList.forEach {
        if (get(it) != null) {
            return
        }
    }
    throw YamlFormatException(
        I18nUtil.getCodeLanMessage(
        messageCode = STAGES_JOBS_STEPS
    ))
}

private fun JsonNode.checkExtendsRequired() {
    if (get("stages") == null && get("jobs") == null && get("steps") == null) {
        throw YamlFormatException(
            I18nUtil.getCodeLanMessage(
                messageCode = STAGES_JOBS_STEPS
            )
        )
    }
}
