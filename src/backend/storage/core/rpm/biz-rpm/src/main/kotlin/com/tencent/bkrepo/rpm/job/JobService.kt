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

package com.tencent.bkrepo.rpm.job

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.HumanReadable
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.hash.sha1
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Sort
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import com.tencent.bkrepo.rpm.REPODATA
import com.tencent.bkrepo.rpm.exception.RpmConfNotFoundException
import com.tencent.bkrepo.rpm.pojo.ArtifactRepeat
import com.tencent.bkrepo.rpm.pojo.IndexType
import com.tencent.bkrepo.rpm.pojo.RpmRepoConf
import com.tencent.bkrepo.rpm.pojo.RpmVersion
import com.tencent.bkrepo.rpm.util.GZipUtils.gZip
import com.tencent.bkrepo.rpm.util.GZipUtils.unGzipInputStream
import com.tencent.bkrepo.rpm.util.RpmConfiguration.toRpmRepoConf
import com.tencent.bkrepo.rpm.util.RpmVersionUtils.toRpmVersion
import com.tencent.bkrepo.rpm.util.XmlStrUtils
import com.tencent.bkrepo.rpm.util.xStream.XStreamUtil
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmChecksum
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmLocation
import com.tencent.bkrepo.rpm.util.xStream.repomd.RepoData
import com.tencent.bkrepo.rpm.util.xStream.repomd.RepoGroup
import com.tencent.bkrepo.rpm.util.xStream.repomd.RepoIndex
import com.tencent.bkrepo.rpm.util.xStream.repomd.Repomd
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import javax.xml.parsers.SAXParserFactory

