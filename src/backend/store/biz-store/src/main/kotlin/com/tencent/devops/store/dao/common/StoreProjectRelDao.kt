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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.dao.common

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.records.TStoreProjectRelRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class StoreProjectRelDao {

    fun addStoreProjectRel(
        dslContext: DSLContext,
        userId: String,
        storeCode: String,
        projectCode: String,
        type: Byte,
        storeType: Byte
    ) {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            dslContext.insertInto(
                this,
                ID,
                STORE_CODE,
                PROJECT_CODE,
                TYPE,
                STORE_TYPE,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    storeCode,
                    projectCode,
                    type,
                    storeType,
                    userId,
                    userId
                )
                .execute()
        }
    }

    fun getInitProjectCodeByStoreCode(dslContext: DSLContext, storeCode: String, storeType: Byte): String? {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.select(PROJECT_CODE).from(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType)).and(TYPE.eq(0)))
                .fetchOne(0, String::class.java)
        }
    }

    fun countInstalledProject(dslContext: DSLContext, projectCode: String, storeCode: String, storeType: Byte): Int {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.selectCount().from(this)
                .where(
                    PROJECT_CODE.eq(projectCode)
                        .and(STORE_CODE.eq(storeCode))
                        .and(STORE_TYPE.eq(storeType))
                )
                .fetchOne(0, Int::class.java)
        }
    }

    fun countInstalledProject(dslContext: DSLContext, storeCode: String, storeType: Byte): Int {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.selectCount().from(this)
                .where(
                    STORE_CODE.eq(storeCode)
                        .and(STORE_TYPE.eq(storeType))
                        .and(TYPE.eq(1))
                )
                .fetchOne(0, Int::class.java)
        }
    }

    /**
     * 根据商城组件标识和用户已授权的项目列表，查询已安装商城组件的项目列表
     */
    fun getInstalledProject(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        authorizedProjectCodeList: Set<String>
    ): Result<TStoreProjectRelRecord>? {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.selectFrom(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType)))
                .and(PROJECT_CODE.`in`(authorizedProjectCodeList))
                .fetch()
        }
    }

    /**
     * 获取项目下已安装的插件
     */
    fun getInstalledComponent(
        dslContext: DSLContext,
        projectCode: String,
        storeType: Byte
    ): Result<TStoreProjectRelRecord>? {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(STORE_TYPE.eq(storeType))
                .fetch()
        }
    }

    /**
     * 卸载时删除关联关系
     */
    fun deleteRel(dslContext: DSLContext, storeCode: String, storeType: Byte) {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            dslContext.deleteFrom(this).where(STORE_CODE.eq(storeCode)).and(STORE_TYPE.eq(storeType)).and(TYPE.eq(1))
                .execute()
        }
    }

    /**
     * 删除组件时删除关联关系
     */
    fun deleteAllRel(dslContext: DSLContext, storeCode: String, storeType: Byte) {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            dslContext.deleteFrom(this).where(STORE_CODE.eq(storeCode)).and(STORE_TYPE.eq(storeType)).execute()
        }
    }
}