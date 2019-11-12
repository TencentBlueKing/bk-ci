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
import com.tencent.bk.codecc.task.vo.UserVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 用户管理逻辑处理实现类
 *
 * @version V1.0
 * @date 2019/4/25
 */
@Service
public class UserManageServiceImpl implements UserManageService
{
    private static Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Autowired
    private Client client;

    @Override
    public Result<UserVO> getInfo()
    {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String userId = request.getHeader(AUTH_HEADER_DEVOPS_USER_ID);
        String bkToken = request.getHeader(AUTH_HEADER_DEVOPS_USER_ID);
        UserVO userVO = new UserVO();
        userVO.setUsername(userId);
        userVO.setAuthenticated(true);
        userVO.setBkToken(bkToken);
        return new Result<>(userVO);
    }

    /*@Override
    public List<DevopsProjectVO> getProjectList(String userId, String accessToken)
    {
        com.tencent.devops.project.pojo.Result<List<ProjectVO>> projectResult =
                client.get(ServiceProjectResource.class).list(userId);
        if (projectResult.isNotOk() || null == projectResult.getData())
        {
            logger.error("get project list fail!");
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
    }*/


}
