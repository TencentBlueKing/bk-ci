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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * CLOC信息实体类
 *
 * @version V1.0
 * @date 2019/9/29
 */
@Data
@Document(collection = "t_cloc_defect")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_tool_name_1_file_name_1",
                def = "{'task_id': 1, 'tool_name': 1, 'file_name': 1}", background = true),
        @CompoundIndex(name = "task_id_1_tool_name_1_status_1",
                def = "{'task_id': 1, 'tool_name': 1, 'status': 1}", background = true)
})
public class CLOCDefectEntity extends CommonEntity
{
    @Field("task_id")
    private Long taskId;

    @Field("stream_name")
    private String streamName;

    @Field("file_name")
    @JsonProperty("filePath")
    private String fileName;

    @Field("rel_path")
    private String relPath;

    @Field("tool_name")
    private String toolName;

    @Field("blank")
    private Long blank;

    @Field("code")
    private Long code;

    @Field("comment")
    private Long comment;

    @Field("efficient_comment")
    private Long efficientComment;

    @Field("language")
    private String language;

    @Field("status")
    private String status;

    @Transient
    private Long lines;
}
