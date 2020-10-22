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

package com.tencent.bk.codecc.coverity.consumer;

import com.tencent.bk.codecc.coverity.service.CovDefectService;
import com.tencent.bk.codecc.coverity.component.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

/**
 * 提单消息队列的消费者
 *
 * @version V1.0
 * @date 2019/10/17
 */
@Component
@Slf4j
public class DefectCommitConsumer
{
    @Autowired
    private CovDefectService covDefectService;
    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    /**
     * 告警提交
     *
     * @param commitDefectVO
     */
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_DEFECT_COMMIT_COVERITY,
            value = @Queue(value = QUEUE_DEFECT_COMMIT_COVERITY, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_DEFECT_COMMIT_COVERITY, durable = "true", delayed = "true", type = "topic")))
    public void commitDefect(CommitDefectVO commitDefectVO)
    {
        log.info("commit defect! {}", commitDefectVO);
        commit(commitDefectVO);
    }

    @RabbitListener(bindings = @QueueBinding(key = ROUTE_OPENSOURCE_DEFECT_COMMIT_COVERITY,
            value = @Queue(value = QUEUE_OPENSOURCE_DEFECT_COMMIT_COVERITY, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_OPENSOURCE_DEFECT_COMMIT_COVERITY, durable = "true", delayed = "true", type = "topic")))
    public void opensourceCommitDefect(CommitDefectVO commitDefectVO)
    {
        log.info("commit defect for opensource! {}", commitDefectVO);
        commit(commitDefectVO);
    }

    protected void commit(CommitDefectVO commitDefectVO)
    {
        try
        {
            // 发送开始提单的分析记录
            uploadTaskLog(commitDefectVO, ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0,null);

            try
            {
                covDefectService.commitDefect(commitDefectVO);
            }
            catch (Exception e)
            {
                log.error("commit defect fail!", e);
                // 发送提单失败的分析记录
                uploadTaskLog(commitDefectVO, ComConstants.StepFlag.FAIL.value(), 0,System.currentTimeMillis(), e.getLocalizedMessage());
                return;
            }

            // 发送提单成功的分析记录
            uploadTaskLog(commitDefectVO, ComConstants.StepFlag.SUCC.value(), 0,System.currentTimeMillis(),null);
        }
        catch (Exception e)
        {
            log.error("commit defect fail!", e);
        }
    }

    /**
     * 发送分析记录
     *
     * @param commitDefectVO
     * @param stepFlag
     * @param msg
     */
    private void uploadTaskLog(CommitDefectVO commitDefectVO, int stepFlag, long startTime, long endTime, String msg)
    {
        UploadTaskLogStepVO uploadTaskLogStepVO = new UploadTaskLogStepVO();
        uploadTaskLogStepVO.setTaskId(commitDefectVO.getTaskId());
        uploadTaskLogStepVO.setStreamName(commitDefectVO.getStreamName());
        uploadTaskLogStepVO.setToolName(commitDefectVO.getToolName());
        uploadTaskLogStepVO.setStartTime(startTime);
        uploadTaskLogStepVO.setEndTime(endTime);
        uploadTaskLogStepVO.setFlag(stepFlag);
        uploadTaskLogStepVO.setMsg(msg);
        uploadTaskLogStepVO.setStepNum(ComConstants.Step4Cov.DEFECT_SYNS.value());
        uploadTaskLogStepVO.setPipelineBuildId(commitDefectVO.getBuildId());
        uploadTaskLogStepVO.setTriggerFrom(commitDefectVO.getTriggerFrom());
        thirdPartySystemCaller.uploadTaskLog(uploadTaskLogStepVO);
    }
}
