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

import com.tencent.bk.codecc.defect.vo.ChartAuthorListVO;
import com.tencent.bk.codecc.defect.vo.ChartAuthorVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDataReportRspVO;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据报表抽象类
 *
 * @version V1.0
 * @date 2019/5/28
 */
public abstract class AbstractDataReportBizService implements IDataReportBizService
{


    @Override
    public CommonDataReportRspVO getDataReport(Long taskId, String toolName)
    {
        return getDataReport(taskId, toolName, 0);
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
        List<ChartAuthorVO> authorList = result.getAuthorList();
        if (CollectionUtils.isNotEmpty(authorList))
        {
            // 按作者排序
            authorList.sort(Comparator.comparing(ChartAuthorVO::getAuthorName));
            authorList.forEach(it ->
            {
                totalSerious.addAndGet(it.getSerious());
                totalNormal.addAndGet(it.getNormal());
                totalPrompt.addAndGet(it.getPrompt());
            });
        }

        ChartAuthorVO totalAuthor = new ChartAuthorVO();
        totalAuthor.setAuthorName(ComConstants.TOTAL_CHART_NODE);
        totalAuthor.setSerious(totalSerious.get());
        totalAuthor.setNormal(totalNormal.get());
        totalAuthor.setPrompt(totalPrompt.get());
        totalAuthor.setTotal(totalAuthor.getSerious() + totalAuthor.getNormal() + totalAuthor.getPrompt());

        result.setMaxMinHeight();
        // 总计
        result.setTotalAuthor(totalAuthor);
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
    protected String setTips(LocalDate date, LocalDate todayDate, LocalDate lastDate)
    {
        // 获取这周的周一
        LocalDate mondayOfWeek = todayDate.with(DayOfWeek.MONDAY);

        String tips;
        if (date.equals(todayDate))
        {
            tips = ComConstants.DATE_TODAY;
        }
        else if (date.equals(lastDate))
        {
            if (date.equals(mondayOfWeek))
            {
                tips = ComConstants.DATE_MONDAY;
            }
            else
            {
                tips = ComConstants.DATE_LAST_MONDAY;
            }
        }
        else
        {
            tips = date.toString();
        }

        return tips;
    }


    protected List<LocalDate> getShowDateList(int size)
    {
        // 当前日期
        if (size == 0)
        {
            size = 12;
        }
        LocalDate currentDate = LocalDate.now();
        List<LocalDate> list = new ArrayList<>(size);
        list.add(currentDate);
        for (int i = 0; i < size - 1; i++)
        {
            LocalDate mondayOfWeek = currentDate.minusWeeks(i).with(DayOfWeek.MONDAY);
            if (!currentDate.equals(mondayOfWeek))
            {
                list.add(mondayOfWeek);
            }
        }
        return list;
    }
}
