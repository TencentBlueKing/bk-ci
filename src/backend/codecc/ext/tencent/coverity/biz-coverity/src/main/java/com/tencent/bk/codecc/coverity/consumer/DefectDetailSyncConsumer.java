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
import com.tencent.bk.codecc.coverity.vo.SyncDefectDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_COV_DEFECT_DETAIL_SYNC;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_COV_DEFECT_DETAIL_SYNC_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_COV_DEFECT_DETAIL_SYNC;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_COV_DEFECT_DETAIL_SYNC_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_COV_DEFECT_DETAIL_SYNC;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_COV_DEFECT_DETAIL_SYNC_OPENSOURCE;

/**
 * 同步告警详情的消息队列的消费者
 *
 * @version V1.0
 * @date 2020/09/14
 */
@Component
@Slf4j
public class DefectDetailSyncConsumer {
    @Autowired
    private CovDefectService covDefectService;

    /**
     * 异步同步告警详情到codecc
     *
     * @param syncDefectDetailVO
     */
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_COV_DEFECT_DETAIL_SYNC,
            value = @Queue(value = QUEUE_COV_DEFECT_DETAIL_SYNC, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_COV_DEFECT_DETAIL_SYNC, durable = "true", delayed = "true")))
    public void syscDefectDetail(SyncDefectDetailVO syncDefectDetailVO) {
        log.info("commit defect! {}", syncDefectDetailVO);
        sysc(syncDefectDetailVO);
    }

    @RabbitListener(bindings = @QueueBinding(key = ROUTE_COV_DEFECT_DETAIL_SYNC_OPENSOURCE,
            value = @Queue(value = QUEUE_COV_DEFECT_DETAIL_SYNC_OPENSOURCE, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_COV_DEFECT_DETAIL_SYNC_OPENSOURCE, durable = "true",
                    delayed = "true")))
    public void opensourceSyscDefectDetail(SyncDefectDetailVO syncDefectDetailVO) {
        log.info("commit defect for opensource! {}", syncDefectDetailVO);
        sysc(syncDefectDetailVO);
    }

    protected void sysc(SyncDefectDetailVO syncDefectDetailVO) {
        try {
            covDefectService.syncDefectDetail(syncDefectDetailVO);
        } catch (Exception e) {
            log.error("commit defect fail!", e);
        }
    }
}
