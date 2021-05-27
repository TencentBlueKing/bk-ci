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

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNStatisticRepository;
import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.service.AbstractDataReportBizService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.*;
import com.tencent.bk.codecc.defect.vo.common.CommonDataReportRspVO;
import com.tencent.bk.codecc.defect.vo.report.CCNChartAuthorVO;
import com.tencent.bk.codecc.defect.vo.report.ChartAuthorListVO;
import com.tencent.devops.common.api.pojo.GlobalMessage;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static com.tencent.devops.common.constant.RedisKeyConstants.GLOBAL_DATA_REPORT_DATE;

/**
 * ccn类工具的数据报表实现
 *
 * @version V1.0
 * @date 2019/5/31
 */
@Service("CCNDataReportBizService")
public class CCNDataReportBizServiceImpl extends AbstractDataReportBizService
{

    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Autowired
    private CCNStatisticRepository ccnStatisticRepository;

    @Override
    public CommonDataReportRspVO getDataReport(Long taskId, String toolName, int size, String startTime, String endTime)
    {
        // 检查日期有效性
        DateTimeUtils.checkDateValidity(startTime, endTime);

        CCNDataReportRspVO ccnDataReportRes = new CCNDataReportRspVO();
        ccnDataReportRes.setTaskId(taskId);
        ccnDataReportRes.setToolName(toolName);
        // 获取函数平均圈复杂度趋势
        ccnDataReportRes.setChartAverageList(getChartAverageList(taskId, toolName, size, startTime, endTime));

        // 查询CCN告警类，只查询NEW的告警
        List<CCNDefectEntity> ccnDefectList = ccnDefectRepository.findByTaskIdAndStatus(taskId, ComConstants.DefectStatus.NEW.value());
        ChartAuthorListVO chartAuthorListVO = CollectionUtils.isNotEmpty(ccnDefectList) ? getChartAuthorList(ccnDefectList) : new ChartAuthorListVO();
        // 获取告警作者分布列表
        ccnDataReportRes.setChartAuthorList(chartAuthorListVO);

        // 获取超标复杂度趋势图
        ChartAverageListVO chartBeyondThresholdList =
                getChartBeyondThresholdList(taskId, toolName, size, startTime, endTime);
        ccnDataReportRes.setChartBeyondThresholdList(chartBeyondThresholdList);
        return ccnDataReportRes;
    }


    /**
     * 获取告警作者分布列表
     *
     * @param ccnFileEntity
     * @return
     */
    private ChartAuthorListVO getChartAuthorList(List<CCNDefectEntity> ccnFileEntity)
    {
        // 作者对应的图表数据Map
        Map<String, CCNChartAuthorVO> chartAuthorMap = new HashMap<>();

        //获取风险系数值
        Map<String, String> riskConfigMap = thirdPartySystemCaller.getRiskFactorConfig(ComConstants.Tool.CCN.name());

        ccnFileEntity.stream()
                .filter(ccn -> StringUtils.isNotBlank(ccn.getAuthor()))
                .forEach(ccn ->
                {
                    String author = ccn.getAuthor();
                    CCNChartAuthorVO chartAuthor = chartAuthorMap.get(author);
                    if (Objects.isNull(chartAuthor))
                    {
                        chartAuthor = new CCNChartAuthorVO();
                        chartAuthor.setAuthorName(author);
                    }

                    setLevel(ccn.getCcn(), chartAuthor, riskConfigMap);
                    chartAuthorMap.put(author, chartAuthor);

                });

        ChartAuthorListVO chartAuthorListVO = new ChartAuthorListVO();
        if (MapUtils.isNotEmpty(chartAuthorMap))
        {
            chartAuthorListVO.setAuthorList(new ArrayList<>(chartAuthorMap.values()));
            super.setTotalChartAuthor(chartAuthorListVO);
        }

        return chartAuthorListVO;
    }


    /**
     * 获取平均复杂度列表
     *
     * @return
     */
    private ChartAverageListVO getChartAverageList(Long taskId, String toolName, int size, String startTime,
            String endTime)
    {
        // 趋势图展示的日期 [12周的ccn报表]
        List<LocalDate> dateList = getShowDateList(size, startTime, endTime);
        LocalDate todayDate = LocalDate.now();
        LocalDate lastDate = todayDate.with(DayOfWeek.MONDAY);

        // 如果是选择了日期则不需要展示国际化描述
        boolean dateRangeFlag = true;
        if (StringUtils.isEmpty(startTime) && StringUtils.isEmpty(endTime))
        {
            dateRangeFlag = false;
        }

        // 数据报表日期国际化
        Map<String, GlobalMessage> globalMessageMap = globalMessageUtil.getGlobalMessageMap(GLOBAL_DATA_REPORT_DATE);
        List<CCNStatisticEntity> statistic = ccnStatisticRepository.findByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);

        // 平均复杂度列表
        List<ChartAverageVO> chartAverageList = new ArrayList<>(dateList.size());
        for (LocalDate date : dateList)
        {
            ChartAverageVO chartAverage = new ChartAverageVO();
            chartAverage.setDate(date.toString());
            chartAverage.setTips(setTips(date, todayDate, lastDate, globalMessageMap, dateRangeFlag));

            // 没有分析记录，则每个日期的平均复杂度为0
            float averageCcn = ComConstants.COMMON_NUM_0F;
            if (CollectionUtils.isNotEmpty(statistic))
            {
                // 时间按倒序排序
                statistic.sort(Comparator.comparing(CCNStatisticEntity::getTime).reversed());

                // 获取比当前日期小的第一个值
                CCNStatisticEntity analysisEntity = statistic.stream()
                        .filter(an -> localDate2Millis(date.plusDays(1)) > an.getTime())
                        .findFirst().orElseGet(CCNStatisticEntity::new);

                if(Objects.nonNull(analysisEntity.getAverageCCN())){
                    averageCcn = analysisEntity.getAverageCCN();
                }
            }

            chartAverage.setAverageCCN((float) (Math.round(averageCcn * 100)) / 100);
            chartAverageList.add(chartAverage);
        }

