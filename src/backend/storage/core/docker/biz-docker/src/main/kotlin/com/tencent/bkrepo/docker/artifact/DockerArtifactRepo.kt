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

package com.tencent.bkrepo.docker.artifact

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.constant.StringPool.EMPTY
import com.tencent.bkrepo.common.api.constant.StringPool.SLASH
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.docker.constant.DOCKER_CREATE_BY
import com.tencent.bkrepo.docker.constant.DOCKER_MANIFEST
import com.tencent.bkrepo.docker.constant.DOCKER_NODE_FULL_PATH
import com.tencent.bkrepo.docker.constant.DOCKER_NODE_PATH
import com.tencent.bkrepo.docker.constant.DOCKER_NODE_SIZE
import com.tencent.bkrepo.docker.constant.LAST_MODIFIED_BY
import com.tencent.bkrepo.docker.constant.LAST_MODIFIED_DATE
import com.tencent.bkrepo.docker.constant.REPO_TYPE
import com.tencent.bkrepo.docker.constant.STAGE_TAG
import com.tencent.bkrepo.docker.context.DownloadContext
import com.tencent.bkrepo.docker.context.RequestContext
import com.tencent.bkrepo.docker.context.UploadContext
import com.tencent.bkrepo.docker.exception.DockerFileReadFailedException
import com.tencent.bkrepo.docker.exception.DockerFileSaveFailedException
import com.tencent.bkrepo.docker.exception.DockerMoveFileFailedException
import com.tencent.bkrepo.docker.exception.DockerRepoNotFoundException
import com.tencent.bkrepo.docker.pojo.DockerImage
import com.tencent.bkrepo.docker.pojo.DockerTag
import com.tencent.bkrepo.repository.api.MetadataClient
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeRenameRequest
import com.tencent.bkrepo.repository.pojo.search.NodeQueryBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream

/**
 * docker repo storage interface
 * to work with storage module
 */
