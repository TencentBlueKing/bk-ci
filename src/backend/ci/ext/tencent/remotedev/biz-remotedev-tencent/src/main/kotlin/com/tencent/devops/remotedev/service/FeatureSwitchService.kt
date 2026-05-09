package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.dao.FeatureSwitchDao
import com.tencent.devops.remotedev.pojo.FeatureSwitch
import com.tencent.devops.remotedev.pojo.FeatureSwitchType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class FeatureSwitchService @Autowired constructor(
    private val dslContext: DSLContext,
    private val featureSwitchDao: FeatureSwitchDao
) {

    companion object {
        private val logger =
            LoggerFactory.getLogger(FeatureSwitchService::class.java)
    }

    fun create(
        operator: String,
        featureSwitch: FeatureSwitch
    ): Long {
        logger.info(
            "operator($operator) creates feature switch: " +
                "projectId=${featureSwitch.projectId}, " +
                "userId=${featureSwitch.userId}, " +
                "workspaceName=${featureSwitch.workspaceName}, " +
                "featureType=${featureSwitch.featureType}, " +
                "enabled=${featureSwitch.enabled}"
        )
        return featureSwitchDao.create(
            dslContext = dslContext,
            featureSwitch = featureSwitch,
            operator = operator
        )
    }

    fun update(
        operator: String,
        id: Long,
        enabled: Boolean
    ): Boolean {
        logger.info(
            "operator($operator) updates feature switch " +
                "id=$id, enabled=$enabled"
        )
        return featureSwitchDao.update(
            dslContext = dslContext,
            id = id,
            enabled = enabled,
            operator = operator
        )
    }

    fun delete(operator: String, id: Long): Boolean {
        logger.info("operator($operator) deletes feature switch id=$id")
        return featureSwitchDao.delete(dslContext = dslContext, id = id)
    }

    fun getById(id: Long): FeatureSwitch? {
        return featureSwitchDao.getById(dslContext = dslContext, id = id)
    }

    fun list(
        projectId: String?,
        userId: String?,
        workspaceName: String?,
        featureType: FeatureSwitchType?
    ): List<FeatureSwitch> {
        return featureSwitchDao.list(
            dslContext = dslContext,
            projectId = projectId,
            userId = userId,
            workspaceName = workspaceName,
            featureType = featureType
        )
    }

    fun isEnabled(
        projectId: String,
        userId: String,
        workspaceName: String,
        featureType: FeatureSwitchType
    ): Boolean {
        return featureSwitchDao.isEnabled(
            dslContext = dslContext,
            projectId = projectId,
            userId = userId,
            workspaceName = workspaceName,
            featureType = featureType
        )
    }
}
