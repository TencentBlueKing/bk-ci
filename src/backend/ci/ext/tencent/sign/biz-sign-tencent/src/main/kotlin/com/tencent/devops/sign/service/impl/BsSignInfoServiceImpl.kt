package com.tencent.devops.sign.service.impl

import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.dao.SignHistoryDao
import com.tencent.devops.sign.dao.SignIpaInfoDao
import com.tencent.devops.sign.service.SignInfoService
import com.tencent.devops.sign.utils.IpaFileUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class BsSignInfoServiceImpl(
    private val dslContext: DSLContext,
    private val signIpaInfoDao: SignIpaInfoDao,
    private val signHistoryDao: SignHistoryDao
) : SignInfoService {

    companion object {
        private val logger = LoggerFactory.getLogger(BsSignInfoServiceImpl::class.java)
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

    override fun finishUpload(resignId: String, ipaFile: File, buildId: String?) {
        logger.info("[$resignId] finishUpload|ipaFile=${ipaFile.canonicalPath}|buildId=$buildId")
        signHistoryDao.finishUpload(dslContext, resignId)
    }

    override fun finishUnzip(resignId: String, unzipDir: File, buildId: String?) {
        logger.info("[$resignId] finishUnzip|unzipDir=${unzipDir.canonicalPath}|buildId=$buildId")
        signHistoryDao.finishUnzip(dslContext, resignId)
    }

    override fun finishResign(resignId: String, buildId: String?) {
        logger.info("[$resignId] finishResign|buildId=$buildId")
        signHistoryDao.finishResign(dslContext, resignId)
    }

    override fun finishZip(resignId: String, signedIpaFile: File, buildId: String?) {
        val resultFileMd5 = IpaFileUtil.getMD5(signedIpaFile)
        logger.info("[$resignId] finishZip|resultFileMd5=$resultFileMd5|signedIpaFile=${signedIpaFile.canonicalPath}|buildId=$buildId")
        signHistoryDao.finishZip(dslContext, resignId, resultFileMd5)
    }

    override fun finishArchive(resignId: String, downloadUrl: String, buildId: String?) {
        logger.info("[$resignId] finishArchive|downloadUrl=$downloadUrl|buildId=$buildId")
        signHistoryDao.finishArchive(
            dslContext = dslContext,
            resignId = resignId,
            downloadUrl = downloadUrl
        )
    }
}