package com.tencent.bk.codecc.defect.model.incremental;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 待清除临时文件信息的构建实体类
 *
 * @version V1.0
 * @date 2019/12/17
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "t_need_clear_temp_file_build")
public class NeedClearTempFileBuildEntity
{
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
     * 构建号
     */
    @Field("build_num")
    private String buildNum;
}
