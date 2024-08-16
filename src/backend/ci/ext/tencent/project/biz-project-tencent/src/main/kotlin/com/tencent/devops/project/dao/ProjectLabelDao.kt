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

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.project.tables.TProjectLabel
import com.tencent.devops.model.project.tables.TProjectLabelRel
import com.tencent.devops.model.project.tables.records.TProjectLabelRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ProjectLabelDao {

    fun add(dslContext: DSLContext, labelName: String) {
        with(TProjectLabel.T_PROJECT_LABEL) {
            dslContext.insertInto(this,
                    ID,
                    LABEL_NAME
            )
                    .values(UUIDUtil.generate(),
                            labelName
                    )
                    .execute()
        }
    }

    fun countByName(dslContext: DSLContext, labelName: String): Int {
        with(TProjectLabel.T_PROJECT_LABEL) {
            return dslContext.selectCount().from(this).where(LABEL_NAME.eq(labelName)).fetchOne(0, Int::class.java)!!
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TProjectLabel.T_PROJECT_LABEL) {
            dslContext.deleteFrom(this)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun getProjectLabel(dslContext: DSLContext, id: String): TProjectLabelRecord? {
        return with(TProjectLabel.T_PROJECT_LABEL) {
            dslContext.selectFrom(this)
                    .where(ID.eq(id))
                    .fetchOne()
        }
    }

    fun getAllProjectLabel(dslContext: DSLContext): Result<TProjectLabelRecord>? {
        return with(TProjectLabel.T_PROJECT_LABEL) {
            dslContext.selectFrom(this)
                    .orderBy(CREATE_TIME.desc())
                    .fetch()
        }
    }

    fun getProjectLabelByProjectId(dslContext: DSLContext, projectId: String): Result<TProjectLabelRecord>? {
        val a = TProjectLabel.T_PROJECT_LABEL.`as`("a")
        val b = TProjectLabelRel.T_PROJECT_LABEL_REL.`as`("b")
        return dslContext.selectFrom(a)
                    .where(a.ID.`in`(dslContext.select(b.LABEL_ID).from(b).where(b.PROJECT_ID.eq(projectId))))
                    .fetch()
    }

    fun update(dslContext: DSLContext, id: String, labelName: String) {
        with(TProjectLabel.T_PROJECT_LABEL) {
            dslContext.update(this)
                    .set(LABEL_NAME, labelName)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(ID.eq(id))
                    .execute()
        }
    }
}
