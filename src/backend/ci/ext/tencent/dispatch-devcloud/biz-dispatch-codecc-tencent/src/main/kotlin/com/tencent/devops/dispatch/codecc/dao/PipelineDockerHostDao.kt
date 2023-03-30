package com.tencent.devops.dispatch.codecc.dao

import com.tencent.devops.model.dispatch.codecc.tables.TDispatchPipelineDockerHost
import com.tencent.devops.model.dispatch.codecc.tables.records.TDispatchPipelineDockerHostRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineDockerHostDao {

    fun insertHost(
        dslContext: DSLContext,
        projectId: String,
        hostIp: String,
        remark: String?
    ): Int {
        with(TDispatchPipelineDockerHost.T_DISPATCH_PIPELINE_DOCKER_HOST) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(
                this,
                PROJECT_CODE,
                HOST_IP,
                REMARK,
                CREATED_TIME,
                UPDATED_TIME
            )
                .values(
                    projectId,
                    hostIp,
                    remark ?: "",
                    now,
                    now
                ).execute()
        }
    }

    fun updateHost(
        dslContext: DSLContext,
        projectId: String,
        hostIp: String,
        remark: String?
    ): Int {
        with(TDispatchPipelineDockerHost.T_DISPATCH_PIPELINE_DOCKER_HOST) {
            val now = LocalDateTime.now()
            return dslContext.update(this)
                .set(HOST_IP, hostIp)
                .set(REMARK, remark)
                .set(UPDATED_TIME, now)
                .where(PROJECT_CODE.eq(projectId))
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String
    ): Int {
        with(TDispatchPipelineDockerHost.T_DISPATCH_PIPELINE_DOCKER_HOST) {
            return dslContext.delete(this).where(PROJECT_CODE.eq(projectId)).execute()
        }
    }

    fun getHost(
        dslContext: DSLContext,
        projectId: String
    ): TDispatchPipelineDockerHostRecord? {
        with(TDispatchPipelineDockerHost.T_DISPATCH_PIPELINE_DOCKER_HOST) {
            return dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectId))
                .fetchAny()
        }
    }

    fun getHostIps(
        dslContext: DSLContext,
        projectId: String
    ): List<String> {
        with(TDispatchPipelineDockerHost.T_DISPATCH_PIPELINE_DOCKER_HOST) {
            val result = dslContext.select(HOST_IP).from(this)
                .where(PROJECT_CODE.eq(projectId))
                .fetchOne(HOST_IP)

            return if (result != null && result.isNotEmpty()) {
                result.split(",")
            } else {
                emptyList()
            }
        }
    }
}

/*

DROP TABLE IF EXISTS `T_DISPATCH_PIPELINE_DOCKER_HOST`;
CREATE TABLE `T_DISPATCH_PIPELINE_DOCKER_HOST` (
  `PROJECT_CODE` varchar(128) NOT NULL,
  `HOST_IP` varchar(128) NOT NULL,
  `REMARK` varchar(1024) DEFAULT NULL,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  `TYPE` int(11) NOT NULL DEFAULT '0' COMMENT '母机类型, 0表示是构建环境的构建机, 1表示是无编译环境的机器',
  `ROUTE_KEY` varchar(45) DEFAULT NULL COMMENT '消息队列的路由KEY',
  PRIMARY KEY (`PROJECT_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8
 */
