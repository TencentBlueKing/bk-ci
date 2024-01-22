package com.tencent.devops.environment.service.job

import com.tencent.devops.model.environment.tables.records.TNodeRecord

interface IQueryOperatorService {
    fun isOperatorOrBakOperator(userId: String, nodeRecords: Set<TNodeRecord>)
}