package com.tencent.devops.remotedev.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.Devfile
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.DevfileImage
import com.tencent.devops.process.yaml.v2.utils.YamlCommonUtils
import com.tencent.devops.remotedev.pojo.PreDevfile
import org.slf4j.LoggerFactory
import javax.ws.rs.core.Response

object DevfileUtil {
    private val logger = LoggerFactory.getLogger(DevfileUtil::class.java)
    fun parseDevfile(fileContent: String): Devfile {
        val preDevfile = kotlin.runCatching {
            YamlCommonUtils.getObjectMapper().readValue(fileContent, object : TypeReference<PreDevfile>() {})
        }.getOrElse {
            logger.warn("yaml parse error $fileContent|${it.message}")
            throw CustomException(Response.Status.BAD_REQUEST, "devfile解析报错，请检查文件内容。错误信息: ${it.message}")
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
                else -> throw ParamBlankException("devfile parse image error")
            }
        }.getOrElse {
            logger.warn("yaml parse image error ${preDevfile.image}|${it.message}")
            throw CustomException(
                Response.Status.BAD_REQUEST,
                "devfile解析image信息报错，请检查文件内容。错误信息: ${it.message}"
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
                gitEmail = gitEmail
            )
        }
    }
}
