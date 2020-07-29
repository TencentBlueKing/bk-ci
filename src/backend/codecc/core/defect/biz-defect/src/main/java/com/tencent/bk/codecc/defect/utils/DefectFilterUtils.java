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
 
package com.tencent.bk.codecc.defect.utils;

import com.tencent.bk.codecc.defect.model.LintFileEntity;
import org.apache.commons.lang.StringUtils;

/**
 * 告警过滤工具
 * 
 * @date 2019/10/25
 * @version V1.0
 */
public class DefectFilterUtils 
{
    /**
     * 临时处理：脏数据告警
     *
     * @param lintFileEntity 告警文件实体
     * @return boolean
     */
    public static boolean isDirtyDataDefect(LintFileEntity lintFileEntity)
    {
        boolean result = false;
        String filePathTmp = lintFileEntity.getFilePath();
        if (StringUtils.isNotBlank(filePathTmp) &&
                (filePathTmp.startsWith("/data/landun/workspace/.temp") ||
                        filePathTmp.startsWith("/data/landun/workspace/.git")))
        {
            result = true;
        }

        return result;
    }

}
