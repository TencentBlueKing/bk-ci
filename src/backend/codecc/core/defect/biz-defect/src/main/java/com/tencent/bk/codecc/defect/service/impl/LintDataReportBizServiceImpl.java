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
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintStatisticRepository;
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.LintStatisticEntity;
import com.tencent.bk.codecc.defect.service.AbstractDataReportBizService;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.defect.vo.ChartLegacyListVO;
import com.tencent.bk.codecc.defect.vo.ChartLegacyVO;
import com.tencent.bk.codecc.defect.vo.LintChartAuthorListVO;
import com.tencent.bk.codecc.defect.vo.LintDataReportRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDataReportRspVO;
import com.tencent.bk.codecc.defect.vo.report.ChartAuthorListVO;
import com.tencent.bk.codecc.defect.vo.report.CommonChartAuthorVO;
import com.tencent.devops.common.api.pojo.GlobalMessage;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.devops.common.constant.RedisKeyConstants.GLOBAL_DATA_REPORT_DATE;

/**
 * Lint类工具的数据报表实现
 *
 * @version V1.0
 * @date 2019/5/29
 */
@Slf4j
@Service("LINTDataReportBizService")
public class LintDataReportBizServiceImpl extends AbstractDataReportBizService {

    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;
    @Autowired
    private LintStatisticRepository lintStatisticRepository;
    @Autowired
    private NewDefectJudgeService newDefectJudgeService;

    /**
     * lint类数据报表
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Override
    public CommonDataReportRspVO getDataReport(Long taskId, String toolName, int size, String startTime, String endTime) {
        // 检查日期有效性
        DateTimeUtils.checkDateValidity(startTime, endTime);

        LintDataReportRspVO lintDataReportRes = new LintDataReportRspVO();
        lintDataReportRes.setTaskId(taskId);
        lintDataReportRes.setToolName(toolName);

        // 查询新老告警判定时间
        long newDefectJudgeTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, toolName, null);

        // 告警作者分布列表
        LintChartAuthorListVO lintChartAuthorListVO = new LintChartAuthorListVO();
        LintStatisticEntity curStatistic = new LintStatisticEntity();

        // 分页的统计当前遗留告警及各个作者的告警数
        pagingStatisticDefect(taskId, toolName, newDefectJudgeTime, curStatistic, lintChartAuthorListVO);

        // 获取告警遗留分布列表
        lintDataReportRes.setChartLegacys(getChartLegacyList(taskId, toolName, size, startTime, endTime, curStatistic));

        lintDataReportRes.setChartAuthors(lintChartAuthorListVO);

        return lintDataReportRes;
    }

    /**
     * 分页的统计当前遗留告警
     *
     * @param taskId
     * @param toolName
     * @param newDefectJudgeTime
     * @param lintChartAuthorListVO
     */
    protected void pagingStatisticDefect(Long taskId, String toolName, long newDefectJudgeTime,
                                         LintStatisticEntity curStatistic,
                                         LintChartAuthorListVO lintChartAuthorListVO) {
        // 新告警
        ChartAuthorListVO newAuthorList = new ChartAuthorListVO();
        // 历史告警
        ChartAuthorListVO historyAuthorList = new ChartAuthorListVO();

        Map<String, CommonChartAuthorVO> newChartAuthorMap = new HashMap<>();
        Map<String, CommonChartAuthorVO> historyChartAuthorMap = new HashMap<>();

        int pageNum = 0;
        int pageSize = 100000;
        int totalPage;
        do {
            log.info("query page:{} for taskId:{}, toolName:{}", pageNum, taskId, toolName);
            Pageable pageable = PageRequest.of(pageNum, pageSize);
            Page<LintDefectV2Entity> defectPage = lintDefectV2Repository.findByTaskIdAndToolNameAndStatus(
                    taskId, toolName, ComConstants.DefectStatus.NEW.value(), pageable);
            totalPage = defectPage.getTotalPages();

            // 超过100W告警则不继续统计
            if (totalPage > 10){
                log.info("defect is too much to statistic, taskId:{}, toolName:{}, totalDefect: {}",
                        taskId, toolName, defectPage);
                return;
            }
            defectPage.forEach(defect -> {
                // 判断是新告警还是历史告警
                long defectCreateTime = DateTimeUtils.getThirteenTimestamp(defect.getLineUpdateTime());
                if (defectCreateTime >= newDefectJudgeTime) {
                    curStatistic.setNewDefectCount(
                            (curStatistic.getNewDefectCount() == null ? 0 : curStatistic.getNewDefectCount()) + 1);

                    calculateAuthorDefectCount(newChartAuthorMap, defect);
                } else {
                    curStatistic.setHistoryDefectCount((curStatistic.getHistoryDefectCount() == null
                            ? 0 : curStatistic.getHistoryDefectCount()) + 1);

                    calculateAuthorDefectCount(historyChartAuthorMap, defect);
                }
            });

            pageNum++;
        }
        while (pageNum < totalPage);

        newAuthorList.setAuthorList(new ArrayList<>(newChartAuthorMap.values()));
        historyAuthorList.setAuthorList(new ArrayList<>(historyChartAuthorMap.values()));

        curStatistic.setTime(System.currentTimeMillis());

        // 统计总数
        super.setTotalChartAuthor(newAuthorList);
        super.setTotalChartAuthor(historyAuthorList);
        lintChartAuthorListVO.setNewAuthorList(newAuthorList);
        lintChartAuthorListVO.setHistoryAuthorList(historyAuthorList);
    }

