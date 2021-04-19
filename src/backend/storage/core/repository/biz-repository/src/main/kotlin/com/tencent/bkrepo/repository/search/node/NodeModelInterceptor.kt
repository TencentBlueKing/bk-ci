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

package com.tencent.bkrepo.repository.search.node

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.interceptor.QueryContext
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.search.common.ModelValidateInterceptor

/**
 * 节点自定义查询规则拦截器
 */
class NodeModelInterceptor : ModelValidateInterceptor() {

    override fun intercept(queryModel: QueryModel, context: QueryContext): QueryModel {
        super.intercept(queryModel, context)
        // 添加deleted属性为null的查询条件
        setDeletedNull(queryModel)
        return queryModel
    }

    /**
     * 添加deleted属性为null的查询条件到[queryModel]中
     */
    private fun setDeletedNull(queryModel: QueryModel) {
        queryModel.addQueryRule(Rule.QueryRule(TNode::deleted.name, StringPool.EMPTY, OperationType.NULL))
    }
}
