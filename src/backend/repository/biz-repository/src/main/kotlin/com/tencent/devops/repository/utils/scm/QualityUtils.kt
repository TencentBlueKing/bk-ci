package com.tencent.devops.repository.utils.scm

object QualityUtils {
    fun getQualityReport(titleData: List<String>, resultData: MutableMap<String, MutableList<List<String>>>): String {

        val status = titleData[0]
        val timeCost = titleData[1]
        val triggerType = titleData[2]
        val pipelineName = titleData[3]
        val url = titleData[4]

        // 生成报表
        val statusLine = if (status == "SUCCEED") "<td style=\"color:#4CAF50;border:none;font-weight: bold;padding-left:0;\">执行成功</td>"
        else "<td style=\"color:red;border:none;font-weight: bold;padding-left:0;\">执行失败</td>"
        val title = "<table><tr>" +
                "<td style=\"border:none;padding-right: 0;\">蓝盾流水线：</td>" +
                "<td style=\"border:none;padding-left:0;\"><a href='$url' style=\"color: #03A9F4\">$pipelineName</a></td>" +
                "<td style=\"border:none;padding-right: 0;\">状态：</td>" +
                statusLine +
                "<td style=\"border:none;padding-right: 0\">触发方式：</td>" +
                "<td style=\"border:none;padding-left:0;\">$triggerType</td>" +
                "<td style=\"border:none;padding-right: 0\">任务耗时：</td>" +
                "<td style=\"border:none;padding-left:0;\">$timeCost</td>" +
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

        return title + body.toString()
    }
}