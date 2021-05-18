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
import com.tencent.bk.codecc.defect.dao.mongorepository.DUPCDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.DUPCStatisticRepository;
import com.tencent.bk.codecc.defect.model.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.model.DUPCStatisticEntity;
import com.tencent.bk.codecc.defect.service.AbstractDataReportBizService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.DupcChartRiskFactorVO;
import com.tencent.bk.codecc.defect.vo.DupcChartTrendListVO;
import com.tencent.bk.codecc.defect.vo.DupcChartTrendVO;
import com.tencent.bk.codecc.defect.vo.DupcDataReportRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDataReportRspVO;
import com.tencent.devops.common.api.pojo.GlobalMessage;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.devops.common.constant.RedisKeyConstants.GLOBAL_DATA_REPORT_DATE;

/**
 * 重复率数据报表
 *
 * @version V1.0
 * @date 2019/6/3
 */
@Service("DUPCDataReportBizService")
public class DUPCDataReportBizServiceImpl extends AbstractDataReportBizService
{
    @Autowired
    private DUPCDefectRepository dupcDefectRepository;

    @Autowired
    private DUPCStatisticRepository dupcStatisticRepository;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Override
    public CommonDataReportRspVO getDataReport(Long taskId, String toolName, int size, String startTime, String endTime)
    {
        // 检查日期有效性
        DateTimeUtils.checkDateValidity(startTime, endTime);

        DupcDataReportRspVO ducpDataReportRsp = new DupcDataReportRspVO();
        ducpDataReportRsp.setTaskId(taskId);
        ducpDataReportRsp.setToolName(toolName);
        ducpDataReportRsp.setChartTrendList(getDupcChartTrend(taskId, toolName, size, startTime, endTime));
        List<DUPCDefectEntity> fileInfoList = dupcDefectRepository.getByTaskIdAndStatus(taskId, ComConstants.DefectStatus.NEW.value());
        DupcChartRiskFactorVO dupcChartRiskFactorVO = CollectionUtils.isNotEmpty(fileInfoList) ? getChartRiskFactor(fileInfoList) : new DupcChartRiskFactorVO();
        ducpDataReportRsp.setChartRiskList(dupcChartRiskFactorVO);
        return ducpDataReportRsp;
    }


    /**
     * 获取重复文件分布列表 [风险级别列表]
     *
     * @param fileInfoList
     * @return
     */
    private DupcChartRiskFactorVO getChartRiskFactor(List<DUPCDefectEntity> fileInfoList)
    {
        DupcChartRiskFactorVO dupcRskFactor = new DupcChartRiskFactorVO();

        // 统计的数据
        AtomicInteger shCount = new AtomicInteger();
        AtomicInteger hCount = new AtomicInteger();
        AtomicInteger mCount = new AtomicInteger();

        Map<String, String> riskConfigMap = thirdPartySystemCaller.getRiskFactorConfig(ComConstants.Tool.DUPC.name());
        float sh = Float.valueOf(riskConfigMap.get(ComConstants.RiskFactor.SH.name()));
        float h = Float.valueOf(riskConfigMap.get(ComConstants.RiskFactor.H.name()));
        float m = Float.valueOf(riskConfigMap.get(ComConstants.RiskFactor.M.name()));

        fileInfoList.forEach(dupc ->
        {
            // 工具侧上报的文件代码重复率是带%号的，比如12.57%，所以要先去除掉后面的百分号比较
            float dupcRate = convertDupRate2Float(dupc.getDupRate());

            if (MapUtils.isNotEmpty(riskConfigMap))
            {
                if (dupcRate >= m && dupcRate < h)
                {
                    mCount.getAndIncrement();
                }
                else if (dupcRate >= h && dupcRate < sh)
                {
                    hCount.getAndIncrement();
                }
                else if (dupcRate >= sh)
                {
                    shCount.getAndIncrement();
                }
            }
        });

        int shCountInt = shCount.get();
        int hCountInt = hCount.get();
        int mCountInt = mCount.get();
        dupcRskFactor.setSuperHighCount(shCountInt);
        dupcRskFactor.setHighCount(hCountInt);
        dupcRskFactor.setMediumCount(mCountInt);
        dupcRskFactor.setTotalCount(shCountInt + hCountInt + mCountInt);

        return dupcRskFactor;
    }


