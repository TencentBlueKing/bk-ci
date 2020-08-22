package com.tencent.devops.common.api.checkerset;

import lombok.Data;

/**
 * 描述
 *
 * @version V1.0
 * @date 2020/1/10
 */
@Data
public class AuthManagementPermissionReqVO
{
    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 用户名
     */
    private String user;

    /**
     * 规则集ID
     */
    private String checkerSetId;
}
