package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TFeatureSwitch
import com.tencent.devops.model.remotedev.tables.records.TFeatureSwitchRecord
import com.tencent.devops.remotedev.pojo.FeatureSwitch
import com.tencent.devops.remotedev.pojo.FeatureSwitchType
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository

@Repository
class FeatureSwitchDao {

    fun create(
        dslContext: DSLContext,
        featureSwitch: FeatureSwitch,
        operator: String
    ): Long {
        with(TFeatureSwitch.T_FEATURE_SWITCH) {
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                USER_ID,
                WORKSPACE_NAME,
                FEATURE_TYPE,
                ENABLED,
                CREATOR,
                UPDATER
            ).values(
                featureSwitch.projectId,
                featureSwitch.userId,
                featureSwitch.workspaceName,
                featureSwitch.featureType.name,
                featureSwitch.enabled,
                operator,
                operator
            ).returning(ID)
                .fetchOne()!!.id
        }
    }

    fun update(
        dslContext: DSLContext,
        id: Long,
        enabled: Boolean,
        operator: String
    ): Boolean {
        with(TFeatureSwitch.T_FEATURE_SWITCH) {
            return dslContext.update(this)
                .set(ENABLED, enabled)
                .set(UPDATER, operator)
                .where(ID.eq(id))
                .execute() == 1
        }
    }

    fun delete(
        dslContext: DSLContext,
        id: Long
    ): Boolean {
        with(TFeatureSwitch.T_FEATURE_SWITCH) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .limit(1)
                .execute() == 1
        }
    }

    fun getById(
        dslContext: DSLContext,
        id: Long
    ): FeatureSwitch? {
        with(TFeatureSwitch.T_FEATURE_SWITCH) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchAny(MAPPER)
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String?,
        userId: String?,
        workspaceName: String?,
        featureType: FeatureSwitchType?
    ): List<FeatureSwitch> {
        with(TFeatureSwitch.T_FEATURE_SWITCH) {
            val query = dslContext.selectFrom(this).where(
                org.jooq.impl.DSL.trueCondition()
            )
            if (!projectId.isNullOrBlank()) {
                query.and(PROJECT_ID.eq(projectId))
            }
            if (!userId.isNullOrBlank()) {
                query.and(USER_ID.eq(userId))
            }
            if (!workspaceName.isNullOrBlank()) {
                query.and(WORKSPACE_NAME.eq(workspaceName))
            }
            if (featureType != null) {
                query.and(FEATURE_TYPE.eq(featureType.name))
            }
            return query.orderBy(UPDATE_TIME.desc()).fetch(MAPPER)
        }
    }

    fun isEnabled(
        dslContext: DSLContext,
        projectId: String,
        userId: String,
        workspaceName: String,
        featureType: FeatureSwitchType
    ): Boolean {
        with(TFeatureSwitch.T_FEATURE_SWITCH) {
            return dslContext.selectFrom(this)
                .where(FEATURE_TYPE.eq(featureType.name))
                .and(ENABLED.eq(true))
                .and(
                    PROJECT_ID.eq(projectId)
                        .or(PROJECT_ID.eq(""))
                )
                .and(
                    USER_ID.eq(userId)
                        .or(USER_ID.eq(""))
                )
                .and(
                    WORKSPACE_NAME.eq(workspaceName)
                        .or(WORKSPACE_NAME.eq(""))
                )
                .limit(1)
                .fetchAny() != null
        }
    }

    class TFeatureSwitchRecordMapper :
        RecordMapper<TFeatureSwitchRecord, FeatureSwitch> {
        override fun map(record: TFeatureSwitchRecord?): FeatureSwitch? {
            return record?.run {
                FeatureSwitch(
                    id = id,
                    projectId = projectId,
                    userId = userId,
                    workspaceName = workspaceName,
                    featureType = FeatureSwitchType.parse(featureType),
                    enabled = enabled,
                    creator = creator,
                    updater = updater,
                    createTime = createTime,
                    updateTime = updateTime
                )
            }
        }
    }

    companion object {
        private val MAPPER = TFeatureSwitchRecordMapper()
    }
}
