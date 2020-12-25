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

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 代码仓库url
 * 
 * @date 2019/10/25
 * @version V1.0
 */
@Data
@Document(collection = "t_code_file_url")
@CompoundIndexes({
        @CompoundIndex(name = "taskid_tool_idx", def = "{'task_id': 1, 'file_path': 1}")
})
public class CodeFileUrlEntity extends CommonEntity
{
    /**
     * 任务ID
     */
    @Field("task_id")
    private long taskId;

    /**
     * 文件路径
     */
    @Field("file_path")
    private String file;

    /**
     * 文件的代码仓库完整路径
     */
    @Field("url")
    private String url;

    /**
     * 文件相对路径
     */
    @Field("file_rel_path")
    private String fileRelPath;
    /**
     * 版本号
     */
    @Field("version")
    private String version;

    /**
     * 代码库类型，svn或git
     */
    @Field("scm_type")
    private String scmType;
}
