package com.tencent.devops.project.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.project.tables.TProjectLabelRel
import com.tencent.devops.model.project.tables.records.TProjectLabelRelRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ProjectLabelRelDao {

    fun getLabelsByProjectId(dslContext: DSLContext, projectId: String): Result<TProjectLabelRelRecord>? {
        with(TProjectLabelRel.T_PROJECT_LABEL_REL) {
            return dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .fetch()
        }
    }

    fun add(dslContext: DSLContext, id: String, labelId: String, projectId: String) {
        with(TProjectLabelRel.T_PROJECT_LABEL_REL) {
            dslContext.insertInto(this,
                    ID,
                    LABEL_ID,
                    PROJECT_ID
            )
                    .values(id,
                            labelId,
                            projectId
                    )
                    .execute()
        }
    }

    fun batchAdd(dslContext: DSLContext, projectId: String, labelIdList: List<String>) {
        with(TProjectLabelRel.T_PROJECT_LABEL_REL) {
            val bachExceute = dslContext.batch("INSERT INTO T_PROJECT_LABEL_REL(ID, LABEL_ID, PROJECT_ID) VALUES (?,?,?)")
            for (item in labelIdList) {
                bachExceute.bind(UUIDUtil.generate(), item, projectId)
            }
            bachExceute.execute()
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TProjectLabelRel.T_PROJECT_LABEL_REL) {
            dslContext.deleteFrom(this)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun deleteByProjectId(dslContext: DSLContext, projectId: String) {
        with(TProjectLabelRel.T_PROJECT_LABEL_REL) {
            dslContext.deleteFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .execute()
        }
    }
}