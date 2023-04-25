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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.stream.v1.service

import com.tencent.devops.stream.v1.dao.V1GitStarterWebYamlDao
import com.tencent.devops.stream.v1.pojo.V1GitStarterWebList
import com.tencent.devops.stream.v1.pojo.V1GitYamlContent
import com.tencent.devops.stream.v1.pojo.V1GitYamlProperty
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service

@Service
@RefreshScope
class V1GitCIStarterWebService @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitStarterWebYamlDao: V1GitStarterWebYamlDao
) {

    fun getYamlList(category: String? = null): List<V1GitYamlContent> {
        logger.info("getYamlList with category: $category")
        return gitStarterWebYamlDao.getYamlContents(dslContext)
    }

    fun getPropertyList(category: String? = null): List<V1GitYamlProperty> {
        logger.info("getPropertyList with category: $category")
        return if (category.isNullOrEmpty()) {
            gitStarterWebYamlDao.getProperties(dslContext)
        } else {
            gitStarterWebYamlDao.getProperties(dslContext).filter {
                it.categories?.contains(category) == true
            }
        }
    }

    fun getStarterWebList(): V1GitStarterWebList {
        val tkexList = mutableListOf<V1GitYamlProperty>()
        val othersList = mutableListOf<V1GitYamlProperty>()
        gitStarterWebYamlDao.getProperties(dslContext).forEach {

            if (it.categories?.contains("TKEX") == true) tkexList.add(it)
            else othersList.add(it)
        }
        return V1GitStarterWebList(tkexList, othersList)
    }

    fun updateStarterYamls(properties: List<V1GitYamlContent>): Int {
        logger.info("updateStarterYamls: $properties")
        return gitStarterWebYamlDao.updateYamls(dslContext, properties)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(V1GitCIStarterWebService::class.java)
    }
}
