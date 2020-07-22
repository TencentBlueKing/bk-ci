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

import com.tencent.bk.codecc.defect.model.TransferAuthorEntity;
import com.tencent.devops.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * 告警处理人转换工具类
 *
 * @version V1.0
 * @date 2019/10/21
 */
@Slf4j
public class AuthorTransferUtils
{
    public static Set<String> authorTrans(Set<String> oldAuthorList, List<String> taskOwner, List<TransferAuthorEntity.TransferAuthorPair> authorTransList)
    {
        oldAuthorList = CollectionUtils.isEmpty(oldAuthorList) ? new TreeSet<>(taskOwner) : oldAuthorList;
        Set<String> newAuthorList;
        if (CollectionUtils.isNotEmpty(oldAuthorList) && CollectionUtils.isNotEmpty(authorTransList))
        {
            newAuthorList = new TreeSet<>(oldAuthorList);
            log.info("transfer author, oldAuthor: [{}], authorTransList:[{}]", oldAuthorList, JsonUtil.INSTANCE.toJson(authorTransList));
            for (TransferAuthorEntity.TransferAuthorPair transferAuthorPair : authorTransList)
            {
                List<String> sourceAuthorList = Arrays.asList(transferAuthorPair.getSourceAuthor().split(";"));
                // 比如原作者为 A;B;C 转换关系为( A;C ---> D ), 则转换后为 B;D  无尽的轮回
                if (newAuthorList.containsAll(sourceAuthorList))
                {
                    newAuthorList.removeAll(sourceAuthorList);
                    newAuthorList.addAll(Arrays.asList(transferAuthorPair.getTargetAuthor().split(";")));
                }
            }
        }
        else
        {
            newAuthorList = oldAuthorList;
        }

        log.info("success transfer author, oldAuthor: [{}], newAuthor:[{}]", oldAuthorList.toString(), newAuthorList.toString());
        return newAuthorList;
    }


    public static String singleAuthorTrans(String oldAuthor, List<String> taskOwner, List<TransferAuthorEntity.TransferAuthorPair> authorPairList)
    {
        if(StringUtils.isBlank(oldAuthor))
        {
            if(CollectionUtils.isNotEmpty(taskOwner))
            {
                oldAuthor = taskOwner.get(0);
            }
        }
        String newAuthor = oldAuthor;
        if(CollectionUtils.isNotEmpty(authorPairList))
        {
            for(TransferAuthorEntity.TransferAuthorPair transferAuthorPair : authorPairList)
            {
                List<String> sourceAuthorList = Arrays.asList(transferAuthorPair.getSourceAuthor().split(";"));
                if(sourceAuthorList.contains(newAuthor))
                {
                    newAuthor = transferAuthorPair.getTargetAuthor();
                }
            }
        }
        return newAuthor;
    }
}
