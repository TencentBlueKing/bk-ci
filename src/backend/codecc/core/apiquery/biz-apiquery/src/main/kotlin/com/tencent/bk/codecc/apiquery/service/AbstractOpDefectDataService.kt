/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.apiquery.service

import com.tencent.bk.codecc.apiquery.task.model.TaskInfoModel
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO
import com.tencent.bk.codecc.apiquery.vo.ToolDefectRspVO
import com.tencent.bk.codecc.task.vo.MetadataVO
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.RedisKeyConstants
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.RedisTemplate

abstract class AbstractOpDefectDataService constructor(
    open val taskService: TaskService,
    open val toolService: ToolService,
    open val taskLogService: TaskLogService,
    open val metaDataService: MetaDataService,
    open val redisTemplate: RedisTemplate<String, String>
) : IOpDefectDataService {

    // 最大查询任务条目数
    val queryTaskPageSize: Int = 200000

    /**
     * 按部门分页批量导出告警列表
     */
    override fun batchQueryDeptDefectList(reqVO: TaskToolInfoReqVO, pageNum: Int?, pageSize: Int?): ToolDefectRspVO {
        return ToolDefectRspVO()
    }

    /**
     * 设置默认条件分页查询任务列表
     */
    protected fun queryTaskInfoPage(reqVO: TaskToolInfoReqVO, pageable: Pageable): Page<TaskInfoModel> {
        reqVO.startTime = null
        reqVO.endTime = null

        val taskIdsReq = reqVO.taskIds
        // 如果只查开源扫描,则需要筛选工具未下架,否则筛选已接入
        val effectiveTaskIds = if (reqVO.createFrom.size == 1 && reqVO.createFrom.contains(
                        ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value())) {
            toolService.findTaskIdByToolNames(taskIdsReq?.toMutableList(), reqVO.toolName,
                    ComConstants.FOLLOW_STATUS.WITHDRAW.value(), true)
        } else {
            toolService.findTaskIdByToolNames(taskIdsReq?.toMutableList(), reqVO.toolName,
                    ComConstants.FOLLOW_STATUS.ACCESSED.value(), false)
        }
        // 如果有效任务数为空则返回空
        if (effectiveTaskIds.isEmpty()) {
            return Page(0, 0, 0, emptyList())
        }
        reqVO.taskIds = effectiveTaskIds

        // 1表示包含管理员任务 其他表示需要排查excludeUserList成员的任务
        if (reqVO.hasAdminTask == null) {
            reqVO.hasAdminTask = 1
        } else {
            reqVO.excludeUserList = metaDataService.queryExcludeUserList()
        }

        // 默认查询使用中的任务
        if (reqVO.status == null) {
            reqVO.status = ComConstants.Status.ENABLE.value()
        }
        return taskService.findTaskInfoPage(reqVO, pageable)
    }

    /**
     * 获取代码语言元数据类型列表
     */
    protected fun queryLangMetadataList(): List<MetadataVO> {
        return metaDataService.codeLangMetadataList
    }

    /**
     * 获取组织架构信息
     */
    protected fun queryDeptInfoMap(): Map<String, String> {
        return redisTemplate.opsForHash<String, String>().entries(RedisKeyConstants.KEY_DEPT_INFOS)
    }
}