package com.tencent.bkrepo.common.query.handler.impl

import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.handler.MongoQueryRuleHandler
import com.tencent.bkrepo.common.query.model.Rule
import org.springframework.data.mongodb.core.query.Criteria

class NinHandler : MongoQueryRuleHandler {

    override fun match(rule: Rule.QueryRule): Boolean {
        return rule.operation == OperationType.NIN
    }

    override fun handle(rule: Rule.QueryRule): Criteria {
        require(rule.value is List<*>)
        return Criteria.where(rule.field).nin(rule.value as List<*>)
    }
}
