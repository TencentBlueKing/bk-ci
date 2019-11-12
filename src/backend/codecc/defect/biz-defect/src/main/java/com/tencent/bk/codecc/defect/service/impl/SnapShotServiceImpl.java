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
import com.tencent.bk.codecc.defect.model.common.ToolSnapShotEntity;
import com.tencent.bk.codecc.defect.service.ICheckReportBizService;
import com.tencent.bk.codecc.defect.service.SnapShotService;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 快照服务层代码
 *
 * @version V1.0
 * @date 2019/6/28
 */
@Service
public class SnapShotServiceImpl implements SnapShotService
{

    @Autowired
    private BizServiceFactory<ICheckReportBizService> checkReportBizServiceBizServiceFactory;

    @Autowired
    private SnapShotRepository snapShotRepository;



    @Override
    public SnapShotEntity saveToolBuildSnapShot(long taskId, String projectId, String pipelineId, String buildId,
                                                String resultStatus, String resultMessage, String toolName)
    {
        ICheckReportBizService checkReportBizService = checkReportBizServiceBizServiceFactory.createBizService(toolName,
                ComConstants.BusinessType.CHECK_REPORT.value(), ICheckReportBizService.class);
        ToolSnapShotEntity toolSnapShotEntity = checkReportBizService.getReport(taskId, projectId, toolName);

        if (null == toolSnapShotEntity)
        {
            toolSnapShotEntity = new ToolSnapShotEntity();
        }
        toolSnapShotEntity.setResultStatus(resultStatus);
        toolSnapShotEntity.setResultMessage(resultMessage);

        SnapShotEntity snapShotEntity = snapShotRepository.findFirstByProjectIdAndBuildId(projectId, buildId);
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
        if(!toolSnapShotEntityList.stream().anyMatch(toolSnapShotEntity1 ->
            toolName.equalsIgnoreCase(toolSnapShotEntity1.getToolNameEn())
        ))
        {
            toolSnapShotEntityList.add(toolSnapShotEntity);
        }
        snapShotRepository.save(snapShotEntity);
        return snapShotEntity;
    }




}
