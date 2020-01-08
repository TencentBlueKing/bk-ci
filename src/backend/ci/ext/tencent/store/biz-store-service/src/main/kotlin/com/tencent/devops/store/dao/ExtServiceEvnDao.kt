package com.tencent.devops.store.dao

import com.tencent.devops.model.store.tables.TExtensionServiceEnvInfo
import com.tencent.devops.store.pojo.ExtServiceEnvCreateInfo
import com.tencent.devops.store.pojo.ExtServiceEnvUpdateInfo
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExtServiceEvnDao {
    fun create(
        dslContext: DSLContext,
        userId: String,
        id: String,
        extServiceEnvCreateInfo: ExtServiceEnvCreateInfo
    ) {
        with(TExtensionServiceEnvInfo.T_EXTENSION_SERVICE_ENV_INFO) {
            dslContext.insertInto(
                this,
                ID,
                SERVICE_ID,
                LANGUAGE,
                PKG_PATH,
                PKG_SHA_CONTENT,
                DOCKER_FILE_CONTENT,
                IMAGE_PATH,
                FRONTEND_ENTRY_FILE,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    id,
                    extServiceEnvCreateInfo.serviceId,
                    extServiceEnvCreateInfo.language,
                    extServiceEnvCreateInfo.pkgPath,
                    extServiceEnvCreateInfo.pkgShaContent,
                    extServiceEnvCreateInfo.dockerFileContent,
                    extServiceEnvCreateInfo.imagePath,
                    extServiceEnvCreateInfo.frontentEntryFile,
                    extServiceEnvCreateInfo.creatorUser,
                    extServiceEnvCreateInfo.modifierUser,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .execute()
        }
    }

    fun updateExtServiceFeatureBaseInfo(
        dslContext: DSLContext,
        userId: String,
        serviceId: String,
        extServiceEnvUpdateInfo: ExtServiceEnvUpdateInfo
    ) {
        with(TExtensionServiceEnvInfo.T_EXTENSION_SERVICE_ENV_INFO) {
            val baseStep = dslContext.update(this)
            val language = extServiceEnvUpdateInfo.language
            if (null != language) {
                baseStep.set(LANGUAGE, language)
            }
            val pkgPath = extServiceEnvUpdateInfo.pkgPath
            if (null != pkgPath) {
                baseStep.set(PKG_PATH, pkgPath)
            }
            val pkgShaContent = extServiceEnvUpdateInfo.pkgShaContent
            if (null != pkgShaContent) {
                baseStep.set(PKG_SHA_CONTENT, pkgShaContent)
            }
            val dockerFileContent = extServiceEnvUpdateInfo.dockerFileContent
            if (null != dockerFileContent) {
                baseStep.set(DOCKER_FILE_CONTENT, dockerFileContent)
            }
            val imagePath = extServiceEnvUpdateInfo.imagePath
            if (null != imagePath) {
                baseStep.set(IMAGE_PATH, imagePath)
            }
            val frontentEntryFile = extServiceEnvUpdateInfo.frontentEntryFile
            if (null != frontentEntryFile) {
                baseStep.set(FRONTEND_ENTRY_FILE, frontentEntryFile)
            }
            baseStep.set(MODIFIER, userId).set(UPDATE_TIME, LocalDateTime.now())
                .where(SERVICE_ID.eq(serviceId))
                .execute()
        }
    }
}