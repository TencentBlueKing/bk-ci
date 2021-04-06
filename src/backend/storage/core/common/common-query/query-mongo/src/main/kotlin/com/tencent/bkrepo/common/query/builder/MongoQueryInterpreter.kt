/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.query.builder

import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.exception.QueryModelException
import com.tencent.bkrepo.common.query.handler.MongoQueryRuleHandler
import com.tencent.bkrepo.common.query.handler.impl.AfterHandler
import com.tencent.bkrepo.common.query.handler.impl.BeforeHandler
import com.tencent.bkrepo.common.query.handler.impl.DefaultMongoNestedRuleHandler
import com.tencent.bkrepo.common.query.handler.impl.EqualHandler
import com.tencent.bkrepo.common.query.handler.impl.GreaterThanHandler
import com.tencent.bkrepo.common.query.handler.impl.GreaterThanOrEqualHandler
import com.tencent.bkrepo.common.query.handler.impl.InHandler
import com.tencent.bkrepo.common.query.handler.impl.LessThanHandler
import com.tencent.bkrepo.common.query.handler.impl.LessThanOrEqualHandler
import com.tencent.bkrepo.common.query.handler.impl.MatchHandler
import com.tencent.bkrepo.common.query.handler.impl.NotEqualHandler
import com.tencent.bkrepo.common.query.handler.impl.NotNullHandler
import com.tencent.bkrepo.common.query.handler.impl.NullHandler
import com.tencent.bkrepo.common.query.handler.impl.PrefixHandler
import com.tencent.bkrepo.common.query.handler.impl.SuffixHandler
import com.tencent.bkrepo.common.query.interceptor.QueryContext
import com.tencent.bkrepo.common.query.interceptor.QueryModelInterceptor
import com.tencent.bkrepo.common.query.interceptor.QueryRuleInterceptor
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

/**
 * MongoDB QueryInterpreter
 */
open class MongoQueryInterpreter {

    private val defaultQueryRuleHandlerMap = mutableMapOf<OperationType, MongoQueryRuleHandler>()
    private val nestedRuleHandler = DefaultMongoNestedRuleHandler()

    private val queryRuleInterceptorList = mutableListOf<QueryRuleInterceptor>()
    private val queryModelInterceptorList = mutableListOf<QueryModelInterceptor>()

    init {
        defaultQueryRuleHandlerMap[OperationType.EQ] = EqualHandler()
        defaultQueryRuleHandlerMap[OperationType.NE] = NotEqualHandler()
        defaultQueryRuleHandlerMap[OperationType.LT] = LessThanHandler()
        defaultQueryRuleHandlerMap[OperationType.LTE] = LessThanOrEqualHandler()
        defaultQueryRuleHandlerMap[OperationType.GT] = GreaterThanHandler()
        defaultQueryRuleHandlerMap[OperationType.GTE] = GreaterThanOrEqualHandler()
        defaultQueryRuleHandlerMap[OperationType.BEFORE] = BeforeHandler()
        defaultQueryRuleHandlerMap[OperationType.AFTER] = AfterHandler()
        defaultQueryRuleHandlerMap[OperationType.IN] = InHandler()
        defaultQueryRuleHandlerMap[OperationType.PREFIX] = PrefixHandler()
        defaultQueryRuleHandlerMap[OperationType.SUFFIX] = SuffixHandler()
        defaultQueryRuleHandlerMap[OperationType.MATCH] = MatchHandler()
        defaultQueryRuleHandlerMap[OperationType.NULL] = NullHandler()
        defaultQueryRuleHandlerMap[OperationType.NOT_NULL] = NotNullHandler()
    }

    open fun interpret(queryModel: QueryModel): QueryContext {
        val mongoQuery = Query()
        val queryContext = initContext(queryModel, mongoQuery)
        var newModel = queryModel
        for (interceptor in queryModelInterceptorList) {
            newModel = interceptor.intercept(newModel, queryContext)
            queryContext.queryModel = newModel
        }
        val pageNumber = newModel.page.getNormalizedPageNumber()
        val pageSize = newModel.page.getNormalizedPageSize()
        newModel.page.let {
            val pageRequest = PageRequest.of(pageNumber - 1, pageSize)
            mongoQuery.with(pageRequest)
        }
        newModel.sort?.let {
            val direction = Sort.Direction.fromString(it.direction.name)
            val sort = Sort.by(it.properties.map { property -> Sort.Order(direction, property) })
            mongoQuery.with(sort)
        }
        newModel.select?.forEach {
            mongoQuery.fields().include(it)
        }
        mongoQuery.addCriteria(resolveRule(queryModel.rule, queryContext))
        return queryContext
    }

    open fun initContext(queryModel: QueryModel, mongoQuery: Query): QueryContext {
        return QueryContext(queryModel, mongoQuery, this)
    }

    fun addRuleInterceptor(interceptor: QueryRuleInterceptor) {
        this.queryRuleInterceptorList.add(interceptor)
    }

    fun addModelInterceptor(interceptor: QueryModelInterceptor) {
        this.queryModelInterceptorList.add(interceptor)
    }

    fun resolveRule(rule: Rule, context: QueryContext): Criteria {
        // interceptor
        if (rule !is Rule.FixedRule) {
            for (interceptor in queryRuleInterceptorList) {
                if (interceptor.match(rule)) {
                    return interceptor.intercept(rule, context)
                }
            }
        }
        // resolve
        return when (rule) {
            is Rule.NestedRule -> resolveNestedRule(rule, context)
            is Rule.QueryRule -> resolveQueryRule(rule)
            is Rule.FixedRule -> resolveQueryRule(rule.wrapperRule)
        }
    }

    private fun resolveNestedRule(rule: Rule.NestedRule, context: QueryContext): Criteria {
        return nestedRuleHandler.handle(rule, context)
    }

    private fun resolveQueryRule(rule: Rule.QueryRule): Criteria {
        // 默认handler
        return findDefaultHandler(rule.operation).handle(rule)
    }

    private fun findDefaultHandler(operation: OperationType): MongoQueryRuleHandler {
        return defaultQueryRuleHandlerMap[operation] ?: throw QueryModelException("Unsupported operation [$operation].")
    }
}
