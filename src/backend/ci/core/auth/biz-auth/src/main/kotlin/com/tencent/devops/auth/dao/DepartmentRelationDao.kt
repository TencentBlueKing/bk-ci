package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TDepartmentRelation
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class DepartmentRelationDao {
    fun create(
        dslContext: DSLContext,
        parentId: Int,
        childrenId: Int,
        depth: Int
    ) {
        with(TDepartmentRelation.T_DEPARTMENT_RELATION) {
            dslContext.insertInto(
                this,
                PARENT_ID,
                CHILDREN_ID,
                DEPTH
            ).values(
                parentId,
                childrenId,
                depth
            ).onDuplicateKeyIgnore()
                .execute()
        }
    }
}
