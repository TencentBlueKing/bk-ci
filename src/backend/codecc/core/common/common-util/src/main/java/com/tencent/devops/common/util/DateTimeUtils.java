package com.tencent.devops.common.util;

import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.CommonMessageCode;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
        String today = getDatebyDiff(0);
        String todayZero = today + " 00:00:00";
        long tina = getTimeStamp(todayZero);
        return tina;
    }

    public static String getDatebyDiff(int diff)
    {
        long l = System.currentTimeMillis() / 1000 + (long) (diff * 24) * 3600;
        Date date = new Date(l * 1000);
        SimpleDateFormat ft =
                new SimpleDateFormat("yyyy-MM-dd");
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
     * 时间戳转为13位
     *
     * @param time
     * @return
     */
    public static long getThirteenTimestamp(Long time)
    {
        if (time == null)
        {
            time = 0L;
        }

        if (time >> TIMESTAMP_SHIFT_SPACE_NUM > 0)
        {
            return time;
        }
        else
        {
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
}
