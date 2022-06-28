package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.process.engine.dao.WebhookBuildParameterDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WebhookBuildParameterService @Autowired constructor(
    private val dslContext: DSLContext,
    private val webhookBuildParameterDao: WebhookBuildParameterDao
) {

    fun save(
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildParameters: List<BuildParameters>
    ) {
        webhookBuildParameterDao.save(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            buildParameters = JsonUtil.toJson(buildParameters)
        )
    }

    fun getBuildParameters(buildId: String): List<BuildParameters>? {
        val record = webhookBuildParameterDao.get(
            dslContext = dslContext,
            buildId = buildId
        )
        return record?.buildParameters?.let { JsonUtil.to(it, object : TypeReference<List<BuildParameters>>() {}) }
    }
}
