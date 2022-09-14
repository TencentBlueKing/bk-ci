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

package com.tencent.devops.sign.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.enums.EnumResignStatus
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.api.pojo.SignDetail
import com.tencent.devops.sign.api.pojo.SignHistory
import com.tencent.devops.sign.dao.SignHistoryDao
import com.tencent.devops.sign.dao.SignIpaInfoDao
import com.tencent.devops.sign.utils.IpaFileUtil
import org.jolokia.util.Base64Util
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
@Suppress("TooManyFunctions")
class SignInfoService(
    private val dslContext: DSLContext,
    private val signIpaInfoDao: SignIpaInfoDao,
    private val signHistoryDao: SignHistoryDao
) {

    fun listHistory(
        userId: String,
        startTime: Long?,
        endTime: Long?,
        offset: Int,
        limit: Int
    ): SQLPage<SignHistory> {
        var startTimeTemp = startTime
        if (startTimeTemp == null) {
            startTimeTemp = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).timestampmilli()
        }
        var endTimeTemp = endTime
        if (endTimeTemp == null) {
            endTimeTemp = LocalDateTime.of(LocalDate.now(), LocalTime.MAX).timestampmilli()
        }

        logger.info("list sign history param|$userId|$startTimeTemp|$endTimeTemp")
        val count = signHistoryDao.count(
            dslContext = dslContext,
            startTime = startTimeTemp,
            endTime = endTimeTemp
        )
        val records = signHistoryDao.list(
            dslContext = dslContext,
            startTime = startTimeTemp,
            endTime = endTimeTemp,
            offset = offset,
            limit = limit
        )
        return SQLPage(
            count,
            records.map {
                val history = signHistoryDao.convert(it)
                val content = signIpaInfoDao.getSignInfoRecord(dslContext, history.resignId)
                content?.let { info ->
                    history.ipaSignInfoStr = String(Base64Util.decode(info.requestContent))
                }
                history
            }
        )
    }

    fun save(resignId: String, ipaSignInfoHeader: String, info: IpaSignInfo): Int {
        logger.info("[$resignId] save ipaSignInfo|header=$ipaSignInfoHeader|info=$info")
        signIpaInfoDao.saveSignInfo(dslContext, resignId, ipaSignInfoHeader, info)
        return signHistoryDao.initHistory(
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
    }

    fun finishUpload(resignId: String, ipaFile: File, info: IpaSignInfo, executeCount: Int) {
        logger.info("[$resignId] finishUpload|ipaFile=${ipaFile.canonicalPath}|" +
            "buildId=${info.buildId}|executeCount=$executeCount")
        signHistoryDao.finishUpload(dslContext, resignId)
    }

    fun finishUnzip(resignId: String, unzipDir: File, info: IpaSignInfo, executeCount: Int) {
        logger.info("[$resignId] finishUnzip|unzipDir=${unzipDir.canonicalPath}" +
            "buildId=${info.buildId}|executeCount=$executeCount")
        signHistoryDao.finishUnzip(dslContext, resignId)
    }

    fun finishResign(resignId: String, info: IpaSignInfo, executeCount: Int) {
        logger.info("[$resignId] finishResign|buildId=${info.buildId}|executeCount=$executeCount")
        signHistoryDao.finishResign(dslContext, resignId)
    }

    fun finishZip(resignId: String, signedIpaFile: File, info: IpaSignInfo, executeCount: Int) {
        val resultFileMd5 = IpaFileUtil.getMD5(signedIpaFile)
        logger.info("[$resignId] finishZip|resultFileMd5=$resultFileMd5|" +
            "signedIpaFile=${signedIpaFile.canonicalPath}|buildId=${info.buildId}|executeCount=$executeCount")
        signHistoryDao.finishZip(dslContext, resignId, signedIpaFile.name, resultFileMd5)
    }

    fun finishArchive(resignId: String, info: IpaSignInfo, executeCount: Int) {
        logger.info("[$resignId] finishArchive|buildId=${info.buildId}|executeCount=$executeCount")
        signHistoryDao.finishArchive(
            dslContext = dslContext,
            resignId = resignId
        )
    }

    fun successResign(resignId: String, info: IpaSignInfo, executeCount: Int) {
        logger.info("[$resignId] success resign|buildId=${info.buildId}|executeCount=$executeCount")
        signHistoryDao.successResign(
            dslContext = dslContext,
            resignId = resignId
        )
    }

    fun failResign(resignId: String, info: IpaSignInfo, executeCount: Int = 1, message: String) {
        logger.info("[$resignId] fail resign|buildId=${info.buildId}|executeCount=$executeCount")
        signHistoryDao.failResign(
            dslContext = dslContext,
            resignId = resignId,
            message = message
        )
    }

    fun getSignStatus(resignId: String): EnumResignStatus {
        val record = signHistoryDao.getSignHistory(dslContext, resignId)
        return EnumResignStatus.parse(record?.status)
    }

    fun getSignDetail(resignId: String): SignDetail {
        val record = signHistoryDao.getSignHistory(dslContext, resignId)
        val status = EnumResignStatus.parse(record?.status)
        return SignDetail(
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
            if (info.mobileProvisionId.isNullOrBlank()) {
                throw ErrorCodeException(
                    errorCode = SignMessageCode.ERROR_CHECK_SIGN_INFO_HEADER,
                    defaultMessage = "非通配符重签未指定主描述文件"
                )
            }
            if (info.certId.isBlank()) {
                throw ErrorCodeException(
                    errorCode = SignMessageCode.ERROR_CHECK_SIGN_INFO_HEADER,
                    defaultMessage = "非通配符重签未指定证书SHA"
                )
            }
        }
        if (info.fileName.isBlank()) {
            throw ErrorCodeException(
                errorCode = SignMessageCode.ERROR_CHECK_SIGN_INFO_HEADER,
                defaultMessage = "文件名不能为空"
            )
        }
        return info
    }

    fun decodeIpaSignInfo(ipaSignInfoHeader: String, objectMapper: ObjectMapper): IpaSignInfo {
        try {
            val ipaSignInfoHeaderDecode = String(Base64Util.decode(ipaSignInfoHeader))
            return objectMapper.readValue(ipaSignInfoHeaderDecode, IpaSignInfo::class.java)
        } catch (ignore: Throwable) {
            logger.warn("解析签名信息失败：$ignore")
            throw ErrorCodeException(
                errorCode = SignMessageCode.ERROR_PARSE_SIGN_INFO_HEADER,
                defaultMessage = "解析签名信息失败"
            )
        }
    }

    fun encodeIpaSignInfo(ipaSignInfo: IpaSignInfo): String {
        try {
            val objectMapper = ObjectMapper()
            val ipaSignInfoJson = objectMapper.writeValueAsString(ipaSignInfo)
            return Base64Util.encode(ipaSignInfoJson.toByteArray())
        } catch (ignored: Throwable) {
            logger.warn("编码签名信息失败：$ignored")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_ENCODE_SIGN_INFO, defaultMessage = "编码签名信息失败")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SignInfoService::class.java)
    }
}
