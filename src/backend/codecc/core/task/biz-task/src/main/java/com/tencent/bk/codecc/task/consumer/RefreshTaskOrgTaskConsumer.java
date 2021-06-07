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

package com.tencent.bk.codecc.task.consumer;


import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.service.TaskService;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

/**
 * 定时刷新组织架构为空的扫描任务
 *
 * @version V2.0
 * @date 2020/8/4
 */
@Component
@Slf4j
public class RefreshTaskOrgTaskConsumer
{
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;


    @RabbitListener(bindings = @QueueBinding(key = ROUTE_TASK_REFRESH_ORG,
            value = @Queue(value = QUEUE_TASK_REFRESH_ORG, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_TASK_REFRESH_ORG, durable = "true")))
    public void refreshTaskOrgInfo()
    {
        // 0表示组织架构为空
        List<TaskInfoEntity> taskInfoEntityList = taskRepository.findByBgId(0);
        if (CollectionUtils.isEmpty(taskInfoEntityList))
        {
            log.info("refreshTaskOrgInfo task list[bg_id=0] is empty.");
            return;
        }

        StringBuilder strBuilder = new StringBuilder();
        log.info("task list[bg_id=0] size: {}", taskInfoEntityList.size());
        for (TaskInfoEntity taskInfoEntity : taskInfoEntityList)
        {
            if (taskInfoEntity == null)
            {
                continue;
            }
            long taskId = taskInfoEntity.getTaskId();

            try
            {
                // 每一次线程休息0.5秒
                Thread.sleep(500);
                Boolean result = taskService.refreshTaskOrgInfo(taskId);
                if (!result)
                {
                    strBuilder.append(taskId).append(ComConstants.STRING_SPLIT);
                }
            }
            catch (Exception e)
            {
                log.error("task {} refresh org failed: {}", taskId, e);
                strBuilder.append(taskId).append(ComConstants.STRING_SPLIT);
            }
        }
        log.info("finish RefreshTaskOrgTask, refresh failed task: [{}]", strBuilder.toString());
    }

}
