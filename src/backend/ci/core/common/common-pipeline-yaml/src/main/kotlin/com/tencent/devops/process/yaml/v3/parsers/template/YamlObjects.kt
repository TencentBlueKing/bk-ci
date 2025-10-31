/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.yaml.v3.parsers.template

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_YAML_FORMAT_EXCEPTION_NEED_PARAM
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.transfer.MetaData
import com.tencent.devops.common.pipeline.pojo.transfer.PreStep
import com.tencent.devops.common.pipeline.pojo.transfer.ResourcesPools
import com.tencent.devops.common.pipeline.pojo.transfer.TemplateInfo
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.agent.DockerOptions
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.yaml.pojo.TemplatePath
import com.tencent.devops.process.yaml.v3.enums.TemplateType
import com.tencent.devops.process.yaml.v3.exception.YamlFormatException
import com.tencent.devops.process.yaml.v3.models.BuildContainerTypeYaml
import com.tencent.devops.process.yaml.v3.models.GitNotices
import com.tencent.devops.process.yaml.v3.models.PacNotices
import com.tencent.devops.process.yaml.v3.models.Variable
import com.tencent.devops.process.yaml.v3.models.VariableDatasource
import com.tencent.devops.process.yaml.v3.models.VariablePropOption
import com.tencent.devops.process.yaml.v3.models.VariablePropType
import com.tencent.devops.process.yaml.v3.models.VariableProps
import com.tencent.devops.process.yaml.v3.models.job.Container
import com.tencent.devops.process.yaml.v3.models.job.Credentials
import com.tencent.devops.process.yaml.v3.models.job.Mutex
import com.tencent.devops.process.yaml.v3.models.job.PreJob
import com.tencent.devops.process.yaml.v3.models.job.Service
import com.tencent.devops.process.yaml.v3.models.job.ServiceWith
import com.tencent.devops.process.yaml.v3.models.job.Strategy
import com.tencent.devops.process.yaml.v3.models.on.PreTriggerOnV3
import com.tencent.devops.process.yaml.v3.models.stage.PreStage
import com.tencent.devops.process.yaml.v3.parameter.Parameters
import com.tencent.devops.process.yaml.v3.parsers.template.models.TemplateDeepTreeNode
import com.tencent.devops.process.yaml.v3.utils.StreamEnvUtils

@Suppress("ComplexMethod")
object YamlObjects {

    fun getTriggerOnV3(triggerOn: Map<String, Any>): PreTriggerOnV3 {
        return JsonUtil.anyTo(triggerOn, object : TypeReference<PreTriggerOnV3>() {})
    }

    fun getVariable(fromPath: TemplatePath, key: String, variable: Map<String, Any>): Variable {
        val props = variable["props"]?.let {
            getVarProps(fromPath, it)
        }
        val type = props?.type
        val va = Variable(
            value = if (type == VariablePropType.REPO_REF.value) {
                variable["value"] ?: mapOf<String, String>()
            } else {
                variable["value"]?.toString()
            },
            readonly = getNullValue("readonly", variable)?.toBoolean(),
            const = getNullValue("const", variable)?.toBoolean(),
            allowModifyAtStartup = getNullValue("allow-modify-at-startup", variable)?.toBoolean(),
            props = props,
            ifCondition = transNullValue<Map<String, String>>(fromPath, "if", "if", variable)
        )

        // 只有列表需要判断
        if (type == VariablePropType.SELECTOR.value || type == VariablePropType.CHECKBOX.value) {
            // 这期暂不对拉取远程接口的参数做校验
            if (props.payload != null) {
                return va
            }
            val value = va.value as String?
            if (!value.isNullOrBlank() && props.options.isNullOrEmpty()) {
                throw YamlFormatException(
                    "$fromPath variable $key format error: value $value not in variable options"
                )
            }
            val expectValues =
                value?.split(",")?.asSequence()?.filter { it.isNotBlank() }?.map { it.trim() }?.toSet()
            val resultValues = props.options?.map { it.id.toString() }?.toSet() ?: emptySet()
            // 说明默认值没有匹配到选项值，报错
            if (expectValues?.subtract(resultValues)?.isEmpty() == false) {
                throw YamlFormatException(
                    "$fromPath variable $key format error: value ${va.value} not in variable options"
                )
            }
        }

        // 校验bool
        if (type == VariablePropType.BOOLEAN.value && (va.value != "true" && va.value != "false")) {
            throw YamlFormatException(
                "$fromPath variable $key format error: bool value ${va.value} not true / false"
            )
        }

        return va
    }

