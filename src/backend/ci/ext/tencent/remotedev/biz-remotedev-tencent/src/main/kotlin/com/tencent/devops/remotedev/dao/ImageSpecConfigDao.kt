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

package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.remotedev.tables.TRemoteDevImageSpecConfig
import com.tencent.devops.model.remotedev.tables.records.TRemoteDevImageSpecConfigRecord
import com.tencent.devops.remotedev.pojo.ImageSpec
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ImageSpecConfigDao {

    fun fetchImageSpec(
        dslContext: DSLContext
    ): TRemoteDevImageSpecConfigRecord? {
        return with(TRemoteDevImageSpecConfig.T_REMOTE_DEV_IMAGE_SPEC_CONFIG) {
            dslContext.selectFrom(this).fetchAny()
        }
    }

    fun listImageSpec(
        dslContext: DSLContext
    ): Result<TRemoteDevImageSpecConfigRecord> {
        return with(TRemoteDevImageSpecConfig.T_REMOTE_DEV_IMAGE_SPEC_CONFIG) {
            dslContext.selectFrom(this).fetch()
        }
    }

    fun addImageSpec(
        dslContext: DSLContext,
        spec: ImageSpec
    ): Boolean {
        return with(TRemoteDevImageSpecConfig.T_REMOTE_DEV_IMAGE_SPEC_CONFIG) {
            dslContext.insertInto(
                this,
                IDE_REF,
                REMOTING_REF,
                IDE_LAYER_REF
            ).values(
                spec.ideRef,
                spec.remotingRef,
                if (spec.ideLayerRef.isNullOrEmpty()) {
                    null
                } else {
                    JSON.json(JsonUtil.toJson(spec.ideLayerRef!!, formatted = false))
                }
            ).execute() > 0
        }
    }

    fun updateImageSpec(
        dslContext: DSLContext,
        id: Int,
        spec: ImageSpec
    ): Boolean {
        return with(TRemoteDevImageSpecConfig.T_REMOTE_DEV_IMAGE_SPEC_CONFIG) {
            dslContext.update(this)
                .set(IDE_REF, spec.ideRef)
                .set(REMOTING_REF, spec.remotingRef)
                .set(
                    IDE_LAYER_REF, if (spec.ideLayerRef.isNullOrEmpty()) {
                        null
                    } else {
                        JSON.json(JsonUtil.toJson(spec.ideLayerRef!!, formatted = false))
                    }
                )
                .where(ID.eq(id))
                .execute() > 0
        }
    }

    fun deleteImageSpec(
        dslContext: DSLContext,
        id: Int
    ): Boolean {
        return with(TRemoteDevImageSpecConfig.T_REMOTE_DEV_IMAGE_SPEC_CONFIG) {
            dslContext.deleteFrom(this).where(ID.eq(id)).execute() > 0
        }
    }
}
