package com.tencent.devops.environment.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.NodeTagDao
import com.tencent.devops.environment.dao.NodeTagKeyDao
import com.tencent.devops.environment.dao.NodeTagValueDao
import com.tencent.devops.environment.pojo.NodeTag
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class NodeTagService @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeTagDao: NodeTagDao,
    private val nodeTagKeyDao: NodeTagKeyDao,
    private val nodeTagValueDao: NodeTagValueDao
) {
    fun createTag(
        projectId: String,
        tagKey: String,
        tagValues: List<String>,
        allowMulValue: Boolean?
    ) {
        if (nodeTagKeyDao.fetchNodeKey(dslContext, projectId, tagKey) != null) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_TAG_EXIST)
        }
        nodeTagDao.createTag(
            dslContext = dslContext,
            projectId = projectId,
            tagName = tagKey,
            tagValue = tagValues,
            allowMulVal = allowMulValue ?: false
        )
    }

    fun fetchNodeTagAndCount(
        projectId: String
    ): List<NodeTag> {
        return nodeTagDao.fetchTagAndNodeCount(dslContext, projectId)
    }
}