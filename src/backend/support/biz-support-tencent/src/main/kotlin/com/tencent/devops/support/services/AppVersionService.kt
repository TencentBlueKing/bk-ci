package com.tencent.devops.support.services

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.support.dao.AppVersionDao
import com.tencent.devops.support.model.app.AppVersionRequest
import com.tencent.devops.support.model.app.pojo.AppVersion
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.tencent.devops.model.support.tables.records.TAppVersionRecord
import org.jooq.DSLContext

@Service
class AppVersionService @Autowired constructor(
    private val dslContext: DSLContext,
    private val appVersionDao: AppVersionDao
) {
    private val logger = LoggerFactory.getLogger(AppVersionService::class.java)

    fun getAllAppVersion(): List<AppVersion> {
        val appVersionResult = mutableListOf<AppVersion>()
        var appVersionRecords = appVersionDao.getAllAppVersion(dslContext)
        appVersionRecords?.forEach {
            appVersionResult.add(
                    AppVersion(
                            it.id,
                            it.versionId,
                            it.releaseDate.timestampmilli(),
                            it.releaseContent,
                            it.channelType
                    )
            )
        }
        return appVersionResult
    }

    fun getAllAppVersionByChannelType(channelType: Byte): List<AppVersion> {
        val appVersionResult = mutableListOf<AppVersion>()
        var appVersionRecords = appVersionDao.getAllAppVersionByChannelType(dslContext, channelType)
        appVersionRecords?.forEach {
            appVersionResult.add(
                    AppVersion(
                            it.id,
                            it.versionId,
                            it.releaseDate.timestampmilli(),
                            it.releaseContent,
                            it.channelType
                    )
            )
        }
        return appVersionResult
    }

    fun getAppVersion(appVersionId: Long): AppVersion? {
        var appVersionRecord = appVersionDao.getAppVersion(dslContext, appVersionId)
        return if (appVersionRecord == null) {
            null
        } else {
            convertAppVersion(appVersionRecord)
        }
    }

    fun setAppVersion(appVersionId: Long? = null, versionRequest: AppVersionRequest): Int {
        return appVersionDao.setAppVersion(dslContext, appVersionId, versionRequest)
    }

    fun deleteAppVersion(appVersionId: Long): Int {
        return appVersionDao.deleteAppVersion(dslContext, appVersionId)
    }

    fun getLastAppVersion(channelType: Byte): AppVersion? {
        val appVersionRecord = appVersionDao.getLastAppVersion(dslContext, channelType)
        return if (appVersionRecord == null) {
            null
        } else {
            convertAppVersion(appVersionRecord)
        }
    }
    fun convertAppVersion(appVersionRecord: TAppVersionRecord): AppVersion {
        return AppVersion(
                appVersionRecord.id,
                appVersionRecord.versionId,
                appVersionRecord.releaseDate.timestampmilli(),
                appVersionRecord.releaseContent,
                appVersionRecord.channelType
        )
    }
}
