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

package com.tencent.bk.codecc.schedule.vo;

import com.tencent.devops.common.api.CommonVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分析服务器的实体类
 *
 * @version V1.0
 * @date 2019/11/4
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileIndexVO extends CommonVO
{
    /**
     * 文件名，不包含路径
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String fileFolder;

    /**
     * 上传类型
     */
    private String uploadType;

    /**
     * 存储类型
     */
    private String storeType;

    /**
     *
     */
    private String downloadUrl;

    /**
     * 0-初始，1-上传成功
     */
    private int status;
}
