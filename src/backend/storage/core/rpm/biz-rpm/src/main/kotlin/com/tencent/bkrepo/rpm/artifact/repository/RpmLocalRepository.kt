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

package com.tencent.bkrepo.rpm.artifact.repository

import com.tencent.bkrepo.common.api.constant.StringPool.DASH
import com.tencent.bkrepo.common.api.constant.StringPool.DOT
import com.tencent.bkrepo.common.api.constant.StringPool.SLASH
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.constant.ATTRIBUTE_OCTET_STREAM_SHA256
import com.tencent.bkrepo.common.artifact.hash.sha1
import com.tencent.bkrepo.common.artifact.pojo.configuration.local.repository.RpmLocalConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Sort
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import com.tencent.bkrepo.rpm.FILELISTS
import com.tencent.bkrepo.rpm.OTHERS
import com.tencent.bkrepo.rpm.PRIMARY
import com.tencent.bkrepo.rpm.XMLGZ
import com.tencent.bkrepo.rpm.REPODATA
import com.tencent.bkrepo.rpm.INDEXER
import com.tencent.bkrepo.rpm.NO_INDEXER
import com.tencent.bkrepo.rpm.artifact.SurplusNodeCleaner
import com.tencent.bkrepo.rpm.pojo.ArtifactRepeat
import com.tencent.bkrepo.rpm.pojo.ArtifactRepeat.FULLPATH_SHA256
import com.tencent.bkrepo.rpm.pojo.ArtifactRepeat.NONE
import com.tencent.bkrepo.rpm.pojo.ArtifactRepeat.FULLPATH
import com.tencent.bkrepo.rpm.pojo.RepomdChildNode
import com.tencent.bkrepo.rpm.pojo.RpmRepoConf
import com.tencent.bkrepo.rpm.pojo.RpmUploadResponse
import com.tencent.bkrepo.rpm.util.GZipUtil.unGzipInputStream
import com.tencent.bkrepo.rpm.util.GZipUtil.gZip
import com.tencent.bkrepo.rpm.util.XmlStrUtil
import com.tencent.bkrepo.rpm.util.rpm.RpmMetadataUtil
import com.tencent.bkrepo.rpm.util.rpm.RpmFormatUtil
import com.tencent.bkrepo.rpm.util.xStream.XStreamUtil.objectToXml
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmXmlMetadata
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmMetadataChangeLog
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmMetadataFileList
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmPackageFileList
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmPackageChangeLog
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmLocation
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmChecksum
import com.tencent.bkrepo.rpm.util.xStream.repomd.RepoData
import com.tencent.bkrepo.rpm.util.xStream.repomd.Repomd
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.nio.channels.Channels

