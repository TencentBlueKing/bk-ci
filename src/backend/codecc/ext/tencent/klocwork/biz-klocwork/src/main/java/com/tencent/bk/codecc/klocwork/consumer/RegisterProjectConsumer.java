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

package com.tencent.bk.codecc.klocwork.consumer;

import com.tencent.bk.codecc.klocwork.component.KlocworkAPIService;
import com.tencent.bk.codecc.task.vo.PlatformVO;
import com.tencent.bk.codecc.task.vo.RegisterPlatformProjVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class RegisterProjectConsumer
{
    @Autowired
    private KlocworkAPIService klocworkAPIService;

    @Value("${default.close.checkers}")
    private String defaultCloseCheckers;

    /**
     * 告警提交
     *
     * @param registerPlatformProjVO
     */
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_REGISTER_KW_PROJECT,
            value = @Queue(value = QUEUE_REGISTER_KW_PROJECT, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_REGISTER_KW_PROJECT, durable = "true", delayed = "true")))
    public void registerProject(RegisterPlatformProjVO registerPlatformProjVO)
    {
        try
        {
            log.info("register project! {}", registerPlatformProjVO);
            String streamName = registerPlatformProjVO.getStreamName();
            String platformIp = registerPlatformProjVO.getPlatformIp();
            PlatformVO platformVO = KlocworkAPIService.getInst(platformIp);
            try
            {
                klocworkAPIService.createKWProject(platformIp, platformVO.getPort(), streamName);
                log.info("succ Register Project: {}", streamName);
            }
            catch (Exception e)
            {
                log.error("register project fail!", e);

                // 注册失败需要回滚，删除klocwork工具 TODO
                return;
            }

            // 创建成功修改异步创建成功确认状态位 TODO

            // 关闭klocwork platform上的部分默认规则
            closeDefaultCheckersInKWPlatform(streamName, platformVO);
        }
        catch (Exception e)
        {
            log.error("register project fail!", e);
        }
    }

    /**
     * 关闭klocwork platform上的部分默认规则
     *
     * @param streamName
     * @param platformVO
     */
    private void closeDefaultCheckersInKWPlatform(String streamName, PlatformVO platformVO)
    {
        if (StringUtils.isNotEmpty(defaultCloseCheckers))
        {
            String[] defaultCloseCheckerArr = defaultCloseCheckers.split(ComConstants.SEMICOLON);
            for (String checker : defaultCloseCheckerArr)
            {
                klocworkAPIService.updateCheckers(streamName, platformVO, checker, false);
            }
        }
    }
}
