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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.monitoring.util

import org.apache.commons.lang3.time.FastDateFormat
import java.util.Locale

data class EmailModuleData(
    val module: String,
    val rowList: List<Triple<String/*名称*/, Double/*成功率*/, String/*详情链接*/>>,
    val observableUrl: String? = null,
    val amountKey: String = "成功率",
    val amountUnit: String = "%"
)

object EmailUtil {

    private val DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss")

    fun getEmailBody(
        startTime: Long,
        endTime: Long,
        emailModuleData: List<EmailModuleData>
    ): String {
        val stringBuffer = StringBuilder()
        stringBuffer.append(SHARE_EMAIL_HTML_PREFIX)

        emailModuleData.forEach {
            stringBuffer.append(
                SHARE_EMAIL_TABLE_PREFIX.replace(
                    BODY_TITLE_TEMPLATE,
                    "${DATE_FORMAT.format(startTime)} - ${DATE_FORMAT.format(endTime)} 的 【${it.module}】 统计" +
                            it.observableUrl?.run { " <a href=\"${it.observableUrl}\">【图例】</a>" }
                )
                    .replace(TABLE_COLUMN1_TITLE, "名称")
                    .replace(TABLE_COLUMN2_TITLE, it.amountKey)
                    .replace(TABLE_COLUMN3_TITLE, "详情")
            )
            it.rowList.forEach { rowList ->
                rowList.run {
                    stringBuffer.append(getTableRow(first, second, third, it.amountUnit))
                }
            }

            stringBuffer.append(SHARE_EMAIL_TABLE_SUFFIX)
        }
        stringBuffer.append(SHARE_EMAIL_HTML_SUFFIX)

        return stringBuffer.toString()
    }

    private fun getTableRow(name: String, percent: Double, url: String, unit: String): String {
        return """
        <tr>
           <td>$name</td>
           <td>${String.format(Locale.getDefault(), "%.5f", percent).toDouble()}$unit</td>
           <td align="center">
               <a href="$url">查看</a>
           </td>
        </tr> 
        """
    }

    private const val BODY_TITLE_TEMPLATE = "#{bodyTitle}"
    private const val TABLE_COLUMN1_TITLE = "#{column1Title}"
    private const val TABLE_COLUMN2_TITLE = "#{column2Title}"
    private const val TABLE_COLUMN3_TITLE = "#{column3Title}"

    private const val SHARE_EMAIL_TABLE_PREFIX = """
<table border="0" cellpadding="0" cellspacing="0" width="100%">
  <tbody>
    <tr>
      <td>
        <table align="center" border="0" cellpadding="0" cellspacing="0" width="963">
          <tbody>
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
                                <table align="center" border="0" cellpadding="0" cellspacing="0" width="833"
                                  class="table-email">
                                  <thead>
                                    <tr>
                                      <th align="left" style="width:40%;background-color:#F6F8F8;">
                                        #{column1Title}</th>
                                      <th align="left" style="width:40%;background-color:#F6F8F8;">
                                        #{column2Title}</th>
                                      <th style="width:20%;background-color:#F6F8F8;">
                                        #{column3Title}</th>
                                    </tr>
                                  </thead>
                                  <tbody>

    """

    private const val SHARE_EMAIL_TABLE_SUFFIX = """
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
    """

    private const val SHARE_EMAIL_HTML_PREFIX = """
<!DOCTYPE html
  PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

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
  </style>
</head>
<body>
        """

    private const val SHARE_EMAIL_HTML_SUFFIX =
        """
</body>
</html> 
        """
}
