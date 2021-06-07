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

package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.SnapShotRepository;
import com.tencent.bk.codecc.defect.model.SnapShotEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.ToolSnapShotEntity;
import com.tencent.bk.codecc.defect.service.ICheckReportBizService;
import com.tencent.bk.codecc.defect.service.SnapShotService;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.service.BizServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 快照服务层代码
 *
 * @version V1.0
 * @date 2019/6/28
 */
@Slf4j
@Service
public class SnapShotServiceImpl implements SnapShotService
{
    /**
     * 字符串锁前缀
     */
    private static final String LOCK_KEY_PREFIX = "GET_AND_SET_SNAPSHOT:";

    /**
     * 分布式锁超时时间
     */
    private static final Long LOCK_TIMEOUT = 10L;

    @Autowired
    private BizServiceFactory<ICheckReportBizService> checkReportBizServiceBizServiceFactory;

    @Autowired
    private SnapShotRepository snapShotRepository;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public SnapShotEntity saveToolBuildSnapShot(long taskId, String projectId, String pipelineId, String buildId,
                                                String resultStatus, String resultMessage, String toolName)
    {
        SnapShotEntity snapShotEntity = null;
        ICheckReportBizService checkReportBizService = checkReportBizServiceBizServiceFactory.createBizService(toolName,
                ComConstants.BusinessType.CHECK_REPORT.value(), ICheckReportBizService.class);
        ToolSnapShotEntity toolSnapShotEntity = checkReportBizService.getReport(taskId, projectId, toolName, buildId);
        if (null == toolSnapShotEntity)
        {
            toolSnapShotEntity = new ToolSnapShotEntity();
        }
        toolSnapShotEntity.setResultStatus(resultStatus);
        toolSnapShotEntity.setResultMessage(resultMessage);

        RedisLock lock = new RedisLock(redisTemplate, LOCK_KEY_PREFIX + taskId + ComConstants.SEPARATOR_SEMICOLON + buildId,
                LOCK_TIMEOUT);
        try
        {
            lock.lock();
            snapShotEntity = snapShotRepository.findFirstByProjectIdAndBuildId(projectId, buildId);
            if (null == snapShotEntity)
            {
                snapShotEntity = new SnapShotEntity();
                snapShotEntity.setProjectId(projectId);
                snapShotEntity.setPipelineId(pipelineId);
                snapShotEntity.setTaskId(taskId);
                snapShotEntity.setBuildId(buildId);
                snapShotEntity.setToolSnapshotList(new ArrayList<>());
            }

            List<ToolSnapShotEntity> toolSnapShotEntityList = snapShotEntity.getToolSnapshotList();
            if (toolSnapShotEntityList.stream().noneMatch(toolSnapShotEntity1 ->
                    toolName.equalsIgnoreCase(toolSnapShotEntity1.getToolNameEn())))
            {
                toolSnapShotEntityList.add(toolSnapShotEntity);
            }
            snapShotRepository.save(snapShotEntity);
        }
        finally
        {
            lock.unlock();
        }
        if (snapShotEntity == null)
        {
            String errMsg = String.format("get tool analysis snapshot fail! taskId: %d, buildId: %s", taskId, buildId);
            log.error(errMsg);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{errMsg}, null);
        }

        return snapShotEntity;
    }
}
