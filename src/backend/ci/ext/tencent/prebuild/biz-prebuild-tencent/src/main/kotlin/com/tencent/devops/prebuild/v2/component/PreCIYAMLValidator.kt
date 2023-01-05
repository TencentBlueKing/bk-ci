/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.prebuild.v2.component

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.process.yaml.v2.models.PreScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.YAME_META_DATA_JSON_FILTER
import com.tencent.devops.process.yaml.v2.models.job.PreJob
import com.tencent.devops.process.yaml.v2.models.job.RunsOn
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.ws.rs.core.Response

/**
 * PreCI YAML 2.0 业务校验
 */
@Component
class PreCIYAMLValidator {
    companion object {
        private val logger = LoggerFactory.getLogger(PreCIYAMLValidator::class.java)
    }

    /**
     * 校验入口
     *
     * @param originYaml 源yaml
     * @return <1:校验结果通过与否, 2:预处理后的yamlObj, 3:校验不过的原因>
     */
    fun validate(originYaml: String): Triple<Boolean, PreScriptBuildYaml?, String> {
        val formatYamlStr = ScriptYmlUtils.formatYaml(originYaml)
        val yamlJsonStr = ScriptYmlUtils.convertYamlToJson(formatYamlStr)
        val yamlSchema = getYamlSchema()
        val (isPassed, errorMessage) = ScriptYmlUtils.validate(
            schema = yamlSchema, yamlJson = yamlJsonStr
        )

        if (!isPassed) {
            return Triple(false, null, errorMessage)
        }

        val preScriptBuildYaml = YamlUtil.getObjectMapper().readValue(formatYamlStr, PreScriptBuildYaml::class.java)
        checkYamlBusiness(preScriptBuildYaml, originYaml)

        return Triple(true, preScriptBuildYaml, "")
    }

    /**
     * 获取整体schema
     */
    private fun getYamlSchema(): String {
        val mapper = ObjectMapper().setFilterProvider(
            SimpleFilterProvider().addFilter(
                YAME_META_DATA_JSON_FILTER, SimpleBeanPropertyFilter.serializeAllExcept(YAME_META_DATA_JSON_FILTER)
            )
        )
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
        val schemaGenerator = JsonSchemaGenerator(mapper)
        val schema = schemaGenerator.generateSchema(PreScriptBuildYaml::class.java)
        with(schema) {
            `$schema` = "http://json-schema.org/draft-03/schema#"
        }

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema)
    }

    /**
     * 获取构建机schema
     */
    private fun getRunsOnYamlSchema(): String {
        val mapper = ObjectMapper()
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
        val schemaGenerator = JsonSchemaGenerator(mapper)
        val schema = schemaGenerator.generateSchema(RunsOn::class.java)
        with(schema) {
            `$schema` = "http://json-schema.org/draft-03/schema#"
        }

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema)
    }

    /**
     * 校验PreCI业务所支持的特性
     */
    private fun checkYamlBusiness(preScriptBuildYaml: PreScriptBuildYaml, originYaml: String) {
        checkRunsOn(preScriptBuildYaml)
        checkVariable(preScriptBuildYaml)
        checkStage(preScriptBuildYaml)
        checkExtend(originYaml)
    }

    /**
     * 检查构建机类型
     */
    private fun checkRunsOn(preScriptBuildYaml: PreScriptBuildYaml) {
        val preJobList = mutableListOf<PreJob>()

        if (!preScriptBuildYaml.stages.isNullOrEmpty()) {
            preScriptBuildYaml.stages!!.forEach { stage ->
                if (!stage.jobs.isNullOrEmpty()) {
                    preJobList.addAll(stage.jobs!!.values)
                }
            }
        }

        if (!preScriptBuildYaml.jobs.isNullOrEmpty()) {
            preJobList.addAll(preScriptBuildYaml.jobs!!.values)
        }

        if (preJobList.isNullOrEmpty()) {
            return
        }

        val runsOnYamlSchema = getRunsOnYamlSchema()
        val objectMapper = ObjectMapper()

        preJobList.forEach { preJob ->
            // runs-on存在3种结构，String、标签的话是数组、T
            if (preJob.runsOn == null) {
                return@forEach
            }

            if (preJob.runsOn!! is String || preJob.runsOn!! is Array<*> || preJob.runsOn!! is List<*>) {
                return@forEach
            }

            val (isPassed, errMsg) = ScriptYmlUtils.validate(
                schema = runsOnYamlSchema, yamlJson = objectMapper.writeValueAsString(preJob.runsOn)
            )

            if (!isPassed) {
                logger.error("Check yaml schema failed [runs-on]. $errMsg")
                throw CustomException(
                    Response.Status.INTERNAL_SERVER_ERROR, "runs-on只能是String、Array、Object其一"
                )
            }
        }
    }

    /**
     * 检查关键字：stages
     */
    private fun checkStage(preScriptBuildYaml: PreScriptBuildYaml) {
        val stageAndJobNotNull = preScriptBuildYaml.stages != null && preScriptBuildYaml.jobs != null
        val stageAndStepNotNull = preScriptBuildYaml.stages != null && preScriptBuildYaml.steps != null
        val jobAndStepNotNull = preScriptBuildYaml.jobs != null && preScriptBuildYaml.steps != null

        if (stageAndJobNotNull || stageAndStepNotNull || jobAndStepNotNull) {
            throw CustomException(
                Response.Status.BAD_REQUEST, "stages, jobs, steps不能并列存在，只能存在其一"
            )
        }
    }

    /**
     * 检查关键字：variables
     */
    private fun checkVariable(preScriptBuildYaml: PreScriptBuildYaml) {
        if (preScriptBuildYaml.variables == null) {
            return
        }

        preScriptBuildYaml.variables!!.forEach {
            val keyRegex = Regex("^[0-9a-zA-Z_]+$")
            if (!keyRegex.matches(it.key)) {
                throw CustomException(
                    Response.Status.BAD_REQUEST, "变量名称必须是英文字母、数字或下划线(_)"
                )
            }
        }
    }

    /**
     * 检查关键字：extends
     */
    private fun checkExtend(yaml: String) {
        val yamlMap = YamlUtil.getObjectMapper().readValue(yaml, object : TypeReference<Map<String, Any?>>() {})
        if (yamlMap["extends"] == null) {
            return
        }

        yamlMap.forEach { (key, _) ->
            // triggerOn == on
            val notEqualOnAndExtends = key != "triggerOn" && key != "on" && key != "extends"
            val notEqualResourcesAndNameAndVersion = key != "resources" && key != "name" && key != "version"

            if (notEqualOnAndExtends && notEqualResourcesAndNameAndVersion) {
                throw CustomException(
                    status = Response.Status.BAD_REQUEST,
                    message = "使用 extends 时顶级关键字只能有触发器 on, resources, name, version"
                )
            }
        }
    }
}
