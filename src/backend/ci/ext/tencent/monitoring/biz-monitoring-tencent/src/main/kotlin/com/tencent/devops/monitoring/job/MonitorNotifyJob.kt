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

package com.tencent.devops.monitoring.job

import com.google.common.collect.Sets
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.notify.utils.HashUtils
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.monitoring.client.InfluxdbClient
import com.tencent.devops.monitoring.dao.SlaDailyDao
import com.tencent.devops.monitoring.util.EmailModuleData
import com.tencent.devops.monitoring.util.EmailUtil
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.commons.lang3.tuple.MutablePair
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.metrics.Avg
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.influxdb.dto.QueryResult
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

@Component
@RefreshScope
@SuppressWarnings(
    "LongParameterList",
    "TooManyFunctions",
    "TooGenericExceptionCaught",
    "MagicNumber",
    "LongMethod",
    "LargeClass",
    "TooGenericExceptionThrown",
    "ThrowsCount"
)
class MonitorNotifyJob @Autowired constructor(
    private val client: Client,
    private val influxdbClient: InfluxdbClient,
    private val slaDailyDao: SlaDailyDao,
    private val dslContext: DSLContext,
    private val restHighLevelClient: RestHighLevelClient,
    private val redisOperation: RedisOperation
) {

    @Value("\${sla.receivers:#{null}}")
    private var receivers: String? = null

    @Value("\${sla.title:#{null}}")
    private var title: String? = null

    @Value("\${sla.url.detail.atom:#{null}}")
    private var atomDetailUrl: String? = null

    @Value("\${sla.url.detail.dispatch:#{null}}")
    private var dispatchDetailUrl: String? = null

    @Value("\${sla.url.detail.userStatus:#{null}}")
    private var userStatusDetailUrl: String? = null

    @Value("\${sla.url.detail.commitCheck:#{null}}")
    private var commitCheckDetailUrl: String? = null

    @Value("\${sla.url.detail.jobTime:#{null}}")
    private var jobTimeDetailUrl: String? = null

    @Value("\${sla.url.detail.codecc:#{null}}")
    private var codeccDetailUrl: String? = null

    @Value("\${sla.url.observable.atom:#{null}}")
    private var atomObservableUrl: String? = null

    @Value("\${sla.url.observable.dispatch:#{null}}")
    private var dispatchObservableUrl: String? = null

    @Value("\${sla.url.observable.userStatus:#{null}}")
    private var userStatusObservableUrl: String? = null

    @Value("\${sla.url.observable.commitCheck:#{null}}")
    private var commitCheckObservableUrl: String? = null

    @Value("\${sla.url.observable.codecc:#{null}}")
    private var codeccObservableUrl: String? = null

    @Value("\${sla.url.observable.jobTime:#{null}}")
    private var jobTimeObservableUrl: String? = null

    @Value("\${sla.oteam.token:#{null}}")
    private var oteamToken: String? = null

    @Value("\${sla.oteam.techmap:#{null}}")
    private var oteamTechmap: String? = null

    @Value("\${sla.oteam.url:#{null}}")
    private var oteamUrl: String? = null

    @Value("\${sla.oteam.target.gateway:#{null}}")
    private var oteamGatewayTarget: Int? = null

    @Value("\${sla.oteam.target.scm:#{null}}")
    private var oteamScmTarget: Int? = null

    @Value("\${sla.oteam.target.atom:#{null}}")
    private var oteamAtomTarget: Int? = null

    @Value("\${sla.oteam.target.job.success:#{null}}")
    private var oteamJobSuccessTarget: Int? = null

    @Value("\${sla.oteam.target.job.time:#{null}}")
    private var oteamJobTimeTarget: Int? = null

    @Value("\${esb.appCode:#{null}}")
    val appCode: String = ""

    @Value("\${esb.appSecret:#{null}}")
    val appSecret: String = ""

    @Value("\${bkdata.url:#{null}}")
    val bkdataUrl: String = ""

    @Value("\${bkdata.token:#{null}}")
    val bkdataToken: String = ""

    @Value("\${pot.sign.url:#{null}}")
    val potSignUrl: String = ""

    @Value("\${pot.sign.user:#{null}}")
    val potSignUser: String = ""

    @Value("\${pot.sign.password:#{null}}")
    val potSignPassword: String = ""

    @Value("\${pot.view.url:#{null}}")
    val potViewUrl: String = ""

    @Value("\${pot.view.id:#{null}}")
    val potViewId: String = ""

    @Value("\${sla.oteam.target.coverage:#{null}}")
    private var oteamCoverageTarget: Int? = null

    @Value("\${sla.oteam.target.user:#{null}}")
    private var oteamUserTarget: Int? = null

    /**
     * 每天发送日报
     */
    @Scheduled(cron = "0 0 2 * * ?")
    fun notifyDaily() {
        if (illegalConfig()) {
            logger.info("some params is null , notifyDaily no start")
            return
        }

        val redisLock = RedisLock(redisOperation, "slaDailyEmail", 60L)
        try {
            logger.info("MonitorNotifyJob , notifyDaily start")
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                doNotify()
                logger.info("MonitorNotifyJob , notifyDaily finish")
            } else {
                logger.info("SLA Daily Email is running")
            }
        } catch (ignored: Throwable) {
            logger.warn("send SLA Daily Email fail!", ignored)
        }
    }

    private fun illegalConfig() =
        null == receivers || null == title || null == atomDetailUrl ||
                null == dispatchDetailUrl || null == userStatusDetailUrl ||
                null == codeccDetailUrl || null == jobTimeDetailUrl ||
                null == atomObservableUrl || null == dispatchObservableUrl ||
                null == userStatusObservableUrl || null == codeccObservableUrl ||
                null == jobTimeObservableUrl

    private fun doNotify() {
        val yesterday = LocalDateTime.now().minusDays(1)
        val startTime = yesterday.withHour(0).withMinute(0).withSecond(0).timestampmilli()
        val endTime = yesterday.withHour(23).withMinute(59).withSecond(59).timestampmilli()

        val moduleDataList = listOf(
            gatewayStatus(startTime, endTime),
            atomMonitor(startTime, endTime),
            dispatchStatus(startTime, endTime),
            userStatus(startTime, endTime),
            commitCheck(startTime, endTime),
            codecc(startTime, endTime),
            dispatchTime(startTime),
            coverage(startTime),
            userNum(startTime)
        )

        // 发送邮件
        val message = EmailNotifyMessage()
        message.addAllReceivers(receivers!!.split(",").toHashSet())
        message.title = title as String
        message.body = EmailUtil.getEmailBody(startTime, endTime, moduleDataList)
        message.format = EnumEmailFormat.HTML
        client.get(ServiceNotifyResource::class).sendEmailNotify(message)

        // 落库
        val startLocalTime = Instant.ofEpochMilli(startTime).atZone(ZoneOffset.ofHours(8)).toLocalDateTime()
        val endLocalTime = Instant.ofEpochMilli(endTime).atZone(ZoneOffset.ofHours(8)).toLocalDateTime()
        moduleDataList.forEach { m ->
            m.rowList.forEach { l ->
                l.run {
                    slaDailyDao.insert(dslContext, m.module, l.first, l.second, startLocalTime, endLocalTime)
                }
            }
        }
    }

    private fun userNum(startTime: Long): EmailModuleData {
        // 用户数
        val reqBody = mapOf(
            "bkdata_authentication_method" to "token",
            "bkdata_data_token" to bkdataToken,
            "bk_app_code" to appCode,
            "bk_app_secret" to appSecret,
            "sql" to """
                    SELECT COUNT(distinct userId) as dailyCount
                    FROM 100205_bkdevops_build_history.druid
                    WHERE thedate='${DateFormatUtils.format(startTime, "yyyyMMdd")}'
                    LIMIT 1
                """.trimIndent(),
            "prefer_storage" to ""
        )
        val bkDataBean = getBkData(reqBody)
        val userNum = bkDataBean.data.list.map { it.values.first() }.first().toDouble()

        // 上报数据
        oteamStatus(userNum, oteamUserTarget, startTime)

        return EmailModuleData(
            "Oteam",
            listOf(Triple("使用用户数", userNum, "https://techmap.woa.com/oteam/8524/operation/coverage")),
            "https://techmap.woa.com/oteam/8524/operation/coverage",
            "人数",
            "人"
        )
    }

    private fun getBkData(reqBody: Map<String, String>): BkDataBean {
        val resp = OkhttpUtils.doPost(
            bkdataUrl,
            JsonUtil.toJson(reqBody),
            mapOf("Content-Type" to "application/json; charset=utf-8")
        )
        if (!resp.isSuccessful) {
            throw RuntimeException("gitResponse is failed , $resp")
        } else if (resp.body == null) {
            throw RuntimeException("gitResponse is empty ")
        }

        return JsonUtil.to(resp.body!!.string(), BkDataBean::class.java)
    }

    private fun coverage(startTime: Long): EmailModuleData {
        try {
            val localDateTime =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault()).withDayOfMonth(1)
            // 蓝盾插件的项目列表
            val reqBody = mapOf(
                "bkdata_authentication_method" to "token",
                "bkdata_data_token" to bkdataToken,
                "bk_app_code" to appCode,
                "bk_app_secret" to appSecret,
                "sql" to """
                    SELECT distinct(projectName)
                    FROM 100205_build_atom_metrics_git.hdfs
                    WHERE thedate>='${localDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}'
                    LIMIT 3000000
                """.trimIndent(),
                "prefer_storage" to ""
            )
            val bkDataBean = getBkData(reqBody)
            val gitPluginProjects = bkDataBean.data.list.map { it.values.first() }.toSet()
            logger.info("git plugin size: ${gitPluginProjects.size}")

            // 工蜂项目列表
            val potAuthResp = OkhttpUtils.doPost(
                potSignUrl,
                """{"credentials":{"name":"$potSignUser","password":"$potSignPassword","site":{"contentUrl":""}}}"""
            )
            var siteId: String? = null
            var token: String? = null
            if (potAuthResp.isSuccessful) {
                potAuthResp.body?.let {
                    val builderFactory = DocumentBuilderFactory.newInstance()
                    builderFactory.isValidating = false
                    builderFactory.isIgnoringElementContentWhitespace = true
                    val builder = builderFactory.newDocumentBuilder()
                    val document = builder.parse(it.byteStream())
                    siteId =
                        document.getElementsByTagName("site").item(0).attributes.getNamedItem("id").nodeValue
                    token =
                        document.getElementsByTagName("credentials").item(0).attributes.getNamedItem("token").nodeValue
                } ?: throw RuntimeException("potAuthResp is empty")
            } else {
                throw RuntimeException("potAuthResp is failed , $potAuthResp")
            }
            val potDataUrl = potViewUrl.replace("##siteId##", siteId!!).replace("##viewId##", potViewId)
            val potDataResp = OkhttpUtils.doGet(potDataUrl, mapOf("x-tableau-auth" to token!!))
            val potProjects = if (potDataResp.isSuccessful) {
                potDataResp.body?.run {
                    IOUtils.readLines(byteStream(), Charsets.UTF_8).filter { it.contains("提交次数") }
                        .map { it.split(",")[0] }.toSet()
                } ?: throw RuntimeException("potDataResp is empty")
            } else {
                throw RuntimeException("potDataResp is failed , $potDataResp")
            }
            logger.info("pot projects size: ${potProjects.size}")

            // 占比
            val percent = 100.0 * Sets.intersection(gitPluginProjects, potProjects).size / potProjects.size

            // 上报数据
            oteamStatus(percent, oteamCoverageTarget, startTime)

            return EmailModuleData(
                "Oteam",
                listOf(Triple("覆盖度", percent, "https://techmap.woa.com/oteam/8524/operation/coverage")),
                "https://techmap.woa.com/oteam/8524/operation/coverage",
                "比例"
            )
        } catch (ignored: Throwable) {
            logger.warn("get oteamCoverage fail!", ignored)
            return EmailModuleData(
                "Oteam",
                emptyList(),
                "https://techmap.woa.com/oteam/8524/operation/coverage"
            )
        }
    }

    private fun dispatchTime(startTime: Long): EmailModuleData {
        val sourceBuilder = SearchSourceBuilder()
        val queryStringQuery = QueryBuilders.queryStringQuery(
            """
              status:200 AND host:"devnet-backend.devops.oa.com" AND log:"2Fstart" AND !log:"2Fdevops.apigw.o.oa.com"
            """.trimIndent()
        )
        sourceBuilder.query(QueryBuilders.boolQuery().must(queryStringQuery))
        sourceBuilder.aggregation(AggregationBuilders.avg("avg_ms").field("ms"))

        val searchRequest = SearchRequest()
        searchRequest.indices("v2_9_bklog_prod_ci_gateway_access_${DateFormatUtils.format(startTime, "yyyyMMdd")}*")
        searchRequest.source(sourceBuilder)

        val aggregations = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT).aggregations
        val avgSecs = aggregations.get<Avg>("avg_ms").value

        oteamStatus(avgSecs, oteamJobTimeTarget, startTime)

        return EmailModuleData(
            module = "构建机启动耗时",
            rowList = listOf(Triple("dispatch", avgSecs, jobTimeDetailUrl!!)),
            observableUrl = jobTimeObservableUrl,
            amountKey = "耗时",
            amountUnit = "ms"
        )
    }

    private fun commitCheck(startTime: Long, endTime: Long): EmailModuleData {
        try {
            val sql =
                "SELECT sum(commit_total_count),sum(commit_success_count) FROM CommitCheck_success_rat_count " +
                        "WHERE time>${startTime}000000 AND time<${endTime}000000"
            val queryResult = influxdbClient.select(sql)

            val rowList = mutableListOf<Triple<String, Double, String>>()
            if (null != queryResult && !queryResult.hasError()) {
                putCommitCheckRowList(queryResult, rowList, startTime, endTime)
            } else {
                logger.warn("commitCheck , get map error , errorMsg:${queryResult?.error}")
            }

            if (rowList.size > 0) {
                oteamStatus(rowList[0].second, oteamScmTarget, startTime)
            }

            return EmailModuleData("工蜂回写统计", rowList, getObservableUrl(startTime, endTime, Module.COMMIT_CHECK))
        } catch (ignored: Throwable) {
            logger.warn("commitCheck fail!", ignored)
            return EmailModuleData("工蜂回写统计", emptyList(), getObservableUrl(startTime, endTime, Module.COMMIT_CHECK))
        }
    }

    private fun putCommitCheckRowList(
        queryResult: QueryResult,
        rowList: MutableList<Triple<String, Double, String>>,
        startTime: Long,
        endTime: Long
    ) {
        queryResult.results.forEach { result ->
            result.series?.forEach { serie ->
                val countAny = serie.values[0][1]
                val successAny = serie.values[0][2]
                val count = if (countAny is Number) countAny.toInt() else 1
                val success = if (successAny is Number) successAny.toInt() else 0
                rowList.add(
                    Triple(
                        "CommitCheck",
                        success * 100.0 / count,
                        getDetailUrl(startTime, endTime, Module.COMMIT_CHECK)
                    )
                )
            }
        }
    }

    fun gatewayStatus(startTime: Long, endTime: Long): EmailModuleData {
        try {
            val rowList = mutableListOf<Triple<String, Double, String>>()
            for (name in arrayOf(
                "process",
                "dispatch",
                "openapi",
                "websocket",
                "store",
                "log",
                "environment"
            )) {
                val errorCount = getHits(startTime, name, true)
                val totalCount = getHits(startTime, name).coerceAtLeast(1)
                rowList.add(
                    Triple(
                        name,
                        100 - (errorCount * 100.0 / totalCount),
                        getDetailUrl(startTime, endTime, Module.GATEWAY, name)
                    )
                )
            }

            oteamStatus(rowList.asSequence().map { it.second }.average(), oteamGatewayTarget, startTime)

            return EmailModuleData(
                "网关统计",
                rowList.asSequence().sortedBy { it.second }.toList(),
                getObservableUrl(startTime, endTime, Module.GATEWAY)
            )
        } catch (ignored: Throwable) {
            logger.warn("gatewayStatus fail!", ignored)
            return EmailModuleData(
                "网关统计",
                emptyList(),
                getObservableUrl(startTime, endTime, Module.GATEWAY)
            )
        }
    }

    /**
     * 上报数据到oteam
     */
    @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught")
    private fun oteamStatus(
        data: Double,
        targetId: Int?,
        startTime: Long
    ) {
        if (null == oteamUrl) {
            logger.warn("null oteamUrl , can not oteam status , targetId: $targetId , data: $data")
            return
        }
        try {
            val yesterday = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault())
            val timestamp = "${System.currentTimeMillis() / 1000}"
            val token = oteamToken
            val techmapId = oteamTechmap
            val techmapType = "oteam"
            val signature = HashUtils.sha256(timestamp + techmapType + techmapId + token + timestamp)
            val response = OkhttpUtils.doPost(
                url = oteamUrl!!,
                jsonParam = """
                        {
                          "method":"measureReport",
                          "params": {
                            "type":"daily",
                            "year":${yesterday.year},
                            "month":${yesterday.month.value},
                            "day":${yesterday.dayOfMonth},
                            "targetId":${targetId!!}, 
                            "value":$data
                          },
                          "jsonrpc":"2.0",
                          "id":"$timestamp"
                        }
                    """.trimIndent(),
                headers = mapOf(
                    "timestamp" to timestamp,
                    "techmapType" to techmapType,
                    "techmapId" to techmapId!!,
                    "signature" to signature,
                    "content-type" to "application/json;charset=UTF-8"
                )
            )
            logger.info("oteam status , id:{} , resp:{}", timestamp, response.body!!.string())
        } catch (ignored: Throwable) {
            logger.warn("oteam data report fail!", ignored)
        }
    }

    fun userStatus(startTime: Long, endTime: Long): EmailModuleData {
        try {
            val sql =
                "SELECT sum(user_total_count),sum(user_success_count) FROM UsersStatus_success_rat_count " +
                        "WHERE time>${startTime}000000 AND time<${endTime}000000"
            val queryResult = influxdbClient.select(sql)

            val rowList = mutableListOf<Triple<String, Double, String>>()
            if (null != queryResult && !queryResult.hasError()) {
                putUserStatusRowList(queryResult, rowList, startTime, endTime)
            } else {
                logger.warn("getUserStatus|error=${queryResult?.error}")
            }

            return EmailModuleData("用户登录统计", rowList, getObservableUrl(startTime, endTime, Module.USER_STATUS))
        } catch (ignored: Throwable) {
            logger.warn("getUserStatus fail!", ignored)
            return EmailModuleData("用户登录统计", emptyList(), getObservableUrl(startTime, endTime, Module.USER_STATUS))
        }
    }

    private fun putUserStatusRowList(
        queryResult: QueryResult,
        rowList: MutableList<Triple<String, Double, String>>,
        startTime: Long,
        endTime: Long
    ) {
        queryResult.results.forEach { result ->
            result.series?.forEach { serie ->
                val countAny = serie.values[0][1]
                val successAny = serie.values[0][2]
                val success = if (successAny is Number) successAny.toInt() else 0
                rowList.add(
                    Triple(
                        "userStatus",
                        success * 100.0 / if (countAny is Number) countAny.toInt() else 1,
                        getDetailUrl(startTime, endTime, Module.USER_STATUS)
                    )
                )
            }
        }
    }

    fun dispatchStatus(startTime: Long, endTime: Long): EmailModuleData {
        try {
            val totalTemplateSql = "SELECT count(errorCode) FROM DispatchStatus WHERE buildType='%s' " +
                    "AND time>${startTime}000000 AND time<${endTime}000000"
            val failedTemplateSql = "SELECT count(errorCode) FROM DispatchStatus WHERE buildType='%s' " +
                    "AND errorCode != '0' AND errorType!='USER' " +
                    "AND time>${startTime}000000 AND time<${endTime}000000"

            val rowList = mutableListOf<Triple<String, Double, String>>()
            val buildTypes = listOf(
                ".macos",
                "third",
                ".devcloud.public",
                ".codecc.scan",
                ".docker.vm",
                ".pcg.sumeru",
                ".gitci.public"
            )

            for (buildType in buildTypes) {
                val failedCount = getInfluxValue(String.format(failedTemplateSql, buildType), 0)
                val totalCount = getInfluxValue(String.format(totalTemplateSql, buildType), 1)

                rowList.add(
                    Triple(
                        buildType,
                        (totalCount - failedCount) * 100.0 / totalCount,
                        getDetailUrl(startTime, endTime, Module.DISPATCH, buildType)
                    )
                )
            }

            oteamStatus(
                rowList.filter { it.first == "third" || it.first == ".docker.vm" }.map { it.second }.average(),
                oteamJobSuccessTarget,
                startTime
            )

            return EmailModuleData(
                "公共构建机统计",
                rowList.asSequence().sortedBy { it.second }.toList(),
                getObservableUrl(startTime, endTime, Module.DISPATCH)
            )
        } catch (ignored: Throwable) {
            logger.warn("getDispatchStatus fail!", ignored)
            return EmailModuleData(
                "公共构建机统计",
                emptyList(),
                getObservableUrl(startTime, endTime, Module.DISPATCH)
            )
        }
    }

    @SuppressWarnings("NestedBlockDepth", "SwallowedException")
    private fun getInfluxValue(sql: String, defaultValue: Int): Int {
        val queryResult = influxdbClient.select(sql)
        return if (null != queryResult && !queryResult.hasError()) {
            try {
                queryResult.results[0].series[0].values[0][1].let { if (it is Number) it.toInt() else defaultValue }
            } catch (e: Exception) {
                defaultValue
            }
        } else {
            defaultValue
        }
    }

    @SuppressWarnings("ComplexMethod", "NestedBlockDepth", "LongMethod")
    fun atomMonitor(startTime: Long, endTime: Long): EmailModuleData {
        try {

            val atomTotalTemplateSql =
                "select count(errorCode) FROM AtomMonitorData where errorCode != -1 AND atomCode = '%s' " +
                        "AND time>${startTime}000000 AND time<${endTime}000000 "
            val atomFailedTemplateSql =
                "select count(errorCode) FROM AtomMonitorData where errorCode != -1 AND errorCode!=0 " +
                        "AND errorType != 'USER' AND atomCode = '%s' " +
                        "AND time>${startTime}000000 AND time<${endTime}000000 "

            val totalCount = getInfluxValue(
                "select count(errorCode) FROM AtomMonitorData where errorCode != -1 " +
                        "AND time>${startTime}000000 AND time<${endTime}000000 ",
                1
            )
            val totalFailed = getInfluxValue(
                "select count(errorCode) FROM AtomMonitorData where errorCode != -1 AND errorCode!=0 " +
                        "AND time>${startTime}000000 AND time<${endTime}000000 " +
                        "AND errorType != 'USER' ",
                0
            )
            val gitCount = getInfluxValue(String.format(atomTotalTemplateSql, "CODE_GIT"), 1)
            val gitFailed = getInfluxValue(String.format(atomFailedTemplateSql, "CODE_GIT"), 0)
            val artiCount = getInfluxValue(String.format(atomTotalTemplateSql, "UploadArtifactory"), 1)
            val artiFailed = getInfluxValue(String.format(atomFailedTemplateSql, "CODE_GIT"), 0)
            val shCount = getInfluxValue(String.format(atomTotalTemplateSql, "linuxScript"), 1)
            val shFailed = getInfluxValue(String.format(atomFailedTemplateSql, "CODE_GIT"), 0)

            val rowList = mutableListOf(
                Triple(
                    "所有插件",
                    (totalCount - totalFailed) * 100.0 / totalCount,
                    getDetailUrl(startTime, endTime, Module.ATOM)
                ),
                Triple(
                    "Git插件",
                    (gitCount - gitFailed) * 100.0 / gitCount,
                    getDetailUrl(startTime, endTime, Module.ATOM, "CODE_GIT")
                ),
                Triple(
                    "artifactory插件",
                    (artiCount - artiFailed) * 100.0 / artiCount,
                    getDetailUrl(startTime, endTime, Module.ATOM, "UploadArtifactory")
                ),
                Triple(
                    "linuxScript插件",
                    (shCount - shFailed) * 100.0 / shCount,
                    getDetailUrl(startTime, endTime, Module.ATOM, "linuxScript")
                )
            )

            oteamStatus(
                100 - (artiFailed + gitFailed + shFailed) * 100.0 / (artiCount + gitCount + shCount),
                oteamAtomTarget,
                startTime
            )

            return EmailModuleData(
                "核心插件统计",
                rowList.asSequence().sortedBy { it.second }.toList(),
                getObservableUrl(startTime, endTime, Module.ATOM)
            )
        } catch (ignored: Throwable) {
            logger.warn("getAtomMonitorData fail!", ignored)
            return EmailModuleData(
                "核心插件统计",
                emptyList(),
                getObservableUrl(startTime, endTime, Module.ATOM)
            )
        }
    }

    fun codecc(startTime: Long, endTime: Long): EmailModuleData {
        try {
            val successSql =
                "SELECT SUM(total_count)  FROM CodeccMonitor_reduce " +
                        "WHERE time>${startTime}000000 AND time<${endTime}000000 AND errorCode='0' GROUP BY toolName"
            val errorSql =
                "SELECT SUM(total_count)  FROM CodeccMonitor_reduce " +
                        "WHERE time>${startTime}000000 AND time<${endTime}000000 AND errorCode!='0' GROUP BY toolName"

            val successMap = getCodeCCMap(successSql)
            val errorMap = getCodeCCMap(errorSql)

            val reduceMap = HashMap<String/*toolName*/, MutablePair<Int/*success*/, Int/*error*/>>()

            for ((k, v) in successMap) {
                reduceMap[k] = MutablePair(v, 0)
            }
            for ((k, v) in errorMap) {
                if (reduceMap.containsKey(k)) {
                    reduceMap[k]?.right = v
                } else {
                    reduceMap[k] = MutablePair(0, v)
                }
            }

            val rowList =
                reduceMap.asSequence().sortedBy { it.value.left * 100.0 / it.value.right }.map {
                    Triple(
                        it.key,
                        it.value.left * 100.0 / (it.value.left + it.value.right),
                        getDetailUrl(startTime, endTime, Module.CODECC, it.key)
                    )
                }.toList()

            return EmailModuleData("CodeCC工具统计", rowList, getObservableUrl(startTime, endTime, Module.CODECC))
        } catch (ignored: Throwable) {
            logger.warn("getCodeccMonitorData fail!", ignored)
            return EmailModuleData("CodeCC工具统计", emptyList(), getObservableUrl(startTime, endTime, Module.CODECC))
        }
    }

    @SuppressWarnings("NestedBlockDepth")
    private fun getCodeCCMap(sql: String): HashMap<String, Int> {
        val queryResult = influxdbClient.select(sql)
        val codeCCMap = HashMap<String/*toolName*/, Int/*count*/>()
        if (null != queryResult && !queryResult.hasError()) {
            queryResult.results.forEach { result ->
                result.series?.forEach { serie ->
                    serie.run {
                        val key = tags["toolName"]
                        if (null != key) {
                            val value = if (values.size > 0 && values[0].size > 1) values[0][1] else 0
                            codeCCMap[key] = if (value is Number) value.toInt() else 0
                        }
                    }
                }
            }
        } else {
            logger.warn("codecc , get map error , errorMsg:${queryResult?.error}")
        }
        return codeCCMap
    }

    private fun getHits(startTime: Long, name: String, error: Boolean = false): Long {
        val sourceBuilder = SearchSourceBuilder()
        val queryStringQuery = QueryBuilders.queryStringQuery(
            """
            path:"/data/bkci/logs/$name/access_log.log" 
            ${if (error) " AND status:[500 TO *] " else ""}
            """.trimIndent()
        )
        val query =
            QueryBuilders.boolQuery()
                .filter(
                    queryStringQuery
                )
        sourceBuilder.query(query).trackTotalHits(true).size(1)

        val searchRequest = SearchRequest()
        searchRequest.indices("v2_9_bklog_prod_ci_service_access_${DateFormatUtils.format(startTime, "yyyyMMdd")}*")
        searchRequest.source(sourceBuilder)
        val hits = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT).hits.totalHits?.value ?: 0
        logger.info("apiStatus:$name , hits:$hits")
        return hits
    }

    private fun getObservableUrl(startTime: Long, endTime: Long, module: Module): String {
        return when (module) {
            Module.GATEWAY -> "http://opdata.devops.oa.com/" +
                    "d/sL8BLj7Gk/v2-wang-guan-accessjian-kong?orgId=1&from=$startTime&to=$endTime"
            Module.CODECC -> "$codeccObservableUrl?from=$startTime&to=$endTime"
            Module.ATOM -> "$atomObservableUrl?from=$startTime&to=$endTime"
            Module.DISPATCH -> "$dispatchObservableUrl?from=$startTime&to=$endTime"
            Module.USER_STATUS -> "$userStatusObservableUrl?from=$startTime&to=$endTime"
            Module.COMMIT_CHECK -> "$commitCheckObservableUrl?from=$startTime&to=$endTime"
        }
    }

    private fun getDetailUrl(startTime: Long, endTime: Long, module: Module, name: String = ""): String {
        return when (module) {
            Module.GATEWAY -> gatewayDetailUrl(startTime, endTime, name)
            Module.ATOM -> "$atomDetailUrl?var-atomCode=$name&from=$startTime&to=$endTime"
            Module.DISPATCH -> "$dispatchDetailUrl?var-buildType=$name&from=$startTime&to=$endTime"
            Module.USER_STATUS -> "$userStatusDetailUrl?from=$startTime&to=$endTime"
            Module.COMMIT_CHECK -> "$commitCheckDetailUrl?from=$startTime&to=$endTime"
            Module.CODECC -> "$codeccDetailUrl?var-toolName=$name&from=$startTime&to=$endTime"
        }
    }

    private fun gatewayDetailUrl(startTime: Long, endTime: Long, name: String): String {
        val startTimeStr = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(startTime),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        ).toString()
        val endTimeStr = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(endTime),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        ).toString()
        return """
            http://logs.ms.devops.oa.com/app/kibana#/discover?_g=(refreshInterval:(pause:!t,value:0),
            time:(from:'${startTimeStr}Z',mode:absolute,to:'${endTimeStr}Z'))&
            _a=(columns:!(_source),index:'4b38ef10-9da1-11eb-8559-712c276f42f2',
            interval:auto,query:(language:lucene,query:'path:%22%2Fdata%2Fbkci%2Flogs%2F$name%2Faccess_log.log
            %22%20AND%20status:%5B500%20TO%20*%5D'),sort:!(time,desc))
        """.trimIndent().replace("\n", "")
    }

    enum class Module {
        GATEWAY,
        ATOM,
        DISPATCH,
        USER_STATUS,
        COMMIT_CHECK,
        CODECC;
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MonitorNotifyJob::class.java)
    }

    data class BkDataBean(
        val result: Boolean,
        val message: String,
        val code: String,
        val data: BkDataData,
        val error: String?
    )

    @SuppressWarnings("ConstructorParameterNaming")
    data class BkDataData(
        val result_table_scan_range: Map<*, *>,
        val cluster: String,
        val totalRecords: Int,
        val timetaken: Double,
        val list: List<Map<String, String>>,
        val bksql_call_elapsed_time: Int,
        val device: String,
        val result_table_ids: List<String>,
        val select_fields_order: List<String>,
        val sql: String
    )
}
