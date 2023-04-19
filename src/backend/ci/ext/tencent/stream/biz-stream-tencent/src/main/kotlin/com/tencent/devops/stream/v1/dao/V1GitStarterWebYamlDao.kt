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

package com.tencent.devops.stream.v1.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.stream.tables.TGitWebStarterYaml.T_GIT_WEB_STARTER_YAML
import com.tencent.devops.model.stream.tables.records.TGitWebStarterYamlRecord
import com.tencent.devops.stream.v1.pojo.V1GitYamlContent
import com.tencent.devops.stream.v1.pojo.V1GitYamlProperty
import org.jooq.DSLContext
import org.jooq.InsertOnDuplicateSetMoreStep
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("NestedBlockDepth")
@Repository
class V1GitStarterWebYamlDao {

    fun getYamlContents(dslContext: DSLContext): List<V1GitYamlContent> {
        with(T_GIT_WEB_STARTER_YAML) {
            val result = dslContext.selectFrom(this).fetch()
            val list = mutableListOf<V1GitYamlContent>()
            result?.forEach {
                val categories = if (it.categories != null) {
                    JsonUtil.getObjectMapper().readValue(it.categories, List::class.java) as List<String>
                } else null
                list.add(
                    V1GitYamlContent(
                        yaml = it.yamlContent,
                        property = V1GitYamlProperty(
                            name = it.name,
                            description = it.description,
                            iconName = it.iconName,
                            categories = categories,
                            yamlUrl = it.yamlUrl,
                            iconUrl = it.iconUrl
                        )
                    )
                )
            }
            return list
        }
    }

    fun getProperties(dslContext: DSLContext): List<V1GitYamlProperty> {
        with(T_GIT_WEB_STARTER_YAML) {
            val result = dslContext.selectFrom(this).fetch()
            val list = mutableListOf<V1GitYamlProperty>()
            result?.forEach {
                val categories = if (it.categories != null) {
                    JsonUtil.getObjectMapper().readValue(it.categories, List::class.java) as List<String>
                } else null
                list.add(
                    V1GitYamlProperty(
                        name = it.name,
                        description = it.description,
                        iconName = it.iconName,
                        categories = categories,
                        yamlUrl = it.yamlUrl,
                        iconUrl = it.iconUrl
                    )
                )
            }
            return list
        }
    }

    fun updateYamls(
        dslContext: DSLContext,
        yamls: List<V1GitYamlContent>
    ): Int {
        with(T_GIT_WEB_STARTER_YAML) {
            // 全表清空
            val originProperties = getProperties(dslContext)
            val sets =
                mutableListOf<InsertOnDuplicateSetMoreStep<TGitWebStarterYamlRecord>>()

            yamls.forEach {
                val category = if (it.property.categories == null) null else JsonUtil.toJson(it.property.categories!!)
                sets.add(
                    dslContext.insertInto(this)
                        .set(NAME, it.property.name)
                        .set(DESCRIPTION, it.property.description)
                        .set(ICON_NAME, it.property.iconName)
                        .set(CATEGORIES, category)
                        .set(YAML_CONTENT, it.yaml)
                        .set(YAML_URL, it.property.yamlUrl)
                        .set(ICON_URL, it.property.iconUrl)
                        .set(UPDATE_TIME, LocalDateTime.now())
                        .onDuplicateKeyUpdate()
                        .set(DESCRIPTION, it.property.description)
                        .set(ICON_NAME, it.property.iconName)
                        .set(CATEGORIES, category)
                        .set(YAML_CONTENT, it.yaml)
                        .set(YAML_URL, it.property.yamlUrl)
                        .set(ICON_URL, it.property.iconUrl)
                        .set(UPDATE_TIME, LocalDateTime.now())
                )
            }
            if (sets.isNotEmpty()) {
                val count = dslContext.batch(sets).execute()
                var success = 0
                count.forEach {
                    if (it == 1) {
                        success++
                    }
                }
                return success
            }
            return 0
        }
    }
}
