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
import com.tencent.bk.codecc.defect.model.DUPCSnapShotEntity;
import com.tencent.bk.codecc.defect.model.DupcChartTrendEntity;
import com.tencent.bk.codecc.defect.model.common.ToolSnapShotEntity;
import com.tencent.bk.codecc.defect.service.ICheckReportBizService;
import com.tencent.bk.codecc.defect.service.IDataReportBizService;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.bk.codecc.defect.vo.DupcChartTrendVO;
import com.tencent.bk.codecc.defect.vo.DupcDataReportRspVO;
import com.tencent.bk.codecc.task.api.ServiceToolRestResource;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.DUPCLastAnalysisResultVO;
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
 * 重复率分析结果报告生成服务代码
 *
 * @version V1.0
 * @date 2019/6/30
 */
@Service("DUPCCheckerReportBizService")
public class DUPCCheckReportBizServiceImpl implements ICheckReportBizService
{
    private static Logger logger = LoggerFactory.getLogger(DUPCCheckReportBizServiceImpl.class);

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
        DUPCSnapShotEntity dupcSnapShotEntity = new DUPCSnapShotEntity();

        handleToolBaseInfo(dupcSnapShotEntity, taskId, toolName, projectId);
        List<ToolLastAnalysisResultVO> toolLastAnalysisResultVOList = taskLogService.getAnalysisResultsList(taskId, toolName);
        if (CollectionUtils.isEmpty(toolLastAnalysisResultVOList))
        {
            return dupcSnapShotEntity;
        }

        if (toolLastAnalysisResultVOList.size() == 1)
        {
            ToolLastAnalysisResultVO toolLastAnalysisResultVO = toolLastAnalysisResultVOList.get(0);
            DUPCLastAnalysisResultVO dupcLastAnalysisResultVO = (DUPCLastAnalysisResultVO) toolLastAnalysisResultVO.getLastAnalysisResultVO();
            dupcSnapShotEntity.setChangedDupfileCount(0);
            dupcSnapShotEntity.setCurrentDupRate(String.format("%s%s", String.format("%.2f", dupcLastAnalysisResultVO.getDupRate()), "%"));
            dupcSnapShotEntity.setChangedDupRate("0.00%");
        }
        else
        {
            ToolLastAnalysisResultVO toolLastAnalysisResultVO = toolLastAnalysisResultVOList.get(0);
            DUPCLastAnalysisResultVO dupcLastAnalysisResultVO = (DUPCLastAnalysisResultVO) toolLastAnalysisResultVO.getLastAnalysisResultVO();
            ToolLastAnalysisResultVO toolSecAnalysisResultVO = toolLastAnalysisResultVOList.get(1);
            DUPCLastAnalysisResultVO dupcSecAnalysisResultVO = (DUPCLastAnalysisResultVO) toolSecAnalysisResultVO.getLastAnalysisResultVO();
            dupcSnapShotEntity.setTotalDupfileCount(dupcLastAnalysisResultVO.getDefectCount());
            dupcSnapShotEntity.setChangedDupfileCount(dupcLastAnalysisResultVO.getDefectCount() - dupcSecAnalysisResultVO.getDefectCount());
            dupcSnapShotEntity.setCurrentDupRate(String.format("%s%s", String.format("%.2f", dupcLastAnalysisResultVO.getDupRate()), "%"));
            dupcSnapShotEntity.setChangedDupRate(String.format("%s%s", String.format("%.2f", dupcLastAnalysisResultVO.getDupRate() - dupcSecAnalysisResultVO.getDupRate()), "%"));
        }

        IDataReportBizService dataReportBizService = dataReportBizServiceBizServiceFactory.createBizService(toolName, ComConstants.BusinessType.DATA_REPORT.value(), IDataReportBizService.class);
        DupcDataReportRspVO dupcDataReportRspVO = (DupcDataReportRspVO) dataReportBizService.getDataReport(taskId, toolName, 5);
        if (null == dupcDataReportRspVO)
        {
            logger.info("dupc data report content is empty! task id: {}", taskId);
            return dupcSnapShotEntity;
        }

        if(null != dupcDataReportRspVO.getChartRiskList())
        {
            dupcSnapShotEntity.setSuperHigh(dupcDataReportRspVO.getChartRiskList().getSuperHighCount());
            dupcSnapShotEntity.setHigh(dupcDataReportRspVO.getChartRiskList().getHighCount());
            dupcSnapShotEntity.setMedium(dupcDataReportRspVO.getChartRiskList().getMediumCount());
        }

        //按日期排序
        dupcDataReportRspVO.getChartTrendList().getDucpChartList().sort(Comparator.comparing(DupcChartTrendVO::getDate));

        //重复率值保留两位小数
        dupcDataReportRspVO.getChartTrendList().getDucpChartList().forEach(dupcChartTrendVO ->
        {
            BigDecimal averageDupc = new BigDecimal(dupcChartTrendVO.getDupc());
            dupcChartTrendVO.setDupc(averageDupc.setScale(2, BigDecimal.ROUND_HALF_DOWN).floatValue());
        });

        dupcSnapShotEntity.setDupcChart(dupcDataReportRspVO.getChartTrendList().getDucpChartList().stream().map(dupcChartTrendVO ->
        {
            DupcChartTrendEntity dupcChartTrendEntity = new DupcChartTrendEntity();
            BeanUtils.copyProperties(dupcChartTrendVO, dupcChartTrendEntity);
            return dupcChartTrendEntity;
        }).
                collect(Collectors.toList()));

        return dupcSnapShotEntity;
    }


    private void handleToolBaseInfo(DUPCSnapShotEntity dupcSnapShotEntity, long taskId, String toolName, String projectId)
    {
        //获取工具信息
        Result<ToolConfigInfoVO> toolResult = client.get(ServiceToolRestResource.class).getToolWithNameByTaskIdAndName(taskId, toolName);
        if (toolResult.isNotOk() || null == toolResult.getData())
        {
            logger.error("get tool config info fail! task id: {}, tool name: {}", taskId, toolName);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        ToolConfigInfoVO toolConfigInfoVO = toolResult.getData();
        dupcSnapShotEntity.setToolNameCn(toolConfigInfoVO.getDisplayName());
        dupcSnapShotEntity.setToolNameEn(toolName);
        if (StringUtils.isNotEmpty(projectId))
        {
            String defectDetailUrl = String.format("http://%s/console/codecc/%s/task/%d/defect/dupc/list", devopsHost, projectId, taskId);
            dupcSnapShotEntity.setDefectDetailUrl(defectDetailUrl);
            String defectReportUrl = String.format("http://%s/console/codecc/%s/task/%d/defect/dupc/charts", devopsHost, projectId, taskId);
            dupcSnapShotEntity.setDefectReportUrl(defectReportUrl);
        }
    }
}