@Component
class RpmLocalRepository(
    val surplusNodeCleaner: SurplusNodeCleaner
) : LocalRepository() {

    fun rpmNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        val nodeCreateRequest = super.getNodeCreateRequest(context)
        return nodeCreateRequest.copy(overwrite = true)
    }

    fun xmlPrimaryNodeCreate(
        userId: String,
        repositoryInfo: RepositoryInfo,
        fullPath: String,
        xmlGZArtifact: ArtifactFile
    ): NodeCreateRequest {
        val sha256 = xmlGZArtifact.getFileSha256()
        val md5 = xmlGZArtifact.getFileMd5()
        return NodeCreateRequest(
            projectId = repositoryInfo.projectId,
            repoName = repositoryInfo.name,
            folder = false,
            overwrite = true,
            fullPath = fullPath,
            size = xmlGZArtifact.getSize(),
            sha256 = sha256,
            md5 = md5,
            operator = userId
        )
    }

    /**
     * 查询仓库设置的repodata 深度，默认为0
     */
    @Deprecated("getRpmRepoConf()")
    private fun searchRpmRepoDataDepth(context: ArtifactUploadContext): Int {
        (context.repositoryInfo.configuration as RpmLocalConfiguration).repodataDepth.let { return it }
    }

    /**
     * 查询rpm仓库属性
     */
    private fun getRpmRepoConf(context: ArtifactUploadContext): RpmRepoConf {
        val repodataDepth = (context.repositoryInfo.configuration as RpmLocalConfiguration).repodataDepth
        val enabledFileLists = (context.repositoryInfo.configuration as RpmLocalConfiguration).enabledFileLists
        return RpmRepoConf(repodataDepth, enabledFileLists)
    }

    /**
     * 检查请求uri地址的层级是否 > 仓库设置的repodata 深度
     * @return true 将会计算rpm包的索引
     * @return false 只提供文件服务器功能，返回提示信息
     */
    private fun checkRequestUri(context: ArtifactUploadContext, repodataDepth: Int): Boolean {
        val artifactUri = context.artifactInfo.artifactUri
            .removePrefix(SLASH).split(SLASH).size
        return artifactUri > repodataDepth
    }

    /**
     * 生成构件索引
     */
    private fun indexer(context: ArtifactUploadContext, repeat: ArtifactRepeat, rpmRepoConf: RpmRepoConf) {

        val repodataDepth = rpmRepoConf.repodataDepth
        val uriMap = XmlStrUtil.splitUriByDepth(context.artifactInfo.artifactUri, repodataDepth)
        val repodataPath = uriMap.repodataPath

        val artifactFile = context.getArtifactFile()
        val rpmFormat = RpmFormatUtil.getRpmFormat(Channels.newChannel(artifactFile.getInputStream()))

        val sha1Digest = artifactFile.getInputStream().sha1()
        val artifactRelativePath = uriMap.artifactRelativePath
        val rpmMetadata = RpmMetadataUtil().interpret(
            rpmFormat,
            artifactFile.getSize(),
            sha1Digest,
            artifactRelativePath
        )
        val indexList = mutableListOf<RepomdChildNode>()
        if (rpmRepoConf.enabledFileLists) {
            val rpmMetadataFileList = RpmMetadataFileList(
                listOf(
                    RpmPackageFileList(
                        rpmMetadata.packages[0].checksum.checksum,
                        rpmMetadata.packages[0].name,
                        rpmMetadata.packages[0].version,
                        rpmMetadata.packages[0].format.files
                    )
                ),
                1L
            )
            updateIndexXml(context, rpmMetadataFileList, repeat, repodataPath, FILELISTS)?.let { indexList.add(it) }
            // 更新filelists.xml
            rpmMetadata.packages[0].format.files.clear()
        }
        val rpmMetadataChangeLog = RpmMetadataChangeLog(
            listOf(
                RpmPackageChangeLog(
                    rpmMetadata.packages[0].checksum.checksum,
                    rpmMetadata.packages[0].name,
                    rpmMetadata.packages[0].version,
                    rpmMetadata.packages[0].format.changeLogs
                )
            ),
            1L
        )
        // 更新others.xml
        updateIndexXml(context, rpmMetadataChangeLog, repeat, repodataPath, OTHERS)?.let { indexList.add(it) }
        rpmMetadata.packages[0].format.changeLogs.clear()
        // 更新primary.xml
        updateIndexXml(context, rpmMetadata, repeat, repodataPath, PRIMARY)?.let { indexList.add(it) }
        storeRepomdNode(indexList, repodataPath, context)
    }

    private fun updateIndexXml(
        context: ArtifactUploadContext,
        rpmXmlMetadata: RpmXmlMetadata,
        repeat: ArtifactRepeat,
        repodataPath: String,
        indexType: String
    ): RepomdChildNode? {
        val target = "$DASH$indexType$DOT$XMLGZ"

        with(context.repositoryInfo) {
            // repodata下'-primary.xml.gz'最新节点。
            val nodeList = nodeClient.list(
                projectId, name,
                "$SLASH${repodataPath}$REPODATA",
                includeFolder = false, deep = false
            ).data
            val targetNodelist = nodeList?.filter {
                it.name.endsWith(target)
            }?.sortedByDescending {
                it.createdDate
            }

            val targetXmlString = if (!targetNodelist.isNullOrEmpty()) {
                val latestPrimaryNode = targetNodelist[0]
                val inputStream = storageService.load(
                    latestPrimaryNode.sha256!!,
                    Range.full(latestPrimaryNode.size),
                    context.storageCredentials
                ) ?: return null
                // 更新primary.xml
                if (repeat == NONE) {
                    XmlStrUtil.insertPackage(indexType, inputStream.unGzipInputStream(), rpmXmlMetadata)
                } else {
                    XmlStrUtil.updatePackage(indexType, inputStream.unGzipInputStream(), rpmXmlMetadata)
                }
            } else {
                // first upload
                rpmXmlMetadata.objectToXml()
            }

            // 删除多余索引节点
            GlobalScope.launch {
                targetNodelist?.let { surplusNodeCleaner.deleteSurplusNode(it) }
            }.start()
            return storeXmlNode(indexType, targetXmlString, repodataPath, context, target)
        }
    }

    /**
     * 保存索引节点
     * @param xmlStr "-primary.xml" 索引文件内容
     * @param repodataPath 契合本次请求的repodata_depth 目录路径
     */
    private fun storeXmlNode(
        indexType: String,
        xmlStr: String,
        repodataPath: String,
        context: ArtifactUploadContext,
        target: String
    ): RepomdChildNode {
        ByteArrayInputStream((xmlStr.toByteArray())).use { xmlInputStream ->
            // 处理xml节点
            val xmlFileSize = xmlStr.toByteArray().size
            // xml.gz文件sha1
            val xmlGZFile = xmlStr.toByteArray().gZip(indexType)
            try {
                val xmlGZFileSha1 = FileInputStream(xmlGZFile).sha1()

                // 先保存primary-xml.gz文件
                val xmlGZArtifact = ArtifactFileFactory.build(FileInputStream(xmlGZFile))
                val fullPath = "$SLASH${repodataPath}$REPODATA$SLASH$xmlGZFileSha1$target"
                val xmlPrimaryNode = xmlPrimaryNodeCreate(
                    context.userId,
                    context.repositoryInfo,
                    fullPath,
                    xmlGZArtifact
                )
                storageService.store(xmlPrimaryNode.sha256!!, xmlGZArtifact, context.storageCredentials)
                nodeClient.create(xmlPrimaryNode)

                // 更新repomd.xml
                // xml文件sha1
                val xmlFileSha1 = xmlInputStream.sha1()
                return RepomdChildNode(indexType, xmlFileSize, xmlGZFileSha1, xmlGZArtifact, xmlFileSha1)
            } finally {
                xmlGZFile.delete()
            }
        }
    }

    /**
     * 更新repomd.xml
     */
    private fun storeRepomdNode(
        indexList: MutableList<RepomdChildNode>,
        repodataPath: String,
        context: ArtifactUploadContext
    ) {
        val repoDataList = mutableListOf<RepoData>()
        for (index in indexList) {
            repoDataList.add(
                with(index) {
                    RepoData(
                        type = indexType,
                        location = RpmLocation("$REPODATA$SLASH$xmlGZFileSha1$DASH${indexType}$DOT$XMLGZ"),
                        checksum = RpmChecksum(xmlGZFileSha1),
                        size = xmlGZArtifact.getSize(),
                        timestamp = System.currentTimeMillis().toString(),
                        openChecksum = RpmChecksum(xmlFileSha1),
                        openSize = xmlFileSize
                    )
                }
            )
        }
        val repomd = Repomd(
            repoDataList
        )
        val xmlRepodataString = repomd.objectToXml()
        ByteArrayInputStream((xmlRepodataString.toByteArray())).use { xmlRepodataInputStream ->
            val xmlRepodataArtifact = ArtifactFileFactory.build(xmlRepodataInputStream)
            // 保存repodata 节点
            val xmlRepomdNode = xmlPrimaryNodeCreate(
                context.userId,
                context.repositoryInfo,
                "$SLASH${repodataPath}$REPODATA${SLASH}repomd.xml",
                xmlRepodataArtifact
            )
            storageService.store(xmlRepomdNode.sha256!!, xmlRepodataArtifact, context.storageCredentials)
            nodeClient.create(xmlRepomdNode)
            xmlRepodataArtifact.delete()
        }
    }

    /**
     * 检查上传的构件是否已在仓库中，判断条件：uri && sha256
     * 降低并发对索引文件的影响
     * @return ArtifactRepeat.FULLPATH_SHA256 存在完全相同构件，不操作索引
     * @return ArtifactRepeat.FULLPATH 请求路径相同，但内容不同，更新索引
     * @return ArtifactRepeat.NONE 无重复构件
     */
    private fun checkRepeatArtifact(context: ArtifactUploadContext): ArtifactRepeat {
        val artifactUri = context.artifactInfo.artifactUri
        val artifactSha256 = context.contextAttributes[ATTRIBUTE_OCTET_STREAM_SHA256] as String

        return with(context.artifactInfo) {
            val projectQuery = Rule.QueryRule("projectId", projectId)
            val repositoryQuery = Rule.QueryRule("repoName", repoName)
            val fullPathQuery = Rule.QueryRule("fullPath", artifactUri)

            val queryRule = Rule.NestedRule(
                mutableListOf(projectQuery, repositoryQuery, fullPathQuery),
                Rule.NestedRule.RelationType.AND
            )
            val queryModel = QueryModel(
                page = PageLimit(0, 10),
                sort = Sort(listOf("name"), Sort.Direction.ASC),
                select = mutableListOf("projectId", "repoName", "fullPath", "sha256"),
                rule = queryRule
            )
            val nodeList = nodeClient.query(queryModel).data?.records
            if (nodeList.isNullOrEmpty()) {
                NONE
            } else {
                // 上传时重名构件默认是覆盖操作，所以只会存在一个重名构件。
                if (nodeList.first()["sha256"] == artifactSha256) {
                    FULLPATH_SHA256
                } else {
                    FULLPATH
                }
            }
        }
    }

    private fun successUpload(context: ArtifactUploadContext, mark: Boolean, repodataDepth: Int) {
        val response = HttpContextHolder.getResponse()
        response.contentType = "application/json; charset=UTF-8"
        with(context.artifactInfo) {
            val description = if (mark) {
                INDEXER
            } else {
                String.format(NO_INDEXER, "$projectId/$repoName", repodataDepth, artifactUri)
            }
            val rpmUploadResponse = RpmUploadResponse(
                projectId, repoName, artifactUri,
                context.getArtifactFile().getFileSha256(), context.getArtifactFile().getFileMd5(), description
            )
            response.writer.print(rpmUploadResponse.toJsonString())
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun onUpload(context: ArtifactUploadContext) {
        // 检查请求路径是否契合仓库repodataDepth 深度设置
        val rpmRepoConf = getRpmRepoConf(context)
        val mark: Boolean = checkRequestUri(context, rpmRepoConf.repodataDepth)
        val repeat = checkRepeatArtifact(context)
        if (mark && (repeat != FULLPATH_SHA256)) { indexer(context, repeat, rpmRepoConf) }
        // 保存rpm 包
        val nodeCreateRequest = rpmNodeCreateRequest(context)
        storageService.store(nodeCreateRequest.sha256!!, context.getArtifactFile(), context.storageCredentials)
        nodeClient.create(nodeCreateRequest)
        successUpload(context, mark, rpmRepoConf.repodataDepth)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RpmLocalRepository::class.java)
    }
}
