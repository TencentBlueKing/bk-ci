package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.config.BkConfig
import com.tencent.devops.remotedev.pojo.windows.TimeScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.util.Date

@Service
class BKBaseService @Autowired constructor(
    private val bkConfig: BkConfig
) {
    fun fetchOnlineUserMin(
        timeScope: TimeScope
    ) {
        val sql = when (timeScope) {
            TimeScope.HOUR -> {
                "SELECT user_id_num, dtEventTime " +
                        "FROM 100656_tob_online_user_min.hdfs " +
                        "WHERE dtEventTime >= '${dateFormat.format(Date())}' " +
                        "ORDER BY dtEventTime LIMIT 61"
            }

            TimeScope.DAY -> {

            }

            TimeScope.WEEK -> {

            }
        }
    }

    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    }
}