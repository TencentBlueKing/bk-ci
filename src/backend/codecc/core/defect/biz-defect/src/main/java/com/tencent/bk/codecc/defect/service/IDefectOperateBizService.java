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

import com.tencent.bk.codecc.defect.vo.SingleCommentVO;

/**
 * 告警操作服务
 * 
 * @date 2020/3/2
 * @version V1.0
 */
public interface IDefectOperateBizService
{
    /**
     * 添加代码评论
     * @param defectId
     * @param commentId
     * @param singleCommentVO
     */
    void addCodeComment(String defectId, String commentId, String userName, SingleCommentVO singleCommentVO);

    /**
     * 更新代码评论
     * @param commentId
     * @param singleCommentVO
     */
    void updateCodeComment(String commentId, String userName, SingleCommentVO singleCommentVO);

    /**
     * 删除代码评论
     * @param commentId
     * @param singleCommentId
     */
    void deleteCodeComment(String commentId, String singleCommentId, String userName);
}
