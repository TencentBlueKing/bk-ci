package com.tencent.bk.codecc.defect.vo.customtool;

import com.tencent.devops.common.api.CommonVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 代码仓库信息实体类
 *
 * @version V4.0
 * @date 2019/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScmInfoVO extends CommonVO
{
    /**
     * 任务ID
     */
    private long taskId;

    /**
     * 最新构建ID
     */
    private String buildId;

    /**
     * 文件最新修改作者
     */
    private String fileUpdateAuthor;

    /**
     * 文件最新修改时间
     */
    private long fileUpdateTime;

    /**
     * 最新版本号
     */
    private String revision;

    /**
     * 仓库URL
     */
    private String url;

    /**
     * 仓库ID
     */
    private String repoId;

    /**
     * 分支
     */
    private String branch;


    /**
     * 根url
     */
    private String rootUrl;


    /**
     * 代码库类型
     */
    private String scmType;

    /**
     * 子模块列表
     */
    private List<RepoSubModuleVO> subModules;
}
