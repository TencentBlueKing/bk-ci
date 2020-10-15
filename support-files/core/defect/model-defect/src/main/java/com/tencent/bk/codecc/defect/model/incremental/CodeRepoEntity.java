package com.tencent.bk.codecc.defect.model.incremental;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 代码仓库信息实体类
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Data
public class CodeRepoEntity
{
    /**
     * 仓库ID
     */
    @Field("repo_id")
    private String repoId;

    /**
     * 仓库url
     */
    private String url;

    /**
     * 仓库版本
     */
    private String revision;

    /**
     * 仓库分支
     */
    private String branch;

    /**
     * 仓库别名
     */
    @Field("alias_name")
    private String aliasName;
}
