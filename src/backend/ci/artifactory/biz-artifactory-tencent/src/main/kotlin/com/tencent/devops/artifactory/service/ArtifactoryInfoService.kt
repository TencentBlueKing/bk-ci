package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.dao.ArtifactoryInfoDao
import com.tencent.devops.artifactory.pojo.ArtifactoryCreateInfo
import com.tencent.devops.artifactory.pojo.ArtifactoryInfo
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.TrendInfoDto
import com.tencent.devops.common.client.Client
import com.tencent.devops.model.artifactory.tables.records.TTipelineArtifacetoryInfoRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneOffset

@Service
class ArtifactoryInfoService @Autowired constructor(
    private val artifactoryInfoDao: ArtifactoryInfoDao,
    private val dslContext: DSLContext,
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactoryInfoService::class.java)
    }

    fun createInfo(
        buildId: String,
        pipelineId: String,
        projectId: String,
        buildNum: Int,
        fileInfo: FileInfo,
        dataFrom: Int
    ): Long {
//        if (artifactoryInfoDao.getByBuildId(dslContext, buildId, fileInfo.fullName) != null) {
//            logger.warn("[$buildId]|artifactory create more once")
//            throw ParamBlankException("该构建ID产物已存在")
//        }
        return artifactoryInfoDao.create(dslContext, fileInfo, pipelineId, buildId, buildNum, projectId, dataFrom)
    }

    fun queryArtifactoryInfo(
        pipelineId: String,
        startTime: Long,
        endTime: Long
    ): TrendInfoDto {
        var trendInfoDto = TrendInfoDto(
            trendData = mutableMapOf()
        )
        val infoList = artifactoryInfoDao.searchAritfactoryInfo(dslContext, pipelineId, startTime, endTime)
        // 根据BUNDLE_ID做聚合,BUNDLE_ID需提供给前端绘制曲线
        var infoMap: MutableMap<String, MutableList<ArtifactoryInfo>> = mutableMapOf()
        infoList?.forEach {
            run resultData@{
                if (it.bundleId.isNullOrBlank()) {
                    return@resultData
                }
                val artifactoryInfo = buildArtifactoryInfo(it)
                var artifactoryInfoList = mutableListOf<ArtifactoryInfo>()
                if (infoMap.get(artifactoryInfo.bundleId) != null) {
                    artifactoryInfoList = infoMap.get(artifactoryInfo.bundleId)!!
                    artifactoryInfoList.add(artifactoryInfo)
                    infoMap.put(artifactoryInfo.bundleId, artifactoryInfoList)
                } else {
                    artifactoryInfoList.add(artifactoryInfo)
                    infoMap.put(artifactoryInfo.bundleId, artifactoryInfoList)
                }
            }
        }
        trendInfoDto.trendData = infoMap

        return trendInfoDto
    }

    fun getListCompensateInfo(): ArtifactoryInfo? {
        val artifactoryInfo = artifactoryInfoDao.getLastCompensateData(dslContext)
        if (artifactoryInfo == null || artifactoryInfo.size == 0) {
            return null
        } else {
            return buildArtifactoryInfo(artifactoryInfo.get(0))
        }
    }

    fun batchCreateArtifactoryInfo(infoList: List<ArtifactoryCreateInfo>): Int {
        var count = 0

        count = artifactoryInfoDao.batchCreate(infoList, dslContext)

        if (count == 0) {
            logger.error("批量添加artifactoryInfo失败")
        }

        return count
    }

    // 获取时间范围内构建条数--buildId去重
    fun getInfoCount(startTime: Long, endTime: Long, dataForm: Int): Int {
        return artifactoryInfoDao.selectCountByDataFrom(dslContext, dataForm, startTime, endTime)
    }

    private fun buildArtifactoryInfo(record: TTipelineArtifacetoryInfoRecord): ArtifactoryInfo {
        val artifactoryInfo = with(record) {
            ArtifactoryInfo(
                pipelineId = record.pipelineId,
                projectId = record.projectId,
                buildNum = record.buildNum,
                bundleId = record.bundleId,
                name = record.name,
                fullName = record.fullName,
                size = record.size.toLong(),
                modifiedTime = record.modifiedTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli() / 1000,
                appVersion = record.appVersion,
                dataForm = record.dataFrom.toInt(),
                fileInfo = null
            )
        }
        return artifactoryInfo
    }
}