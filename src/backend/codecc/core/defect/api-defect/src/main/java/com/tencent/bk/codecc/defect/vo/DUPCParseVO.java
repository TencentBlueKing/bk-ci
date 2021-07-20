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
public class DUPCParseVO
{

    @JsonProperty("dup_line_count")
    private Long dupLineCount;

    @JsonProperty("total_line_count")
    private Long totalLineCount;

    @JsonProperty("dupc_files")
    private List<DUPCFileParseVO> dupcFiles;

}
