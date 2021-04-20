/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.repository.service.metadata.impl

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.path.PathUtils.normalizeFullPath
import com.tencent.bkrepo.common.service.util.SpringContextUtils.Companion.publishEvent
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.listener.event.metadata.MetadataDeletedEvent
import com.tencent.bkrepo.repository.listener.event.metadata.MetadataSavedEvent
import com.tencent.bkrepo.repository.model.TMetadata
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.pojo.metadata.MetadataDeleteRequest
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import com.tencent.bkrepo.repository.service.metadata.MetadataService
import com.tencent.bkrepo.repository.util.MetadataUtils
import com.tencent.bkrepo.repository.util.NodeQueryHelper
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 元数据服务实现类
 */
@Service
class MetadataServiceImpl(
    private val nodeDao: NodeDao
) : MetadataService {

    override fun listMetadata(projectId: String, repoName: String, fullPath: String): Map<String, Any> {
        return MetadataUtils.toMap(nodeDao.findOne(NodeQueryHelper.nodeQuery(projectId, repoName, fullPath))?.metadata)
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun saveMetadata(request: MetadataSaveRequest) {
        with(request) {
            if (metadata.isNullOrEmpty()) {
                logger.info("Metadata is empty, skip saving")
                return
            }
            val fullPath = normalizeFullPath(fullPath)
            val node = nodeDao.findNode(projectId, repoName, fullPath)
                ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, fullPath)
            val originalMetadata = MetadataUtils.toMap(node.metadata).toMutableMap()
            metadata!!.forEach { (key, value) -> originalMetadata[key] = value }
            node.metadata = MetadataUtils.fromMap(originalMetadata)
            nodeDao.save(node)
            publishEvent(MetadataSavedEvent(request))
            logger.info("Save metadata[$metadata] on node[/$projectId/$repoName$fullPath] success.")
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun deleteMetadata(request: MetadataDeleteRequest) {
        with(request) {
            if (keyList.isNullOrEmpty()) {
                logger.info("Metadata key list is empty, skip deleting")
                return
            }
            val fullPath = normalizeFullPath(request.fullPath)
            val query = NodeQueryHelper.nodeQuery(projectId, repoName, fullPath)
            val update = Update().pull(
                TNode::metadata.name,
                Query.query(where(TMetadata::key).inValues(keyList))
            )
            nodeDao.updateMulti(query, update)
            publishEvent(MetadataDeletedEvent(this))
            logger.info("Delete metadata[$keyList] on node[/$projectId/$repoName$fullPath] success.")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MetadataServiceImpl::class.java)
    }
}
