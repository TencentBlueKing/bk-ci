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

import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_CODE
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_NAME
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_ID
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class MarketAtomClassifyDao : AtomBaseDao() {

    companion object {
        private const val ATOM_COUNT_ALIAS = "atomNum"
        private const val CLASSIFY_ID_ALIAS = "classifyId"
    }

    /**
     * 获取所有插件分类信息
     *
     * @param dslContext JOOQ DSL上下文
     * @param serviceScope 服务范围筛选条件（可选）
     * @return 分类信息结果集，包含每个分类的插件数量统计
     */
    fun getAllAtomClassify(
        dslContext: DSLContext,
        serviceScope: ServiceScopeEnum? = null
    ): Result<out Record> {
        val tAtom = TAtom.T_ATOM
        val tClassify = TClassify.T_CLASSIFY

        // 构建插件可见性条件
        val atomVisibleConditions = setAtomVisibleCondition(tAtom)
        serviceScope?.let {
            atomVisibleConditions.add(buildServiceScopeCondition(tAtom, serviceScope))
        }

        // 构建分类ID字段（根据serviceScope动态选择CLASSIFY_ID或CLASSIFY_ID_MAP）
        val classifyIdField = buildClassifyIdField(tAtom, serviceScope)

        // 子查询：按分类ID分组统计插件数量，T_ATOM 只扫描一次
        val atomStats = dslContext
            .select(
                classifyIdField.`as`(CLASSIFY_ID_ALIAS),
                DSL.count().`as`(ATOM_COUNT_ALIAS)
            )
            .from(tAtom)
            .where(atomVisibleConditions)
            .groupBy(classifyIdField)
            .asTable("atom_stats")

        // 主查询：LEFT JOIN 聚合结果，无插件的分类 atomNum 为 0
        val query = dslContext.select(
            tClassify.ID.`as`(KEY_ID),
            tClassify.CLASSIFY_CODE.`as`(KEY_CLASSIFY_CODE),
            tClassify.CLASSIFY_NAME.`as`(KEY_CLASSIFY_NAME),
            DSL.isnull(atomStats.field(ATOM_COUNT_ALIAS, Int::class.java), 0).`as`(ATOM_COUNT_ALIAS),
            tClassify.CREATE_TIME.`as`(KEY_CREATE_TIME),
            tClassify.UPDATE_TIME.`as`(KEY_UPDATE_TIME)
        ).from(tClassify)
            .leftJoin(atomStats)
            .on(tClassify.ID.eq(atomStats.field(CLASSIFY_ID_ALIAS, String::class.java)))
            .where(tClassify.TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))

        // 如果serviceScope不为null，则添加SERVICE_SCOPE筛选条件
        serviceScope?.let {
            query.and(tClassify.SERVICE_SCOPE.eq(it.name))
        }

        return query.orderBy(tClassify.WEIGHT.desc())
            .fetch()
    }
}
