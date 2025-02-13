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

package com.tencent.devops.store.common.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStoreBelongDeptRel
import com.tencent.devops.store.pojo.common.StoreBelongDeptRel
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class TxStoreBelongDeptRelDao {

    fun batchAdd(userId: String, dslContext: DSLContext, storeBelongDeptRelList: List<StoreBelongDeptRel>) {
        with(TStoreBelongDeptRel.T_STORE_BELONG_DEPT_REL) {
            val addStep = storeBelongDeptRelList.map { storeBelongDeptRel ->
                dslContext.insertInto(
                    this,
                    ID,
                    STORE_CODE,
                    STORE_TYPE,
                    BG_ID,
                    BG_NAME,
                    DEPT_ID,
                    DEPT_NAME,
                    CENTER_ID,
                    CENTER_NAME,
                    GROUP_ID,
                    GROUP_NAME,
                    BUSINESS_LINE_ID,
                    BUSINESS_LINE_NAME,
                    CREATOR,
                    MODIFIER,
                    CREATE_TIME,
                    UPDATE_TIME
                ).values(
                    UUIDUtil.generate(),
                    storeBelongDeptRel.storeCode,
                    storeBelongDeptRel.storeType.type.toByte(),
                    storeBelongDeptRel.storeDeptInfo.bgId,
                    storeBelongDeptRel.storeDeptInfo.bgName,
                    storeBelongDeptRel.storeDeptInfo.deptId,
                    storeBelongDeptRel.storeDeptInfo.deptName,
                    storeBelongDeptRel.storeDeptInfo.centerId,
                    storeBelongDeptRel.storeDeptInfo.centerName,
                    storeBelongDeptRel.storeDeptInfo.groupId,
                    storeBelongDeptRel.storeDeptInfo.groupName,
                    storeBelongDeptRel.storeDeptInfo.businessLineId,
                    storeBelongDeptRel.storeDeptInfo.businessLineName,
                    userId,
                    userId,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                ).onDuplicateKeyUpdate()
                    .set(BG_ID, storeBelongDeptRel.storeDeptInfo.bgId)
                    .set(BG_NAME, storeBelongDeptRel.storeDeptInfo.bgName)
                    .set(DEPT_ID, storeBelongDeptRel.storeDeptInfo.deptId)
                    .set(DEPT_NAME, storeBelongDeptRel.storeDeptInfo.deptName)
                    .set(CENTER_ID, storeBelongDeptRel.storeDeptInfo.centerId)
                    .set(CENTER_NAME, storeBelongDeptRel.storeDeptInfo.centerName)
                    .set(GROUP_ID, storeBelongDeptRel.storeDeptInfo.groupId)
                    .set(GROUP_NAME, storeBelongDeptRel.storeDeptInfo.groupName)
                    .set(BUSINESS_LINE_ID, storeBelongDeptRel.storeDeptInfo.businessLineId)
                    .set(BUSINESS_LINE_NAME, storeBelongDeptRel.storeDeptInfo.businessLineName)
                    .set(MODIFIER, userId)
                    .set(UPDATE_TIME, LocalDateTime.now())
            }
            dslContext.batch(addStep).execute()
        }
    }

    fun delete(dslContext: DSLContext, storeCode: String, storeType: StoreTypeEnum) {
        with(TStoreBelongDeptRel.T_STORE_BELONG_DEPT_REL) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType.type.toByte())))
                .execute()
        }
    }
}
