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

package com.tencent.bk.codecc.defect.consumer;

import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetTaskRelationshipRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CheckerSetDao;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetTaskRelationshipEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

/**
 * 刷新规则集使用量消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component
@Slf4j
public class RefreshCheckerSetUsageConsumer
{
    @Autowired
    private CheckerSetTaskRelationshipRepository checkerSetTaskRelationshipRepository;

    @Autowired
    private CheckerSetDao checkerSetDao;

    @RabbitListener(bindings = @QueueBinding(key = ROUTE_REFRESH_CHECKERSET_USAGE,
            value = @Queue(value = QUEUE_REFRESH_CHECKERSET_USAGE, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_REFRESH_CHECKERSET_USAGE, durable = "true", delayed = "true")))
    public void refreshCheckerSetUsage()
    {
        log.info("begin refreshCheckerSetUsage.");

        try
        {
            List<CheckerSetTaskRelationshipEntity> taskRelationshipEntityList = checkerSetTaskRelationshipRepository.findAll();

            Map<String, Long> checkerSetCountMap = taskRelationshipEntityList.stream().filter(checkerSetTaskRelationshipEntity ->
                    StringUtils.isNotBlank(checkerSetTaskRelationshipEntity.getCheckerSetId())).
                    collect(Collectors.groupingBy(CheckerSetTaskRelationshipEntity::getCheckerSetId, Collectors.counting()));

            checkerSetDao.updateCheckerSetUsage(checkerSetCountMap);
        }
        catch (Exception e)
        {
            log.error("refreshCheckerSetUsage fail.", e);
        }
        log.info("end refreshCheckerSetUsage.");
    }


}
