package com.tencent.devops.project.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.project.tables.TProjectLabel
import com.tencent.devops.model.project.tables.TProjectLabelRel
import com.tencent.devops.model.project.tables.records.TProjectLabelRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ProjectLabelDao {

    fun add(dslContext: DSLContext, labelName: String) {
        with(TProjectLabel.T_PROJECT_LABEL) {
            dslContext.insertInto(this,
                    ID,
                    LABEL_NAME
            )
                    .values(UUIDUtil.generate(),
                            labelName
                    )
                    .execute()
        }
    }

    fun countByName(dslContext: DSLContext, labelName: String): Int {
        with(TProjectLabel.T_PROJECT_LABEL) {
            return dslContext.selectCount().from(this).where(LABEL_NAME.eq(labelName)).fetchOne(0, Int::class.java)
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TProjectLabel.T_PROJECT_LABEL) {
            dslContext.deleteFrom(this)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun getProjectLabel(dslContext: DSLContext, id: String): TProjectLabelRecord? {
        return with(TProjectLabel.T_PROJECT_LABEL) {
            dslContext.selectFrom(this)
                    .where(ID.eq(id))
                    .fetchOne()
        }
    }

    fun getAllProjectLabel(dslContext: DSLContext): Result<TProjectLabelRecord>? {
        return with(TProjectLabel.T_PROJECT_LABEL) {
            dslContext.selectFrom(this)
                    .orderBy(CREATE_TIME.desc())
                    .fetch()
        }
    }

    fun getProjectLabelByProjectId(dslContext: DSLContext, projectId: String): Result<TProjectLabelRecord>? {
        val a = TProjectLabel.T_PROJECT_LABEL.`as`("a")
        val b = TProjectLabelRel.T_PROJECT_LABEL_REL.`as`("b")
        return dslContext.selectFrom(a)
                    .where(a.ID.`in`(dslContext.select(b.LABEL_ID).from(b).where(b.PROJECT_ID.eq(projectId))))
                    .fetch()
    }

    fun update(dslContext: DSLContext, id: String, labelName: String) {
        with(TProjectLabel.T_PROJECT_LABEL) {
            dslContext.update(this)
                    .set(LABEL_NAME, labelName)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(ID.eq(id))
                    .execute()
        }
    }
}