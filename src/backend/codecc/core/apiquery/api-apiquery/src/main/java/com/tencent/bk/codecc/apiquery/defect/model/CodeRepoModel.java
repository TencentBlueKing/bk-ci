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

package com.tencent.bk.codecc.apiquery.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 代码仓库信息model
 *
 * @version V1.0
 * @date 2021/3/22
 */

@Data
public class CodeRepoModel {

    /**
     * 仓库ID
     */
    @JsonProperty("repo_id")
    private String repoId;

    /**
     * 仓库url
     */
    private String url;

    /**
     * 仓库版本
     */
    private String revision;

    /**
     * 仓库分支
     */
    private String branch;

    /**
     * 仓库别名
     */
    @JsonProperty("alias_name")
    private String aliasName;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CodeRepoModel) {
            CodeRepoModel codeRepo = (CodeRepoModel) obj;
            return (url.equals(codeRepo.url) && branch.equals(codeRepo.branch));
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

}
