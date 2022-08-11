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

package com.tencent.devops.sign.dao

import com.tencent.devops.model.sign.tables.TSignIpaUpload
import com.tencent.devops.model.sign.tables.records.TSignIpaUploadRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class IpaUploadDao {

    @Suppress("LongParameterList")
    fun save(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        uploadToken: String
    ) {
        with(TSignIpaUpload.T_SIGN_IPA_UPLOAD) {
            dslContext.insertInto(this,
                UPLOAD_TOKEN,
                USER_ID,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                CREATE_TIME
            ).values(
                uploadToken,
                userId,
                projectId,
                pipelineId,
                buildId,
                LocalDateTime.now()
            ).execute()
        }
    }

    fun get(dslContext: DSLContext, uploadToken: String): TSignIpaUploadRecord? {
        return with(TSignIpaUpload.T_SIGN_IPA_UPLOAD) {
            dslContext.selectFrom(this)
                .where(
                    UPLOAD_TOKEN.eq(uploadToken)
                ).fetchAny()
        }
    }

    fun update(dslContext: DSLContext, uploadToken: String, resignId: String) {
        return with(TSignIpaUpload.T_SIGN_IPA_UPLOAD) {
            dslContext.update(this)
                .set(RESIGN_ID, resignId)
                .where(UPLOAD_TOKEN.eq(uploadToken))
                .execute()
        }
    }
}
