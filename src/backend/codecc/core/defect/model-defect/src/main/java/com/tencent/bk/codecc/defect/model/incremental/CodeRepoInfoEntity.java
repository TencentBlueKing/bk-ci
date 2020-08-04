package com.tencent.bk.codecc.defect.model.incremental;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 代码仓库信息实体类
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_code_repo_info")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_build_id_1", def = "{'task_id': 1, 'build_id': 1}")
})
public class CodeRepoInfoEntity extends CommonEntity
{
    /**
     * 任务ID
     */
    @Field("task_id")
    private Long taskId;

    /**
     * 蓝盾构建ID
     */
    @Field("build_id")
    private String buildId;

    /**
     * 仓库列表
     */
    @Field("repo_list")
    private List<CodeRepoEntity> repoList;

    /**
     * 扫描目录白名单
     */
    @Field("repo_white_list")
    private List<String> repoWhiteList;

    /**
     * 临时存储待删除文件列表
     */
    @Deprecated
    @Field("temp_delete_files")
    private List<String> tempDeleteFiles;
}
