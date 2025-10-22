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
package com.tencent.devops.store.image.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.store.tables.TImageAgentType
import com.tencent.devops.store.image.dao.Constants.KEY_IMAGE_AGENT_TYPE
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ImageAgentTypeDao {
    fun getAgentTypeByImageCode(dslContext: DSLContext, imageCode: String): Result<Record1<String>>? {
        val tImageAgentType = TImageAgentType.T_IMAGE_AGENT_TYPE.`as`("tImageAgentType")
        return dslContext.select(
            tImageAgentType.AGENT_TYPE.`as`(KEY_IMAGE_AGENT_TYPE)
        ).from(tImageAgentType)
            .where(tImageAgentType.IMAGE_CODE.eq(imageCode))
            .skipCheck()
            .fetch()
    }

    fun deleteAgentTypeByImageCode(dslContext: DSLContext, imageCode: String): Int {
        val tImageAgentType = TImageAgentType.T_IMAGE_AGENT_TYPE.`as`("tImageAgentType")
        return dslContext.deleteFrom(
            tImageAgentType
        )
            .where(tImageAgentType.IMAGE_CODE.eq(imageCode))
            .execute()
    }

    fun addAgentTypeByImageCode(dslContext: DSLContext, imageCode: String, agentType: ImageAgentTypeEnum): Int {
        with(TImageAgentType.T_IMAGE_AGENT_TYPE) {
            val baseQuery = dslContext.insertInto(
                this,
                ID,
                IMAGE_CODE,
                AGENT_TYPE
            )
                .values(
                    UUIDUtil.generate(),
                    imageCode,
                    agentType.name
                )
            return baseQuery.execute()
        }
    }

    fun getImageCodesByAgentType(dslContext: DSLContext, agentType: ImageAgentTypeEnum): Result<Record1<String>>? {
        with(TImageAgentType.T_IMAGE_AGENT_TYPE) {
            val baseQuery = dslContext.select(
                IMAGE_CODE
            ).from(this).where(AGENT_TYPE.eq(agentType.name))
            return baseQuery.fetch()
        }
    }
}
