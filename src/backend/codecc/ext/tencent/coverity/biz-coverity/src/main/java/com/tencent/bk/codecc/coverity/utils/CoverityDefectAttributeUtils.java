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

package com.tencent.bk.codecc.coverity.utils;

import com.coverity.ws.v9.*;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameChangeRecordVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.PathUtils;
import org.apache.commons.lang3.tuple.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;

import java.util.*;

/**
 * coverity告警属性工具类
 *
 * @version V1.0
 * @date 2019/10/20
 */
@Slf4j
public class CoverityDefectAttributeUtils
{
    private static final Map<String, Integer> SEVERITY_MAP = createMap();

    private static Map<String, Integer> createMap()
    {
        Map<String, Integer> severityMap = new HashMap<>();
        severityMap.put("High", new Integer(ComConstants.SERIOUS));
        severityMap.put("Medium", new Integer(ComConstants.NORMAL));
        severityMap.put("Low", new Integer(ComConstants.PROMPT));
        return severityMap;
    }

    /**
     * 获取告警严重程度
     *
     * @param streamDefect
     * @return
     */
    public static int getSeverity(StreamDefectDataObj streamDefect)
    {
        DefectInstanceDataObj defectInstance = streamDefect.getDefectInstances().get(0);
        LocalizedValueDataObj impact = defectInstance.getImpact();
        String impactCode = impact.getName();

        Integer severity = SEVERITY_MAP.get(impactCode);

        if (severity == null)
        {
            log.error("can't get severity from SEVERITY_MAP by [{}] for streamName [{}] cid [{}]",
                    impactCode, streamDefect.getStreamId().getName(), streamDefect.getCid());

            // 默认一般
            severity = ComConstants.NORMAL;
        }
        return severity;
    }

    /**
     * 获取告警状态
     *
     * @param streamDefect
     * @return
     */
    public static int getStatus(StreamDefectDataObj streamDefect)
    {
        List<DefectStateAttributeValueDataObj> defectStateAttributeValues = streamDefect.getDefectStateAttributeValues();
        int status = getStatus(defectStateAttributeValues);

//        log.info("CID: {},  Status:{}", streamDefect.getCid(), status);
        return status;
    }

    /**
     * 获取告警状态
     *
     * @param defectStateAttributeValues
     * @return
     */
    public static int getStatus(List<DefectStateAttributeValueDataObj> defectStateAttributeValues)
    {
        String defectStatus = "";
        for (DefectStateAttributeValueDataObj defectState : defectStateAttributeValues)
        {
            if (defectState == null)
            {
                continue;
            }
            AttributeDefinitionIdDataObj iobj = defectState.getAttributeDefinitionId();
            String id = (iobj == null) ? "" : iobj.getName();
            if ("DefectStatus".equalsIgnoreCase(id))
            {
                AttributeValueIdDataObj vobj = defectState.getAttributeValueId();
                defectStatus = (vobj == null) ? "" : vobj.getName();
                break;
            }
        }
//        log.info("Status:{}", defectStatus);
        if ("Fixed".equalsIgnoreCase(defectStatus) || "Dismissed".equalsIgnoreCase(defectStatus) || "Absent Dismissed".equalsIgnoreCase(defectStatus))
        {
            return ComConstants.DefectStatus.FIXED.value();
        }

        return ComConstants.DefectStatus.NEW.value();
    }

    public static String getDisplayCategory(StreamDefectDataObj streamDefect)
    {
        DefectInstanceDataObj defectInstance = streamDefect.getDefectInstances().get(0);
        String displayCategory = defectInstance.getCategory().getDisplayName();
        return displayCategory;
    }

    public static String getDisplayType(StreamDefectDataObj streamDefect)
    {
        DefectInstanceDataObj defectInstance = streamDefect.getDefectInstances().get(0);
        String displayType = defectInstance.getType().getDisplayName();
        return displayType;
    }

    public static String getFilePathname(StreamDefectDataObj streamDefect)
    {
        DefectInstanceDataObj defectInstance = streamDefect.getDefectInstances().get(0);
        String filePathname = defectInstance.getFunction().getFileId().getFilePathname();
        return filePathname;
    }

