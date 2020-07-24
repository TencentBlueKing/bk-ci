package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.constant.DEFAULT_DOCKER_CLUSTER
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.dispatch.dao.PipelineDockerDevClusterDao
import com.tencent.devops.dispatch.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.pojo.DockerDevCluster
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DockerDevClusterService {

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var pipelineDockerDevClusterDao: PipelineDockerDevClusterDao

    @Autowired
    private lateinit var pipelineDockerIPInfoDao: PipelineDockerIPInfoDao

    fun create(userId: String, dockerDevCluster: DockerDevCluster): Boolean {
        dockerDevCluster.clusterId = UUIDUtil.generate()
        logger.info("[$dockerDevCluster creating by $userId]")
        pipelineDockerDevClusterDao.create(dslContext, dockerDevCluster, userId)
        return true
    }

    fun update(userId: String, dockerDevCluster: DockerDevCluster): Boolean {
        logger.info("[$dockerDevCluster updating by $userId]")
        pipelineDockerDevClusterDao.update(dslContext, dockerDevCluster, userId)
        return true
    }

    fun list(
        userId: String,
        page: Int?,
        pageSize: Int?,
        includeDisable: Boolean?
    ): Page<DockerDevCluster> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 10
        val enable = if (includeDisable == true) null else true
        val clusterList = pipelineDockerDevClusterDao.list(
            dslContext, pageNotNull, pageSizeNotNull, enable
        )
        val count = pipelineDockerDevClusterDao.count(dslContext, enable)

        if (clusterList.size == 0 || count == 0L) {
            return Page(
                pageNotNull, pageSizeNotNull, 0, emptyList()
            )
        }
        val list = clusterList.map(pipelineDockerDevClusterDao::convert)
        return Page(pageNotNull, pageSizeNotNull, count, list)
    }

    fun delete(userId: String, clusterId: String): Boolean {
        logger.warn("[$clusterId deleting by $userId]")
        dslContext.transaction { c ->
            val context = DSL.using(c)
            pipelineDockerIPInfoDao.updateClusterIdByClusterId(
                context, clusterId, DEFAULT_DOCKER_CLUSTER
            )
            pipelineDockerDevClusterDao.delete(context, clusterId)
        }
        return true
    }

    private val logger = LoggerFactory.getLogger(this::class.qualifiedName)
}