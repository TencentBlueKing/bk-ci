package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.UserTaskPermissionRestResource;
import com.tencent.bk.codecc.task.vo.perm.MgrResourceResult;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.auth.api.external.PermissionService;
import com.tencent.devops.common.auth.api.pojo.external.response.AuthMgrResourceResponse;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

@RestResource
public class UserTaskPermissionRestResourceImpl implements UserTaskPermissionRestResource {

    @Autowired
    private PermissionService permissionService;

    @Override
    public CodeCCResult<MgrResourceResult> getMgrResource(String projectId, String resourceTypeCode, String resourceCode) {
        AuthMgrResourceResponse authResult = permissionService.getMgrResource(projectId, resourceTypeCode, resourceCode);
        MgrResourceResult result = new MgrResourceResult();
        BeanUtils.copyProperties(authResult, result);
        return new CodeCCResult<>(result);
    }
}