@Component
class JobService(
    private val nodeClient: NodeClient,
    private val repositoryClient: RepositoryClient,
    private val storageService: StorageService,
    private val storageManager: StorageManager
) {

    /**
     * 查询下所有rpm仓库
     */
    fun getAllRpmRepo(): List<RepositoryDetail>? {
        val repoPage = repositoryClient.pageByType(0, 10, "RPM").data
        val total = (repoPage?.totalRecords ?: 10).toInt()
        return repositoryClient.pageByType(0, total + 1, "RPM").data?.records
    }

    /**
     * 查询仓库下所有repodata目录
     */
    fun findRepodataDirs(repo: RepositoryDetail): List<String> {
        val ruleList = mutableListOf<Rule>(
            Rule.QueryRule("projectId", repo.projectId, OperationType.EQ),
            Rule.QueryRule("repoName", repo.name, OperationType.EQ),
            Rule.QueryRule("folder", true, OperationType.EQ),
            Rule.QueryRule("name", "repodata", OperationType.EQ)
        )
        val queryModel = QueryModel(
            page = PageLimit(1, MAX_REPO_PAGE_SIE),
            sort = Sort(listOf("lastModifiedDate"), Sort.Direction.DESC),
            select = mutableListOf("fullPath"),
            rule = Rule.NestedRule(ruleList, Rule.NestedRule.RelationType.AND)
        )
        if (logger.isDebugEnabled) {
            logger.debug("queryRepodata: $queryModel")
        }
        val page = nodeClient.search(queryModel).data!!
        return page.records.map { it["fullPath"] as String }
    }

    /**
     * 查询rpm仓库属性
     */
    private fun getRpmRepoConf(project: String, repoName: String): RpmRepoConf {
        val repositoryInfo = repositoryClient.getRepoInfo(project, repoName).data
            ?: throw RpmConfNotFoundException("can not found $project | $repoName conf")
        val rpmConfiguration = repositoryInfo.configuration
        return rpmConfiguration.toRpmRepoConf()
    }

    /**
     * 合并索引和group 字段
     */
    fun getTargetIndexList(groupXmlList: MutableList<String>, enabledFileLists: Boolean): MutableList<String> {
        val doubleSet = mutableSetOf<String>()
        for (str in groupXmlList) {
            doubleSet.add(str)
            doubleSet.add("$str.gz")
        }
        val groupAndIndex = mutableListOf<String>()
        if (enabledFileLists) groupAndIndex.add("${IndexType.FILELISTS.value}.xml.gz")
        groupAndIndex.add("${IndexType.PRIMARY.value}.xml.gz")
        groupAndIndex.add("${IndexType.OTHERS.value}.xml.gz")
        groupAndIndex.addAll(doubleSet)
        return groupAndIndex
    }

    fun findIndexXml(repo: RepositoryDetail, repoDataPath: String): List<NodeInfo> {
        val rpmRepoConf = getRpmRepoConf(repo.projectId, repo.name)
        val enabledFileLists = rpmRepoConf.enabledFileLists
        val groupXmlSet = rpmRepoConf.groupXmlSet
        val groupAndIndex = getTargetIndexList(groupXmlSet, enabledFileLists)
        val targetIndexList = mutableListOf<NodeInfo>()

        for (index in groupAndIndex) {
            getLatestIndexNode(repo, repoDataPath, index)?.let { targetIndexList.add(it) }
        }
        return targetIndexList
    }

    fun flushRepoMdXML(repo: RepositoryDetail, repoDataPath: String) {
        val targetIndexList = findIndexXml(repo, repoDataPath)
        val repoDataList = mutableListOf<RepoIndex>()
        val regex = Regex("-filelists\\.xml\\.gz|-others\\.xml\\.gz|-primary\\.xml\\.gz")
        for (index in targetIndexList) {
            repoDataList.add(
                if ((index.name).contains(regex)) {
                    RepoData(
                        type = index.metadata?.get("indexType") as String,
                        location = RpmLocation("$REPODATA${StringPool.SLASH}${index.name}"),
                        checksum = RpmChecksum(index.metadata?.get("checksum") as String),
                        size = (index.metadata?.get("size") as String).toLong(),
                        timestamp = index.metadata?.get("timestamp") as String,
                        openChecksum = RpmChecksum(index.metadata?.get("openChecksum") as String),
                        openSize = (index.metadata?.get("openSize") as String).toLong()
                    )
                } else {
                    RepoGroup(
                        type = index.metadata?.get("indexType") as String,
                        location = RpmLocation("$REPODATA${StringPool.SLASH}${index.name}"),
                        checksum = RpmChecksum(index.metadata?.get("checksum") as String),
                        size = (index.metadata?.get("size") as String).toLong(),
                        timestamp = index.metadata?.get("timestamp") as String
                    )
                }
            )
        }

        val repomd = Repomd(repoDataList)
        ByteArrayInputStream((repomd.toXml().toByteArray())).use { xmlRepodataInputStream ->
            val xmlRepodataArtifact = ArtifactFileFactory.build(xmlRepodataInputStream)
            // 保存repodata 节点
            val xmlRepomdNode = NodeCreateRequest(
                repo.projectId,
                repo.name,
                "$repoDataPath/repomd.xml",
                false,
                0L,
                true,
                xmlRepodataArtifact.getSize(),
                xmlRepodataArtifact.getFileSha256(),
                xmlRepodataArtifact.getFileMd5()
            )
            store(xmlRepomdNode, xmlRepodataArtifact)
        }
    }

    /**
     * 保存索引节点
     */
    fun storeXmlGZNode(
        repo: RepositoryDetail,
        xmlFile: File,
        repodataPath: String,
        indexType: IndexType
    ) {
        val xmlGZFile = xmlFile.gZip()
        try {
            val xmlFileSha1 = xmlFile.sha1()
            val xmlGZFileSha1 = xmlGZFile.sha1()
            val xmlGZArtifact = ArtifactFileFactory.build(FileInputStream(xmlGZFile))
            val fullPath = "$repodataPath/$xmlGZFileSha1-${indexType.value}.xml.gz"
            // 保存节点同时保存节点信息到元数据方便repomd更新。
            val metadata = mutableMapOf(
                "indexType" to indexType.value,
                "checksum" to xmlGZFileSha1,
                "size" to (xmlGZArtifact.getSize().toString()),
                "timestamp" to System.currentTimeMillis().toString(),
                "openChecksum" to xmlFileSha1,
                "openSize" to (xmlFile.length().toString())
            )

            val xmlGZNode = NodeCreateRequest(
                repo.projectId,
                repo.name,
                fullPath,
                false,
                0L,
                true,
                xmlGZArtifact.getSize(),
                xmlGZArtifact.getFileSha256(),
                xmlGZArtifact.getFileMd5(),
                metadata
            )
            store(xmlGZNode, xmlGZArtifact)
            GlobalScope.launch {
                val indexTypeList = getIndexTypeList(repo, repodataPath, indexType)
                deleteSurplusNode(indexTypeList)
            }.start()
        } finally {
            xmlGZFile.delete()
        }
    }

    fun deleteSurplusNode(list: List<NodeInfo>) {
        if (list.size > 2) {
            val surplusNodes = list.subList(2, list.size)
            for (node in surplusNodes) {
                nodeClient.deleteNode(NodeDeleteRequest(node.projectId, node.repoName, node.fullPath, node.createdBy))
                logger.info("Success to delete ${node.projectId}/${node.repoName}/${node.fullPath}")
            }
        }
    }

    fun getIndexTypeList(repo: RepositoryDetail, repodataPath: String, indexType: IndexType): List<NodeInfo> {
        val target = "-${indexType.value}.xml.gz"
        val indexList = nodeClient.listNodePage(
            repo.projectId, repo.name, repodataPath,
            NodeListOption(
                1,
                100,
                includeFolder = false,
                includeMetadata = false,
                deep = false,
                sort = false
            )
        ).data?.records ?: return mutableListOf()
        return indexList.filter { it.name.endsWith(target) }.sortedByDescending { it.lastModifiedDate }
    }

    fun store(node: NodeCreateRequest, artifactFile: ArtifactFile) {
        storageManager.storeArtifactFile(node, artifactFile, null)
        artifactFile.delete()
        with(node) { logger.info("Success to store$projectId/$repoName/$fullPath") }
        logger.info("Success to insert $node")
    }

    /**
     * 获取最新的索引或分组文件。
     */
    fun getLatestIndexNode(repo: RepositoryDetail, repodataPath: String, nameSuffix: String): NodeInfo? {
        logger.debug("getLatestIndexNode: [${repo.projectId}|${repo.name}|$repodataPath|$nameSuffix]")
        val ruleList = mutableListOf<Rule>(
            Rule.QueryRule("projectId", repo.projectId),
            Rule.QueryRule("repoName", repo.name),
            Rule.QueryRule("path", "${repodataPath.removeSuffix("/")}/"),
            Rule.QueryRule("folder", false, OperationType.EQ),
            Rule.QueryRule("name", "*-$nameSuffix", OperationType.MATCH)
        )
        val queryModel = QueryModel(
            page = PageLimit(1, 1),
            sort = Sort(listOf("lastModifiedDate"), Sort.Direction.DESC),
            select = mutableListOf(
                "projectId", "repoName", "fullPath", "name", "path", "metadata",
                "sha256", "md5", "size", "folder", "lastModifiedDate", "lastModifiedBy", "createdDate", "createdBy"
            ),
            rule = Rule.NestedRule(ruleList, Rule.NestedRule.RelationType.AND)
        )
        if (logger.isDebugEnabled) {
            logger.debug("queryModel: $queryModel")
        }
        var nodeList = nodeClient.search(queryModel).data!!.records.map { resolveNode(it) }
        val regex = Regex(
            "${IndexType.PRIMARY.value}.xml.gz" +
                "|${IndexType.OTHERS.value}.xml.gz" +
                "|${IndexType.FILELISTS.value}.xml.gz"
        )
        // 如果是索引文件则执行
        if (nameSuffix.matches(regex)) {
            val indexType = IndexType.valueOf(nameSuffix.removeSuffix(".xml.gz").toUpperCase())
            if (nodeList.isEmpty()) {
                initIndex(repo, repodataPath, indexType)
                nodeList = nodeClient.search(queryModel).data!!.records.map { resolveNode(it) }
            }
            if (nodeList.isEmpty()) {
                throw NodeNotFoundException(
                    "latest index node not found: [${repo.projectId}|${repo.name}|$repodataPath|$indexType]"
                )
            }
        } else {
            if (nodeList.isEmpty()) {
                return null
            }
        }
        return nodeList.first()
    }

    /**
     * 初始化索引
     */
    fun initIndex(repo: RepositoryDetail, repodataPath: String, indexType: IndexType) {
        val initStr = when (indexType) {
            IndexType.PRIMARY -> {
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                    "<metadata xmlns=\"http://linux.duke.edu/metadata/common\" xmlns:rpm=\"http://linux.duke" +
                    ".edu/metadata/rpm\" packages=\"0\">\n" +
                    "</metadata>"
            }
            IndexType.FILELISTS -> {
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                    "<metadata xmlns=\"http://linux.duke.edu/metadata/filelists\" packages=\"0\">\n" +
                    "</metadata>"
            }
            IndexType.OTHERS -> {
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                    "<metadata xmlns=\"http://linux.duke.edu/metadata/other\" packages=\"0\">\n" +
                    "</metadata>"
            }
        }
        val initIndexFile = File.createTempFile("initIndex", IndexType.OTHERS.value)
        FileOutputStream(initIndexFile).use { fos ->
            fos.write(initStr.toByteArray())
            fos.flush()
        }
        try {
            storeXmlGZNode(repo, initIndexFile, repodataPath, indexType)
        } finally {
            initIndexFile.delete()
        }
    }

    /**
     * [randomAccessFile] 索引文件
     * [repeat] 标记对rpm包的动作，比如，新增，更新，删除
     * [repo] 仓库
     * [locationStr] 节点路径
     * [indexType] 索引类型
     * return
     */
    fun updateIndex(
        randomAccessFile: RandomAccessFile,
        markNodeInfo: NodeInfo,
        repeat: ArtifactRepeat,
        repo: RepositoryDetail,
        repodataPath: String,
        locationStr: String?,
        indexType: IndexType
    ): Int {
        logger.info("updateIndex: [${repo.projectId}|${repo.name}|$repodataPath|$repeat|$locationStr|$indexType]")
        when (repeat) {
            ArtifactRepeat.NONE -> {
                logger.info("insert index of [${repo.projectId}|${repo.name}|${markNodeInfo.fullPath}")
                val markContent = resolveIndexXml(markNodeInfo, indexType) ?: return 0
                return XmlStrUtils.insertPackageIndex(randomAccessFile, markContent)
            }
            ArtifactRepeat.DELETE -> {
                logger.info("delete index of [${repo.projectId}|${repo.name}|${markNodeInfo.fullPath}]")
                val rpmVersion = markNodeInfo.metadata!!.toRpmVersion(markNodeInfo.fullPath)
                val uniqueStr = getLocationStr(indexType, rpmVersion, locationStr)
                return XmlStrUtils.deletePackageIndex(randomAccessFile, indexType, uniqueStr)
            }
            ArtifactRepeat.FULLPATH -> {
                logger.info("replace index of [${repo.projectId}|${repo.name}|${markNodeInfo.fullPath}]")
                val rpmVersion = markNodeInfo.metadata!!.toRpmVersion(markNodeInfo.fullPath)
                val uniqueStr = getLocationStr(indexType, rpmVersion, locationStr)
                val markContent = resolveIndexXml(markNodeInfo, indexType) ?: return 0
                return XmlStrUtils.updatePackageIndex(randomAccessFile, indexType, uniqueStr, markContent)
            }
            ArtifactRepeat.FULLPATH_SHA256 -> {
                logger.info("skip index of [${repo.projectId}|${repo.name}|${markNodeInfo.fullPath}]")
                return 0
            }
        }
    }

    /**
     * 在索引中可以唯一确定一个包的识别字段
     */
    private fun getLocationStr(
        indexType: IndexType,
        rpmVersion: RpmVersion,
        location: String?
    ): String {
        return when (indexType) {
            IndexType.OTHERS, IndexType.FILELISTS -> {
                with(rpmVersion) {
                    """name="$name">
    <version epoch="$epoch" ver="$ver" rel="$rel"/>"""
                }
            }
            IndexType.PRIMARY -> {
                """<location href="$location"/>"""
            }
        }
    }

    private fun resolveIndexXml(indexNodeInfo: NodeInfo, indexType: IndexType): ByteArray? {
        storageService.load(
            indexNodeInfo.sha256!!,
            Range.full(indexNodeInfo.size), null
        ).use { inputStream ->
            val content = inputStream!!.readBytes()
            return if (XStreamUtil.checkMarkFile(content, indexType)) {
                content
            } else {
                null
            }
        }
    }

    private fun resolveNode(mapData: Map<String, Any?>): NodeInfo {
        return NodeInfo(
            createdBy = mapData["createdBy"] as String,
            createdDate = mapData["lastModifiedBy"] as String,
            lastModifiedBy = mapData["lastModifiedDate"] as String,
            lastModifiedDate = mapData["lastModifiedDate"] as String,
            folder = mapData["folder"] as Boolean,
            path = mapData["path"] as String,
            name = mapData["name"] as String,
            fullPath = mapData["fullPath"] as String,
            size = mapData["size"].toString().toLong(),
            sha256 = mapData["sha256"] as String,
            md5 = mapData["md5"] as String,
            projectId = mapData["projectId"] as String,
            repoName = mapData["repoName"] as String,
            metadata = if (mapData["metadata"] == null) {
                mapOf()
            } else {
                (mapData["metadata"] as Map<String, Any>).mapValues { it.value.toString() }
            }
        )
    }

    /**
     * 查询带处理的节点
     */
    fun listMarkNodes(
        repo: RepositoryDetail,
        repodataPath: String,
        indexType: IndexType,
        limit: Int
    ): Page<NodeInfo> {
        logger.debug("listMarkNodes: [$repo|$repodataPath|$indexType|$limit])")
        val indexMarkFolder = "$repodataPath/${indexType.value}/"
        val ruleList = mutableListOf<Rule>(
            Rule.QueryRule("projectId", repo.projectId, OperationType.EQ),
            Rule.QueryRule("repoName", repo.name, OperationType.EQ),
            Rule.QueryRule("path", indexMarkFolder, OperationType.EQ),
            Rule.QueryRule("folder", false, OperationType.EQ),
            Rule.QueryRule("name", "*.rpm", OperationType.MATCH)
        )
        val queryModel = QueryModel(
            page = PageLimit(1, limit),
            sort = Sort(listOf("lastModifiedDate"), Sort.Direction.ASC),
            select = mutableListOf(
                "projectId", "repoName", "fullPath", "name", "path", "metadata",
                "sha256", "md5", "size", "folder", "lastModifiedDate", "lastModifiedBy", "createdDate", "createdBy"
            ),
            rule = Rule.NestedRule(ruleList, Rule.NestedRule.RelationType.AND)
        )
        if (logger.isDebugEnabled) {
            logger.debug("queryModel: $queryModel")
        }
        val resultPage = nodeClient.search(queryModel).data!!
        with(resultPage) { return Page(pageNumber, pageSize, totalRecords, records.map { resolveNode(it) }) }
    }

    /**
     * 更新索引
     */
    fun batchUpdateIndex(repo: RepositoryDetail, repodataPath: String, indexType: IndexType, maxCount: Int) {
        logger.info("batchUpdateIndex, [${repo.projectId}|${repo.name}|$repodataPath|$indexType]")
        val markNodePage = listMarkNodes(repo, repodataPath, indexType, maxCount)
        if (markNodePage.records.isEmpty()) {
            logger.info("no index file to process")
            return
        }
        logger.info(
            "${markNodePage.records.size} of " +
                    "${markNodePage.totalRecords} ${indexType.name} mark file to process"
        )
        val markNodes = markNodePage.records
        val latestIndexNode = getLatestIndexNode(repo, repodataPath, "${indexType.value}.xml.gz")!!
        logger.info("latestIndexNode, fullPath: ${latestIndexNode.fullPath}")
        val unzipedIndexTempFile = storageService.load(
            latestIndexNode.sha256!!,
            Range.full(latestIndexNode.size), null
        )!!.use { it.unGzipInputStream() }
        logger.info(
            "temp index file " +
                    "${unzipedIndexTempFile.absolutePath}(${HumanReadable.size(unzipedIndexTempFile.length())}) created"
        )
        try {
            val processedMarkNodes = mutableListOf<NodeInfo>()
            var changeCount = 0
            RandomAccessFile(unzipedIndexTempFile, "rw").use { randomAccessFile ->
                markNodes.forEach { markNode ->
                    changeCount += updateIndexFile(randomAccessFile, markNode, indexType, repo, repodataPath)
                    processedMarkNodes.add(markNode)
                }

                logger.debug("changeCount: $changeCount")
                if (changeCount != 0) {
                    val start = System.currentTimeMillis()
                    XmlStrUtils.updatePackageCount(randomAccessFile, indexType, changeCount, false)
                    logger.debug(
                        "updatePackageCount indexType: $indexType," +
                                " indexFileSize: ${HumanReadable.size(randomAccessFile.length())}, " +
                                "cost: ${System.currentTimeMillis() - start} ms"
                    )
                }
            }
            checkValid(unzipedIndexTempFile)
            storeXmlGZNode(repo, unzipedIndexTempFile, repodataPath, indexType)
            flushRepoMdXML(repo, repodataPath)
            deleteNodes(processedMarkNodes)
        } finally {
            unzipedIndexTempFile.delete()
            logger.info("temp index file ${unzipedIndexTempFile.absolutePath} deleted")
        }
    }

    private fun updateIndexFile(
        randomAccessFile: RandomAccessFile,
        markNode: NodeInfo,
        indexType: IndexType,
        repo: RepositoryDetail,
        repodataPath: String
    ): Int {
        // rpm构件位置
        val locationStr = markNode.fullPath.replace("/repodata/${indexType.value}", "")
        val repodataDepth = getRpmRepoConf(repo.projectId, repo.name).repodataDepth
        // 保存在索引中的相对路径
        val pathList = locationStr.removePrefix("/").split("/")
        val stringBuilder = StringBuilder()
        for (i in repodataDepth until pathList.size) {
            stringBuilder.append("/").append(pathList[i])
        }
        val locationHref = stringBuilder.toString().removePrefix("/")
        logger.debug("locationStr: $locationStr, locationHref: $locationHref")
        with(markNode) { logger.info("process mark node[$projectId|$repoName|$fullPath]") }
        val repeat = ArtifactRepeat.valueOf(
            markNode.metadata?.get("repeat") as String? ?: "FULLPATH_SHA256"
        )
        return if (repeat == ArtifactRepeat.DELETE) {
            updateIndex(randomAccessFile, markNode, repeat, repo, repodataPath, locationHref, indexType)
        } else {
            val rpmNode = nodeClient.getNodeDetail(markNode.projectId, markNode.repoName, locationStr).data
            if (rpmNode == null) {
                with(markNode) {
                    logger.info("rpm node[$projectId|$repoName|$locationStr] no found, skip index")
                }
                return 0
            }
            updateIndex(randomAccessFile, markNode, repeat, repo, repodataPath, locationHref, indexType)
        }
    }

    fun checkValid(xmlFile: File) {
        val start = System.currentTimeMillis()
        val factory = SAXParserFactory.newInstance()
        val saxParser = factory.newSAXParser()
        saxParser.parse(xmlFile, DefaultHandler())
        logger.debug("checkValid, cost: ${System.currentTimeMillis() - start} ms")
    }

    private fun deleteNodes(nodes: List<NodeInfo>) {
        nodes.forEach { nodeInfo ->
            with(nodeInfo) {
                try {
                    nodeClient.deleteNode(NodeDeleteRequest(projectId, repoName, fullPath, "system"))
                    logger.info("node[$projectId|$repoName|$fullPath] deleted")
                } catch (e: Exception) {
                    logger.info("node[$projectId|$repoName|$fullPath] delete exception, ${e.message}")
                }
            }
        }
    }

    companion object {
        private const val MAX_REPO_PAGE_SIE = 1000
        private val logger: Logger = LoggerFactory.getLogger(JobService::class.java)
    }
}
