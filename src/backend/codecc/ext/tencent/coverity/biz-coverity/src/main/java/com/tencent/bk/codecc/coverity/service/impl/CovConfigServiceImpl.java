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

package com.tencent.bk.codecc.coverity.service.impl;

import com.tencent.bk.codecc.coverity.component.CovPlatformLoadBalancer;
import com.tencent.bk.codecc.coverity.component.CoverityService;
import com.tencent.bk.codecc.coverity.component.LBConstants;
import com.tencent.bk.codecc.coverity.service.CovConfigService;
import com.tencent.bk.codecc.coverity.vo.UpdateComponentMapVO;
import com.tencent.bk.codecc.task.vo.RegisterPlatformProjVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * coverity项目配置业务接口实现
 *
 * @version V1.0
 * @date 2019/10/1
 */
@Service
@Slf4j
public class CovConfigServiceImpl implements CovConfigService
{
    @Override
    public String registerProject(RegisterPlatformProjVO registerPlatformProjVO)
    {
        String streamName = registerPlatformProjVO.getStreamName();
        String createFrom = registerPlatformProjVO.getCreateFrom();
        String platform = CovPlatformLoadBalancer.getRegisterSelectedPlatform(LBConstants.LB_ALGOL.RANDOM.value(), createFrom);
        CoverityService coverityService = CoverityService.getInst(platform);

        //创建Triage Store
        boolean bRes = coverityService.createTriageStore(streamName);
        if (!bRes)
        {
            log.error("fail create triage store: {}", streamName);
            return null;
        }
        log.info("succ create triage store: {}", streamName);

        //创建ComponentMap
//        bRes = coverityService.createComponentMap(streamName);
//        if (!bRes)
//        {
//            coverityService.delTriageStore(streamName);
//            log.error("fail create componentmap: {}", streamName);
//            return null;
//        }
//        log.info("succ create componentmap: {}", streamName);

        //创建项目
        bRes = coverityService.createProject(streamName);
        if (!bRes)
        {
            coverityService.delTriageStore(streamName);
//            coverityService.delComponentMap(streamName);
            log.error("fail create project: {}", streamName);
            return null;
        }
        log.info("succ create project:" + streamName);

        //创建流
        bRes = coverityService.createStream(streamName, streamName, streamName, streamName);
        if (!bRes)
        {
            coverityService.delTriageStore(streamName);
//            coverityService.delComponentMap(streamName);
            coverityService.delProject(streamName);
            log.error("fail create stream: {}", streamName);
            return null;
        }
        log.info("succ create stream: {}", streamName);
        log.info("succ Register Project: {}", streamName);

        return platform;
    }

    @Override
    public boolean updateComponentMap(UpdateComponentMapVO updateComponentMapVO)
    {
        String streamName = updateComponentMapVO.getStreamName();
        String platform = updateComponentMapVO.getPlatformIp();
        List<String> pathList = updateComponentMapVO.getPathList();

        //更新映射组件的屏蔽路径
        boolean bRes = CoverityService.getInst(platform).updateComponentMap(streamName, pathList);
        if (!bRes)
        {
            log.error("fail commitDefect: {}", streamName);
            return false;
        }
        log.info("succ commitDefect: {}", streamName);
        return true;
    }
}
