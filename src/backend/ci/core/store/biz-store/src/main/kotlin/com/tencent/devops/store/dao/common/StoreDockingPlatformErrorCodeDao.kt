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

package com.tencent.devops.store.dao.common

import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStoreDockingPlatformErrorCode
import com.tencent.devops.store.pojo.common.ErrorCodeInfo
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StoreDockingPlatformErrorCodeDao {

    fun batchSaveErrorCodeInfo(dslContext: DSLContext, platformCode: String, errorCodeInfos: List<ErrorCodeInfo>?) {
        with(TStoreDockingPlatformErrorCode.T_STORE_DOCKING_PLATFORM_ERROR_CODE) {
            val now = LocalDateTime.now()
            dslContext.batch(errorCodeInfos?.map {
                dslContext.insertInto(
                    this,
                    ID,
                    PLATFORM_CODE,
                    ERROR_CODE,
                    ERROR_MSG_ZH_CN,
                    ERROR_MSG_ZH_TW,
                    ERROR_MSG_EN,
                    CREATOR,
                    CREATE_TIME,
                    MODIFIER,
                    UPDATE_TIME
                    ).values(
                    UUIDUtil.generate(),
                    platformCode,
                    it.errorCode,
                    it.errorMsgZhCn,
                    it.errorMsgZhTw,
                    it.errorMsgEn,
                    SYSTEM_USER,
                    now,
                    SYSTEM_USER,
                    now
                    ).onDuplicateKeyUpdate()
                    .set(ERROR_MSG_ZH_CN, it.errorMsgZhCn)
                    .set(ERROR_MSG_ZH_TW, it.errorMsgZhTw)
                    .set(ERROR_MSG_EN, it.errorMsgEn)
                    .set(MODIFIER, SYSTEM_USER)
                    .set(UPDATE_TIME, now)
            }).execute()
        }
    }

    fun deletePlatformErrorCodeInfo(
        dslContext: DSLContext,
        platformCode: String,
        errorCodes: List<Int>
    ) {
        with(TStoreDockingPlatformErrorCode.T_STORE_DOCKING_PLATFORM_ERROR_CODE) {
            dslContext.deleteFrom(this)
                .where(PLATFORM_CODE.eq(platformCode))
                .and(ERROR_CODE.`in`(errorCodes))
                .execute()
        }
    }
}
