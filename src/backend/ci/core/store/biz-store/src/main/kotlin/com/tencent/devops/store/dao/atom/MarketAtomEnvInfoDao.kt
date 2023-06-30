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
import org.jooq.Record13
import org.jooq.Result
import org.jooq.SelectJoinStep
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class MarketAtomEnvInfoDao {

    fun addMarketAtomEnvInfo(dslContext: DSLContext, atomId: String, atomEnvRequests: List<AtomEnvRequest>) {
        with(TAtomEnvInfo.T_ATOM_ENV_INFO) {
            atomEnvRequests.forEach { atomEnvRequest ->
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
                    OS_NAME,
                    OS_ARCH,
                    RUNTIME_VERSION,
                    DEFAULT_FLAG,
                    FINISH_KILL_FLAG,
                    CREATOR,
                    MODIFIER
                )
                    .values(
                        UUIDUtil.generate(),
                        atomId,
                        atomEnvRequest.pkgName,
                        atomEnvRequest.pkgRepoPath,
                        atomEnvRequest.language,
                        atomEnvRequest.minVersion,
                        atomEnvRequest.target,
                        atomEnvRequest.shaContent,
                        atomEnvRequest.preCmd,
                        atomEnvRequest.atomPostInfo?.postEntryParam,
                        atomEnvRequest.atomPostInfo?.postCondition,
                        atomEnvRequest.osName,
                        atomEnvRequest.osArch,
                        atomEnvRequest.runtimeVersion,
                        atomEnvRequest.defaultFlag,
                        atomEnvRequest.finishKillFlag,
                        atomEnvRequest.userId,
                        atomEnvRequest.userId
                    ).execute()
            }
        }
    }

    fun getProjectAtomBaseInfo(
        dslContext: DSLContext,
        projectCode: String,
        atomCode: String,
        version: String,
        atomDefaultFlag: Boolean,
        atomStatusList: List<Byte>?
    ): Record? {
        val tAtom = TAtom.T_ATOM
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        return if (atomDefaultFlag) {
            getAtomBaseInfoStep(dslContext, tAtom)
                .where(queryDefaultAtomCondition(
                    tAtom = tAtom,
                    atomCode = atomCode,
                    version = version,
                    atomStatusList = atomStatusList
                )).orderBy(tAtom.CREATE_TIME.desc()).limit(1).fetchOne()
        } else {
            getAtomBaseInfoStep(dslContext, tAtom)
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

    private fun getAtomBaseInfoStep(
        dslContext: DSLContext,
        tAtom: TAtom
    ): SelectJoinStep<Record13<String, String, Byte, String, String, String, Boolean, String, String, Boolean, String, LocalDateTime, LocalDateTime>> {
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
            tAtom.UPDATE_TIME
        ).from(tAtom)
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

    fun getAtomEnvInfo(
        dslContext: DSLContext,
        atomId: String,
        osName: String? = null,
        osArch: String? = null
    ): TAtomEnvInfoRecord? {
        return with(TAtomEnvInfo.T_ATOM_ENV_INFO) {
            val conditions = mutableListOf<Condition>()
            conditions.add(ATOM_ID.eq(atomId))
            if (!osName.isNullOrBlank()) {
                conditions.add(OS_NAME.eq(osName))
            }
            if (!osArch.isNullOrBlank()) {
                conditions.add(OS_ARCH.eq(osArch))
            }
            dslContext.selectFrom(this)
                .where(conditions)
                .limit(1)
                .fetchOne()
        }
    }

    fun getNewestAtomEnvInfo(dslContext: DSLContext, atomId: String): TAtomEnvInfoRecord? {
        return with(TAtomEnvInfo.T_ATOM_ENV_INFO) {
            dslContext.selectFrom(this)
                .where(ATOM_ID.eq(atomId))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun getDefaultAtomEnvInfo(
        dslContext: DSLContext,
        atomId: String,
        osName: String? = null
    ): TAtomEnvInfoRecord? {
        return with(TAtomEnvInfo.T_ATOM_ENV_INFO) {
            val conditions = mutableListOf<Condition>()
            conditions.add(ATOM_ID.eq(atomId))
            conditions.add(DEFAULT_FLAG.eq(true))
            if (!osName.isNullOrBlank()) {
                conditions.add(OS_NAME.eq(osName))
            }
            dslContext.selectFrom(this)
                .where(conditions)
                .limit(1)
                .fetchOne()
        }
    }

    fun getMarketAtomEnvInfosByAtomId(dslContext: DSLContext, atomId: String): Result<TAtomEnvInfoRecord>? {
        return with(TAtomEnvInfo.T_ATOM_ENV_INFO) {
            dslContext.selectFrom(this)
                .where(ATOM_ID.eq(atomId))
                .fetch()
        }
    }

    fun updateMarketAtomEnvInfo(dslContext: DSLContext, atomId: String, atomEnvRequest: AtomEnvRequest) {
        with(TAtomEnvInfo.T_ATOM_ENV_INFO) {
            val baseStep = dslContext.update(this)
                .set(PKG_PATH, atomEnvRequest.pkgRepoPath)
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
            if (!atomEnvRequest.runtimeVersion.isNullOrEmpty()) {
                baseStep.set(RUNTIME_VERSION, atomEnvRequest.runtimeVersion)
            }
            atomEnvRequest.defaultFlag?.let { baseStep.set(DEFAULT_FLAG, it) }
            atomEnvRequest.finishKillFlag?.let { baseStep.set(FINISH_KILL_FLAG, it) }
            val atomPostInfo = atomEnvRequest.atomPostInfo
            baseStep.set(POST_ENTRY_PARAM, atomPostInfo?.postEntryParam)
            baseStep.set(POST_CONDITION, atomPostInfo?.postCondition)
            val conditions = mutableListOf<Condition>()
            conditions.add(ATOM_ID.eq(atomId))
            if (!atomEnvRequest.osName.isNullOrBlank()) {
                conditions.add(OS_NAME.eq(atomEnvRequest.osName))
            }
            if (!atomEnvRequest.osArch.isNullOrBlank()) {
                conditions.add(OS_ARCH.eq(atomEnvRequest.osArch))
            }
            baseStep.set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, atomEnvRequest.userId)
                .where(conditions)
                .execute()
        }
    }

    fun deleteAtomEnvInfoById(dslContext: DSLContext, atomId: String) {
        with(TAtomEnvInfo.T_ATOM_ENV_INFO) {
            dslContext.deleteFrom(this)
                .where(ATOM_ID.eq(atomId))
                .execute()
        }
    }

    fun deleteAtomEnvInfoByCode(dslContext: DSLContext, atomCode: String) {
        val ta = TAtom.T_ATOM
        val atomIds = dslContext.select(ta.ID).from(ta).where(ta.ATOM_CODE.eq(atomCode)).fetch()
        with(TAtomEnvInfo.T_ATOM_ENV_INFO) {
            dslContext.deleteFrom(this)
                .where(ATOM_ID.`in`(atomIds))
                .execute()
        }
    }

    fun getAtomLanguage(dslContext: DSLContext, atomCode: String, version: String): String? {
        val tAtom = TAtom.T_ATOM
        val tAtomEnvInfo = TAtomEnvInfo.T_ATOM_ENV_INFO
        return dslContext.select(tAtomEnvInfo.LANGUAGE)
            .from(tAtomEnvInfo)
            .join(tAtom)
            .on(tAtomEnvInfo.ATOM_ID.eq(tAtom.ID))
            .where(tAtom.ATOM_CODE.eq(atomCode).and(tAtom.VERSION.eq(version)))
            .fetchOne(0, String::class.java)
    }
}
