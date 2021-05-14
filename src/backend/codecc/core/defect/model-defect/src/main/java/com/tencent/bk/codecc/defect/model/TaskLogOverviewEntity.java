package com.tencent.bk.codecc.defect.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 插件实际执行扫描工具记录的实体类
 *
 * @version V1.0
 * @date 2020/11/02
 */
@Data
@NoArgsConstructor
@Document(collection = "t_task_log_overview")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_build_id", def = "{'task_id': 1, 'build_id': 1}", background = true),
        @CompoundIndex(name = "task_id_build_num", def = "{'task_id': 1, 'build_num': 1}", background = true),
        @CompoundIndex(name = "task_id_status_start_time", def = "{'task_id': 1, 'status': 1, 'start_time': 1}",
                background = true),
        @CompoundIndex(name = "task_id_start_time", def = "{'task_id': 1, 'start_time': 1}", background = true)
})
public class TaskLogOverviewEntity {

    public TaskLogOverviewEntity(
            String entityId,
            Long taskId,
            String buildId,
            Integer status,
            Long startTime,
            List<TaskLogEntity> taskLogEntityList) {
        this.entityId = entityId;
        this.taskId = taskId;
        this.buildId = buildId;
        this.status = status;
        this.startTime = startTime;
        this.taskLogEntityList = taskLogEntityList;
    }

    @Id
    private String entityId;

    @Field("task_id")
    private Long taskId;

    @Field("build_id")
    private String buildId;

    @Field("build_num")
    private String buildNum;

    @Field("status")
    private Integer status;

    @Field("start_time")
    private Long startTime;

    @Field("end_time")
    private Long endTime;

    @DBRef
    @Field("task_log_list")
    private List<TaskLogEntity> taskLogEntityList;

    @Field("tool_list")
    private List<String> toolList;
}
