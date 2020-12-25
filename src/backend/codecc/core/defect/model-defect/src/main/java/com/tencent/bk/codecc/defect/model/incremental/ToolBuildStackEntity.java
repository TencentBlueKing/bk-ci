package com.tencent.bk.codecc.defect.model.incremental;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 工具构建运行时栈表，每次启动分析是生成
 *
 * @version V3.0
 * @date 2020/05/18
 */
@Data
@Document(collection = "t_tool_build_stack")
@CompoundIndexes({
        @CompoundIndex(name = "taskid_toolname_buildid_idx", def = "{'task_id': 1, 'tool_name': 1, 'build_id': 1}")
})
public class ToolBuildStackEntity extends CommonEntity
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
     * 告警快照基准构建ID
     */
    @Field("base_build_id")
    private String baseBuildId;

    /**
     * 是否全量扫描， true：全量扫描 false：增量
     */
    @Field("full_scan")
    private boolean fullScan;

    /**
     * 删除文件列表
     */
    @Field("delete_files")
    private List<String> deleteFiles;
}
