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

package com.tencent.devops.environment.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.environment.constant.BATCH_TOKEN_HEADER
import com.tencent.devops.environment.pojo.enums.AgentType
import com.tencent.devops.environment.pojo.thirdpartyagent.TPAInstallType
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLEncoder

@Service
open class AgentUrlService @Autowired constructor(
    private val commonConfig: CommonConfig
) {
    fun genAgentInstallUrl(agentRecord: TEnvironmentThirdpartyAgentRecord): String {
        val gw = genGateway(agentRecord)
        val agentHashId = HashUtil.encodeLongId(agentRecord.id)
        return "$gw/ms/environment/api/external/thirdPartyAgent/$agentHashId/install"
    }

    /**
     *生成Agent URL
     */
    fun genAgentUrl(agentRecord: TEnvironmentThirdpartyAgentRecord): String {
        val gw = genGateway(agentRecord)
        val agentHashId = HashUtil.encodeLongId(agentRecord.id)
        return if (agentRecord.os == OS.WINDOWS.name) {
            // windows下不需要区分架构，删除arch
            "$gw/ms/environment/api/external/thirdPartyAgent/$agentHashId/agent"
        } else {
            "$gw/ms/environment/api/external/thirdPartyAgent/$agentHashId/agent?arch=\${ARCH}"
        }
    }

    /**
     * 生成构建机脚本下载链接
     */
    fun genAgentInstallScript(agentRecord: TEnvironmentThirdpartyAgentRecord): String {
        val installUrl = genAgentInstallUrl(agentRecord)
        return if (agentRecord.os != OS.WINDOWS.name) {
            "curl -H \"$AUTH_HEADER_DEVOPS_PROJECT_ID: ${agentRecord.projectId}\" $installUrl | bash"
        } else {
            ""
        }
    }

    /**
     * 生成批量下载构建机脚本链接
     */
    fun genAgentBatchInstallScript(
        os: OS,
        zoneName: String?,
        gateway: String?,
        token: String,
        loginName: String?,
        loginPassword: String?,
        installType: TPAInstallType?,
        reInstallId: String?,
        agentType: AgentType?
    ): String {
        val gw = fixGateway(gateway)
        if (os == OS.WINDOWS) {
            var sc = "\$ProgressPreference = 'SilentlyContinue';" +
                    "\$headers = @{ \"$BATCH_TOKEN_HEADER\" = \"$token\" };" +
                    "\$uri = \"$gw/ms/environment/api/external/thirdPartyAgent/${os.name}/batchInstall"
            var t = "?"
            if (!zoneName.isNullOrBlank()) {
                sc += "${t}zoneName=$zoneName"
                t = "&"
            }
            if (!loginName.isNullOrBlank()) {
                sc += "${t}loginName=${URLEncoder.encode(loginName, "UTF-8")}"
                t = "&"
            }
            if (!loginPassword.isNullOrBlank()) {
                sc += "${t}loginPassword=${URLEncoder.encode(loginPassword, "UTF-8")}"
                t = "&"
            }
            if (installType != null) {
                sc += "${t}installType=$installType"
                t = "&"
            }
            if (reInstallId != null) {
                sc += "${t}reInstallId=$reInstallId"
                t = "&"
            }
            if (agentType != null) {
                sc += "${t}agentType=${agentType.name}"
                t = "&"
            }
            sc += "\";\$webClient = New-Object System.Net.WebClient;" +
                    "foreach (\$key in \$headers.Keys) {\$webClient.Headers.Add(\$key, \$headers[\$key])};"
            sc += "\$ps = \$webClient.DownloadString(\$uri);Invoke-Expression -Command \$ps"
            return sc
        }
        var url = "curl -H \"$BATCH_TOKEN_HEADER: $token\" " +
                "\"$gw/ms/environment/api/external/thirdPartyAgent/${os.name}/batchInstall"
        var t = "?"
        if (!zoneName.isNullOrBlank()) {
            url += "${t}zoneName=$zoneName"
            t = "&"
        }
        if (reInstallId != null) {
            url += "${t}reInstallId=$reInstallId"
            t = "&"
        }
        return "$url\" | bash"
    }

    /**
     * 生成安装会话对应的构建机安装命令，具体配置由后台会话快照决定。
     */
    fun genAgentSessionInstallScript(os: OS, gateway: String?, token: String): String {
        val gw = fixGateway(gateway)
        val url = "$gw/ms/environment/api/external/thirdPartyAgent/${os.name}/sessionInstall"
        return if (os == OS.WINDOWS) {
            "\$ProgressPreference = 'SilentlyContinue';" +
                    "\$headers = @{ \"$BATCH_TOKEN_HEADER\" = \"$token\" };" +
                    "\$webClient = New-Object System.Net.WebClient;" +
                    "foreach (\$key in \$headers.Keys) {\$webClient.Headers.Add(\$key, \$headers[\$key])};" +
                    "\$ps = \$webClient.DownloadString(\"$url\");" +
                    "Invoke-Expression -Command \$ps"
        } else {
            "curl -H \"$BATCH_TOKEN_HEADER: $token\" \"$url\" | bash"
        }
    }

    /**
     * 生成网关域名
     */
    fun genGateway(agentRecord: TEnvironmentThirdpartyAgentRecord): String {
        return fixGateway(agentRecord.gateway)
    }

    /**
     * 生成文件网关域名
     */
    fun genFileGateway(agentRecord: TEnvironmentThirdpartyAgentRecord): String {
        return if (agentRecord.fileGateway.isNullOrBlank()) {
            genGateway(agentRecord)
        } else {
            fixGateway(agentRecord.fileGateway)
        }
    }

    /**
     * 调整gateway格式
     */
    fun fixGateway(gateway: String?): String {
        val gw = if (gateway.isNullOrBlank()) commonConfig.devopsBuildGateway else gateway
        return if (gw!!.startsWith("http")) {
            gw.removeSuffix("/")
        } else {
            "http://$gw"
        }
    }
}
