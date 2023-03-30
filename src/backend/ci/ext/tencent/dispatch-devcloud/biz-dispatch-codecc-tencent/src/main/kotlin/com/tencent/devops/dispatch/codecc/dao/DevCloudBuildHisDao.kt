package com.tencent.devops.dispatch.codecc.dao

import com.tencent.devops.model.dispatch.codecc.tables.TDevcloudBuildHis
import com.tencent.devops.model.dispatch.codecc.tables.records.TDevcloudBuildHisRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DevCloudBuildHisDao {

    fun create(
        dslContext: DSLContext,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        secretKey: String,
        codeccTaskId: Long = -101
    ): Long {
        with(TDevcloudBuildHis.T_DEVCLOUD_BUILD_HIS) {
            return dslContext.insertInto(
                this,
                PIPELINE_ID,
                BUIDLD_ID,
                VM_SEQ_ID,
                SECRET_KEY,
                CODECC_TASK_ID,
                GMT_CREATE,
                GMT_MODIFIED
            ).values(
                pipelineId,
                buildId,
                vmSeqId,
                secretKey,
                codeccTaskId,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).returning(ID).fetchOne()!!.id
        }
    }

    fun get(
        dslContext: DSLContext,
        buildId: String
    ): TDevcloudBuildHisRecord? {
        with(TDevcloudBuildHis.T_DEVCLOUD_BUILD_HIS) {
            return dslContext.selectFrom(this)
                .where(BUIDLD_ID.eq(buildId))
                .fetchAny()
        }
    }

    fun getLatestBuildHistory(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String
    ): TDevcloudBuildHisRecord? {
        with(TDevcloudBuildHis.T_DEVCLOUD_BUILD_HIS) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .orderBy(GMT_CREATE.desc())
                .fetchAny()
        }
    }

    fun updateContainerName(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String,
        containerName: String
    ) {
        with(TDevcloudBuildHis.T_DEVCLOUD_BUILD_HIS) {
            dslContext.update(this)
                .set(CONTAINER_NAME, containerName)
                .where(BUIDLD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .execute()
        }
    }
}

/*
CREATE TABLE `T_DEVCLOUD_BUILD_HIS` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `PIPELINE_ID` varchar(64) NOT NULL DEFAULT '' COMMENT 'pipeline id',
    `BUIDLD_ID` varchar(64) NOT NULL DEFAULT '' COMMENT 'build id',
    `VM_SEQ_ID` varchar(64) NOT NULL DEFAULT '' COMMENT 'vm seq id',
    `CONTAINER_NAME` varchar(128) DEFAULT '' COMMENT '容器名称',
    `SECRET_KEY` varchar(128) DEFAULT '' COMMENT '随机密钥',
    `GMT_CREATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `GMT_MODIFIED` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`ID`),
    INDEX `idx_1` (`BUIDLD_ID`, `PIPELINE_ID`, `VM_SEQ_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='devcloud 构建历史记录';*/