    private fun getVarProps(fromPath: TemplatePath, props: Any): VariableProps {
        val propsMap = transValue<Map<String, Any?>>(fromPath, "props", props)
        val po = VariableProps(
            label = getNullValue("label", propsMap),
            type = getNullValue("type", propsMap),
            options = getVarPropOptions(fromPath, propsMap["options"]),
            description = getNullValue("description", propsMap),
            group = getNullValue("group", propsMap),
            multiple = getNullValue("multiple", propsMap)?.toBooleanStrictOrNull(),
            required = getNullValue("required", propsMap)?.toBooleanStrictOrNull(),
            repoHashId = getNullValue("repo-id", propsMap),
            relativePath = getNullValue("relative-path", propsMap),
            versionControl = getNullValue("version-control", propsMap)?.toBooleanStrictOrNull(),
            scmType = getNullValue("scm-type", propsMap),
            containerType = getVarPropContainerType(fromPath, propsMap["container-type"]),
            glob = getNullValue("filter-rule", propsMap),
            properties = transNullValue<Map<String, String>>(
                file = fromPath,
                type = "metadata",
                key = "metadata",
                map = propsMap
            ),
            payload = propsMap["payload"]
        )

        return po
    }

    private fun getVarPropOptions(fromPath: TemplatePath, options: Any?): List<VariablePropOption>? {
        if (options == null) {
            return null
        }

        val optionsMap = transValue<List<Map<String, Any?>>>(fromPath, "options", options)

        return optionsMap.map { option ->
            VariablePropOption(
                id = getNotNullValueAny("id", "option", option),
                label = getNullValue("label", option),
                description = getNullValue("description", option)
            )
        }
    }

    private fun getVarPropDataSource(fromPath: TemplatePath, datasource: Any?): VariableDatasource? {
        if (datasource == null) {
            return null
        }

        val datasourceMap = transValue<Map<String, Any?>>(fromPath, "datasource", datasource)

        return VariableDatasource(
            url = getNotNullValue("url", "datasource", datasourceMap),
            dataPath = getNullValue("data-path", datasourceMap),
            paramId = getNullValue("param-id", datasourceMap),
            paramName = getNullValue("param-name", datasourceMap),
            hasAddItem = getNullValue("has-add-item", datasourceMap)?.toBoolean(),
            itemText = getNullValue("item-text", datasourceMap),
            itemTargetUrl = getNullValue("item-target-url", datasourceMap)
        )
    }

    private fun getVarPropContainerType(fromPath: TemplatePath, containerType: Any?): BuildContainerTypeYaml? {
        if (containerType == null) {
            return null
        }

        val map = transValue<Map<String, Any?>>(fromPath, "containerType", containerType)

        return BuildContainerTypeYaml(
            buildType = BuildType.valueOf(getNotNullValue("build-type", "containerType", map)),
            os = OS.valueOf(getNotNullValue("os", "containerType", map))
        )
    }

