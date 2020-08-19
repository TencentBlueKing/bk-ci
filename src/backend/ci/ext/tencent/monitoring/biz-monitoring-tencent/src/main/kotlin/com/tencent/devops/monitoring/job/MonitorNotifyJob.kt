package com.tencent.devops.monitoring.job

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.EnumEmailFormat
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
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class MonitorNotifyJob @Autowired constructor(
    private val client: Client,
    private val influxdbClient: InfluxdbClient,
    private val slaDailyDao: SlaDailyDao,
    private val dslContext: DSLContext,
    private val profile: Profile,
    private val restHighLevelClient: RestHighLevelClient
) {

    @Value("\${sla.receivers:#{null}}")
    private var receivers: String? = null

    @Value("\${sla.title:#{null}}")
    private var title: String? = null

    /**
     * 每天发送日报
     */
    @Scheduled(cron = "0 0 10 * * ?")
    fun notifyDaily() {
        /*
        网关访问成功率(DONE)
        CodeCC插件非编译型工具扫描成功率(DONE)
        工蜂回写成功率
        核心插件故障率(DONE)
        公共构建机准备成功率(DONE)
        登录成功率(DONE)
        */
        if (null == receivers || null == title) {
            logger.info("notifyDaily no start , receivers:$receivers , title:$title")
            return
        }

        val yesterday = LocalDateTime.now().minusDays(1)

        val startTime = yesterday.withHour(0).withMinute(0).withSecond(0).timestampmilli()
        val endTime = yesterday.withHour(23).withMinute(59).withSecond(59).timestampmilli()

        val moduleDatas = listOf(
            gatewayStatus(startTime, endTime),
            atomMonitor(startTime, endTime),
            dispatchStatus(startTime, endTime),
            userStatus(startTime, endTime),
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
            reduceMap.asSequence().sortedBy { it.value.left * 100.0 / it.value.right }.take(10).map {
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
            Module.CODECC -> {
                when {
                    profile.isProd() -> "http://9.56.38.242:443/d/l_M8W4HMz/codecckan-ban?orgId=1&from=$startTime&to=$endTime" // TODO
                    profile.isTest() -> "http://9.56.38.242:443/d/l_M8W4HMz/codecckan-ban?orgId=1&from=$startTime&to=$endTime" // TODO
                    else -> "http://9.56.38.242:443/d/l_M8W4HMz/codecckan-ban?orgId=1&from=$startTime&to=$endTime"
                }
            }
            else -> {
                when {
                    profile.isProd() -> "http://9.56.38.242:443/d/NH9FIfVGk/slakan-ban?orgId=1&from=$startTime&to=$endTime" // TODO
                    profile.isTest() -> "http://9.56.38.242:443/d/NH9FIfVGk/slakan-ban?orgId=1&from=$startTime&to=$endTime" // TODO
                    else -> "http://9.56.38.242:443/d/NH9FIfVGk/slakan-ban?orgId=1&from=$startTime&to=$endTime"
                }
            }
        }
    }

    private fun getDetailUrl(startTime: Long, endTime: Long, module: Module, name: String = ""): String {

        when {
            profile.isTest() -> { // TODO
                when (module) {
                    Module.ATOM -> return "http://9.56.38.242:443/d/Z_R3JrVMz/cha-jian-shi-bai-xiang-qing?var-atomCode=$name&from=$startTime&to=$endTime"
                    Module.DISPATCH -> return "http://9.56.38.242:443/d/ET3bo3VGz/gou-jian-ji-shi-bai-xiang-qing?var-buildType=$name&from=$startTime&to=$endTime"
                    Module.USER_STATUS -> return "http://9.56.38.242:443/d/MdTo03VMk/yong-hu-deng-lu-shi-bai-xiang-qing?from=$startTime&to=$endTime"
                    Module.CODECC -> return "http://9.56.38.242:443/d/uJaL6mNMz/codeccgong-ju-shang-bao-xiang-qing?var-toolName=$name&from=$startTime&to=$endTime"
                    Module.GATEWAY -> return "http://logs.ms.devops.oa.com/app/kibana#/discover?_g=(refreshInterval:(pause:!t,value:0),time:(from:'${
                        DateFormatUtils.format(
                            startTime,
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                        )
                    }',mode:absolute,to:'${
                        DateFormatUtils.format(
                            endTime,
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                        )
                    }'))&_a=(columns:!(_source),filters:!(('\$state':(store:appState),meta:(alias:!n,disabled:!f,index:'68f5fd50-798e-11ea-8327-85de2e827c67',key:beat.hostname,negate:!f,params:(query:v2-gateway-idc,type:phrase),type:phrase,value:v2-gateway-idc)," +
                        "query:(match:(beat.hostname:(query:v2-gateway-idc,type:phrase)))),('\$state':(store:appState),meta:(alias:!n,disabled:!f,index:'68f5fd50-798e-11ea-8327-85de2e827c67',key:service,negate:!f,params:(query:$name,type:phrase),type:phrase," +
                        "value:$name),query:(match:(service:(query:$name,type:phrase))))),index:'68f5fd50-798e-11ea-8327-85de2e827c67',interval:auto,query:(language:lucene,query:'beat.hostname:%22v2-gateway-idc%22%20AND%20service:%22$name%22'),sort:!('@timestamp',desc))"
                }
            }
            profile.isProd() -> { // TODO
                when (module) {
                    Module.ATOM -> return "http://9.56.38.242:443/d/Z_R3JrVMz/cha-jian-shi-bai-xiang-qing?var-atomCode=$name&from=$startTime&to=$endTime"
                    Module.DISPATCH -> return "http://9.56.38.242:443/d/ET3bo3VGz/gou-jian-ji-shi-bai-xiang-qing?var-buildType=$name&from=$startTime&to=$endTime"
                    Module.USER_STATUS -> return "http://9.56.38.242:443/d/MdTo03VMk/yong-hu-deng-lu-shi-bai-xiang-qing?from=$startTime&to=$endTime"
                    Module.CODECC -> return "http://9.56.38.242:443/d/uJaL6mNMz/codeccgong-ju-shang-bao-xiang-qing?var-toolName=$name&from=$startTime&to=$endTime"
                    Module.GATEWAY -> return "http://logs.ms.devops.oa.com/app/kibana#/discover?_g=(refreshInterval:(pause:!t,value:0),time:(from:'${
                        DateFormatUtils.format(
                            startTime,
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                        )
                    }',mode:absolute,to:'${
                        DateFormatUtils.format(
                            endTime,
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                        )
                    }'))&_a=(columns:!(_source),filters:!(('\$state':(store:appState),meta:(alias:!n,disabled:!f,index:'68f5fd50-798e-11ea-8327-85de2e827c67',key:beat.hostname,negate:!f,params:(query:v2-gateway-idc,type:phrase),type:phrase,value:v2-gateway-idc)," +
                        "query:(match:(beat.hostname:(query:v2-gateway-idc,type:phrase)))),('\$state':(store:appState),meta:(alias:!n,disabled:!f,index:'68f5fd50-798e-11ea-8327-85de2e827c67',key:service,negate:!f,params:(query:$name,type:phrase),type:phrase," +
                        "value:$name),query:(match:(service:(query:$name,type:phrase))))),index:'68f5fd50-798e-11ea-8327-85de2e827c67',interval:auto,query:(language:lucene,query:'beat.hostname:%22v2-gateway-idc%22%20AND%20service:%22$name%22'),sort:!('@timestamp',desc))"
                }
            }

            else -> {
                when (module) {
                    Module.ATOM -> return "http://9.56.38.242:443/d/Z_R3JrVMz/cha-jian-shi-bai-xiang-qing?var-atomCode=$name&from=$startTime&to=$endTime"
                    Module.DISPATCH -> return "http://9.56.38.242:443/d/ET3bo3VGz/gou-jian-ji-shi-bai-xiang-qing?var-buildType=$name&from=$startTime&to=$endTime"
                    Module.USER_STATUS -> return "http://9.56.38.242:443/d/MdTo03VMk/yong-hu-deng-lu-shi-bai-xiang-qing?from=$startTime&to=$endTime"
                    Module.CODECC -> return "http://9.56.38.242:443/d/uJaL6mNMz/codeccgong-ju-shang-bao-xiang-qing?var-toolName=$name&from=$startTime&to=$endTime"
                    Module.GATEWAY -> return "http://logs.ms.devops.oa.com/app/kibana#/discover?_g=(refreshInterval:(pause:!t,value:0),time:(from:'${
                        DateFormatUtils.format(
                            startTime,
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                        )
                    }',mode:absolute,to:'${
                        DateFormatUtils.format(
                            endTime,
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                        )
                    }'))&_a=(columns:!(_source),filters:!(('\$state':(store:appState),meta:(alias:!n,disabled:!f,index:'68f5fd50-798e-11ea-8327-85de2e827c67',key:beat.hostname,negate:!f,params:(query:v2-gateway-idc,type:phrase),type:phrase,value:v2-gateway-idc)," +
                        "query:(match:(beat.hostname:(query:v2-gateway-idc,type:phrase)))),('\$state':(store:appState),meta:(alias:!n,disabled:!f,index:'68f5fd50-798e-11ea-8327-85de2e827c67',key:service,negate:!f,params:(query:$name,type:phrase),type:phrase," +
                        "value:$name),query:(match:(service:(query:$name,type:phrase))))),index:'68f5fd50-798e-11ea-8327-85de2e827c67',interval:auto,query:(language:lucene,query:'beat.hostname:%22v2-gateway-idc%22%20AND%20service:%22$name%22'),sort:!('@timestamp',desc))"
                }
            }
        }
    }

    enum class Module {
        GATEWAY,
        ATOM,
        DISPATCH,
        USER_STATUS,
        CODECC;
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MonitorNotifyJob::class.java)
    }
}