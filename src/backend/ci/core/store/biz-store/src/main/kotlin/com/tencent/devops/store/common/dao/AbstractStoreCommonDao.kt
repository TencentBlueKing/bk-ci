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

import com.tencent.devops.store.pojo.common.StoreBaseInfo
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result

@Suppress("ALL")
abstract class AbstractStoreCommonDao {

    abstract fun getStoreNameById(
        dslContext: DSLContext,
        storeId: String
    ): String?

    abstract fun getStoreNameByCode(
        dslContext: DSLContext,
        storeCode: String
    ): String?

    abstract fun getNewestStoreNameByCode(
        dslContext: DSLContext,
        storeCode: String
    ): String?

    abstract fun getStorePublicFlagByCode(
        dslContext: DSLContext,
        storeCode: String
    ): Boolean

    abstract fun getStoreCodeListByName(
        dslContext: DSLContext,
        storeName: String
    ): Result<out Record>?

    abstract fun getLatestStoreInfoListByCodes(
        dslContext: DSLContext,
        storeCodeList: List<String>
    ): Result<out Record>?

    abstract fun getStoreDevLanguages(
        dslContext: DSLContext,
        storeCode: String
    ): List<String>?

    abstract fun getNewestStoreBaseInfoByCode(
        dslContext: DSLContext,
        storeCode: String,
        storeStatus: Byte? = null
    ): StoreBaseInfo?

    abstract fun getStoreRepoHashIdByCode(dslContext: DSLContext, storeCode: String): String?

    abstract fun getStoreCodeById(
        dslContext: DSLContext,
        storeId: String
    ): String?
}
