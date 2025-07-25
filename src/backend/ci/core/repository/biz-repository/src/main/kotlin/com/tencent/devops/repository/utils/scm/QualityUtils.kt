/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.repository.utils.scm

object QualityUtils {

    @Suppress("ALL")
    fun getQualityReport(titleData: List<String>, resultData: MutableMap<String, MutableList<List<String>>>): String {

        val triggerType = titleData[2]
        val pipelineName = titleData[3]
        val url = titleData[4]
        val pipelineNameTitle = titleData[5]
        val ruleName = titleData[6]
        // codecc开源扫描不需要展示title,只需要展示质量红线明细
        val (showTitle, pipelineLinkElement) = if (url.isBlank()) {
            Pair(false, pipelineName)
        } else {
            Pair(true, "<a href='$url' style=\"color: #03A9F4\">$pipelineName</a>")
        }
        val title = "<table><tr>" +
            "<td style=\"border:none;padding-right: 0;\">$pipelineNameTitle：</td>" +
            "<td style=\"border:none;padding-left:0;\">$pipelineLinkElement</td>" +
            "<td style=\"border:none;padding-right: 0\">触发方式：</td>" +
            "<td style=\"border:none;padding-left:0;\">$triggerType</td>" +
            "<td style=\"border:none;padding-right: 0\">质量红线：</td>" +
            "<td style=\"border:none;padding-left:0;\">$ruleName</td>" +
            "</tr></table>"
        val body = StringBuilder("")
        body.append("<table border=\"1\" cellspacing=\"0\" width=\"450\">")
        body.append("<tr>")
        body.append("<th style=\"text-align:left;\">质量红线产出插件</th>")
        body.append("<th style=\"text-align:left;\">指标</th>")
        body.append("<th style=\"text-align:left;\">结果</th>")
        body.append("<th style=\"text-align:left;\">预期</th>")
        body.append("<th style=\"text-align:left;\"></th>")
        body.append("</tr>")
        resultData.forEach { elementName, result ->
            result.forEachIndexed { index, list ->
                body.append("<tr>")
                if (index == 0) body.append("<td>$elementName</td>") else body.append("<td></td>")
                body.append("<td>${list[0]}</td>")
                body.append("<td>${list[1]}</td>")
                body.append("<td>${list[2]}</td>")
                if (list[3] == "true") {
                    body.append("<td style=\"color: #4CAF50; font-weight: bold;\">&nbsp; &radic; &nbsp;</td>")
                } else {
                    body.append("<td style=\"color: #F44336; font-weight: bold;\">&nbsp; &times; &nbsp;</td>")
                }
                body.append("</tr>")
            }
        }
        body.append("</table>")

        return if (showTitle) {
            title + body.toString()
        } else {
            body.toString()
        }
    }
}
