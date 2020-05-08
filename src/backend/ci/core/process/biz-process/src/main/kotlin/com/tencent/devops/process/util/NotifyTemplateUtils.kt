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

package com.tencent.devops.process.util

import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.process.utils.PIPELINE_TIME_DURATION
import com.tencent.devops.process.utils.PROJECT_NAME_CHINESE

object NotifyTemplateUtils {

    const val EMAIL_STARTUP_TITLE = "蓝盾流水线【#{pipelineName}】##{buildNum} 开始构建"
    const val EMAIL_SHUTDOWN_SUCCESS_TITLE = "蓝盾流水线【#{pipelineName}】##{buildNum} 构建成功"
    const val EMAIl_SHUTDOWN_FAILURE_TITLE = "蓝盾流水线【#{pipelineName}】##{buildNum} 构建失败"
    const val EMAIL_STARTUP_BODY = "蓝盾流水线【#{projectName}】-【#{pipelineName}】##{buildNum} 构建任务开始执行，<a href=\"#{detailUrl}\">点此</a>查看详情"
    const val EMAIL_SHUTDOWN_SUCCESS_BODY =
        "流水线 #{projectName} - #{pipelineName}<br>\n" +
            "<br>\n" +
            "构建号 ##{buildNum}<br>\n" +
            "<br>\n" +
            "开始时间 #{startTime}<br>\n" +
            "<br>\n" +
            "耗时 #{duration}<br>\n" +
            "<br>\n" +
            "触发方式 #{trigger}（#{username}）<br>\n" +
            "<br>\n" +
            "#{emailSuccessContent} <br>\n" +
            "<br>\n" +
            "<br><a href=\"#{detailUrl}\">查看详情</a><br>\n"
    const val EMAIL_SHUTDOWN_FAILURE_BODY =
        "流水线 #{pipelineName} - #{projectName}<br>\n" +
            "<br>\n" +
            "构建号 ##{buildNum}<br>\n" +
            "<br>\n" +
            "开始时间 #{startTime}<br>\n" +
            "<br>\n" +
            "耗时 #{duration}<br>\n" +
            "<br>\n" +
            "触发方式 #{trigger}（#{username}）<br>\n" +
            "<br>\n" +
            "#{emailFailContent} <br>\n" +
            "<br>\n" +
            "<br><a href=\"#{detailUrl}\">查看详情</a><br>\n"

    const val RTX_STARTUP_TITLE = "蓝盾流水线【#{pipelineName}】##{buildNum} 开始执行"
    const val RTX_STARTUP_BODY_DETAIL = "【#{projectName}】-【#{pipelineName}】##{buildNum} 开始执行 \n\n查看详情:#{detailUrl}"
    const val RTX_STARTUP_BODY = "【#{projectName}】-【#{pipelineName}】##{buildNum} 开始执行"
    const val RTX_SHUTDOWN_SUCCESS_TITLE = "蓝盾流水线【#{pipelineName}】##{buildNum} 构建成功"
    const val RTX_SHUTDOWN_FAILURE_TITLE = "蓝盾流水线【#{pipelineName}】##{buildNum} 构建失败"
    const val RTX_SHUTDOWN_SUCCESS_BODY_DETAIL = "✔️#{successContent} \n\n查看详情:#{detailUrl}"
    const val RTX_SHUTDOWN_SUCCESS_BODY = "✔️#{successContent}"
    const val RTX_SHUTDOWN_FAILURE_BODY_DETAIL = "❌#{failContent} \n\n查看详情:#{detailUrl}"
    const val RTX_SHUTDOWN_FAILURE_BODY = "❌#{failContent}"
    const val SMS_SHUTDOWN_SUCCESS_BODY_DETAIL = "#{successContent} \n\n查看详情: #{detailShortOuterUrl}"
    const val SMS_SHUTDOWN_SUCCESS_BODY = "#{successContent}"
    const val SMS_SHUTDOWN_FAILURE_BODY_DETAIL = "#{failContent} \n\n查看详情: #{detailShortOuterUrl}"
    const val SMS_SHUTDOWN_FAILURE_BODY = "#{failContent}"
    const val WECHAT_SHUTDOWN_SUCCESS_BODY_DETAIL = "✔️#{successContent} \n\n查看详情:#{detailUrl}"
    const val WECHAT_SHUTDOWN_SUCCESS_BODY = "✔️#{successContent}"
    const val WECHAT_SHUTDOWN_FAILURE_BODY_DETAIL = "❌#{failContent} \n\n查看详情:#{detailUrl}"
    const val WECHAT_SHUTDOWN_FAILURE_BODY = "❌#{failContent}"
    const val WECHAT_GROUP_SHUTDOWN_SUCCESS_BODY_DETAIL = "✔️#{successContent} \n\n查看详情: #{detailUrl}"
    const val WECHAT_GROUP_SHUTDOWN_SUCCESS_BODY = "✔️#{successContent}"
    const val WECHAT_GROUP_SHUTDOWN_FAILURE_BODY_DETAIL = "❌#{failContent} \n\n查看详情: #{detailUrl}"
    const val WECHAT_GROUP_SHUTDOWN_FAILURE_BODY = "❌#{failContent}"
    const val COMMON_SHUTDOWN_SUCCESS_CONTENT = "【\${$PROJECT_NAME_CHINESE}】- 【\${$PIPELINE_NAME}】#\${$PIPELINE_BUILD_NUM} 执行成功，耗时\${$PIPELINE_TIME_DURATION}, 触发人：\${$PIPELINE_START_USER_NAME}。"
    const val COMMON_SHUTDOWN_FAILURE_CONTENT = "【\${$PROJECT_NAME_CHINESE}】- 【\${$PIPELINE_NAME}】#\${$PIPELINE_BUILD_NUM} 执行失败，耗时\${$PIPELINE_TIME_DURATION}, 触发人：\${$PIPELINE_START_USER_NAME}。 "

