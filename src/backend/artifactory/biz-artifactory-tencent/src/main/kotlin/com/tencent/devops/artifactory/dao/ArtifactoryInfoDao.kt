package com.tencent.devops.artifactory.dao

import com.tencent.devops.artifactory.Constants
import com.tencent.devops.artifactory.pojo.ArtifactoryCreateInfo
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.artifactory.Tables.T_TIPELINE_ARTIFACETORY_INFO
import com.tencent.devops.model.artifactory.tables.TTipelineArtifacetoryInfo
import com.tencent.devops.model.artifactory.tables.records.TTipelineArtifacetoryInfoRecord
import org.jooq.DSLContext
import org.jooq.Insert
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Repository
class ArtifactoryInfoDao {

    fun create(
        dslContext: DSLContext,
        fileInfo: FileInfo,
        pipelineId: String,
        buildId: String,
        buildNum: Int,
        projcetId: String,
        dataFrom: Int
    ): Long {

        with(TTipelineArtifacetoryInfo.T_TIPELINE_ARTIFACETORY_INFO) {
            var bundleIdentifier = ""
            var appVersion = ""
            fileInfo.properties!!.forEach {
                if (it.key.equals("bundleIdentifier")) {
                    bundleIdentifier = it.value
                }
                if (it.key.equals("appVersion")) {
                    appVersion = it.value
                }
            }

            val record = dslContext.insertInto(
                this,
                PIPELINE_ID,
                BUILD_ID,
                PROJECT_ID,
                BUNDLE_ID,
                BUILD_NUM,
                NAME,
                FULL_NAME,
                PATH,
                FULL_PATH,
                SIZE,
                MODIFIED_TIME,
                ARTIFACTORY_TYPE,
                PROPERTIES,
                APP_VERSION,
                DATA_FROM
            ).values(
                pipelineId,
                buildId,
                projcetId,
                bundleIdentifier,
                buildNum,
                fileInfo.name,
                fileInfo.fullName,
                fileInfo.path,
                fileInfo.fullPath,
                fileInfo.size.toInt(),
                LocalDateTime.ofInstant(Instant.ofEpochSecond(fileInfo.modifiedTime), ZoneId.systemDefault()),
                fileInfo.artifactoryType.toString(),
                JsonUtil.toJson(fileInfo.properties ?: emptyList<Property>()),
                appVersion,
                dataFrom.toByte()
            ).returning(ID)
                .fetchOne()
            return record.id
        }
    }

    fun searchAritfactoryInfo(
        dslContext: DSLContext,
        pipelineId: String,
        startTime: Long,
        endTime: Long
    ): Result<TTipelineArtifacetoryInfoRecord>? {
        return with(TTipelineArtifacetoryInfo.T_TIPELINE_ARTIFACETORY_INFO) {
            val where = dslContext.selectFrom(this).where(
                PIPELINE_ID.eq(pipelineId)
            )
            if (startTime > 0) {
                where.and(
                    MODIFIED_TIME.ge(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(startTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }

            if (endTime > 0) {
                where.and(
                    MODIFIED_TIME.le(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(endTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
            where.orderBy(ID.asc())
                .fetch()
        }
    }

    fun getLastCompensateData(
        dslContext: DSLContext
    ): Result<TTipelineArtifacetoryInfoRecord> {
        return with(TTipelineArtifacetoryInfo.T_TIPELINE_ARTIFACETORY_INFO) {
            val where = dslContext.selectFrom(this).where(
                DATA_FROM.eq(Constants.SYN_DATA_FROM_COMPENSATE.toByte())
            )
            where.orderBy(MODIFIED_TIME.desc()).limit(0, 1)

            where.fetch()
        }
    }

    fun batchCreate(infoList: List<ArtifactoryCreateInfo>, dslContext: DSLContext): Int {
        val sets =
            mutableListOf<Insert<TTipelineArtifacetoryInfoRecord>>()
        with(T_TIPELINE_ARTIFACETORY_INFO) {
            infoList.forEach {
                var bundleIdentifier = ""
                var appVersion = ""
                it.fileInfo?.properties!!.forEach {
                    if (it.key.equals("bundleIdentifier")) {
                        bundleIdentifier = it.value
                    }
                    if (it.key.equals("appVersion")) {
                        appVersion = it.value
                    }
                }
                val set =
                    dslContext.insertInto(
                        this,
                        PIPELINE_ID,
                        BUILD_ID,
                        PROJECT_ID,
                        BUNDLE_ID,
                        BUILD_NUM,
                        NAME,
                        FULL_NAME,
                        PATH,
                        FULL_PATH,
                        SIZE,
                        MODIFIED_TIME,
                        ARTIFACTORY_TYPE,
                        PROPERTIES,
                        APP_VERSION,
                        DATA_FROM
                    ).values(
                        it.pipelineId,
                        it.buildId,
                        it.projectId,
                        bundleIdentifier,
                        it.buildNum,
                        it.fileInfo?.name,
                        it.fileInfo?.fullName,
                        it.fileInfo?.path,
                        it.fileInfo?.fullPath,
                        it.fileInfo?.size!!.toInt(),
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(it.fileInfo!!.modifiedTime),
                            ZoneId.systemDefault()
                        ),
                        it.fileInfo?.artifactoryType.toString(),
                        JsonUtil.toJson(it.fileInfo?.properties ?: emptyList<Property>()),
                        appVersion,
                        it.dataForm.toByte()
                    )
                sets.add(set)
            }
        }
        if (sets.isNotEmpty()) {
            val count = dslContext.batch(sets).execute()
            var success = 0
            count.forEach {
                if (it == 1) {
                    success++
                }
            }
            return success
        }

        return 0
    }

    fun selectCountByDataFrom(dslContext: DSLContext, dataForm: Int, startTime: Long, endTime: Long): Int {
        return with(T_TIPELINE_ARTIFACETORY_INFO) {
            val where = dslContext.selectDistinct(BUILD_ID).from(this).where(DATA_FROM.eq(dataForm.toByte()))

            if (startTime > 0) {
                where.and(
                    MODIFIED_TIME.ge(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(startTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }

            if (endTime > 0) {
                where.and(
                    MODIFIED_TIME.le(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(endTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }

            where.count()
        }
    }
}
