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

package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.report.ChartAuthorBaseVO;
import com.tencent.bk.codecc.defect.vo.report.CCNChartAuthorVO;
import com.tencent.bk.codecc.defect.vo.report.ChartAuthorListVO;
import com.tencent.bk.codecc.defect.vo.report.CommonChartAuthorVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDataReportRspVO;
import com.tencent.devops.common.api.pojo.GlobalMessage;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.utils.GlobalMessageUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据报表抽象类
 *
 * @version V1.0
 * @date 2019/5/28
 */
public abstract class AbstractDataReportBizService implements IDataReportBizService
{
    private static Logger logger = LoggerFactory.getLogger(AbstractDataReportBizService.class);

    public abstract List<LocalDate> getShowDateList(int size, String startTime, String endTime);

    @Autowired
    protected TaskLogService taskLogService;

    @Autowired
    protected GlobalMessageUtil globalMessageUtil;

    @Override
    public CommonDataReportRspVO getDataReport(Long taskId, String toolName, String startTime, String endTime)
    {
        logger.info("data report params: taskId:{}, toolName:{}, startTime:{}, endTime:{}", taskId, toolName, startTime,
                endTime);
        return getDataReport(taskId, toolName, 0, startTime, endTime);
    }

    /**
     * 统计作者告警总数
     *
     * @param result
     */
    protected void setTotalChartAuthor(ChartAuthorListVO result)
    {
        if (Objects.isNull(result))
        {
            return;
        }

        AtomicInteger totalSerious = new AtomicInteger();
        AtomicInteger totalNormal = new AtomicInteger();
        AtomicInteger totalPrompt = new AtomicInteger();
        AtomicInteger totalSuperHigh = new AtomicInteger();
        AtomicInteger totalHigh = new AtomicInteger();
        AtomicInteger totalMedium = new AtomicInteger();
        AtomicInteger totalLow = new AtomicInteger();
        ChartAuthorBaseVO totalAuthor = null;
        List<ChartAuthorBaseVO> authorList = result.getAuthorList();
        if (CollectionUtils.isNotEmpty(authorList))
        {
            // 按作者排序
            authorList.sort(Comparator.comparing(ChartAuthorBaseVO::getAuthorName));
            for (ChartAuthorBaseVO chartAuthor : authorList)
            {
                if (chartAuthor instanceof CommonChartAuthorVO)
                {
                    if (totalAuthor == null)
                    {
                        totalAuthor = new CommonChartAuthorVO();
                    }
                    CommonChartAuthorVO commonChartAuthor = (CommonChartAuthorVO)chartAuthor;
                    totalSerious.addAndGet(commonChartAuthor.getSerious());
                    totalNormal.addAndGet(commonChartAuthor.getNormal());
                    totalPrompt.addAndGet(commonChartAuthor.getPrompt());
                }
                else if (chartAuthor instanceof CCNChartAuthorVO)
                {
                    if (totalAuthor == null)
                    {
                        totalAuthor = new CCNChartAuthorVO();
                    }
                    CCNChartAuthorVO ccnChartAuthor = (CCNChartAuthorVO)chartAuthor;
                    totalSuperHigh.addAndGet(ccnChartAuthor.getSuperHigh());
                    totalHigh.addAndGet(ccnChartAuthor.getHigh());
                    totalMedium.addAndGet(ccnChartAuthor.getMedium());
                    totalLow.addAndGet(ccnChartAuthor.getLow());
                }
            }
        }

        if (totalAuthor != null)
        {
            totalAuthor.setAuthorName(ComConstants.TOTAL_CHART_NODE);
            if (totalAuthor instanceof CommonChartAuthorVO)
            {
                CommonChartAuthorVO commonTotalAuthor = (CommonChartAuthorVO)totalAuthor;
                commonTotalAuthor.setSerious(totalSerious.get());
                commonTotalAuthor.setNormal(totalNormal.get());
                commonTotalAuthor.setPrompt(totalPrompt.get());
                commonTotalAuthor.setTotal(commonTotalAuthor.getSerious() + commonTotalAuthor.getNormal() + commonTotalAuthor.getPrompt());
            }
            else if (totalAuthor instanceof CCNChartAuthorVO)
            {
                CCNChartAuthorVO ccnTotalAuthor = (CCNChartAuthorVO)totalAuthor;
                ccnTotalAuthor.setSuperHigh(totalSuperHigh.get());
                ccnTotalAuthor.setHigh(totalHigh.get());
                ccnTotalAuthor.setMedium(totalMedium.get());
                ccnTotalAuthor.setLow(totalLow.get());
                ccnTotalAuthor.setTotal(ccnTotalAuthor.getSuperHigh() + ccnTotalAuthor.getHigh() + ccnTotalAuthor.getMedium() + ccnTotalAuthor.getLow());
            }
            // 总计
            result.setTotalAuthor(totalAuthor);
        }
        else
        {
            result.setTotalAuthor(new ChartAuthorBaseVO());
        }

        result.setMaxMinHeight();

    }


