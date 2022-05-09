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

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TAtomEnvInfo
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.records.TAtomEnvInfoRecord
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.utils.VersionUtils
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record21
import org.jooq.SelectOnConditionStep
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class MarketAtomEnvInfoDao {

    fun addMarketAtomEnvInfo(dslContext: DSLContext, atomId: String, atomEnvRequest: AtomEnvRequest) {
        with(TAtomEnvInfo.T_ATOM_ENV_INFO) {
            dslContext.insertInto(
                this,
                ID,
                ATOM_ID,
                PKG_NAME,
                PKG_PATH,
                LANGUAGE,
                MIN_VERSION,
                TARGET,
                SHA_CONTENT,
                PRE_CMD,
                POST_ENTRY_PARAM,
                POST_CONDITION,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    atomId,
                    atomEnvRequest.pkgName,
                    atomEnvRequest.pkgPath,
                    atomEnvRequest.language,
                    atomEnvRequest.minVersion,
                    atomEnvRequest.target,
                    atomEnvRequest.shaContent,
                    atomEnvRequest.preCmd,
                    atomEnvRequest.atomPostInfo?.postEntryParam,
                    atomEnvRequest.atomPostInfo?.postCondition,
                    atomEnvRequest.userId,
                    atomEnvRequest.userId
                ).execute()
        }
    }

    fun getProjectMarketAtomEnvInfo(
        dslContext: DSLContext,
        projectCode: String,
        atomCode: String,
        version: String,
        atomDefaultFlag: Boolean,
        atomStatusList: List<Byte>?
    ): Record? {
        val tAtom = TAtom.T_ATOM
        val tAtomEnvInfo = TAtomEnvInfo.T_ATOM_ENV_INFO
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        return if (atomDefaultFlag) {
            getAtomEnvInfoBaseStep(dslContext, tAtom, tAtomEnvInfo)
                .where(queryDefaultAtomCondition(
                    tAtom = tAtom,
                    atomCode = atomCode,
                    version = version,
                    atomStatusList = atomStatusList
                )).orderBy(tAtom.CREATE_TIME.desc()).limit(1).fetchOne()
        } else {
            getAtomEnvInfoBaseStep(dslContext, tAtom, tAtomEnvInfo)
                .join(tStoreProjectRel)
                .on(tAtom.ATOM_CODE.eq(tStoreProjectRel.STORE_CODE))
                .where(queryNormalAtomCondition(
                    tAtom = tAtom,
                    tStoreProjectRel = tStoreProjectRel,
                    projectCode = projectCode,
                    atomCode = atomCode,
                    version = version,
                    atomStatusList = atomStatusList
                )).orderBy(tAtom.CREATE_TIME.desc()).limit(1).fetchOne()
        }
    }

    private fun getAtomEnvInfoBaseStep(
        dslContext: DSLContext,
        tAtom: TAtom,
        tAtomEnvInfo: TAtomEnvInfo
    ): SelectOnConditionStep<Record21<String, String, Byte, String, String, String, Boolean, String, String, Boolean, String, LocalDateTime, LocalDateTime, String, String, String, String, String, String, String, String>> {
        return dslContext.select(
            tAtom.ID,
            tAtom.ATOM_CODE,
            tAtom.ATOM_STATUS,
            tAtom.NAME,
            tAtom.CREATOR,
            tAtom.VERSION,
            tAtom.DEFAULT_FLAG,
            tAtom.SUMMARY,
            tAtom.DOCS_LINK,
            tAtom.BUILD_LESS_RUN_FLAG,
            tAtom.JOB_TYPE,
            tAtom.CREATE_TIME,
            tAtom.UPDATE_TIME,
            tAtomEnvInfo.PKG_PATH,
            tAtomEnvInfo.LANGUAGE,
            tAtomEnvInfo.MIN_VERSION,
            tAtomEnvInfo.TARGET,
            tAtomEnvInfo.SHA_CONTENT,
            tAtomEnvInfo.PRE_CMD,
            tAtomEnvInfo.POST_ENTRY_PARAM,
            tAtomEnvInfo.POST_CONDITION
        ).from(tAtom)
            .join(tAtomEnvInfo)
            .on(tAtom.ID.eq(tAtomEnvInfo.ATOM_ID))
    }

    private fun getBaseQueryCondition(
        tAtom: TAtom,
        atomCode: String,
        version: String,
        atomStatusList: List<Byte>?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(tAtom.ATOM_CODE.eq(atomCode))
        conditions.add(tAtom.VERSION.like(VersionUtils.generateQueryVersion(version)))
        if (atomStatusList != null && atomStatusList.isNotEmpty()) {
            conditions.add(tAtom.ATOM_STATUS.`in`(atomStatusList))
        }
        return conditions
    }

    private fun queryDefaultAtomCondition(
        tAtom: TAtom,
        atomCode: String,
        version: String,
        atomStatusList: List<Byte>?
    ): MutableList<Condition> {
        val conditions = getBaseQueryCondition(tAtom, atomCode, version, atomStatusList)
        conditions.add(tAtom.DEFAULT_FLAG.eq(true)) // 查默认插件（所有项目都可用）
        return conditions
    }

    private fun queryNormalAtomCondition(
        tAtom: TAtom,
        tStoreProjectRel: TStoreProjectRel,
        projectCode: String,
        atomCode: String,
        version: String,
        atomStatusList: List<Byte>?
    ): MutableList<Condition> {
        val conditions = getBaseQueryCondition(tAtom, atomCode, version, atomStatusList)
        conditions.add(tAtom.DEFAULT_FLAG.eq(false)) // 查普通插件
        conditions.add(tStoreProjectRel.PROJECT_CODE.eq(projectCode))
        conditions.add(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
        return conditions
    }

    fun getMarketAtomEnvInfoByAtomId(dslContext: DSLContext, atomId: String): TAtomEnvInfoRecord? {
        return with(TAtomEnvInfo.T_ATOM_ENV_INFO) {
            dslContext.selectFrom(this)
                .where(ATOM_ID.eq(atomId))
                .fetchOne()
        }
    }

    fun updateMarketAtomEnvInfo(dslContext: DSLContext, atomId: String, atomEnvRequest: AtomEnvRequest) {
        with(TAtomEnvInfo.T_ATOM_ENV_INFO) {
            val baseStep = dslContext.update(this)
                .set(PKG_PATH, atomEnvRequest.pkgPath)
            if (!atomEnvRequest.language.isNullOrEmpty()) {
                baseStep.set(LANGUAGE, atomEnvRequest.language)
            }
            if (!atomEnvRequest.minVersion.isNullOrEmpty()) {
                baseStep.set(MIN_VERSION, atomEnvRequest.minVersion)
            }
            if (!atomEnvRequest.target.isNullOrEmpty()) {
                baseStep.set(TARGET, atomEnvRequest.target)
            }
            if (!atomEnvRequest.shaContent.isNullOrEmpty()) {
                baseStep.set(SHA_CONTENT, atomEnvRequest.shaContent)
            }
            if (!atomEnvRequest.preCmd.isNullOrEmpty()) {
                baseStep.set(PRE_CMD, atomEnvRequest.preCmd)
            }
            if (!atomEnvRequest.pkgName.isNullOrEmpty()) {
                baseStep.set(PKG_NAME, atomEnvRequest.pkgName)
            }
            val atomPostInfo = atomEnvRequest.atomPostInfo
            baseStep.set(POST_ENTRY_PARAM, atomPostInfo?.postEntryParam)
            baseStep.set(POST_CONDITION, atomPostInfo?.postCondition)
            baseStep.set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, atomEnvRequest.userId)
                .where(ATOM_ID.eq(atomId))
                .execute()
        }
    }

    fun deleteAtomEnvInfo(dslContext: DSLContext, atomCode: String) {
        val ta = TAtom.T_ATOM
        val atomIds = dslContext.select(ta.ID).from(ta).where(ta.ATOM_CODE.eq(atomCode)).fetch()
        with(TAtomEnvInfo.T_ATOM_ENV_INFO) {
            dslContext.deleteFrom(this)
                .where(ATOM_ID.`in`(atomIds))
                .execute()
        }
    }
}
