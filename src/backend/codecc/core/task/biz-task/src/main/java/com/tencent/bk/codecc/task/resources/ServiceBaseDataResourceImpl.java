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

package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.bk.codecc.task.service.BaseDataService;
import com.tencent.bk.codecc.task.service.PipelineService;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.bk.codecc.task.vo.RepoInfoVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基础数据服务接口
 *
 * @version V1.0
 * @date 2019/5/28
 */
@RestResource
public class ServiceBaseDataResourceImpl implements ServiceBaseDataResource
{

    @Autowired
    private BaseDataService baseDataService;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private AuthExPermissionApi authExPermissionApi;


    @Override
    public Result<List<BaseDataVO>> getInfoByTypeAndCode(String paramType, String paramCode)
    {
        return new Result<>(baseDataService.findBaseDataInfoByTypeAndCode(paramType, paramCode));
    }

    @Override
    public Result<Map<String, RepoInfoVO>> getRepoUrlByProjects(Set<String> bkProjectIds)
    {
        return new Result<>(pipelineService.getRepoUrlByBkProjects(bkProjectIds));
    }

    @Override
    public Result<List<BaseDataVO>> getParamsByType(String paramType)
    {
        return new Result<>(baseDataService.findBaseDataInfoByType(paramType));
    }

    @Override
    public Result<Integer> batchSave(String uerId, List<BaseDataVO> baseDataVOList) {
        return new Result<>(baseDataService.batchSave(uerId, baseDataVOList));
    }

    @Override
    public Result<Integer> deleteById(String id) {
        return new Result<>(baseDataService.deleteById(id));
    }

    @Override
    public Result<Boolean> updateExcludeUserMember(String userName, BaseDataVO baseDataVO) {
        // 判断是否为管理员
        if (!authExPermissionApi.isAdminMember(userName)) {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER, new String[]{"admin member"});
        }
        return new Result<>(baseDataService.updateExcludeUserMember(baseDataVO, userName));
    }


    @Override
    public Result<List<String>> queryExcludeUserMember() {
        return new Result<>(baseDataService.queryMemberListByParamType(ComConstants.KEY_EXCLUDE_USER_LIST));
    }


    @Override
    public Result<Boolean> updateAdminMember(String userName, BaseDataVO baseDataVO) {
        // 判断是否为管理员
        if (!authExPermissionApi.isAdminMember(userName)) {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER, new String[]{"admin member"});
        }
        return new Result<>(baseDataService.updateAdminMember(baseDataVO, userName));
    }


    @Override
    public Result<List<String>> queryAdminMember() {
        return new Result<>(baseDataService.queryMemberListByParamType(ComConstants.KEY_ADMIN_MEMBER));
    }

    @Override
    public Result<List<BaseDataVO>> findBaseData() {
        return new Result<>(baseDataService.findBaseData());
    }
}
