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
 
package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.defect.vo.common.AuthorTransferVO;
import com.tencent.bk.codecc.task.service.IAuthorTransferBizService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_AUTHOR_TRANS;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_AUTHOR_TRANS;

/**
 * 告警处理人转换业务实现
 * 
 * @date 2019/12/3
 * @version V1.0
 */
@Service
@Slf4j
public class AuthorTransferBizServiceImpl implements IAuthorTransferBizService
{
    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Override
//    @OperationHistory(funcId = FUNC_DEFECT_MANAGE, operType = AUTHOR_TRANSFER)
    public Boolean authorTransfer(AuthorTransferVO authorTransferVO)
    {
        log.info("begin authorTransfer:\n{}", authorTransferVO);
        rabbitTemplate.convertAndSend(EXCHANGE_AUTHOR_TRANS, ROUTE_AUTHOR_TRANS, authorTransferVO);
        return true;
    }
}
