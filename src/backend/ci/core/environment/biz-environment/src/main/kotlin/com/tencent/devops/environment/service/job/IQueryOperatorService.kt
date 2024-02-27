package com.tencent.devops.environment.service.job

import org.jooq.Record5

interface IQueryOperatorService {
    fun isOperatorOrBakOperator(userId: String, nodeRecords: Set<Record5<Long, String, Long, Long, String>>)
}