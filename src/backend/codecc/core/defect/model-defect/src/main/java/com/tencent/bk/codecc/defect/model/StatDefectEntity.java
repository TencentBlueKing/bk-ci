package com.tencent.bk.codecc.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_stat_defect")
@NoArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_tool_name_1_status_1", def = "{'task_id': 1, 'tool_name': 1, 'status': 1}", background = true)
})
public class StatDefectEntity extends org.bson.Document {

    @Field("task_id")
    @JsonProperty("task_id")
    private long taskId;
    @Field("tool_name")
    @JsonProperty("tool_name")
    private String toolName;
    @Field("status")
    @JsonProperty("status")
    private String status;

    public StatDefectEntity(long taskId, String toolName) {
        this.taskId = taskId;
        this.toolName = toolName;
        this.status = "ENABLED";
        this.append("task_id", taskId);
        this.append("tool_name", toolName);
        this.append("status", status);
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.append("status", status);
    }
}