package com.tencent.devops.dispatch.dao

import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.dispatch.pojo.enums.DockerHostType
import com.tencent.devops.common.service.utils.ByteUtils
import com.tencent.devops.model.dispatch.tables.TDispatchPipelineDockerHostZone
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerHostZoneRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineDockerHostZoneDao {

    fun insertHostZone(
        dslContext: DSLContext,
        hostIp: String,
        zone: String,
        remark: String?,
        type: DockerHostType = DockerHostType.BUILD,
        routeKey: String? = null
    ): Int {
        with(TDispatchPipelineDockerHostZone.T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(
                this,
                HOST_IP,
                ZONE,
                TYPE,
                ROUTE_KEY,
                REMARK,
                CREATED_TIME,
                UPDATED_TIME
            )
                .values(
                    hostIp,
                    zone,
                    type.ordinal,
                    routeKey,
                    remark ?: "",
                    now,
                    now
                ).execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        hostIp: String
    ) {
        with(TDispatchPipelineDockerHostZone.T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE) {
            dslContext.deleteFrom(this)
                    .where(HOST_IP.eq(hostIp))
                    .execute()
        }
    }

    fun count(dslContext: DSLContext) = dslContext.selectCount()
            .from(TDispatchPipelineDockerHostZone.T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE)
            .fetchOne(0, Int::class.java)

    fun getList(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Result<TDispatchPipelineDockerHostZoneRecord>? {
        with(TDispatchPipelineDockerHostZone.T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE) {
            return dslContext.selectFrom(this)
                    .orderBy(CREATED_TIME.desc())
                    .limit(pageSize).offset((page - 1) * pageSize)
                    .fetch()
        }
    }

    fun enable(
        dslContext: DSLContext,
        hostIp: String,
        enable: Boolean
    ) {
        with(TDispatchPipelineDockerHostZone.T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE) {
            dslContext.update(this)
                    .set(ENABLE, ByteUtils.bool2Byte(enable))
                    .where(HOST_IP.eq(hostIp))
                    .execute()
        }
    }

    fun getHostZone(dslContext: DSLContext, hostIp: String): TDispatchPipelineDockerHostZoneRecord? {
        with(TDispatchPipelineDockerHostZone.T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE) {
            return dslContext.selectFrom(this)
                .where(HOST_IP.eq(hostIp))
                .fetchAny()
        }
    }

    fun getOneHostByRandom(dslContext: DSLContext): TDispatchPipelineDockerHostZoneRecord {
        with(TDispatchPipelineDockerHostZone.T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE) {
            return dslContext.selectFrom(this)
                    .orderBy(DSL.rand())
                    .fetchAny()
        }
    }

    fun getOneHostZoneByZone(dslContext: DSLContext, zone: Zone): TDispatchPipelineDockerHostZoneRecord {
        with(TDispatchPipelineDockerHostZone.T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE) {
            return dslContext.selectFrom(this)
                .where(ZONE.eq(zone.name))
                .fetchAny()
        }
    }
}

/*

DROP TABLE IF EXISTS `T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE`;
CREATE TABLE `T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE` (
  `HOST_IP` varchar(128) NOT NULL,
  `ZONE` varchar(128) NOT NULL,
  `ENABLE` TINYINT(1) NULL DEFAULT 1,
  `REMARK` varchar(1024) NULL,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  `TYPE` int(11) NOT NULL DEFAULT '0' COMMENT '母机类型, 0表示是构建环境的构建机, 1表示是无编译环境的机器',
  `ROUTE_KEY` varchar(45) DEFAULT NULL COMMENT '消息队列的路由KEY',
  PRIMARY KEY (`HOST_IP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

 */
