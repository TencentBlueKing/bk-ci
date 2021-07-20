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
 
package com.tencent.bk.codecc.task.pojo;

import com.tencent.bk.codecc.task.enums.EmailType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮件通知数据模型
 * 
 * @date 2019/11/19
 * @version V1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailNotifyModel 
{
    /**
     * 任务id
     */
    private Long taskId;

    /**
     * 构建id
     */
    private String buildId;

    /**
     * 邮件类型
     */
    private EmailType emailType;

}
