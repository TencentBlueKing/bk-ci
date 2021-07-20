/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.pypi.artifact.repository

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactMigrateContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.repository.migration.MigrateDetail
import com.tencent.bkrepo.common.artifact.repository.migration.PackageMigrateDetail
import com.tencent.bkrepo.common.artifact.repository.migration.VersionMigrateErrorDetail
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.file.multipart.MultipartArtifactFile
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Sort
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.pypi.artifact.model.MigrateDataCreateNode
import com.tencent.bkrepo.pypi.artifact.model.MigrateDataInfo
import com.tencent.bkrepo.pypi.artifact.model.TMigrateData
import com.tencent.bkrepo.pypi.artifact.url.UrlPatternUtil.parameterMaps
import com.tencent.bkrepo.pypi.artifact.xml.Value
import com.tencent.bkrepo.pypi.artifact.xml.XmlUtil
import com.tencent.bkrepo.pypi.exception.PypiMigrateReject
import com.tencent.bkrepo.pypi.pojo.Basic
import com.tencent.bkrepo.pypi.pojo.PypiArtifactVersionData
import com.tencent.bkrepo.pypi.pojo.PypiMigrateResponse
import com.tencent.bkrepo.pypi.util.ArtifactFileUtils
import com.tencent.bkrepo.pypi.util.HttpUtil.downloadUrlHttpClient
import com.tencent.bkrepo.pypi.util.JsoupUtil.htmlHrefs
import com.tencent.bkrepo.pypi.util.JsoupUtil.sumTasks
import com.tencent.bkrepo.pypi.util.PypiVersionUtils.toPypiPackagePojo
import com.tencent.bkrepo.pypi.util.XmlUtils
import com.tencent.bkrepo.pypi.util.XmlUtils.readXml
import com.tencent.bkrepo.pypi.util.pojo.PypiInfo
import com.tencent.bkrepo.repository.api.StageClient
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.nodes.Element
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.net.UnknownServiceException
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Component
class PypiLocalRepository(
    private val mongoTemplate: MongoTemplate,
    private val migrateDataRepository: MigrateDataRepository,
    private val stageClient: StageClient
) : LocalRepository() {

    /**
     * 获取PYPI节点创建请求
     */
    override fun buildNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        val repositoryDetail = context.repositoryDetail
        val artifactFile = context.getArtifactFile("content")
        val metadata = context.request.parameterMaps()
        val filename = (artifactFile as MultipartArtifactFile).getOriginalFilename()
        val sha256 = artifactFile.getFileSha256()
        val md5 = artifactFile.getFileMd5()
        val name: String = context.request.getParameter("name")
        val version: String = context.request.getParameter("version")
        val artifactFullPath = "/$name/$version/$filename"

        return NodeCreateRequest(
            projectId = repositoryDetail.projectId,
            repoName = repositoryDetail.name,
            folder = false,
            overwrite = true,
            fullPath = artifactFullPath,
            size = artifactFile.getSize(),
            sha256 = sha256,
            md5 = md5,
            operator = context.userId,
            metadata = metadata
        )
    }

    override fun onUpload(context: ArtifactUploadContext) {
        val nodeCreateRequest = buildNodeCreateRequest(context)
        val artifactFile = context.getArtifactFile("content")
        val name: String = context.request.getParameter("name")
        val version: String = context.request.getParameter("version")
        packageClient.createVersion(
            PackageVersionCreateRequest(
                projectId = context.projectId,
                repoName = context.repoName,
                packageName = name,
                packageKey = PackageKeys.ofPypi(name),
                packageType = PackageType.PYPI,
                versionName = version,
                size = context.getArtifactFile("content").getSize(),
                artifactPath = nodeCreateRequest.fullPath,
                overwrite = true,
                createdBy = context.userId
            )
        )
        store(nodeCreateRequest, artifactFile, context.storageCredentials)
    }

    /**
     * pypi search
     */
    override fun search(context: ArtifactSearchContext): List<Value> {
        val pypiSearchPojo = XmlUtils.getPypiSearchPojo(context.request.reader.readXml())
        val name = pypiSearchPojo.name
        val summary = pypiSearchPojo.summary
        if (name != null && summary != null) {
            val projectId = Rule.QueryRule("projectId", context.projectId)
            val repoName = Rule.QueryRule("repoName", context.repoName)
            val packageQuery = Rule.QueryRule("metadata.name", "*$name*", OperationType.MATCH)
            val summaryQuery = Rule.QueryRule("metadata.summary", "*$summary*", OperationType.MATCH)
            val filetypeQuery = Rule.QueryRule("metadata.filetype", "bdist_wheel")
            val matchQuery = Rule.NestedRule(
                mutableListOf(packageQuery, summaryQuery),
                Rule.NestedRule.RelationType.OR
            )
            val rule = Rule.NestedRule(
                mutableListOf(repoName, projectId, filetypeQuery, matchQuery),
                Rule.NestedRule.RelationType.AND
            )

            val queryModel = QueryModel(
                page = PageLimit(pageLimitCurrent, pageLimitSize),
                sort = Sort(listOf("name"), Sort.Direction.ASC),
                select = mutableListOf("projectId", "repoName", "fullPath", "metadata"),
                rule = rule
            )
            val nodeList: List<Map<String, Any?>>? = nodeClient.search(queryModel).data?.records
            if (nodeList != null) {
                return XmlUtil.nodeLis2Values(nodeList)
            }
        }
        return mutableListOf()
    }

    /**
     * pypi 产品删除接口
     */
    override fun remove(context: ArtifactRemoveContext) {
        val packageKey = HttpContextHolder.getRequest().getParameter("packageKey")
        val name = PackageKeys.resolvePypi(packageKey)
        val version = HttpContextHolder.getRequest().getParameter("version")
        if (version.isNullOrBlank()) {
            // 删除包
            nodeClient.deleteNode(
                NodeDeleteRequest(
                    context.projectId,
                    context.repoName,
                    "/$name",
                    context.userId
                )
            )
            packageClient.deletePackage(
                context.projectId,
                context.repoName,
                packageKey
            )
        } else {
            // 删除版本
            nodeClient.deleteNode(
                NodeDeleteRequest(
                    context.projectId,
                    context.repoName,
                    "/$name/$version",
                    context.userId
                )
            )
            packageClient.deleteVersion(context.projectId, context.repoName, packageKey, version)
        }
    }

    /**
     * 1，pypi 产品 版本详情
     * 2，pypi simple html页面
     */
    override fun query(context: ArtifactQueryContext): Any? {
        val servletPath = context.request.servletPath
        return if (servletPath.startsWith("/ext/version/detail")) {
            // 请求版本详情
            getVersionDetail(context)
        } else {
            getSimpleHtml(context.artifactInfo)
        }
    }

    fun getVersionDetail(context: ArtifactQueryContext): Any? {
        val packageKey = context.request.getParameter("packageKey")
        val version = context.request.getParameter("version")
        val name = PackageKeys.resolvePypi(packageKey)
        val trueVersion = packageClient.findVersionByName(
            context.projectId,
            context.repoName,
            packageKey,
            version
        ).data ?: return null
        val artifactPath = trueVersion.contentPath ?: return null
        with(context.artifactInfo) {
            val jarNode = nodeClient.getNodeDetail(
                projectId, repoName, artifactPath
            ).data ?: return null
            val stageTag = stageClient.query(projectId, repoName, packageKey, version).data
            val pypiArtifactMetadata = jarNode.metadata
            val packageVersion = packageClient.findVersionByName(
                projectId, repoName, packageKey, version
            ).data
            val count = packageVersion?.downloads ?: 0
            val pypiArtifactBasic = Basic(
                name,
                version,
                jarNode.size, jarNode.fullPath,
                jarNode.createdBy, jarNode.createdDate,
                jarNode.lastModifiedBy, jarNode.lastModifiedDate,
                count,
                jarNode.sha256,
                jarNode.md5,
                stageTag,
                null
            )
            return PypiArtifactVersionData(pypiArtifactBasic, pypiArtifactMetadata)
        }
    }

    /**
     *
     */
    fun getSimpleHtml(artifactInfo: ArtifactInfo): Any? {
        val request = HttpContextHolder.getRequest()
        if (!request.requestURI.endsWith("/")) {
            val response = HttpContextHolder.getResponse()
            response.sendRedirect("${request.requestURL}/")
            response.writer.flush()
        }
        with(artifactInfo) {
            val node = nodeClient.getNodeDetail(projectId, repoName, getArtifactFullPath()).data
                ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, getArtifactFullPath())
            // 请求不带包名，返回包名列表.
            if (getArtifactFullPath() == "/") {
                if (node.folder) {
                    val nodeList = nodeClient.listNode(
                        projectId, repoName, getArtifactFullPath(), includeFolder = true,
                        deep =
                            true
                    ).data
                        ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, getArtifactFullPath())
                    // 过滤掉'根节点',
                    return buildPackageListContent(
                        artifactInfo.projectId,
                        artifactInfo.repoName,
                        nodeList.filter { it.folder }.filter { it.path == "/" }
                    )
                }
            }
            // 请求中带包名，返回对应包的文件列表。
            else {
                if (node.folder) {
                    val packageNode = nodeClient.listNode(
                        projectId, repoName, getArtifactFullPath(), includeFolder = false,
                        deep = true
                    ).data
                        ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, getArtifactFullPath())
                    return buildPypiPageContent(
                        buildPackageFileNodeListContent(packageNode)
                    )
                }
            }
        }
        return null
    }

    /**
     * html 页面公用的元素
     * @param listContent 显示的内容
     */
    private fun buildPypiPageContent(listContent: String): String {
        return """
            <html>
                <head><title>Simple Index</title><meta name="api-version" value="2" /></head>
                <body>
                    $listContent
                </body>
            </html>
        """.trimIndent()
    }

    /**
     * 对应包中的文件列表
     * [nodeList]
     */
    private fun buildPackageFileNodeListContent(nodeList: List<NodeInfo>): String {
        val builder = StringBuilder()
        if (nodeList.isEmpty()) {
            builder.append("The directory is empty.")
        }
        for (node in nodeList) {
            val md5 = node.md5
            // 查询的对应的文件节点的metadata
            val metadata = filenodeMetadata(node)
            builder.append(
                "<a data-requires-python=\">=$metadata[\"requires_python\"]\" href=\"../../packages${
                    node
                        .fullPath
                }#md5=$md5\" rel=\"internal\" >${node.name}</a><br/>"
            )
        }
        return builder.toString()
    }

    /**
     * 所有包列表
     * @param projectId
     * @param repoName
     * @param nodeList
     */
    private fun buildPackageListContent(projectId: String, repoName: String, nodeList: List<NodeInfo>): String {
        val builder = StringBuilder()
        if (nodeList.isEmpty()) {
            builder.append("The directory is empty.")
        }
        for (node in nodeList) {
            builder.append(
                "<a data-requires-python=\">=\" href=\"${node.name}\"" +
                    " rel=\"internal\" >${node.name}</a><br/>"
            )
        }
        return builder.toString()
    }

    /**
     * 根据每个文件节点数据去查metadata
     * @param nodeInfo 节点
     */
    fun filenodeMetadata(nodeInfo: NodeInfo): List<Map<String, Any?>>? {
        val fileNodeList: List<Map<String, Any?>>?
        with(nodeInfo) {
            val projectId = Rule.QueryRule("projectId", projectId)
            val repoName = Rule.QueryRule("repoName", repoName)
            val packageQuery = Rule.QueryRule("metadata.name", name, OperationType.EQ)
            val fullPathQuery = Rule.QueryRule("fullPath", fullPath)
            val rule = Rule.NestedRule(
                mutableListOf(repoName, projectId, packageQuery, fullPathQuery),
                Rule.NestedRule.RelationType.AND
            )
            val queryModel = QueryModel(
                page = PageLimit(pageLimitCurrent, pageLimitSize),
                sort = Sort(listOf("name"), Sort.Direction.ASC),
                select = mutableListOf("projectId", "repoName", "fullPath", "metadata"),
                rule = rule
            )
            fileNodeList = nodeClient.search(queryModel).data?.records
        }
        return fileNodeList
    }

    @org.springframework.beans.factory.annotation.Value("\${migrate.url:''}")
    private val migrateUrl: String = StringPool.EMPTY

    @org.springframework.beans.factory.annotation.Value("\${limitPackages:10}")
    private val limitPackages: Int = DEFAULT_COUNT

    private val failSet = mutableSetOf<String>()

    fun migrateResult(context: ArtifactMigrateContext): PypiMigrateResponse<String> {
        with(context.artifactInfo) {
            val migrateDataInfo = findMigrateResult(projectId, repoName)
            migrateDataInfo?.let {
                return PypiMigrateResponse(
                    it.description,
                    it.filesNum,
                    it.filesNum - it.errorData.size,
                    it.errorData.size,
                    it.elapseTimeSeconds,
                    it.errorData as Set<String>,
                    it.createdDate
                )
            }
            return PypiMigrateResponse("未找到数据迁移记录，如果已经调用迁移接口{migrate/url},请稍后查询")
        }
    }

    fun findMigrateResult(projectId: String, repoName: String): MigrateDataInfo? {
        val criteria = Criteria.where(TMigrateData::projectId.name).`is`(projectId)
            .and(TMigrateData::repoName.name)
            .`is`(repoName)
        val sort = org.springframework.data.domain.Sort.by(
            org.springframework.data.domain.Sort.Direction.DESC,
            TMigrateData::lastModifiedDate.name
        )
        val query = Query.query(criteria).with(sort).limit(0)
        return mongoTemplate.findOne(query, TMigrateData::class.java)?.let { convert(it) }
    }

    fun migrateData(context: ArtifactMigrateContext): PypiMigrateResponse<String> {
        val job = GlobalScope.launch {
            migrate(context)
        }
        job.start()
        return migrateResult(context)
    }

    override fun migrate(context: ArtifactMigrateContext): MigrateDetail {
        val verifiedUrl = beforeMigrate()
        val packageMigrateDetailList = mutableListOf<PackageMigrateDetail>()
        var totalCount: Int
        val cpuCore = cpuCore()
        val threadPool = ThreadPoolExecutor(
            cpuCore, cpuCore.shl(doubleNum), threadAliveTime, TimeUnit.SECONDS,
            LinkedBlockingQueue(),
            ThreadFactoryBuilder().setNameFormat("pypiRepo-migrate-thread-%d").build(),
            PypiMigrateReject()
        )

        // 获取所有的包,开始计时
        val start = Instant.now()
        verifiedUrl.htmlHrefs(limitPackages).let { simpleHrefs ->
            totalCount = migrateUrl.sumTasks(simpleHrefs)
            for (e in simpleHrefs) {
                // 每一个包所包含的文件列表
                migratePackage(e, verifiedUrl, threadPool, context)
            }
        }
        threadPool.shutdown()
        while (!threadPool.awaitTermination(2, TimeUnit.SECONDS)) {
            logger.info("migrate thread pool running!")
        }
        val end = Instant.now()
        val elapseTimeSeconds = Duration.between(start, end)
        insertMigrateData(
            context,
            failSet,
            limitPackages,
            totalCount,
            elapseTimeSeconds.seconds
        )
        return MigrateDetail(
            context.projectId,
            context.repoName,
            packageMigrateDetailList,
            elapseTimeSeconds,
            null
        )
    }

    private fun migratePackage(
        element: Element,
        verifiedUrl: String,
        threadPool: ThreadPoolExecutor,
        context: ArtifactMigrateContext
    ): PackageMigrateDetail? {
        // 每一个包所包含的文件列表
        element.text()?.let { packageName ->
            val packageMigrateDetail = PackageMigrateDetail(
                packageName,
                mutableSetOf(),
                mutableSetOf()
            )
            "$verifiedUrl/$packageName".htmlHrefs().let { fileNodes ->
                for (fileNode in fileNodes) {
                    threadPool.submit {
                        migrateUpload(context, fileNode, verifiedUrl, packageName, packageMigrateDetail)
                    }
                }
            }
            return packageMigrateDetail
        }
        return null
    }

    private fun insertMigrateData(
        context: ArtifactMigrateContext,
        collect: Set<String>,
        packagesName: Int,
        filesNum: Int,
        elapseTimeSeconds: Long
    ) {
        val dataCreateRequest = MigrateDataCreateNode(
            projectId = context.artifactInfo.projectId,
            repoName = context.artifactInfo.repoName,
            errorData = jacksonObjectMapper().writeValueAsString(collect),
            packagesNum = packagesName,
            filesNum = filesNum,
            elapseTimeSeconds = elapseTimeSeconds,
            description = "最近一次任务 {$migrateUrl} 迁移结果如下，请注意检查迁移完成时间"
        )
        create(dataCreateRequest)
    }

    @Transactional(rollbackFor = [Throwable::class])
    fun create(dataCreateRequest: MigrateDataCreateNode) {
        with(dataCreateRequest) {
            val errorData = TMigrateData(
                projectId = projectId,
                repoName = repoName,
                errorData = errorData,
                createdBy = createdBy,
                createdDate = java.time.LocalDateTime.now(),
                lastModifiedBy = createdBy,
                lastModifiedDate = java.time.LocalDateTime.now(),
                packagesNum = packagesNum,
                filesNum = filesNum,
                elapseTimeSeconds = elapseTimeSeconds,
                description = description
            )
            migrateDataRepository.insert(errorData)
                .also { logger.info("Create migration error data [$dataCreateRequest] success.") }
        }
    }

    fun migrateUpload(
        context: ArtifactMigrateContext,
        fileNode: Element,
        verifiedUrl: String,
        packageName: String,
        packageMigrateDetail: PackageMigrateDetail
    ) {
        val filename = fileNode.text()
        val hrefValue = fileNode.attributes()["href"]
        // 获取文件流
        try {
            "$verifiedUrl/$packageName/$hrefValue".downloadUrlHttpClient()?.use { inputStream ->
                val artifactFile = ArtifactFileFactory.build(inputStream)
                val pypiInfo = ArtifactFileUtils.getPypiInfo(filename, artifactFile)
                val isExists = checkExists(context, pypiInfo, packageName, filename)
                if (isExists == null) {
                    packageMigrateDetail.failureVersionDetailList.add(
                        VersionMigrateErrorDetail(
                            pypiInfo.version,
                            "查询该版本在仓库中是否存在失败，不处理"
                        )
                    )
                } else if (isExists == true) {
                    packageMigrateDetail.failureVersionDetailList.add(
                        VersionMigrateErrorDetail(
                            pypiInfo.version,
                            "改版本在仓库中已存在，不处理"
                        )
                    )
                } else {
                    val nodeCreateRequest = createMigrateNode(context, artifactFile, packageName, filename, pypiInfo)
                    store(nodeCreateRequest, artifactFile, context.storageCredentials)
                    packageMigrateDetail.successVersionList.add(pypiInfo.version)
                }
            }
        } catch (unknownServiceException: UnknownServiceException) {
            logger.error(unknownServiceException.message)
            failSet.add("$verifiedUrl/$packageName/${fileNode.attributes()["href"]}")
        }
    }

    /**
     * 读取到的远程节点在本地仓库是否存在
     */
    fun checkExists(
        context: ArtifactMigrateContext,
        pypiInfo: PypiInfo,
        packageName: String,
        filename: String
    ): Boolean? {
        with(context.repositoryDetail) {
            // 文件fullPath
            val path = "/$packageName/${pypiInfo.version}/$filename"
            return nodeClient.checkExist(projectId, name, path).data
        }
    }

    fun createMigrateNode(
        context: ArtifactMigrateContext,
        artifactFile: ArtifactFile,
        packageName: String,
        filename: String,
        pypiInfo: PypiInfo
    ): NodeCreateRequest {
        val metadata = context.request.parameterMaps()
        // 文件fullPath
        val path = "/$packageName/${pypiInfo.version}/$filename"
        return NodeCreateRequest(
            projectId = context.projectId,
            repoName = context.repoName,
            folder = false,
            overwrite = true,
            fullPath = path,
            size = artifactFile.getSize(),
            sha256 = artifactFile.getFileSha256(),
            md5 = artifactFile.getFileMd5(),
            operator = context.userId,
            metadata = metadata
        )
    }

    /**
     * 检验地址格式
     */
    fun beforeMigrate(): String {
        return migrateUrl.removeSuffix("/")
    }

    /**
     * 获取CPU核心数
     */
    fun cpuCore(): Int {
        return Runtime.getRuntime().availableProcessors()
    }

    fun store(node: NodeCreateRequest, artifactFile: ArtifactFile, storageCredentials: StorageCredentials?) {
        storageManager.storeArtifactFile(node, artifactFile, storageCredentials)
        artifactFile.delete()
        with(node) { logger.info("Success to store$projectId/$repoName/$fullPath") }
        logger.info("Success to insert $node")
    }

    // pypi 客户端下载统计
    override fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource
    ): PackageDownloadRecord? {
        with(context) {
            val fullPath = context.artifactInfo.getArtifactFullPath()
            val pypiPackagePojo = fullPath.toPypiPackagePojo()
            val packageKey = PackageKeys.ofPypi(pypiPackagePojo.name)
            return PackageDownloadRecord(
                projectId, repoName,
                packageKey, pypiPackagePojo.version
            )
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PypiLocalRepository::class.java)
        fun convert(tMigrateData: TMigrateData): MigrateDataInfo {
            return tMigrateData.let {
                MigrateDataInfo(
                    errorData = jacksonObjectMapper().readValue(it.errorData, Set::class.java),
                    projectId = it.projectId,
                    repoName = it.repoName,
                    createdBy = it.createdBy,
                    createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    packagesNum = it.packagesNum,
                    filesNum = it.filesNum,
                    elapseTimeSeconds = it.elapseTimeSeconds,
                    description = it.description
                )
            }
        }

        const val pageLimitCurrent = 0
        const val pageLimitSize = 10
        const val threadAliveTime = 15L
        const val doubleNum = 1
        const val DEFAULT_COUNT = 10
    }
}
