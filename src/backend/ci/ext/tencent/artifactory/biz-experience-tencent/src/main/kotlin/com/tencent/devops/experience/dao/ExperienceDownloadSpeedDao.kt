package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceDownloadSpeed
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
@Suppress("LongParameterList")
class ExperienceDownloadSpeedDao {
    fun create(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        artifactoryType: String,
        path: String,
        experienceId: Long?,
        downloadSpeed: Long,
        downloadType: String,
        createTime: LocalDateTime,
        updateTime: LocalDateTime
    ) {
        with(TExperienceDownloadSpeed.T_EXPERIENCE_DOWNLOAD_SPEED){
            dslContext.insertInto(this)
                .set(USER_ID, userId)
                .set(PROJECT_ID, projectId)
                .set(ARTIFACTORY_TYPE, artifactoryType)
                .set(ARTIFACTORY_PATH, path)
                .set(RECORD_ID, experienceId)
                .set(DOWNLOAD_SPEED, downloadSpeed)
                .set(DOWNLOAD_TYPE, downloadType)
                .set(CREATE_TIME, createTime)
                .set(UPDATE_TIME, updateTime)
                .execute()
        }
    }

}