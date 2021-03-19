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

import lombok.Data;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * 分析服务器的实体类
 * 
 * @date 2019/11/4
 * @version V1.0
 */
@Data
public class FileInfoModel
{
    /**
     * 文件名，不包含路径
     */
    private  String fileName;

    /**
     * 文件完整路径
     */
    private String filePath;

    /**
     * 文件最近修改时间
     */
    private long lastModifiedTime;

    /**
     * 文件大小
     */
    private long size;

    /**
     * 文件内容的MD5
     */
    private String contentMd5;

    /**
     * 该缓存对象的创建时间
     */
    private long createtime;
}
