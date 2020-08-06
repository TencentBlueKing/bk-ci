/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.UserRepoRestResource;
import com.tencent.bk.codecc.defect.service.PipelineService;
import com.tencent.bk.codecc.defect.vo.coderepository.CodeRepoVO;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;

/**
 * 代码库前端接口实现
 * 
 * @date 2019/12/26
 * @version V1.0
 */
@RestResource
public class UserRepoRestResourceImpl implements UserRepoRestResource
{
    @Autowired
    private PipelineService pipelineService;

    @Override
    public CodeCCResult<Map<Long, Set<CodeRepoVO>>> getCodeRepoListByTaskIds(Set<Long> taskIds, String projectId)
    {
        return new CodeCCResult<>(pipelineService.getCodeRepoListByTaskIds(taskIds, projectId));
    }
}
