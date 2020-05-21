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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.gitci.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.gitci.pojo.GitYamlProperty
import com.tencent.devops.model.gitci.tables.TGitWebStarterYaml.T_GIT_WEB_STARTER_YAML
import com.tencent.devops.model.gitci.tables.records.TGitWebStarterYamlRecord
import org.jooq.DSLContext
import org.jooq.InsertSetMoreStep
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GitStarterWebYamlDao {

    fun getList(
        dslContext: DSLContext,
        yamlUrl: String? = "",
        iconUrl: String? = ""
    ): List<GitYamlProperty> {
        with(T_GIT_WEB_STARTER_YAML) {
            val result = dslContext.selectFrom(this).fetch()
            val list = mutableListOf<GitYamlProperty>()
            result?.forEach {
                if (it.categories != null) {
                    list.add(
                        GitYamlProperty(
                            name = it.name,
                            description = it.description,
                            iconName = it.iconName,
                            categories = JsonUtil.getObjectMapper().readValue(it.categories, List::class.java) as List<String>,
                            yamlUrl = "$yamlUrl/${it.name}.yml",
                            iconUrl = "$iconUrl/${it.iconName}"
                        )
                    )
                } else {
                    list.add(
                        GitYamlProperty(
                            name = it.name,
                            description = it.description,
                            iconName = it.iconName,
                            categories = null,
                            yamlUrl = "$yamlUrl/${it.name}.yml",
                            iconUrl = "$iconUrl/${it.iconName}"
                        )
                    )
                }
            }
            return list
        }
    }

    fun refreshProperties(
        dslContext: DSLContext,
        properties: List<GitYamlProperty>
    ): Int {
        with(T_GIT_WEB_STARTER_YAML) {
            // 全表清空
            dslContext.deleteFrom(this).where(true).execute()
            val sets =
                mutableListOf<InsertSetMoreStep<TGitWebStarterYamlRecord>>()
            properties.forEach {
                sets.add(
                    dslContext.insertInto(this)
                        .set(NAME, it.name)
                        .set(DESCRIPTION, it.description)
                        .set(ICON_NAME, it.iconName)
                        .set(CATEGORIES, if (it.categories == null) null else JsonUtil.toJson(it.categories!!))
                        .set(CREATE_TIME, LocalDateTime.now())
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
