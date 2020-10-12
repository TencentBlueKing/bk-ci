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

package com.tencent.bkrepo.repository.service.impl

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.listener.event.node.NodeCopiedEvent
import com.tencent.bkrepo.repository.listener.event.node.NodeCreatedEvent
import com.tencent.bkrepo.repository.listener.event.node.NodeMovedEvent
import com.tencent.bkrepo.repository.listener.event.node.NodeRenamedEvent
import com.tencent.bkrepo.repository.listener.event.node.NodeUpdatedEvent
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.model.TRepository
import com.tencent.bkrepo.repository.pojo.node.CrossRepoNodeRequest
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import com.tencent.bkrepo.repository.pojo.node.service.NodeCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeRenameRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeUpdateRequest
import com.tencent.bkrepo.repository.service.FileReferenceService
import com.tencent.bkrepo.repository.service.NodeService
import com.tencent.bkrepo.repository.service.RepositoryService
import com.tencent.bkrepo.repository.service.StorageCredentialService
import com.tencent.bkrepo.repository.util.NodeUtils
import com.tencent.bkrepo.repository.util.NodeUtils.combineFullPath
import com.tencent.bkrepo.repository.util.NodeUtils.combinePath
import com.tencent.bkrepo.repository.util.NodeUtils.escapeRegex
import com.tencent.bkrepo.repository.util.NodeUtils.formatFullPath
import com.tencent.bkrepo.repository.util.NodeUtils.formatPath
import com.tencent.bkrepo.repository.util.NodeUtils.getName
import com.tencent.bkrepo.repository.util.NodeUtils.getParentPath
import com.tencent.bkrepo.repository.util.NodeUtils.isRootPath
import com.tencent.bkrepo.repository.util.NodeUtils.parseFullPath
import com.tencent.bkrepo.repository.util.QueryHelper.nodeDeleteUpdate
import com.tencent.bkrepo.repository.util.QueryHelper.nodeExpireDateUpdate
import com.tencent.bkrepo.repository.util.QueryHelper.nodeListCriteria
import com.tencent.bkrepo.repository.util.QueryHelper.nodeListQuery
import com.tencent.bkrepo.repository.util.QueryHelper.nodePathUpdate
import com.tencent.bkrepo.repository.util.QueryHelper.nodeQuery
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 节点服务实现类
 */
@Service
class NodeServiceImpl : AbstractService(), NodeService {

    @Autowired
    private lateinit var nodeDao: NodeDao

    @Autowired
    private lateinit var repositoryService: RepositoryService

    @Autowired
    private lateinit var fileReferenceService: FileReferenceService

    @Autowired
    private lateinit var storageCredentialService: StorageCredentialService

    @Autowired
    private lateinit var storageService: StorageService

    /**
     * 查询节点详情
     */
    override fun detail(projectId: String, repoName: String, fullPath: String, repoType: String?): NodeDetail? {
        repositoryService.checkRepository(projectId, repoName, repoType)
        val formattedFullPath = formatFullPath(fullPath)
        return convertToDetail(queryNode(projectId, repoName, formattedFullPath))
    }

    /**
     * 计算文件或者文件夹大小
     */
    override fun computeSize(projectId: String, repoName: String, fullPath: String): NodeSizeInfo {
        repositoryService.checkRepository(projectId, repoName)

        val formattedFullPath = formatFullPath(fullPath)
        val node = queryNode(projectId, repoName, formattedFullPath)
            ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, formattedFullPath)
        // 节点为文件直接返回
        if (!node.folder) {
            return NodeSizeInfo(subNodeCount = 0, size = node.size)
        }

        val criteria =
            nodeListCriteria(projectId, repoName, formatPath(formattedFullPath), includeFolder = true, deep = true)
        val count = nodeDao.count(Query(criteria))

        val aggregation = Aggregation.newAggregation(
            Aggregation.match(criteria),
            Aggregation.group().sum(TNode::size.name).`as`(NodeSizeInfo::size.name)
        )
        val aggregateResult = nodeDao.aggregate(aggregation, HashMap::class.java)
        val size = if (aggregateResult.mappedResults.size > 0) {
            aggregateResult.mappedResults[0][NodeSizeInfo::size.name] as? Long ?: 0
        } else 0

