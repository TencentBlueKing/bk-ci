package com.tencent.devops.sign.service.impl

import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.dao.SignHistoryDao
import com.tencent.devops.sign.dao.SignIpaInfoDao
import com.tencent.devops.sign.service.SignInfoService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class SignInfoServiceImpl(
    private val dslContext: DSLContext,
    private val signIpaInfoDao: SignIpaInfoDao,
    private val signHistoryDao: SignHistoryDao
) : SignInfoService {

    companion object {
        private val logger = LoggerFactory.getLogger(SignInfoServiceImpl::class.java)
    }

    override fun save(resignId: String, ipaSignInfoHeader: String, info: IpaSignInfo) {
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

    override fun finishUpload(resignId: String, ipaFile: File) {
        logger.info("[$resignId] finishUpload|ipaFile=${ipaFile.canonicalPath}")
        signHistoryDao.finishUpload(dslContext, resignId)
    }

    override fun finishSign(resignId: String, resultFileMd5: String, downloadUrl: String) {
        logger.info("[$resignId] finishSign|resultFileMd5=$resultFileMd5|downloadUrl=$downloadUrl")
        signHistoryDao.finishSign(
            dslContext = dslContext,
            resignId = resignId,
            resultFileMd5 = resultFileMd5,
            downloadUrl = downloadUrl
        )
    }
}