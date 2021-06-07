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

import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO
import com.tencent.bk.codecc.apiquery.vo.ToolDefectRspVO
import com.tencent.bk.codecc.apiquery.vo.op.TaskDefectVO
import com.tencent.devops.common.api.pojo.Page

interface IOpDefectDataService {

    /**
     * 按部门查询对应工具的原始告警数据统计
     */
    fun queryDeptTaskDefect(
        reqVO: TaskToolInfoReqVO,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<TaskDefectVO>

    /**
     * 按部门分页批量导出告警列表
     */
    fun batchQueryDeptDefectList(
        reqVO: TaskToolInfoReqVO,
        pageNum: Int?,
        pageSize: Int?
    ): ToolDefectRspVO
}