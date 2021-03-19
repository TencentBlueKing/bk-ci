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

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.listener.event.metadata.MetadataDeletedEvent
import com.tencent.bkrepo.repository.listener.event.metadata.MetadataSavedEvent
import com.tencent.bkrepo.repository.model.TMetadata
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.pojo.metadata.MetadataDeleteRequest
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import com.tencent.bkrepo.repository.service.MetadataService
import com.tencent.bkrepo.repository.service.RepositoryService
import com.tencent.bkrepo.repository.util.NodeUtils.formatFullPath
import com.tencent.bkrepo.repository.util.QueryHelper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 元数据服务实现类
 */
@Service
class MetadataServiceImpl : AbstractService(), MetadataService {

    @Autowired
    private lateinit var repositoryService: RepositoryService

    @Autowired
    private lateinit var nodeDao: NodeDao

    override fun query(projectId: String, repoName: String, fullPath: String): Map<String, String> {
        repositoryService.checkRepository(projectId, repoName)
        return convert(nodeDao.findOne(QueryHelper.nodeQuery(projectId, repoName, fullPath))?.metadata)
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun save(request: MetadataSaveRequest) {
        request.apply {
            if (!metadata.isNullOrEmpty()) {
                repositoryService.checkRepository(projectId, repoName)
                val fullPath = formatFullPath(fullPath)
                nodeDao.findOne(QueryHelper.nodeQuery(projectId, repoName, fullPath))?.let { node ->
                    val originalMetadata = convert(node.metadata).toMutableMap()
                    metadata!!.forEach { (key, value) -> originalMetadata[key] = value }
                    node.metadata = convert(originalMetadata)
                    nodeDao.save(node)
                } ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, fullPath)
            } else {
                logger.info("Metadata key list is empty, skip saving[$this]")
                return
            }
        }.also {
            publishEvent(MetadataSavedEvent(it))
        }.also {
            logger.info("Save metadata [$it] success.")
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun delete(request: MetadataDeleteRequest) {
        request.apply {
            if (keyList.isNotEmpty()) {
                val fullPath = formatFullPath(request.fullPath)
                repositoryService.checkRepository(projectId, repoName)
                val query = QueryHelper.nodeQuery(projectId, repoName, fullPath)
                val update = Update().pull(
                    TNode::metadata.name,
                    Query.query(Criteria.where(TMetadata::key.name).`in`(keyList))
                )
                nodeDao.updateMulti(query, update)
            } else {
                logger.info("Metadata key list is empty, skip deleting[$this]")
                return
            }
        }.also {
            publishEvent(MetadataDeletedEvent(it))
        }.also {
            logger.info("Delete metadata [$it] success.")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MetadataServiceImpl::class.java)

        fun convert(metadataMap: Map<String, String>?): List<TMetadata> {
            return metadataMap?.filter { it.key.isNotBlank() }?.map { TMetadata(it.key, it.value) }.orEmpty()
        }

        fun convert(metadataList: List<TMetadata>?): Map<String, String> {
            return metadataList?.map { it.key to it.value }?.toMap().orEmpty()
        }

        fun convertOrNull(metadataList: List<TMetadata>?): Map<String, String>? {
            return metadataList?.map { it.key to it.value }?.toMap()
        }
    }
}
