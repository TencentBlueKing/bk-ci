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

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TAtomEnvInfo
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.records.TAtomEnvInfoRecord
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

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
        atomStatusList: List<Byte>?
    ): Record? {
        val a = TAtom.T_ATOM.`as`("a")
        val b = TAtomEnvInfo.T_ATOM_ENV_INFO.`as`("b")
        val c = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("c")
        val defaultAtomCondition = queryDefaultAtomCondition(a, atomCode, version, atomStatusList)
        val normalAtomCondition = queryNormalAtomCondition(a, c, projectCode, atomCode, version, atomStatusList)
        val t = dslContext.select(
            a.ID.`as`("atomId"),
            a.ATOM_CODE.`as`("atomCode"),
            a.ATOM_STATUS.`as`("atomStatus"),
            a.NAME.`as`("atomName"),
            a.CREATOR.`as`("creator"),
            a.VERSION.`as`("version"),
            a.SUMMARY.`as`("summary"),
            a.DOCS_LINK.`as`("docsLink"),
            a.PROPS.`as`("props"),
            a.CREATE_TIME.`as`("createTime"),
            a.UPDATE_TIME.`as`("updateTime"),
            b.PKG_PATH.`as`("pkgPath"),
            b.LANGUAGE.`as`("language"),
            b.MIN_VERSION.`as`("minVersion"),
            b.TARGET.`as`("target"),
            b.SHA_CONTENT.`as`("shaContent"),
            b.PRE_CMD.`as`("preCmd")
        ).from(a)
            .join(b)
            .on(a.ID.eq(b.ATOM_ID))
            .where(defaultAtomCondition)
            .union(
                dslContext.select(
                    a.ID.`as`("atomId"),
                    a.ATOM_CODE.`as`("atomCode"),
                    a.ATOM_STATUS.`as`("atomStatus"),
                    a.NAME.`as`("atomName"),
                    a.CREATOR.`as`("creator"),
                    a.VERSION.`as`("version"),
                    a.SUMMARY.`as`("summary"),
                    a.DOCS_LINK.`as`("docsLink"),
                    a.PROPS.`as`("props"),
                    a.CREATE_TIME.`as`("createTime"),
                    a.UPDATE_TIME.`as`("updateTime"),
                    b.PKG_PATH.`as`("pkgPath"),
                    b.LANGUAGE.`as`("language"),
                    b.MIN_VERSION.`as`("minVersion"),
                    b.TARGET.`as`("target"),
                    b.SHA_CONTENT.`as`("shaContent"),
                    b.PRE_CMD.`as`("preCmd")
                ).from(a)
                    .join(b)
                    .on(a.ID.eq(b.ATOM_ID))
                    .join(c)
                    .on(a.ATOM_CODE.eq(c.STORE_CODE))
                    .where(normalAtomCondition)
            )
            .asTable("t")
        return dslContext.selectFrom(t).orderBy(t.field("createTime").desc()).limit(1).fetchOne()
    }

    private fun getBaseQueryCondition(
        a: TAtom,
        atomCode: String,
        version: String,
        atomStatusList: List<Byte>?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(a.ATOM_CODE.eq(atomCode))
        conditions.add(a.VERSION.like("$version%"))
        if (atomStatusList != null && atomStatusList.isNotEmpty()) {
            conditions.add(a.ATOM_STATUS.`in`(atomStatusList))
        }
        return conditions
    }

    private fun queryDefaultAtomCondition(
        a: TAtom,
        atomCode: String,
        version: String,
        atomStatusList: List<Byte>?
    ): MutableList<Condition> {
        val conditions = getBaseQueryCondition(a, atomCode, version, atomStatusList)
        conditions.add(a.DEFAULT_FLAG.eq(true)) // 查默认插件（所有项目都可用）
        return conditions
    }

    private fun queryNormalAtomCondition(
        a: TAtom,
        c: TStoreProjectRel,
        projectCode: String,
        atomCode: String,
        version: String,
        atomStatusList: List<Byte>?
    ): MutableList<Condition> {
        val conditions = getBaseQueryCondition(a, atomCode, version, atomStatusList)
        conditions.add(a.DEFAULT_FLAG.eq(false)) // 查普通插件
        conditions.add(c.PROJECT_CODE.eq(projectCode))
        conditions.add(c.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
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

            baseStep.set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, atomEnvRequest.userId)
                .where(ATOM_ID.eq(atomId))
                .execute()
        }
    }
}