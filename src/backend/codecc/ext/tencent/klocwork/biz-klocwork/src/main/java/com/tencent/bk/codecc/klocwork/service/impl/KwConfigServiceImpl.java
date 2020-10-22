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

package com.tencent.bk.codecc.klocwork.service.impl;

import com.tencent.bk.codecc.klocwork.component.KlocworkAPIService;
import com.tencent.bk.codecc.klocwork.component.KwPlatformLoadBalancer;
import com.tencent.bk.codecc.klocwork.component.LBConstants;
import com.tencent.bk.codecc.klocwork.service.KwConfigService;
import com.tencent.bk.codecc.task.vo.PlatformVO;
import com.tencent.bk.codecc.task.vo.RegisterPlatformProjVO;
import com.tencent.devops.common.web.mq.ConstantsKt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * coverity项目配置业务接口实现
 *
 * @version V1.0
 * @date 2019/10/1
 */
@Service
@Slf4j
public class KwConfigServiceImpl implements KwConfigService
{
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public String registerProject(RegisterPlatformProjVO registerPlatformProjVO)
    {
        String platformIp = KwPlatformLoadBalancer.getRegisterSelectedPlatform(LBConstants.LB_ALGOL.RANDOM.value());
        registerPlatformProjVO.setPlatformIp(platformIp);

        // klocwork创建项目耗时较长，采用异步处理
        rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_REGISTER_KW_PROJECT,
                ConstantsKt.ROUTE_REGISTER_KW_PROJECT, registerPlatformProjVO);
        return platformIp;
    }
}
