package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.defect.service.AbstractDataReportBizService;
import com.tencent.bk.codecc.defect.vo.CovKlocChartDateVO;
import com.tencent.bk.codecc.defect.vo.CovKlocChartVO;
import com.tencent.bk.codecc.defect.vo.CovKlocDataReportRspVO;
import com.tencent.bk.codecc.defect.vo.DefectMostEarlyTime;
import com.tencent.bk.codecc.defect.vo.common.CommonDataReportRspVO;
import com.tencent.bk.codecc.defect.vo.report.ChartAuthorBaseVO;
import com.tencent.bk.codecc.defect.vo.report.ChartAuthorListVO;
import com.tencent.bk.codecc.defect.vo.report.CommonChartAuthorVO;
import com.tencent.devops.common.api.pojo.GlobalMessage;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.utils.GlobalMessageUtil;
import com.tencent.devops.common.util.DateTimeUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.DefectStatus.*;
import static com.tencent.devops.common.constant.RedisKeyConstants.GLOBAL_DATA_REPORT_DATE;

/**
 * Coverity、Klocwork等工具的数据报表实现
 *
 * @version V1.0
 * @date 2019/11/05
 */
@Service("CommonDataReportBizService")
public class CommonDataReportBizServiceImpl extends AbstractDataReportBizService {

    @Autowired
    private DefectRepository defectRepository;

    @Autowired
    private GlobalMessageUtil globalMessageUtil;

    @Override
    public CommonDataReportRspVO getDataReport(Long taskId, String toolName, int size, String startTime, String endTime)
    {
        // 检查日期有效性
        DateTimeUtils.checkDateValidity(startTime, endTime);

        List<DefectEntity> defectList = defectRepository.findByTaskIdAndToolName(taskId, toolName);
        CovKlocDataReportRspVO covKlocDataReportRspVO = new CovKlocDataReportRspVO();
        size = size == 0 ? 14 : size;

        covKlocDataReportRspVO.setTaskId(taskId);
        covKlocDataReportRspVO.setToolName(toolName);
        covKlocDataReportRspVO.setAuthorChart(getAuthorChart(defectList));
        covKlocDataReportRspVO.setNewCloseFixChart(getChart(size, defectList, startTime, endTime));

        return covKlocDataReportRspVO;
    }


    /**
     * 统计待修复告警、每日新增告警、每日关闭、每日修复的告警数
     *
     * @param days       展示天数
     * @param defectList 告警列表
     * @param startTime  开始日期
     * @param endTime    截止日期
     * @return chart
     */
    private CovKlocChartVO getChart(int days, List<DefectEntity> defectList, String startTime, String endTime)
    {
        List<CovKlocChartDateVO> covKlocChartDateVos;
        if (StringUtils.isNotEmpty(startTime) || StringUtils.isNotEmpty(endTime))
        {
            // 视图显示的时间列表
            List<LocalDate> dateTimeList = getShowDateList(days, startTime, endTime);
            covKlocChartDateVos = statisticsEveNewDefect(defectList, dateTimeList);
        }
        else
        {
            covKlocChartDateVos = statisticsEveNewDefect(days, defectList);
        }

        CovKlocChartVO covKlocChartVo = new CovKlocChartVO();
        covKlocChartVo.setElemList(covKlocChartDateVos);
        setChartHeight(covKlocChartVo, ComConstants.ChartType.REPAIR.name());
        setChartHeight(covKlocChartVo, ComConstants.ChartType.NEW.name());
        setChartHeight(covKlocChartVo, ComConstants.ChartType.CLOSE.name());
        return covKlocChartVo;
    }


    /**
     * 统计每天新增的告警数
     *
     * @param defectList    告警列表
     * @param dateTimeList  时间列表
     * @return chart list
     */
    private List<CovKlocChartDateVO> statisticsEveNewDefect(List<DefectEntity> defectList, List<LocalDate> dateTimeList)
    {
        int days = dateTimeList.size();
        List<CovKlocChartDateVO> listElem = new ArrayList<>(days);

        dateTimeList.forEach(date -> {
            CovKlocChartDateVO elem = new CovKlocChartDateVO();
            elem.setDate(date.toString());
            elem.setTips(date.format(DateTimeFormatter.ofPattern(DateTimeUtils.MMddFormat)));
            listElem.add(elem);
        });

        if (CollectionUtils.isEmpty(defectList)) {
            return listElem;
        }

        for (DefectEntity defectEntity : defectList) {
            // 1. 待修复的告警趋势
            statisticsUnFixDefect(days, listElem, defectEntity);
            // 2. 统计每天新增的告警数
            statisticsEveNewDefect(days, listElem, defectEntity);
            // 3. 统计每天关闭/修复的告警数
            statisticsClosedAndRepaired(days, listElem, defectEntity);
        }

        return listElem;
    }


