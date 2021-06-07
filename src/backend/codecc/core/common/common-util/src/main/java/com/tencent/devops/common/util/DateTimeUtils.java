package com.tencent.devops.common.util;

import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.CommonMessageCode;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

/**
 * 日期工具类
 *
 * @version V1.0
 * @date 2019/10/28
 */
public class DateTimeUtils
{
    private static Logger logger = LoggerFactory.getLogger(DateTimeUtils.class);

    public static final String hhmmFormat = "HH:mm";
    public static final String hhmmssFormat = "HH:mm:ss";
    public static final String MMddFormat = "MM-dd";
    public static final String yyyyFormat = "yyyy";
    public static final String yyyyMMddFormat = "yyyy-MM-dd";
    public static final String fullFormat = "yyyy-MM-dd HH:mm:ss";
    public static final String MMddChineseFormat = "MM月dd日";
    public static final String yyyyMMddChineseFormat = "yyyy年MM月dd日";
    public static final String fullChineseFormat = "yyyy年MM月dd日 HH时mm分ss秒";
    public static final String yyyyMMddHHmmss = "yyyyMMddHHmmss";
    public static final String fullFormatWithT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final int DAY_TIMESTAMP = 86400000;
    public static final String UTC_FULL_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private static final int TIMESTAMP_SHIFT_SPACE_NUM = 12;

    /**
     * 从秒数获得一个日期距离今天的天数
     * 比如昨天返回-1，后天返回2
     *
     * @param second
     * @return
     */
    public static int second2DateDiff(long second)
    {
        int result;
        long temp = second - getTodayZeroMillis() / 1000L;
        result = (int) (temp / (24 * 3600));
        if (temp < 0)
        {
            result = result - 1;
        }
        return result;
    }

    /**
     * 从时间获得一个日期距离今天的天数
     * 输入比如2016-02-02 11:12:00
     * 返回值比如昨天返回-1，后天返回2
     *
     * @param moment
     * @return
     */
    public static int moment2DateDiff(String moment)
    {
        int result;
        long temp = getTimeStamp(moment) - getTodayZeroMillis();
        result = (int) (temp / (24 * 3600 * 1000L));
        if (temp < 0)
        {
            result = result - 1;
        }
        return result;
    }

    /**
     * 从时间获得一个日期距离今天的天数
     * 输入比如2016-02-02 11:12:00
     * 返回值比如昨天返回-1，后天返回2
     *
     * @param moment
     * @return
     */
    public static int moment2DateDiff(long moment)
    {
        int result;
        long temp = moment - getTodayZeroMillis();
        result = (int) (temp / (24 * 3600 * 1000L));
        if (temp < 0)
        {
            result = result - 1;
        }
        return result;
    }

