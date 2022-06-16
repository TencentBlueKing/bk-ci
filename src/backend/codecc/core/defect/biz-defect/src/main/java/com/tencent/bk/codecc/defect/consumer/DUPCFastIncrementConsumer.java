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

package com.tencent.bk.codecc.defect.consumer;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.mongorepository.DUPCStatisticRepository;
import com.tencent.bk.codecc.defect.model.DUPCStatisticEntity;
import com.tencent.bk.codecc.defect.model.DupcChartTrendEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.service.IDataReportBizService;
import com.tencent.bk.codecc.defect.vo.DupcChartTrendVO;
import com.tencent.bk.codecc.defect.vo.DupcDataReportRspVO;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lint告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component
@Slf4j
public class DUPCFastIncrementConsumer extends AbstractFastIncrementConsumer
{
    @Autowired
    private BizServiceFactory<IDataReportBizService> dataReportBizServiceBizServiceFactory;
    @Autowired
    private DUPCStatisticRepository dupcStatisticRepository;

    @Override
    protected void generateResult(AnalyzeConfigInfoVO analyzeConfigInfoVO)
    {
        long taskId = analyzeConfigInfoVO.getTaskId();
        String streamName = analyzeConfigInfoVO.getNameEn();
        String toolName = analyzeConfigInfoVO.getMultiToolType();
        String buildId = analyzeConfigInfoVO.getBuildId();

        TaskDetailVO taskVO = thirdPartySystemCaller.getTaskInfo(streamName);

        ToolBuildStackEntity toolBuildStackEntity = toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);

        // 统计本次扫描的告警
        statistic(taskId, toolName, buildId, toolBuildStackEntity);

        // 更新告警快照基准构建ID
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);

        // 保存质量红线数据
        redLineReportService.saveRedLineData(taskVO, toolName, buildId);
    }

    private void statistic(long taskId, String toolName, String buildId, ToolBuildStackEntity toolBuildStackEntity)
    {
        // 因为代码没有变更，默认重复率不变，所以直接取上一个分析的统计信息
        String baseBuildId;
        if (toolBuildStackEntity == null)
        {
            ToolBuildInfoEntity toolBuildINfoEntity = toolBuildInfoRepository.findFirstByTaskIdAndToolName(taskId, toolName);
            baseBuildId = toolBuildINfoEntity != null && StringUtils.isNotEmpty(toolBuildINfoEntity.getDefectBaseBuildId()) ? toolBuildINfoEntity.getDefectBaseBuildId() : "";
        }
        else
        {
            baseBuildId = StringUtils.isNotEmpty(toolBuildStackEntity.getBaseBuildId()) ? toolBuildStackEntity.getBaseBuildId() : "";
        }
        DUPCStatisticEntity statisticEntity = dupcStatisticRepository.findFirstByTaskIdAndBuildId(taskId, baseBuildId);
        if (statisticEntity != null) {
            statisticEntity.setEntityId(null);
            statisticEntity.setBuildId(buildId);
            statisticEntity.setDefectChange(0);
            statisticEntity.setDupRateChange(0F);
            statisticEntity.setTime(System.currentTimeMillis());
            dupcStatisticRepository.save(statisticEntity);
        }

        // 获取最近5天重复率趋势
        List<DupcChartTrendEntity> dupcChart = Lists.newArrayList();
        IDataReportBizService dataReportBizService = dataReportBizServiceBizServiceFactory.createBizService(ComConstants.Tool.DUPC.name(),
                ComConstants.BusinessType.DATA_REPORT.value(), IDataReportBizService.class);
        DupcDataReportRspVO dupcDataReportRspVO = (DupcDataReportRspVO) dataReportBizService
                .getDataReport(taskId, ComConstants.Tool.DUPC.name(), 5, null, null);
        if (dupcDataReportRspVO != null)
        {
            //按日期排序
            dupcDataReportRspVO.getChartTrendList().getDucpChartList().sort(Comparator.comparing(DupcChartTrendVO::getDate));

            //重复率值保留两位小数
            dupcDataReportRspVO.getChartTrendList().getDucpChartList().forEach(dupcChartTrendVO ->
            {
                BigDecimal averageDupc = new BigDecimal(dupcChartTrendVO.getDupc());
                dupcChartTrendVO.setDupc(averageDupc.setScale(2, BigDecimal.ROUND_HALF_DOWN).floatValue());
            });

            dupcChart.addAll(dupcDataReportRspVO.getChartTrendList().getDucpChartList().stream().map(dupcChartTrendVO ->
            {
                DupcChartTrendEntity dupcChartTrendEntity = new DupcChartTrendEntity();
                BeanUtils.copyProperties(dupcChartTrendVO, dupcChartTrendEntity);
                return dupcChartTrendEntity;
            }).collect(Collectors.toList()));
        }
        statisticEntity.setDupcChart(dupcChart);
        dupcStatisticRepository.save(statisticEntity);
    }
}
