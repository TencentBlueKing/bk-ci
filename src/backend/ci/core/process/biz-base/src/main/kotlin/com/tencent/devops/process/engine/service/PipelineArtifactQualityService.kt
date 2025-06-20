package com.tencent.devops.process.engine.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.artifactory.api.service.ServiceArtifactQualityMetadataResource
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.common.archive.pojo.ArtifactQualityMetadataAnalytics
import com.tencent.bkrepo.repository.pojo.metadata.label.MetadataLabelDetail
import com.tencent.devops.common.client.Client
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class PipelineArtifactQualityService(
    private val client: Client
) {
    private val metadataLabelsCache = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .maximumSize(1000)
        .recordStats()
        .build<Pair<String, String>, List<MetadataLabelDetail>>()

    fun getArtifactQualityList(
        userId: String,
        projectId: String,
        artifactoryFileList: List<FileInfo>
    ): List<ArtifactQualityMetadataAnalytics> {
        val metadataLabels = getMetadataLabels(userId, projectId)

        if (metadataLabels.isEmpty() || artifactoryFileList.isEmpty())
            return emptyList()

        // 1. 创建标签键到元数据的快速映射
        val labelMap = metadataLabels.associateBy { it.labelKey }

        // 2. 创建分析结果容器：Map<Pair<标签键, 值>, 计数器>
        val analyticsMap = mutableMapOf<Pair<String, String>, Int>()

        // 3. 遍历所有制品属性
        artifactoryFileList.mapNotNull { it.properties }.forEach { properties ->
            properties.forEach { (key, value) ->
                // 4. 只处理在元数据标签中定义的键
                labelMap[key]?.let {
                    val pair = key to value
                    analyticsMap[pair] = analyticsMap.getOrDefault(pair, 0) + 1
                }
            }
        }

        // 5. 转换为最终结果结构
        return analyticsMap.map { (pair, count) ->
            val (labelKey, value) = pair
            val labelDetail = labelMap[labelKey]!!
            logger.debug("artifact quality result:{}|{}|{}|{}", labelKey, value, labelDetail, count)
            ArtifactQualityMetadataAnalytics(
                labelKey = labelKey,
                value = value,
                color = labelDetail.labelColorMap?.get(value) ?: "#C4C6CC",
                count = count
            )
        }
    }

    fun buildArtifactQuality(
        userId: String?,
        projectId: String,
        artifactQualityList: List<ArtifactQualityMetadataAnalytics>? = null
    ): Map<String, List<ArtifactQualityMetadataAnalytics>> {
        if (userId == null || artifactQualityList.isNullOrEmpty()) return emptyMap()

        val metadataLabels = getMetadataLabels(userId, projectId)
        // 构建有效标签的快速查询映射：labelKey -> MetadataLabelDetail
        val validMetadataMap = metadataLabels
            .filter { it.display }
            .associateBy { it.labelKey }

        // 双层过滤：标签有效性 + 枚举值有效性
        return artifactQualityList
            .filter { item ->
                validMetadataMap[item.labelKey]?.let { labelDetail ->
                    if (labelDetail.enumType) {
                        // 枚举类型：检查 value 是否存在于 labelColorMap 中
                        labelDetail.labelColorMap.containsKey(item.value)
                    } else {
                        // 非枚举类型：标签有效即保留
                        true
                    }
                } ?: false // 标签无效则直接过滤
            }
            .groupBy { it.labelKey } // 按标签分组返回
    }

    private fun getMetadataLabels(
        userId: String,
        projectId: String
    ): List<MetadataLabelDetail> {
        return metadataLabelsCache.get(userId to projectId) { key ->
            try {
                client.get(ServiceArtifactQualityMetadataResource::class).list(key.first, key.second).data.orEmpty()
            } catch (ex: Exception) {
                logger.warn("Fetch metadata labels failed ${key.first}|${key.second}|${ex.message}")
                emptyList()
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineArtifactQualityService::class.java)
    }
}
