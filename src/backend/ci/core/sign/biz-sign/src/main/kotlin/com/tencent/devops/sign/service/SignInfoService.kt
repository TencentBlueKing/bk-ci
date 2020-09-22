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

package com.tencent.devops.sign.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.enums.EnumResignStatus
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.api.pojo.SignResult
import com.tencent.devops.sign.dao.SignHistoryDao
import com.tencent.devops.sign.dao.SignIpaInfoDao
import com.tencent.devops.sign.utils.IpaFileUtil
import org.jolokia.util.Base64Util
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class SignInfoService(
    private val dslContext: DSLContext,
    private val signIpaInfoDao: SignIpaInfoDao,
    private val signHistoryDao: SignHistoryDao,
    private val buildLogPrinter: BuildLogPrinter
) {

    fun save(resignId: String, ipaSignInfoHeader: String, info: IpaSignInfo): Int {
        logger.info("[$resignId] save ipaSignInfo|header=$ipaSignInfoHeader|info=$info")
        signIpaInfoDao.saveSignInfo(dslContext, resignId, ipaSignInfoHeader, info)
        val executeCount = signHistoryDao.initHistory(
            dslContext = dslContext,
            resignId = resignId,
            userId = info.userId,
            projectId = info.projectId,
            pipelineId = info.pipelineId,
            buildId = info.buildId,
            taskId = info.taskId,
            archiveType = info.archiveType,
            archivePath = info.archivePath,
            md5 = info.md5
        )
        if (!info.buildId.isNullOrBlank() && !info.taskId.isNullOrBlank()) buildLogPrinter.addLine(
            buildId = info.buildId!!,
            message = "Start resign ipa package with info: $info",
            tag = info.taskId!!,
            jobId = null,
            executeCount = executeCount
        )
        return executeCount
    }

    fun finishUpload(resignId: String, ipaFile: File, info: IpaSignInfo, executeCount: Int) {
        logger.info("[$resignId] finishUpload|ipaFile=${ipaFile.canonicalPath}|buildId=${info.buildId}")
        if (!info.buildId.isNullOrBlank() && !info.taskId.isNullOrBlank()) buildLogPrinter.addLine(
            buildId = info.buildId!!,
            message = "Finished ipa package upload: ${ipaFile.name}",
            tag = info.taskId!!,
            jobId = null,
            executeCount = executeCount
        )
        signHistoryDao.finishUpload(dslContext, resignId)
    }

    fun finishUnzip(resignId: String, unzipDir: File, info: IpaSignInfo, executeCount: Int) {
        logger.info("[$resignId] finishUnzip|unzipDir=${unzipDir.canonicalPath}|buildId=${info.buildId}")
        if (!info.buildId.isNullOrBlank() && !info.taskId.isNullOrBlank()) buildLogPrinter.addLine(
            buildId = info.buildId!!,
            message = "Finished unzip ipa package: ${unzipDir.name}",
            tag = info.taskId!!,
            jobId = null,
            executeCount = executeCount
        )
        signHistoryDao.finishUnzip(dslContext, resignId)
    }

    fun finishResign(resignId: String, info: IpaSignInfo, executeCount: Int) {
        logger.info("[$resignId] finishResign|buildId=${info.buildId}")
        if (!info.buildId.isNullOrBlank() && !info.taskId.isNullOrBlank()) buildLogPrinter.addLine(
            buildId = info.buildId!!,
            message = "Finished resign!",
            tag = info.taskId!!,
            jobId = null,
            executeCount = executeCount
        )
        signHistoryDao.finishResign(dslContext, resignId)
    }

    fun finishZip(resignId: String, signedIpaFile: File, info: IpaSignInfo, executeCount: Int) {
        val resultFileMd5 = IpaFileUtil.getMD5(signedIpaFile)
        logger.info("[$resignId] finishZip|resultFileMd5=$resultFileMd5|signedIpaFile=${signedIpaFile.canonicalPath}|buildId=${info.buildId}")
        if (!info.buildId.isNullOrBlank() && !info.taskId.isNullOrBlank()) buildLogPrinter.addLine(
            buildId = info.buildId!!,
            message = "Finished zip the signed ipa file with result:${signedIpaFile.name}",
            tag = info.taskId!!,
            jobId = null,
            executeCount = executeCount
        )
        signHistoryDao.finishZip(dslContext, resignId, signedIpaFile.name, resultFileMd5)
    }

    fun finishArchive(resignId: String, info: IpaSignInfo, executeCount: Int) {
        logger.info("[$resignId] finishArchive|buildId=${info.buildId}")
        if (!info.buildId.isNullOrBlank() && !info.taskId.isNullOrBlank()) buildLogPrinter.addLine(
            buildId = info.buildId!!,
            message = "Finished archive the signed ipa file. (archiveType=${info.archiveType},archivePath=${info.archivePath})",
            tag = info.taskId!!,
            jobId = null,
            executeCount = executeCount
        )
        signHistoryDao.finishArchive(
            dslContext = dslContext,
            resignId = resignId
        )
    }

    fun successResign(resignId: String, info: IpaSignInfo, executeCount: Int) {
        logger.info("[$resignId] success resign|buildId=${info.buildId}")
        if (!info.buildId.isNullOrBlank() && !info.taskId.isNullOrBlank()) buildLogPrinter.addLine(
            buildId = info.buildId!!,
            message = "End resign ipa file.",
            tag = info.taskId!!,
            jobId = null,
            executeCount = executeCount
        )
        signHistoryDao.successResign(
            dslContext = dslContext,
            resignId = resignId
        )
    }

    fun failResign(resignId: String, info: IpaSignInfo, executeCount: Int = 1, message: String) {
        logger.info("[$resignId] fail resign|buildId=${info.buildId}")
        if (!info.buildId.isNullOrBlank() && !info.taskId.isNullOrBlank()) buildLogPrinter.addLine(
            buildId = info.buildId!!,
            message = message,
            tag = info.taskId!!,
            jobId = null,
            executeCount = executeCount
        )
        signHistoryDao.failResign(
            dslContext = dslContext,
            resignId = resignId,
            message = message
        )
    }

    fun getSignStatus(resignId: String): SignResult {
        val record = signHistoryDao.getSignHistory(dslContext, resignId)
        val status = EnumResignStatus.parse(record?.status)
        return SignResult(
            resignId = resignId,
            status = status.getValue(),
            message = when (status) {
                EnumResignStatus.SUCCESS -> "Sign finished."
                EnumResignStatus.RUNNING -> "Sign is running..."
                else -> record?.errorMessage ?: "Unknown error."
            }
        )
    }

    /*
    * 检查IpaSignInfo信息，并补齐默认值，如果返回null则表示IpaSignInfo的值不合法
    * */
    fun check(info: IpaSignInfo): IpaSignInfo {
        if (!info.wildcard) {
            if (info.mobileProvisionId.isNullOrBlank())
                throw ErrorCodeException(errorCode = SignMessageCode.ERROR_CHECK_SIGN_INFO_HEADER, defaultMessage = "非通配符重签未指定主描述文件")
            if (info.certId.isBlank())
                throw ErrorCodeException(errorCode = SignMessageCode.ERROR_CHECK_SIGN_INFO_HEADER, defaultMessage = "非通配符重签未指定证书SHA")
        }
        return info
    }

    fun decodeIpaSignInfo(ipaSignInfoHeader: String, objectMapper: ObjectMapper): IpaSignInfo {
        try {
            val ipaSignInfoHeaderDecode = String(Base64Util.decode(ipaSignInfoHeader))
            return objectMapper.readValue(ipaSignInfoHeaderDecode, IpaSignInfo::class.java)
        } catch (e: Exception) {
            logger.error("解析签名信息失败：$e")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_PARSE_SIGN_INFO_HEADER, defaultMessage = "解析签名信息失败")
        }
    }

    fun encodeIpaSignInfo(ipaSignInfo: IpaSignInfo): String {
        try {
            val objectMapper = ObjectMapper()
            val ipaSignInfoJson = objectMapper.writeValueAsString(ipaSignInfo)
            return Base64Util.encode(ipaSignInfoJson.toByteArray())
        } catch (e: Exception) {
            logger.error("编码签名信息失败：$e")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_ENCODE_SIGN_INFO, defaultMessage = "编码签名信息失败")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SignInfoService::class.java)
    }
}