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

import com.tencent.devops.common.kafka.KafkaClient;
import com.tencent.devops.common.kafka.KafkaTopic;
import com.tencent.devops.common.web.mq.ConstantsKt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

/**
 * 数据平台消费
 *
 * @date 2019/12/13
 * @version V1.0
 */
@Component
public class KafkaDataPlatformConsumer
{
    @Autowired
    private KafkaClient kafkaClient;

    private static Logger logger = LoggerFactory.getLogger(KafkaDataPlatformConsumer.class);

    /*@RabbitListener(bindings = @QueueBinding(key = ROUTE_KAFKA_DATA_TASK_DETAIL,
            value = @Queue(value = QUEUE_KAFKA_DATA_TASK_DETAIL, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_KAFKA_DATA_PLATFORM, durable = "true", delayed = "true", type = "topic")))*/
    public void sendKafkaTaskMsg(String taskStr)
    {
        try
        {
            kafkaClient.send(KafkaTopic.TASK_DETAIL_TOPIC, taskStr);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            logger.error("send kafka task info message fail! msg: {}", e.getMessage());
        }
    }


    /*@RabbitListener(bindings = @QueueBinding(key = ROUTE_KAFKA_DATA_GONGFENG_PROJECT,
            value = @Queue(value = QUEUE_KAFKA_DATA_GONGFENG_PROJECT, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_KAFKA_DATA_PLATFORM, durable = "true", delayed = "true", type = "topic")))*/
    public void sendKafkaGongfengMsg(String gongfengMsg)
    {
        try
        {
            kafkaClient.send(KafkaTopic.GONGFENG_PROJECT_TOPIC, gongfengMsg);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            logger.error("send kafka gongfeng info message fail! msg : {}", e.getMessage());
        }
    }


    /*@RabbitListener(bindings = @QueueBinding(key = ROUTE_KAFKA_DATA_COMMON_STATISTIC,
            value = @Queue(value = QUEUE_KAFKA_DATA_COMMON_STATISTIC, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_KAFKA_DATA_PLATFORM, durable = "true", delayed = "true", type = "topic")))*/
    public void sendKafkaCommonStatMsg(String commonStatisticStr)
    {
        try
        {
            kafkaClient.send(KafkaTopic.STATISTIC_TOPIC, commonStatisticStr);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            logger.error("send kafka statistic info message fail!msg {}", e.getMessage());
        }
    }


    /*@RabbitListener(bindings = @QueueBinding(key = ROUTE_KAFKA_DATA_LINT_STATISTIC,
            value = @Queue(value = QUEUE_KAFKA_DATA_LINT_STATISTIC, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_KAFKA_DATA_PLATFORM, durable = "true", delayed = "true", type = "topic")))*/
    public void sendKafkaLintStatMsg(String lintStatisticStr)
    {
        try
        {
            kafkaClient.send(KafkaTopic.LINT_STATISTIC_TOPIC, lintStatisticStr);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            logger.error("send kafka lint statistic info message fail! msg: {}", e.getMessage());
        }
    }


    /*@RabbitListener(bindings = @QueueBinding(key = ROUTE_KAFKA_DATA_CCN_STATISTIC,
            value = @Queue(value = QUEUE_KAFKA_DATA_CCN_STATISTIC, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_KAFKA_DATA_PLATFORM, durable = "true", delayed = "true", type = "topic")))*/
    public void sendKafkaCCNStatMsg(String ccnStatisticStr)
    {
        try
        {
            kafkaClient.send(KafkaTopic.CNN_STATISTIC_TOPIC, ccnStatisticStr);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            logger.error("send kafka ccn statistic info message fail! msg: {}", e.getMessage());
        }
    }


    /*@RabbitListener(bindings = @QueueBinding(key = ROUTE_KAFKA_DATA_DUPC_STATISTIC,
            value = @Queue(value = QUEUE_KAFKA_DATA_DUPC_STATISTIC, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_KAFKA_DATA_PLATFORM, durable = "true", delayed = "true", type = "topic")))*/
    public void sendKafkaDUPCStatMsg(String dupcStatisticStr)
    {
        try
        {
            kafkaClient.send(KafkaTopic.DUPC_STATISTIC_TOPIC, dupcStatisticStr);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            logger.error("send kafka dupc statistic info message fail! msg : {}", e.getMessage());
        }
    }


    /*@RabbitListener(bindings = @QueueBinding(key = ROUTE_KAFKA_DATA_ACTIVE_PROJECT,
            value = @Queue(value = QUEUE_KAFKA_DATA_ACTIVE_PROJECT, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_KAFKA_DATA_PLATFORM, durable = "true", delayed = "true", type = "topic")))*/
    public void sendKafkaActiveProjMsg(String activeProjStr)
    {
        try{
            kafkaClient.send(KafkaTopic.ACTIVE_GONGFENG_PROJECT_TOPIC, activeProjStr);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            logger.error("send active project info message fail!", e.getMessage());
        }
    }

    /*@RabbitListener(bindings = @QueueBinding(key = ROUTE_KAFKA_DATA_CLOC_DEFECT,
            value = @Queue(value = QUEUE_KAFKA_DATA_CLOC_DEFECT, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_KAFKA_DATA_PLATFORM, durable = "true", delayed = "true", type = "topic")))*/
    public void sendKafkaClocDefectMsg(String clocDefectList)
    {
        try{
            kafkaClient.send("tendata-bkdevops-296-topic-cloc-defect", clocDefectList);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            logger.error("send cloc defect info message fail!", e.getMessage());
        }
    }
}
