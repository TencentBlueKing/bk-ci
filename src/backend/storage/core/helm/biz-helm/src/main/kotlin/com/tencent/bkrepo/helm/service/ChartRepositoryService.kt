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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.helm.service

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.util.JsonUtils.objectMapper
import com.tencent.bkrepo.common.artifact.constant.OCTET_STREAM
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.context.RepositoryHolder
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Sort
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.helm.artifact.HelmArtifactInfo
import com.tencent.bkrepo.helm.constants.CHART_PACKAGE_FILE_EXTENSION
import com.tencent.bkrepo.helm.constants.CREATED
import com.tencent.bkrepo.helm.constants.DATA_TIME_FORMATTER
import com.tencent.bkrepo.helm.constants.DIGEST
import com.tencent.bkrepo.helm.constants.FULL_PATH
import com.tencent.bkrepo.helm.constants.INDEX_CACHE_YAML
import com.tencent.bkrepo.helm.constants.NAME
import com.tencent.bkrepo.helm.constants.NODE_CREATE_DATE
import com.tencent.bkrepo.helm.constants.NODE_FULL_PATH
import com.tencent.bkrepo.helm.constants.NODE_METADATA
import com.tencent.bkrepo.helm.constants.NODE_NAME
import com.tencent.bkrepo.helm.constants.NODE_SHA256
import com.tencent.bkrepo.helm.constants.PROJECT_ID
import com.tencent.bkrepo.helm.constants.REPO_NAME
import com.tencent.bkrepo.helm.constants.TGZ_SUFFIX
import com.tencent.bkrepo.helm.constants.URLS
import com.tencent.bkrepo.helm.constants.V1
import com.tencent.bkrepo.helm.constants.VERSION
import com.tencent.bkrepo.helm.exception.HelmFileNotFoundException
import com.tencent.bkrepo.helm.lock.MongoLock
import com.tencent.bkrepo.helm.pojo.IndexEntity
import com.tencent.bkrepo.helm.utils.DecompressUtil.getArchivesContent
import com.tencent.bkrepo.helm.utils.HelmZipResponseWriter
import com.tencent.bkrepo.helm.utils.YamlUtils
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.util.NodeUtils
import com.tencent.bkrepo.repository.util.NodeUtils.FILE_SEPARATOR
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class ChartRepositoryService {

    @Value("\${helm.registry.domain: ''}")
    private lateinit var domain: String

    @Autowired
    private lateinit var nodeClient: NodeClient

    @Autowired
    private lateinit var mongoLock: MongoLock

    @Permission(ResourceType.REPO, PermissionAction.READ)
    @Transactional(rollbackFor = [Throwable::class])
    fun getIndexYaml(artifactInfo: HelmArtifactInfo) {
        // val lockKey = "${artifactInfo.projectId}_${artifactInfo.repoName}"
        // try {
        //     if (mongoLock.tryLock(lockKey, LOCK_VALUE)) {
        //         freshIndexFile(artifactInfo)
        //     }
        // } finally {
        //     mongoLock.releaseLock(lockKey, LOCK_VALUE)
        // }
        freshIndexFile(artifactInfo)
        downloadIndexYaml()
    }

    fun freshIndexFile(artifactInfo: HelmArtifactInfo) {
        // 先查询index.yaml文件，如果不存在则创建，
        // 存在则根据最后一次更新时间与node节点创建时间对比进行增量更新
        val exist = nodeClient.exist(artifactInfo.projectId, artifactInfo.repoName, INDEX_CACHE_YAML).data!!
        if (!exist) {
            val indexEntity = initIndexEntity()
            val nodeList = queryNodeList(artifactInfo, false)
            logger.info("query node list success, size [${nodeList.size}]")
            if (nodeList.isNotEmpty()) {
                logger.info("start generate index.yaml ... ")
                generateIndexFile(nodeList, indexEntity, artifactInfo)
            }
            uploadIndexYaml(indexEntity).also { logger.info("generate index.yaml success！") }
            return
        }

        val indexEntity = getOriginalIndexYaml()
        val dateTime = indexEntity.generated.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) }
        val now = LocalDateTime.now()
        val nodeList = queryNodeList(artifactInfo, lastModifyTime = dateTime)
        if (nodeList.isNotEmpty()) {
            logger.info(
                "start regenerate index.yaml, original index.yaml entries size : [${indexEntity.entriesSize()}]"
            )
            generateIndexFile(nodeList, indexEntity, artifactInfo)
            indexEntity.generated = now.format(DateTimeFormatter.ofPattern(DATA_TIME_FORMATTER))
            uploadIndexYaml(indexEntity).also {
                logger.info(
                    "regenerate index.yaml success, current index.yaml entries size : [${indexEntity.entriesSize()}]"
                )
            }
        }
    }

    fun queryNodeList(
        artifactInfo: HelmArtifactInfo,
        exist: Boolean = true,
        lastModifyTime: LocalDateTime? = null
    ): List<Map<String, Any>> {
        val projectRule = Rule.QueryRule(PROJECT_ID, artifactInfo.projectId)
        val repoNameRule = Rule.QueryRule(REPO_NAME, artifactInfo.repoName)
        val fullPathRule = Rule.QueryRule(NODE_FULL_PATH, TGZ_SUFFIX, OperationType.SUFFIX)
        var createDateRule: Rule.QueryRule? = null
        if (exist) {
            createDateRule = lastModifyTime?.let { Rule.QueryRule(NODE_CREATE_DATE, it, OperationType.AFTER) }
        }
        val queryRuleList = mutableListOf(projectRule, repoNameRule, fullPathRule)
        createDateRule?.let { queryRuleList.add(it) }
        val rule = Rule.NestedRule(queryRuleList.toMutableList())
        val queryModel = QueryModel(
            page = PageLimit(page, size),
            sort = Sort(listOf(NODE_FULL_PATH), Sort.Direction.ASC),
            select = mutableListOf(
                PROJECT_ID,
                REPO_NAME,
                NODE_NAME,
                NODE_FULL_PATH,
                NODE_METADATA,
                NODE_SHA256,
                NODE_CREATE_DATE
            ),
            rule = rule
        )
        val result = nodeClient.query(queryModel).data ?: run {
            logger.warn("don't find node list in repository: [${artifactInfo.projectId}, ${artifactInfo.repoName}]!")
            return emptyList()
        }
        return result.records
    }

    @Suppress("UNCHECKED_CAST")
    fun generateIndexFile(
        result: List<Map<String, Any>>,
        indexEntity: IndexEntity,
        artifactInfo: HelmArtifactInfo
    ) {
        val context = ArtifactSearchContext()
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        result.forEach { it ->
            Thread.sleep(SLEEP_MILLIS)
            context.contextAttributes[FULL_PATH] = it[NODE_FULL_PATH] as String
            var chartName: String? = null
            var chartVersion: String? = null
            try {
                val artifactInputStream = repository.search(context) as ArtifactInputStream
                val content = artifactInputStream.use { it.getArchivesContent(CHART_PACKAGE_FILE_EXTENSION) }
                val chartInfoMap = YamlUtils.convertStringToEntity<MutableMap<String, Any>>(content)
                chartName = chartInfoMap[NAME] as String
                chartVersion = chartInfoMap[VERSION] as String
                chartInfoMap[URLS] = listOf(
                    domain.trimEnd('/') + NodeUtils.formatFullPath(
                        "${artifactInfo.projectId}/${artifactInfo.repoName}/charts/$chartName-$chartVersion.tgz"
                    )
                )
                chartInfoMap[CREATED] = convertDateTime(it[NODE_CREATE_DATE] as String)
                chartInfoMap[DIGEST] = it[NODE_SHA256] as String
                addIndexEntries(indexEntity, chartInfoMap)
            } catch (ex: HelmFileNotFoundException) {
                logger.error(
                    "generate indexFile for chart [$chartName-$chartVersion.tgz] in " +
                        "[${artifactInfo.projectId}/${artifactInfo.repoName}] failed, ${ex.message}"
                )
            }
        }
    }

    private fun uploadIndexYaml(indexEntity: IndexEntity) {
        val artifactFile = ArtifactFileFactory.build(YamlUtils.transEntityToStream(indexEntity))
        val context = ArtifactUploadContext(artifactFile)
        context.contextAttributes[OCTET_STREAM + FULL_PATH] = "$FILE_SEPARATOR$INDEX_CACHE_YAML"
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        repository.upload(context)
    }

    fun initIndexEntity(): IndexEntity {
        return IndexEntity(
            apiVersion = V1,
            generated = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATA_TIME_FORMATTER))
        )
    }

    fun addIndexEntries(
        indexEntity: IndexEntity,
        chartInfoMap: MutableMap<String, Any>
    ) {
        val chartName = chartInfoMap[NAME] as String
        val chartVersion = chartInfoMap[VERSION] as String
        val isFirstChart = !indexEntity.entries.containsKey(chartName)
        indexEntity.entries.let {
            if (isFirstChart) {
                it[chartName] = mutableListOf(chartInfoMap)
            } else {
                // force upload
                run stop@{
                    it[chartName]?.forEachIndexed { index, chartMap ->
                        if (chartVersion == chartMap[VERSION] as String) {
                            it[chartName]?.removeAt(index)
                            return@stop
                        }
                    }
                }
                it[chartName]?.add(chartInfoMap)
            }
        }
    }

    fun getOriginalIndexYaml(): IndexEntity {
        val context = ArtifactSearchContext()
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        context.contextAttributes[FULL_PATH] = "$FILE_SEPARATOR$INDEX_CACHE_YAML"
        val indexMap = (repository.search(context) as ArtifactInputStream).run {
            YamlUtils.convertFileToEntity<Map<String, Any>>(this)
        }
        return objectMapper.convertValue(indexMap, IndexEntity::class.java).also {
            logger.info(
                with(context.artifactInfo) {
                    "search original $INDEX_CACHE_YAML success in [$projectId/$repoName], " +
                        "entries size [${it.entries.size}]."
                }
            )
        }
    }

    fun downloadIndexYaml() {
        val context = ArtifactDownloadContext()
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        context.contextAttributes[FULL_PATH] = "$FILE_SEPARATOR$INDEX_CACHE_YAML"
        repository.download(context)
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    @Transactional(rollbackFor = [Throwable::class])
    fun regenerateIndexYaml(artifactInfo: HelmArtifactInfo) {
        val indexEntity = initIndexEntity()
        val nodeList = queryNodeList(artifactInfo, false)
        logger.info("query node list for full refresh index.yaml success, size [${nodeList.size}]")
        if (nodeList.isNotEmpty()) {
            logger.info("start full refresh index.yaml ... ")
            generateIndexFile(nodeList, indexEntity, artifactInfo)
        }
        uploadIndexYaml(indexEntity).also { logger.info("Full refresh index.yaml success！") }
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    @Transactional(rollbackFor = [Throwable::class])
    fun installTgz(artifactInfo: HelmArtifactInfo) {
        val context = ArtifactDownloadContext()
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        context.contextAttributes[FULL_PATH] = artifactInfo.artifactUri
        repository.download(context)
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    @Transactional(rollbackFor = [Throwable::class])
    fun batchInstallTgz(artifactInfo: HelmArtifactInfo, startTime: LocalDateTime) {
        val artifactResourceList = mutableListOf<ArtifactResource>()
        val nodeList = queryNodeList(artifactInfo, lastModifyTime = startTime)
        if (nodeList.isEmpty()) {
            throw HelmFileNotFoundException(
                "no chart found in repository [${artifactInfo.projectId}/${artifactInfo.repoName}]"
            )
        }
        val context = ArtifactSearchContext()
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        nodeList.forEach {
            context.contextAttributes[FULL_PATH] = it[NODE_FULL_PATH] as String
            val artifactInputStream = repository.search(context) as ArtifactInputStream
            artifactResourceList.add(ArtifactResource(artifactInputStream, it[NODE_NAME] as String, null))
        }
        HelmZipResponseWriter.write(artifactResourceList)
    }

    companion object {
        const val page = 0
        const val size = 100000
        const val SLEEP_MILLIS = 20L

        val logger: Logger = LoggerFactory.getLogger(ChartRepositoryService::class.java)
        val LOCK_VALUE = UUID.randomUUID().toString()

        fun convertDateTime(timeStr: String): String {
            val localDateTime = LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_DATE_TIME)
            return localDateTime.format(DateTimeFormatter.ofPattern(DATA_TIME_FORMATTER))
        }
    }
}
