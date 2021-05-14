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

package com.tencent.bk.codecc.schedule.service;

import com.tencent.bk.codecc.schedule.model.AnalyzeHostPoolModel;
import com.tencent.bk.codecc.schedule.vo.FreeVO;
import com.tencent.bk.codecc.schedule.vo.PushVO;
import com.tencent.bk.codecc.schedule.vo.TailLogRspVO;

/**
 * 调度服务
 * 
 * @date 2019/11/4
 * @version V1.0
 */
public interface ScheduleService
{
    Boolean push(String streamName, String toolName, String buildId, String createFrom, String projectId);

    Boolean dipatch(PushVO pushVO, AnalyzeHostPoolModel mostIdleHost);

    void abort(PushVO pushVO);

    Boolean free(FreeVO freeVO);

    void checkAnalyzeHostThreadAlive();

    TailLogRspVO tailLog(String streamName, String toolName, String buildId, long beginLine);
}