    fun getStep(fromPath: TemplatePath, step: Map<String, Any>, repo: TemplateInfo?): PreStep {
        val preStep = PreStep(
            enable = getNullValue("enable", step)?.toBoolean(),
            name = step["name"]?.toString(),
            id = step["id"]?.toString(),
            ifField = step["if"],
            ifModify = if (step["if-modify"] is List<*>) {
                val ifModifyList = step["if-modify"] as List<*>
                ifModifyList.map { it.toString() }.toList()
            } else null,
            uses = step["uses"]?.toString(),
            with = if (step["with"] == null) {
                null
            } else {
                transValue<Map<String, Any>>(fromPath, "with", step["with"])
            },
            timeoutMinutes = getNullValue("timeout-minutes", step),
            continueOnError = getNullValue("continue-on-error", step),
            retryTimes = getNullValue("retry-times", step)?.toInt(),
            canPauseBeforeRun = getNullValue("can-pause-before-run", step)?.toBooleanStrictOrNull(),
            pauseNoticeReceivers = if (step["pause-notice-receivers"] is List<*>) {
                val receivers = step["pause-notice-receivers"] as List<*>
                receivers.map { it.toString() }.toList()
            } else null,
            env = if (step["env"] == null) {
                null
            } else {
                transValue<Map<String, Any?>>(fromPath, "env", step["env"])
            },
            run = step["run"]?.toString(),
            shell = step["shell"]?.toString(),
            checkout = step["checkout"],
            manualRetry = getNullValue("can-manually-retry", step)?.toBoolean(),
            yamlMetaData = if (step["yamlMetaData"] == null) {
                MetaData(templateInfo = repo)
            } else {
                getYamlMetaData(fromPath, step["yamlMetaData"]!!)
            }
        )

        if (preStep.uses == null && preStep.run == null && preStep.checkout == null) {
            throw YamlFormatException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ERROR_YAML_FORMAT_EXCEPTION_NEED_PARAM,
                    params = arrayOf(fromPath.toString())
                )
            )
        }

        // 检测step env合法性
        StreamEnvUtils.checkEnv(preStep.env, fromPath)
        return preStep
    }

    fun getResourceExclusiveDeclaration(fromPath: TemplatePath, resource: Any): Mutex {
        val resourceMap = transValue<Map<String, Any?>>(fromPath, "mutex", resource)
        return Mutex(
            label = getNotNullValue(key = "label", mapName = "mutex", map = resourceMap),
            queueLength = resourceMap["queue-length"]?.toString()?.toInt(),
            timeoutMinutes = resourceMap["timeout-minutes"]?.toString()
        )
    }

    fun getService(fromPath: TemplatePath, service: Any): Map<String, Service> {
        val serviceMap = transValue<Map<String, Any?>>(fromPath, "services", service)
        val newServiceMap = mutableMapOf<String, Service>()
        serviceMap.forEach { (key, value) ->
            val newValue = transValue<Map<String, Any>>(fromPath, "services", value)
            val with = transValue<Map<String, Any>>(fromPath, "with", newValue["with"])
            newServiceMap.putAll(
                mapOf(
                    key to Service(
                        image = getNotNullValue(key = "image", mapName = "Container", map = newValue),
                        with = ServiceWith(
                            password = getNotNullValue(key = "password", mapName = "with", map = with)
                        )
                    )
                )
            )
        }
        return newServiceMap
    }

    fun getContainer(fromPath: TemplatePath, container: Any): Container {
        val containerMap = transValue<Map<String, Any?>>(fromPath, "container", container)
        return Container(
            image = getNotNullValue(key = "image", mapName = "Container", map = containerMap),
            credentials = if (containerMap["credentials"] == null) {
                null
            } else {
                val credentialsMap =
                    transValue<Map<String, String>>(fromPath, "credentials", containerMap["credentials"])
                Credentials(
                    username = credentialsMap["username"]!!,
                    password = credentialsMap["password"]!!
                )
            },
            options = if (containerMap["options"] == null) {
                null
            } else {
                val optionsMap =
                    transValue<Map<String, Any?>>(fromPath, "options", containerMap["options"])
                DockerOptions(
                    gpus = getNullValue("gpus", optionsMap),
                    volumes = if (optionsMap["volumes"] == null) {
                        null
                    } else {
                        transValue<List<String>>(fromPath, "volumes", optionsMap["volumes"])
                    },
                    mounts = if (optionsMap["mounts"] == null) {
                        null
                    } else {
                        transValue<List<String>>(fromPath, "mounts", optionsMap["mounts"])
                    },
                    privileged = getNullValue("privileged", optionsMap)?.toBoolean()
                )
            },
            imagePullPolicy = getNullValue(key = "image-pull-policy", map = containerMap)
        )
    }

    fun getStrategy(fromPath: TemplatePath, strategy: Any?): Strategy? {
        val strategyMap = transValue<Map<String, Any?>>(fromPath, "strategy", strategy)
        val matrix = strategyMap["matrix"]
        val include = strategyMap["include"]
        val exclude = strategyMap["exclude"]
        return Strategy(
            matrix = matrix,
            include = include,
            exclude = exclude,
            fastKill = getNullValue("fast-kill", strategyMap)?.toBoolean(),
            maxParallel = getNullValue("max-parallel", strategyMap)?.toInt()
        )
    }

    fun getNoticeV2(fromPath: TemplatePath, notice: Map<String, Any?>): GitNotices {
        return GitNotices(
            type = notice["type"].toString(),
            title = notice["title"]?.toString(),
            ifField = notice["if"]?.toString(),
            content = notice["content"]?.toString(),
            receivers = if (notice["receivers"] == null) {
                null
            } else {
                transValue<List<String>>(fromPath, "receivers", notice["receivers"]).toSet()
            },
            ccs = if (notice["ccs"] == null) {
                null
            } else {
                transValue<List<String>>(fromPath, "ccs", notice["ccs"]).toSet()
            },
            chatId = if (notice["chat-id"] == null) {
                null
            } else {
                transValue<List<String>>(fromPath, "chat-id", notice["chat-id"]).toSet()
            }
        )
    }

    fun getNoticeV3(fromPath: TemplatePath, notice: Map<String, Any?>): PacNotices {
        return PacNotices(
            type = if (notice["receivers"] == null) {
                emptyList<String>()
            } else {
                transValue(fromPath, "type", notice["type"])
            },
            receivers = if (notice["receivers"] == null) {
                null
            } else {
                transValue<List<String>>(fromPath, "receivers", notice["receivers"])
            },
            groups = if (notice["groups"] == null) {
                null
            } else {
                transValue<List<String>>(fromPath, "groups", notice["groups"])
            },
            content = notice["content"]?.toString(),
            ifField = notice["if"]?.toString(),
            chatId = if (notice["chat-id"] == null) {
                null
            } else {
                transValue<List<String>>(fromPath, "chat-id", notice["chat-id"])
            },
            notifyMarkdown = if (notice["content-format"] == null) {
                null
            } else {
                transValue<String>(fromPath, "content-format", notice["content-format"])
            },
            notifyDetail = if (notice["attach-build-url"] == null) {
                null
            } else {
                transValue<Boolean>(fromPath, "attach-build-url", notice["attach-build-url"])
            }
        )
    }

    fun getYamlMetaData(fromPath: TemplatePath, yamlMetaData: Any): MetaData {
        val metaData = transValue<Map<String, Any>>(fromPath, "yamlMetaData", yamlMetaData)
        if (metaData["templateInfo"] == null) {
            return MetaData(templateInfo = null)
        }
        val templateInfo = transValue<Map<String, Any?>>(fromPath, "templateInfo", metaData["templateInfo"]!!)
        return MetaData(
            templateInfo = TemplateInfo(
                remote = getNotNullValue("remote", "templateInfo", templateInfo).toBoolean(),
                remoteTemplateProjectId = getNullValue("remoteTemplateProjectId", templateInfo)
            )
        )
    }

    fun getParameter(fromPath: TemplatePath, param: Map<String, Any>): Parameters {
        return Parameters(
            name = getNotNullValue(key = "name", mapName = TemplateType.PARAMETERS.text, map = param),
            type = getNotNullValue(key = "type", mapName = TemplateType.PARAMETERS.text, map = param),
            default = param["default"],
            values = if (param["values"] == null) {
                null
            } else {
                transValue<List<String>>(fromPath, "values", param["values"])
            }
        )
    }

    fun getResourcePools(fromPath: TemplatePath, resources: Any): List<ResourcesPools> {
        val resourcesD = transValue<Map<String, Any>>(fromPath, "resources", resources)
        if (resourcesD["pools"] == null) {
            return emptyList()
        }
        val poolList = transValue<List<Map<String, Any>>>(fromPath, "pools", resourcesD["pools"])
        return poolList.map { poolD ->
            ResourcesPools(
                from = poolD["from"]?.toString(),
                name = poolD["name"]?.toString()
            )
        }
    }

    inline fun <reified T> getObjectFromYaml(path: TemplatePath, template: String): T {
        return try {
            TemplateYamlMapper.getObjectMapper().readValue(template, object : TypeReference<T>() {})
        } catch (e: Exception) {
            throw YamlFormatException(Constants.YAML_FORMAT_ERROR.format("${path.path} ${path.ref ?: ""}", e.message))
        }
    }

    inline fun <reified T> transValue(file: TemplatePath, type: String, value: Any?): T {
        if (value == null) {
            throw YamlFormatException(Constants.TRANS_AS_ERROR.format(file.toString(), type))
        }
        return try {
            value as T
        } catch (e: Exception) {
            throw YamlFormatException(Constants.TRANS_AS_ERROR.format(file.toString(), type))
        }
    }

    inline fun <reified T> transNullValue(file: TemplatePath, type: String, key: String, map: Map<String, Any?>): T? {
        return if (map[key] == null) {
            null
        } else {
            return try {
                map[key] as T
            } catch (e: Exception) {
                throw YamlFormatException(Constants.TRANS_AS_ERROR.format(file.toString(), type))
            }
        }
    }

    fun getNullValue(key: String, map: Map<String, Any?>): String? {
        return if (map[key] == null) {
            null
        } else {
            map[key].toString()
        }
    }

    private fun getNotNullValue(key: String, mapName: String, map: Map<String, Any?>): String {
        return if (map[key] == null) {
            throw YamlFormatException(Constants.ATTR_MISSING_ERROR.format(key, mapName))
        } else {
            map[key].toString()
        }
    }

    private fun getNotNullValueAny(key: String, mapName: String, map: Map<String, Any?>): Any {
        return if (map[key] == null) {
            throw YamlFormatException(Constants.ATTR_MISSING_ERROR.format(key, mapName))
        } else {
            map[key]!!
        }
    }
}

