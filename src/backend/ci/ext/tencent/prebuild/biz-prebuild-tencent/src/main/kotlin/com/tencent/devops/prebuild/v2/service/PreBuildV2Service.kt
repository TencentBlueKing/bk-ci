package com.tencent.devops.prebuild.v2.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.PreScriptBuildYaml
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response


@Service
class PreBuildV2Service @Autowired constructor(
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PreBuildV2Service::class.java)
    }

    fun checkYaml(yamlStr: String): Pair<Boolean, String> {
        val formatYamlStr = ScriptYmlUtils.formatYaml(yamlStr)
        val yamlToJson = ScriptYmlUtils.convertYamlToJson(formatYamlStr)
        val yamlSchema = getYamlSchema()
        val (schemaPassed, errorMessage) = ScriptYmlUtils.validate(
            schema = yamlSchema,
            yamlJson = yamlToJson
        )

        // schema校验
        if (!schemaPassed) {
            logger.error("Check yaml schema failed. $errorMessage")
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, errorMessage)
        }

        val yamlObject =
            YamlUtil.getObjectMapper().readValue(formatYamlStr, PreScriptBuildYaml::class.java)
        checkYamlStage(yamlObject)

        return Pair(true, "")
    }

    private fun getYamlSchema(): String {
        val mapper = ObjectMapper()
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
        val schemaGenerator = JsonSchemaGenerator(mapper)
        val schema = schemaGenerator.generateSchema(PreScriptBuildYaml::class.java)
        with(schema) {
            `$schema` = "http://json-schema.org/draft-03/schema#"
        }

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema)
    }

    private fun checkYamlStage(preScriptBuildYaml: PreScriptBuildYaml) {

    }
}