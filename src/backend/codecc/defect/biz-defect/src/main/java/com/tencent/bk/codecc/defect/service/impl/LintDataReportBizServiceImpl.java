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

import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintStatisticRepository;
import com.tencent.bk.codecc.defect.model.LintDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.model.LintStatisticEntity;
import com.tencent.bk.codecc.defect.service.AbstractDataReportBizService;
import com.tencent.bk.codecc.defect.vo.*;
import com.tencent.bk.codecc.defect.vo.common.CommonDataReportRspVO;
import com.tencent.devops.common.api.pojo.GlobalMessage;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.utils.GlobalMessageUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.RedisKeyConstants.GLOBAL_DATA_REPORT_DATE;

/**
 * Lint类工具的数据报表实现
 *
 * @version V1.0
 * @date 2019/5/29
 */
@Service("LINTDataReportBizService")
public class LintDataReportBizServiceImpl extends AbstractDataReportBizService
{

    @Autowired
    private GlobalMessageUtil globalMessageUtil;

    @Autowired
    private LintDefectRepository lintDefectRepository;

    @Autowired
    private LintStatisticRepository lintStatisticRepository;

    /**
     * lint类数据报表
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Override
    public CommonDataReportRspVO getDataReport(Long taskId, String toolName, int size)
    {
        LintDataReportRspVO lintDataReportRes = new LintDataReportRspVO();
        // 取出lintFile status为new的文件
        List<LintFileEntity> lintFileEntity = lintDefectRepository.findByTaskIdAndToolNameAndStatus(taskId, toolName, ComConstants.TaskFileStatus.NEW.value());
        if (CollectionUtils.isEmpty(lintFileEntity))
        {
            return null;
        }

        // 取出defectList中的status为new告警的文件
        List<LintDefectEntity> lintDefect = lintFileEntity.stream()
                .filter(lint -> CollectionUtils.isNotEmpty(lint.getDefectList()))
                .map(LintFileEntity::getDefectList)
                .flatMap(Collection::stream)
                .filter(lint -> ComConstants.DefectStatus.NEW.value() == lint.getStatus())
                .collect(Collectors.toList());

        lintDataReportRes.setTaskId(taskId);
        lintDataReportRes.setToolName(toolName);
        // 获取告警遗留分布列表
        lintDataReportRes.setChartLegacys(getChartLegacyList(taskId, toolName, size));
        // 获取告警作者分布列表
        lintDataReportRes.setChartAuthors(getChartAuthorList(lintDefect));

        return lintDataReportRes;
    }


    /**
     * 获取告警遗留分布列表
     *
     * @param taskId
     * @param toolName
     * @return
     */
    private ChartLegacyListVO getChartLegacyList(long taskId, String toolName, int size)
    {
        // 获取lint分析记录数据
        List<LintStatisticEntity> lintStatistic = lintStatisticRepository
                .findByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);

        // 数据报表日期国际化
        Map<String, GlobalMessage> globalMessageMap = globalMessageUtil.getGlobalMessageMap(GLOBAL_DATA_REPORT_DATE);

        // 视图显示的时间列表
        List<LocalDate> dateTimeList = getShowDateList(size);
        LocalDate todayDate = dateTimeList.get(0);
        LocalDate yesterdayDate = dateTimeList.get(1);

        List<ChartLegacyVO> legacyList = new ArrayList<>();
        dateTimeList.forEach(date ->
        {
            ChartLegacyVO chartLegacy = new ChartLegacyVO();

            // 设置时间
            String dateString = date.toString();
            chartLegacy.setDate(dateString);

            // 设置前端展示的tip
            String tips = dateString.equals(todayDate.toString()) ? globalMessageUtil.getMessageByLocale(globalMessageMap.get(ComConstants.DATE_TODAY)) :
                    dateString.equals(yesterdayDate.toString()) ? globalMessageUtil.getMessageByLocale(globalMessageMap.get(ComConstants.DATE_YESTERDAY)) : dateString;
            chartLegacy.setTips(tips.substring(tips.indexOf("-") + 1));

            if (CollectionUtils.isNotEmpty(lintStatistic))
            {
                // 时间按倒序排序
                lintStatistic.sort(Comparator.comparing(LintStatisticEntity::getTime).reversed());

                // 获取比当前日期小的第一个值
                LintStatisticEntity statisticEntity = lintStatistic.stream()
                        .filter(an -> localDate2Millis(date.plusDays(1)) > an.getTime())
                        .findFirst().orElseGet(LintStatisticEntity::new);

                chartLegacy.setNewCount(statisticEntity.getNewDefectCount());
                chartLegacy.setHistoryCount(statisticEntity.getHistoryDefectCount());

            }
            legacyList.add(chartLegacy);

        });


        if (CollectionUtils.isNotEmpty(legacyList))
        {
            legacyList.sort(Comparator.comparing(ChartLegacyVO::getDate).reversed());
        }

