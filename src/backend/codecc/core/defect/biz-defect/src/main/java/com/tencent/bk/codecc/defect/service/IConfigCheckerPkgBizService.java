/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.ConfigCheckersPkgReqVO;
import com.tencent.bk.codecc.defect.vo.GetCheckerListRspVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;

/**
 * 规则包配置业务接口
 *
 * @version V1.0
 * @date 2019/6/5
 */
public interface IConfigCheckerPkgBizService
{

    /**
     * 获取配置规则包
     * @param taskId
     * @param toolName
     * @param codeLang
     * @param toolConfig
     * @return
     */
    GetCheckerListRspVO getConfigCheckerPkg(Long taskId, String toolName, Long codeLang, ToolConfigInfoVO toolConfig);

    /**
     * 获取配置规则包
     *
     * @param taskId
     * @param toolName
     * @return
     */
    GetCheckerListRspVO getConfigCheckerPkg(Long taskId, String toolName);


    /**
     * 开启/关闭 规则包配置
     *
     * @param taskId
     * @param toolName
     * @param checker
     * @param updatePipelineUser
     * @return
     */
    Boolean configCheckerPkg(Long taskId, String toolName, ConfigCheckersPkgReqVO checker, String updatePipelineUser);

    /**
     * 开启/关闭 规则包 不加操作记录
     * @param taskId
     * @param toolName
     * @param configCheckersPkgReq
     * @return
     */
    Boolean syncConfigCheckerPkg(Long taskId, String toolName, ConfigCheckersPkgReqVO configCheckersPkgReq);
}
