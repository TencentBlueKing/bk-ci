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
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.ServiceScopeConfig
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
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
    private fun buildScopeJsonPath(scopeKey: String): String = "\$.${scopeKey}"

    /**
     * 构建 JSON_CONTAINS 条件（避免 DSL.inline 对双引号的转义问题）。
     * 直接使用 DSL.condition 构造原始 SQL 条件，确保 JSON_CONTAINS 的参数正确传递。
     *
     * @param jsonField JSON 字段
     * @param value JSON 值（不含外层引号，如 CREATIVE_STREAM）
     * @param path JSON 路径，如 $.CREATIVE_STREAM；为 null 时不传路径参数
     * @return JSON_CONTAINS(...) = 1 条件
     */
    private fun jsonContainsCondition(
        jsonField: Field<*>,
        value: String,
        path: String? = null
    ): Condition {
        return if (path != null) {
            DSL.condition(
                "JSON_CONTAINS({0}, CONCAT('\"', {1}, '\"'), {2})",
                jsonField,
                DSL.inline(value),
                DSL.inline(path)
            )
        } else {
            DSL.condition(
                "JSON_CONTAINS({0}, CONCAT('\"', {1}, '\"'))",
                jsonField,
                DSL.inline(value)
            )
        }
    }

    /**
     * 构建 JSON_VALID 条件
     */
    private fun jsonValidCondition(jsonField: Field<*>): Condition {
        return DSL.condition("JSON_VALID({0})", jsonField)
    }

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
        val ta = TAtom.T_ATOM
        val conditions = mutableListOf<Condition>()
        if (!os.isNullOrBlank()) {
            conditions.add(ta.OS.eq(os))
        }
        if (!classType.isNullOrBlank()) {
            conditions.add(ta.CLASS_TYPE.eq(classType))
        }
        buildJobTypeCondition(ta, JobTypeEnum.AGENT.name, ServiceScopeEnum.PIPELINE)?.let {
            conditions.add(it)
        }
        conditions.add(ta.ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte()))
        return dslContext.selectDistinct(ta.ATOM_CODE)
            .from(ta)
            .where(conditions)
            .fetch()
    }

    /**
     * 根据 classifyCode 查询 CLASSIFY_ID 的公共方法
     * 
     * @param dslContext DSL上下文
     * @param classifyCode 分类代码
     * @param serviceScope 服务范围，为 null 时不按 scope 过滤
     * @return CLASSIFY_ID，如果未找到则返回null
     */
    protected fun getClassifyIdByCode(
        dslContext: DSLContext,
        classifyCode: String?,
        serviceScope: ServiceScopeEnum? = null
    ): String? {
        if (classifyCode.isNullOrBlank()) {
            return null
        }
        val tClassify = TClassify.T_CLASSIFY
        val query = dslContext.select(tClassify.ID)
            .from(tClassify)
            .where(tClassify.CLASSIFY_CODE.eq(classifyCode).and(tClassify.TYPE.eq(StoreTypeEnum.ATOM.type.toByte())))
        if (serviceScope != null) {
            query.and(tClassify.SERVICE_SCOPE.eq(serviceScope.name))
        }
        return query.limit(1).fetchOne(0, String::class.java)
    }

    /**
     * 从 ServiceScopeConfig 列表构建 scope → classifyId 映射。
     * 通过 classifyCode + serviceScope 查询 T_CLASSIFY 表解析出 classifyId。
     */
    protected fun buildClassifyIdMapFromConfigs(
        dslContext: DSLContext,
        configs: List<ServiceScopeConfig>
    ): Map<String, String> {
        return configs.mapNotNull { config ->
            getClassifyIdByCode(dslContext, config.classifyCode, config.serviceScope)
                ?.let { id ->
                    (ServiceScopeUtil.normalize(config.serviceScope.name) ?: config.serviceScope.name) to id
                }
        }.toMap()
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

    /**
     * 构建 T_ATOM 和 T_CLASSIFY 的关联条件（支持 CLASSIFY_ID_MAP）
     * 
     * 根据 serviceScope 参数动态构建关联条件：
     * - 如果 serviceScope 是 PIPELINE，优先使用 CLASSIFY_ID 字段关联
     * - 如果 serviceScope 是其他，从 CLASSIFY_ID_MAP 中提取对应的分类ID进行关联
     * 
     * @param ta TAtom 表
     * @param tc TClassify 表
     * @param serviceScope 服务范围，如果为null则默认使用 PIPELINE
     * @return 关联条件
     */
    protected fun buildClassifyJoinCondition(
        ta: TAtom,
        tc: TClassify,
        serviceScope: ServiceScopeEnum? = null
    ): Condition {
        val classifyIdField = buildClassifyIdField(ta, serviceScope)
        return classifyIdField.eq(tc.ID)
    }

    /**
     * 构建 JOB_TYPE / JOB_TYPE_MAP 查询条件（双字段策略，兼容多环境滚动发布）。
     *
     * JOB_TYPE 字段只存纯字符串（PIPELINE 范围），不会出现 JSON。
     * - PIPELINE scope（或 scope 为 null）：直接匹配 JOB_TYPE 纯字符串，或从 JOB_TYPE_MAP 中匹配
     * - 非 PIPELINE scope：仅从 JOB_TYPE_MAP（JSON）中匹配
     *
     * @param ta TAtom 表
     * @param jobType 要筛选的 Job 类型名称（如 AGENT、AGENT_LESS、CREATIVE_STREAM、CLOUD_TASK）
     * @param serviceScope 服务范围，null 视为 PIPELINE
     * @param queryFitAgentBuildLessAtomFlag 仅 PIPELINE+AGENT 时有效：true 表示同时匹配 BUILD_LESS_RUN_FLAG=true；false 表示排除无编译；null 不附加
     * @return 查询条件，若 jobType 为空则返回 null
     */
    protected fun buildJobTypeCondition(
        ta: TAtom,
        jobType: String?,
        serviceScope: ServiceScopeEnum?,
        queryFitAgentBuildLessAtomFlag: Boolean? = null
    ): Condition? {
        if (jobType.isNullOrBlank()) return null
        val normalizedScope = serviceScope?.let { ServiceScopeUtil.normalize(it.name) }

        val jobTypeMatchCondition = if (normalizedScope == null || normalizedScope == ServiceScopeEnum.PIPELINE.name) {
            // PIPELINE scope: JOB_TYPE 是纯字符串，直接等值匹配；或从 JOB_TYPE_MAP 匹配
            val isMapValid = jsonValidCondition(ta.JOB_TYPE_MAP)
            val mapJsonContains = jsonContainsCondition(
                jsonField = ta.JOB_TYPE_MAP,
                value = jobType,
                path = buildScopeJsonPath(ServiceScopeEnum.PIPELINE.name)
            )
            ta.JOB_TYPE.eq(jobType).or(isMapValid.and(mapJsonContains))
        } else {
            // 非 PIPELINE scope: 仅从 JOB_TYPE_MAP 中按 scope 查找（JOB_TYPE 只存 PIPELINE 纯字符串，无需回退）
            val isMapValid = jsonValidCondition(ta.JOB_TYPE_MAP)
            val mapJsonContains = jsonContainsCondition(
                jsonField = ta.JOB_TYPE_MAP,
                value = jobType,
                path = buildScopeJsonPath(normalizedScope)
            )
            isMapValid.and(mapJsonContains)
        }

        val isBuildEnvJobType = runCatching { JobTypeEnum.valueOf(jobType).isBuildEnv() }.getOrDefault(false)
        if (isBuildEnvJobType && queryFitAgentBuildLessAtomFlag != null) {
            return when (queryFitAgentBuildLessAtomFlag) {
                true -> jobTypeMatchCondition.or(ta.BUILD_LESS_RUN_FLAG.eq(true))
                false -> jobTypeMatchCondition.and(ta.BUILD_LESS_RUN_FLAG.ne(true).or(ta.BUILD_LESS_RUN_FLAG.isNull))
            }
        }
        return jobTypeMatchCondition
    }

    /**
     * 按服务范围返回“无编译环境”对应的 JOB_TYPE 值（用于 os=all 且 fitOsFlag=false 时的筛选）。
     * PIPELINE 为 AGENT_LESS；创作流（CREATIVE_STREAM）为 CLOUD_TASK；其他 scope 返回 null。
     */
    protected fun getAgentLessJobTypeForScope(serviceScope: ServiceScopeEnum?): String? {
        val normalizedScope = serviceScope?.let { ServiceScopeUtil.normalize(it.name) } ?: return null
        return when (normalizedScope) {
            ServiceScopeEnum.PIPELINE.name -> JobTypeEnum.AGENT_LESS.name
            ServiceScopeEnum.CREATIVE_STREAM.name -> JobTypeEnum.CLOUD_TASK.name
            else -> null
        }
    }
}
