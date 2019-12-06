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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.ci

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.common.ci.yaml.Trigger
import com.tencent.devops.common.ci.yaml.MatchRule
import com.tencent.devops.common.ci.yaml.MergeRequest
import com.tencent.devops.common.ci.yaml.JobDetail
import com.tencent.devops.common.ci.yaml.Pool
import com.tencent.devops.common.ci.yaml.Stage
import com.tencent.devops.common.ci.yaml.Job
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.StringReader
import javax.ws.rs.core.Response

object CiYamlUtils {

    private val logger = LoggerFactory.getLogger(CiYamlUtils::class.java)

//    private const val dockerHubUrl = "https://index.docker.io/v1/"
    private const val dockerHubUrl = ""

    fun parseImage(imageNameInput: String): Triple<String, String, String> {
        val imageNameStr = imageNameInput.removePrefix("http://").removePrefix("https://")
        val arry = imageNameStr.split(":")
        if (arry.size == 1) {
            val str = imageNameStr.split("/")
            return if (str.size == 1) {
                Triple(dockerHubUrl, imageNameStr, "latest")
            } else {
                Triple(str[0], imageNameStr.substringAfter(str[0] + "/"), "latest")
            }
        } else if (arry.size == 2) {
            val str = imageNameStr.split("/")
            when {
                str.size == 1 -> return Triple(dockerHubUrl, arry[0], arry[1])
                str.size >= 2 -> return if (str[0].contains(":")) {
                    Triple(str[0], imageNameStr.substringAfter(str[0] + "/"), "latest")
                } else {
                    if (str.last().contains(":")) {
                        val nameTag = str.last().split(":")
                        Triple(str[0], imageNameStr.substringAfter(str[0] + "/").substringBefore(":" + nameTag[1]), nameTag[1])
                    } else {
                        Triple(str[0], str.last(), "latest")
                    }
                }
                else -> {
                    logger.error("image name invalid: $imageNameStr")
                    throw Exception("image name invalid.")
                }
            }
        } else if (arry.size == 3) {
            val str = imageNameStr.split("/")
            if (str.size >= 2) {
                val tail = imageNameStr.removePrefix(str[0] + "/")
                val nameAndTag = tail.split(":")
                if (nameAndTag.size != 2) {
                    logger.error("image name invalid: $imageNameStr")
                    throw Exception("image name invalid.")
                }
                return Triple(str[0], nameAndTag[0], nameAndTag[1])
            } else {
                logger.error("image name invalid: $imageNameStr")
                throw Exception("image name invalid.")
            }
        } else {
            logger.error("image name invalid: $imageNameStr")
            throw Exception("image name invalid.")
        }
    }

    fun formatYaml(yamlStr: String): String {
        val sb = StringBuilder()
        val br = BufferedReader(StringReader(yamlStr))
        val taskTypeRegex = Regex("- $TASK_TYPE:\\s+")
        val mrNoneRegex = Regex("^(mr:)\\s*(none)\$")
        val triggerNoneRegex = Regex("^(mr:)\\s*(none)\$")
        var line: String? = br.readLine()
        while (line != null) {
            val taskTypeMatches = taskTypeRegex.find(line)
            if (null != taskTypeMatches) {
                val taskType = taskTypeMatches.groupValues[0]
                val taskVersion = line.removePrefix(taskType)
                val task = taskVersion.split("@")
                if (task.size != 2 || (task.size == 2 && task[1].isNullOrBlank())) {
                    line = task[0] + "@latest"
                }
            }

            val mrNoneMatches = mrNoneRegex.find(line)
            if (null != mrNoneMatches) {
                line = "mr:" + "\n" + "  enable: false"
            }

            val triggerNoneMatches = triggerNoneRegex.find(line)
            if (null != triggerNoneMatches) {
                line = "trigger:" + "\n" + "  enable: false"
            }

            sb.append(line).append("\n")
            line = br.readLine()
        }
        return sb.toString()
    }

    fun normalizeGitCiYaml(originYaml: CIBuildYaml): CIBuildYaml {
        if (originYaml.stages != null && originYaml.steps != null) {
            logger.error("Invalid yaml: steps and stages conflict") // 不能并列存在steps和stages
            throw CustomException(Response.Status.BAD_REQUEST, "stages和steps不能并列存在!")
        }
        val defaultTrigger = originYaml.trigger ?: Trigger(false, MatchRule(listOf("*"), null), null, null)
        val defaultMr = originYaml.mr ?: MergeRequest(disable = false, autoCancel = true, branches = MatchRule(listOf("*"), null), paths = null)
        val variable = originYaml.variables
        val services = originYaml.services
        val stages = originYaml.stages ?: listOf(Stage(listOf(Job(JobDetail("job1", VM_JOB, Pool(null, null), originYaml.steps!!, null)))))

        // 校验job类型
        stages.forEach {
            it.stage.forEach { job ->
                run {
                    val type = job.job.type
                    if (type != null && type != "" && type != VM_JOB && type != NORMAL_JOB) {
                        throw CustomException(Response.Status.BAD_REQUEST, "非法的job类型")
                    }
                }
            }
        }

        return CIBuildYaml(defaultTrigger, defaultMr, variable, services, stages, null)
    }

    fun normalizePrebuildYaml(originYaml: CIBuildYaml): CIBuildYaml {
        if (originYaml.stages != null && originYaml.steps != null) {
            logger.error("Invalid yaml: steps and stages conflict") // 不能并列存在steps和stages
            throw CustomException(Response.Status.BAD_REQUEST, "stages和steps不能并列存在!")
        }

        val stages = originYaml.stages ?: listOf(Stage(listOf(Job(JobDetail("job1", "vmBuild", Pool(null, null), originYaml.steps!!, null)))))

        return CIBuildYaml(null, null, null, null, stages, null)
    }
}