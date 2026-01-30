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

package com.tencent.devops.process.strategy.bus.impl

import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.pojo.builds.HistoryConditionQueryParam
import com.tencent.devops.process.strategy.pojo.HistoryConditionQueryRequest
import com.tencent.devops.process.strategy.bus.HistoryConditionQueryStrategy
import org.jooq.Field

/**
 * 抽象历史条件查询策略
 */
abstract class AbstractHistoryConditionQueryStrategy : HistoryConditionQueryStrategy {
    
    /**
     * 获取要查询的字段
     */
    abstract fun getField(): Field<String?>

    /**
     * 将原始值转换为IdValue
     * 子类可以重写此方法以实现自定义转换逻辑
     * @param userId 用户ID
     * @param value 原始值
     * @return IdValue对象
     */
    protected open fun convertToIdValue(userId: String, value: String): IdValue {
        return IdValue(value, value)
    }

    /**
     * 批量转换原始值为IdValue
     * 子类可以重写此方法以实现批量处理或远程调用等逻辑
     * @param values 原始值列表
     * @return IdValue列表
     */
    protected open fun convertToIdValues(userId: String, values: List<String>): List<IdValue> {
        return values.map { convertToIdValue(userId, it) }
    }

    override fun query(
        request: HistoryConditionQueryRequest
    ): Page<IdValue> {
        // 构建查询参数
        val queryParam = HistoryConditionQueryParam(
            dslContext = CommonUtils.getJooqDslContext(),
            projectId = request.projectId,
            pipelineId = request.pipelineId,
            field = getField(),
            keyword = request.keyword,
            debug = request.debug,
            page = request.page,
            pageSize = request.pageSize
        )
        // 查询原始数据
        val pipelineBuildDao = SpringContextUtil.getBean(PipelineBuildDao::class.java)
        val result = pipelineBuildDao.queryHistoryConditions(queryParam)
        // 转换为IdValue
        val idValues = convertToIdValues(request.userId, result.values)
        // 构建分页结果
        return Page(
            page = request.page,
            pageSize = request.pageSize,
            count = result.totalCount,
            records = idValues
        )
    }
}