    /**
     * 统计每天新增的告警数
     *
     * @param days
     * @param defectList
     */
    private List<CovKlocChartDateVO> statisticsEveNewDefect(int days, List<DefectEntity> defectList) {

        Map<String, GlobalMessage> globalMessageMap = globalMessageUtil.getGlobalMessageMap(GLOBAL_DATA_REPORT_DATE);
        List<CovKlocChartDateVO> listElem = new ArrayList<>(days);
        // 视图显示的时间列表
        List<LocalDate> dateTimeList = getShowDateList(days, null, null);
        LocalDate todayDate = dateTimeList.get(0);
        LocalDate yesterdayDate = dateTimeList.get(1);
        dateTimeList.forEach(date -> {
            CovKlocChartDateVO elem = new CovKlocChartDateVO();
            String tips = setDateTips(globalMessageMap, todayDate, yesterdayDate, date);
            elem.setDate(date.toString());
            elem.setTips(tips);
            listElem.add(elem);
        });

        if (CollectionUtils.isEmpty(defectList)) {
            return listElem;
        }

        for (DefectEntity defectEntity : defectList) {
            // 1. 待修复的告警趋势
            statisticsUnFixDefect(days, listElem, defectEntity);
            // 2. 统计每天新增的告警数
            statisticsEveNewDefect(days, listElem, defectEntity);
            // 3. 统计每天关闭/修复的告警数
            statisticsClosedAndRepaired(days, listElem, defectEntity);
        }

        return listElem;
    }


    /**
     * 统计待修复的告警趋势
     *
     * @param days
     * @param listElem
     * @param defectModel
     */
    private void statisticsUnFixDefect(int days, List<CovKlocChartDateVO> listElem, DefectEntity defectModel) {
        int createDiff = super.moment2DateDiff(defectModel.getCreateTime() / 1000);
        // 待修复
        if (NEW.value() == defectModel.getStatus()) {
            int boundary = Math.min(-createDiff, days - 1);
            if (createDiff <= 0) {
                // 影响: [ 创建-今天 ]
                for (int i = 0; i <= boundary; i++) {
                    listElem.get(i).increaseFixCount();
                }
            }
        }
        // 已关闭
        else{
            // 通过status获取到当前t_defect的修复时间、忽略时间、屏蔽时间中属于的一个
            DefectMostEarlyTime earlyTime = getDefectMostEarlyTime(defectModel);
            int offDiff = super.moment2DateDiff(earlyTime.getTime() / 1000);
            if (offDiff < createDiff || offDiff > 0)
            {
                return;
            }
            int boundary = Math.min(-createDiff, days - 1);
            for (int i = -offDiff + 1; i <= boundary; i++)
            {
                //影响到创建当天至关闭的前一天
                listElem.get(i).increaseFixCount();
            }
        }
    }


    /**
     * 统计每天新增的告警数
     *
     * @param days
     * @param listElem
     * @param defectModel
     * @return
     */
    private void statisticsEveNewDefect(int days, List<CovKlocChartDateVO> listElem, DefectEntity defectModel) {
        int createDiff = moment2DateDiff(defectModel.getCreateTime() / 1000);
        // 日期在请求天数的范围之内如果该告警未修复，范围内的每一天都属于"新增数"
        if (createDiff > (-days)) {
            listElem.get(-createDiff).increaseNewCount();

            // 如果status不等于"FIXED"则在当天添加 "待修复的个数"
            int status = defectModel.getStatus();
            if ((FIXED.value() & status) == 0 && (status & 31) == 1) {
                listElem.get(-createDiff).increaseExistCount();
            }
        }
    }


    /**
     * 统计每天关闭/修复的告警数
     *
     * @param days
     * @param listElem
     * @param defectModel
     */
    private void statisticsClosedAndRepaired(int days, List<CovKlocChartDateVO> listElem, DefectEntity defectModel) {
        int status = defectModel.getStatus();

        // 状态为待修复[ 新告警 ]的不考虑
        if (status <= 1) {
            return;
        }

        // 通过status获取到当前t_defect的修复时间、忽略时间、屏蔽时间中属于的一个
        DefectMostEarlyTime earlyTime = getDefectMostEarlyTime(defectModel);
        int closedDiff = super.moment2DateDiff(earlyTime.getTime() / 1000);

        // 直接查告警的时候指定时间就不要判断 "日期在请求天数的范围之内" ?
        if (closedDiff > (-days)) {
            //日期在请求天数的范围之内
            //判断是否为已修复告警的标准：修复状态为1，缺陷state字段为fixed，且修复时间最早
            Integer action = earlyTime.getAction();
            if ((status & FIXED.value()) > 0 && action == FIXED.value()) {
                listElem.get(-closedDiff).increaseRepairedCount();
                listElem.get(-closedDiff).increaseClosedCount();
            }
            //判断是否为已忽略告警的标准：忽略状态为1，且忽略时间最早
            else if ((IGNORE.value() & status) > 0 && action == IGNORE.value()) {
                listElem.get(-closedDiff).increaseIgnoreCount();
                listElem.get(-closedDiff).increaseClosedCount();
            }
            //判断是否为已屏蔽告警的标准，两个告警屏蔽状态位为1，并且屏蔽时间最早
            else if (((status & PATH_MASK.value()) > 0 || (status & CHECKER_MASK.value()) > 0) &&
                    action == (PATH_MASK.value() | CHECKER_MASK.value())) {
                listElem.get(-closedDiff).increaseExcludedCount();
                listElem.get(-closedDiff).increaseClosedCount();
            }
        }
    }


