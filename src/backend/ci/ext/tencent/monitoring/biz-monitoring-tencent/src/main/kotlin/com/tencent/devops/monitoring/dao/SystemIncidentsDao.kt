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
package com.tencent.devops.monitoring.dao

import com.tencent.devops.model.monitoring.tables.TAlertUser
import com.tencent.devops.model.monitoring.tables.TIncident
import com.tencent.devops.model.monitoring.tables.records.TAlertUserRecord
import com.tencent.devops.model.monitoring.tables.records.TIncidentRecord
import com.tencent.devops.monitoring.pojo.Incident
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class SystemIncidentsDao {

    fun add(dslContext: DSLContext, incident: Incident): Long {
        with(TIncident.T_INCIDENT) {
            return dslContext.insertInto(
                this,
                DAY_TIME,
                MODULE_NAME,
                LEVEL,
                DURING_TIME,
                STATUS,
                MESSAGE,
                CREATED_TIME,
                UPDATED_TIME
            ).values(
                    Timestamp(incident.dayTime).toLocalDateTime(),
                    incident.moduleName,
                    incident.level,
                    incident.duringTime,
                    incident.status.statusName,
                    incident.message ?: "",
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .returning(ID)
                .fetchOne().id
        }
    }

    fun update(dslContext: DSLContext, incident: Incident) {
        with(TIncident.T_INCIDENT) {
            dslContext.update(this)
                .set(DAY_TIME, Timestamp(incident.dayTime).toLocalDateTime())
                .set(MODULE_NAME, incident.moduleName)
                .set(LEVEL, incident.level)
                .set(DURING_TIME, incident.duringTime)
                .set(STATUS, incident.status.statusName)
                .set(MESSAGE, incident.message ?: "")
                .set(UPDATED_TIME, java.time.LocalDateTime.now())
                .where(ID.eq(incident.id))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, incidentId: Long): TIncidentRecord? {
        with(TIncident.T_INCIDENT) {
            return dslContext.selectFrom(this)
                .where(ID.eq(incidentId))
                .fetchOne()
        }
    }

    fun delete(dslContext: DSLContext, incidentId: Long) {
        with(TIncident.T_INCIDENT) {
            dslContext.deleteFrom(this)
                .where(ID.eq(incidentId))
                .execute()
        }
    }

    fun list(dslContext: DSLContext): List<TAlertUserRecord> {
        with(TAlertUser.T_ALERT_USER) {
            return dslContext.selectFrom(this)
                    .fetch()
        }
    }

}

/*

CREATE TABLE `T_INCIDENT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `DAY_TIME` datetime NOT NULL,
  `MODULE_NAME` varchar(128) NOT NULL,
  `LEVEL` varchar(32) NOT NULL,
  `DURING_TIME` int(20) NOT NULL,
  `STATUS` varchar(32) NOT NULL,
  `MESSAGE` varchar(2048) NULL,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8

 */