    val EMAIL_BODY = "<table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt; background-color: #fff; background: #fff;\">\n" +
        "\t<tbody>\n" +
        "\t\t<tr>\n" +
        "\t\t\t<td align=\"center\" valign=\"top\" width=\"100%\" style=\"padding: 16px;\">\n" +
        "\t\t\t   <table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"956\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
        "\t\t\t\t\t<tbody>\n" +
        "\t\t\t\t\t\t<tr>\n" +
        "\t\t\t\t\t\t\t<td valign=\"top\" align=\"center\">\n" +
        "\t\t\t\t\t\t\t\t<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#f9f8f6\" class=\"layout layout-table root-table\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
        "\t\t\t\t\t\t\t\t\t<tbody>\n" +
        "\t\t\t\t\t\t\t\t\t\t<tr style=\"height: 64px; background: #555;\">\n" +
        "\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding-left: 24px;\" width=\"60\" align=\"center\">\n" +
        "\t\t\t\t\t\t\t\t\t\t\t\t<img src=\"#{logoUrl}\" width=\"52\" style=\"display: block\">\n" +
        "\t\t\t\t\t\t\t\t\t\t\t</td>\n" +
        "\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding-left: 6px;\">\n" +
        "\t\t\t\t\t\t\t\t\t\t\t\t<img src=\"#{titleUrl}\" width=\"176\" style=\"display: block\">\n" +
        "\t\t\t\t\t\t\t\t\t\t\t</td>\n" +
        "\t\t\t\t\t\t\t\t\t\t</tr>\n" +
        "\t\t\t\t\t\t\t\t\t</tbody>\n" +
        "\t\t\t\t\t\t\t\t</table>\n" +
        "\t\t\t\t\t\t\t</td>\n" +
        "\t\t\t\t\t\t</tr>\n" +
        "\t\t\t\t\t\t<tr>\n" +
        "\t\t\t\t\t\t\t<td valign=\"top\" align=\"center\" style=\"padding: 24px;\" bgcolor=\"#f9f8f6\">\n" +
        "\t\t\t\t\t\t\t\t<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6;\">\n" +
        "\t\t\t\t\t\t\t\t\t<tr>\n" +
        "\t\t\t\t\t\t\t\t\t\t<td class=\"email-title\" style=\"padding: 20px 36px; line-height: 1.5; border-bottom: 1px solid #e6e6e6; background: #fff; font-size: 22px;\">#{templateTitle}</td>\n" +
        "\t\t\t\t\t\t\t\t\t</tr>\n" +
        "\t\t\t\t\t\t\t\t\t<tr>\n" +
        "\t\t\t\t\t\t\t\t\t\t<td class=\"email-content\" style=\"padding: 0 36px; background: #fff;\">\n" +
        "\t\t\t\t\t\t\t\t\t\t\t<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
        "\t\t\t\t\t\t\t\t\t\t\t\t<tr>\n" +
        "\t\t\t\t\t\t\t\t\t\t\t\t\t<td class=\"email-source\" style=\"padding: 14px 0; color: #bebebe;\">来自蓝盾DevOps平台的推送</td>\n" +
        "\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
        "\n" +
        "\t\t\t\t\t\t\t\t\t\t\t\t<tr class=\"information-item\">\n" +
        "\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding-top: 20px; color: #707070;\">\n" +
        "\t\t\t\t\t\t\t\t\t\t\t\t\t\t<div class=\"item-value\" style=\"margin-top: 4px; line-height: 1.5;\">#{templateContent}</div>\n" +
        "\t\t\t\t\t\t\t\t\t\t\t\t\t</td>\n" +
        "\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
        "\t\t\t\t\t\t\t\t\t\t\t\t<tr class=\"prompt-tips\">\n" +
        "\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding-top: 32px; padding-bottom: 10px; color: #707070;\">如有任何问题，可随时联系蓝盾助手。</td>\n" +
        "\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
        "\t\t\t\t\t\t\t\t\t\t\t</table>\n" +
        "\t\t\t\t\t\t\t\t\t\t</td>\n" +
        "\t\t\t\t\t\t\t\t\t</tr>\n" +
        "\t\t\t\t\t\t\t\t\t<tr class=\"email-footer\">\n" +
        "\t\t\t\t\t\t\t\t\t\t<td style=\" padding: 20px 0 20px 36px; border-top: 1px solid #e6e6e6; background: #fff; color: #c7c7c7;\">你收到此邮件，是因为你关注了 #{projectName} 项目，或其他人@了你</td>\n" +
        "\t\t\t\t\t\t\t\t\t</tr>\n" +
        "\t\t\t\t\t\t\t\t</table>\n" +
        "\t\t\t\t\t\t\t</td>\n" +
        "\t\t\t\t\t\t</tr>\n" +
        "\t\t\t\t\t</tbody>\n" +
        "\t\t\t   </table>\n" +
        "\t\t\t</td>\n" +
        "\t\t</tr>\n" +
        "\t</tbody>\n" +
        "</table>"
}
