package com.tencent.devops.environment.dao

import com.tencent.devops.model.environment.tables.TBcsCluster
import com.tencent.devops.model.environment.tables.records.TBcsClusterRecord
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class BcsClusterDao @Autowired constructor(
    private val dslContext: DSLContext
) {
    fun list(): List<TBcsClusterRecord> {
        with(TBcsCluster.T_BCS_CLUSTER) {
            return dslContext.selectFrom(this).fetch()
        }
    }
}