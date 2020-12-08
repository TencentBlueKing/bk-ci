package com.tencent.devops.common.api.checkerset;

import lombok.Data;

/**
 * 规则集管理请求体视图
 *
 * @version V1.0
 * @date 2020/1/8
 */
@Data
public class CheckerSetManagementReqVO
{
    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 规则集可见范围1：公开；2：仅我的项目；
     */
    private Integer scope;

    /**
     * 是否是默认规则集
     */
    private Boolean defaultCheckerSet;

    /**
     * 删除规则集
     */
    private Boolean deleteCheckerSet;

    /**
     * 卸载规则集
     */
    private Boolean uninstallCheckerSet;

    /**
     * 切换版本
     */
    private Integer versionSwitchTo;

    /**
     * 不再使用该规则集的任务
     */
    private Long discardFromTask;
}
