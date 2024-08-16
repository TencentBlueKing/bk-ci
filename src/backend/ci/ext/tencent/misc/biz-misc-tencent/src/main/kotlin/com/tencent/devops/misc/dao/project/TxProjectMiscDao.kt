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

package com.tencent.devops.misc.dao.project

import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.model.project.tables.TProject
import com.tencent.devops.model.project.tables.TShardingRoutingRule
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class TxProjectMiscDao {

    fun getProjectInfoList(
        dslContext: DSLContext,
        projectIdList: List<String>? = null,
        minId: Long? = null,
        maxId: Long? = null,
        channelCodeList: List<String>? = null,
        dsName: String? = null
    ): Result<out Record>? {
        val tp = TProject.T_PROJECT
        val tsrr = TShardingRoutingRule.T_SHARDING_ROUTING_RULE
        val conditions = mutableListOf<Condition>()
        if (!projectIdList.isNullOrEmpty()) {
            conditions.add(tp.ENGLISH_NAME.`in`(projectIdList))
        }
        if (minId != null) {
            conditions.add(tp.ID.ge(minId))
        }
        if (maxId != null) {
            conditions.add(tp.ID.lt(maxId))
        }
        if (!channelCodeList.isNullOrEmpty()) {
            conditions.add(tp.CHANNEL.`in`(channelCodeList))
        }
        if (!dsName.isNullOrBlank()) {
            conditions.add(tsrr.ROUTING_RULE.eq(dsName))
        }
        val clusterName = CommonUtils.getDbClusterName()
        conditions.add(tsrr.CLUSTER_NAME.eq(clusterName))
        conditions.add(tsrr.TYPE.eq(ShardingRuleTypeEnum.DB.name))
        conditions.add(tsrr.MODULE_CODE.eq(SystemModuleEnum.PROCESS.name))
        return dslContext.select(
            tp.ID.`as`("ID"),
            tp.ENGLISH_NAME.`as`("ENGLISH_NAME"),
            tp.CHANNEL.`as`("CHANNEL"),
            tsrr.ROUTING_RULE.`as`("ROUTING_RULE")
        ).from(tp).leftJoin(tsrr).on(tp.ENGLISH_NAME.eq(tsrr.ROUTING_NAME))
            .where(conditions).fetch()
    }
}
