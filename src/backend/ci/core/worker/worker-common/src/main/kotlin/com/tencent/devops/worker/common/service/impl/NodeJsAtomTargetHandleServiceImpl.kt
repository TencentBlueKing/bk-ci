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

package com.tencent.devops.worker.common.service.impl

import com.tencent.devops.common.api.constant.NODEJS
import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.util.PropertyUtil
import com.tencent.devops.common.api.util.script.CommonScriptUtils
import com.tencent.devops.store.pojo.app.BuildEnv
import com.tencent.devops.store.pojo.common.enums.BuildHostTypeEnum
import com.tencent.devops.worker.common.NODEJS_PATH_ENV
import com.tencent.devops.worker.common.service.AtomTargetHandleService
import org.slf4j.LoggerFactory

class NodeJsAtomTargetHandleServiceImpl : AtomTargetHandleService {

    private val logger = LoggerFactory.getLogger(NodeJsAtomTargetHandleServiceImpl::class.java)

    companion object {
        private const val AGENT_PROPERTIES_FILE_NAME = "/.agent.properties"
    }

    override fun handleAtomTarget(
        target: String,
        osType: OSType,
        buildHostType: BuildHostTypeEnum,
        systemEnvVariables: Map<String, String>,
        buildEnvs: List<BuildEnv>,
        postEntryParam: String?
    ): String {
        logger.info("handleAtomTarget target:$target,osType:$osType,buildHostType:$buildHostType,buildEnvs:$buildEnvs")
        // npm install命令兼容使用第三方依赖的插件包的情况
        val installCmd = "npm install --unsafe-perm"
        val command = StringBuilder()
        val machineNodeSwitch = PropertyUtil.getPropertyValue("machine.node.switch", AGENT_PROPERTIES_FILE_NAME)
        if (machineNodeSwitch.toBoolean()) {
            var nodeEnvFlag = false
            try {
                // 探测构建机上是否有node环境
                CommonScriptUtils.execute("node -v")
                nodeEnvFlag = true
            } catch (ignored: Throwable) {
                logger.warn("No node environment", ignored)
            }
            if (nodeEnvFlag) {
                addInstallCmd(command, installCmd, osType)
                // 如果构建机上有安装node，则直接使用户配置的node插件启动命令
                command.append(target)
                return command.toString()
            }
        }
        var convertTarget = target
        // 当构建机为公共构建机并且没有配置nodejs依赖、node命令也没有加到path变量中，则用系统默认配置的nodejs环境执行
        if (buildHostType == BuildHostTypeEnum.PUBLIC) {
            var flag = false
            for (it in buildEnvs) {
                if (it.name == NODEJS) {
                    flag = true
                    break
                }
            }
            var finallyInstallCmd = installCmd
            if (!flag) {
                val executePath = systemEnvVariables[NODEJS_PATH_ENV]
                finallyInstallCmd = "$executePath$installCmd"
                convertTarget = "$executePath$target"
            }
            addInstallCmd(command, finallyInstallCmd, osType)
        } else {
            addInstallCmd(command, installCmd, osType)
        }
        if (!postEntryParam.isNullOrBlank()) {
            convertTarget = "$target --post-action=$postEntryParam"
        }
        logger.info("handleAtomTarget convertTarget:$convertTarget")
        command.append(convertTarget)
        return command.toString()
    }

    private fun addInstallCmd(command: StringBuilder, installCmd: String, osType: OSType) {
        command.append(installCmd)
        if (osType == OSType.WINDOWS) {
            command.append("\r\n")
        } else {
            command.append("\n")
        }
    }
}