@Service
class DockerArtifactRepo @Autowired constructor(
    val repoClient: RepositoryClient,
    private val nodeClient: NodeClient,
    private val storage: StorageService,
    private val metadata: MetadataClient,
    private val permission: PermissionManager
) {

    lateinit var userId: String

    @Value("\${docker.domain: ''}")
    val domain: String = EMPTY

    @Value("\${docker.http: false}")
    val enableHttp: Boolean = false

    /**
     * start a append upload
     * @param context the request context params
     * @return String append Id
     */
    fun startAppend(context: RequestContext): String {
        // check repository
        val repository = repoClient.getRepoDetail(context.projectId, context.repoName, REPO_TYPE).data ?: run {
            logger.warn("user [$userId]  download file [$context] repository not found")
            throw DockerRepoNotFoundException(context.repoName)
        }
        logger.debug("user [$userId] start to append file ")
        return storage.createAppendId(repository.storageCredentials)
    }

    /**
     * write data in a append upload
     * @param context the request context params
     * @param uuid the append id
     * @param artifactFile file object
     * @return Long the size of the file
     */
    fun writeAppend(context: RequestContext, uuid: String, artifactFile: ArtifactFile): Long {
        // check repository
        val repository = repoClient.getRepoDetail(context.projectId, context.repoName, REPO_TYPE).data ?: run {
            logger.warn("user [$userId] append file [$context] repository not found")
            throw DockerRepoNotFoundException(context.repoName)
        }
        logger.debug("user [$userId]  append file id [$uuid]")
        return this.storage.append(uuid, artifactFile, repository.storageCredentials)
    }

    /**
     * finish a append upload
     * @param context the upload context params
     * @param uuid the append id
     * @return Boolean is the file upload success
     */
    fun finishAppend(context: UploadContext, uuid: String): Boolean {
        // check repository
        val repository = repoClient.getRepoDetail(context.projectId, context.repoName, REPO_TYPE).data ?: run {
            logger.warn("user[$userId]  finish append file  [$context] not found")
            throw DockerRepoNotFoundException(context.repoName)
        }
        val file = this.storage.finishAppend(uuid, repository.storageCredentials)
        val node = NodeCreateRequest(
            projectId = context.projectId,
            repoName = context.repoName,
            folder = false,
            fullPath = context.fullPath,
            size = file.size,
            sha256 = file.sha256,
            md5 = file.md5,
            operator = userId,
            metadata = emptyMap(),
            overwrite = true
        )
        // save node
        val result = nodeClient.createNode(node)
        if (result.isNotOk()) {
            logger.error("user [$userId] finish append file  [${context.fullPath}] failed: [$result]")
            throw DockerFileSaveFailedException(context.fullPath)
        }
        logger.debug("user [$userId] finish append file  [${context.fullPath} , ${file.sha256}] success")
        return true
    }

    /**
     * download a file
     * @param context the download context params
     * @return InputStream the file download inputstream
     */
    fun download(context: DownloadContext): InputStream {
        // check repository
        val rContext = context.context
        val repository = repoClient.getRepoDetail(rContext.projectId, rContext.repoName, REPO_TYPE).data ?: run {
            logger.warn("user [$userId]  download file [$rContext] repository not found")
            throw DockerRepoNotFoundException(rContext.repoName)
        }
        logger.debug("load file  [$rContext]")
        // load file from storage
        return storage.load(
            context.sha256,
            Range.full(context.length),
            repository.storageCredentials
        ) ?: run {
            logger.error("user [$userId] fail to load data [$rContext] from storage  ")
            throw DockerFileReadFailedException(rContext.repoName)
        }
    }

    /**
     * upload a file
     * @param context the upload context params
     * @return Boolean is the file upload success
     */
    fun upload(context: UploadContext): Boolean {
        with(context) {
            // check repository
            val repository = repoClient.getRepoDetail(projectId, repoName, REPO_TYPE).data ?: run {
                logger.warn("user [$userId]  upload file  [$context] repository not found")
                throw DockerRepoNotFoundException(context.repoName)
            }

            logger.debug("user [$userId] start to store file [${context.sha256}]")
            // store the file
            storage.store(sha256, artifactFile!!, repository.storageCredentials)
            // save the node
            val result = nodeClient.createNode(
                NodeCreateRequest(
                    projectId = projectId,
                    repoName = repoName,
                    folder = false,
                    fullPath = fullPath,
                    size = artifactFile!!.getSize(),
                    sha256 = artifactFile!!.getFileSha256(),
                    md5 = artifactFile!!.getFileMd5(),
                    operator = userId,
                    metadata = metadata,
                    overwrite = true
                )
            )
            if (result.isNotOk()) {
                logger.error("user [$userId]  upload file [${context.fullPath}] failed: [$result]")
                throw DockerFileSaveFailedException(context.fullPath)
            }
            logger.debug("user [$userId]  upload file [${context.fullPath}] success")
            return true
        }
    }

    /**
     * copy a file from source path to destination path
     * @param context the upload context params
     * @param srcPath the source path of file
     * @param destPath the destination file of file
     * @return Boolean is the file upload success
     */
    fun copy(context: RequestContext, srcPath: String, destPath: String): Boolean {
        logger.debug("user [$userId] start to copy file [$context,$srcPath,$destPath]")
        with(context) {
            val copyRequest = NodeMoveCopyRequest(
                srcProjectId = projectId,
                srcRepoName = repoName,
                srcFullPath = srcPath,
                destProjectId = projectId,
                destRepoName = repoName,
                destFullPath = destPath,
                overwrite = true,
                operator = userId
            )
            if (nodeClient.copyNode(copyRequest).isNotOk()) {
                logger.error("user [$userId] request [$copyRequest] copy file fail")
                throw DockerMoveFileFailedException("$srcPath->$destPath")
            }
            return true
        }
    }

    /**
     * move a file from source path to destination path
     * @param context the upload context params
     * @param from the source path of file
     * @param to the destination file of file
     * @return Boolean is the file upload success
     */
    fun move(context: RequestContext, from: String, to: String): Boolean {
        with(context) {
            val renameRequest = NodeRenameRequest(projectId, repoName, from, to, userId)
            logger.debug("move request [$renameRequest]")
            val result = nodeClient.renameNode(renameRequest)
            if (result.isNotOk()) {
                logger.error("user [$userId] request [$renameRequest] move file fail")
                throw DockerMoveFileFailedException("$from->$to")
            }
            return true
        }
    }

    /**
     * get attributes of a file
     * @param projectId project of the repo
     * @param repoName name of the repo
     * @param fullPath full path of file
     * @param key metadata
     * @return String is the value of the metadata
     */
    fun getAttribute(projectId: String, repoName: String, fullPath: String, key: String): String? {
        val result = metadata.listMetadata(projectId, repoName, fullPath).data!!
        logger.debug("get attribute params : [$projectId, $repoName, $fullPath, $key] ,result: [$result]")
        return result[key] as String
    }

    /**
     * check is the node is exist
     * @param projectId project of the repo
     * @param repoName name of the repo
     * @param fullPath full path of file
     * @return Boolean is the file exist
     */
    fun exists(projectId: String, repoName: String, fullPath: String): Boolean {
        return nodeClient.checkExist(projectId, repoName, fullPath).data ?: return false
    }

    /**
     * check is the node can be read
     * @param context the request context
     * @return Boolean is the file can be read
     */
    fun canRead(context: RequestContext): Boolean {
        val repoPermission = checkProjectRepoPermission(context, ResourceType.REPO, PermissionAction.READ)
        if (repoPermission) return true
        return checkProjectRepoPermission(context, ResourceType.PROJECT, PermissionAction.READ)
    }

    /**
     * check is the node can be write
     * @param context the request context
     * @return Boolean is the file can be write
     */
    fun canWrite(context: RequestContext): Boolean {
        val repoPermission = checkProjectRepoPermission(context, ResourceType.REPO, PermissionAction.WRITE)
        if (repoPermission) return true
        return checkProjectRepoPermission(context, ResourceType.PROJECT, PermissionAction.WRITE)
    }

    /**
     * check  project repository permission
     * @param context the request context
     * @param resourceType ResourceType
     * @param action PermissionAction
     * @return Boolean is the file can be write
     */
    private fun checkProjectRepoPermission(
        context: RequestContext,
        resourceType: ResourceType,
        action: PermissionAction
    ): Boolean {
        try {
            if (resourceType == ResourceType.PROJECT) {
                permission.checkProjectPermission(action, context.projectId)
            }
            if (resourceType == ResourceType.REPO) {
                permission.checkRepoPermission(action, context.projectId, context.repoName)
            }
        } catch (e: Exception) {
            logger.debug("user: [$userId] ,check  permission fail [$context]")
            return false
        }
        return true
    }

    // get docker artifact
    fun getArtifact(projectId: String, repoName: String, fullPath: String): DockerArtifact? {
        val node = nodeClient.getNodeDetail(projectId, repoName, fullPath).data ?: run {
            logger.warn("get artifact detail failed: [$projectId, $repoName, $fullPath] found no artifact")
            return null
        }
        node.sha256 ?: run {
            logger.error("get artifact detail failed: [$projectId, $repoName, $fullPath] sha 256 empty")
            return null
        }
        return DockerArtifact(projectId, repoName, fullPath).sha256(node.sha256!!).length(node.size)
    }

    // get node detail
    fun getNodeDetail(context: RequestContext, fullPath: String): NodeDetail? {
        with(context) {
            return nodeClient.getNodeDetail(projectId, repoName, fullPath).data ?: run<RequestContext, NodeDetail> {
                logger.warn("get artifact detail failed: [$projectId, $repoName, $fullPath] found no artifact")
                return null
            }
        }
    }

    // get artifact list by name
    fun getArtifactListByName(projectId: String, repoName: String, artifactName: String): List<Map<String, Any?>> {
        val queryModel = NodeQueryBuilder()
            .select(DOCKER_NODE_FULL_PATH, DOCKER_NODE_PATH, DOCKER_NODE_SIZE)
            .projectId(projectId)
            .repoName(repoName)
            .name(artifactName)
            .sortByAsc(DOCKER_NODE_FULL_PATH)
            .page(DOCKER_SEARCH_INDEX, DOCKER_SEARCH_LIMIT_SMALL)

        val result = nodeClient.search(queryModel.build()).data ?: run {
            logger.warn("get artifact list: [$projectId, $repoName, $artifactName] found no node")
            return emptyList()
        }
        return result.records
    }

    // get artifact list by name and digest
    fun getArtifactByNameAndDigest(context: RequestContext, fileName: String, sha256: String): Map<String, Any?>? {
        with(context) {
            val queryModelBuilder = NodeQueryBuilder()
                .select(DOCKER_NODE_FULL_PATH, DOCKER_NODE_SIZE)
                .projectId(projectId)
                .repoName(repoName)
                .name(fileName)
                .sha256(sha256)
                .fullPath("/$artifactName", OperationType.PREFIX)
                .sortByAsc(DOCKER_NODE_FULL_PATH)
                .page(DOCKER_SEARCH_INDEX, DOCKER_SEARCH_LIMIT_SMALL)
            logger.debug("get artifact params [$context, $fileName, $sha256]")
            val result = nodeClient.search(queryModelBuilder.build()).data ?: run {
                logger.warn("get artifact list: [$context, $fileName, $sha256] found no node")
                return null
            }
            if (result.records.isEmpty()) return null
            return result.records[0]
        }
    }

    // get docker image list
    fun getDockerArtifactList(
        projectId: String,
        repoName: String,
        pageNumber: Int,
        pageSize: Int,
        name: String?
    ): List<DockerImage> {
        val queryModel = NodeQueryBuilder().select(
            DOCKER_NODE_FULL_PATH,
            DOCKER_NODE_PATH,
            DOCKER_NODE_SIZE,
            LAST_MODIFIED_BY,
            LAST_MODIFIED_DATE
        ).projectId(projectId).repoName(repoName).name(DOCKER_MANIFEST).sortByAsc(DOCKER_NODE_FULL_PATH)
        name?.let {
            queryModel.path("*$name*", OperationType.MATCH)
        }
        val result = nodeClient.search(queryModel.build()).data ?: run {
            logger.warn("find repo list failed: [$projectId, $repoName] ")
            return emptyList()
        }
        val data = mutableListOf<DockerImage>()
        val repoList = mutableListOf<String>()
        result.records.forEach {
            val path = it[DOCKER_NODE_PATH] as String
            val imageName =
                path.removeSuffix(SLASH).replaceAfterLast(SLASH, EMPTY).removeSuffix(SLASH).removePrefix(SLASH)
            val lastModifiedBy = it[LAST_MODIFIED_BY] as String
            val lastModifiedDate = it[LAST_MODIFIED_DATE] as String
            val downLoadCount = 0L
            data.add(DockerImage(imageName, lastModifiedBy, lastModifiedDate, downLoadCount, EMPTY, EMPTY))
            repoList.add(imageName)
        }
        val repoInfo = mutableListOf<DockerImage>()
        repoList.distinct().forEach { its ->
            var downloadCount = 0L
            var lastModifiedBy = EMPTY
            var lastModifiedDate = EMPTY
            data.forEach {
                if (its == it.name) {
                    downloadCount += it.downloadCount
                    lastModifiedBy = it.lastModifiedBy
                    lastModifiedDate = it.lastModifiedDate
                }
            }
            repoInfo.add(DockerImage(its, lastModifiedBy, lastModifiedDate, downloadCount, EMPTY, EMPTY))
        }
        return repoInfo
    }

    // get repo tag list
    fun getRepoTagList(context: RequestContext, pageNumber: Int, pageSize: Int, version: String?): List<DockerTag> {
        with(context) {
            var queryModel = NodeQueryBuilder().select(
                DOCKER_NODE_FULL_PATH,
                DOCKER_NODE_PATH,
                DOCKER_NODE_SIZE,
                DOCKER_CREATE_BY,
                LAST_MODIFIED_BY,
                LAST_MODIFIED_DATE,
                STAGE_TAG
            ).sortByAsc(DOCKER_NODE_FULL_PATH).page(pageNumber, pageSize).projectId(projectId).repoName(repoName)
            queryModel = if (version == null) {
                queryModel.name(DOCKER_MANIFEST).path("/$artifactName/", OperationType.PREFIX)
            } else {
                queryModel.name(DOCKER_MANIFEST).path("/$artifactName/*$version*", OperationType.MATCH)
            }

            val result = nodeClient.search(queryModel.build()).data ?: run {
                logger.warn("find artifacts failed: [$projectId, $repoName] found no node")
                return emptyList()
            }
            val data = mutableListOf<DockerTag>()
            result.records.forEach {
                val path = it[DOCKER_NODE_PATH] as String
                val tag = path.removePrefix("/$artifactName/").removeSuffix(SLASH)
                val lastModifiedBy = it[LAST_MODIFIED_BY] as String
                val lastModifiedDate = it[LAST_MODIFIED_DATE] as String
                val size = it[DOCKER_NODE_SIZE] as Int
                val downLoadCount = 0L
                val registryUrl = "$domain/$projectId/$repoName/$artifactName:$tag"
                data.add(DockerTag(tag, EMPTY, size, lastModifiedBy, lastModifiedDate, downLoadCount, registryUrl))
            }
            return data
        }
    }

    fun getRepoTagCount(context: RequestContext, tag: String?): Long {
        with(context) {
            var queryModel = NodeQueryBuilder().select(
                DOCKER_NODE_FULL_PATH,
                DOCKER_NODE_PATH,
                DOCKER_NODE_SIZE,
                DOCKER_CREATE_BY,
                LAST_MODIFIED_BY,
                LAST_MODIFIED_DATE,
                STAGE_TAG
            ).sortByAsc(DOCKER_NODE_FULL_PATH).projectId(projectId).repoName(repoName)
            queryModel = if (tag == null) {
                queryModel.name(DOCKER_MANIFEST).path("/$artifactName/", OperationType.PREFIX)
            } else {
                queryModel.name(DOCKER_MANIFEST).path("/$artifactName/*$tag*", OperationType.MATCH)
            }

            val result = nodeClient.search(queryModel.build()).data ?: run {
                logger.warn("find artifacts failed: [$projectId, $repoName] found no node")
                return 0L
            }
            return result.totalRecords
        }
    }

    // get blob list by digest
    fun getBlobListByDigest(projectId: String, repoName: String, digestName: String): List<Map<String, Any?>>? {
        val queryModel = NodeQueryBuilder().select(DOCKER_NODE_PATH, DOCKER_NODE_SIZE)
            .sortByAsc(DOCKER_NODE_PATH)
            .projectId(projectId)
            .repoName(repoName)
            .name(digestName)
            .page(DOCKER_SEARCH_INDEX, DOCKER_SEARCH_LIMIT)
        val result = nodeClient.search(queryModel.build()).data ?: run {
            logger.warn("find artifacts failed: [$projectId, $repoName, $digestName]  found no node")
            return null
        }
        return result.records
    }

    // to delete docker image tag
    fun deleteByTag(context: RequestContext, tag: String): Boolean {
        with(context) {
            val fullPath = "/$artifactName/$tag/$DOCKER_MANIFEST"
            val deleteNodeRequest = NodeDeleteRequest(projectId, repoName, fullPath, userId)
            return nodeClient.deleteNode(deleteNodeRequest).isOk()
        }
    }

    companion object {
        private const val DOCKER_SEARCH_INDEX = 0
        private const val DOCKER_SEARCH_LIMIT = 9999999
        private const val DOCKER_SEARCH_LIMIT_SMALL = 10
        private val logger = LoggerFactory.getLogger(DockerArtifactRepo::class.java)
    }
}
