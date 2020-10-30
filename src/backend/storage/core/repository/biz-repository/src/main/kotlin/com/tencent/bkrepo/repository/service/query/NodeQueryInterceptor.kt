/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.  
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.repository.service.query

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.interceptor.QueryModelInterceptor
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.repository.model.TNode

/**
 * - 校验node query model的格式
 * - 添加deleted属性为null的查询条件
 */
class NodeQueryInterceptor : QueryModelInterceptor {

    override fun intercept(queryModel: QueryModel): QueryModel {
        validateModel(queryModel)
        setDeletedNull(queryModel)

        return queryModel
    }

    /**
     * 查询条件必须满足以下格式：
     *   1. rule必须为AND类型的嵌套查询
     *   2. rule嵌套查询规则列表中，必须指定projectId条件，且为EQ操作
     *   3. rule嵌套查询规则列表中，必须指定repoName条件，且为EQ 或者 IN操作
     * 对于rule嵌套查询规则列表中的其它规则，不做限定
     */
    private fun validateModel(queryModel: QueryModel) {
        val rule = queryModel.rule
        // rule必须为AND类型的嵌套查询
        if (rule is Rule.NestedRule && rule.relation == Rule.NestedRule.RelationType.AND) {
            var projectIdValidation = false
            var repoNameValidation = false
            for (subRule in rule.rules) {
                if (checkProjectId(subRule)) projectIdValidation = true
                if (checkRepoName(subRule)) repoNameValidation = true
            }
            if (!projectIdValidation) {
                throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "projectId")
            }
            if (!repoNameValidation) {
                throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "repoName")
            }
        } else {
            throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "relation")
        }
    }

    /**
     * rule嵌套查询规则列表中，必须指定projectId条件，且为EQ操作
     */
    private fun checkRepoName(rule: Rule): Boolean {
        if (rule is Rule.QueryRule && rule.field == TNode::repoName.name) {
            if (rule.operation == OperationType.EQ || rule.operation == OperationType.IN) {
                return true
            }
        }
        return false
    }

    /**
     * rule嵌套查询规则列表中，必须指定repoName条件，且为EQ 或者 IN操作
     */
    private fun checkProjectId(rule: Rule): Boolean {
        if (rule is Rule.QueryRule && rule.field == TNode::projectId.name) {
            if (rule.operation == OperationType.EQ && rule.value.toString().isNotBlank()) {
                return true
            }
        }
        return false
    }

    /**
     * 添加deleted属性为null的查询条件
     */
    private fun setDeletedNull(queryModel: QueryModel) {
        queryModel.addQueryRule(Rule.QueryRule(TNode::deleted.name, "", OperationType.NULL))
    }
}
