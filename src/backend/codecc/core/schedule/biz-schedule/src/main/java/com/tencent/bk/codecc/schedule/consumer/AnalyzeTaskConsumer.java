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

package com.tencent.bk.codecc.schedule.consumer;

import com.rabbitmq.client.Channel;
import com.tencent.bk.codecc.schedule.dao.redis.AnalyzeHostPoolDao;
import com.tencent.bk.codecc.schedule.model.AnalyzeHostPoolModel;
import com.tencent.bk.codecc.schedule.service.ScheduleService;
import com.tencent.bk.codecc.schedule.vo.PushVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

/**
 * 分析任务消息队列的消费者
 *
 * @version V1.0
 * @date 2019/10/17
 */
@Component
@Slf4j
public class AnalyzeTaskConsumer
{
    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private AnalyzeHostPoolDao analyzeHostPoolDao;

    /**
     * 告警提交
     *
     * @param pushVO
     */
    public void schedule(PushVO pushVO, Channel channel, @Headers Map<String,Object> headers)
    {
        log.info("schedule：{}", pushVO);
        long deliveryTag = (Long)headers.get(AmqpHeaders.DELIVERY_TAG);

        // 判断是否需要卡住项目暂时不分析
        String dispatchFlag = analyzeHostPoolDao.getStopDispatchFlag();
        if (Boolean.FALSE.toString().equalsIgnoreCase(dispatchFlag))
        {
            log.info("schedule stop by DispatchFlag");
            nack(channel, deliveryTag);
            return;
        }

        // 分发分析任务前，先检查中断当前正在分析的任务
        scheduleService.abort(pushVO);

        AnalyzeHostPoolModel idleHost = null;
        try
        {
            // 挑选出当前空闲线程数最多的机器
            idleHost = analyzeHostPoolDao.getMostIdleHost(pushVO);
        }
        catch (Exception e)
        {
            log.error("schedule fail!", e);
        }

        if (idleHost == null)
        {
            nack(channel, deliveryTag);
            return;
        }

        log.info("schedule to host: {}", idleHost);

        try
        {
            // 挑选出当前空闲线程数最多的机器
            Boolean dispatchSuccess = scheduleService.dipatch(pushVO, idleHost);
            if (dispatchSuccess != null && dispatchSuccess)
            {
                log.info("schedule success: {}", pushVO);
                channel.basicAck(deliveryTag, true);
                Long queueCount = analyzeHostPoolDao.getSetProjectQueueCount(pushVO.getProjectId(), -1);
                if (queueCount < 0){
                    analyzeHostPoolDao.resetProjectQueueCount(pushVO.getProjectId());
                }
                return;
            }
        }
        catch (Exception e)
        {
            log.error("schedule fail: {}", pushVO, e);
        }

        String buildId = idleHost.getJobList().get(idleHost.getJobList().size() - 1).getBuildId();
        analyzeHostPoolDao.freeHostThread(pushVO.getToolName(), pushVO.getStreamName(), idleHost.getIp(), buildId);
        nack(channel, deliveryTag);
        log.info("schedule fail: {}", pushVO);
    }

    @RabbitListener(bindings = @QueueBinding(key = ROUTE_CHECK_THREAD_ALIVE,
            value = @Queue(value = QUEUE_CHECK_THREAD_ALIVE, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_CHECK_THREAD_ALIVE, durable = "true", delayed = "true")))
    public void checkAnalyzeHostThreadAlive()
    {
        log.info("begin checkAnalyzeHostThreadAlive.");

        try
        {
            scheduleService.checkAnalyzeHostThreadAlive();
        }
        catch (Exception e)
        {
            log.error("checkAnalyzeHostThreadAlive fail.", e);
        }
        log.info("end checkAnalyzeHostThreadAlive.");
    }

    /**
     * 给消息队列发送确认消息：消息没有被消费，重新放回队列
     * @param channel
     * @param deliveryTag
     */
    private void nack(Channel channel, long deliveryTag)
    {
        try
        {
            channel.basicNack(deliveryTag, true, true);
        }
        catch (IOException e)
        {
            log.error("nack fail!", e);
        }

        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            log.error("sleep exception!", e);
        }
    }

}
