package com.tencent.bk.codecc.defect.model.incremental;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 代码仓库信息实体类
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Data
public class CodeRepoEntity extends CommonEntity
{
    /**
     * 仓库ID
     */
    @Field("repo_id")
    private String repoId;

    /**
     * 仓库版本
     */
    @Field("revision")
    private String revision;

    /**
     * 仓库分支
     */
    @Field("branch")
    private String branch;
}
