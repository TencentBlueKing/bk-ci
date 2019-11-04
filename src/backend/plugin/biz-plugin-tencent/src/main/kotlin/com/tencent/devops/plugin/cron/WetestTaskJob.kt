/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.plugin.cron

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.plugin.client.WeTestClient
import com.tencent.devops.plugin.pojo.wetest.WetestInstStatus
import com.tencent.devops.plugin.service.WetestEmailGroupService
import com.tencent.devops.plugin.service.WetestTaskInstService
import com.tencent.devops.plugin.service.WetestTaskService
import com.tencent.devops.plugin.utils.CommonUtils
import com.tencent.devops.plugin.utils.NotifyUtils
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import javax.ws.rs.NotFoundException

@Component
class WetestTaskJob @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val wetestTaskService: WetestTaskService,
    private val wetestTaskInstService: WetestTaskInstService,
    private val wetestEmailGroupService: WetestEmailGroupService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WetestTaskJob::class.java)
    }

    @Value("\${email.url.logo:#{null}}")
    private lateinit var logoUrl: String

    @Value("\${email.url.title:#{null}}")
    private lateinit var titleUrl: String

    private val executor = Executors.newFixedThreadPool(10)
    private val lockKey = "wetest_task_instance_job"

    // 一分钟
    @Scheduled(initialDelay = 5000, fixedDelay = 60 * 1000)
    fun run() {
        logger.info("<<< WetestTaskJob >>>")
        val redisLock = RedisLock(redisOperation, lockKey, 3600L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("<<< WetestTaskJob Start >>>")
                doJob()
            } else {
                logger.info("<<< WetestTaskJob Has Running, Do Not Start>>>")
            }
        } catch (e: Throwable) {
            logger.error("WeTestTaskJob exception:", e)
        } finally {
            redisLock.unlock()
        }
    }

    private fun doJob() {
        wetestTaskInstService.getUnfinishTask()?.forEach { task ->
            executor.execute {
                // 私有云最长8小时;公有云快速兼容最长900秒,其余测试2小时
                val maxTime = if (task.isPrivateCloud == "1") 8 * 3600 * 1000L else 2 * 3600 * 1000L
                if (System.currentTimeMillis() - task.beginTime.timestampmilli() > maxTime + 30 * 60 * 1000) {
                    wetestTaskInstService.updateTaskInstStatus(task.testId, WetestInstStatus.TIMEOUT)
                } else {
                    val pair = CommonUtils.getCredential(task.startUser)
                    val client = WeTestClient(pair.first, pair.second)
                    val statusJson = client.testStatus(task.testId)
                    val status = statusJson.toMap()
                    val testStatus = status?.get("teststatus") as Map<*, *>
                    if (status["ret"] as Int != 0) {
                        wetestTaskInstService.updateTaskInstStatus(task.testId, WetestInstStatus.FAIL)
                        logger.info("WeTest task failed, msg:$statusJson")
                        sendEmail(true, task.emailGroupId, task.projectId, task.pipelineId, task.buildId, task.testId, statusJson)
                    } else if (testStatus["isdone"] as Boolean) {
                        val privateInfo = wetestTaskService.getPrivateTestInfo(task.startUser, task.projectId, task.testId)
                        val testInfo = privateInfo["testinfo"] as JSONObject
                        val passRate = testInfo["passrate"]
                        val value = passRate as? Int ?: passRate as? Double
                        logger.info("Update weTest task status success, paasRate: $passRate")
                        wetestTaskInstService.updateTaskInstStatus(task.testId, WetestInstStatus.SUCCESS, value?.toString())
                        val emailData = client.getEmailData(task.testId)
                        logger.info("WeTest task success, msg:$statusJson, send email")
                        sendEmail(true, task.emailGroupId, task.projectId, task.pipelineId, task.buildId, task.testId, emailData)
                    }
                }
            }
        }
    }

    private fun sendEmail(isSuccess: Boolean, groupId: Int, projectId: String, pipelineId: String, buildId: String, testId: String, emailData: JSONObject) {
        val pipelineNames = client.get(ServicePipelineResource::class).getPipelineNameByIds(projectId, setOf(pipelineId)).data
                ?: throw RuntimeException("no pipeline name found for$pipelineId")
        val pipelineName = pipelineNames.getValue(pipelineId)
        logger.info("WeTest send email, pipelineName: $pipelineName")
        val weTestRet = emailData.optInt("ret")
        val success = isSuccess && weTestRet == 0
        val title = if (success) "$pipelineName WeTest执行成功" else "$pipelineName WeTest执行失败, 失败信息：${emailData.optString("msg")}"
        logger.info("WeTest send email, title: $title")
        val host = HomeHostUtil.innerServerHost()
        val url = "$host/console/pipeline/$projectId/$pipelineId/detail/$buildId"
        val weTestUrl = "https://wetest.qq.com/cloud/report/result?testid=$testId#0"
        val content = if (!success) "蓝盾流水线执行wetest扫描任务结束，具体可以点击：<a href=\"$url\">查看详情</a> <a target='_blank' href=\"$weTestUrl\">WeTest报告</a>" else createEmailContent(emailData, url, weTestUrl)
        logger.info("WeTest send email, content: $content")

        try {
            val bkAuthProject = client.get(ServiceProjectResource::class).get(projectId).data ?: throw NotFoundException("Fail to find the project info of project($projectId)")
            logger.info("WeTest send email, projectName: ${bkAuthProject.projectName}")
            val templateParams = mapOf(
                    "templateTitle" to title,
                    "templateContent" to content,
                    "projectName" to (bkAuthProject.projectName),
                    "logoUrl" to logoUrl,
                    "titleUrl" to titleUrl
            )
            val message = EmailNotifyMessage().apply {
                format = EnumEmailFormat.HTML
                body = NotifyUtils.parseMessageTemplate(NotifyUtils.EMAIL_BODY, templateParams)
                this.title = title
            }
            val receivers = wetestEmailGroupService.getWetestEmailGroup(projectId, groupId)!!.userInternal.split(",").toSet()
            message.addAllReceivers(receivers)
            client.get(ServiceNotifyResource::class).sendEmailNotify(message)
            logger.info("WeTest send email finished")
        } catch (e: Throwable) {
            logger.error("WeTest send email exception: ", e)
        }
    }

    private fun createEmailContent(emailData: JSONObject, url: String, weTestUrl: String): String {
        try {

            val version = emailData.optString("version")
            val totalDeviceNum = emailData.optString("totalDeviceNum")
            val passDeviceNum = emailData.optString("passDeviceNum")
            val crashDeviceCount = emailData.optString("crashDeviceCount")
            val blackDeviceCount = emailData.optString("blackDeviceCount")
            val over7DeviceCount = emailData.optString("over7DeviceCount")
            val modelCrashRate = emailData.optString("modelCrashRate")
            val modelPassRate = emailData.optString("modelPassRate")
            val casePassRate = emailData.optString("casePassRate")

            val emailBodyTotal = """
                <tr class="information-item">
                <td style="padding-top: 20px; color: #707070;">
                    <div class="item-value" style="margin-top: 4px; line-height: 1.5;">蓝盾流水线执行wetest扫描任务结束，具体可以点击：<a href="$url">查看详情</a> <a target='_blank' href="$weTestUrl">WeTest报告</a></div>
                    <!-- 表格内容 -->
                    <tr class="email-information">
                            <td class="table-info">
                                <table cellpadding="0" cellspacing="0" width="100%" style="font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;">
                                    <tr class="table-title">
                                        <td style="padding-top: 36px; padding-bottom: 14px; color: #707070;">总体概述：</td>
                                    </tr>
                                    <tr>
                                        <td>
                                            <table cellpadding="0" cellspacing="0" width="100%" style="font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6; border-collapse: collapse;">
                                                <tbody style="color: #707070;">
                                                    <tr>
                                                        <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">推送机型总数</td>
                                                        <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">$totalDeviceNum</td>
                                                    </tr>
                                                    <tr>
                                                        <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">运行正常机型数</td>
                                                        <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">$passDeviceNum</td>
                                                    </tr>
                                                    <tr>
                                                        <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">崩溃机型数</td>
                                                        <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">$crashDeviceCount</td>
                                                    </tr>
                                                    <tr>
                                                        <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">黑屏机型数</td>
                                                        <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">$blackDeviceCount</td>
                                                    </tr>
                                                    <tr>
                                                        <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">Android7.0以上机型数</td>
                                                        <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">$over7DeviceCount</td>
                                                    </tr>
                                                    <tr>
                                                        <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">崩溃率</td>
                                                        <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">$modelCrashRate%</td>
                                                    </tr>
                                                    <tr>
                                                        <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">适配机型通过率</td>
                                                        <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">$modelPassRate%</td>
                                                    </tr>
                                                    <tr>
                                                        <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">特性通过率</td>
                                                        <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">$casePassRate%</td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </td>
                </tr>

            """

            val resultStr = StringBuilder(emailBodyTotal).append(
                """

                <tr class="email-information">
                    <td class="table-info">
                        <table cellpadding="0" cellspacing="0" width="100%" style="font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;">
                            <tr class="table-title">
                                <td style="padding-top: 36px; padding-bottom: 14px; color: #707070;">问题概述：</td>
                            </tr>
                            <tr>
                                <td>
                                    <table cellpadding="0" cellspacing="0" width="100%" style="font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6; border-collapse: collapse;">
                                        <thead style="background: #f6f8f8;">
                                            <tr style="color: #333C48;">
                                                <th width="20%" style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">版本</th>
                                                <th width="65%" style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">崩溃类型</th>
                                                <th width="15%" style=" padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;">机型数量</th>
                                            </tr>
                                        </thead>
                                        <tbody style="color: #707070;">
                """
            )
            val crashDetailArray = emailData.optJSONArray("crashDetail")
            crashDetailArray.forEachIndexed { index, it ->
                val obj = it as JSONObject
                if (index == 0) {
                    resultStr.append("""
                                            <tr>
                                                <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;" rowspan="${crashDetailArray.length()}">$version</td>
                                                <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">${obj.optString("type")}</td>
                                                <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">${obj.optInt("num")}</td>
                                            </tr>
                    """)
                } else {
                    resultStr.append("""
                                            <tr>
                                                <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">${obj.optString("type")}</td>
                                                <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">${obj.optInt("num")}</td>
                                            </tr>
                    """)
                }
            }
            resultStr.append("""
                                        </tbody>
                                    </table>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr class="email-information">
                    <td class="table-info">
                        <table cellpadding="0" cellspacing="0" width="100%" style="font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;">
                            <tr class="table-title">
                                <td style="padding-top: 36px; padding-bottom: 14px; color: #707070;">用例概述：</td>
                            </tr>
                            <tr>
                                <td>
                                    <table cellpadding="0" cellspacing="0" width="100%" style="font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6; border-collapse: collapse;">
                                        <thead style="background: #f6f8f8;">
                                            <tr style="color: #333C48;">
                                                <th width="45%" style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">用例名称</th>
                                                <th width="15%" style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">通过机型数</th>
                                                <th width="15%" style=" padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;">失败机型数</th>
                                                <th width="15%" style=" padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;">测试机型数</th>
                                                <th width="10%" style=" padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;">通过率</th>
                                            </tr>
                                        </thead>
                                        <tbody style="color: #707070;">
            """)

            val caseArray = emailData.optJSONArray("caseResult").sortedBy { (it as JSONObject).optInt("rate") }
            caseArray.forEach {
                val obj = it as JSONObject
                resultStr.append("""
                                            <tr>
                                                <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">${obj.optString("testcasename")}</td>
                                                <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">${obj.optInt("pass")}</td>
                                                <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">${obj.optInt("fail")}</td>
                                                <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">${obj.optInt("total")}</td>
                                                <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">${obj.optInt("rate")}%</td>
                                            </tr>

            """)
            }
            resultStr.append("""
                                        </tbody>
                                    </table>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            """)
            return resultStr.toString()
        } catch (e: Throwable) {
            logger.error("", e)
            return ""
        }
    }
}