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

import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result

/**
 * 插件数据库操作基类
 *
 * since: 2019-01-17
 */
@Suppress("ALL")
abstract class AtomBaseDao {

    /**
     * 设置插件市场可见插件查询条件
     */
    protected fun setAtomVisibleCondition(a: TAtom): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
//        conditions.add(a.DEFAULT_FLAG.eq(false)) // 需安装的
        conditions.add(a.ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte())) // 已发布的
        conditions.add(a.LATEST_FLAG.eq(true)) // 最新版本
        return conditions
    }

    fun getLatestAtomByCode(dslContext: DSLContext, atomCode: String): TAtomRecord? {
        return with(TAtom.T_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .and(LATEST_FLAG.eq(true))
                .fetchOne()
        }
    }

    fun getLatestAtomListByCodes(dslContext: DSLContext, atomCodes: List<String>): Result<TAtomRecord?> {
        return with(TAtom.T_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.`in`(atomCodes))
                .and(LATEST_FLAG.eq(true))
                .fetch()
        }
    }

    fun getNewestAtomByCode(dslContext: DSLContext, atomCode: String): TAtomRecord? {
        return with(TAtom.T_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun getMaxVersionAtomByCode(dslContext: DSLContext, atomCode: String): TAtomRecord? {
        return with(TAtom.T_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .orderBy(
                    JooqUtils.subStr(
                        str = VERSION,
                        delim = ".",
                        count = 1
                    ).plus(0).desc(),
                    JooqUtils.subStr(
                        str = JooqUtils.subStr(
                            str = VERSION,
                            delim = ".",
                            count = -2
                        ),
                        delim = ".",
                        count = 1
                    ).plus(0).desc(),
                    JooqUtils.subStr(
                        str = VERSION,
                        delim = ".",
                        count = -1
                    ).plus(0).desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun getSupportGitCiAtom(dslContext: DSLContext, os: String?, classType: String?): Result<Record1<String>> {
        return with(TAtom.T_ATOM) {
            val conditions = mutableListOf<Condition>()
            if (!os.isNullOrBlank()) {
                conditions.add(OS.eq(os))
            }
            if (!classType.isNullOrBlank()) {
                conditions.add(CLASS_TYPE.eq(classType))
            }
            conditions.add(JOB_TYPE.eq(JobTypeEnum.AGENT.name))
            conditions.add(ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte()))
            dslContext.selectDistinct(ATOM_CODE)
                .from(this)
                .where(conditions)
                .fetch()
        }
    }
}