    // 根据年 月获得对应的月份天数
    public static int getDaysByYearMonth(int year, int month)
    {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DATE, 1);
        return cal.getActualMaximum(Calendar.DATE);
    }

    // 根据年份和周数获取周的开始和结束日期, 返回日期:月-日/月-日
    public static String getWeekDate(int year, int weekNum)
    {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(yyyyMMddFormat);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.WEEK_OF_YEAR, weekNum);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String weekDate = sdf.format(cal.getTime()).substring(5) + "/";
        cal.add(Calendar.DAY_OF_WEEK, 6);
        weekDate += sdf.format(cal.getTime()).substring(5);
        return weekDate;
    }

    /**
     * 从秒数获得一个日期的字符串值，比如2016-12-07
     *
     * @param second
     * @return
     */
    public static String second2DateString(long second)
    {
        Date date = new Date(second);
        SimpleDateFormat ft = new SimpleDateFormat(yyyyMMddFormat);
        ft.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return ft.format(date);
    }

    public static String second2TimeString(long second)
    {
        Date date = new Date(second * 1000);
        SimpleDateFormat ft =
                new SimpleDateFormat("hh:mm");
        ft.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return ft.format(date);
    }

    /**
     * 从秒数获得一个具体时间，24小时制，比如2016-12-12 23:23:15
     *
     * @param second
     * @return
     */
    public static String second2Moment(long second)
    {
        Date date = new Date(second * 1000);
        SimpleDateFormat ft =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ft.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return ft.format(date);
    }

    /**
     * 从一个具体时间，比如2016-12-12 23:23:15，获得秒数
     *
     * @param time
     * @return
     */
    public static long getTimeStamp(String time)
    {
        if (time == null || time.isEmpty())
        {
            return 0;
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        Date date;
        try
        {
            date = simpleDateFormat.parse(time);
        }
        catch (ParseException e)
        {
            logger.error("parse string time[{}] to timestamp failed", time);
            return 0;
        }
        return date.getTime();
    }

    /**
     * 获取一个具体日期的开始时间
     *
     * @param date 日期
     * @return long
     */
    public static long getTimeStampStart(String date)
    {
        if (StringUtils.isEmpty(date))
        {
            return 0;
        }

        return getTimeStamp(date + " 00:00:00");
    }


    /**
     * 获取一个具体日期的结束时间
     *
     * @param date 日期
     * @return long
     */
    public static long getTimeStampEnd(String date)
    {
        if (StringUtils.isEmpty(date))
        {
            return 0;
        }

        return getTimeStamp(date + " 23:59:59");
    }

    public static long getTimeStampHans(String dateTime)
    {
        if (dateTime == null || dateTime.isEmpty())
        {
            return 0;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH:mm:ss");
        dateTime = dateTime.replace(" ", "");
        Date date = null;
        try
        {
            date = sdf.parse(dateTime);
        }
        catch (ParseException e)
        {
            logger.error("parse string time[{}] to timestamp failed" + e.toString(), dateTime);
            return 0;
        }
        return date.getTime();
    }

    /**
     * 获得今天0点的毫秒数
     *
     * @return
     */
    public static long getTodayZeroMillis()
    {
        String today = getDateByDiff(0);
        String todayZero = today + " 00:00:00";
        long tina = getTimeStamp(todayZero);
        return tina;
    }

    public static String getDateByDiff(int diff) {
        long l = System.currentTimeMillis() + (long) diff * DAY_TIMESTAMP;
        Date date = new Date(l);
        SimpleDateFormat ft = new SimpleDateFormat(yyyyMMddFormat);
        ft.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return ft.format(date);
    }

    /**
     * 从工具侧上报过来的文件信息（GIT和SVN工具命令获取到的时间格式不同，这里做格式化适配处理）
     *
     * @param tzTime
     * @return
     */
    public static Long getTimeFromTZFormat(String tzTime)
    {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        sdf1.setTimeZone(tz);
        sdf2.setTimeZone(tz);

        Date date = null;
        try
        {
            date = sdf1.parse(tzTime);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (date == null)
        {
            try
            {
                date = sdf2.parse(tzTime);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        if (date == null)
        {
            try
            {
                date = sdf3.parse(tzTime);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        if (date != null)
        {
            return date.getTime();
        }
        else
        {//如果都转换异常，则置为0
            return new Date(0).getTime();
        }
    }

    /**
     * 得到指定时间的时间日期格式
     *
     * @param date   指定的时间
     * @param format 时间日期格式
     * @return
     */
    public static String convertDateToString(Date date, String format)
    {
        DateFormat df = new SimpleDateFormat(format);
        return df.format(date);
    }

    /**
     * 得到指定时间的时间日期格式
     *
     * @param timeInMillis 指定的时间毫秒数
     * @param format       时间日期格式
     * @return
     */
    public static String convertLongTimeToString(Long timeInMillis, String format)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeInMillis);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        String mdate = simpleDateFormat.format(cal.getTime());
        return mdate;
    }

    /**
     * 得到指定时间的时间日期格式
     *
     * @param dateStr
     * @param format
     * @return
     */
    public static Date convertStringToDate(String dateStr, String format)
    {

        DateFormat df = new SimpleDateFormat(format);
        try
        {
            return df.parse(dateStr);
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException("解析日期字符串发生异常，参数数据不合法", e);
        }
    }

    /**
     * 得到指定时间的时间日期格式
     *
     * @param dateStr
     * @param format
     * @return
     */
    public static Long convertStringDateToLongTime(String dateStr, String format)
    {

        DateFormat df = new SimpleDateFormat(format);
        try
        {
            return df.parse(dateStr).getTime();
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException("解析日期字符串发生异常，参数数据不合法", e);
        }
    }

    /**
     * 得到周一日期
     *
     * @param interval 0表示本周一，1表示上周一，以此类推
     * @return yyyy-MM-dd
     */
    public static String getMonday(int interval)
    {
        Calendar c = Calendar.getInstance();
        int day_of_week = c.get(Calendar.DAY_OF_WEEK) - 1;
        if (day_of_week == 0)
        {
            day_of_week = 7;
        }
        c.add(Calendar.DATE, -day_of_week + 1 - interval * Calendar.DAY_OF_WEEK);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return simpleDateFormat.format(c.getTime());
    }

    /**
     * 根据时间过滤
     *
     * @param startDate
     * @param endDate
     * @param time
     * @return
     */
    public static boolean filterDate(String startDate, String endDate, long time)
    {
        if (StringUtils.isNotEmpty(startDate))
        {
            long startTime = getTimeStamp(startDate + " 00:00:00");

            long endTime;
            if (StringUtils.isEmpty(endDate))
            {
                endTime = System.currentTimeMillis();
            }
            else
            {
                endTime = getTimeStamp(endDate + " 23:59:59");
            }

            if (time < startTime || time > endTime)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据日期字符串获取时间戳
     *
     * @param startCreateDate
     * @param endCreateDate
     * @return
     */
    public static long[] getStartTimeAndEndTime(String startCreateDate, String endCreateDate)
    {
        long startTime = 0;
        long endTime = 0;
        if (StringUtils.isNotEmpty(startCreateDate) && StringUtils.isNotEmpty(endCreateDate))
        {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try
            {
                startTime = df.parse(startCreateDate + " 00:00:00").getTime();
                endTime = df.parse(endCreateDate + " 23:59:59").getTime();
            }
            catch (ParseException e)
            {
                String errMsg = String.format("输入的开始时间或结束时间有误！ 开始时间：%s，结束时间：%s", startCreateDate, endCreateDate);
                logger.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
        }
        return new long[]{startTime, endTime};
    }

    /**
     * 将LocalDate转为时间戳
     */
    public static long localDateTransformTimestamp(LocalDate localDate) {
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = localDate.atStartOfDay().atZone(zone).toInstant();
        Date date = Date.from(instant);
        String dateStr = convertDateToString(date, "yyyy/MM/dd :hh:mm:ss");
        long timestamp = new Date(dateStr).getTime();
        return timestamp;
    }

    /**
     * 将时间戳转为LocalDate
     * @return
     */
    public static LocalDate timestampTransformLocalDate(Long timestamp) {
        Date date = new Date(timestamp);
        Instant instant = date.toInstant();
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, zone);
        LocalDate localDate = localDateTime.toLocalDate();
        return localDate;
    }


    /**
     * 时间戳转为13位
     *
     * @param time
     * @return
     */
    public static long getThirteenTimestamp(Long time) {
        if (time == null) {
            time = 0L;
        }

        String timeStr = String.valueOf(Math.abs(time));
        if (timeStr.length() > 12) {
            return time;
        } else {
            return time * 1000;
        }
    }


    /**
     * 检查两个日期有效性
     *
     * @param startTime 开始时间
     * @param endTime   截止时间
     */
    public static void checkDateValidity(String startTime, String endTime)
    {
        if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime))
        {
            // 指定日期格式为四位年/两位月份/两位日期，注意yyyy/MM/dd区分大小写；
            SimpleDateFormat format = new SimpleDateFormat(yyyyMMddFormat);
            try
            {
                // 设置lenient为false. 否则SimpleDateFormat会比较宽松地验证日期，比如2007/02/29会被接受，并转换成2007/03/01
                format.setLenient(false);
                Date startDate = format.parse(startTime);
                Date endDate = format.parse(endTime);
                if (startDate.after(endDate))
                {
                    String errMsg = String.format("Start time can not later than end time, startTime: %s, endTime: %s",
                            startTime, endTime);
                    logger.error(errMsg);
                    throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID);
                }
            }
            catch (ParseException e)
            {
                String errMsg = String.format("Time format error, startTime: %s, endTime: %s", startTime, endTime);
                logger.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID);
            }
        }
    }

    /**
     * 获取当前日期前X天的日期
     *
     * @return
     */
    @NotNull
    public static List<String> getBeforeDaily(Integer day) {
        Calendar calendar = Calendar.getInstance();
        //获得当前日期前 day 天的日期
        calendar.add(Calendar.DATE, -day);
        List<String> dates = new ArrayList<String>();
        for (int i = 0; i < day; i++) {
            calendar.add(Calendar.DATE, 1);
            dates.add(new SimpleDateFormat(yyyyMMddFormat).format(calendar.getTime()));
        }
        return dates;
    }

    /**
     * 获取开始日期和结束日期中间所有的时间集合
     * @param startTime
     * @param endTime
     * @return
     */
    public static List<String> getStartTimeBetweenEndTime(String startTime, String endTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(startTime));
            List<String> date = new ArrayList();
            for (long d = calendar.getTimeInMillis(); d <= sdf.parse(endTime).getTime();
                 d = getPlusDayMillis(calendar)) {
                date.add(sdf.format(d));
            }
            return date;
        } catch (ParseException e) {
            String errMsg = String.format("Time format error, startTime: %s, endTime: %s", startTime, endTime);
            logger.error(errMsg);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID);
        }
    }

    public static long getPlusDayMillis(Calendar c) {
        c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH) + 1);
        return c.getTimeInMillis();
    }


    /**
     * 获取开始时间和结束时间
     *
     * @return
     */
    public static HashMap<String, String> getStartDateAndEndDate() {
        HashMap<String, String> dateMap = new HashMap<>();

        SimpleDateFormat sp = new SimpleDateFormat(yyyyMMddFormat);
        //1.获取当前时间
        Date time = new Date();
        String endTime = sp.format(time);
        endTime = endTime + " 23:59:59";
        dateMap.put("endTime", endTime);

        //2.获取昨天的时间为开始时间
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -6);
        Date yesterday = cal.getTime();
        String startTime = sp.format(yesterday);
        startTime = startTime + " 00:00:00";
        //开始时间
        dateMap.put("startTime", startTime);
        return dateMap;
    }


    /**
     * 字符串日期时间格式转LocalDate
     *
     * @param fullFormatStr str
     * @return LocalDate
     */
    public static LocalDate convertString2LocalDate(String fullFormatStr) {
        return LocalDate.parse(fullFormatStr, DateTimeFormatter.ofPattern(fullFormat));
    }

    /**
     * 获取时间段每一天的时间
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param day       指定获得多少天的日期
     * @return
     */
    public static List<String> getDatesByStartTimeAndEndTime(String startTime, String endTime, int day) {
        List<String> dates;
        if (StringUtils.isEmpty(startTime) || StringUtils.isEmpty(endTime)) {
            dates = DateTimeUtils.getBeforeDaily(day);
        } else {
            dates = DateTimeUtils.getStartTimeBetweenEndTime(startTime, endTime);
        }
        return dates;
    }
    /**
     * 获取2020年开始每一周时间段
     *
     * @return list
     */

    public static List<String> getWeekTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = sdf.parse("2019/12/30");
            endDate = sdf.parse(sdf.format(new Date()));
        } catch (ParseException e) {
            String errMsg = String.format("Time format error, startTime: %s, endTime: %s", startDate, endDate);
            logger.error(errMsg);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID);
        }
        ArrayList<String> weekList = new ArrayList<>();
        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        startCal.setTime(startDate);
        endCal.setTime(endDate);

        Calendar fistMonday = Calendar.getInstance();
        // 设置该日期为开始日期的年份1月1号，方便计算开始日期是第几周(week of year)
        fistMonday.set(startCal.get(Calendar.YEAR), Calendar.JANUARY, 1, 0, 0, 0);

        // 如果开始日期不是周一,则向以前偏移到最近的周一日期
        while (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            startCal.add(Calendar.DAY_OF_YEAR, 1);
        }
        // 如果当前日期不是周日,则向以前偏移到上一个周日(tmp: 计算本周偏移量日期)
        int tmp = 0;
        while (endCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            tmp++;
            endCal.add(Calendar.DAY_OF_YEAR, -1);
        }
        int startYear = startCal.get(Calendar.YEAR);
        while (startCal.compareTo(endCal) < 0) {
            int endYear = startCal.get(Calendar.YEAR);
            // 跨年处理
            if (startYear < endYear) {
                // 设置日期回到跨年后的1月1号0点,重新偏移到第一个周一
                fistMonday.set(endYear, Calendar.JANUARY, 1, 0, 0, 0);
                while (fistMonday.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    fistMonday.add(Calendar.DAY_OF_YEAR, 1);
                }
                startYear = endYear;
            }
            int weekNum = startCal.get(Calendar.WEEK_OF_YEAR);
            String weekNumStr = String.valueOf(weekNum);
            if (weekNum < 10) {
                weekNumStr = "0" + weekNum;
            }

            String monday = sdf.format(startCal.getTime());
            startCal.add(Calendar.DATE, 6);
            String sunday = sdf.format(startCal.getTime());
            startCal.add(Calendar.DATE, 1);
            String week = startCal.get(Calendar.YEAR) + "年第" + weekNumStr + "周" + monday
                    + "-" + sunday;
            weekList.add(week);
        }
        return weekList;
    }

    /**
     * 从秒数获得一个具体时间，24小时制，比如2016-12-12 23:23:15
     * 自动判断是否为13位的时间戳
     *
     * @param second
     * @return
     */
    public static String timestamp2StringDate(long second) {
        Date date = new Date(getThirteenTimestamp(second));
        SimpleDateFormat ft = new SimpleDateFormat(fullFormat);
        ft.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return ft.format(date);
    }

}