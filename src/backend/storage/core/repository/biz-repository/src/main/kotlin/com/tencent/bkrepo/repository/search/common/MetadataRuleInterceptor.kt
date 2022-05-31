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

package com.tencent.bkrepo.repository.search.common

import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.interceptor.QueryContext
import com.tencent.bkrepo.common.query.interceptor.QueryRuleInterceptor
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.repository.constant.METADATA_PREFIX
import com.tencent.bkrepo.repository.model.TMetadata
import com.tencent.bkrepo.repository.model.TNode
import org.springframework.data.mongodb.core.query.Criteria

/**
 * 元数据规则拦截器
 *
 * 条件构造器中传入元数据的条件是`metadata.key=value`，需要适配成mongodb的查询条件
 */
class MetadataRuleInterceptor : QueryRuleInterceptor {

    override fun match(rule: Rule): Boolean {
        return rule is Rule.QueryRule && rule.field.startsWith(METADATA_PREFIX)
    }

    override fun intercept(rule: Rule, context: QueryContext): Criteria {
        require(rule is Rule.QueryRule)
        val key = rule.field.removePrefix(METADATA_PREFIX)
        val keyRule = Rule.QueryRule(TMetadata::key.name, key, OperationType.EQ).toFixed()
        val valueRule = Rule.QueryRule(TMetadata::value.name, rule.value, rule.operation).toFixed()
        val nestedAndRule = Rule.NestedRule(mutableListOf(keyRule, valueRule))

        // 历史数据以X-BKREPO-META-{key}设置元数据时未忽略大小写，查询时需要同时查询大小写key
        val criteria = if (key.contains(Regex(ALPHA_PATTERN))) {
            val lowerKeyRule = Rule.QueryRule(TMetadata::key.name, key.toLowerCase(), OperationType.EQ).toFixed()
            val lowerKeyNestedAndRule = Rule.NestedRule(mutableListOf(lowerKeyRule, valueRule))
            val nestedOrRule =
                Rule.NestedRule(mutableListOf(nestedAndRule, lowerKeyNestedAndRule), Rule.NestedRule.RelationType.OR)
            context.interpreter.resolveRule(nestedOrRule, context)
        } else {
            context.interpreter.resolveRule(nestedAndRule, context)
        }
        // 在流水线含有pipelineId或者buildId时，使用METADATA_IDX查询效率更高
        if (isInPipelineSearch(key)) {
            context.mongoQuery.withHint(TNode.METADATA_IDX)
        }
        return Criteria.where(TNode::metadata.name).elemMatch(criteria)
    }

    /**
     * 是否是流水线相关查询
     * */
    private fun isInPipelineSearch(key: String): Boolean {
        return key == PIPELINE_ID || key == BUILD_ID
    }

    companion object {
        const val ALPHA_PATTERN = "[A-Z]"
        const val PIPELINE_ID = "pipelineId"
        const val BUILD_ID = "buildId"
    }
}