        return NodeSizeInfo(subNodeCount = count, size = size)
    }

    /**
     * 查询文件节点数量
     */
    override fun countFileNode(projectId: String, repoName: String, path: String): Long {
        repositoryService.checkRepository(projectId, repoName)
        val formattedPath = formatPath(path)
        val query = nodeListQuery(projectId, repoName, formattedPath, includeFolder = false, includeMetadata = false, deep = true)
        return nodeDao.count(query)
    }

    /**
     * 列表查询节点
     */
    override fun list(
        projectId: String,
        repoName: String,
        path: String,
        includeFolder: Boolean,
        includeMetadata: Boolean,
        deep: Boolean
    ): List<NodeInfo> {
        repositoryService.checkRepository(projectId, repoName)
        val query = nodeListQuery(projectId, repoName, path, includeFolder, includeMetadata, deep)
        if (nodeDao.count(query) >= LIST_THRESHOLD) {
            throw ErrorCodeException(ArtifactMessageCode.NODE_LIST_TOO_LARGE)
        }
        return nodeDao.find(query).map { convert(it)!! }
    }

    /**
     * 分页查询节点
     */
    override fun page(
        projectId: String,
        repoName: String,
        path: String,
        page: Int,
        size: Int,
        includeFolder: Boolean,
        includeMetadata: Boolean,
        deep: Boolean
    ): Page<NodeInfo> {
        page.takeIf { it >= 0 } ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "page")
        size.takeIf { it in 0..LIST_THRESHOLD } ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "size")
        repositoryService.checkRepository(projectId, repoName)

        val query = nodeListQuery(projectId, repoName, path, includeFolder, includeMetadata, deep)
        val count = nodeDao.count(query)
        val pageNumber = if (page <= 0) 1 else page
        val listData = nodeDao.find(query.with(PageRequest.of(pageNumber - 1, size))).map { convert(it)!! }

        return Page(page, size, count, listData)
    }

    /**
     * 判断节点是否存在
     */
    override fun exist(projectId: String, repoName: String, fullPath: String): Boolean {
        val formattedPath = formatFullPath(fullPath)
        val query = nodeQuery(projectId, repoName, formattedPath)

        return nodeDao.exists(query)
    }

    /**
     * 判断节点列表是否存在
     */
    override fun listExistFullPath(projectId: String, repoName: String, fullPathList: List<String>): List<String> {
        val formatFullPathList = fullPathList.map { formatFullPath(it) }
        val query = nodeListQuery(projectId, repoName, formatFullPathList)

        return nodeDao.find(query).map { it.fullPath }
    }

    /**
     * 创建节点，返回id
     */
    @Transactional(rollbackFor = [Throwable::class])
    override fun create(createRequest: NodeCreateRequest): NodeDetail {
        with(createRequest) {
            this.takeIf { folder || !sha256.isNullOrBlank() }
                ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, this::sha256.name)
            this.takeIf { folder || !md5.isNullOrBlank() }
                ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, this::md5.name)
            val fullPath = parseFullPath(fullPath)
            val repo = repositoryService.checkRepository(projectId, repoName)
            // 路径唯一性校验
            queryNode(projectId, repoName, fullPath)?.let {
                if (!overwrite) {
                    throw ErrorCodeException(ArtifactMessageCode.NODE_EXISTED, fullPath)
                } else if (it.folder || this.folder) {
                    throw ErrorCodeException(ArtifactMessageCode.NODE_CONFLICT, fullPath)
                } else {
                    deleteByPath(projectId, repoName, fullPath, operator)
                }
            }
            // 判断父目录是否存在，不存在先创建
            mkdirs(projectId, repoName, getParentPath(fullPath), operator)
            // 创建节点
            val node = TNode(
                folder = folder,
                path = getParentPath(fullPath),
                name = getName(fullPath),
                fullPath = fullPath,
                expireDate = if (folder) null else parseExpireDate(expires),
                size = if (folder) 0 else size ?: 0,
                sha256 = if (folder) null else sha256,
                md5 = if (folder) null else md5,
                projectId = projectId,
                repoName = repoName,
                metadata = MetadataServiceImpl.convert(metadata),
                createdBy = createdBy ?: operator,
                createdDate = createdDate ?: LocalDateTime.now(),
                lastModifiedBy = createdBy ?: operator,
                lastModifiedDate = lastModifiedDate ?: LocalDateTime.now()
            )
            return node.apply { doCreate(this, repo) }
                .also { publishEvent(NodeCreatedEvent(createRequest)) }
                .also { logger.info("Create node [$createRequest] success.") }
                .let { convertToDetail(it)!! }
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun createRootNode(projectId: String, repoName: String, operator: String) {
        val rootNode = TNode(
            projectId = projectId,
            repoName = repoName,
            folder = true,
            path = NodeUtils.FILE_SEPARATOR,
            name = StringPool.EMPTY,
            fullPath = NodeUtils.FILE_SEPARATOR,
            size = 0,
            createdBy = operator,
            createdDate = LocalDateTime.now(),
            lastModifiedBy = operator,
            lastModifiedDate = LocalDateTime.now()
        )
        rootNode.apply { doCreate(this) }.also { logger.info("Create node [$it] success.") }
    }

    /**
     * 重命名文件或者文件夹
     * 重命名过程中出现错误则抛异常，剩下的文件不会再移动
     * 遇到同名文件或者文件夹直接抛异常
     */
    @Transactional(rollbackFor = [Throwable::class])
    override fun rename(renameRequest: NodeRenameRequest) {
        renameRequest.apply {
            val fullPath = formatFullPath(this.fullPath)
            val newFullPath = formatFullPath(this.newFullPath)

            repositoryService.checkRepository(projectId, repoName)
            val node = queryNode(projectId, repoName, fullPath) ?: throw ErrorCodeException(
                ArtifactMessageCode.NODE_NOT_FOUND,
                fullPath
            )
            doRename(node, newFullPath, operator)
        }.also {
            publishEvent(NodeRenamedEvent(it))
        }.also {
            logger.info("Rename node [$it] success.")
        }
    }

    /**
     * 更新节点
     */
    @Transactional(rollbackFor = [Throwable::class])
    override fun update(updateRequest: NodeUpdateRequest) {
        updateRequest.apply {
            val fullPath = formatFullPath(this.fullPath)
            repositoryService.checkRepository(projectId, repoName)
            val node = queryNode(projectId, repoName, fullPath) ?: throw ErrorCodeException(
                ArtifactMessageCode.NODE_NOT_FOUND,
                fullPath
            )
            val selfQuery = nodeQuery(projectId, repoName, node.fullPath)
            val selfUpdate = nodeExpireDateUpdate(parseExpireDate(expires), operator)
            nodeDao.updateFirst(selfQuery, selfUpdate)
        }.also {
            publishEvent(NodeUpdatedEvent(it))
        }.also {
            logger.info("Rename node [$it] success.")
        }
    }

    /**
     * 移动文件或者文件夹
     * 采用fast-failed模式，移动过程中出现错误则抛异常，剩下的文件不会再移动
     * 行为类似linux mv命令
     * mv 文件名 文件名	将源文件名改为目标文件名
     * mv 文件名 目录名	将文件移动到目标目录
     * mv 目录名 目录名	目标目录已存在，将源目录（目录本身及子文件）移动到目标目录；目标目录不存在则改名
     * mv 目录名 文件名	出错
     */
    @Transactional(rollbackFor = [Throwable::class])
    override fun move(moveRequest: NodeMoveRequest) {
        moveOrCopy(moveRequest, moveRequest.operator)
    }

    /**
     * 拷贝文件或者文件夹
     * 采用fast-failed模式，拷贝过程中出现错误则抛异常，剩下的文件不会再拷贝
     * 行为类似linux cp命令
     * cp 文件名 文件名	将源文件拷贝到目标文件
     * cp 文件名 目录名	将文件移动到目标目录下
     * cp 目录名 目录名	cp 目录名 目录名	目标目录已存在，将源目录（目录本身及子文件）拷贝到目标目录；目标目录不存在则将源目录下文件拷贝到目标目录
     * cp 目录名 文件名	出错
     */
    @Transactional(rollbackFor = [Throwable::class])
    override fun copy(copyRequest: NodeCopyRequest) {
        moveOrCopy(copyRequest, copyRequest.operator)
    }

    /**
     * 删除指定节点, 逻辑删除
     */
    @Transactional(rollbackFor = [Throwable::class])
    override fun delete(deleteRequest: NodeDeleteRequest) {
        with(deleteRequest) {
            repositoryService.checkRepository(this.projectId, this.repoName)
            deleteByPath(this.projectId, this.repoName, this.fullPath, this.operator)
        }
    }

    /**
     * 将节点重命名为指定名称
     */
    private fun doRename(node: TNode, newFullPath: String, operator: String) {
        val projectId = node.projectId
        val repoName = node.repoName
        val newPath = getParentPath(newFullPath)
        val newName = getName(newFullPath)

        // 检查新路径是否被占用
        if (exist(projectId, repoName, newFullPath)) {
            logger.warn("Rename node [${node.fullPath}] failed: $newFullPath is exist.")
            throw ErrorCodeException(ArtifactMessageCode.NODE_EXISTED, newFullPath)
        }

        // 如果为文件夹，查询子节点并修改
        if (node.folder) {
            mkdirs(projectId, repoName, newFullPath, operator)
            val newParentPath = formatPath(newFullPath)
            val fullPath = formatPath(node.fullPath)
            val query = nodeListQuery(projectId, repoName, fullPath, includeFolder = true, includeMetadata = false, deep = false)
            nodeDao.find(query).forEach { doRename(it, newParentPath + it.name, operator) }
            // 删除自己
            nodeDao.remove(nodeQuery(projectId, repoName, node.fullPath))
        } else {
            // 修改自己
            val selfQuery = nodeQuery(projectId, repoName, node.fullPath)
            val selfUpdate = nodePathUpdate(newPath, newName, operator)
            nodeDao.updateFirst(selfQuery, selfUpdate)
        }
    }

    /**
     * 根据全路径删除文件或者目录
     */
    override fun deleteByPath(projectId: String, repoName: String, fullPath: String, operator: String, soft: Boolean) {
        val formattedFullPath = formatFullPath(fullPath)
        val formattedPath = formatPath(formattedFullPath)
        val escapedPath = escapeRegex(formattedPath)
        val query = nodeQuery(projectId, repoName)
        query.addCriteria(
            Criteria().orOperator(
                Criteria.where(TNode::fullPath.name).regex("^$escapedPath"),
                Criteria.where(TNode::fullPath.name).`is`(formattedFullPath)
            )
        )
        if (soft) {
            // 软删除
            try {
                nodeDao.updateMulti(query, nodeDeleteUpdate(operator))
            } catch (exception: DuplicateKeyException) {
                logger.warn("Soft delete node[$projectId/$repoName$fullPath] error: [${exception.message}]")
            }
        } else {
            // 硬删除
            nodeDao.remove(query)
        }
        logger.info("Delete node [$projectId/$repoName$fullPath] by [$operator] success.")
    }

    /**
     * 查询节点model
     */
    private fun queryNode(projectId: String, repoName: String, fullPath: String): TNode? {
        val query = nodeQuery(projectId, repoName, formatFullPath(fullPath))
        return nodeDao.findOne(query)
    }

    /**
     * 递归创建目录
     */
    private fun mkdirs(projectId: String, repoName: String, path: String, createdBy: String) {
        if (!exist(projectId, repoName, path)) {
            val parentPath = getParentPath(path)
            val name = getName(path)
            path.takeUnless { isRootPath(it) }?.run { mkdirs(projectId, repoName, parentPath, createdBy) }
            val node = TNode(
                folder = true,
                path = parentPath,
                name = name,
                fullPath = combineFullPath(parentPath, name),
                size = 0,
                expireDate = null,
                metadata = emptyList(),
                projectId = projectId,
                repoName = repoName,
                createdBy = createdBy,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = createdBy,
                lastModifiedDate = LocalDateTime.now()
            )
            doCreate(node, null)
        }
    }

    /**
     * 处理节点操作请求
     */
    private fun moveOrCopy(request: CrossRepoNodeRequest, operator: String) {
        with(request) {
            val srcFullPath = formatFullPath(srcFullPath)
            val destProjectId = request.destProjectId ?: srcProjectId
            val destRepoName = request.destRepoName ?: srcRepoName
            val destFullPath = formatFullPath(request.destFullPath)

            val isSameRepository = srcProjectId == destProjectId && srcRepoName == destRepoName
            // 查询repository
            val srcRepository = repositoryService.checkRepository(srcProjectId, srcRepoName)
            val destRepository = if (!isSameRepository) {
                repositoryService.checkRepository(destProjectId, destRepoName)
            } else srcRepository
            // 查询storageCredentials
            val srcCredentials = srcRepository.credentialsKey?.let {
                storageCredentialService.findByKey(it)
            }
            val destCredentials = if (!isSameRepository) {
                destRepository.credentialsKey?.let { storageCredentialService.findByKey(it) }
            } else srcCredentials
            // 只允许local类型仓库操作
            if (srcRepository.category != RepositoryCategory.LOCAL || destRepository.category != RepositoryCategory.LOCAL) {
                throw ErrorCodeException(CommonMessageCode.OPERATION_UNSUPPORTED)
            }
            val srcNode = queryNode(srcProjectId, srcRepoName, srcFullPath) ?: throw ErrorCodeException(
                ArtifactMessageCode.NODE_NOT_FOUND,
                srcFullPath
            )
            val destNode = queryNode(destProjectId, destRepoName, destFullPath)
            // 同路径，跳过
            if (isSameRepository && srcNode.fullPath == destNode?.fullPath) return
            // src为dest目录下的子节点，跳过
            if (isSameRepository && destNode?.folder == true && srcNode.path == formatPath(destNode.fullPath)) return
            // 目录 ->
            if (srcNode.folder) {
                // 目录 -> 文件: error
                if (destNode?.folder == false) {
                    throw ErrorCodeException(ArtifactMessageCode.NODE_CONFLICT, destFullPath)
                }
                val destRootNodePath = if (destNode == null) {
                    // 目录 -> 不存在的目录
                    val path = getParentPath(destFullPath)
                    val name = getName(destFullPath)
                    // 创建dest父目录
                    mkdirs(destProjectId, destRepoName, path, operator)
                    // 操作节点
                    moveOrCopyNode(srcNode, destRepository, srcCredentials, destCredentials, path, name, request, operator)
                    combinePath(path, name)
                } else {
                    // 目录 -> 存在的目录
                    val path = formatPath(destNode.fullPath)
                    // 操作节点
                    moveOrCopyNode(srcNode, destRepository, srcCredentials, destCredentials, path, srcNode.name, request, operator)
                    combinePath(path, srcNode.name)
                }
                val srcRootNodePath = formatPath(srcNode.fullPath)
                val query = nodeListQuery(
                    srcNode.projectId,
                    srcNode.repoName,
                    srcRootNodePath,
                    includeFolder = true,
                    includeMetadata = false,
                    deep = true
                )
                // 目录下的节点 -> 创建好的目录
                nodeDao.find(query).forEach {
                    val destPath = it.path.replaceFirst(srcRootNodePath, destRootNodePath)
                    moveOrCopyNode(it, destRepository, srcCredentials, destCredentials, destPath, null, request, operator)
                }
            } else {
                // 文件 ->
                val destPath = if (destNode?.folder == true) formatPath(destNode.fullPath) else getParentPath(destFullPath)
                val destName = if (destNode?.folder == true) srcNode.name else getName(destFullPath)
                // 创建dest父目录
                mkdirs(destProjectId, destRepoName, destPath, operator)
                moveOrCopyNode(srcNode, destRepository, srcCredentials, destCredentials, destPath, destName, request, operator)
            }
            // event
            if (request is NodeMoveRequest) {
                publishEvent(NodeMovedEvent(request))
            } else if (request is NodeCopyRequest) {
                publishEvent(NodeCopiedEvent(request))
            }
            logger.info("[${request.getOperateName()}] node success: [$this]")
        }
    }

    /**
     * 移动/拷贝节点
     */
    private fun moveOrCopyNode(
        srcNode: TNode,
        destRepository: TRepository,
        srcStorageCredentials: StorageCredentials?,
        destStorageCredentials: StorageCredentials?,
        destPath: String,
        nodeName: String?,
        request: CrossRepoNodeRequest,
        operator: String
    ) {
        // 计算destName
        val destName = nodeName ?: srcNode.name
        val destFullPath = combineFullPath(destPath, destName)
        // 冲突检查
        val existNode = queryNode(destRepository.projectId, destRepository.name, destFullPath)
        // 目录 -> 目录: 跳过
        if (srcNode.folder && existNode?.folder == true) return
        // 目录 -> 文件: 出错
        if (srcNode.folder && existNode?.folder == false) {
            throw ErrorCodeException(ArtifactMessageCode.NODE_CONFLICT, existNode.fullPath)
        }
        // 文件 -> 文件 & 不允许覆盖: 出错
        if (!srcNode.folder && existNode?.folder == false && !request.overwrite) {
            throw ErrorCodeException(ArtifactMessageCode.NODE_CONFLICT, existNode.fullPath)
        }

        // copy目标节点
        val destNode = srcNode.copy(
            id = null,
            projectId = destRepository.projectId,
            repoName = destRepository.name,
            path = destPath,
            name = destName,
            fullPath = destFullPath,
            lastModifiedBy = operator,
            lastModifiedDate = LocalDateTime.now()
        )
        // move操作，create信息保留
        if (request is NodeMoveRequest) {
            destNode.createdBy = operator
            destNode.createdDate = LocalDateTime.now()
        }
        // 文件 -> 文件 & 允许覆盖: 删除old
        if (!srcNode.folder && existNode?.folder == false && request.overwrite) {
            val query = nodeQuery(existNode.projectId, existNode.repoName, existNode.fullPath)
            val update = nodeDeleteUpdate(operator)
            nodeDao.updateFirst(query, update)
        }
        // 文件 & 跨存储
        if (!srcNode.folder && srcStorageCredentials != destStorageCredentials) {
            storageService.copy(srcNode.sha256!!, srcStorageCredentials, destStorageCredentials)
        }
        // 创建dest节点
        doCreate(destNode, destRepository)
        // move操作，创建dest节点后，还需要删除src节点
        // 因为分表所以不能直接更新src节点，必须创建新的并删除旧的
        if (request is NodeMoveRequest) {
            val query = nodeQuery(srcNode.projectId, srcNode.repoName, srcNode.fullPath)
            val update = nodeDeleteUpdate(operator)
            nodeDao.updateFirst(query, update)
        }
    }

    private fun doCreate(node: TNode, repository: TRepository? = null): TNode {
        try {
            nodeDao.insert(node)
            node.takeUnless { it.folder }?.run { fileReferenceService.increment(this, repository) }
        } catch (exception: DuplicateKeyException) {
            logger.warn("Insert node[$node] error: [${exception.message}]")
        }

        return node
    }

    /**
     * 根据有效天数，计算到期时间
     */
    private fun parseExpireDate(expireDays: Long?): LocalDateTime? {
        return expireDays?.takeIf { it > 0 }?.run { LocalDateTime.now().plusDays(this) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NodeServiceImpl::class.java)

        private const val LIST_THRESHOLD: Long = 100000L

        private fun convert(tNode: TNode?): NodeInfo? {
            return tNode?.let {
                NodeInfo(
                    createdBy = it.createdBy,
                    createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    lastModifiedBy = it.lastModifiedBy,
                    lastModifiedDate = it.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    folder = it.folder,
                    path = it.path,
                    name = it.name,
                    fullPath = it.fullPath,
                    size = it.size,
                    sha256 = it.sha256,
                    md5 = it.md5,
                    metadata = MetadataServiceImpl.convertOrNull(it.metadata),
                    repoName = it.repoName,
                    projectId = it.projectId
                )
            }
        }

        private fun convertToDetail(tNode: TNode?): NodeDetail? {
            return convert(tNode)?.let {
                NodeDetail(
                    nodeInfo = it,
                    metadata = it.metadata.orEmpty()
                )
            }
        }
    }
}
