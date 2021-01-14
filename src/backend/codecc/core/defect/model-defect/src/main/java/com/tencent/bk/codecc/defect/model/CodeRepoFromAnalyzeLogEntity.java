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
 
package com.tencent.bk.codecc.defect.model;

import lombok.Data;
import org.json.JSONObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工具侧上报的分析记录中代码仓库url
 * 
 * @date 2019/10/25
 * @version V1.0
 */
@Data
@Document(collection = "t_code_repo_from_analyzelog")
@CompoundIndexes({
        @CompoundIndex(name = "taskid_buildId_idx", def = "{'task_id': 1, 'build_id': 1}")
})
public class CodeRepoFromAnalyzeLogEntity
{
    @Id
    private String entityId;

    /**
     * 任务ID
     */
    @Field("task_id")
    private long taskId;

    /**
     * 代码仓库
     */
    @Field("code_repo_list")
    private Set<CodeRepo> codeRepoList;


    @Data
    public static class CodeRepo
    {
        private String buildId;

        private String url;

        private String branch;

        private String version;

        private Long createDate;

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof CodeRepo) {
                CodeRepo codeRepo = (CodeRepo) obj;
                return (url.equals(codeRepo.url) && branch.equals(codeRepo.branch));
            }
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            return url.hashCode();
        }
    }
}