@Suppress("ComplexMethod")
fun <T> YamlTemplate<T>.getStage(
    fromPath: TemplatePath,
    stage: Map<String, Any>,
    deepTree: TemplateDeepTreeNode
): PreStage {
    return PreStage(
        id = stage["id"]?.toString(),
        enable = YamlObjects.getNullValue("enable", stage)?.toBoolean(),
        name = stage["name"]?.toString(),
        label = stage["label"],
        ifField = stage["if"],
        ifModify = if (stage["if-modify"] is List<*>) {
            val ifModifyList = stage["if-modify"] as List<*>
            ifModifyList.map { it.toString() }.toList()
        } else null,
        fastKill = YamlObjects.getNullValue("fast-kill", stage)?.toBoolean(),
        jobs = if (stage["jobs"] == null) {
            null
        } else {
            val jobs = YamlObjects.transValue<Map<String, Any>>(fromPath, TemplateType.JOB.text, stage["jobs"])
            val map = LinkedHashMap<String, PreJob>()
            jobs.forEach { (key, value) ->
                // 检查根文件处jobId重复
                val newJob = this.replaceJobTemplate(mapOf(key to value), filePath, deepTree)
                if (key == Constants.TEMPLATE_KEY) {
                    TemplateYamlUtil.checkDuplicateKey(filePath = filePath, keys = jobs.keys, newKeys = newJob.keys)
                }
                map.putAll(newJob)
            }
            map
        },
        checkIn = if (stage["check-in"] != null) {
            this.replaceStageCheckTemplate(
                stageName = stage["name"]?.toString() ?: "",
                check = YamlObjects.transValue<Map<String, Any>>(fromPath, TemplateType.GATE.text, stage["check-in"]),
                fromPath = filePath,
                deepTree = deepTree
            )
        } else {
            null
        },
        checkOut = if (stage["check-out"] != null) {
            this.replaceStageCheckTemplate(
                stageName = stage["name"]?.toString() ?: "",
                check = YamlObjects.transValue<Map<String, Any>>(fromPath, TemplateType.GATE.text, stage["check-out"]),
                fromPath = filePath,
                deepTree = deepTree
            )
        } else {
            null
        }
    )
}

