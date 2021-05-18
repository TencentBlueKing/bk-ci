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

package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.CodeRepoFromAnalyzeLogRepository;
import com.tencent.bk.codecc.defect.model.CodeRepoFromAnalyzeLogEntity;
import com.tencent.bk.codecc.defect.service.CodeRepoService;
import com.tencent.bk.codecc.defect.vo.CodeRepoVO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 代码库服务逻辑实现
 *
 * @version V1.0
 * @date 2019/12/3
 */
@Service
public class CodeRepoServiceImpl implements CodeRepoService {
    @Autowired
    private CodeRepoFromAnalyzeLogRepository codeRepoFromAnalyzeLogRepository;

    @Override
    public Set<CodeRepoVO> getCodeRepoInfoByTaskId(Long taskId, String buildId) {
        CodeRepoFromAnalyzeLogEntity codeRepoFromAnalyzeLogEntity;
        codeRepoFromAnalyzeLogEntity = codeRepoFromAnalyzeLogRepository.findCodeRepoFromAnalyzeLogEntityByTaskId(taskId);
        if (null == codeRepoFromAnalyzeLogEntity) {
            return new HashSet<>();
        }
        Set<CodeRepoFromAnalyzeLogEntity.CodeRepo> codeRepoList = codeRepoFromAnalyzeLogEntity.getCodeRepoList();

        Iterator<CodeRepoFromAnalyzeLogEntity.CodeRepo> it = codeRepoList.iterator();
        //取最后一个代码库地址
        if (CollectionUtils.isEmpty(codeRepoList)) {
            return new HashSet<>();
        }
        CodeRepoFromAnalyzeLogEntity.CodeRepo codeRepo = codeRepoList.stream().max(Comparator.comparing(
                codeRepo1 -> null == codeRepo1.getCreateDate() ? 0L : codeRepo1.getCreateDate())).get();
        CodeRepoVO codeRepoVO = new CodeRepoVO();
        codeRepoVO.setUrl(codeRepo.getUrl());
        codeRepoVO.setBranch(codeRepo.getBranch());
        codeRepoVO.setVersion(codeRepo.getVersion());
        codeRepoVO.setToolNames(new HashSet<>());
        Set<CodeRepoVO> codeRepos;
        codeRepos = new HashSet<CodeRepoVO>() {{
            add(codeRepoVO);
        }};
        return codeRepos;

    }

    @Override
    public Map<Long, Set<CodeRepoVO>> getCodeRepoListByTaskIds(Set<Long> taskIds) {
        //根据任务id集查询代码仓库信息
        Set<CodeRepoFromAnalyzeLogEntity> codeRepoFromAnalyzeLogEntitySet = codeRepoFromAnalyzeLogRepository.findByTaskIdIn(taskIds);

        if (CollectionUtils.isNotEmpty(codeRepoFromAnalyzeLogEntitySet)) {
            return codeRepoFromAnalyzeLogEntitySet.stream().collect(Collectors.toMap(CodeRepoFromAnalyzeLogEntity::getTaskId,
                    codeRepoFromAnalyzeLogEntity -> codeRepoFromAnalyzeLogEntity.getCodeRepoList().stream().map(codeRepo -> {
                        CodeRepoVO codeRepoVO = new CodeRepoVO();
                        codeRepoVO.setUrl(codeRepo.getUrl());
                        codeRepoVO.setBranch(codeRepo.getBranch());
                        codeRepoVO.setVersion(codeRepo.getVersion());
                        codeRepoVO.setToolNames(new HashSet<>());
                        return codeRepoVO;
                    }).collect(Collectors.toSet()),
                    (k, v) -> v
            ));
        } else {
            return new HashMap<>();
        }
    }


}