        ChartLegacyListVO result = new ChartLegacyListVO();
        result.setLegacyList(legacyList);
        result.setMaxMinHeight();

        return result;
    }


    /**
     * 获取告警作者分布列表
     *
     * @param lintDefect
     * @return
     */
    private LintChartAuthorListVO getChartAuthorList(List<LintDefectEntity> lintDefect)
    {
        // 新告警
        ChartAuthorListVO newAuthorList = new ChartAuthorListVO();
        // 历史告警
        ChartAuthorListVO historyAuthorList = new ChartAuthorListVO();

        List<Integer> defectTypes = Arrays.asList(ComConstants.DefectType.NEW.value(), ComConstants.DefectType.HISTORY.value());
        defectTypes.forEach(type ->
        {
            if (CollectionUtils.isNotEmpty(lintDefect))
            {
                // 按照告警作者分组
                Map<String, List<LintDefectEntity>> authorLintDefectMap = lintDefect.stream()
                        .filter(file -> type.equals(file.getDefectType()))
                        .filter(file -> (StringUtils.isNotBlank(file.getAuthor())))
                        .collect(Collectors.groupingBy(LintDefectEntity::getAuthor));

                Map<String, ChartAuthorVO> chartAuthorMap = getChartAuthorSeverityMap(authorLintDefectMap);

                // 设置作者列表
                if (MapUtils.isNotEmpty(chartAuthorMap))
                {
                    List<ChartAuthorVO> chartAuthors = new ArrayList<>(chartAuthorMap.values());

                    // 设置作者列表
                    if (ComConstants.DefectType.NEW.value() == type)
                    {
                        newAuthorList.setAuthorList(chartAuthors);
                    }
                    else
                    {
                        historyAuthorList.setAuthorList(chartAuthors);
                    }
                }
                else
                {
                    if (ComConstants.DefectType.NEW.value() == type)
                    {
                        newAuthorList.setAuthorList(new ArrayList<>());
                    }
                    else
                    {
                        historyAuthorList.setAuthorList(new ArrayList<>());
                    }
                }
            }
            else
            {
                // 设置默认值
                if (CollectionUtils.isEmpty(newAuthorList.getAuthorList()))
                {
                    newAuthorList.setAuthorList(new ArrayList<>());
                }
                if (CollectionUtils.isEmpty(historyAuthorList.getAuthorList()))
                {
                    historyAuthorList.setAuthorList(new ArrayList<>());
                }
            }

            // 统计总数
            super.setTotalChartAuthor(newAuthorList);
            super.setTotalChartAuthor(historyAuthorList);

        });

        LintChartAuthorListVO result = new LintChartAuthorListVO();
        result.setNewAuthorList(newAuthorList);
        result.setHistoryAuthorList(historyAuthorList);

        return result;
    }


    /**
     * 根据作者告警获取作者的严重等级信息
     *
     * @param authorDefectMap
     * @return
     */
    private Map<String, ChartAuthorVO> getChartAuthorSeverityMap(Map<String, List<LintDefectEntity>> authorDefectMap)
    {
        if (MapUtils.isEmpty(authorDefectMap))
        {
            return new HashMap<>();
        }

        Map<String, ChartAuthorVO> chartAuthorMap = new HashMap<>(authorDefectMap.size());
        authorDefectMap.keySet().forEach(author ->
        {
            // 按照作者的严重等级分组
            Map<Integer, LongSummaryStatistics> severityMap = authorDefectMap.get(author)
                    .stream()
                    .collect(Collectors.groupingBy(LintDefectEntity::getSeverity,
                            Collectors.summarizingLong(LintDefectEntity::getSeverity)));
            severityMap.forEach((severityKey, value) ->
            {
                int count = (int) value.getCount();
                ChartAuthorVO authorVO = chartAuthorMap.get(author);
                if (Objects.isNull(authorVO))
                {
                    authorVO = new ChartAuthorVO();
                    authorVO.setAuthorName(author);
                }

                // 设置作者告警的每个严重等级
                statisticsDefect(severityKey, count, authorVO);
                chartAuthorMap.put(author, authorVO);
            });
        });

        return chartAuthorMap;
    }


    private void statisticsDefect(Integer severityKey, Integer count, ChartAuthorVO authorVO)
    {
        switch (severityKey)
        {
            case ComConstants.SERIOUS:
                authorVO.setSerious(count);
                break;
            case ComConstants.NORMAL:
                authorVO.setNormal(count);
                break;
            case ComConstants.PROMPT:
                authorVO.setPrompt(count);
                break;
            default:
                break;
        }

        authorVO.setTotal(authorVO.getSerious() + authorVO.getNormal() + authorVO.getPrompt());
    }


    @Override
    public List<LocalDate> getShowDateList(int size)
    {
        if (size == 0)
        {
            size = 7;
        }
        LocalDate currentDate = LocalDate.now();
        List<LocalDate> dateTimeList = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
        {
            dateTimeList.add(currentDate.minusDays(i));
        }
        return dateTimeList;
    }


}
