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

package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TProject
import com.tencent.devops.model.project.tables.records.TProjectRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import org.springframework.util.StringUtils
import java.net.URLDecoder

@Repository
class ProjectLocalDao {

    fun getProjectListExclude(
        dslContext: DSLContext,
        offset: Int,
        limit: Int,
        englishNamesExclude: List<String>
    ): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            val conditions = generateQueryProjectCondition(
                projectName = null,
                englishName = null,
                projectType = null,
                isSecrecy = null,
                creator = null,
                approver = null,
                approvalStatus = null,
                grayFlag = false,
                englishNames = null,
                englishNamesExclude = englishNamesExclude
            )
            return dslContext.selectFrom(this).where(conditions).orderBy(CREATED_AT.desc()).limit(offset, limit).fetch()
        }
    }

    private fun TProject.generateQueryProjectCondition(
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        grayFlag: Boolean,
        englishNames: Set<String>?,
        englishNamesExclude: List<String>
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        if (!StringUtils.isEmpty(projectName)) conditions.add(
            PROJECT_NAME.like(
                "%" + URLDecoder.decode(
                    projectName,
                    "UTF-8"
                ) + "%"
            )
        )
        if (!StringUtils.isEmpty(englishName)) conditions.add(
            ENGLISH_NAME.like(
                "%" + URLDecoder.decode(
                    englishName,
                    "UTF-8"
                ) + "%"
            )
        )
        if (!StringUtils.isEmpty(projectType)) conditions.add(PROJECT_TYPE.eq(projectType))
        if (!StringUtils.isEmpty(isSecrecy)) conditions.add(IS_SECRECY.eq(isSecrecy))
        if (!StringUtils.isEmpty(creator)) conditions.add(CREATOR.eq(creator))
        if (!StringUtils.isEmpty(approver)) conditions.add(APPROVER.eq(approver))
        if (!StringUtils.isEmpty(approvalStatus)) conditions.add(APPROVAL_STATUS.eq(approvalStatus))
        if (grayFlag) {
            if (englishNames == null) {
                conditions.add(ENGLISH_NAME.`in`(setOf<String>()))
            } else {
                conditions.add(ENGLISH_NAME.`in`(englishNames))
            }
        }
        if (englishNamesExclude != null && englishNamesExclude.size > 0) {
            englishNamesExclude.forEach { name ->
                conditions.add(
                    ENGLISH_NAME.notLike(
                        "%" + URLDecoder.decode(
                            name,
                            "UTF-8"
                        ) + "%"
                    )
                )
            }
        }
        return conditions
    }
}
