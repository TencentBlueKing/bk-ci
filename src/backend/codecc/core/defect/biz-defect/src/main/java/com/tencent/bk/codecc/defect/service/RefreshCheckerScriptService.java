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

package com.tencent.bk.codecc.defect.service;

/**
 * 刷新 规则描述、详细说明 数据
 *
 * @version V2.0
 * @date 2020/11/18
 */
public interface RefreshCheckerScriptService {
    /**
     * 仅用于刷一次规则描述、详细说明数据
     *
     * @param toolName
     * @param pageNum
     * @param pageSize
     * @param sortField
     * @param sortType
     * @return
     */
    Boolean initCheckerDetailScript(String toolName, Integer pageNum, Integer pageSize, String sortField,
            String sortType);

}