        ChartAverageListVO result = new ChartAverageListVO();
        result.setAverageList(chartAverageList);
        result.setMaxMinHeight();

        return result;
    }

    /**
     * 获取超标圈复杂度趋势图
     *
     * @param taskId        任务ID
     * @param toolName      工具名称
     * @param size          元素个数
     * @return chart
     */
    private ChartAverageListVO getChartBeyondThresholdList(Long taskId, String toolName, int size, String startTime, String endTime)
    {
        List<CCNStatisticEntity> statistic = ccnStatisticRepository.findByTaskIdAndToolName(taskId, toolName);
        // 时间按倒序排序
        statistic.sort(Comparator.comparing(CCNStatisticEntity::getTime).reversed());

        // 如果是选择了日期则不需要展示国际化描述
        boolean dateRangeFlag = true;
        if (StringUtils.isEmpty(startTime) && StringUtils.isEmpty(endTime))
        {
            dateRangeFlag = false;
        }

        // 趋势图展示的日期 [12周的ccn报表]
        List<LocalDate> dateList = getShowDateList(size, startTime, endTime);
        LocalDate todayDate = LocalDate.now();
        LocalDate lastDate = todayDate.with(DayOfWeek.MONDAY);

        // 数据报表日期国际化
        Map<String, GlobalMessage> globalMessageMap = globalMessageUtil.getGlobalMessageMap(GLOBAL_DATA_REPORT_DATE);

        List<ChartAverageVO> chartAverageList = Lists.newArrayListWithCapacity(dateList.size());
        for (LocalDate localDate : dateList)
        {
            ChartAverageVO chartAverage = new ChartAverageVO();
            chartAverage.setDate(localDate.toString());
            chartAverage.setTips(setTips(localDate, todayDate, lastDate, globalMessageMap, dateRangeFlag));

            // 获取比当前日期小的第一个值
            CCNStatisticEntity analysisEntity =
                    statistic.stream().filter(an -> localDate2Millis(localDate.plusDays(1)) > an.getTime()).findFirst()
                            .orElseGet(CCNStatisticEntity::new);

            int ccnBeyondThresholdSum = 0;
            Integer ccnSum = analysisEntity.getCcnBeyondThresholdSum();
            if (ccnSum != null)
            {
                ccnBeyondThresholdSum = ccnSum;
            }

            chartAverage.setCcnBeyondThresholdSum(ccnBeyondThresholdSum);
            chartAverageList.add(chartAverage);
        }

        ChartAverageListVO result = new ChartAverageListVO();
        result.setAverageList(chartAverageList);
        result.setMaxMinHeightSum();

        return result;
    }


    /**
     * 设置严重程度数量
     *
     * @param ccn
     * @param author
     */
    private void setLevel(int ccn, CCNChartAuthorVO author, Map<String, String> riskConfigMap)
    {
        // 统计的数据
        int superHigh = author.getSuperHigh();
        int high = author.getHigh();
        int medium = author.getMedium();
        int low = author.getLow();

        if (MapUtils.isNotEmpty(riskConfigMap))
        {
            int sh = Integer.valueOf(riskConfigMap.get(ComConstants.RiskFactor.SH.name()));
            int h = Integer.valueOf(riskConfigMap.get(ComConstants.RiskFactor.H.name()));
            int m = Integer.valueOf(riskConfigMap.get(ComConstants.RiskFactor.M.name()));

            if (ccn >= sh)
            {
                ++superHigh;
            }
            else if (ccn >= h && ccn < sh)
            {
                ++high;
            }
            else if (ccn >= m && ccn < h)
            {
                ++medium;
            }
            else if (ccn < m)
            {
                ++low;
            }
        }

        author.setSuperHigh(superHigh);
        author.setHigh(high);
        author.setMedium(medium);
        author.setLow(low);
        author.setTotal(author.getSuperHigh() + author.getHigh() + author.getMedium() + author.getLow());
    }


    @Override
    public List<LocalDate> getShowDateList(int size, String startTime, String endTime)
    {
        // [2019-12-24, 2019-12-23, 2019-12-16, 2019-12-09, 2019-12-02, 2019-12-01]
        List<LocalDate> dateList = Lists.newArrayList();
        if (StringUtils.isNotEmpty(startTime) || StringUtils.isNotEmpty(endTime))
        {
            // 开始时间
            getWeekDateListByDate(startTime, endTime, dateList);
        }
        else
        {
            // 如果开始、结束时间都为空 则按日期节点数展示
            if (size == 0)
            {
                size = 12;
            }
            LocalDate currentDate = LocalDate.now();

            dateList.add(currentDate);
            for (int i = 0; i < size - 1; i++)
            {
                // 按每周一维度
                LocalDate mondayOfWeek = currentDate.minusWeeks(i).with(DayOfWeek.MONDAY);
                if (!currentDate.equals(mondayOfWeek))
                {
                    dateList.add(mondayOfWeek);
                }
            }
        }
        return dateList;
    }


}
