/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.model.TaskLogGroupEntity;
import com.tencent.bk.codecc.defect.service.PipelineService;
import com.tencent.bk.codecc.defect.service.PipelineTaskService;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.Future;

/**
 * 任务操作服务实现
 *
 * @version V1.0
 * @date 2019/7/18
 */
@Service
public class PipelineTaskServiceImpl implements PipelineTaskService
{
    private static Logger logger = LoggerFactory.getLogger(SnapShotServiceImpl.class);

    @Autowired
    private BizServiceFactory<IBizService> bizServiceFactory;

    @Autowired
    private PipelineService pipelineService;

    @Override
    @Async("asyncTaskExecutor")
    public Future<Boolean> handleStopTask(String projectId, String pipelineId, long taskId, TaskLogGroupEntity filteredTaskLog,
                                          String userName, Boolean stopFlag, String streamName)
    {
        if (stopFlag)
        {
            //先停止流水线
            pipelineService.stopRunningTask(projectId, pipelineId, taskId, filteredTaskLog.getBuildId(), userName, streamName);
        }
        //再更新任务步骤状态
        updateTaskAbortStep(streamName, filteredTaskLog.getToolName(), filteredTaskLog, "任务被手动中断");
        return new AsyncResult<>(true);
    }


    private void updateTaskAbortStep(String streamName, String toolName, TaskLogGroupEntity taskLogGroupEntity, String msg)
    {

        UploadTaskLogStepVO uploadTaskLogStepVO = new UploadTaskLogStepVO();
        uploadTaskLogStepVO.setStepNum(taskLogGroupEntity.getCurrStep());
        uploadTaskLogStepVO.setFlag(ComConstants.StepFlag.FAIL.value());
        uploadTaskLogStepVO.setStartTime(0L);
        uploadTaskLogStepVO.setEndTime(System.currentTimeMillis());
        uploadTaskLogStepVO.setMsg(msg);
        uploadTaskLogStepVO.setStreamName(streamName);
        uploadTaskLogStepVO.setToolName(toolName.toUpperCase());

        IBizService taskLogService = bizServiceFactory.createBizService(uploadTaskLogStepVO.getToolName(),
                ComConstants.BusinessType.ANALYZE_TASK.value(), IBizService.class);
        taskLogService.processBiz(uploadTaskLogStepVO);
        logger.info("update task abort step success! stream name: {}, tool name: {}", streamName, toolName);
    }

}
