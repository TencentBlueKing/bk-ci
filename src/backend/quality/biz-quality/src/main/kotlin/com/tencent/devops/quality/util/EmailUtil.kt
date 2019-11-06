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

package com.tencent.devops.quality.util

import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.quality.pojo.RuleCheckSingleResult
import java.text.SimpleDateFormat
import java.util.Date

object EmailUtil {
    fun makeEndMessage(projectName: String, pipelineName: String, buildNo: String, time: String, interceptList: List<String>, url: String, receivers: Set<String>): EmailNotifyMessage {
        val title = getEndEmailTitle()
        val body = getEndEmailBody(projectName, pipelineName, buildNo, interceptList, time, url)
        val message = EmailNotifyMessage()
        message.title = title
        message.body = body
        message.format = EnumEmailFormat.HTML
        message.sender = "DevOps"
        message.addAllReceivers(receivers)
        return message
    }

    private fun getEndEmailTitle(): String {
        return "【质量红线拦截通知】你有一个流水线被拦截"
    }

    private fun getEndEmailBody(projectName: String, pipelineName: String, buildNo: String, interceptList: List<String>, time: String, url: String): String {
        val simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日")
        val date = simpleDateFormat.format(Date())

        val title = getEndEmailTitle()
        val interceptSb = StringBuilder()
        interceptList.forEach {
            interceptSb.append(
                    "                                                            <tr class=\"table-title\">\n" +
                            "                                                                <td style=\"padding-top: 8px; padding-bottom: 0px; color: #707070;\">$it</td>\n" +
                            "                                                            </tr>\n"
            )
        }

        return HTML_END_BODY.replace(HEADER_TITLE_TEMPLATE, title)
                .replace(BODY_PROJECT_TEMPLATE, projectName)
                .replace(BODY_PIPELINE_TEMPLATE, "$pipelineName(#$buildNo)")
                .replace(BODY_TIME_TEMPLATE, time)
                .replace(BODY_INTERCEPT_TEMPLATE, interceptSb.toString())
                .replace(BODY_URL_TEMPLATE, url)
                .replace(BODY_DATE_TEMPLATE, date)
    }

    fun makeAuditMessage(projectName: String, pipelineName: String, buildNo: String, time: String, resultList: List<RuleCheckSingleResult>, url: String, receivers: Set<String>): EmailNotifyMessage {
        val title = getAuditEmailTitle()
        val body = getAuditEmailBody(projectName, pipelineName, buildNo, resultList, time, url)
        val message = EmailNotifyMessage()
        message.title = title
        message.body = body
        message.format = EnumEmailFormat.HTML
        message.sender = "DevOps"
        message.addAllReceivers(receivers)
        return message
    }

    private fun getAuditEmailTitle(): String {
        return "【质量红线审核通知】你有一个流水线需要审核"
    }

    private fun getAuditEmailBody(projectName: String, pipelineName: String, buildNo: String, resultList: List<RuleCheckSingleResult>, time: String, url: String): String {
        val simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日")
        val date = simpleDateFormat.format(Date())

        val title = getAuditEmailTitle()
        val interceptSb = StringBuilder()
        resultList.forEach { result ->
            if (result.messagePairs.isNotEmpty()) {
                interceptSb.append("                                                 <tr class=\"table-title\">\n" +
                        "                                                                <td style=\"padding-top: 18px; padding-bottom: 0px; color: #707070;\">拦截规则：${result.ruleName} </td>\n" +
                        "                                                            </tr>\n")
                interceptSb.append("                                                            <tr class=\"table-title\">\n" +
                        "                                                                <td style=\"padding-top: 18px; padding-bottom: 0px; color: #707070;\">拦截指标：</td>\n" +
                        "                                                            </tr>\n")
            }
            result.messagePairs.forEach {
                interceptSb.append(
                        "                                                            <tr class=\"table-title\">\n" +
                                "                                                                <td style=\"padding-top: 8px; padding-bottom: 0px; color: #707070;\">$it</td>\n" +
                                "                                                            </tr>\n"
                )
            }
        }

        return HTML_AUDIT_BODY.replace(HEADER_TITLE_TEMPLATE, title)
                .replace(BODY_PROJECT_TEMPLATE, projectName)
                .replace(BODY_PIPELINE_TEMPLATE, "$pipelineName(#$buildNo)")
                .replace(BODY_TIME_TEMPLATE, time)
                .replace(BODY_INTERCEPT_TEMPLATE, interceptSb.toString())
                .replace(BODY_URL_TEMPLATE, url)
                .replace(BODY_DATE_TEMPLATE, date)
    }

