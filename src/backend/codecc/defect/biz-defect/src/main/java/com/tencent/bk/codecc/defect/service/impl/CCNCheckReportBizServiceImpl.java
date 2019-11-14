/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.codecc.defect.model.CCNSnapShotEntity;
import com.tencent.bk.codecc.defect.model.ChartAverageEntity;
import com.tencent.bk.codecc.defect.model.common.ToolSnapShotEntity;
import com.tencent.bk.codecc.defect.service.ICheckReportBizService;
import com.tencent.bk.codecc.defect.service.IDataReportBizService;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.bk.codecc.defect.vo.CCNDataReportRspVO;
import com.tencent.bk.codecc.defect.vo.ChartAverageVO;
import com.tencent.bk.codecc.task.api.ServiceToolRestResource;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.CCNLastAnalysisResultVO;
import com.tencent.devops.common.api.ToolLastAnalysisResultVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 圈复杂度分析完成生成报告服务代码
 *
 * @version V1.0
 * @date 2019/6/29
 */
@Service("CCNCheckerReportBizService")
public class CCNCheckReportBizServiceImpl implements ICheckReportBizService
{
    private static Logger logger = LoggerFactory.getLogger(CCNCheckReportBizServiceImpl.class);

    @Autowired
    private Client client;

    @Autowired
    private TaskLogService taskLogService;

    @Autowired
    private BizServiceFactory<IDataReportBizService> dataReportBizServiceBizServiceFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${devopsGateway.host:#{null}}")
    private String devopsHost;

    @Override
    public ToolSnapShotEntity getReport(long taskId, String projectId, String toolName)
    {
        CCNSnapShotEntity ccnSnapShotEntity = new CCNSnapShotEntity();

        handleToolBaseInfo(ccnSnapShotEntity, taskId, toolName, projectId);
        List<ToolLastAnalysisResultVO> toolLastAnalysisResultVOList = taskLogService.getAnalysisResultsList(taskId, toolName);
        if (CollectionUtils.isEmpty(toolLastAnalysisResultVOList))
        {
            return ccnSnapShotEntity;
        }
        if (toolLastAnalysisResultVOList.size() == 1)
        {
            ToolLastAnalysisResultVO toolLastAnalysisResultVO = toolLastAnalysisResultVOList.get(0);
            CCNLastAnalysisResultVO ccnLastAnalysisResultVO = (CCNLastAnalysisResultVO) toolLastAnalysisResultVO.getLastAnalysisResultVO();
            ccnSnapShotEntity.setTotalRiskFuncCount(ccnLastAnalysisResultVO.getDefectCount());
            ccnSnapShotEntity.setChangeedRiskFuncCount(0);
            ccnSnapShotEntity.setAverageCcn(String.format("%.2f", ccnLastAnalysisResultVO.getAverageCCN()));
            ccnSnapShotEntity.setChangedCcn("0.00");
        }
        else
        {
            ToolLastAnalysisResultVO toolLastAnalysisResultVO = toolLastAnalysisResultVOList.get(0);
            CCNLastAnalysisResultVO ccnLastAnalysisResultVO = (CCNLastAnalysisResultVO) toolLastAnalysisResultVO.getLastAnalysisResultVO();
            ToolLastAnalysisResultVO toolSecAnalysisResultVO = toolLastAnalysisResultVOList.get(1);
            CCNLastAnalysisResultVO ccnSecAnalysisResultVO = (CCNLastAnalysisResultVO) toolSecAnalysisResultVO.getLastAnalysisResultVO();
            ccnSnapShotEntity.setTotalRiskFuncCount(ccnLastAnalysisResultVO.getDefectCount());
            ccnSnapShotEntity.setChangeedRiskFuncCount(ccnLastAnalysisResultVO.getDefectCount() - ccnSecAnalysisResultVO.getDefectCount());
            ccnSnapShotEntity.setAverageCcn(String.format("%.2f", ccnLastAnalysisResultVO.getAverageCCN()));
            ccnSnapShotEntity.setChangedCcn(String.format("%.2f", ccnLastAnalysisResultVO.getAverageCCN() - ccnSecAnalysisResultVO.getAverageCCN()));
        }

        IDataReportBizService dataReportBizService = dataReportBizServiceBizServiceFactory.createBizService(toolName, ComConstants.BusinessType.DATA_REPORT.value(), IDataReportBizService.class);
        CCNDataReportRspVO ccnDataReportRspVO = (CCNDataReportRspVO) dataReportBizService.getDataReport(taskId, toolName, 5);
        if (null == ccnDataReportRspVO)
        {
            logger.info("ccn data report content is empty! task id: {}", taskId);
            return ccnSnapShotEntity;
        }

        if (null != ccnDataReportRspVO.getChartAuthorList() && null != ccnDataReportRspVO.getChartAuthorList().getTotalAuthor())
        {
            ccnSnapShotEntity.setSuperHigh(ccnDataReportRspVO.getChartAuthorList().getTotalAuthor().getSerious());
            ccnSnapShotEntity.setHigh(ccnDataReportRspVO.getChartAuthorList().getTotalAuthor().getNormal());
            ccnSnapShotEntity.setMedium(ccnDataReportRspVO.getChartAuthorList().getTotalAuthor().getPrompt());
        }

        //平均圈复杂度按日期从早到晚排序
        ccnDataReportRspVO.getChartAverageList().getAverageList().sort(Comparator.comparing(ChartAverageVO::getDate));

        //平均圈复杂度图表数值保留两位小数
        ccnDataReportRspVO.getChartAverageList().getAverageList().forEach(chartAverageVO ->
        {
            BigDecimal averageCCN = new BigDecimal(chartAverageVO.getAverageCCN());
            chartAverageVO.setAverageCCN(averageCCN.setScale(2, BigDecimal.ROUND_HALF_DOWN).floatValue());
        });

        ccnSnapShotEntity.setAverageCcnChart(ccnDataReportRspVO.getChartAverageList().getAverageList().stream().map(chartAverageVO ->
        {
            ChartAverageEntity chartAverageEntity = new ChartAverageEntity();
            BeanUtils.copyProperties(chartAverageVO, chartAverageEntity);
            return chartAverageEntity;
        }).
                collect(Collectors.toList()));

        return ccnSnapShotEntity;
    }


    private void handleToolBaseInfo(CCNSnapShotEntity ccnSnapShotEntity, long taskId, String toolName, String projectId)
    {
        //获取工具信息
        Result<ToolConfigInfoVO> toolResult = client.get(ServiceToolRestResource.class).getToolWithNameByTaskIdAndName(taskId, toolName);
        if (toolResult.isNotOk() || null == toolResult.getData())
        {
            logger.error("get tool config info fail! task id: {}, tool name: {}", taskId, toolName);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        ToolConfigInfoVO toolConfigInfoVO = toolResult.getData();
        ccnSnapShotEntity.setToolNameCn(toolConfigInfoVO.getDisplayName());
        ccnSnapShotEntity.setToolNameEn(toolName);
        if (StringUtils.isNotEmpty(projectId))
        {
            String defectDetailUrl = String.format("http://%s/console/codecc/%s/task/%d/defect/ccn/list", devopsHost, projectId, taskId);
            ccnSnapShotEntity.setDefectDetailUrl(defectDetailUrl);
            String defectReportUrl = String.format("http://%s/console/codecc/%s/task/%d/defect/ccn/charts", devopsHost, projectId, taskId);
            ccnSnapShotEntity.setDefectReportUrl(defectReportUrl);
        }
    }
}
