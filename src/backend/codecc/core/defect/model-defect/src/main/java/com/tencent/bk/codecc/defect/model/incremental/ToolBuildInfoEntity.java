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
 * 工具构建信息实体类
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "t_tool_build_info")
@CompoundIndexes({
        @CompoundIndex(name = "taskid_toolname_idx", def = "{'task_id': 1, 'tool_name': 1}")
})
public class ToolBuildInfoEntity extends CommonEntity
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
     * 删除文件列表
     */
    @Deprecated
    @Field("delete_files")
    private List<String> deleteFiles;

    /**
     * 强制全量扫描标志 Y：强制全量扫描 N：按任务配置扫描
     */
    @Field("force_full_scan")
    private String forceFullScan;

    /**
     * 告警快照基准构建ID
     */
    @Field("defect_base_build_id")
    private String defectBaseBuildId;

    /**
     * 告警快照基准构建号
     */
    @Deprecated
    @Field("defect_bas_build_num")
    private String defectBaseBuildNum;
}
