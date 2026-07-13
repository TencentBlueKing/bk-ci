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

package com.tencent.devops.store.common.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStoreProjectVisibleRel
import com.tencent.devops.model.store.tables.records.TStoreProjectVisibleRelRecord
import com.tencent.devops.store.pojo.common.visible.StoreVisibleProjectInfo
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class StoreVisibleProjectRelDao {

    /**
     * 获取组件已设置的项目可见范围
     */
    fun getProjectInfosByStoreCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte
    ): Result<TStoreProjectVisibleRelRecord> {
        with(TStoreProjectVisibleRel.T_STORE_PROJECT_VISIBLE_REL) {
            return dslContext.selectFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .fetch()
        }
    }

    /**
     * 批量获取在指定项目下可见的组件编码集合。
     * 用于列表接口计算安装/可用标识(flag)时，判断组件是否因"按项目授权"而对当前项目可见。
     */
    fun listStoreCodesByProject(
        dslContext: DSLContext,
        storeType: Byte,
        projectCode: String,
        storeCodes: Collection<String>
    ): Set<String> {
        if (storeCodes.isEmpty()) return emptySet()
        with(TStoreProjectVisibleRel.T_STORE_PROJECT_VISIBLE_REL) {
            return dslContext.select(STORE_CODE)
                .from(this)
                .where(STORE_TYPE.eq(storeType))
                .and(PROJECT_CODE.eq(projectCode))
                .and(STORE_CODE.`in`(storeCodes))
                .fetchSet(STORE_CODE)
        }
    }

    /**
     * 批量设置组件的项目可见范围（幂等，存在则更新项目名称）
     */
    fun batchAdd(
        dslContext: DSLContext,
        userId: String,
        storeCode: String,
        storeType: Byte,
        projectInfoList: List<StoreVisibleProjectInfo>
    ) {
        if (projectInfoList.isEmpty()) return
        with(TStoreProjectVisibleRel.T_STORE_PROJECT_VISIBLE_REL) {
            val addStep = projectInfoList.map {
                dslContext.insertInto(
                    this,
                    ID,
                    STORE_CODE,
                    STORE_TYPE,
                    PROJECT_CODE,
                    PROJECT_NAME,
                    CREATOR,
                    MODIFIER
                ).values(
                    UUIDUtil.generate(),
                    storeCode,
                    storeType,
                    it.projectCode,
                    it.projectName,
                    userId,
                    userId
                ).onDuplicateKeyUpdate()
                    .set(PROJECT_NAME, it.projectName)
                    .set(MODIFIER, userId)
                    .set(UPDATE_TIME, LocalDateTime.now())
            }
            dslContext.batch(addStep).execute()
        }
    }

    /**
     * 批量删除组件指定的项目可见范围
     */
    fun batchDelete(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        projectCodeList: List<String>
    ) {
        if (projectCodeList.isEmpty()) return
        with(TStoreProjectVisibleRel.T_STORE_PROJECT_VISIBLE_REL) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .and(PROJECT_CODE.`in`(projectCodeList))
                .execute()
        }
    }

    /**
     * 删除组件全部的项目可见范围
     */
    fun deleteByStoreCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte
    ) {
        with(TStoreProjectVisibleRel.T_STORE_PROJECT_VISIBLE_REL) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .execute()
        }
    }
}
