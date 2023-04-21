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

package com.tencent.bk.codecc.codeccjob.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.codecc.defect.dto.WebsocketDTO;
import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

/**
 * websocket消息消费者
 *
 * @version V1.0
 * @date 2019/12/14
 */
@Component
public class WebsocketConsumer {
    private static Logger logger = LoggerFactory.getLogger(WebsocketConsumer.class);

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;


    public void sendWebsocketMsg(WebsocketDTO websocketDTO) {
        TaskLogVO taskLogVO = websocketDTO.getTaskLogVO();
        TaskOverviewVO.LastAnalysis lastAnalysis = websocketDTO.getLastAnalysisResultList();
        TaskDetailVO taskDetailVO = websocketDTO.getTaskDetailVO();
        TaskLogOverviewVO taskLogOverviewVO = websocketDTO.getTaskLogOverviewVO();
        //1.推送消息到详情界面
        try
        {
            simpMessagingTemplate.convertAndSend(String.format("/topic/analysisInfo/taskId/%d", taskLogVO.getTaskId()),
                    objectMapper.writeValueAsString(lastAnalysis));
        }
        catch (JsonProcessingException e1)
        {
            logger.error("serialize last analysis info failed! task id: {}, tool name: {}", taskLogVO.getTaskId(),
                    taskLogVO.getToolName());
        }
        catch (Exception e2)
        {
            logger.error("execute last analysis info failed! task id: {}, tool name: {}", taskLogVO.getTaskId(),
                    taskLogVO.getToolName(), e2);
        }

        //2.推送消息至单个任务的详情界面
        try {
            simpMessagingTemplate.convertAndSend(String.format("/topic/analysisDetail/taskId/%d/toolName/%s",
                    taskLogVO.getTaskId(), taskLogVO.getToolName()),
                    objectMapper.writeValueAsString(taskLogVO));
        } catch (JsonProcessingException e1) {
            logger.error("serialize last analysis detail failed! task id: {}, tool name: {}", taskLogVO.getTaskId(),
                    taskLogVO.getToolName());
        } catch (Exception e2)
        {
            logger.error("execute last analysis detail failed! task id: {}, tool name: {}", taskLogVO.getTaskId(),
                    taskLogVO.getToolName(), e2);
        }

        try {
            simpMessagingTemplate.convertAndSend(String.format("/topic/analysisDetail/taskId/%d",
                    taskLogOverviewVO.getTaskId()),
                    objectMapper.writeValueAsString(taskLogOverviewVO));
        } catch (JsonProcessingException e1) {
            logger.error("serialize last analysis detail failed! task id: {}", taskLogOverviewVO.getTaskId());
        } catch (Exception e2)
        {
            logger.error("execute last analysis detail failed! task id: {}", taskLogVO.getTaskId(), e2);
        }

        //3.推送工具进度条至消息详情界面
        try{
            simpMessagingTemplate.convertAndSend(String.format("/topic/analysisProgress/projectId/%s",
                    taskDetailVO.getProjectId()),
                    objectMapper.writeValueAsString(taskDetailVO)
                    );
        } catch (JsonProcessingException e1) {
            logger.error("serialize last analysis progress failed! task id: {}, tool name: {}", taskDetailVO.getTaskId(),
                    taskDetailVO.getDisplayToolName());
        } catch (Exception e2)
        {
            logger.error("serialize last analysis progress failed! task id: {}, tool name: {}", taskLogVO.getTaskId(),
                    taskLogVO.getToolName(), e2);
        }


        //4.推送总览头部进度条websocket
        try{
            simpMessagingTemplate.convertAndSend(String.format("/topic/generalProgress/taskId/%d",
                    taskDetailVO.getTaskId()),
                    objectMapper.writeValueAsString(taskDetailVO));
        } catch (JsonProcessingException e) {
            logger.error("serialize general analysis progress failed! task id: {}, tool name: {}", taskDetailVO.getTaskId(),
                    taskDetailVO.getDisplayToolName());
        } catch (Exception e2)
        {
            logger.error("serialize general analysis progress failed! task id: {}, tool name: {}", taskLogVO.getTaskId(),
                    taskLogVO.getToolName(), e2);
        }

        logger.info("send websocket msg successfully! task id: {}, tool name: {}", taskDetailVO.getTaskId(),
                taskDetailVO.getDisplayToolName());

    }

}
