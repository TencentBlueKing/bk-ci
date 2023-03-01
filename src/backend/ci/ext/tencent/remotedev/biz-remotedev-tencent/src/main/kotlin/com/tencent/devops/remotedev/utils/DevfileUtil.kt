package com.tencent.devops.remotedev.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.Devfile
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.DevfileImage
import com.tencent.devops.process.yaml.v2.utils.YamlCommonUtils
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.pojo.PreDevfile
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

        val devfileImage = kotlin.runCatching {
            when (preDevfile.image) {
                is String -> DevfileImage(publicImage = preDevfile.image, file = null)
                is Map<*, *> -> {
                    JsonUtil.anyTo(preDevfile.image, object : TypeReference<DevfileImage>() {}).let {
                        DevfileImage(
                            publicImage = it.publicImage,
                            file = it.file,
                            imagePullCertificate = it.imagePullCertificate
                        )
                    }
                }
                else -> throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.DEVFILE_ERROR.errorCode,
                    defaultMessage = ErrorCodeEnum.DEVFILE_ERROR.formatErrorMessage
                        .format("devfile parse image error"),
                    params = arrayOf("devfile parse image error")
                )
            }
        }.getOrElse {
            logger.warn("yaml parse image error ${preDevfile.image}|${it.message}")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.DEVFILE_ERROR.errorCode,
                defaultMessage = ErrorCodeEnum.DEVFILE_ERROR.formatErrorMessage
                    .format(
                        "An error was reported when parsing the image information, " +
                            "please check the file content. Error message ${it.message}"
                    ),
                params = arrayOf(
                    "An error was reported when parsing the image information, " +
                        "please check the file content. Error message ${it.message}"
                )
            )
        }

        return with(preDevfile) {
            Devfile(
                version = version,
                envs = envs,
                image = devfileImage,
                vscode = vscode,
                ports = ports,
                commands = commands,
                gitEmail = null,
                dotfileRepo = null
            )
        }
    }
}