    /**
     * 查看一个告警的修复时间，忽略时间，屏蔽时间中最早的时间，以确定归属于哪个动作
     */
    private DefectMostEarlyTime getDefectMostEarlyTime(DefectEntity defectEntity) {
        int indexStatus = defectEntity.getStatus();

        // 一个告警可能有多种状态，所以用List封装动作以及时间，最后再取时间最早的一个
        List<Long> defectTimeList = new ArrayList<>();

        // 当前告警属于已修复
        if ((indexStatus & FIXED.value()) > 0) {
            defectTimeList.add(defectEntity.getFixedTime());
        }
        // 当前告警属于已忽略
        if ((indexStatus & IGNORE.value()) > 0) {
            defectTimeList.add(defectEntity.getIgnoreTime());
        }
        // 当前告警属于已屏蔽 [路径屏蔽、规则屏蔽]
        if (((indexStatus & PATH_MASK.value()) > 0) ||
                ((indexStatus & ComConstants.DefectStatus.CHECKER_MASK.value()) > 0)) {
            defectTimeList.add(defectEntity.getExcludeTime());
        }
        //取得不为0的最小值
        Long minTime = defectTimeList.stream()
                .filter(time -> time != 0L).reduce(Long::min).orElse(ComConstants.COMMON_NUM_0L);

        int action = 0;
        long time = ComConstants.COMMON_NUM_0L;
        if (minTime == defectEntity.getFixedTime()) {
            action = FIXED.value();
            time = defectEntity.getFixedTime();
        } else if (minTime == defectEntity.getIgnoreTime()) {
            action = IGNORE.value();
            time = defectEntity.getIgnoreTime();
        } else if (minTime == defectEntity.getExcludeTime()) {
            action = PATH_MASK.value() | ComConstants.DefectStatus.CHECKER_MASK.value();
            time = defectEntity.getExcludeTime();
        }

        DefectMostEarlyTime defectMostEarlyTime = new DefectMostEarlyTime();
        defectMostEarlyTime.setAction(action);
        defectMostEarlyTime.setTime(time);

        return defectMostEarlyTime;

    }


    /**
     * 获取告警作者分布列表
     *
     * @param defectList
     * @return
     */
    private ChartAuthorListVO getAuthorChart(List<DefectEntity> defectList) {
        ChartAuthorListVO authorList = new ChartAuthorListVO();
        if (CollectionUtils.isNotEmpty(defectList)) {
            defectList = defectList.stream()
                    .filter(defect -> ComConstants.DefectStatus.NEW.value() == defect.getStatus()).collect(Collectors.toList());

            // 按照告警作者分组
            Map<String, List<DefectEntity>> authorLintDefectMap = new HashMap<>();
            for (DefectEntity defectEntity : defectList) {
                Set<String> authorSet = defectEntity.getAuthorList();
                if (CollectionUtils.isEmpty(authorSet)) {
                    continue;
                }

                for (String author : authorSet) {
                    List<DefectEntity> authorDefect = authorLintDefectMap.get(author);
                    if (CollectionUtils.isNotEmpty(authorDefect)) {
                        authorDefect.add(defectEntity);
                    } else {
                        List<DefectEntity> list = new ArrayList<>();
                        list.add(defectEntity);
                        authorLintDefectMap.put(author, list);
                    }
                }
            }

            Map<String, CommonChartAuthorVO> chartAuthorMap = getChartAuthorSeverityMap(authorLintDefectMap);

            // 设置作者列表
            List<ChartAuthorBaseVO> list = MapUtils.isNotEmpty(chartAuthorMap) ? new ArrayList<>(chartAuthorMap.values()) : new ArrayList<>();
            authorList.setAuthorList(list);

        } else {
            // 设置默认值
            authorList.setAuthorList(new ArrayList<>());
        }

        // 统计总数
        super.setTotalChartAuthor(authorList);

        return authorList;
    }


