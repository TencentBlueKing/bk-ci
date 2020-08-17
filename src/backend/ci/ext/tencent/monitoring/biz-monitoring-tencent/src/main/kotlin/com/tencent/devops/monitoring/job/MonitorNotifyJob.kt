package com.tencent.devops.monitoring.job

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.monitoring.client.InfluxdbClient
import com.tencent.devops.monitoring.util.EmailUtil
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import org.apache.commons.lang3.tuple.MutablePair
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MonitorNotifyJob @Autowired constructor(
    private val client: Client,
    private val influxdbClient: InfluxdbClient
) {
    /**
     * 每天清理一次一个月前的数据
     */
    @Scheduled(cron = "0 0 4 * * ?")
    fun notifyByEmail() {
        /*OpenAPI访问成功率
        页面500错误率
        CodeCC插件非编译型工具扫描成功率
        工蜂回写成功率
        日志API成功率
        核心插件故障率
        核心插件含：
        拉取GIT（命令行）
        CodeCC代码检查
        归档构件
        归档报告
        公共构建机准备成功率
        登录成功率*/
        codecc(0, 1597664799999)
    }

    fun codecc(startTime: Long, endTime: Long) {
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
            reduceMap.asSequence().sortedBy { it.value.left * 100 / it.value.right }.take(10).map {
                Triple(
                    it.key,
                    "${it.value.left * 100 / (it.value.left + it.value.right)}%",
                    "http://www.tencent.com"
                )
            }.toList()

        val message = EmailNotifyMessage()
        message.addAllReceivers(hashSetOf("stubenhuang@tencent.com"))
        message.title = "测试"
        message.body = EmailUtil.getEmailBody(startTime, endTime, "CodeCC工具", rowList)
        message.format = EnumEmailFormat.HTML

        client.get(ServiceNotifyResource::class).sendEmailNotify(message)
    }

    private fun getCodeCCMap(sql: String): HashMap<String, Int> {
        val queryResult = influxdbClient.select(sql)
        val codeCCMap = HashMap<String/*toolName*/, Int/*count*/>()
        if (null != queryResult && !queryResult.hasError()) {
            queryResult.results.forEach { result ->
                result.series.forEach { serie ->
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

    companion object {
        private val logger = LoggerFactory.getLogger(MonitorNotifyJob::class.java)
    }
}