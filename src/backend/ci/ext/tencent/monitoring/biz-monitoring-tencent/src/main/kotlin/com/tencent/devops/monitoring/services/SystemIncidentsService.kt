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
package com.tencent.devops.monitoring.services

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.monitoring.dao.SystemIncidentsDao
import com.tencent.devops.monitoring.pojo.Incident
import com.tencent.devops.monitoring.pojo.enums.IncidentStatus
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service

@Service
@RefreshScope
class SystemIncidentsService @Autowired constructor(
    private val dslContext: DSLContext,
    private val systemIncidentsDao: SystemIncidentsDao
) {
    private val logger = LoggerFactory.getLogger(SystemIncidentsService::class.java)

    fun addIncidents(incident: Incident): Long {
        logger.info("add incident, incident: $incident")
        return systemIncidentsDao.add(dslContext, incident)
    }

    fun updateIncidents(incident: Incident) {
        logger.info("update incident, incident: $incident")
        systemIncidentsDao.update(dslContext, incident)
    }

    fun deleteIncidents(incidentId: Long) {
        logger.info("update incident, incident: $incidentId")
        systemIncidentsDao.delete(dslContext, incidentId)
    }

    fun getIncidents(incidentId: Long): Incident? {
        val incident = systemIncidentsDao.get(dslContext, incidentId) ?: return null
        return Incident(
            incident.id,
            DateTimeUtil.convertLocalDateTimeToTimestamp(incident.dayTime),
            incident.moduleName,
            incident.level,
            incident.duringTime,
            IncidentStatus.parse(incident.status),
            incident.message ?: ""
        )
    }

}
