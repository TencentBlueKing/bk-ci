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

import com.tencent.bkrepo.common.query.builder.MongoQueryInterpreter
import com.tencent.bkrepo.common.query.interceptor.QueryContext
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.repository.search.common.LocalDatetimeRuleInterceptor
import com.tencent.bkrepo.repository.search.common.MetadataRuleInterceptor
import com.tencent.bkrepo.repository.search.common.RepoNameRuleInterceptor
import com.tencent.bkrepo.repository.search.common.RepoTypeRuleInterceptor
import com.tencent.bkrepo.repository.search.common.SelectFieldInterceptor
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy

@Component
class NodeQueryInterpreter @Autowired @Lazy constructor(
    private val permissionManager: PermissionManager,
    private val repoNameRuleInterceptor: RepoNameRuleInterceptor,
    private val repoTypeRuleInterceptor: RepoTypeRuleInterceptor,
    private val localDatetimeRuleInterceptor: LocalDatetimeRuleInterceptor
) : MongoQueryInterpreter() {

    @PostConstruct
    fun init() {
        addModelInterceptor(NodeModelInterceptor(permissionManager))
        addModelInterceptor(SelectFieldInterceptor())
        addRuleInterceptor(repoTypeRuleInterceptor)
        addRuleInterceptor(repoNameRuleInterceptor)
        addRuleInterceptor(MetadataRuleInterceptor())
        addRuleInterceptor(localDatetimeRuleInterceptor)
    }

    override fun initContext(queryModel: QueryModel, mongoQuery: Query): QueryContext {
        return NodeQueryContext(queryModel, mongoQuery, this)
    }
}
