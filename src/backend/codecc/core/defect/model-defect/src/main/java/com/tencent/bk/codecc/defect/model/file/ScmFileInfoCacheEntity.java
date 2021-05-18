package com.tencent.bk.codecc.defect.model.file;

import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@Document(collection = "t_scm_file_info_cache")
@CompoundIndexes({
    @CompoundIndex(name = "task_tool_file_indx", def = "{'task_id': 1, 'tool_name': 1, 'file_rel_path': 1}", background = true)
})
public class ScmFileInfoCacheEntity
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
     * 文件路径
     */
    @Field("file_path")
    private String filePath;

    @Field("file_rel_path")
    private String fileRelPath;

    /**
     * 文件md5
     */
    @Field("file_md5")
    private String md5;

    /**
     * 上报时间
     */
    @Field("file_update_time")
    private long fileUpdateTime;

    private String branch;

    private String revision;

    private String url;

    @Field("scm_type")
    private String scmType;

    @Field("build_id")
    private String buildId;

    @Field("change_records")
    private List<ScmBlameChangeRecordVO> changeRecords;

    @Data
    public static class ScmBlameChangeRecordVO
    {
        private String author;

        private List<Object> lines;

        @Field("line_update_time")
        private long lineUpdateTime;
    }
}
