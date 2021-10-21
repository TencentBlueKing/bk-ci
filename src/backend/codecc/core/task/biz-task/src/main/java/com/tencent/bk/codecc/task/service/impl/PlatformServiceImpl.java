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

package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.dao.mongorepository.PlatformInfoRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.PlatformInfoDao;
import com.tencent.bk.codecc.task.model.PlatformInfoEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.service.PlatformService;
import com.tencent.bk.codecc.task.vo.PlatformVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Platform业务实现类
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Service
@Slf4j
public class PlatformServiceImpl implements PlatformService
{
    @Autowired
    private PlatformInfoRepository platformInfoRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private PlatformInfoDao platformInfoDao;

    @Override
    public List<PlatformVO> getPlatformByToolName(String toolName)
    {
        List<PlatformInfoEntity> entityList = platformInfoRepository.findByToolName(toolName);
        return null == entityList ? new ArrayList<>() : entityList.stream()
                .map(baseDataEntity ->
                {
                    PlatformVO platformVO = new PlatformVO();
                    BeanUtils.copyProperties(baseDataEntity, platformVO);
                    return platformVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public String getPlatformIp(long taskId, String toolName)
    {
        ToolConfigInfoEntity toolEntity = toolRepository.findPlatformIpFirstByTaskIdAndToolName(taskId, toolName);

        if (toolEntity == null)
        {
            log.error("can't find platform ip by taskId [{}] and toolName [{}]", taskId, toolName);
            return null;
        }
        String platformIp = toolEntity.getPlatformIp();
        log.info("success getPlatformIp: {}", platformIp);
        return platformIp;
    }

    @Override
    public PlatformVO getPlatformByToolNameAndIp(String toolName, String ip)
    {
        PlatformInfoEntity platformEntity = platformInfoRepository.findFirstByToolNameAndIp(toolName, ip);

        if (platformEntity != null)
        {
            PlatformVO platformVO = new PlatformVO();
            BeanUtils.copyProperties(platformEntity, platformVO);

            return platformVO;
        }

        return null;
    }

    @Override
    public List<PlatformVO> getPlatformInfo(String toolName, String platformIp)
    {
        List<PlatformInfoEntity> platformInfoEntities = platformInfoDao.queryEntity(toolName, platformIp);
        log.info("query platform info entity count: {}", platformInfoEntities.size());

        List<PlatformVO> platformVoList = null;
        if (CollectionUtils.isNotEmpty(platformInfoEntities))
        {
            platformVoList = platformInfoEntities.stream().map(entity ->
            {
                PlatformVO platformVO = new PlatformVO();
                BeanUtils.copyProperties(entity, platformVO);
                return platformVO;
            }).collect(Collectors.toList());
        }

        return platformVoList;
    }

}