    private val HEADER_TITLE_TEMPLATE = "#{headerTitle}"
    private val BODY_PROJECT_TEMPLATE = "#{bodyProject}"
    private val BODY_PIPELINE_TEMPLATE = "#{bodyPipeline}"
    private val BODY_TIME_TEMPLATE = "#{bodyTime}"
    private val BODY_INTERCEPT_TEMPLATE = "#{bodyIntercept}"
    private val BODY_URL_TEMPLATE = "#{bodyUrl}"
    private val BODY_DATE_TEMPLATE = "#{bodyDate}"

    private val HTML_END_BODY = "<table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt; background-color: #fff; background: #fff;\">\n" +
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
            "                                                                <td style=\"padding-top: 24px; padding-bottom: 0px; color: #707070;\">所属项目：$BODY_PROJECT_TEMPLATE</td>\n" +
            "                                                            </tr>\n" +

            "                                                            <tr class=\"table-title\">\n" +
            "                                                                <td style=\"padding-top: 24px; padding-bottom: 0px; color: #707070;\">流水线：$BODY_PIPELINE_TEMPLATE</td>\n" +
            "                                                            </tr>\n" +

            "                                                            <tr class=\"table-title\">\n" +
            "                                                                <td style=\"padding-top: 18px; padding-bottom: 0px; color: #707070;\">拦截时间：$BODY_TIME_TEMPLATE</td>\n" +
            "                                                            </tr>\n" +

            "                                                            <tr class=\"table-title\">\n" +
            "                                                                <td style=\"padding-top: 18px; padding-bottom: 0px; color: #707070;\">拦截指标：</td>\n" +
            "                                                            </tr>\n" +
            BODY_INTERCEPT_TEMPLATE +

            "                                                            <tr class=\"table-title\">\n" +
            "                                                                <td style=\"padding-top: 18px; padding-bottom: 0px; color: #707070;\">操作：<a href=\"$BODY_URL_TEMPLATE\" style=\"color: #3c96ff\">查看详情</a</td>\n" +
            "                                                            </tr>\n" +

            "                                                        </table>\n" +
            "                                                    </td>\n" +
            "                                                </tr>\n" +
            "                                                <tr class=\"prompt-tips\">\n" +
            "                                                    <td style=\"padding-top: 24px; padding-bottom: 0px; color: #707070;\">如有问题，请联系蓝盾人工客服。</td>\n" +
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
            "                                        <td style=\" padding: 20px 0 20px 36px; border-top: 1px solid #e6e6e6; background: #fff; color: #c7c7c7;\">你收到此邮件，是因为你关注了 $BODY_PROJECT_TEMPLATE 项目，或其他人@了你</td>\n" +
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

    private val HTML_AUDIT_BODY = "<table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt; background-color: #fff; background: #fff;\">\n" +
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
            "                                                                <td style=\"padding-top: 24px; padding-bottom: 0px; color: #707070;\">所属项目：$BODY_PROJECT_TEMPLATE</td>\n" +
            "                                                            </tr>\n" +

            "                                                            <tr class=\"table-title\">\n" +
            "                                                                <td style=\"padding-top: 24px; padding-bottom: 0px; color: #707070;\">流水线：$BODY_PIPELINE_TEMPLATE</td>\n" +
            "                                                            </tr>\n" +

            "                                                            <tr class=\"table-title\">\n" +
            "                                                                <td style=\"padding-top: 18px; padding-bottom: 0px; color: #707070;\">拦截时间：$BODY_TIME_TEMPLATE</td>\n" +
            "                                                            </tr>\n" +

            BODY_INTERCEPT_TEMPLATE +

            "                                                            <tr class=\"table-title\">\n" +
            "                                                                <td style=\"padding-top: 18px; padding-bottom: 0px; color: #707070;\">操作：<a href=\"$BODY_URL_TEMPLATE\" style=\"color: #3c96ff\">去审核</a</td>\n" +
            "                                                            </tr>\n" +

            "                                                        </table>\n" +
            "                                                    </td>\n" +
            "                                                </tr>\n" +
            "                                                <tr class=\"prompt-tips\">\n" +
            "                                                    <td style=\"padding-top: 24px; padding-bottom: 0px; color: #707070;\">如有问题，请联系蓝盾人工客服。</td>\n" +
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
            "                                        <td style=\" padding: 20px 0 20px 36px; border-top: 1px solid #e6e6e6; background: #fff; color: #c7c7c7;\">你收到此邮件，是因为你关注了 $BODY_PROJECT_TEMPLATE 项目，或其他人@了你</td>\n" +
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