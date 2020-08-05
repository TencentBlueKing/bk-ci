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

package com.tencent.bk.codecc.codeccjob.consumer;

import com.tencent.bk.codecc.codeccjob.service.OperationHistoryService;
import com.tencent.devops.common.web.aop.model.OperationHistoryDTO;
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
 * 操作记录队列消费逻辑
 *
 * @version V1.0
 * @date 2019/6/18
 */
@Component
public class OperationHistoryConsumer
{
    private static Logger logger = LoggerFactory.getLogger(OperationHistoryConsumer.class);

    @Autowired
    private OperationHistoryService operationHistoryService;

    /**
     * 保存历史记录信息
     *
     * @param operationHistoryDTO
     */
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_OPERATION_HISTORY,
            value = @Queue(value = QUEUE_OPERATION_HISTORY, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_OPERATION_HISTORY, durable = "true", delayed = "true", type = "topic")))
    public void saveOpHistoryMessage(OperationHistoryDTO operationHistoryDTO)
    {
        logger.info("save operation history message");
        try
        {
            operationHistoryService.saveOperationHistory(operationHistoryDTO);
        }
        catch (Exception e)
        {
            logger.error("save operation history error! task id: {}, func id: {}",
                    operationHistoryDTO.getTaskId(), operationHistoryDTO.getFuncId());
        }

    }


}
