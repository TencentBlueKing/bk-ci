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

package com.tencent.bk.codecc.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 配置存储信息实体类
 *
 * @version V1.0
 * @date 2019/9/26
 */
@Data
public class ConfigStorageInfoEntity
{
    @JsonProperty("limit_lfs_file_size")
    @Field("limit_lfs_file_size")
    private Integer limitLfsFileSize;

    @JsonProperty("limit_size")
    @Field("limit_size")
    private Integer limitSize;

    @JsonProperty("limit_file_size")
    @Field("limit_file_size")
    private Integer limitFileSize;

    @JsonProperty("limit_lfs_size")
    @Field("limit_lfs_size")
    private Integer limitLfsSize;
}
