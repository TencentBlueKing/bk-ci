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

package com.tencent.devops.store.dao

import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TExtensionService
import com.tencent.devops.model.store.tables.TIdeAtom
import com.tencent.devops.model.store.tables.TImage
import com.tencent.devops.model.store.tables.TTemplate
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class TxOpMigrateStoreDescriptionDao {

    fun getAtomDescription(
        dslContext: DSLContext,
        limit: Int,
        offset: Int
    ): Result<out Record>? {
        return with(TAtom.T_ATOM) {
            dslContext.select(ID, ATOM_CODE, DESCRIPTION, CREATOR).from(this)
                .orderBy(CREATE_TIME.asc(), ID.asc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun updateAtomDescription(
        dslContext: DSLContext,
        id: String,
        description: String
    ) {
        with(TAtom.T_ATOM) {
            dslContext.update(this)
                .set(DESCRIPTION, description)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getTemplateDescription(
        dslContext: DSLContext,
        limit: Int,
        offset: Int
    ): Result<out Record>? {
        return with(TTemplate.T_TEMPLATE) {
            dslContext.select(ID, TEMPLATE_CODE, DESCRIPTION, CREATOR).from(this)
                .orderBy(CREATE_TIME.asc(), ID.asc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun updateTemplateDescription(
        dslContext: DSLContext,
        id: String,
        description: String
    ) {
        with(TTemplate.T_TEMPLATE) {
            dslContext.update(this)
                .set(DESCRIPTION, description)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getIdeAtomDescription(
        dslContext: DSLContext,
        limit: Int,
        offset: Int
    ): Result<out Record>? {
        return with(TIdeAtom.T_IDE_ATOM) {
            dslContext.select(ID, ATOM_CODE, DESCRIPTION, CREATOR).from(this)
                .orderBy(CREATE_TIME.asc(), ID.asc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun updateIdeAtomDescription(
        dslContext: DSLContext,
        id: String,
        description: String
    ) {
        with(TIdeAtom.T_IDE_ATOM) {
            dslContext.update(this)
                .set(DESCRIPTION, description)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getImageDescription(
        dslContext: DSLContext,
        limit: Int,
        offset: Int
    ): Result<out Record>? {
        return with(TImage.T_IMAGE) {
            dslContext.select(ID, IMAGE_CODE, DESCRIPTION, CREATOR).from(this)
                .orderBy(CREATE_TIME.asc(), ID.asc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun updateImageDescription(
        dslContext: DSLContext,
        id: String,
        description: String
    ) {
        with(TImage.T_IMAGE) {
            dslContext.update(this)
                .set(DESCRIPTION, description)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getExtServiceDescription(
        dslContext: DSLContext,
        limit: Int,
        offset: Int
    ): Result<out Record>? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.select(ID, SERVICE_CODE, DESCRIPTION, CREATOR).from(this)
                .orderBy(CREATE_TIME.asc(), ID.asc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun updateExtServiceDescription(
        dslContext: DSLContext,
        id: String,
        description: String
    ) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.update(this)
                .set(DESCRIPTION, description)
                .where(ID.eq(id))
                .execute()
        }
    }
}
