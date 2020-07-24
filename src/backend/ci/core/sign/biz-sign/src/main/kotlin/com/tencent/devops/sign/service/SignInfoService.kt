package com.tencent.devops.sign.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.api.pojo.SignResult
import com.tencent.devops.sign.dao.SignHistoryDao
import com.tencent.devops.sign.dao.SignIpaInfoDao
import com.tencent.devops.sign.utils.IpaFileUtil
import com.tencent.devops.sign.utils.SignUtils.DEFAULT_CER_ID
import org.jolokia.util.Base64Util
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.io.File

class SignInfoService(
    private val dslContext: DSLContext,
    private val signIpaInfoDao: SignIpaInfoDao,
    private val signHistoryDao: SignHistoryDao
){

    fun save(resignId: String, ipaSignInfoHeader: String, info: IpaSignInfo) {
        logger.info("[$resignId] save ipaSignInfo|header=$ipaSignInfoHeader|info=$info")
        signIpaInfoDao.saveSignInfo(dslContext, resignId, ipaSignInfoHeader, info)
        signHistoryDao.initHistory(
            dslContext = dslContext,
            resignId = resignId,
            userId = info.userId,
            projectId = info.projectId,
            pipelineId = info.projectId,
            buildId = info.buildId,
            archiveType = info.archiveType,
            archivePath = info.archivePath,
            md5 = info.md5
        )
    }

    fun finishUpload(resignId: String, ipaFile: File, buildId: String?) {
        logger.info("[$resignId] finishUpload|ipaFile=${ipaFile.canonicalPath}|buildId=$buildId")
        signHistoryDao.finishUpload(dslContext, resignId)
    }

    fun finishUnzip(resignId: String, unzipDir: File, buildId: String?) {
        logger.info("[$resignId] finishUnzip|unzipDir=${unzipDir.canonicalPath}|buildId=$buildId")
        signHistoryDao.finishUnzip(dslContext, resignId)
    }

     fun finishResign(resignId: String, buildId: String?) {
        logger.info("[$resignId] finishResign|buildId=$buildId")
        signHistoryDao.finishResign(dslContext, resignId)
    }

    fun finishZip(resignId: String, signedIpaFile: File, buildId: String?) {
        val resultFileMd5 = IpaFileUtil.getMD5(signedIpaFile)
        logger.info("[$resignId] finishZip|resultFileMd5=$resultFileMd5|signedIpaFile=${signedIpaFile.canonicalPath}|buildId=$buildId")
        signHistoryDao.finishZip(dslContext, resignId, resultFileMd5)
    }

    fun finishArchive(resignId: String, buildId: String?) {
        logger.info("[$resignId] finishArchive|buildId=$buildId")
        signHistoryDao.finishArchive(
            dslContext = dslContext,
            resignId = resignId
        )
    }

    fun getSignResult(resignId: String): SignResult {
        val record = signHistoryDao.getSignHistory(dslContext, resignId)
        return if (record?.archiveFinishTime != null) SignResult(
            resignId = record.resignId,
            finished = true,
            finishdTime = record.archiveFinishTime
        ) else SignResult(
            resignId = resignId,
            finished = false,
            finishdTime = null
        )
    }

    /*
    * 检查IpaSignInfo信息，并补齐默认值，如果返回null则表示IpaSignInfo的值不合法
    * */
    fun check(info: IpaSignInfo): IpaSignInfo {
        if (info.certId.isBlank()) info.certId = DEFAULT_CER_ID
        if (!info.wildcard) {
            if (info.mobileProvisionId.isNullOrBlank())
                throw ErrorCodeException(errorCode = SignMessageCode.ERROR_CHECK_SIGN_INFO_HEADER, defaultMessage = "非通配符重签未指定主描述文件")
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
            val ipaSignInfoJson =objectMapper.writeValueAsString(ipaSignInfo)
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