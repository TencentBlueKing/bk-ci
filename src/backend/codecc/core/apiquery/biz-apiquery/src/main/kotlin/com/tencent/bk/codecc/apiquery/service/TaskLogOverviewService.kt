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

import com.tencent.bk.codecc.apiquery.defect.model.TaskLogOverviewModel

interface TaskLogOverviewService {

    fun statTaskAnalyzeCount(taskIds: Collection<Long>, status: Int?, startTime: Long?, endTime: Long?): Int

    fun queryAnalyzeCount(taskIds: Collection<Long>, status: Int?, startTime: Long?,
                          endTime: Long?): List<TaskLogOverviewModel>

    fun statAnalyzeCountByTaskIds(taskIds: Collection<Long>, buildNum: Int?, status: Int?, startTime: Long?,
                                  endTime: Long?): Map<Long, Int>

    fun findLatestAnalyzeStatus(taskIds: Collection<Long>, status: Int?): List<TaskLogOverviewModel>

    fun findBuildIdsByStartTime(taskIds: Collection<Long>, status: Int?, startTime: Long?,
                                endTime: Long?): List<TaskLogOverviewModel>

}
