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

package com.tencent.devops.store.dao.atom

import com.tencent.devops.common.api.constant.JS
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TAtomEnvInfo
import com.tencent.devops.model.store.tables.TStoreBuildInfo
import com.tencent.devops.model.store.tables.TStorePipelineRel
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.store.dao.common.AbstractStoreCommonDao
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
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

    override fun getNewestStoreNameByCode(dslContext: DSLContext, storeCode: String): String? {
        return with(TAtom.T_ATOM) {
            dslContext.select(NAME).from(this)
                .where(ATOM_CODE.eq(storeCode))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne(0, String::class.java)
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
        val ta = TAtom.T_ATOM.`as`("ta")
        val taei = TAtomEnvInfo.T_ATOM_ENV_INFO.`as`("taei")
        val tsbi = TStoreBuildInfo.T_STORE_BUILD_INFO.`as`("tsbi")
        val tspr = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("tspr")
        val tspir = TStorePipelineRel.T_STORE_PIPELINE_REL.`as`("tspir")
        return dslContext.select(
            ta.ATOM_CODE.`as`("storeCode"),
            ta.VERSION.`as`("version"),
            ta.REPOSITORY_HASH_ID.`as`("repositoryHashId"),
            ta.CODE_SRC.`as`("codeSrc"),
            taei.LANGUAGE.`as`("language"),
            tsbi.SCRIPT.`as`("script"),
            tsbi.REPOSITORY_PATH.`as`("repositoryPath"),
            tspr.PROJECT_CODE.`as`("projectCode"),
            tspr.CREATOR.`as`("creator"),
            tspir.PIPELINE_ID.`as`("pipelineId")
        ).from(ta).join(taei).on(ta.ID.eq(taei.ATOM_ID))
            .join(tsbi).on(taei.LANGUAGE.eq(tsbi.LANGUAGE))
            .join(tspr).on(ta.ATOM_CODE.eq(tspr.STORE_CODE).and(tsbi.STORE_TYPE.eq(tspr.STORE_TYPE)))
            .join(tspir).on(ta.ATOM_CODE.eq(tspir.STORE_CODE).and(tsbi.STORE_TYPE.eq(tspir.STORE_TYPE)))
            .where(tsbi.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
            .and(ta.LATEST_FLAG.eq(true))
            .and(tspr.TYPE.eq(StoreProjectTypeEnum.INIT.type.toByte()))
            .and(ta.ATOM_CODE.`in`(storeCodeList))
            .fetch()
    }

    override fun getStoreDevLanguages(dslContext: DSLContext, storeCode: String): List<String>? {
        val ta = TAtom.T_ATOM.`as`("ta")
        val taei = TAtomEnvInfo.T_ATOM_ENV_INFO.`as`("taei")
        val record = dslContext.select(
            ta.HTML_TEMPLATE_VERSION.`as`("htmlTemplateVersion"),
            taei.LANGUAGE.`as`("language")
        ).from(ta).join(taei).on(ta.ID.eq(taei.ATOM_ID))
            .where(ta.ATOM_CODE.eq(storeCode).and(ta.LATEST_FLAG.eq(true)))
            .fetchOne()
        val htmlTemplateVersion = record[0] as String
        val language = record[1] as String
        return if (htmlTemplateVersion == FrontendTypeEnum.SPECIAL.typeVersion) {
            arrayListOf(language, JS)
        } else {
            arrayListOf(language)
        }
    }
}