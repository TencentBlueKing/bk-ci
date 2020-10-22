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
 
package com.tencent.bk.codecc.task.utils;

import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.devops.common.kafka.KafkaClient;
import com.tencent.devops.common.kafka.KafkaTopic;
import com.tencent.devops.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * kafka客户端
 * 
 * @date 2020/7/23
 * @version V1.0
 */
@Component
@Slf4j
public class CommonKafkaClient 
{
    @Autowired
    private KafkaClient kafkaClient;

    /**
     * 将任务详情推送到数据平台
     * @param taskInfoEntity
     */
    public void pushTaskDetailToKafka(TaskInfoEntity taskInfoEntity)
    {
        Map<String, Object> taskInfoMap = JsonUtil.INSTANCE.toMap(taskInfoEntity);
        String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        taskInfoMap.put("washTime", dateString);

        try{
            kafkaClient.send(KafkaTopic.TASK_DETAIL_TOPIC, JsonUtil.INSTANCE.toJson(taskInfoMap));
        } catch (Exception e) {
            log.error("send task info to kafka failed!", e);
        }
    }
}
