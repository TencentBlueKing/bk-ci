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

import com.tencent.bk.codecc.defect.constant.DefectConstants;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNStatisticRepository;
import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.service.AbstractDataReportBizService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.*;
import com.tencent.bk.codecc.defect.vo.common.CommonDataReportRspVO;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

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
    public CommonDataReportRspVO getDataReport(Long taskId, String toolName, int size)
    {
        CCNDataReportRspVO ccnDataReportRes = new CCNDataReportRspVO();
        ccnDataReportRes.setTaskId(taskId);
        ccnDataReportRes.setToolName(toolName);
        // 获取函数平均圈复杂度趋势
        ccnDataReportRes.setChartAverageList(getChartAverageList(taskId, toolName, size));

        // 查询CCN告警类，只查询NEW的告警
        List<CCNDefectEntity> ccnDefectList = ccnDefectRepository.findByTaskIdAndStatus(taskId, DefectConstants.DefectStatus.NEW.value());
        ChartAuthorListVO chartAuthorListVO = CollectionUtils.isNotEmpty(ccnDefectList) ? getChartAuthorList(ccnDefectList) : new ChartAuthorListVO();
        // 获取告警作者分布列表
        ccnDataReportRes.setChartAuthorList(chartAuthorListVO);

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
        Map<String, ChartAuthorVO> chartAuthorMap = new HashMap<>();

        //获取风险系数值
        Map<String, String> riskConfigMap = thirdPartySystemCaller.getRiskFactorConfig(ComConstants.Tool.CCN.name());

        ccnFileEntity.stream()
                .filter(ccn -> StringUtils.isNotBlank(ccn.getAuthor()))
                .forEach(ccn ->
                {
                    String author = ccn.getAuthor();
                    ChartAuthorVO chartAuthor = chartAuthorMap.get(author);
                    if (Objects.isNull(chartAuthor))
                    {
                        chartAuthor = new ChartAuthorVO();
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
    private ChartAverageListVO getChartAverageList(Long taskId, String toolName, int size)
    {
        // 趋势图展示的日期 [12周的ccn报表]
        List<LocalDate> dateList = super.getShowDateList(size);
        LocalDate todayDate = dateList.get(0);
        LocalDate lastDate = dateList.get(1);

        List<CCNStatisticEntity> statistic = ccnStatisticRepository.findByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);

        // 平均复杂度列表
        List<ChartAverageVO> chartAverageList = new ArrayList<>(dateList.size());
        dateList.forEach(date ->
        {
            ChartAverageVO chartAverage = new ChartAverageVO();
            chartAverage.setDate(date.toString());
            chartAverage.setTips(setTips(date, todayDate, lastDate));

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

                averageCcn = analysisEntity.getAverageCCN();
            }

            chartAverage.setAverageCCN((float) (Math.round(averageCcn * 100)) / 100);
            chartAverageList.add(chartAverage);

        });

        ChartAverageListVO result = new ChartAverageListVO();
        result.setAverageList(chartAverageList);
        result.setMaxMinHeight();

        return result;
    }


    /**
     * 设置严重程度数量
     *
     * @param ccn
     * @param author
     */
    private void setLevel(int ccn, ChartAuthorVO author, Map<String, String> riskConfigMap)
    {
        // 统计的数据
        int serious = author.getSerious();
        int normal = author.getNormal();
        int prompt = author.getPrompt();

        if (MapUtils.isNotEmpty(riskConfigMap))
        {
            int sh = Integer.valueOf(riskConfigMap.get(ComConstants.RiskFactor.SH.name()));
            int h = Integer.valueOf(riskConfigMap.get(ComConstants.RiskFactor.H.name()));
            int m = Integer.valueOf(riskConfigMap.get(ComConstants.RiskFactor.M.name()));

            if (ccn >= sh)
            {
                ++serious;
            }
            else if (ccn >= h)
            {
                ++normal;
            }
            else if (ccn >= m)
            {
                ++prompt;
            }

        }

        author.setSerious(serious);
        author.setNormal(normal);
        author.setPrompt(prompt);
        author.setTotal(author.getSerious() + author.getNormal() + author.getPrompt());
    }



}
