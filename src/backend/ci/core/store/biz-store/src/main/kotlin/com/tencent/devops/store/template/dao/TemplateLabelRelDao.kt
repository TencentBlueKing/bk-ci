/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.template.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TLabel
import com.tencent.devops.model.store.tables.TTemplate
import com.tencent.devops.model.store.tables.TTemplateLabelRel
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_LABEL_CODE
import com.tencent.devops.store.pojo.common.KEY_LABEL_ID
import com.tencent.devops.store.pojo.common.KEY_LABEL_NAME
import com.tencent.devops.store.pojo.common.KEY_LABEL_TYPE
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class TemplateLabelRelDao {

    fun getLabelsByTemplateId(
        dslContext: DSLContext,
        templateId: String
    ): Result<out Record>? {
        val a = TLabel.T_LABEL.`as`("a")
        val b = TTemplateLabelRel.T_TEMPLATE_LABEL_REL.`as`("b")
        return dslContext.select(
            a.ID.`as`(KEY_LABEL_ID),
            a.LABEL_CODE.`as`(KEY_LABEL_CODE),
            a.LABEL_NAME.`as`(KEY_LABEL_NAME),
            a.TYPE.`as`(KEY_LABEL_TYPE),
            a.CREATE_TIME.`as`(KEY_CREATE_TIME),
            a.UPDATE_TIME.`as`(KEY_UPDATE_TIME)
        ).from(a).join(b).on(a.ID.eq(b.LABEL_ID))
            .where(b.TEMPLATE_ID.eq(templateId))
            .fetch()
    }

    fun deleteByTemplateId(dslContext: DSLContext, templateId: String) {
        with(TTemplateLabelRel.T_TEMPLATE_LABEL_REL) {
            dslContext.deleteFrom(this)
                .where(TEMPLATE_ID.eq(templateId))
                .execute()
        }
    }

    fun deleteByTemplateCode(dslContext: DSLContext, templateCode: String) {
        val tt = TTemplate.T_TEMPLATE
        val templateIds = dslContext.select(tt.ID).from(tt).where(tt.TEMPLATE_CODE.eq(templateCode)).fetch()
        with(TTemplateLabelRel.T_TEMPLATE_LABEL_REL) {
            dslContext.deleteFrom(this)
                .where(TEMPLATE_ID.`in`(templateIds))
                .execute()
        }
    }

    fun batchAdd(dslContext: DSLContext, userId: String, templateId: String, labelIdList: List<String>) {
        with(TTemplateLabelRel.T_TEMPLATE_LABEL_REL) {
            val addStep = labelIdList.map {
                dslContext.insertInto(this,
                    ID,
                    TEMPLATE_ID,
                    LABEL_ID,
                    CREATOR,
                    MODIFIER
                )
                    .values(
                        UUIDUtil.generate(),
                        templateId,
                        it,
                        userId,
                        userId
                    )
            }
            dslContext.batch(addStep).execute()
        }
    }
}
