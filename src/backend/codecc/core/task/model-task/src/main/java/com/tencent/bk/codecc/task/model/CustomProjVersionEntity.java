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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

/**
 * api触发项目版本信息
 * 
 * @date 2021/1/12
 * @version V1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomProjVersionEntity {
    /**
     * git插件版本
     */
    @Field("git_plugin_version")
    private String gitPluginVersion;

    /**
     * codecc插件版本
     */
    @Field("codecc_plugin_version")
    private String codeccPluginVersion;

    /**
     * 提交id
     */
    @Field("commit_id")
    private String commitId;

    /**
     * 运行时参数
     */
    @Field("runtime_map")
    private Map<String, String> runtimeMap;

    /**
     * codecc扫描参数
     */
    @Field("scan_param_map")
    private Map<String, Object> scanParamMap;


    /**
     * 路由信息
     */
    @Field("dispatch_route")
    private String dispatchRoute;
}
