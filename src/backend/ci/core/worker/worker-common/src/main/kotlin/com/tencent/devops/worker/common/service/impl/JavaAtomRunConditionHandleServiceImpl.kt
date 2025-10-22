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

package com.tencent.devops.worker.common.service.impl

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.store.pojo.common.ATOM_POST_ENTRY_PARAM
import com.tencent.devops.worker.common.JAVA_PATH_ENV
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.service.AtomRunConditionHandleService
import org.slf4j.LoggerFactory
import java.io.File

class JavaAtomRunConditionHandleServiceImpl : AtomRunConditionHandleService {

    companion object {
        private const val BK_CI_JAVA_ATOM_EXECUTE_ENV_PATH = "BK_CI_JAVA_ATOM_EXECUTE_ENV_PATH"
    }

    private val logger = LoggerFactory.getLogger(JavaAtomRunConditionHandleServiceImpl::class.java)

    override fun prepareRunEnv(
        osType: OSType,
        language: String,
        runtimeVersion: String,
        workspace: File
    ): Boolean {
        val runtimeJdkVersion = AgentEnv.getRuntimeJdkVersion()
        val runtimeJdkPath = AgentEnv.getRuntimeJdkPath()
        // 这里首先使用的是插件配置的jdk版本，如果构建机没有该jdk环境，那么打印日志提示并使用worker的jdk版本执行
        val atomJdkVersion = runtimeVersion
        var javaPath = runtimeJdkPath
        if (atomJdkVersion != runtimeJdkVersion) {
            val atomJdkPath = System.getenv(String.format(AgentEnv.AGENT_JDK_PATH, atomJdkVersion))
            if (atomJdkPath.isNullOrBlank()) {
                LoggerService.addWarnLine(
                    "the java$atomJdkVersion environment required by the plugin is not available, " +
                            "so it is run using the agent's java$runtimeJdkVersion environment."
                )
            } else {
                javaPath = atomJdkPath
            }
        }
        System.setProperty(BK_CI_JAVA_ATOM_EXECUTE_ENV_PATH, javaPath)
        return true
    }

    override fun handleAtomTarget(
        target: String,
        osType: OSType,
        postEntryParam: String?
    ): String {
        val executePath = System.getProperty(BK_CI_JAVA_ATOM_EXECUTE_ENV_PATH)
        logger.info(
            "handleAtomTarget|target:$target,osType:$osType,postEntryParam:$postEntryParam,executePath:$executePath"
        )
        var convertTarget = target
        if (!executePath.isNullOrBlank()) {
            convertTarget = if (target.startsWith("\$$JAVA_PATH_ENV")) {
                target.replace("\$$JAVA_PATH_ENV", executePath)
            } else {
                target.replace("java ", "$executePath ")
            }
        } else if (osType == OSType.WINDOWS) {
            convertTarget = target.replace("\$$JAVA_PATH_ENV", "%$JAVA_PATH_ENV%")
        }
        if (postEntryParam != null) {
            convertTarget = convertTarget.replace(
                oldValue = " -jar ", newValue = " -D$ATOM_POST_ENTRY_PARAM=$postEntryParam -jar "
            )
        }
        logger.info("handleAtomTarget convertTarget:$convertTarget")
        return convertTarget
    }

    override fun handleAtomPreCmd(
        preCmd: String,
        osName: String,
        pkgName: String,
        runtimeVersion: String?
    ): String {
        return preCmd
    }
}
