package com.tencent.bk.codecc.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 文件圈复杂度列表
 *
 * @version V1.0
 * @date 2020/2/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_file_ccn")
@CompoundIndexes({
        @CompoundIndex(name = "taskid_filerelpath_idx", def = "{'task_id': 1, 'file_rel_path': 1}", background = true)
})
public class FileCCNEntity extends CommonEntity
{
    /**
     * 任务ID
     */
    @Field("task_id")
    private Long taskId;

    /**
     * 文件路径
     */
    @Field("file_path")
    @JsonProperty("file_path")
    private String filePath;

    /**
     * 文件相对路径
     */
    @Field("file_rel_path")
    @JsonProperty("file_rel_path")
    private String fileRelPath;

    /**
     * 文件所有函数圈复杂度之和
     */
    @Field("total_ccn_count")
    @JsonProperty("total_ccn_count")
    private String totalCCNCount;
}
