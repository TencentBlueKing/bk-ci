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

package com.tencent.bk.codecc.defect.service.impl.pipelinereport;

import com.tencent.bk.codecc.defect.dao.mongorepository.DUPCStatisticRepository;
import com.tencent.bk.codecc.defect.model.DUPCStatisticEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.DUPCSnapShotEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.ToolSnapShotEntity;
import com.tencent.bk.codecc.defect.service.ICheckReportBizService;
import com.tencent.devops.common.service.ToolMetaCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 重复率分析结果报告生成服务代码
 *
 * @version V1.0
 * @date 2019/6/30
 */
@Service("DUPCCheckerReportBizService")
@Slf4j
public class DUPCCheckReportBizServiceImpl implements ICheckReportBizService
{
    @Autowired
    private DUPCStatisticRepository dupcStatisticRepository;

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    @Value("${bkci.public.url:#{null}}")
    private String devopsHost;

    @Override
    public ToolSnapShotEntity getReport(long taskId, String projectId, String toolName, String buildId)
    {
        DUPCSnapShotEntity dupcSnapShotEntity = new DUPCSnapShotEntity();
        handleToolBaseInfo(dupcSnapShotEntity, taskId, toolName, projectId);
        DUPCStatisticEntity dupcStatistic = dupcStatisticRepository.findFirstByTaskIdAndBuildId(taskId, buildId);
        if (dupcStatistic == null)
        {
            return dupcSnapShotEntity;
        }
        dupcSnapShotEntity.setTotalDupfileCount(dupcStatistic.getDefectCount());
        dupcSnapShotEntity.setChangedDupfileCount(dupcStatistic.getDefectChange());
        dupcSnapShotEntity.setCurrentDupRate(String.format("%s%s", String.format("%.2f", dupcStatistic.getDupRate()), "%"));
        dupcSnapShotEntity.setChangedDupRate(String.format("%s%s", String.format("%.2f", dupcStatistic.getDupRateChange()), "%"));
        dupcSnapShotEntity.setSuperHigh(dupcStatistic.getSuperHighCount());
        dupcSnapShotEntity.setHigh(dupcStatistic.getHighCount());
        dupcSnapShotEntity.setMedium(dupcStatistic.getMediumCount());
        dupcSnapShotEntity.setDupcChart(dupcStatistic.getDupcChart());

        return dupcSnapShotEntity;
    }


    private void handleToolBaseInfo(DUPCSnapShotEntity dupcSnapShotEntity, long taskId, String toolName, String projectId)
    {
        //获取工具信息
        dupcSnapShotEntity.setToolNameCn(toolMetaCacheService.getToolDisplayName(toolName));
        dupcSnapShotEntity.setToolNameEn(toolName);
        if (StringUtils.isNotEmpty(projectId))
        {
            String defectDetailUrl = String.format("%s/console/codecc/%s/task/%d/defect/dupc/list",
                    devopsHost, projectId, taskId);
            dupcSnapShotEntity.setDefectDetailUrl(defectDetailUrl);
            String defectReportUrl = String.format("%s/console/codecc/%s/task/%d/defect/dupc/charts",
                    devopsHost, projectId, taskId);
            dupcSnapShotEntity.setDefectReportUrl(defectReportUrl);
        }
    }
}
