package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Set;

/**
 * 描述
 *
 * @version V1.0
 * @date 2019/12/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_build_defect")
@CompoundIndexes({
        @CompoundIndex(name = "taskid_toolname_buildid_filepath_idx", def = "{'task_id': 1, 'tool_name': 1, 'build_id': 1, 'file_path': 1}", background = true),
        @CompoundIndex(name = "taskid_toolname_buildid_defectid_idx", def = "{'task_id': 1, 'tool_name': 1, 'build_id': 1, 'defect_id': 1}", background = true)
})
public class BuildDefectEntity extends CommonEntity
{
    /**
     * 工具名称
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

    /**
     * 文件相对路径
     */
    @Field("file_rel_path")
    private String fileRelPath;

    /**
     * 文件路径
     */
    @Field("file_path")
    private String filePath;

    /**
     * Coverity、Klocwork、Pinpoint、CCN工具快照字段，告警唯一ID
     */
    @Field("defect_id")
    private String defectId;

    /**
     * Lint类工具快照字段----文件包含的告警唯一ID列表
     */
    @Field("file_defect_ids")
    private Set<String> fileDefectIds;
}
