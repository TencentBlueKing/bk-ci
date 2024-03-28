package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.remotedev.config.BkConfig
import com.tencent.devops.remotedev.pojo.windows.TimeScope
import com.tencent.devops.remotedev.pojo.windows.UserLoginTimeResp
import com.tencent.devops.remotedev.pojo.windows.UserLoginTimeRespData
import java.security.cert.CertificateException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class BKBaseService @Autowired constructor(
    private val bkConfig: BkConfig,
    private val objectMapper: ObjectMapper
) {
    fun fetchOnlineUserMin(
        timeScope: TimeScope?,
        projectId: String
    ): UserLoginTimeResp? {
        val gal = Calendar.getInstance()
        val sql = when (timeScope) {
            TimeScope.DAY -> {
                gal.add(Calendar.DAY_OF_WEEK, -1)
                "SELECT minute2, COUNT(DISTINCT user_id) AS unum " +
                        "FROM 100656_cgs_report_game_all " +
                        "WHERE dtEventTime >= '${dateFormat.format(gal.time)}' " +
                        "AND project_id = '$projectId' " +
                        "GROUP BY minute2 " +
                        "ORDER BY minute2 " +
                        "LIMIT 721"
            }

            TimeScope.WEEK -> {
                gal.add(Calendar.WEEK_OF_MONTH, -1)
                "SELECT minute10, COUNT(DISTINCT user_id) AS unum " +
                        "FROM 100656_cgs_report_game_all " +
                        "WHERE dtEventTime >= '${dateFormat.format(gal.time)}' " +
                        "AND project_id = '$projectId' " +
                        "GROUP BY minute10 " +
                        "ORDER BY minute10 " +
                        "LIMIT 1009"
            }

            else -> {
                gal.add(Calendar.HOUR_OF_DAY, -1)
                "SELECT minute1, COUNT(DISTINCT user_id) AS unum " +
                        "FROM 100656_cgs_report_game_all " +
                        "WHERE dtEventTime >= '${dateFormat.format(gal.time)}' " +
                        "AND project_id = '$projectId' " +
                        "GROUP BY minute1 " +
                        "ORDER BY minute1 " +
                        "LIMIT 61"
            }
        }

        val url = "${bkConfig.baseUrl}/prod/v3/queryengine/query_sync/"
        val body = BakeBaseQuerySyncReq(
            bkdataDataToken = bkConfig.baseToken,
            bkAppCode = bkConfig.appCode,
            bkAppSecret = bkConfig.appSecret,
            sql = sql
        )
        val request = Request.Builder()
            .url(url)
            .post(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        val resp = try {
            okHttpClient.newCall(request).execute().use { response ->
                val data = response.body!!.string()
                logger.debug("fetchOnlineUserMin｜req|{}|response code|{}|content|{}", body, response.code, data)
                if (!response.isSuccessful) {
                    logger.error("fetchOnlineUserMin｜req|{}|response code|{}|content|{}", body, response.code, data)
                    return null
                }

                val resp = objectMapper.readValue<BakeBaseQuerySyncResp>(data)
                if (!resp.result) {
                    logger.error("fetchOnlineUserMin｜req|{}|response code|{}|content|{}", body, response.code, data)
                    return null
                }
                resp
            }
        } catch (e: Exception) {
            logger.error("fetchOnlineUserMin request error", e)
            return null
        }

        val result = mutableListOf<UserLoginTimeRespData>()
        try {
            resp.data?.list?.forEach { l ->
                val data = when (timeScope) {
                    TimeScope.DAY -> {
                        UserLoginTimeRespData(
                            num = l["unum"] as Int? ?: 0,
                            time = if (l["minute2"] == null) {
                                ""
                            } else {
                                dateFormat.format(dataInputFormat.parse(l["minute2"] as String))
                            }
                        )
                    }

                    TimeScope.WEEK -> {
                        UserLoginTimeRespData(
                            num = l["unum"] as Int? ?: 0,
                            time = if (l["minute10"] == null) {
                                ""
                            } else {
                                dateFormat.format(dataInputFormat.parse(l["minute10"] as String))
                            }
                        )
                    }

                    else -> {
                        UserLoginTimeRespData(
                            num = l["unum"] as Int? ?: 0,
                            time = if (l["minute1"] == null) {
                                ""
                            } else {
                                dateFormat.format(dataInputFormat.parse(l["minute1"] as String))
                            }
                        )
                    }
                }
                result.add(data)
            } ?: return UserLoginTimeResp(0, emptyList())
        } catch (e: Exception) {
            logger.error("fetchOnlineUserMin parse data error", e)
            return UserLoginTimeResp(0, emptyList())
        }

        return UserLoginTimeResp(resp.data.totalRecords, result)
    }

    fun fetchActiveIps(
        date: LocalDateTime,
        limit: Int = 1000,
        offset: Int = 0,
        result: MutableMap<String, Int> = mutableMapOf()
    ): Map<String, Int> {
        val sql = "select zone_id,inner_ip,count(distinct thedate) as cnt " +
                "from 100656_ads_desktop_daily_activity_res " +
                "where thedate > '${date.format(theDateFormat)}' and activity_flag > 0 " +
                "group by inner_ip,zone_id LIMIT $limit OFFSET $offset"

        val resp = doHttp(sql) ?: return result

        try {
            resp.data?.list?.forEach { l ->
                result["${l["zone_id"] as String}.${l["inner_ip"] as String}"] = l["cnt"] as Int
            } ?: return result
            if (resp.data.list.size == limit) {
                fetchActiveIps(
                    date, offset + limit, limit, result
                )
            }
        } catch (e: Exception) {
            logger.error("fetchActiveIps parse data error", e)
            return result
        }

        return result
    }

    // 获取云桌面的活跃时长
    fun fetchActiveTimes(
        date: LocalDateTime,
        limit: Int = 1000,
        offset: Int = 0,
        result: MutableMap<String, Int> = mutableMapOf()
    ): Map<String, Int> {
        val sql = "select zone_id,inner_ip,sum(activity_minus_cnt) as cnt " +
            "from 100656_ads_desktop_daily_activity_res " +
            "where thedate > '${date.format(theDateFormat)}' " +
            "group by inner_ip,zone_id LIMIT $limit OFFSET $offset"

        val resp = doHttp(sql) ?: return result

        try {
            resp.data?.list?.forEach { l ->
                result["${l["zone_id"] as String}.${l["inner_ip"] as String}"] = l["cnt"] as Int
            } ?: return result
            if (resp.data.list.size == limit) {
                fetchActiveTimes(
                    date, offset + limit, limit, result
                )
            }
        } catch (e: Exception) {
            logger.error("fetchActiveTimes parse data error", e)
            return result
        }

        return result
    }

    private fun doHttp(
        sql: String
    ): BakeBaseQuerySyncResp? {
        val body = BakeBaseQuerySyncReq(
            bkdataDataToken = bkConfig.baseToken,
            bkAppCode = bkConfig.appCode,
            bkAppSecret = bkConfig.appSecret,
            sql = sql
        )
        val url = "${bkConfig.baseUrl}/prod/v3/queryengine/query_sync/"
        val request = Request.Builder()
            .url(url)
            .post(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                val data = response.body!!.string()
                logger.info("fetchOnlineUserMin｜req|{}|response code|{}|content|{}", body, response.code, data)
                if (!response.isSuccessful) {
                    logger.error("fetchOnlineUserMin｜req|{}|response code|{}|content|{}", body, response.code, data)
                    return null
                }

                val resp = objectMapper.readValue<BakeBaseQuerySyncResp>(data)
                if (!resp.result) {
                    logger.error("fetchOnlineUserMin｜req|{}|response code|{}|content|{}", body, response.code, data)
                    return null
                }
                return resp
            }
        } catch (e: Exception) {
            logger.error("fetchOnlineUserMin request error", e)
            return null
        }
    }

    fun fetchOnlineIps(
        date: LocalDateTime,
        limit: Int = 1000,
        offset: Int = 0,
        result: MutableMap<String, String> = mutableMapOf()
    ): Map<String, String> {
        val sql = "SELECT node_id, MAX(dtEventTime) " +
                "FROM 100656_cgs_report_game_all " +
                "WHERE thedate >= '${date.format(theDateFormat)}' " +
                "GROUP BY node_id LIMIT $limit OFFSET $offset"

        val resp = doHttp(sql) ?: return result

        try {
            resp.data?.list?.forEach { l ->
                result.put(l["node_id"] as String, l["_col1"] as String)
            } ?: return result
            if (resp.data.list.size == limit) {
                fetchOnlineIps(
                    date, offset + limit, limit, result
                )
            }
        } catch (e: Exception) {
            logger.error("fetchOnlineUserMin parse data error", e)
            return result
        }

        return result
    }

    fun fetchLastOnline(
        nodeIds: Set<String>
    ): Map<String, String> {
        val sql = "SELECT node_id, MAX(dtEventTime) " +
            "FROM 100656_cgs_report_game_all " +
            "WHERE where node_id in (${
            nodeIds.joinToString(separator = "','", prefix = "'", postfix = "'")
            }) " +
            "GROUP BY node_id"

        val url = "${bkConfig.baseUrl}/prod/v3/queryengine/query_sync/"
        val body = BakeBaseQuerySyncReq(
            bkdataDataToken = bkConfig.baseToken,
            bkAppCode = bkConfig.appCode,
            bkAppSecret = bkConfig.appSecret,
            sql = sql
        )
        val request = Request.Builder()
            .url(url)
            .post(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        val resp = try {
            okHttpClient.newCall(request).execute().use { response ->
                val data = response.body!!.string()
                logger.debug("fetchLastOnline｜req|{}|response code|{}|content|{}", body, response.code, data)
                if (!response.isSuccessful) {
                    logger.error("fetchLastOnline｜req|{}|response code|{}|content|{}", body, response.code, data)
                    return emptyMap()
                }

                val resp = objectMapper.readValue<BakeBaseQuerySyncResp>(data)
                if (!resp.result) {
                    logger.error("fetchLastOnline｜req|{}|response code|{}|content|{}", body, response.code, data)
                    return emptyMap()
                }
                resp
            }
        } catch (e: Exception) {
            logger.error("fetchLastOnline request error", e)
            return emptyMap()
        }

        val result = mutableMapOf<String, String>()
        try {
            resp.data?.list?.forEach { l ->
                result[l["node_id"] as String] = l["_col1"] as String
            } ?: return result
        } catch (e: Exception) {
            logger.error("fetchLastOnline parse data error", e)
            return result
        }

        return result
    }

    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        private val theDateFormat = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val dataInputFormat = SimpleDateFormat("yyyyMMddHHmm")
        private val logger = LoggerFactory.getLogger(BKBaseService::class.java)

        // 数据平台请求的时间有点长，这里拿 okhttp 单独加下时间
        private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
            }

            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                return arrayOf()
            }
        })
        private val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10L, TimeUnit.SECONDS)
            .readTimeout(120L, TimeUnit.SECONDS)
            .writeTimeout(120L, TimeUnit.SECONDS)
            .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()

        private fun sslSocketFactory(): SSLSocketFactory {
            try {
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                return sslContext.socketFactory
            } catch (ingored: Exception) {
                throw RemoteServiceException(ingored.message!!)
            }
        }
    }
}

data class BakeBaseQuerySyncReq(
    @JsonProperty("bkdata_authentication_method")
    val bkdataAuthenticationMethod: String = "token",
    @JsonProperty("bkdata_data_token")
    val bkdataDataToken: String,
    @JsonProperty("bk_app_code")
    val bkAppCode: String,
    @JsonProperty("bk_app_secret")
    val bkAppSecret: String,
    val sql: String,
    @JsonProperty("prefer_storage")
    val preferStorage: String = ""
)

data class BakeBaseQuerySyncResp(
    val result: Boolean,
    val message: String,
    val code: String,
    val data: BakeBaseQuerySyncRespData?,
    val errors: Any?
)

data class BakeBaseQuerySyncRespData(
    val totalRecords: Int,
    val timetaken: Double,
    val list: List<Map<String, Any>>
)
