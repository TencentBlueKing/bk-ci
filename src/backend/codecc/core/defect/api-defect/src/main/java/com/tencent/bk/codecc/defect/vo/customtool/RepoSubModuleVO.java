package com.tencent.bk.codecc.defect.vo.customtool;

import lombok.Data;

/**
 * 描述
 *
 * @version V1.0
 * @date 2020/3/26
 */
@Data
public class RepoSubModuleVO
{
    /**
     * 子模块名称
     */
    private String subModule;

    /**
     * 子模块URL
     */
    private String url;

    /**
     * 仓库ID
     */
    private String repoId;
}
