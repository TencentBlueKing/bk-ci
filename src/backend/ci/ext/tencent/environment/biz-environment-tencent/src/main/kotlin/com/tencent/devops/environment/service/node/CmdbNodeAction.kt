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

package com.tencent.devops.environment.service.node

import com.tencent.devops.environment.constant.T_NODE_HOST_ID
import com.tencent.devops.environment.dao.job.CmdbNodeDao
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.job.jobreq.Host
import com.tencent.devops.environment.service.job.QueryFromCCService
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class CmdbNodeAction @Autowired constructor(
    private val dslContext: DSLContext,
    private val cmdbNodeDao: CmdbNodeDao,
    private val queryFromCCService: QueryFromCCService
) : NodeAction {
    @Value("\${environment.cc.bkBizScopeId:}")
    private val bkBizScopeId = ""

    companion object {
        private val logger = LoggerFactory.getLogger(CmdbNodeAction::class.java)

        const val BIZ_SIZE = 1
    }

    override fun type(): NodeActionFactory.Action = NodeActionFactory.Action.DELETE

    override fun action(nodeRecords: List<TNodeRecord>) {
        // 判断节点在CC中的业务，为蓝盾对应的公共业务：find_host_biz_relations接口查询出所属业务，看返回值中的data数组中对象的bk_biz_id是否等于蓝盾测试机业务。
        val hostIdList = nodeRecords.filterNot {
            it.nodeType == NodeType.THIRDPARTY.name || it.nodeType == NodeType.DEVCLOUD.name
        }.mapNotNull { it.hostId }
        if (hostIdList.isNotEmpty()) {
            val hostIdQueryCCRes = queryFromCCService.queryCCFindHostBizRelations(hostIdList)
            val hostIdQueryCCList = hostIdQueryCCRes.data // 所有cc中返回的节点记录

            // 条件1. 这个业务的bizid等于蓝盾测试机
            val queryCCEqualBizList = hostIdQueryCCList?.filter {
                bkBizScopeId == it.bkBizId.toString()
            } // cc返回记录中，biz是蓝盾测试机的
            val queryCCEqualBizHostIdList = queryCCEqualBizList?.map { Host(it.bkHostId.toLong()) } ?: listOf()

            // 条件2. 判断节点在蓝盾中的项目，没在其他项目下：用host_id去T_NODE中查记录，只有等于当前项目id的一个项目。
            val nodeRecordByHostId = cmdbNodeDao.getNodesFromHostListByBkHostId(
                dslContext, queryCCEqualBizHostIdList
            )
            if (logger.isDebugEnabled)
                logger.debug("[deleteNodes]nodeRecordByHostId:${nodeRecordByHostId.joinToString()}")
            val hostIdToNodeMap = nodeRecordByHostId.groupBy({ it[T_NODE_HOST_ID] as? Long }, { it })
            val deleteHostIdMap = hostIdToNodeMap.filter { (key, value) ->
                BIZ_SIZE == value.size // key -> host_id, value -> host_id对应T_NODE表记录
            } // 只有一个项目

            // 满足以上2个条件，将其从CC蓝盾业务下移出：调用cc的delete接口，将机器从CC中移除。
            if (deleteHostIdMap.isNotEmpty()) {
                queryFromCCService.deleteHostFromCiBiz(deleteHostIdMap.keys.filterNotNull().toSet())
            }
        }
    }
}