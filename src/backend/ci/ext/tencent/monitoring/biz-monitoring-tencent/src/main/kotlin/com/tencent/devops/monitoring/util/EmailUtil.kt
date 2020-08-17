package com.tencent.devops.monitoring.util

import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import org.apache.commons.lang3.time.FastDateFormat

object EmailUtil {

    private val DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd")

    fun getMessage(
        startTime: Long,
        endTime: Long,
        rowList: List<Triple<String, String, String>>,
        title: String,
        module: String,
        receivers: String?
    ): EmailNotifyMessage? {
        if (null == receivers || receivers.isNullOrBlank()) {
            return null
        }
        val message = EmailNotifyMessage()
        message.addAllReceivers(receivers.split(",").asSequence().toHashSet())
        message.title = title
        message.body = getEmailBody(startTime, endTime, module, rowList)
        message.format = EnumEmailFormat.HTML
        return message
    }

    fun getEmailBody(
        startTime: Long,
        endTime: Long,
        module: String,
        rowList: List<Triple<String, String, String>>
    ): String {
        val stringBuffer = StringBuilder()
        stringBuffer.append(SHARE_EMAIL_HTML_PREFIX)
        rowList.forEach {
            stringBuffer.append(getShareEmailBodyRow(it.first, it.second, it.third))
        }
        stringBuffer.append(SHARE_EMAIL_HTML_SUFFIX)
        val template = stringBuffer.toString()

        return template.replace(
            BODY_TITLE_TEMPLATE,
            "${DATE_FORMAT.format(startTime)} - ${DATE_FORMAT.format(endTime)} 的 【$module】 统计"
        )
            .replace(TABLE_COLUMN1_TITLE, "名称")
            .replace(TABLE_COLUMN2_TITLE, "成功率")
            .replace(TABLE_COLUMN3_TITLE, "详情")
    }

    fun getShareEmailBodyRow(name: String, projectName: String, url: String): String {
        return """
                                                                            <tr>
                                                                               <td>$name</td>
                                                                               <td>$projectName</td>
                                                                               <td align="center">
                                                                                   <a href="$url">查看</a>
                                                                               </td>
                                                                           </tr> 
        """
    }

    private val BODY_TITLE_TEMPLATE = "#{bodyTitle}"
    private val TABLE_COLUMN1_TITLE = "#{column1Title}"
    private val TABLE_COLUMN2_TITLE = "#{column2Title}"
    private val TABLE_COLUMN3_TITLE = "#{column3Title}"

    private val SHARE_EMAIL_HTML_PREFIX =
        """
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<!-- saved from url=(0064)http://open.oa.com/static_api/v3/templates/template10/index.html -->
<html xmlns="http://www.w3.org/1999/xhtml"><head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    
    <title></title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
<style type="text/css">
    * {
        font-family: '微软雅黑';
        font-size: 14px;
    }

    img {
        outline: none;
        border: none;
        text-decoration: none;
    }

    a {
        border: none;
        text-decoration: none;
        color: #5c90d2;
    }

    a:hover {
        color: #3169b1;
        text-decoration: none;
    }

    .table-email thead tr th,
    .table-email tbody tr td {
        padding: 10px;
        border: 1px solid #ddd;
        border-right: none;
        border-bottom: none;
    }

    .table-email thead tr th:last-child,
    .table-email tbody tr td:last-child {
        border: 1px solid #ddd;
        border-bottom: none;
        text-align: center;
    }

    .table-email tbody tr:last-child td {
        border: 1px solid #ddd;
        border-right: none;
    }

    .table-email tbody tr:last-child td:last-child {
        border: 1px solid #ddd;
    }

    .email-info {
        display: inline-block;
        width: 80px;
        height: 28px;
        line-height: 28px;
        background-color: #4A9BFF;
        border: #2180F5 1px solid;
    }

    .email-info>a {
        font-size: 12px;
        color: #fff;
    }

    .email-info>a:hover {
        color: #fff;
    }

    .course-titles {
        font-size: 18px;
    }

    .table-lr-blue {
        width: 45px;
        background: #0b1731;
    }
</style></head>


<body>
    <table border="0" cellpadding="0" cellspacing="0" width="100%">
        <tbody>
            <tr>
                <td>
                    <table align="center" border="0" cellpadding="0" cellspacing="0" width="963">
                        <tbody>
                            <tr>
                                <td colspan="3">
                                    <img src="http://devops.oa.com/console/devops_bg.png" alt="img" style="width:963px;height:104px;">
                                </td>
                            </tr>
                            <tr>
                                <td class="table-lr-blue"></td>
                                <td>
                                    <table align="center" border="0" cellpadding="0" cellspacing="0" width="873" style="background:#fff;">
                                        <tbody>
                                            <tr>
                                                <td style="width:20px;"></td>
                                                <td>
                                                    <table align="center" border="0" cellpadding="0" cellspacing="0" width="833">
                                                        <tbody>
                                                            <tr>
                                                                <td>
                                                                    <div style="margin-top:10px;">#{bodyTitle}</div>
                                                                </td>
                                                            </tr>
                                                            <tr>
                                                                <td>
                                                                    <div style="margin-top:10px;"></div>
                                                                </td>
                                                            </tr>
                                                            <tr>
                                                                <td>
                                                                    <table align="center" border="0" cellpadding="0" cellspacing="0" width="833" class="table-email">
                                                                        <thead>
                                                                            <tr>
                                                                                <th align="left" style="width:40%;background-color:#F6F8F8;">#{column1Title}</th>
                                                                                <th align="left" style="width:40%;background-color:#F6F8F8;">#{column2Title}</th>
                                                                                <th style="width:20%;background-color:#F6F8F8;">#{column3Title}</th>
                                                                            </tr>
                                                                        </thead>
                                                                        <tbody>
        """

    private val SHARE_EMAIL_HTML_SUFFIX =
        """
                                                                                   </tbody>
                                                                    </table>
                                                                </td>
                                                            </tr>
                                                            <tr>
                                                                <td>
                                                                    <div style="margin-bottom:10px;"></div>
                                                                </td>
                                                            </tr>
                                                        </tbody>
                                                    </table>
                                                </td>
                                                <td style="width:20px;"></td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </td>
                                <td class="table-lr-blue"></td>
                            </tr>
                            <tr>
                                <td colspan="3" style="height:40px;background:#0b1731;"></td>
                            </tr>
                        </tbody>
                    </table>
                </td>
            </tr>
        </tbody>
    </table>
</body>
</html> 
        """
}

fun main(args: Array<String>) {
    println(EmailUtil.getShareEmailBodyRow("\$name", "\$projectName", "\$url"))
}