    /**
     * 根据作者告警获取作者的严重等级信息
     *
     * @param authorDefectMap
     * @return
     */
    private Map<String, CommonChartAuthorVO> getChartAuthorSeverityMap(Map<String, List<DefectEntity>> authorDefectMap) {
        if (MapUtils.isEmpty(authorDefectMap)) {
            return new HashMap<>();
        }

        Map<String, CommonChartAuthorVO> chartAuthorMap = new HashMap<>(authorDefectMap.size());
        authorDefectMap.keySet().forEach(author ->
        {
            // 按照作者的严重等级分组
            Map<Integer, LongSummaryStatistics> severityMap = authorDefectMap.get(author)
                    .stream()
                    .collect(Collectors.groupingBy(DefectEntity::getSeverity,
                            Collectors.summarizingLong(DefectEntity::getSeverity)));
            severityMap.forEach((severityKey, value) ->
            {
                int count = (int) value.getCount();
                CommonChartAuthorVO authorVO = chartAuthorMap.get(author);
                if (Objects.isNull(authorVO)) {
                    authorVO = new CommonChartAuthorVO();
                    authorVO.setAuthorName(author);
                }

                // 设置作者告警的每个严重等级
                statisticsDefect(severityKey, count, authorVO);
                chartAuthorMap.put(author, authorVO);
            });
        });

        return chartAuthorMap;
    }


    private void statisticsDefect(Integer severityKey, Integer count, CommonChartAuthorVO authorVO) {
        switch (severityKey) {
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
    public List<LocalDate> getShowDateList(int size, String startTime, String endTime)
    {
        List<LocalDate> dateList = Lists.newArrayList();
        if (StringUtils.isEmpty(startTime) && StringUtils.isEmpty(endTime))
        {
            if (size == 0)
            {
                size = 14;
            }
            LocalDate currentDate = LocalDate.now();

            for (int i = 0; i < size; i++)
            {
                dateList.add(currentDate.minusDays(i));
            }
        }
        else
        {
            generateDateList(startTime, endTime, dateList);
        }
        return dateList;
    }


    private void setChartHeight(CovKlocChartVO covKlocChartVo, String chartType) {
        int max = 0;
        int min = 10000;
        List<CovKlocChartDateVO> defectList = covKlocChartVo.getElemList();
        if (CollectionUtils.isNotEmpty(defectList)) {
            if (ComConstants.ChartType.REPAIR.name().equals(chartType)) {
                for (CovKlocChartDateVO elem : defectList) {
                    int value = elem.getUnFixCount();
                    if (value > max) {
                        max = value;
                    }
                    if (value < min) {
                        min = value;
                    }
                }
                covKlocChartVo.setUnFixMaxHeight(max);
                covKlocChartVo.setUnFixMinHeight(min);
            } else if (ComConstants.ChartType.NEW.name().equals(chartType)) {
                for (CovKlocChartDateVO elem : defectList) {
                    int value = elem.getNewCount();
                    if (value > max) {
                        max = value;
                    }
                    if (value < min) {
                        min = value;
                    }
                }
                covKlocChartVo.setNewMaxHeight(max);
                covKlocChartVo.setNewMinHeight(min);
            } else if (ComConstants.ChartType.CLOSE.name().equals(chartType)) {
                for (CovKlocChartDateVO elem : defectList) {
                    int value = elem.getClosedCount();
                    if (value > max) {
                        max = value;
                    }
                    if (value < min) {
                        min = value;
                    }
                }
                covKlocChartVo.setCloseMaxHeight(max);
                covKlocChartVo.setCloseMinHeight(min);
            }
        }
    }


    /**
     * 设置前端显示时间
     * @param globalMessageMap
     * @param todayDate
     * @param yesterdayDate
     * @param date
     * @return
     */
    private String setDateTips(Map<String, GlobalMessage> globalMessageMap, LocalDate todayDate, LocalDate yesterdayDate, LocalDate date) {
        // 设置前端展示的tip
        return date.equals(todayDate) ? String.format("%s(%s)", date.format(DateTimeFormatter.ofPattern("MM-dd")), globalMessageUtil.getMessageByLocale(globalMessageMap.get(ComConstants.DATE_TODAY))):
                (date.equals(yesterdayDate) ? String.format("%s(%s)", date.format(DateTimeFormatter.ofPattern("MM-dd")), globalMessageUtil.getMessageByLocale(globalMessageMap.get(ComConstants.DATE_YESTERDAY))) :
                        date.format(DateTimeFormatter.ofPattern("MM-dd")));
    }


}





