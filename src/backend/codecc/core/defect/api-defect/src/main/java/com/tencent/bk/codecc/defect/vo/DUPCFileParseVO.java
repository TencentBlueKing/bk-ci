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
 
package com.tencent.bk.codecc.defect.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 重复率文件解析视图
 * 
 * @date 2020/2/12
 * @version V1.0
 */
@Data
public class DUPCFileParseVO 
{

    @JsonProperty("block_num")
    private Integer blockNum;

    @JsonProperty("dup_lines")
    private Long dupLines;

    @JsonProperty("dup_rate")
    private String dupRate;

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("total_lines")
    private Long totalLines;

    @JsonProperty("block_list")
    private List<DUPCBlockParseVO> blockList;
}
