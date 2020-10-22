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

package com.tencent.bk.codecc.klocwork.utils;

import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameChangeRecordVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.klocwork.constant.KlocworkConstants;
import com.tencent.bk.codecc.klocwork.vo.KWDefectDTO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.PathUtils;
import org.apache.commons.lang3.tuple.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * klocwork告警属性工具类
 *
 * @version V1.0
 * @date 2019/10/20
 */
@Slf4j
public class KlocworkDefectAttributeUtils
{
    public static int getSeverity(KWDefectDTO defectDTO)
    {
        /*
         * 将klocwork告警的严重程度按这样映射：
         * klocwork(Critical-1)-->codecc(严重-1)，klocwork(Error-2、Warning-3)-->codecc(一般)，klocwork(Review-4)-->codecc(提示-4)
         */
        int severity = defectDTO.getSeverityCode();
        if (ComConstants.SERIOUS != severity && ComConstants.PROMPT != severity)
        {
            severity = ComConstants.NORMAL;
        }
        return severity;
    }

    public static int getStatus(KWDefectDTO defectDTO)
    {
        return KlocworkConstants.KWDefectState.valueOf(defectDTO.getState()).value();
    }

    /*
    样例：

    */
    /**
     * 获取告警相关行的所有作者
     *
     * @param defectDTO
     * @param fileLineAuthorInfos
     * @return
     *
     *  样例：
     *  defectDTO: /d/zhenwtest/kloc/src/com/tencent/android/rd/historyadapter.java (小写)
     *  fileLineAuthorInfos: /D/zhenwtest/kloc/src/com/tencent/android/rd/HistoryAdapter.java
     */
    public static Pair<Set<String>, String> getAuthor(
        KWDefectDTO defectDTO, Map<String, ScmBlameVO> fileLineAuthorInfos)
    {
        Set<String> authorSet = new TreeSet<>();
        String revision = "";

        if (MapUtils.isEmpty(fileLineAuthorInfos))
        {
            log.info("Scm change records json is empty!");
            return Pair.of(authorSet, revision);
        }
        String srcFile = defectDTO.getFile();
        long lineNum = defectDTO.getLine();

        ScmBlameVO fileLineAuthorInfo = fileLineAuthorInfos.get(srcFile);
        if (fileLineAuthorInfo == null)
        {
            // 统一成linux格式的路径
            srcFile = PathUtils.trimWinDifferentPath(srcFile).toLowerCase();
            for (Map.Entry<String, ScmBlameVO> entry : fileLineAuthorInfos.entrySet()) {
                String fileName = PathUtils.trimWinDifferentPath(entry.getValue().getFilePath()).toLowerCase();
                if (fileName.equals(srcFile))
                {
                    fileLineAuthorInfo = entry.getValue();
                    break;
                }
            }
        }

        if (fileLineAuthorInfo == null)
        {
            log.info("get author is empty for id: {}", defectDTO.getId());
            return Pair.of(authorSet, revision);
        }

        List<ScmBlameChangeRecordVO> changeRecords = fileLineAuthorInfo.getChangeRecords();
        if (CollectionUtils.isNotEmpty(changeRecords)) {
            for (ScmBlameChangeRecordVO scmBlameChangeRecordVO : changeRecords) {
                boolean isFound = false;
                List<Object> lines = scmBlameChangeRecordVO.getLines();
                if (lines != null && lines.size() > 0)
                {
                    for (Object line : lines)
                    {
                        if (line instanceof Integer && lineNum == (int) line)
                        {
                            isFound = true;
                        }
                        else if (line instanceof List)
                        {
                            List<Integer> lineScope = (List<Integer>) line;
                            if (CollectionUtils.isNotEmpty(lineScope) && lineScope.size() > 1
                                && lineScope.get(0) <= lineNum && lineScope.get(lineScope.size() - 1) >= lineNum)
                            {
                                isFound = true;
                            }
                        }
                        if (isFound)
                        {
                            String authCur = scmBlameChangeRecordVO.getAuthor();
                            if (StringUtils.isNotEmpty(authCur))
                            {
                                // 去掉中文名
                                int keyIndex = authCur.indexOf("(");
                                if (keyIndex != -1)
                                {
                                    authCur = authCur.substring(0, keyIndex);
                                }
                            }
                            authorSet.add(authCur);
                            break;
                        }
                    }
                }
                if (isFound)
                {
                    revision = fileLineAuthorInfo.getRevision();
                    break;
                }
            }
        }
        log.info("get author for id: {}, author: {}", defectDTO.getId(), authorSet.toString());
        return Pair.of(authorSet, revision);
    }
}
