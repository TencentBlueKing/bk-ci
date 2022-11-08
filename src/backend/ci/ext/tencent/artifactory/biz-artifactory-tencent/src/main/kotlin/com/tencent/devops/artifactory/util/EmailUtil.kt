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

package com.tencent.devops.artifactory.util

import com.tencent.devops.artifactory.service.pojo.FileShareInfo
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import java.text.SimpleDateFormat
import java.util.Date

object EmailUtil {
    fun getShareEmailTitle(userId: String, fileName: String, size: Int): String {
        return if (size == 1)
            "【蓝盾版本仓库通知】${userId}与你共享${fileName}文件"
        else
            "【蓝盾版本仓库通知】${userId}与你共享${fileName}等${size}个文件"
    }

    fun getShareEmailBody(projectName: String, title: String, userId: String, days: Int, FileShareInfoList: List<FileShareInfo>): String {
        val simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日")
        val date = simpleDateFormat.format(Date())
        val stringBuffer = StringBuilder()
        stringBuffer.append(SHARE_EMAIL_HTML_PREFIX)
        FileShareInfoList.forEach {
            stringBuffer.append(getShareEmailBodyRow(it.fileName, it.md5, it.projectName, it.downloadUrl))
        }
        stringBuffer.append(SHARE_EMAIL_HTML_SUFFIX)
        val template = stringBuffer.toString()
        return template
                .replace(HEADER_TITLE_TEMPLATE, title)
                .replace(BODY_TITLE_TEMPLATE, "${userId}与你共享以下文件，请在有效期（${days}天）内及时下载：")
                .replace(BODY_PROJECT_TEMPLATE, projectName)
                .replace(BODY_DATE_TEMPLATE, date)
                .replace(TABLE_COLUMN1_TITLE, "文件名")
                .replace(TABLE_COLUMN2_TITLE, "所属项目")
                .replace(TABLE_COLUMN3_TITLE, "操作")
    }

    fun makeEmailNotifyMessage(title: String, body: String, receivers: Set<String>): EmailNotifyMessage {
        val emailNotifyMessage = EmailNotifyMessage()
        emailNotifyMessage.addAllReceivers(receivers)
        emailNotifyMessage.title = title
        emailNotifyMessage.body = body
        emailNotifyMessage.format = EnumEmailFormat.HTML
        return emailNotifyMessage
    }

    private fun getShareEmailBodyRow(fileName: String, md5: String, projectName: String, downloadUrl: String): String {
        return "                                                                            <tr>\n" +
                "                                                                                <td style=\"padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">" +
                "                                                                                   <p style=\"margin: 0px\">$fileName</p>\n" +
                "                                                                                   <p style=\"margin: 0px;color: #c7c7c7\">MD5：$md5</p>\n" +
                "                                                                                </td>\n" +
                "                                                                                <td style=\"padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">$projectName</td>\n" +
                "                                                                                <td style=\"padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;\"><a href=\"$downloadUrl\" style=\"color: #3c96ff\">下载</a></td>\n" +
                "                                                                            </tr>\n"
    }

    private val HEADER_TITLE_TEMPLATE = "#{headerTitle}"
    private val BODY_PROJECT_TEMPLATE = "#{bodyProject}"
    private val BODY_TITLE_TEMPLATE = "#{bodyTitle}"
    private val BODY_DATE_TEMPLATE = "#{bodyDate}"
    private val TABLE_COLUMN1_TITLE = "#{column1Title}"
    private val TABLE_COLUMN2_TITLE = "#{column2Title}"
    private val TABLE_COLUMN3_TITLE = "#{column3Title}"

    private val SHARE_EMAIL_HTML_PREFIX = "<table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt; background-color: #fff; background: #fff;\">\n" +
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
            "                                    <tr class=\"email-footer\">\n" +
            "                                        <td style=\" padding: 20px 0 20px 36px; border-top: 1px solid #e6e6e6; background: #fff; color: #c7c7c7;\">你收到此邮件，是因为你关注了 $BODY_PROJECT_TEMPLATE 项目，或其它人@了你</td>\n" +
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