    @Override
    public List<LocalDate> getShowDateList(int size, String startTime, String endTime)
    {
        List<LocalDate> dateList = Lists.newArrayList();
        if (StringUtils.isEmpty(startTime) && StringUtils.isEmpty(endTime))
        {
            // 当前日期
            if (size == 0)
            {
                size = 12;
            }
            LocalDate currentDate = LocalDate.now();
            dateList.add(currentDate);
            for (int i = 0; i < size - 1; i++)
            {
                LocalDate mondayOfWeek = currentDate.minusWeeks(i).with(DayOfWeek.MONDAY);
                if (!currentDate.equals(mondayOfWeek))
                {
                    dateList.add(mondayOfWeek);
                }
            }
        }
        else
        {
            getWeekDateListByDate(startTime, endTime, dateList);
        }
        return dateList;
    }

    /**
     * 获取重复率趋势图
     *
     * @param taskId
     * @param toolName
     * @return
     */
    private DupcChartTrendListVO getDupcChartTrend(Long taskId, String toolName, int size, String startTime,
            String endTime)
    {
        // 趋势图展示的日期 [12周的dupc报表]
        List<LocalDate> dateList = getShowDateList(size, startTime, endTime);
        LocalDate todayDate = LocalDate.now();
        LocalDate lastDate = todayDate.with(DayOfWeek.MONDAY);

        // 数据报表日期国际化
        Map<String, GlobalMessage> globalMessageMap = globalMessageUtil.getGlobalMessageMap(GLOBAL_DATA_REPORT_DATE);
        List<DUPCStatisticEntity> statistic = dupcStatisticRepository.findByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);

        // 如果是选择了日期则不需要展示国际化描述
        boolean dateRangeFlag = true;
        if (StringUtils.isEmpty(startTime) && StringUtils.isEmpty(endTime))
        {
            dateRangeFlag = false;
        }

        // 平均复杂度列表
        List<DupcChartTrendVO> chartAverageList = new ArrayList<>(dateList.size());
        for (LocalDate date : dateList)
        {
            DupcChartTrendVO chartTrend = new DupcChartTrendVO();
            chartTrend.setDate(date.toString());
            chartTrend.setTips(setTips(date, todayDate, lastDate, globalMessageMap, dateRangeFlag));

            // 没有分析记录，则每个日期的平均复杂度为0
            float dupc = ComConstants.COMMON_NUM_0F;
            if (CollectionUtils.isNotEmpty(statistic))
            {
                // 时间按倒序排序
                statistic.sort(Comparator.comparing(DUPCStatisticEntity::getTime).reversed());

                // 获取比当前日期小的第一个值
                DUPCStatisticEntity statisticEntity = statistic.stream()
                        .filter(an -> localDate2Millis(date.plusDays(1)) > an.getTime())
                        .findFirst().orElseGet(DUPCStatisticEntity::new);

                if(Objects.nonNull(statisticEntity.getDupRate())){
                    dupc = statisticEntity.getDupRate();
                }
            }

            chartTrend.setDupc((float) (Math.round(dupc * 100)) / 100);
            chartAverageList.add(chartTrend);
        }

        DupcChartTrendListVO result = new DupcChartTrendListVO();
        result.setDucpChartList(chartAverageList);
        result.setMaxMinHeight();

        return result;
    }

    /**
     * 将重复率的百分数转换成浮点数
     * 工具侧上报的文件代码重复率是带%号的，比如12.57%，所以要先去除掉后面的百分号比较
     *
     * @param dupRateStr
     * @return
     */
    private float convertDupRate2Float(String dupRateStr)
    {
        float dupRate = 0;
        if (StringUtils.isNotBlank(dupRateStr))
        {
            dupRate = Float.valueOf(dupRateStr.substring(0, dupRateStr.length() - 1));
        }
        return dupRate;
    }

}
