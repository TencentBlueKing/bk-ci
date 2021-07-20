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

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.api.ServiceRepoResource;
import com.tencent.bk.codecc.defect.service.CodeRepoService;
import com.tencent.bk.codecc.defect.vo.CodeRepoVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 代码库信息接口实现
 *
 * @version V1.0
 * @date 2019/12/3
 */
@RestResource
public class ServiceRepoResourceImpl implements ServiceRepoResource {

    @Autowired
    private CodeRepoService codeRepoService;

    @Override
    public Result<Set<CodeRepoVO>> getCodeRepoByTaskIdAndBuildId(Long taskId, String buildId) {
        return new Result<>(codeRepoService.getCodeRepoInfoByTaskId(taskId, buildId));
    }

    @Override public Result<Map<Long, Set<CodeRepoVO>>> getCodeRepoByTaskIds(Collection<Long> taskIds) {
        return new Result<>(codeRepoService.getCodeRepoListByTaskIds(Sets.newHashSet(taskIds)));
    }

}