    /**
     * 获取告警遗留分布列表
     *
     * @param taskId
     * @param toolName
     * @param size
     * @param startTime
     * @param endTime
     * @param curStatistic
     * @return
     */
    private ChartLegacyListVO getChartLegacyList(long taskId, String toolName, int size, String startTime,
                                                 String endTime, LintStatisticEntity curStatistic) {
        // 获取lint分析记录数据
        List<LintStatisticEntity> lintStatisticList = lintStatisticRepository.findByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);
        if (CollectionUtils.isNotEmpty(lintStatisticList)) {
            // 获取遗留告警统计作为当前最新的统计
            lintStatisticList.add(curStatistic);

            // 时间按倒序排序
            lintStatisticList.sort(Comparator.comparing(LintStatisticEntity::getTime).reversed());
        }

        // 数据报表日期国际化
        Map<String, GlobalMessage> globalMessageMap = globalMessageUtil.getGlobalMessageMap(GLOBAL_DATA_REPORT_DATE);

        // 视图显示的时间列表
        List<LocalDate> dateTimeList = getShowDateList(size, startTime, endTime);
        LocalDate todayDate = LocalDate.now();
        LocalDate yesterdayDate = todayDate.minusDays(1);

        // 如果是选择了日期则不需要展示国际化描述
        boolean dateRangeFlag = true;
        if (StringUtils.isEmpty(startTime) && StringUtils.isEmpty(endTime)) {
            dateRangeFlag = false;
        }

        List<ChartLegacyVO> legacyList = new ArrayList<>();
        for (LocalDate date : dateTimeList) {
            ChartLegacyVO chartLegacy = new ChartLegacyVO();

            // 设置时间
            String dateString = date.toString();
            chartLegacy.setDate(dateString);

            // 设置前端展示的tip
            String tips;
            if (dateRangeFlag) {
                tips = dateString;
            } else {
                tips = dateString.equals(todayDate.toString()) ?
                        globalMessageUtil.getMessageByLocale(globalMessageMap.get(ComConstants.DATE_TODAY)) :
                        dateString.equals(yesterdayDate.toString()) ? globalMessageUtil
                                .getMessageByLocale(globalMessageMap.get(ComConstants.DATE_YESTERDAY)) : dateString;
            }
            chartLegacy.setTips(tips.substring(tips.indexOf("-") + 1));

            if (CollectionUtils.isNotEmpty(lintStatisticList)) {
                // 获取比当前日期小的第一个值
                LintStatisticEntity statisticEntity = lintStatisticList.stream()
                        .filter(an -> localDate2Millis(date.plusDays(1)) > an.getTime())
                        .findFirst().orElseGet(LintStatisticEntity::new);

                if (Objects.nonNull(statisticEntity.getNewDefectCount())) {
                    chartLegacy.setNewCount(statisticEntity.getNewDefectCount());
                }
                if (Objects.nonNull(statisticEntity.getHistoryDefectCount())) {
                    chartLegacy.setHistoryCount(statisticEntity.getHistoryDefectCount());
                }
            }
            legacyList.add(chartLegacy);
        }

        if (CollectionUtils.isNotEmpty(legacyList)) {
            legacyList.sort(Comparator.comparing(ChartLegacyVO::getDate).reversed());
        }

        ChartLegacyListVO result = new ChartLegacyListVO();
        result.setLegacyList(legacyList);
        result.setMaxMinHeight();

        return result;
    }

    private void calculateAuthorDefectCount(Map<String, CommonChartAuthorVO> chartAuthorMap, LintDefectV2Entity defect) {
        String author = StringUtils.isEmpty(defect.getAuthor()) ? "No Author" : defect.getAuthor();
        CommonChartAuthorVO authorVO = chartAuthorMap.get(author);
        if (authorVO == null) {
            authorVO = new CommonChartAuthorVO();
            authorVO.setAuthorName(author);
            chartAuthorMap.put(author, authorVO);
        }

        // 计算每个严重等级的告警数
        switch (defect.getSeverity()) {
            case ComConstants.SERIOUS:
                authorVO.setSerious(authorVO.getSerious() + 1);
                break;
            case ComConstants.NORMAL:
                authorVO.setNormal(authorVO.getNormal() + 1);
                break;
            case ComConstants.PROMPT_IN_DB:
                authorVO.setPrompt(authorVO.getPrompt() + 1);
                break;
            default:
                break;
        }

        authorVO.setTotal(authorVO.getTotal() + 1);
    }

    @Override
    public List<LocalDate> getShowDateList(int size, String startTime, String endTime) {
        List<LocalDate> dateList = Lists.newArrayList();
        if (StringUtils.isEmpty(startTime) && StringUtils.isEmpty(endTime)) {
            if (size == 0) {
                size = 7;
            }
            LocalDate currentDate = LocalDate.now();
            for (int i = 0; i < size; i++) {
                dateList.add(currentDate.minusDays(i));
            }
        } else {
            generateDateList(startTime, endTime, dateList);
        }
        return dateList;
    }
}