    /**
     * LocalDate转时间戳
     *
     * @param localDate
     * @return
     */
    protected Long localDate2Millis(LocalDate localDate)
    {
        return LocalDateTime.of(localDate, LocalTime.MIN).toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
    }


    /**
     * 时间戳转LocalDate
     *
     * @param timestamp
     * @return
     */
    protected LocalDate long2LocalDate(long timestamp)
    {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
    }


    /**
     * 获取显示的日期
     *
     * @param date
     * @param todayDate
     * @param lastDate
     * @return
     */
    protected String setTips(LocalDate date, LocalDate todayDate, LocalDate lastDate,
            Map<String, GlobalMessage> globalMessageMap, boolean dateRangeFlag)
    {
        // 获取这周的周一
        LocalDate mondayOfWeek = todayDate.with(DayOfWeek.MONDAY);

        String tips;
        if (dateRangeFlag)
        {
            String dateStr = date.toString();
            tips = dateStr.substring(dateStr.indexOf("-") + 1);
        }
         else if (date.equals(todayDate))
        {
            tips = String.format("%s(%s)", date.format(DateTimeFormatter.ofPattern("MM-dd")), globalMessageUtil.getMessageByLocale(globalMessageMap.get(ComConstants.DATE_TODAY)));
        }
        else if (date.equals(lastDate))
        {
            if (date.equals(mondayOfWeek))
            {
                tips = String.format("%s(%s)", date.format(DateTimeFormatter.ofPattern("MM-dd")), globalMessageUtil.getMessageByLocale(globalMessageMap.get(ComConstants.DATE_MONDAY)));
            }
            else
            {
                tips = String.format("%s(%s)", date.format(DateTimeFormatter.ofPattern("MM-dd")), globalMessageUtil.getMessageByLocale(globalMessageMap.get(ComConstants.DATE_LAST_MONDAY)));
            }
        }
        else
        {
            String dateStr = date.toString();
            tips = dateStr.substring(dateStr.indexOf("-") + 1);
        }

        String globalTips = globalMessageUtil.getMessageByLocale(globalMessageMap.get(tips));
        return ComConstants.STRING_TIPS.equals(globalTips) ? tips : globalTips;
    }


    private String getDatebyDiff(int diff){
        long l = System.currentTimeMillis() / 1000 + (long)(diff * 24) * 3600;
        Date date = new Date(l*1000);
        SimpleDateFormat ft =
                new SimpleDateFormat("yyyy-MM-dd");
        ft.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return ft.format(date);
    }

    /**
     * 从时间获得一个日期距离今天的天数
     * 输入比如2016-02-02 11:12:00
     * 返回值比如昨天返回-1，后天返回2
     * @param moment
     * @return
     */
    protected int moment2DateDiff(long moment){
        int result;
        long temp = moment - getTina();
        result = (int)(temp/(24*3600));
        if(temp<0) result = result-1;
        return result;
    }


