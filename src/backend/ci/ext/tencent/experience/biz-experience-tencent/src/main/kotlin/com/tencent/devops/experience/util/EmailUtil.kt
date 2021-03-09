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

package com.tencent.devops.experience.util

import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import java.text.SimpleDateFormat
import java.util.Date

object EmailUtil {

    fun makeMessage(
        userId: String,
        projectName: String,
        name: String,
        version: String,
        url: String,
        receivers: Set<String>
    ): EmailNotifyMessage {
        val message = EmailNotifyMessage()
        message.addAllReceivers(receivers)
        message.title = getShareEmailTitle(userId, name, version)
        message.body =
            getShareEmailBody(userId, name, version, listOf(Triple("$name-$version", projectName, url)), message.title)
        message.format = EnumEmailFormat.HTML
        return message
    }

    private fun getShareEmailTitle(userId: String, name: String, version: String): String {
        return "【蓝盾版本体验通知】${userId}邀您体验【$name-$version】"
    }

    private fun getShareEmailBody(
        userId: String,
        name: String,
        version: String,
        rowList: List<Triple<String, String, String>>,
        title: String
    ): String {
        val simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日")
        val date = simpleDateFormat.format(Date())

        val stringBuffer = StringBuilder()
        stringBuffer.append(SHARE_EMAIL_HTML_PREFIX)
        rowList.forEach {
            stringBuffer.append(getShareEmailBodyRow(it.first, it.second, it.third))
        }
        stringBuffer.append(SHARE_EMAIL_HTML_SUFFIX)
        val template = stringBuffer.toString()
        return template.replace(BODY_TITLE_TEMPLATE, "${userId}邀您体验【$name-$version】")
            .replace(TABLE_COLUMN1_TITLE, "名称")
            .replace(TABLE_COLUMN2_TITLE, "所属项目")
            .replace(TABLE_COLUMN3_TITLE, "操作")
            .replace(HEADER_TITLE_TEMPLATE, title)
            .replace(BODY_DATE_TEMPLATE, date)
    }

    private fun getShareEmailBodyRow(name: String, projectName: String, url: String): String {
        return "                                                                            <tr>\n" +
            "                                                                                <td style=\"padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">$name</td>\n" +
            "                                                                                <td style=\"padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">$projectName</td>\n" +
            "                                                                                <td style=\"padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;\"><a href=\"$url\" style=\"color: #3c96ff\">查看</a></td>\n" +
            "                                                                            </tr>\n"
    }

    private val HEADER_TITLE_TEMPLATE = "#{headerTitle}"
    private val BODY_TITLE_TEMPLATE = "#{bodyTitle}"
    private val TABLE_COLUMN1_TITLE = "#{column1Title}"
    private val TABLE_COLUMN2_TITLE = "#{column2Title}"
    private val TABLE_COLUMN3_TITLE = "#{column3Title}"
    private val BODY_DATE_TEMPLATE = "#{bodyDate}"

    private val SHARE_EMAIL_HTML_PREFIX =
        "<table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt; background-color: #fff; background: #fff;\">\n" +
            "    <tbody>\n" +
            "        <tr>\n" +
            "            <td align=\"center\" valign=\"top\" width=\"100%\" style=\"padding: 16px;\">\n" +
            "               <table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"956\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
            "                    <tbody>\n" +
            "                        <tr>\n" +
            "                            <td valign=\"top\" align=\"center\">\n" +
            "                                <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#f9f8f6\" class=\"layout layout-table root-table\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
            "                                    <tbody>\n" +
            "                                        <tr style=\"height: 64px; background: #555;\">\n" +
            "                                            <td style=\"padding-left: 24px;\" width=\"60\" align=\"center\">\n" +
            "                                                <img src=\"http://dev.gw.open.oa.com/email/logo.png\" width=\"52\" style=\"display: block\">\n" +
            "                                            </td>\n" +
            "                                            <td style=\"padding-left: 6px;\">\n" +
            "                                                <img src=\"http://dev.gw.open.oa.com/email/title.png\" width=\"176\" style=\"display: block\">\n" +
            "                                            </td>\n" +
            "                                        </tr>\n" +
            "                                    </tbody>\n" +
            "                                </table>\n" +
            "                            </td>\n" +
            "                        </tr>\n" +
            "                        <tr>\n" +
            "                            <td valign=\"top\" align=\"center\" style=\"padding: 24px;\" bgcolor=\"#f9f8f6\">\n" +
            "                                <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6;\">\n" +
            "                                    <tr>\n" +
            "                                        <td class=\"email-title\" style=\"padding: 20px 36px; line-height: 1.5; border-bottom: 1px solid #e6e6e6; background: #fff; font-size: 22px;\">$HEADER_TITLE_TEMPLATE</td>\n" +
            "                                    </tr>\n" +
            "                                    <tr>\n" +
            "                                        <td class=\"email-content\" style=\"padding: 0 36px; background: #fff;\">\n" +
            "                                            <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
            "                                                <tr>\n" +
            "                                                    <td class=\"email-source\" style=\"padding: 14px 0; color: #bebebe;\">来自蓝盾DevOps平台的推送</td>\n" +
            "                                                </tr>\n" +
            "                                                <!-- 表格内容 -->\n" +
            "                                                <tr class=\"email-information\">\n" +
            "                                                    <td class=\"table-info\">\n" +
            "                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
            "                                                            <tr class=\"table-title\">\n" +
            "                                                                <td style=\"padding-top: 36px; padding-bottom: 14px; color: #707070;\">$BODY_TITLE_TEMPLATE</td>\n" +
            "                                                            </tr>\n" +
            "                                                            <tr>\n" +
            "                                                                <td>\n" +
            "                                                                    <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6; border-collapse: collapse;\">\n" +
            "                                                                        <thead style=\"background: #f6f8f8;\">\n" +
            "                                                                            <tr style=\"color: #333C48;\">\n" +
            "                                                                                <th width=\"50%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">$TABLE_COLUMN1_TITLE</th>\n" +
            "                                                                                <th width=\"35%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">$TABLE_COLUMN2_TITLE</th>\n" +
            "                                                                                <th width=\"15%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;\">$TABLE_COLUMN3_TITLE</th>\n" +
            "                                                                            </tr>\n" +
            "                                                                        </thead>\n" +
            "                                                                        <tbody style=\"color: #707070;\">\n"

    private val SHARE_EMAIL_HTML_SUFFIX =
        "                                                                        </tbody>\n" +
            "                                                                    </table>\n" +
            "                                                                </td>\n" +
            "                                                            </tr>\n" +
            "                                                        </table>\n" +
            "                                                    </td>\n" +
            "                                                </tr>\n" +
            "\n" +
            "                                                <tr class=\"prompt-tips\">\n" +
            "                                                    <td style=\"padding-top: 32px; padding-bottom: 10px; color: #707070;\">如有任何问题，可随时联系蓝盾助手。</td>\n" +
            "                                                </tr>\n" +
            "                                                <tr class=\"info-remark\">\n" +
            "                                                    <td style=\"padding: 20px 0; text-align: right; line-height: 24px; color: #707070;\">\n" +
            "                                                        <div>$BODY_DATE_TEMPLATE</div>\n" +
            "                                                    </td>\n" +
            "                                                </tr>\n" +
            "                                            </table>\n" +
            "                                        </td>\n" +
            "                                    </tr>\n" +
            "                                </table>\n" +
            "                            </td>\n" +
            "                        </tr>\n" +
            "                    </tbody>\n" +
            "               </table>\n" +
            "            </td>\n" +
            "        </tr>\n" +
            "    </tbody>\n" +
            "</table>\n"
}
