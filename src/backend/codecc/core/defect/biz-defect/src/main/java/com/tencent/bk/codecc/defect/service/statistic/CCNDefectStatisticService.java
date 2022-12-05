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

package com.tencent.bk.codecc.defect.service.statistic;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.CCNNotRepairedAuthorEntity;
import com.tencent.bk.codecc.defect.model.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.model.ChartAverageEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.IDataReportBizService;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.CCNDataReportRspVO;
import com.tencent.bk.codecc.defect.vo.ChartAverageVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC_CCN;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC_CCN;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE;

/**
 * Lint告警统计
 *
 * @version V1.0
 * @date 2020/8/13
 */
@Component
@Slf4j
public class CCNDefectStatisticService
{
    @Autowired
    private ToolBuildInfoRepository toolBuildInfoRepository;
    @Autowired
    private BizServiceFactory<IDataReportBizService> dataReportBizServiceBizServiceFactory;
    @Autowired
    private CCNStatisticRepository ccnStatisticRepository;
    @Autowired
    public ThirdPartySystemCaller thirdPartySystemCaller;
    @Autowired
    private CheckerService checkerService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private NewDefectJudgeService newDefectJudgeService;

    /**
     * 统计本次扫描的告警
     *
     * @param taskVO
     * @param toolName
     * @param averageCCN
     * @param buildId
     * @param toolBuildStackEntity
     * @param allNewDefectList
     */
    public void statistic(TaskDetailVO taskVO, String toolName, float averageCCN, String buildId, ToolBuildStackEntity toolBuildStackEntity, List<CCNDefectEntity> allNewDefectList)
    {
        long taskId = taskVO.getTaskId();
        // 获取各严重级别定义
        Map<String, String> riskConfigMap = thirdPartySystemCaller.getRiskFactorConfig(ComConstants.Tool.CCN.name());
        if (riskConfigMap == null)
        {
            log.error("Has not init risk factor config!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"风险系数"}, null);
        }
        int sh = Integer.valueOf(riskConfigMap.get(ComConstants.RiskFactor.SH.name()));
        int h = Integer.valueOf(riskConfigMap.get(ComConstants.RiskFactor.H.name()));
        int m = Integer.valueOf(riskConfigMap.get(ComConstants.RiskFactor.M.name()));

        // 获取超标圈复杂度阈值，优先从规则里面取，取不到从个性化参数里面取，再取不到就是用默认值
        ToolConfigInfoVO toolConfigInfoVO = taskVO.getToolConfigInfoList().stream()
                .filter(toolConfig -> toolConfig.getToolName().equalsIgnoreCase(ComConstants.Tool.CCN.name()))
                .findAny()
                .orElseGet(ToolConfigInfoVO::new);
        int ccnThreshold = checkerService.getCcnThreshold(toolConfigInfoVO);

        int ccnBeyondThresholdSum = 0;
        int existCount = 0;
        int superHighCount = 0;
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;

        // 新增统计项
        int newSuperHighCount = 0;
        int newHighCount = 0;
        int newMediumCount = 0;
        int newLowCount = 0;

        Map<String, CCNNotRepairedAuthorEntity> newAuthorMap = Maps.newHashMap();
        Map<String, CCNNotRepairedAuthorEntity> existAuthorMap = Maps.newHashMap();

        if (CollectionUtils.isNotEmpty(allNewDefectList)) {
            // 新老告警判定时间
            long newDefectJudgeTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, toolName, taskVO);

            for (CCNDefectEntity defectEntity : allNewDefectList) {
                // 统计遗留告警数
                if (defectEntity.getStatus() != ComConstants.DefectStatus.NEW.value()) {
                    continue;
                }

                existCount++;

                int ccn = defectEntity.getCcn();
                if (ccn >= m && ccn < h) {
                    mediumCount++;
                } else if (ccn >= h && ccn < sh) {
                    highCount++;
                } else if (ccn >= sh) {
                    superHighCount++;
                } else if (ccn < m) {
                    lowCount++;
                }

                // 计算超标复杂度
                int diff = ccn - ccnThreshold;
                if (diff > 0) {
                    ccnBeyondThresholdSum += diff;
                }

                int riskVal = getRiskFactorVal(ccn, sh, h, m);
                long defectLastUpdateTime = DateTimeUtils.getThirteenTimestamp(
                        defectEntity.getLatestDateTime() == null ? 0 : defectEntity.getLatestDateTime()
                );
                boolean isNewDefectByJudgeTime = defectLastUpdateTime >= newDefectJudgeTime;

                if (isNewDefectByJudgeTime) {
                    if (ComConstants.RiskFactor.SH.value() == riskVal) {
                        newSuperHighCount++;
                    } else if (ComConstants.RiskFactor.H.value() == riskVal) {
                        newHighCount++;
                    } else if (ComConstants.RiskFactor.M.value() == riskVal) {
                        newMediumCount++;
                    } else if (ComConstants.RiskFactor.L.value() == riskVal) {
                        newLowCount++;
                    }
                }

                // 统计处理人信息
                if (StringUtils.isEmpty(defectEntity.getAuthor())) {
                    continue;
                }

                CCNNotRepairedAuthorEntity authorStatistic;
                if (isNewDefectByJudgeTime) {
                    authorStatistic = newAuthorMap.get(defectEntity.getAuthor());
                    if (authorStatistic == null) {
                        authorStatistic = new CCNNotRepairedAuthorEntity();
                        authorStatistic.setName(defectEntity.getAuthor());
                        newAuthorMap.put(defectEntity.getAuthor(), authorStatistic);
                    }
                } else {
                    authorStatistic = existAuthorMap.get(defectEntity.getAuthor());
                    if (authorStatistic == null) {
                        authorStatistic = new CCNNotRepairedAuthorEntity();
                        authorStatistic.setName(defectEntity.getAuthor());
                        existAuthorMap.put(defectEntity.getAuthor(), authorStatistic);
                    }
                }

                if (ComConstants.RiskFactor.SH.value() == riskVal) {
                    authorStatistic.setSuperHighCount(authorStatistic.getSuperHighCount() + 1);
                } else if (ComConstants.RiskFactor.H.value() == riskVal) {
                    authorStatistic.setHighCount(authorStatistic.getHighCount() + 1);
                } else if (ComConstants.RiskFactor.M.value() == riskVal) {
                    authorStatistic.setMediumCount(authorStatistic.getMediumCount() + 1);
                } else if (ComConstants.RiskFactor.L.value() == riskVal) {
                    authorStatistic.setLowCount(authorStatistic.getLowCount() + 1);
                }
            }
        }

        log.info("existCount-->{}", existCount);

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

        CCNStatisticEntity baseBuildCcnStatistic = ccnStatisticRepository.findFirstByTaskIdAndBuildId(taskVO.getTaskId(), baseBuildId);

        CCNStatisticEntity newCcnStatistic = new CCNStatisticEntity();
        if (baseBuildCcnStatistic != null)
        {
            newCcnStatistic.setDefectChange(existCount - (baseBuildCcnStatistic.getDefectCount() == null ? 0 : baseBuildCcnStatistic.getDefectCount()));
            newCcnStatistic.setAverageCCNChange(averageCCN - (baseBuildCcnStatistic.getAverageCCN() == null ? 0 : baseBuildCcnStatistic.getAverageCCN()));
            newCcnStatistic.setLastDefectCount(baseBuildCcnStatistic.getDefectCount());
            newCcnStatistic.setLastAverageCCN(baseBuildCcnStatistic.getAverageCCN());
        }
        else
        {
            newCcnStatistic.setDefectChange(existCount);
            newCcnStatistic.setAverageCCNChange(averageCCN);
            newCcnStatistic.setLastDefectCount(0);
            newCcnStatistic.setLastAverageCCN(0.0F);
        }
        newCcnStatistic.setDefectCount(existCount);
        newCcnStatistic.setAverageCCN(averageCCN);
        newCcnStatistic.setTime(System.currentTimeMillis());
        newCcnStatistic.setTaskId(taskId);
        newCcnStatistic.setToolName(toolName);
        newCcnStatistic.setBuildId(buildId);
        newCcnStatistic.setSuperHighCount(superHighCount);
        newCcnStatistic.setHighCount(highCount);
        newCcnStatistic.setMediumCount(mediumCount);
        newCcnStatistic.setLowCount(lowCount);
        newCcnStatistic.setCcnBeyondThresholdSum(ccnBeyondThresholdSum);
        newCcnStatistic.setNewAuthorStatistic(Lists.newArrayList(newAuthorMap.values()));
        newCcnStatistic.setExistAuthorStatistic(Lists.newArrayList(existAuthorMap.values()));
        newCcnStatistic.setNewSuperHighCount(newSuperHighCount);
        newCcnStatistic.setNewHighCount(newHighCount);
        newCcnStatistic.setNewMediumCount(newMediumCount);
        newCcnStatistic.setNewlowCount(newLowCount);
        ccnStatisticRepository.save(newCcnStatistic);

        // 获取最近5日平均圈复杂度趋势数据，由于需要使用最新统计结果，所以先保存再获取趋势数据然后再次保存
        List<ChartAverageEntity> averageList = Lists.newArrayList();
        IDataReportBizService dataReportBizService = dataReportBizServiceBizServiceFactory.createBizService(toolName,
                ComConstants.BusinessType.DATA_REPORT.value(), IDataReportBizService.class);
        CCNDataReportRspVO ccnDataReportRspVO = (CCNDataReportRspVO) dataReportBizService
                .getDataReport(taskId, toolName, 5, null, null);
        if (ccnDataReportRspVO != null)
        {
            //平均圈复杂度按日期从早到晚排序
            ccnDataReportRspVO.getChartAverageList().getAverageList().sort(Comparator.comparing(ChartAverageVO::getDate));

            //平均圈复杂度图表数值保留两位小数
            ccnDataReportRspVO.getChartAverageList().getAverageList().forEach(chartAverageVO ->
            {
                BigDecimal averageCcnBd = new BigDecimal(chartAverageVO.getAverageCCN());
                chartAverageVO.setAverageCCN(averageCcnBd.setScale(2, BigDecimal.ROUND_HALF_DOWN).floatValue());
            });

            averageList.addAll(ccnDataReportRspVO.getChartAverageList().getAverageList().stream().map(chartAverageVO ->
            {
                ChartAverageEntity chartAverageEntity = new ChartAverageEntity();
                BeanUtils.copyProperties(chartAverageVO, chartAverageEntity);
                return chartAverageEntity;
            }).collect(Collectors.toList()));
        }
        newCcnStatistic.setAverageList(averageList);
        newCcnStatistic = ccnStatisticRepository.save(newCcnStatistic);

        // 异步统计非new状态的告警数
        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(taskVO.getCreateFrom())) {
            rabbitTemplate.convertAndSend(EXCHANGE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE,
                    ROUTE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE, newCcnStatistic);
        } else {
            rabbitTemplate.convertAndSend(EXCHANGE_CLOSE_DEFECT_STATISTIC_CCN, ROUTE_CLOSE_DEFECT_STATISTIC_CCN, newCcnStatistic);
        }
    }

    /**
     * 根据复杂度获取风险等级枚举值
     */
    private int getRiskFactorVal(int ccn, int sh, int h, int m) {
        if (ccn >= sh) {
            return ComConstants.RiskFactor.SH.value();
        } else if (ccn < sh && ccn >= h) {
            return ComConstants.RiskFactor.H.value();
        } else if (ccn < h && ccn >= m) {
            return ComConstants.RiskFactor.M.value();
        } else {
            return ComConstants.RiskFactor.L.value();
        }
    }
}