    /**
     * 获得今天0点的秒数
     * @return
     */
    private long getTina(){
        String today = getDatebyDiff(0);
        String todayZero = today + " 00:00:00";
        long tina = getTimeStamp(todayZero);

        return tina;
    }


    /**
     * 从一个具体时间，比如2016-12-12 23:23:15，获得秒数
     * @param time
     * @return
     */
    private int getTimeStamp(String time)
    {
        if(time==null || time.isEmpty()) return 0;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        Date date;
        try
        {
            date = simpleDateFormat.parse(time);
        } catch (ParseException e)
        {
            return 0;
        }
        long timeStamp = date.getTime() / 1000;
        return (int) timeStamp;
    }


    /**
     * 按时间区间获取图表时间节点(CCN/DUPC)
     *
     * @param startTime 开始日期
     * @param endTime   截止日期
     */
    protected void getWeekDateListByDate(String startTime, String endTime, List<LocalDate> dateList)
    {
        LocalDate startDate = getStartLocalDate(startTime);
        LocalDate endDate = getEndLocalDate(endTime);

        // 先把结束时间放在第一位
        dateList.add(endDate);

        int weekNum = 0;
        LocalDate mondayOfWeek = endDate.minusWeeks(weekNum).with(DayOfWeek.MONDAY);
        weekNum++;
        if (!endDate.equals(mondayOfWeek) && !startDate.isAfter(mondayOfWeek))
        {
            dateList.add(mondayOfWeek);
        }

        while (!endDate.equals(startDate) && startDate.isBefore(mondayOfWeek))
        {
            mondayOfWeek = endDate.minusWeeks(weekNum).with(DayOfWeek.MONDAY);
            if (!endDate.equals(mondayOfWeek) && startDate.isBefore(mondayOfWeek))
            {
                dateList.add(mondayOfWeek);
            }

            // 最后都是开始时间
            if (startDate.isAfter(mondayOfWeek))
            {
                if (startDate.isBefore(endDate))
                {
                    dateList.add(startDate);
                }
                break;
            }
            weekNum++;
        }
    }


    /**
     * 解析数据报表截止日期
     * 截止日期是未来日期,或者为空时,设置为当天日期
     *
     * @param endTime 截止日期
     * @return localDate
     */
    private LocalDate getEndLocalDate(String endTime)
    {
        LocalDate endDate;
        LocalDate nowDate = LocalDate.now();
        if (StringUtils.isEmpty(endTime))
        {
            endDate = nowDate;
        }
        else
        {
            LocalDate endDateParse = LocalDate.parse(endTime);
            if (endDateParse.isAfter(nowDate))
            {
                endDate = nowDate;
            }
            else
            {
                endDate = endDateParse;
            }
        }
        return endDate;
    }


    private LocalDate getStartLocalDate(String startTime)
    {
        LocalDate startDate;
        if (StringUtils.isEmpty(startTime))
        {
            // CodeCC上线日期
            startDate = LocalDate.parse("2016-10-24");
        }
        else
        {
            startDate = LocalDate.parse(startTime);
        }
        return startDate;
    }


    /**
     * 按开始结束日期生成DateList(common/lint)
     *
     * @param startTime 开始日期 yyyy-MM-dd
     * @param endTime   截止日期
     * @param dateList  date list
     */
    protected void generateDateList(String startTime, String endTime, List<LocalDate> dateList)
    {
        LocalDate startDate = getStartLocalDate(startTime);
        LocalDate endDate = getEndLocalDate(endTime);

        // 如果开始和截止日期都在未来,则仅保留截止日期(今天)
        if (endDate.isBefore(startDate))
        {
            dateList.add(endDate);
        }

        while (startDate.isBefore(endDate) || startDate.equals(endDate))
        {
            dateList.add(endDate);
            endDate = endDate.minusDays(1);
        }
    }

}
