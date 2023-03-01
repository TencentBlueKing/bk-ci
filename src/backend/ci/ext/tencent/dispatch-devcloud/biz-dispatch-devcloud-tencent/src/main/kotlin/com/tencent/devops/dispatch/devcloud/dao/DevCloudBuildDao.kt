package com.tencent.devops.dispatch.devcloud.dao

import com.tencent.devops.model.dispatch_devcloud.tables.TDevcloudBuild
import com.tencent.devops.model.dispatch_devcloud.tables.records.TDevcloudBuildRecord
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.Field
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class DevCloudBuildDao {

    private val logger = LoggerFactory.getLogger(DevCloudBuildDao::class.java)

    fun createOrUpdate(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        poolNo: Int,
        projectId: String,
        containerName: String,
        image: String,
        status: Int,
        userId: String,
        cpu: Int,
        memory: String,
        disk: String
    ): Int {
        return with(TDevcloudBuild.T_DEVCLOUD_BUILD) {
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                VM_SEQ_ID,
                POOL_NO,
                PROJECT_ID,
                CONTAINER_NAME,
                IMAGES,
                STATUS,
                CREATED_TIME,
                UPDATE_TIME,
                USER_ID,
                CPU,
                MEMORY,
                DISK
            ).values(
                pipelineId,
                vmSeqId,
                poolNo,
                projectId,
                containerName,
                image,
                status,
                LocalDateTime.now(),
                LocalDateTime.now(),
                userId,
                cpu,
                memory,
                disk
            ).onDuplicateKeyUpdate()
                .set(PIPELINE_ID, pipelineId)
                .set(VM_SEQ_ID, vmSeqId)
                .set(POOL_NO, poolNo)
                .set(PROJECT_ID, projectId)
                .set(CONTAINER_NAME, containerName)
                .set(IMAGES, image)
                .set(STATUS, status)
                .set(CPU, cpu)
                .set(MEMORY, memory)
                .set(DISK, disk)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun create(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        poolNo: Int,
        projectId: String,
        containerName: String,
        image: String,
        status: Int,
        userId: String
    ) {
        with(TDevcloudBuild.T_DEVCLOUD_BUILD) {
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                VM_SEQ_ID,
                POOL_NO,
                PROJECT_ID,
                CONTAINER_NAME,
                IMAGES,
                STATUS,
                CREATED_TIME,
                UPDATE_TIME,
                USER_ID
            ).values(
                pipelineId,
                vmSeqId,
                poolNo,
                projectId,
                containerName,
                image,
                status,
                LocalDateTime.now(),
                LocalDateTime.now(),
                userId
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        poolNo: Int,
        containerName: String,
        image: String,
        status: Int
    ) {
        with(TDevcloudBuild.T_DEVCLOUD_BUILD) {
            dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(IMAGES, image)
                .set(CONTAINER_NAME, containerName)
                .set(STATUS, status)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(POOL_NO.eq(poolNo))
                .execute()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String?,
        poolNo: Int?,
        status: Int
    ) {
        with(TDevcloudBuild.T_DEVCLOUD_BUILD) {
            logger.info("$pipelineId|$vmSeqId|$poolNo update status: $status")
            val sql = dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(STATUS, status)
                .where(PIPELINE_ID.eq(pipelineId))
                if (null != vmSeqId) {
                    sql.and(VM_SEQ_ID.eq(vmSeqId))
                }
                if (null != poolNo) {
                    sql.and(POOL_NO.eq(poolNo))
                }
            sql.execute()
        }
    }

    fun updateDebugStatus(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String?,
        containerName: String?,
        debugStatus: Boolean
    ) {
        with(TDevcloudBuild.T_DEVCLOUD_BUILD) {
            val sql = dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(DEBUG_TIME, LocalDateTime.now())
                .set(DEBUG_STATUS, debugStatus)
                .where(PIPELINE_ID.eq(pipelineId))
            if (null != vmSeqId) {
                sql.and(VM_SEQ_ID.eq(vmSeqId))
            }
            if (null != containerName) {
                sql.and(CONTAINER_NAME.eq(containerName))
            }
            sql.execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        poolNo: Int
    ): TDevcloudBuildRecord? {
        with(TDevcloudBuild.T_DEVCLOUD_BUILD) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(POOL_NO.eq(poolNo))
                .fetchAny()
        }
    }

    fun getContainerStatus(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        containerName: String
    ): TDevcloudBuildRecord? {
        with(TDevcloudBuild.T_DEVCLOUD_BUILD) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(CONTAINER_NAME.eq(containerName))
                .fetchAny()
        }
    }

    fun delete(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        poolNo: Int
    ): Int {
        return with(TDevcloudBuild.T_DEVCLOUD_BUILD) {
            dslContext.delete(this)
                    .where(PIPELINE_ID.eq(pipelineId))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .and(POOL_NO.eq(poolNo))
                    .execute()
        }
    }

    fun getTimeOutBusyContainer(dslContext: DSLContext): Result<TDevcloudBuildRecord> {
        with(TDevcloudBuild.T_DEVCLOUD_BUILD) {
            return dslContext.selectFrom(this)
                .where(timestampDiff(DatePart.DAY, UPDATE_TIME.cast(java.sql.Timestamp::class.java)).greaterOrEqual(7))
                .and(STATUS.eq(1))
                .limit(1000)
                .fetch()
        }
    }

    fun getNoUseIdleContainer(dslContext: DSLContext): Result<TDevcloudBuildRecord> {
        with(TDevcloudBuild.T_DEVCLOUD_BUILD) {
            return dslContext.selectFrom(this)
                    .where(timestampDiff(DatePart.DAY, UPDATE_TIME.cast(java.sql.Timestamp::class.java)).greaterOrEqual(7))
                    .and(STATUS.eq(0))
                    .fetch()
        }
    }

    fun getTimeoutBusyDebugContainer(dslContext: DSLContext): Result<TDevcloudBuildRecord> {
        with(TDevcloudBuild.T_DEVCLOUD_BUILD) {
            return dslContext.selectFrom(this)
                .where(STATUS.eq(0))
                .and(DEBUG_STATUS.eq(true))
                .and(timestampDiff(DatePart.HOUR, DEBUG_TIME.cast(java.sql.Timestamp::class.java)).greaterOrEqual(1))
                .fetch()
        }
    }

    fun timestampDiff(part: DatePart, t1: Field<Timestamp>): Field<Int> {
        return DSL.field("timestampdiff({0}, {1}, NOW())",
                Int::class.java, DSL.keyword(part.toSQL()), t1)
    }
}

/*

CREATE DATABASE `devops_dispatch_devcloud` /*!40100 DEFAULT CHARACTER SET utf8 */

DROP TABLE IF EXISTS `T_DEVCLOUD_BUILD`;
CREATE TABLE `T_DEVCLOUD_BUILD` (
    `PIPELINE_ID` varchar(34) NOT NULL,
    `VM_SEQ_ID` varchar(34) NOT NULL,
    `POOL_NO` int(11) NOT NULL,
    `PROJECT_ID` varchar(64) NOT NULL,
    `CONTAINER_NAME` varchar(128) NOT NULL,
    `IMAGES` varchar(1024) NOT NULL,
    `STATUS` int(11) NOT NULL,
    `CREATED_TIME` timestamp NULL DEFAULT NULL,
    `UPDATE_TIME` timestamp NULL DEFAULT NULL,
    `USER_ID` varchar(34) NOT NULL,
  PRIMARY KEY (`PIPELINE_ID`, `VM_SEQ_ID`, `POOL_NO`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='DevCloud流水线构建机信息';


*/
