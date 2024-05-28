package com.tencent.devops.process.yaml.transfer.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.benmanes.caffeine.cache.Caffeine
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.tencent.devops.common.api.constant.CommonMessageCode.YAML_NOT_VALID
import com.tencent.devops.common.api.util.ReflectUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.transfer.PipelineTransferException
import com.tencent.devops.process.yaml.transfer.TransferMapper
import com.tencent.devops.process.yaml.v2.enums.TemplateType
import java.io.FileNotFoundException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
class CodeSchemaCheck @Autowired constructor(
    private val redisOperation: RedisOperation
) {

    companion object {
        private const val REDIS_STREAM_YAML_SCHEMA = "pac:yaml.schema:json"
        private const val CI_SCHEMA = "ci"
        private const val TEMPLATE_EXTEND_SCHEMA = "template-extends"
        private const val TEMPLATE_STAGE_SCHEMA = "template-stages"
        private const val TEMPLATE_JOB_SCHEMA = "template-jobs"
        private const val TEMPLATE_STEP_SCHEMA = "template-steps"
        private const val TEMPLATE_VARIABLE_SCHEMA = "template-variables"
        private const val TEMPLATE_GATE_SCHEMA = "template-gates"
    }

    private val schemaRedisFiles = Caffeine.newBuilder()
        .maximumSize(20)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, String> { key ->
            kotlin.runCatching {
                redisOperation.get(key) ?: ""
            }.onFailure { logger.warn("get $key in schemaRedisFiles error.", it) }.getOrNull() ?: ""
        }

    private val logger =
        LoggerFactory.getLogger(CodeSchemaCheck::class.java)

    private val schemaFactory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
        .objectMapper(YamlUtil.getObjectMapper())
        .build()

    private val objectMapperFactory = ThreadLocal.withInitial(
        Supplier<ObjectMapper> {
            ObjectMapper(
                YAMLFactory().disable(YAMLGenerator.Feature.SPLIT_LINES)
            ).registerKotlinModule()
        }
    )

    private val schemaMap = mutableMapOf<String, JsonSchema>()

    fun check(originYaml: String) {
        check(originYaml, null, true)
    }

    // 给来自前端的接口用，直接扔出去就好
    fun check(originYaml: String, templateType: TemplateType?, isCiFile: Boolean) {
        try {
            checkYamlSchema(originYaml, templateType, isCiFile)
        } catch (ignore: FileNotFoundException) {
            logger.warn("schema file not find. ${ignore.message}")
        }
    }

    private fun checkYamlSchema(originYaml: String, templateType: TemplateType? = null, isCiFile: Boolean) {
        val loadYaml = try {
            toYaml(TransferMapper.getYamlFactory().load(originYaml) as Any)
        } catch (ignored: Exception) {
            logger.warn("YAML_SCHEMA_CHECK|${ignored.message}|originYaml=$originYaml", ignored)
            throw PipelineTransferException(
                YAML_NOT_VALID,
                arrayOf("There may be a problem with your yaml syntax ${ignored.message}")
            )
        }
        // 解析锚点
        val yamlJson = TransferMapper.getObjectMapper().readTree(loadYaml)
        when (yamlJson.version()) {
//            YamlVersion.V2_0.tag -> {
//                check(yamlJson, YamlVersion.V2_0, templateType, isCiFile)
//            }

            YamlVersion.V3_0.tag -> {
                check(yamlJson, YamlVersion.V3_0, templateType, isCiFile)
            }

            else -> throw PipelineTransferException(
                YAML_NOT_VALID,
                arrayOf("yaml version(${yamlJson.version()}) not valid, only support v3.0")
            )
        }
    }

    private fun check(
        yamlJson: JsonNode,
        version: YamlVersion,
        templateType: TemplateType? = null,
        isCiFile: Boolean
    ) {
        if (isCiFile) {
            getSchema(CI_SCHEMA, version).check(yamlJson)
        }
        if (templateType == null) {
            return
        }
        when (templateType) {
            TemplateType.EXTEND -> {
                getSchema(TEMPLATE_EXTEND_SCHEMA, version).check(yamlJson)
            }

            TemplateType.VARIABLE -> {
                getSchema(TEMPLATE_VARIABLE_SCHEMA, version).check(yamlJson)
            }

            TemplateType.STAGE -> getSchema(TEMPLATE_STAGE_SCHEMA, version).check(yamlJson)
            TemplateType.GATE -> getSchema(TEMPLATE_GATE_SCHEMA, version).check(yamlJson)
            TemplateType.JOB -> getSchema(TEMPLATE_JOB_SCHEMA, version).check(yamlJson)
            TemplateType.STEP -> getSchema(TEMPLATE_STEP_SCHEMA, version).check(yamlJson)
            else -> {
                return
            }
        }
    }

    private fun toYaml(bean: Any): String {
        if (ReflectUtil.isNativeType(bean) || bean is String) {
            return bean.toString()
        }
        return objectMapperFactory.get().writeValueAsString(bean)!!
    }

    private fun getSchema(file: String, version: YamlVersion): JsonSchema {
        val str = schemaRedisFiles.get(
            "$REDIS_STREAM_YAML_SCHEMA:${version.name}:$file"
        )?.ifBlank { null } ?: return getSchemaFromGit(file, version)
        return schemaFactory.getSchema(str)
    }

    private fun getSchemaFromGit(file: String, version: YamlVersion): JsonSchema {
        val path = "schema/${version.name}/$file.json"
        if (schemaMap[path] != null) {
            return schemaMap[path]!!
        }
        val schema = schemaFactory.getSchema(
            ClassPathResource(path).inputStream.readBytes()
                .toString(Charset.defaultCharset())
        )
        schemaMap[path] = schema
        return schema
    }

    private fun JsonSchema.check(yaml: JsonNode) {
        validate(yaml).let {
            if (!it.isNullOrEmpty()) {
                throw PipelineTransferException(
                    YAML_NOT_VALID,
                    arrayOf(it.toString())
                )
            }
        }
    }

    private fun JsonNode.version(): String? {
        return get("version")?.textValue()
    }
}
