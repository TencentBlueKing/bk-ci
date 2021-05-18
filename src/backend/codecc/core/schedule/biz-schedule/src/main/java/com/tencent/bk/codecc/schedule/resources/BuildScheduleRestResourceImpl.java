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
 
package com.tencent.bk.codecc.schedule.resources;

import com.tencent.bk.codecc.schedule.api.BuildScheduleRestResource;
import com.tencent.bk.codecc.schedule.service.ScheduleService;
import com.tencent.bk.codecc.schedule.vo.TailLogRspVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 分析服务器调度接口实现
 * 
 * @date 2019/11/4
 * @version V1.0
 */
@RestResource
public class BuildScheduleRestResourceImpl implements BuildScheduleRestResource
{
    @Autowired
    private ScheduleService scheduleService;

    @Override
    public Result<Boolean> push(String streamName, String toolName, String buildId, String createFrom, String projectId)
    {
        return new Result<>(scheduleService.push(streamName, toolName, buildId, createFrom, projectId));
    }

    @Override
    public Result<TailLogRspVO> tailLog(String streamName, String toolName, String buildId, long beginLine)
    {
        return new Result<>(scheduleService.tailLog(streamName, toolName, buildId, beginLine));
    }
}
