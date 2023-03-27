package com.tencent.devops.remotedev.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.Container
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.Credentials
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.Devfile
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.RunsOn
import com.tencent.devops.process.yaml.v2.utils.YamlCommonUtils
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.pojo.PreDevfile
import com.tencent.devops.remotedev.pojo.PreRunsOn
import org.slf4j.LoggerFactory

object DevfileUtil {
    private val logger = LoggerFactory.getLogger(DevfileUtil::class.java)
    fun parseDevfile(fileContent: String): Devfile {
        val preDevfile = kotlin.runCatching {
            YamlCommonUtils.getObjectMapper().readValue(fileContent, object : TypeReference<PreDevfile>() {})
        }.getOrElse {
            logger.warn("yaml parse error $fileContent|${it.message}")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.DEVFILE_ERROR.errorCode,
                defaultMessage = ErrorCodeEnum.DEVFILE_ERROR.formatErrorMessage
                    .format("Please check the file content. Error message: ${it.message}"),
                params = arrayOf("Please check the file content. Error message: ${it.message}")
            )
        }

        val devfileRunsOn = kotlin.runCatching {
            when (preDevfile.runsOn) {
                is String -> RunsOn(container = Container(image = preDevfile.runsOn))
                is Map<*, *> -> {
                    JsonUtil.anyTo(preDevfile.runsOn, object : TypeReference<PreRunsOn>() {}).let {
                        RunsOn(
                            selfHosted = it.selfHosted,
                            poolName = it.poolName,
                            container = it.container?.let { r ->
                                Container(
                                    r.image,
                                    r.host,
                                    when (val c = r.credentials) {
                                        is String -> /*是凭据*/Credentials("", "")
                                        is Map<*, *> -> Credentials(
                                            c[Credentials::username.name] as String,
                                            c[Credentials::password.name] as String
                                        )
                                        null -> null
                                        else -> throw ErrorCodeException(
                                            errorCode = ErrorCodeEnum.DEVFILE_ERROR.errorCode,
                                            defaultMessage = ErrorCodeEnum.DEVFILE_ERROR.formatErrorMessage
                                                .format("devfile parse Credentials error"),
                                            params = arrayOf("devfile parse Credentials error")
                                        )
                                    }
                                )
                            },
                            agentSelector = it.agentSelector,
                            workspace = it.workspace,
                            xcode = it.xcode,
                            queueTimeoutMinutes = it.queueTimeoutMinutes,
                            needs = it.needs
                        )
                    }
                }
                else -> throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.DEVFILE_ERROR.errorCode,
                    defaultMessage = ErrorCodeEnum.DEVFILE_ERROR.formatErrorMessage
                        .format("devfile parse runs-on error"),
                    params = arrayOf("devfile parse runs-on error")
                )
            }
        }.getOrElse {
            logger.warn("yaml parse image error ${preDevfile.runsOn}|${it.message}")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.DEVFILE_ERROR.errorCode,
                defaultMessage = ErrorCodeEnum.DEVFILE_ERROR.formatErrorMessage
                    .format(
                        "An error was reported when parsing the runs-on information, " +
                            "please check the file content. Error message ${it.message}"
                    ),
                params = arrayOf(
                    "An error was reported when parsing the runs-on information, " +
                        "please check the file content. Error message ${it.message}"
                )
            )
        }

        return with(preDevfile) {
            Devfile(
                version = version,
                envs = envs,
                runsOn = devfileRunsOn,
                vscode = vscode,
                ports = ports,
                commands = commands,
                gitEmail = null,
                dotfileRepo = null
            )
        }
    }
}