// 构造对象,因为未保存远程库的template信息，所以在递归回溯时无法通过yaml文件直接生成，故手动构造
@Suppress("ComplexMethod")
fun <T> YamlTemplate<T>.getJob(fromPath: TemplatePath, job: Map<String, Any>, deepTree: TemplateDeepTreeNode): PreJob {
    val preJob = PreJob(
        enable = YamlObjects.getNullValue("enable", job)?.toBoolean(),
        name = job["name"]?.toString(),
        runsOn = job["runs-on"],
        showRunsOn = YamlObjects.getNullValue("show-runs-on", job)?.toBoolean(),
        mutex = if (job["mutex"] == null) {
            null
        } else {
            YamlObjects.getResourceExclusiveDeclaration(fromPath, job["mutex"]!!)
        },
        container = if (job["container"] == null) {
            null
        } else {
            YamlObjects.getContainer(fromPath, job["container"]!!)
        },
        services = if (job["services"] == null) {
            null
        } else {
            YamlObjects.getService(fromPath, job["services"]!!)
        },
        ifField = job["if"],
        ifModify = if (job["if-modify"] is List<*>) {
            val ifModifyList = job["if-modify"] as List<*>
            ifModifyList.map { it.toString() }.toList()
        } else null,
        steps = if (job["steps"] == null) {
            null
        } else {
            val steps = YamlObjects.transValue<List<Map<String, Any>>>(fromPath, TemplateType.STEP.text, job["steps"])
            val list = mutableListOf<PreStep>()
            steps.forEach {
                list.addAll(this.replaceStepTemplate(listOf(it), filePath, deepTree))
            }
            list
        },
        timeoutMinutes = YamlObjects.getNullValue("timeout-minutes", job),
        env = if (job["env"] == null) {
            null
        } else {
            YamlObjects.transValue<Map<String, String>>(fromPath, "env", job["env"])
        },
        continueOnError = YamlObjects.getNullValue("continue-on-error", job)?.toBoolean(),
        strategy = if (job["strategy"] == null) {
            null
        } else {
            YamlObjects.getStrategy(fromPath, job["strategy"])
        },
        dependOn = if (job["depend-on"] == null) {
            null
        } else {
            YamlObjects.transValue<List<String>>(fromPath, "depend-on", job["depend-on"])
        },
        yamlMetaData = if (job["yamlMetaData"] == null) {
            MetaData(
                templateInfo = TemplateInfo(
                    remote = repo != null,
                    remoteTemplateProjectId = repo?.repository
                )
            )
        } else {
            YamlObjects.getYamlMetaData(fromPath, job["yamlMetaData"]!!)
        }
    )

    // 检测job env合法性
    StreamEnvUtils.checkEnv(preJob.env, fromPath)
    return preJob
}
