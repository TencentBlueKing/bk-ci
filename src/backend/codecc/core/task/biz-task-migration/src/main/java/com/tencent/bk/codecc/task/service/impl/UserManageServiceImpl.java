/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.service.UserManageService;
import com.tencent.bk.codecc.task.vo.DevopsProjectOrgVO;
import com.tencent.bk.codecc.task.vo.DevopsProjectVO;
import com.tencent.bk.codecc.task.vo.UserVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.project.api.service.ServiceProjectResource;
import com.tencent.devops.project.pojo.ProjectVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_ACCESS_TOKEN;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 用户管理逻辑处理实现类
 *
 * @version V1.0
 * @date 2019/4/25
 */
@Service
@Slf4j
public class UserManageServiceImpl implements UserManageService
{
    @Autowired
    private Client client;

    @Override
    public Result<UserVO> getInfo(String userId)
    {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String bkToken = request.getHeader(AUTH_HEADER_DEVOPS_ACCESS_TOKEN);
        UserVO userVO = new UserVO();
        userVO.setUsername(userId);
        userVO.setAuthenticated(true);
        userVO.setBkToken(bkToken);
        return new Result<>(userVO);
    }

    @Override
    public List<DevopsProjectVO> getProjectList(String userId, String accessToken)
    {
        com.tencent.devops.project.pojo.Result<List<ProjectVO>> projectResult =
                client.getDevopsService(ServiceProjectResource.class).list(accessToken);
        if (projectResult.isNotOk() || null == projectResult.getData())
        {
            log.error("get project list fail!");
            throw new CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR);
        }
        List<ProjectVO> projectVOList = projectResult.getData();
        return projectVOList.stream().
                map(projectVO ->
                        new DevopsProjectVO(
                                projectVO.getProjectId(),
                                projectVO.getProjectName(),
                                projectVO.getProjectCode(),
                                projectVO.getProjectType()
                        )
                ).
                collect(Collectors.toList());
    }

    @Override
    public DevopsProjectOrgVO getDevopsProjectOrg(String projectId)
    {
        DevopsProjectOrgVO projectOrgVO = new DevopsProjectOrgVO();
        com.tencent.devops.project.pojo.Result<ProjectVO> projectResult =
                client.getDevopsService(ServiceProjectResource.class).get(projectId);
        if (projectResult.isNotOk() || projectResult.getData() == null)
        {
            log.error("getDevopsProject fail! [{}]", projectId);
            return projectOrgVO;
        }

        ProjectVO projectVO = projectResult.getData();
        String bgId = projectVO.getBgId();
        String deptId = projectVO.getDeptId();
        String centerId = projectVO.getCenterId();

        if (StringUtils.isBlank(bgId))
        {
            log.error("getDevopsProject bgId is empty: [{}]", projectId);
            return projectOrgVO;
        }

        projectOrgVO.setBgId(Integer.parseInt(bgId));
        projectOrgVO.setDeptId(Integer.parseInt(StringUtils.isBlank(deptId) ? "0" : deptId));
        projectOrgVO.setCenterId(Integer.parseInt(StringUtils.isBlank(centerId) ? "0" : centerId));

        return projectOrgVO;
    }


}
