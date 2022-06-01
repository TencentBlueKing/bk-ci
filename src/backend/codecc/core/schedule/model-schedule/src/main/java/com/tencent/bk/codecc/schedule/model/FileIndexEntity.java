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

package com.tencent.bk.codecc.schedule.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Set;

/**
 * 文件路由的实体类
 *
 * @date 2019/11/4
 * @version V1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_file_index")
public class FileIndexEntity extends CommonEntity
{
    /**
     * 文件名（不带路径）
     */
    @Field("file_name")
    @Indexed
    private String fileName;

    /**
     * 文件路径
     */
    @Field("file_folder")
    private String fileFolder;

    /**
     * 上传类型
     */
    @Field("upload_type")
    private String uploadType;

    /**
     * 存储类型
     * {@link com.tencent.codecc.common.storage.constant.StorageType}
     */
    @Field("store_type")
    private String storeType;

    /**
     * 下载地址，NFS文件存储模式下。为空
     */
    @Field("download_url")
    private String downloadUrl;


    /**
     * 上传ID
     */
    @Field("upload_id")
    private String uploadId;
//    /**
//     * 0-初始，1-上传成功
//     */
//    @Field("status")
//    private int status;
}
