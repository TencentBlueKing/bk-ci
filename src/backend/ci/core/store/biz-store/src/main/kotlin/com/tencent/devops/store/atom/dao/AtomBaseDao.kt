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

package com.tencent.devops.store.atom.dao

import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import com.tencent.devops.store.util.ServiceScopeUtil
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record1
import org.jooq.Result
import org.jooq.impl.DSL

/**
 * 插件数据库操作基类
 *
 * since: 2019-01-17
 */
@Suppress("ALL")
abstract class AtomBaseDao {

    /**
     * 构建 JSON_EXTRACT/JSON_CONTAINS 的路径，统一输出 "$.SCOPE_KEY" 形式。
     */
    private fun buildScopeJsonPath(scopeKey: String): String = "\$.$scopeKey"

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

    fun getNewestAtomByCode(dslContext: DSLContext, atomCode: String, branchTestFlag: Boolean = false): TAtomRecord? {
        return with(TAtom.T_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode).and(BRANCH_TEST_FLAG.eq(branchTestFlag)))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun getMaxVersionAtomByCode(
        dslContext: DSLContext,
        atomCode: String,
        atomStatus: AtomStatusEnum? = null
    ): TAtomRecord? {
        return with(TAtom.T_ATOM) {
            val conditions = mutableListOf<Condition>()
            conditions.add(ATOM_CODE.eq(atomCode))
            if (atomStatus != null) {
                conditions.add(ATOM_STATUS.eq(atomStatus.status.toByte()))
            }
            dslContext.selectFrom(this)
                .where(conditions)
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

    /**
     * 根据服务范围构建分类ID字段表达式
     *
     * - serviceScope 为 null 或 PIPELINE 时，使用 CLASSIFY_ID 字段（性能最好，有索引）
     * - serviceScope 为其他时，从 CLASSIFY_ID_MAP JSON 字段中提取对应的分类ID
     */
    protected fun buildClassifyIdField(
        ta: TAtom,
        serviceScope: ServiceScopeEnum?
    ): Field<String> {
        val normalizedScope = serviceScope?.let { ServiceScopeUtil.normalize(it.name) }
        return if (normalizedScope == null || normalizedScope == ServiceScopeEnum.PIPELINE.name) {
            ta.CLASSIFY_ID
        } else {
            DSL.field(
                "COALESCE(JSON_UNQUOTE(JSON_EXTRACT({0}, {1})), {2})",
                String::class.java,
                ta.CLASSIFY_ID_MAP,
                DSL.inline(buildScopeJsonPath(normalizedScope)),
                ta.CLASSIFY_ID
            )
        }
    }

    /**
     * 构建分类查询条件（支持多服务范围）
     *
     * 根据 serviceScope 参数动态构建分类过滤条件：
     * - 如果 serviceScope 是 PIPELINE，使用 CLASSIFY_ID 字段查询
     * - 如果 serviceScope 是其他，从 CLASSIFY_ID_MAP 中查询
     *
     * @param ta TAtom 表
     * @param classifyId 分类ID
     * @param serviceScope 服务范围
     * @return 查询条件，如果不需要过滤则返回 null
     */
    protected fun buildClassifyCondition(
        ta: TAtom,
        classifyId: String?,
        serviceScope: ServiceScopeEnum?
    ): Condition? {
        if (classifyId.isNullOrBlank()) return null
        val normalizedScope = serviceScope?.let { ServiceScopeUtil.normalize(it.name) }

        if (normalizedScope == null || normalizedScope == ServiceScopeEnum.PIPELINE.name) {
            val jsonExtractField = DSL.field(
                "JSON_UNQUOTE(JSON_EXTRACT({0}, {1}))",
                String::class.java,
                ta.CLASSIFY_ID_MAP,
                DSL.inline("\$.${ServiceScopeEnum.PIPELINE.name}")
            )
            return ta.CLASSIFY_ID.eq(classifyId).or(jsonExtractField.eq(classifyId))
        }

        val jsonExtractField = DSL.field(
            "JSON_UNQUOTE(JSON_EXTRACT({0}, {1}))",
            String::class.java,
            ta.CLASSIFY_ID_MAP,
            DSL.inline(buildScopeJsonPath(normalizedScope))
        )
        return jsonExtractField.eq(classifyId).or(ta.CLASSIFY_ID.eq(classifyId))
    }
}
