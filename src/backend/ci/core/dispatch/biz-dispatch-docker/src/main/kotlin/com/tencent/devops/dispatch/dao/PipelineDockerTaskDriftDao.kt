package com.tencent.devops.dispatch.dao

import com.tencent.devops.model.dispatch.tables.TDispatchPipelineDockerTaskDrift
import com.tencent.devops.model.dispatch.tables.TDispatchPipelineDockerTaskSimple
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerTaskDriftRecord
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerTaskSimpleRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineDockerTaskDriftDao @Autowired constructor() {
    fun create(
        dslContext: DSLContext,
        pipelineId: String,
        buildId: String,
        vmSeq: String,
        oldIdcIp: String,
        newIdcIp: String
    ) {
        with(TDispatchPipelineDockerTaskDrift.T_DISPATCH_PIPELINE_DOCKER_TASK_DRIFT) {
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                BUILD_ID,
                VM_SEQ,
                OLD_DOCKER_IP,
                NEW_DOCKER_IP,
                GMT_CREATE,
                GMT_MODIFIED
            ).values(
                pipelineId,
                buildId,
                vmSeq,
                oldIdcIp,
                newIdcIp,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).execute()
        }
    }

    fun getByPipelineIdAndVMSeq(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeq: String
    ): Result<TDispatchPipelineDockerTaskDriftRecord> {
        with(TDispatchPipelineDockerTaskDrift.T_DISPATCH_PIPELINE_DOCKER_TASK_DRIFT) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ.eq(vmSeq))
                .fetch()
        }
    }
}

/*
CREATE TABLE `T_DISPATCH_PIPELINE_DOCKER_TASK_DRIFT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `PIPELINE_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '流水线ID',
  `BUILD_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '构建ID',
  `VM_SEQ` varchar(64) NOT NULL DEFAULT '' COMMENT '构建机序号',
  `OLD_DOCKER_IP` varchar(64) NOT NULL DEFAULT '' COMMENT '旧构建容器IP',
  `NEW_DOCKER_IP` varchar(64) NOT NULL DEFAULT '' COMMENT '新构建容器IP',
  `GMT_CREATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `GMT_MODIFIED` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNI_BUILD_SEQ` (`PIPELINE_ID`,`VM_SEQ`),
  INDEX `IDX_P_B`(`PIPELINE_ID`, `BUILD_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='DOCKER构建任务漂移记录表';*/
