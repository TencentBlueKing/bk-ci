package com.tencent.bk.codecc.defect.model;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_stat_statistic")
@NoArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_build_id_1_tool_name_1",
                def = "{'task_id': 1, 'build_id': 1, 'tool_name': 1}")
})
public class StatStatisticEntity extends org.bson.Document {

    @Id
    private String entityId;
    /**
     * 任务ID
     */
    @Field("task_id")
    private Long taskId;
    /**
     * 工具名称
     */
    @Field("tool_name")
    private String toolName;
    /**
     * 构建ID
     */
    @Field("build_id")
    private String buildId;
    /**
     * 统计的时间
     */
    @Field("time")
    private long time;

    public StatStatisticEntity(long taskId, String toolName, String buildId) {
        this.taskId = taskId;
        this.buildId = buildId;
        this.toolName = toolName;
        ObjectId id = ObjectId.get();
        this.entityId = id.toString();
        this.time = System.currentTimeMillis();
        this.append("_id", entityId);
        this.append("task_id", taskId);
        this.append("tool_name", toolName);
        this.append("build_id", buildId);
        this.append("time", time);
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
        this.append("_id", entityId);
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
        this.append("task_id", taskId);
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
        this.append("tool_name", toolName);
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
        this.append("build_id", buildId);
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
        this.append("time", time);
    }
}