    public static Pair<Set<String>, String> getAuthor(StreamDefectDataObj streamDefect,
                                                      Map<String, ScmBlameVO> fileChangeRecordsMap)
    {
        if (MapUtils.isEmpty(fileChangeRecordsMap))
        {
            log.info("file change record map is empty for cid: {}", streamDefect.getCid());
            return Pair.of(new HashSet<>(), "");
        }

        List<DefectInstanceDataObj> defectInstanceList = streamDefect.getDefectInstances();
        Map<String, List<Integer>> lineMap = new HashMap<>();
        Map<String, List<Integer>> lineMapAll = new HashMap<>();
        boolean hasMain = initLineMap(defectInstanceList, lineMap, lineMapAll);

        Pair<Set<String>, String> pair;
        if (hasMain)
        {
            pair = getAuthorsInLine(lineMap, fileChangeRecordsMap);
        }
        else
        {
            pair = getAuthorsInLine(lineMapAll, fileChangeRecordsMap);
        }
        log.info("get author for cid: {}, author: {}, revision: {}",
            streamDefect.getCid(), pair.getKey().toString(), pair.getValue());
        return pair;
    }

    /**
     * 初始化LineMap
     *
     * @param defectInstanceList
     * @param lineMap
     * @param lineMapAll
     * @return
     */
    private static boolean initLineMap(List<DefectInstanceDataObj> defectInstanceList, Map<String, List<Integer>> lineMap,
                                       Map<String, List<Integer>> lineMapAll)
    {
        boolean hasMain = false;

        for (DefectInstanceDataObj defectInstance : defectInstanceList)
        {
            List<EventDataObj> eventList = defectInstance.getEvents();

            for (EventDataObj event : eventList)
            {
                FileIdDataObj fileId = event.getFileId();
                String filePathname = fileId.getFilePathname();
                int lineCur = event.getLineNumber();
                String tagCur = event.getEventTag();

                // judge eventTag, if is example then not append
                if (!tagCur.startsWith("example_"))
                {
                    // judge is the main event
                    if (event.isMain())
                    {
                        dealEachLine(lineMap, filePathname, lineCur);
                        hasMain = true;
                    }

                    dealEachLine(lineMapAll, filePathname, lineCur);
                }
            }
        }
        return hasMain;
    }

    /**
     * 处理每行数据
     *
     * @param lineMap
     * @param filePathname
     * @param lineCur
     */
    private static void dealEachLine(Map<String, List<Integer>> lineMap, String filePathname, int lineCur)
    {
        if (lineCur != 0)
        {
            List<Integer> lineList = lineMap.get(filePathname);
            if (lineList == null)
            {
                lineList = new ArrayList<>();
                lineMap.put(filePathname, lineList);
            }
            lineList.add(lineCur);
        }
    }

    /**
     * 获取告警相关行的所有作者
     *
     * @param lineMap
     * @return
     *
     *  样例：
     *  lineMap: /xxx/bbbb
     *  fileChangeRecordsMap: d:/xxx/bbb or /d/xxx/bbb
     */
    public static Pair<Set<String>, String> getAuthorsInLine(
        Map<String, List<Integer>> lineMap, Map<String, ScmBlameVO> fileChangeRecordsMap)
    {
        Set<String> authorSet = new TreeSet<>();
        String revision = "";

        try
        {
            for (Map.Entry<String, ScmBlameVO> entry : fileChangeRecordsMap.entrySet())
            {
                String srcFile = PathUtils.trimWinDifferentPath(entry.getKey());
                List<Integer> lineNumList = lineMap.get(srcFile);

                // coverity 和 blame.json都存在才处理
                if (lineNumList == null)
                {
                    continue;
                }

                ScmBlameVO scmBlame = entry.getValue();
                List<ScmBlameChangeRecordVO> changeRecords = scmBlame.getChangeRecords();

                // 其他逻辑跟以前一样，不变
                if (CollectionUtils.isNotEmpty(changeRecords))
                {
                    // 遍历所有行，匹配每行的最新修改人
                    for (int lineNum : lineNumList)
                    {
                        for (ScmBlameChangeRecordVO scmBlameChangeRecordVO : changeRecords)
                        {
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
                                revision = scmBlame.getRevision();
                                break;
                            }
                        }
                    }
                }
            }
        }
        catch (JSONException e)
        {
            log.error("ERROR!!!", e);
        }
        return Pair.of(authorSet, revision);
    }
}
