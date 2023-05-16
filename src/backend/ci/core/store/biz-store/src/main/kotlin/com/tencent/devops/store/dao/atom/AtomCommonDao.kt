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

package com.tencent.devops.store.dao.atom

import com.tencent.devops.common.api.constant.JS
import com.tencent.devops.common.api.constant.KEY_REPOSITORY_HASH_ID
import com.tencent.devops.common.api.constant.KEY_REPOSITORY_PATH
import com.tencent.devops.common.api.constant.KEY_SCRIPT
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TAtomEnvInfo
import com.tencent.devops.model.store.tables.TStoreBuildInfo
import com.tencent.devops.model.store.tables.TStorePipelineRel
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.process.utils.KEY_PIPELINE_ID
import com.tencent.devops.store.dao.common.AbstractStoreCommonDao
import com.tencent.devops.store.pojo.common.KEY_CODE_SRC
import com.tencent.devops.store.pojo.common.KEY_CREATOR
import com.tencent.devops.store.pojo.common.KEY_LANGUAGE
import com.tencent.devops.store.pojo.common.KEY_PROJECT_CODE
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository(value = "ATOM_COMMON_DAO")
class AtomCommonDao : AbstractStoreCommonDao() {

    override fun getStoreNameById(dslContext: DSLContext, storeId: String): String? {
        return with(TAtom.T_ATOM) {
            dslContext.select(NAME).from(this).where(ID.eq(storeId)).fetchOne(0, String::class.java)
        }
    }

    override fun getStoreNameByCode(dslContext: DSLContext, storeCode: String): String? {
        return with(TAtom.T_ATOM) {
            dslContext.select(NAME)
                .from(this)
                .where(ATOM_CODE.eq(storeCode).and(LATEST_FLAG.eq(true)))
                .fetchOne(0, String::class.java)
        }
    }

    override fun getNewestStoreNameByCode(dslContext: DSLContext, storeCode: String): String? {
        return with(TAtom.T_ATOM) {
            dslContext.select(NAME).from(this)
                .where(ATOM_CODE.eq(storeCode))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne(0, String::class.java)
        }
    }

    override fun getStorePublicFlagByCode(dslContext: DSLContext, storeCode: String): Boolean {
        return with(TAtom.T_ATOM) {
            dslContext.select(DEFAULT_FLAG).from(this)
                .where(ATOM_CODE.eq(storeCode).and(LATEST_FLAG.eq(true)))
                .fetchOne(0, Boolean::class.java)!!
        }
    }

    override fun getStoreCodeListByName(dslContext: DSLContext, storeName: String): Result<out Record>? {
        return with(TAtom.T_ATOM) {
            dslContext.select(ATOM_CODE.`as`("storeCode")).from(this)
                .where(NAME.contains(storeName))
                .groupBy(ATOM_CODE)
                .fetch()
        }
    }

    override fun getLatestStoreInfoListByCodes(
        dslContext: DSLContext,
        storeCodeList: List<String>
    ): Result<out Record>? {
        val ta = TAtom.T_ATOM
        val taei = TAtomEnvInfo.T_ATOM_ENV_INFO
        val tsbi = TStoreBuildInfo.T_STORE_BUILD_INFO
        val tspr = TStoreProjectRel.T_STORE_PROJECT_REL
        val tspir = TStorePipelineRel.T_STORE_PIPELINE_REL
        return dslContext.select(
            ta.ATOM_CODE.`as`(KEY_STORE_CODE),
            ta.VERSION.`as`(KEY_VERSION),
            ta.REPOSITORY_HASH_ID.`as`(KEY_REPOSITORY_HASH_ID),
            ta.CODE_SRC.`as`(KEY_CODE_SRC),
            taei.LANGUAGE.`as`(KEY_LANGUAGE),
            tsbi.SCRIPT.`as`(KEY_SCRIPT),
            tsbi.REPOSITORY_PATH.`as`(KEY_REPOSITORY_PATH),
            tspr.PROJECT_CODE.`as`(KEY_PROJECT_CODE),
            tspr.CREATOR.`as`(KEY_CREATOR),
            tspir.PIPELINE_ID.`as`(KEY_PIPELINE_ID)
        ).from(ta).leftJoin(taei).on(ta.ID.eq(taei.ATOM_ID))
            .join(tsbi).on(taei.LANGUAGE.eq(tsbi.LANGUAGE))
            .join(tspr).on(ta.ATOM_CODE.eq(tspr.STORE_CODE).and(tsbi.STORE_TYPE.eq(tspr.STORE_TYPE)))
            .join(tspir).on(ta.ATOM_CODE.eq(tspir.STORE_CODE).and(tsbi.STORE_TYPE.eq(tspir.STORE_TYPE)))
            .where(tsbi.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
            .and(ta.LATEST_FLAG.eq(true))
            .and(tspr.TYPE.eq(StoreProjectTypeEnum.INIT.type.toByte()))
            .and(ta.ATOM_CODE.`in`(storeCodeList))
            .and(taei.DEFAULT_FLAG.eq(true))
            .fetch()
    }

    override fun getStoreDevLanguages(dslContext: DSLContext, storeCode: String): List<String>? {
        val tAtom = TAtom.T_ATOM
        val tAtomEnvInfo = TAtomEnvInfo.T_ATOM_ENV_INFO
        val conditions = mutableListOf<Condition>()
        conditions.add(tAtom.ATOM_CODE.eq(storeCode))
        conditions.add(tAtom.LATEST_FLAG.eq(true))
        conditions.add(tAtomEnvInfo.DEFAULT_FLAG.eq(true))
        val record = dslContext.select(
            tAtom.HTML_TEMPLATE_VERSION,
            tAtomEnvInfo.LANGUAGE
        ).from(tAtom).join(tAtomEnvInfo).on(tAtom.ID.eq(tAtomEnvInfo.ATOM_ID))
            .where(conditions)
            .limit(1)
            .fetchOne()!!
        val htmlTemplateVersion = record[0] as String
        val language = record[1] as String
        return if (htmlTemplateVersion == FrontendTypeEnum.SPECIAL.typeVersion) {
            arrayListOf(language, JS)
        } else {
            arrayListOf(language)
        }
    }

    override fun getNewestStoreBaseInfoByCode(
        dslContext: DSLContext,
        storeCode: String,
        storeStatus: Byte?
    ): StoreBaseInfo? {
        return with(TAtom.T_ATOM) {
            val conditions = mutableListOf<Condition>()
            conditions.add(ATOM_CODE.eq(storeCode))
            if (storeStatus != null) {
                conditions.add(ATOM_STATUS.eq(storeStatus))
            }
            val atomRecord = dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
            if (atomRecord != null) {
                StoreBaseInfo(
                    storeId = atomRecord.id,
                    storeCode = atomRecord.atomCode,
                    storeName = atomRecord.name,
                    version = atomRecord.version,
                    publicFlag = atomRecord.defaultFlag
                )
            } else {
                null
            }
        }
    }
}
