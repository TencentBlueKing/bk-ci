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

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.model.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.CCNSnapShotEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.ToolSnapShotEntity;
import com.tencent.bk.codecc.defect.service.ICheckReportBizService;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.devops.common.service.ToolMetaCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 圈复杂度分析完成生成报告服务代码
 *
 * @version V1.0
 * @date 2019/6/29
 */
@Service("CCNCheckerReportBizService")
@Slf4j
public class CCNCheckReportBizServiceImpl implements ICheckReportBizService
{
    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    @Autowired
    private CCNStatisticRepository ccnStatisticRepository;

    @Value("${bkci.public.url:#{null}}")
    private String devopsHost;

    @Autowired
    private NewDefectJudgeService newDefectJudgeService;

    @Autowired
    private CCNDefectDao ccnDefectDao;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Override
    public ToolSnapShotEntity getReport(long taskId, String projectId, String toolName, String buildId)
    {
        CCNSnapShotEntity ccnSnapShotEntity = new CCNSnapShotEntity();

        handleToolBaseInfo(ccnSnapShotEntity, taskId, toolName, projectId, buildId);

        CCNStatisticEntity ccnStatistic = ccnStatisticRepository.findFirstByTaskIdAndBuildId(taskId, buildId);
        if (ccnStatistic == null)
        {
            return ccnSnapShotEntity;
        }
        ccnSnapShotEntity.setTotalRiskFuncCount(ccnStatistic.getDefectCount() == null ? 0 : ccnStatistic.getDefectCount());
        ccnSnapShotEntity.setChangeedRiskFuncCount(ccnStatistic.getDefectChange() == null ? 0 : ccnStatistic.getDefectChange());
        ccnSnapShotEntity.setAverageCcn(String.format("%.2f", ccnStatistic.getAverageCCN() == null ? 0.0F : ccnStatistic.getAverageCCN()));
        ccnSnapShotEntity.setChangedCcn(String.format("%.2f", ccnStatistic.getAverageCCNChange() == null ? 0.0F : ccnStatistic.getAverageCCNChange()));
        ccnSnapShotEntity.setSuperHigh(ccnStatistic.getSuperHighCount() == null ? 0 : ccnStatistic.getSuperHighCount());
        ccnSnapShotEntity.setHigh(ccnStatistic.getHighCount() == null ? 0 : ccnStatistic.getHighCount());
        ccnSnapShotEntity.setMedium(ccnStatistic.getMediumCount() == null ? 0 : ccnStatistic.getMediumCount());
        ccnSnapShotEntity.setLow(ccnStatistic.getLowCount() == null ? 0 : ccnStatistic.getLowCount());
        ccnSnapShotEntity.setAverageCcnChart(ccnStatistic.getAverageList() == null ? Lists.newArrayList() : ccnStatistic.getAverageList());
        ccnSnapShotEntity.setCcnBeyondThresholdSum(ccnStatistic.getCcnBeyondThresholdSum() == null
                ? 0 : ccnStatistic.getCcnBeyondThresholdSum());

        return ccnSnapShotEntity;
    }


    private void handleToolBaseInfo(CCNSnapShotEntity ccnSnapShotEntity, long taskId, String toolName, String projectId, String buildId)
    {
        //获取工具信息
        ccnSnapShotEntity.setToolNameCn(toolMetaCacheService.getToolDisplayName(toolName));
        ccnSnapShotEntity.setToolNameEn(toolName);
        if (StringUtils.isNotEmpty(projectId))
        {
            String defectDetailUrl = String.format("%s/console/codecc/%s/task/%d/defect/ccn/list?buildId=%s",
                    devopsHost, projectId, taskId, buildId);
            ccnSnapShotEntity.setDefectDetailUrl(defectDetailUrl);
            String defectReportUrl = String.format("%s/console/codecc/%s/task/%d/defect/ccn/charts",
                    devopsHost, projectId, taskId);
            ccnSnapShotEntity.setDefectReportUrl(defectReportUrl);
        }
    }
}
