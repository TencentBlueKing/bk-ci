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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 单个代码评论视图
 * 
 * @date 2020/3/2
 * @version V1.0
 */
@Data
@ApiModel("单个代码评论视图")
public class SingleCommentVO
{
    @ApiModelProperty("评论id")
    private String singleCommentId;

    @ApiModelProperty("用户名")
    private String userName;

    @ApiModelProperty("评论")
    private String comment;

    @ApiModelProperty("评论时间")
    private Long commentTime;

}
