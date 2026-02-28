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
     * 根据 classifyCode 查询 CLASSIFY_ID 的公共方法
     * 
     * @param dslContext DSL上下文
     * @param classifyCode 分类代码
     * @param serviceScope 服务范围（可选），用于过滤分类
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
        query.and(tClassify.SERVICE_SCOPE.eq(serviceScope?.name ?: ServiceScopeEnum.PIPELINE.name))
        return query.fetchOne(0, String::class.java)
    }

    /**
     * 根据服务范围构建分类ID字段表达式
     * 
     * 如果 serviceScope 是 PIPELINE，使用 CLASSIFY_ID 字段（性能最好，有索引）
     * 如果 serviceScope 是其他，从 CLASSIFY_ID_MAP JSON 字段中提取对应的分类ID
     * 
     * @param ta TAtom 表
     * @param serviceScope 服务范围，如 "PIPELINE"、"CREATIVE_STREAM"，如果为null则默认使用 PIPELINE
     * @return 分类ID字段表达式（Field<String>）
     */
    protected fun buildClassifyIdField(
        ta: TAtom,
        serviceScope: ServiceScopeEnum?
    ): Field<String> {
        val normalizedScope = ServiceScopeUtil.normalize(serviceScope?.name) ?: ServiceScopeEnum.PIPELINE.name
        
        return if (normalizedScope == ServiceScopeEnum.PIPELINE.name) {
            // PIPELINE 使用 CLASSIFY_ID 字段（性能最好，有索引）
            ta.CLASSIFY_ID
        } else {
            // 其他服务范围从 CLASSIFY_ID_MAP 中提取
            // 使用 COALESCE 回退到 CLASSIFY_ID（兼容处理）
            DSL.field(
                "COALESCE(JSON_UNQUOTE(JSON_EXTRACT({0}, {1})), {2})",
                String::class.java,
                ta.CLASSIFY_ID_MAP,
                DSL.inline("$.$normalizedScope"),
                ta.CLASSIFY_ID  // 如果未找到，回退到 CLASSIFY_ID
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
        val servicePipelineScopeName = ServiceScopeEnum.PIPELINE.name
        val normalizedScope = ServiceScopeUtil.normalize(serviceScope?.name) ?: servicePipelineScopeName

        // 构建 JSON_EXTRACT 表达式
        val jsonExtractField = DSL.field(
            "JSON_UNQUOTE(JSON_EXTRACT({0}, {1}))",
            String::class.java,
            ta.CLASSIFY_ID_MAP,
            DSL.inline("$.$normalizedScope")
        )

        return if (normalizedScope == servicePipelineScopeName) {
            // PIPELINE: 使用 CLASSIFY_ID 或 CLASSIFY_ID_MAP
            ta.CLASSIFY_ID.eq(classifyId).or(jsonExtractField.eq(classifyId))
        } else {
            // 非 PIPELINE：优先匹配 CLASSIFY_ID_MAP，回退到 CLASSIFY_ID（兼容未填充 MAP 的数据）
            jsonExtractField.eq(classifyId).or(ta.CLASSIFY_ID.eq(classifyId))
        }
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
     * 构建 JOB_TYPE 查询条件（支持多服务范围、一对多 jobType）。
     *
     * JOB_TYPE 字段可能为以下三种格式：
     * 1. 纯字符串 "AGENT"（老数据，隐含 PIPELINE）
     * 2. V1 JSON：{"PIPELINE":"AGENT","CREATIVE_STREAM":"CREATIVE_STREAM"}
     * 3. V2 JSON：{"PIPELINE":["AGENT"],"CREATIVE_STREAM":["CREATIVE_STREAM","CLOUD_TASK"]}
     *
     * 使用 JSON_CONTAINS 统一处理 V1/V2 两种 JSON 格式（对标量和数组均有效）。
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
        val normalizedScope = ServiceScopeUtil.normalize(serviceScope?.name) ?: ServiceScopeEnum.PIPELINE.name
        val isPipeline = normalizedScope == ServiceScopeEnum.PIPELINE.name

        // JSON_CONTAINS(JOB_TYPE, '"AGENT"', '$.PIPELINE') 同时兼容 V1(标量) 和 V2(数组)
        val isJsonValid = DSL.field("JSON_VALID({0})", Boolean::class.java, ta.JOB_TYPE).eq(true)
        val jsonContains = DSL.field(
            "JSON_CONTAINS({0}, {1}, {2})",
            Boolean::class.java,
            ta.JOB_TYPE,
            DSL.inline("\"$jobType\""),
            DSL.inline("$.$normalizedScope")
        ).eq(true)
        val jobTypeMatchCondition = ta.JOB_TYPE.eq(jobType).or(isJsonValid.and(jsonContains))

        if (isPipeline && jobType == JobTypeEnum.AGENT.name) {
            return when (queryFitAgentBuildLessAtomFlag) {
                true -> jobTypeMatchCondition.or(ta.BUILD_LESS_RUN_FLAG.eq(true))
                false -> jobTypeMatchCondition.and(ta.BUILD_LESS_RUN_FLAG.ne(true).or(ta.BUILD_LESS_RUN_FLAG.isNull))
                null -> jobTypeMatchCondition
            }
        }
        return jobTypeMatchCondition
    }

    /**
     * 按服务范围返回“无编译环境”对应的 JOB_TYPE 值（用于 os=all 且 fitOsFlag=false 时的筛选）。
     * PIPELINE 为 AGENT_LESS；创作流（CREATIVE_STREAM）为 CLOUD_TASK；其他 scope 返回 null。
     */
    protected fun getAgentLessJobTypeForScope(serviceScope: ServiceScopeEnum?): String? {
        return when (ServiceScopeUtil.normalize(serviceScope?.name) ?: ServiceScopeEnum.PIPELINE.name) {
            ServiceScopeEnum.PIPELINE.name -> JobTypeEnum.AGENT_LESS.name
            ServiceScopeEnum.CREATIVE_STREAM.name -> JobTypeEnum.CLOUD_TASK.name
            else -> null
        }
    }
}
