package com.tencent.devops.monitoring.job

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.Profile
import com.tencent.devops.monitoring.client.InfluxdbClient
import com.tencent.devops.monitoring.dao.SlaDailyDao
import com.tencent.devops.monitoring.util.EmailModuleData
import com.tencent.devops.monitoring.util.EmailUtil
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.commons.lang3.tuple.MutablePair
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
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

@Component
@RefreshScope
class MonitorNotifyJob @Autowired constructor(
    private val client: Client,
    private val influxdbClient: InfluxdbClient,
    private val slaDailyDao: SlaDailyDao,
    private val dslContext: DSLContext,
    private val restHighLevelClient: RestHighLevelClient,
    private val profile: Profile,
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

    /**
     * 每天发送日报
     */
    @Scheduled(cron = "0 0 10 * * ?")
    fun notifyDaily() {
        if (profile.isProd() && !profile.isProdGray()) {
            logger.info("profile is prod , no start")
            return
        }

        if (null == receivers || null == title || //
                null == atomDetailUrl || null == dispatchDetailUrl || null == userStatusDetailUrl || null == codeccDetailUrl || //
                null == atomObservableUrl || null == dispatchObservableUrl || null == userStatusObservableUrl || null == codeccObservableUrl //
        ) {
            logger.info("some params is null , notifyDaily no start")
            return
        }

        val redisLock = RedisLock(redisOperation, "slaDailyEmail", 60L)
        try {
            logger.info("MonitorNotifyJob , notifyDaily start")
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                val yesterday = LocalDateTime.now().minusDays(1)
                val startTime = yesterday.withHour(0).withMinute(0).withSecond(0).timestampmilli()
                val endTime = yesterday.withHour(23).withMinute(59).withSecond(59).timestampmilli()

                val moduleDatas = listOf(
                        gatewayStatus(startTime, endTime),
                        atomMonitor(startTime, endTime),
                        dispatchStatus(startTime, endTime),
                        userStatus(startTime, endTime),
                        commitCheck(startTime, endTime),
                        codecc(startTime, endTime)
                )

                // 发送邮件
                val message = EmailNotifyMessage()
                message.addAllReceivers(receivers!!.split(",").toHashSet())
                message.title = title as String
                message.body = EmailUtil.getEmailBody(startTime, endTime, moduleDatas)
                message.format = EnumEmailFormat.HTML
                client.get(ServiceNotifyResource::class).sendEmailNotify(message)

                // 落库
                val startLocalTime = Instant.ofEpochMilli(startTime).atZone(ZoneOffset.ofHours(8)).toLocalDateTime()
                val endLocalTime = Instant.ofEpochMilli(endTime).atZone(ZoneOffset.ofHours(8)).toLocalDateTime()
                moduleDatas.forEach { m ->
                    m.rowList.forEach { l ->
                        l.run {
                            slaDailyDao.insert(dslContext, m.module, l.first, l.second, startLocalTime, endLocalTime)
                        }
                    }
                }

                logger.info("MonitorNotifyJob , notifyDaily finish")
            } else {
                logger.info("SLA Daily Email is running")
            }
        } catch (e: Throwable) {
            logger.error("SLA Daily Email error:", e)
        } finally {
            redisLock.unlock()
        }
    }

    private fun commitCheck(startTime: Long, endTime: Long): EmailModuleData {
        val sql =
                "SELECT sum(commit_total_count),sum(commit_success_count) FROM CommitCheck_success_rat_count WHERE time>${startTime}000000 AND time<${endTime}000000"
        val queryResult = influxdbClient.select(sql)

        val rowList = mutableListOf<Triple<String, Double, String>>()
        if (null != queryResult && !queryResult.hasError()) {
            queryResult.results.forEach { result ->
                result.series?.forEach { serie ->
                    serie.run {
                        val count = serie.values[0][1].let { if (it is Number) it.toInt() else 1 }
                        val success = serie.values[0][2].let { if (it is Number) it.toInt() else 0 }
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
        } else {
            logger.error("commitCheck , get map error , errorMsg:${queryResult?.error}")
        }

        return EmailModuleData("工蜂回写统计", rowList, getObservableUrl(startTime, endTime, Module.COMMIT_CHECK))
    }

    fun gatewayStatus(startTime: Long, endTime: Long): EmailModuleData {
        val rowList = mutableListOf<Triple<String, Double, String>>()
        for (name in arrayOf(
                "process",
                "dispatch",
                "openapi",
                "artifactory",
                "websocket",
                "store",
                "log",
                "environment"
        )) {
            val errorCount = getHits(startTime, endTime, name, true)
            val totalCount = getHits(startTime, endTime, name)
            rowList.add(
                    Triple(
                            name,
                            100 - (errorCount * 100.0 / totalCount),
                            getDetailUrl(startTime, endTime, Module.GATEWAY, name)
                    )
            )
        }

        return EmailModuleData(
                "网关统计",
                rowList.asSequence().sortedBy { it.second }.toList(),
                getObservableUrl(startTime, endTime, Module.GATEWAY)
        )
    }

    fun userStatus(startTime: Long, endTime: Long): EmailModuleData {
        val sql =
                "SELECT sum(user_total_count),sum(user_success_count) FROM UsersStatus_success_rat_count WHERE time>${startTime}000000 AND time<${endTime}000000"
        val queryResult = influxdbClient.select(sql)

        val rowList = mutableListOf<Triple<String, Double, String>>()
        if (null != queryResult && !queryResult.hasError()) {
            queryResult.results.forEach { result ->
                result.series?.forEach { serie ->
                    serie.run {
                        val count = serie.values[0][1].let { if (it is Number) it.toInt() else 1 }
                        val success = serie.values[0][2].let { if (it is Number) it.toInt() else 0 }
                        rowList.add(
                                Triple(
                                        "userStatus",
                                        success * 100.0 / count,
                                        getDetailUrl(startTime, endTime, Module.USER_STATUS)
                                )
                        )
                    }
                }
            }
        } else {
            logger.error("userStatus , get map error , errorMsg:${queryResult?.error}")
        }

        return EmailModuleData("用户登录统计", rowList, getObservableUrl(startTime, endTime, Module.USER_STATUS))
    }

    fun dispatchStatus(startTime: Long, endTime: Long): EmailModuleData {
        val sql =
                "SELECT sum(devcloud_total_count),sum(devcloud_success_count) FROM DispatchStatus_success_rat_count WHERE time>${startTime}000000 AND time<${endTime}000000 GROUP BY buildType"
        val queryResult = influxdbClient.select(sql)

        val rowList = mutableListOf<Triple<String, Double, String>>()
        if (null != queryResult && !queryResult.hasError()) {
            queryResult.results.forEach { result ->
                result.series?.forEach { serie ->
                    serie.run {
                        val count = serie.values[0][1].let { if (it is Number) it.toInt() else 1 }
                        val success = serie.values[0][2].let { if (it is Number) it.toInt() else 0 }
                        val name = tags["buildType"] ?: "Unknown"
                        rowList.add(
                                Triple(
                                        name,
                                        success * 100.0 / count,
                                        getDetailUrl(startTime, endTime, Module.DISPATCH, name)
                                )
                        )
                    }
                }
            }
        } else {
            logger.error("dispatchStatus , get map error , errorMsg:${queryResult?.error}")
        }

        return EmailModuleData(
                "公共构建机统计",
                rowList.asSequence().sortedBy { it.second }.toList(),
                getObservableUrl(startTime, endTime, Module.DISPATCH)
        )
    }

    fun atomMonitor(startTime: Long, endTime: Long): EmailModuleData {
        val sql =
                "SELECT sum(total_count),sum(success_count),sum(CODE_GIT_total_count),sum(CODE_GIT_success_count),sum(UploadArtifactory_total_count),sum(UploadArtifactory_success_count)," +
                        "sum(linuxscript_total_count),sum(linuxscript_success_count) FROM AtomMonitorData_success_rat_count WHERE time>${startTime}000000 AND time<${endTime}000000"
        val queryResult = influxdbClient.select(sql)

        var totalCount = 1
        var totalSuccess = 0
        var gitCount = 1
        var gitSuccess = 0
        var artiCount = 1
        var artiSuccess = 0
        var shCount = 1
        var shSuccess = 0

        if (null != queryResult && !queryResult.hasError()) {
            queryResult.results.forEach { result ->
                result.series?.forEach { serie ->
                    serie.run {
                        totalCount = serie.values[0][1].let { if (it is Number) it.toInt() else 1 }
                        totalSuccess = serie.values[0][2].let { if (it is Number) it.toInt() else 0 }
                        gitCount = serie.values[0][3].let { if (it is Number) it.toInt() else 1 }
                        gitSuccess = serie.values[0][4].let { if (it is Number) it.toInt() else 0 }
                        artiCount = serie.values[0][5].let { if (it is Number) it.toInt() else 1 }
                        artiSuccess = serie.values[0][6].let { if (it is Number) it.toInt() else 0 }
                        shCount = serie.values[0][7].let { if (it is Number) it.toInt() else 1 }
                        shSuccess = serie.values[0][8].let { if (it is Number) it.toInt() else 0 }
                    }
                }
            }
        } else {
            logger.error("atomMonitor , get map error , errorMsg:${queryResult?.error}")
        }

        val rowList = mutableListOf(
                Triple("所有插件", totalSuccess * 100.0 / totalCount, getDetailUrl(startTime, endTime, Module.ATOM)),
                Triple("Git插件", gitSuccess * 100.0 / gitCount, getDetailUrl(startTime, endTime, Module.ATOM, "CODE_GIT")),
                Triple(
                        "artifactory插件",
                        artiSuccess * 100.0 / artiCount,
                        getDetailUrl(startTime, endTime, Module.ATOM, "UploadArtifactory")
                ),
                Triple(
                        "linuxScript插件",
                        shSuccess * 100.0 / shCount,
                        getDetailUrl(startTime, endTime, Module.ATOM, "linuxScript")
                )
        )

        return EmailModuleData(
                "核心插件统计",
                rowList.asSequence().sortedBy { it.second }.toList(),
                getObservableUrl(startTime, endTime, Module.ATOM)
        )
    }

    fun codecc(startTime: Long, endTime: Long): EmailModuleData {
        val successSql =
                "SELECT SUM(total_count)  FROM CodeccMonitor_reduce WHERE time>${startTime}000000 AND time<${endTime}000000 AND errorCode='0' GROUP BY toolName"
        val errorSql =
                "SELECT SUM(total_count)  FROM CodeccMonitor_reduce WHERE time>${startTime}000000 AND time<${endTime}000000 AND errorCode!='0' GROUP BY toolName"

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
    }

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
            logger.error("codecc , get map error , errorMsg:${queryResult?.error}")
        }
        return codeCCMap
    }

    private fun getHits(startTime: Long, endTime: Long, name: String, error: Boolean = false): Long {
        val sourceBuilder = SearchSourceBuilder()
        val query =
                QueryBuilders.boolQuery().filter(QueryBuilders.rangeQuery("@timestamp").gte(startTime).lte(endTime))
                        .filter(QueryBuilders.queryStringQuery("beat.hostname:\"v2-gateway-idc\" AND service:\"$name\"" + (if (error) " AND status: \"500\"" else "")))
        sourceBuilder.query(query).size(1)

        val searchRequest = SearchRequest()
        searchRequest.indices("bkdevops-gateway-v2-access-${DateFormatUtils.format(startTime, "yyyy.MM.dd")}") // TODO
        searchRequest.source(sourceBuilder)
        val hits = restHighLevelClient.search(searchRequest).hits.getTotalHits()
        logger.info("apiStatus:$name , hits:$hits")
        return hits
    }

    private fun getObservableUrl(startTime: Long, endTime: Long, module: Module): String {
        return when (module) {
            Module.GATEWAY -> "http://opdata.devops.oa.com/d/sL8BLj7Gk/v2-wang-guan-accessjian-kong?orgId=1&from=$startTime&to=$endTime"
            Module.CODECC -> "$codeccObservableUrl?from=$startTime&to=$endTime"
            Module.ATOM -> "$atomObservableUrl?from=$startTime&to=$endTime"
            Module.DISPATCH -> "$dispatchObservableUrl?from=$startTime&to=$endTime"
            Module.USER_STATUS -> "$userStatusObservableUrl?from=$startTime&to=$endTime"
            Module.COMMIT_CHECK -> "$commitCheckObservableUrl?from=$startTime&to=$endTime"
        }
    }

    private fun getDetailUrl(startTime: Long, endTime: Long, module: Module, name: String = ""): String {
        return when (module) {
            Module.GATEWAY -> "http://logs.ms.devops.oa.com/app/kibana#/discover?_g=(refreshInterval:(pause:!t,value:0),time:(from:'${
                LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(startTime),
                        ZoneId.ofOffset("UTC", ZoneOffset.UTC)
                ).toString() + "Z"
            }',mode:absolute,to:'${
                LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(endTime),
                        ZoneId.ofOffset("UTC", ZoneOffset.UTC)
                ).toString() + "Z"
            }'))&_a=(columns:!(_source),filters:!(('\$state':(store:appState),meta:(alias:!n,disabled:!f,index:'68f5fd50-798e-11ea-8327-85de2e827c67'," +
                    "key:beat.hostname,negate:!f,params:(query:v2-gateway-idc,type:phrase),type:phrase,value:v2-gateway-idc),query:(match:(beat.hostname:(query:v2-gateway-idc,type:phrase))))," +
                    "('\$state':(store:appState),meta:(alias:!n,disabled:!f,index:'68f5fd50-798e-11ea-8327-85de2e827c67',key:service,negate:!f,params:(query:$name,type:phrase),type:phrase,value:$name)," +
                    "query:(match:(service:(query:$name,type:phrase)))),('\$state':(store:appState),meta:(alias:!n,disabled:!f,index:'68f5fd50-798e-11ea-8327-85de2e827c67'," +
                    "key:status,negate:!f,params:(query:'500',type:phrase),type:phrase,value:'500'),query:(match:(status:(query:'500',type:phrase))))),index:'68f5fd50-798e-11ea-8327-85de2e827c67'," +
                    "interval:auto,query:(language:lucene,query:''),sort:!('@timestamp',desc))"
            Module.ATOM -> "$atomDetailUrl?var-atomCode=$name&from=$startTime&to=$endTime"
            Module.DISPATCH -> "$dispatchDetailUrl?var-buildType=$name&from=$startTime&to=$endTime"
            Module.USER_STATUS -> "$userStatusDetailUrl?from=$startTime&to=$endTime"
            Module.COMMIT_CHECK -> "$commitCheckDetailUrl?from=$startTime&to=$endTime"
            Module.CODECC -> "$codeccDetailUrl?var-toolName=$name&from=$startTime&to=$endTime"
        }
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
}