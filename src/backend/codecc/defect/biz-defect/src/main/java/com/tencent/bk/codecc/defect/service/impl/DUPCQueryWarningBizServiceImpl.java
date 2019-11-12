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

package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.constant.DefectConstants;
import com.tencent.bk.codecc.defect.dao.mongorepository.DUPCDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.DUPCDefectDao;
import com.tencent.bk.codecc.defect.model.CodeBlockEntity;
import com.tencent.bk.codecc.defect.model.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.model.DUPCFileQueryRspEntity;
import com.tencent.bk.codecc.defect.service.AbstractQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.PipelineService;
import com.tencent.bk.codecc.defect.service.TreeService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.*;
import com.tencent.bk.codecc.defect.vo.common.*;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.util.PathUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 圈复杂度告警管理服务实现
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Service("DUPCQueryWarningBizService")
public class DUPCQueryWarningBizServiceImpl extends AbstractQueryWarningBizService
{
    private static Logger logger = LoggerFactory.getLogger(DUPCQueryWarningBizServiceImpl.class);

    @Autowired
    private DUPCDefectDao dupcDefectDao;

    @Autowired
    private DUPCDefectRepository dupcDefectRepository;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Autowired
    private BizServiceFactory<TreeService> treeServiceBizServiceFactory;

    @Autowired
    private PipelineService pipelineService;

    @Override
    public QueryWarningPageInitRspVO processQueryWarningPageInitRequest(Long taskId, String toolName)
    {
        List<DUPCDefectEntity> dupcDefectEntityList = dupcDefectRepository.findByTaskIdAndStatus(
                taskId, DefectConstants.DefectStatus.NEW.value());

        Set<String> authorSet = new HashSet<>();
        Set<String> defectPaths = new TreeSet<>();
        if (CollectionUtils.isNotEmpty(dupcDefectEntityList))
        {
            dupcDefectEntityList.forEach(defect ->
            {
                // 设置作者
                String authorList = defect.getAuthorList();
                if (StringUtils.isNotEmpty(authorList))
                {
                    String[] authorArray = authorList.split(";");
                    for (String author : authorArray)
                    {
                        if (null != author && author.trim().length() != 0)
                        {
                            authorSet.add(author);
                        }
                    }
                }

                // 获取所有警告文件的相对路径
                String relativePath = PathUtils.getRelativePath(defect.getUrl(), defect.getRelPath());
                if (org.apache.commons.lang3.StringUtils.isNotBlank(relativePath))
                {
                    defectPaths.add(relativePath);
                }
            });
        }
        QueryWarningPageInitRspVO queryWarningPageInitRspVO = new QueryWarningPageInitRspVO();
        queryWarningPageInitRspVO.setAuthorList(authorSet);

        // 处理文件树
        TreeService treeService = treeServiceBizServiceFactory.createBizService(toolName, ComConstants.BusinessType.TREE_SERVICE.value(), TreeService.class);
        TreeNodeVO treeNode = treeService.getTreeNode(taskId, defectPaths);
        queryWarningPageInitRspVO.setFilePathTree(treeNode);
        return queryWarningPageInitRspVO;
    }

    @Override
    public CommonFileQueryRspVO processQueryWarningRequest(long taskId, CommonFileQueryReqVO queryWarningReq, int pageNum, int pageSize, String sortField, Sort.Direction sortType)
    {
        Sort pageSort;
        if (StringUtils.isEmpty(sortField) || null == sortType)
        {
            pageSort = new Sort(Sort.Direction.ASC, "file_path");
        }
        else
        {
            pageSort = new Sort(sortType, sortField);
        }

        //封装分页类
        Pageable pageable = new PageRequest(pageNum - 1 < 0 ? 0 : pageNum - 1, pageSize <= 0 ? 10 : pageSize, pageSort);

        //获取风险系数值
        Map<String, String> riskConfigMap = thirdPartySystemCaller.getRiskFactorConfig(ComConstants.Tool.DUPC.name());

        //查询数值
        DUPCFileQueryRspEntity dupcFileQueryRspEntity = dupcDefectDao.findDUPCFileByParam(taskId, queryWarningReq.getFileList(), queryWarningReq.getAuthor(),
                queryWarningReq.getSeverity(), riskConfigMap, pageable);
        List<DUPCDefectEntity> dupcDefectEntityList = dupcFileQueryRspEntity.getDefectList().getContent();
        List<DUPCDefectVO> dupcDefectVOS = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(dupcDefectEntityList))
        {
            for (DUPCDefectEntity defect : dupcDefectEntityList)
            {
                DUPCDefectVO dupcDefectVO = new DUPCDefectVO();
                BeanUtils.copyProperties(defect, dupcDefectVO);
                dupcDefectVOS.add(dupcDefectVO);
            }
        }
        Page<DUPCDefectVO> ccnDefectVOPage = new PageImpl<>(dupcDefectVOS, pageable, dupcFileQueryRspEntity.getDefectList().getTotalElements());
        DUPCFileQueryRspVO dupcFileQueryRspVO = new DUPCFileQueryRspVO();
        BeanUtils.copyProperties(dupcFileQueryRspEntity, dupcFileQueryRspVO, "defectList");
        dupcFileQueryRspVO.setDefectList(ccnDefectVOPage);

        return dupcFileQueryRspVO;


    }

    @Override
    public CommonDefectQueryRspVO processQueryWarningDetailRequest(long taskId, CommonDefectQueryReqVO queryWarningDetailReq, String sortField, Sort.Direction sortType)
    {

        DUPCDefectQueryRspVO dupcDefectQueryRspVO = new DUPCDefectQueryRspVO();

        // 获取源重复块
        getSourceCodeBlockDetail(taskId, queryWarningDetailReq, dupcDefectQueryRspVO);

        // 获取目标重复块
        getTargetCodeBlockDetail(taskId, dupcDefectQueryRspVO);

        return dupcDefectQueryRspVO;
    }

    @Override
    public CommonDefectQueryRspVO processGetFileContentSegmentRequest(long taskId, GetFileContentSegmentReqVO reqModel)
    {
        String toolName = reqModel.getToolName();
        String filePath = reqModel.getFilePath();
        logger.debug("processGetFileContentSegmentRequest() ===BEGIN=== filePath: [{}]", filePath);
        DUPCDefectEntity dupcDefectEntity = dupcDefectRepository.findFirstByTaskIdAndFilePath(taskId, filePath);

        if (dupcDefectEntity == null)
        {
            logger.error("Can't find DUPC defect entity by taskId:{}, toolName:{}, filePath:{}", taskId, toolName, filePath);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{"重复率的缺陷实体"}, null);
        }

        // 1. 根据文件路径从分析集群获取文件内容
        String content = pipelineService.getFileContent(taskId, dupcDefectEntity.getRepoId(), dupcDefectEntity.getRelPath(),
                dupcDefectEntity.getRevision(), dupcDefectEntity.getBranch(), dupcDefectEntity.getSubModule());
        // 2. 根据告警的开始行和结束行截取文件片段
        CommonDefectQueryRspVO dupcDefectQueryRspVO = new CommonDefectQueryRspVO();
        if (reqModel.getEndLine() > 0)
        {
            content = trimCodeSegment(content, reqModel.getBeginLine(), reqModel.getEndLine(), dupcDefectQueryRspVO);
        }
        dupcDefectQueryRspVO.setFileContent(content);

        //获取文件的相对路径
        String relativePath = PathUtils.getRelativePath(dupcDefectEntity.getUrl(), dupcDefectEntity.getRelPath());
        dupcDefectQueryRspVO.setRelativePath(relativePath);
        dupcDefectQueryRspVO.setFilePath(filePath);
        int fileNameIndex = filePath.lastIndexOf("/");
        if (fileNameIndex == -1)
        {
            fileNameIndex = filePath.lastIndexOf("\\");
        }
        dupcDefectQueryRspVO.setFileName(filePath.substring(fileNameIndex + 1));

        return dupcDefectQueryRspVO;
    }

    /**
     * 获取源重复代码块详情
     *
     * @param taskId
     * @param defectQueryReqVO
     * @param dupcDefectQueryRspVO
     * @return
     */
    @NotNull
    private CommonDefectQueryRspVO getSourceCodeBlockDetail(long taskId, CommonDefectQueryReqVO defectQueryReqVO, DUPCDefectQueryRspVO dupcDefectQueryRspVO)
    {
        //查询告警信息
        DUPCDefectEntity dupcDefectEntity = dupcDefectRepository.findByEntityId(defectQueryReqVO.getEntityId());

        if (dupcDefectEntity == null)
        {
            logger.error("Can't find DUPC defect entity by entityId:{}", defectQueryReqVO.getEntityId());
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{defectQueryReqVO.getEntityId()}, null);
        }


        // 校验传入的路径是否合法（路径是否是告警对应的文件）
        verifyFilePathIsValid(defectQueryReqVO.getFilePath(), dupcDefectEntity.getFilePath());

        //根据文件路径从分析集群获取文件内容
        String content = pipelineService.getFileContent(taskId, dupcDefectEntity.getRepoId(), dupcDefectEntity.getRelPath(),
                dupcDefectEntity.getRevision(), dupcDefectEntity.getBranch(), dupcDefectEntity.getSubModule());
        List<CodeBlockEntity> blockEntityList = dupcDefectEntity.getBlockList();
        if (blockEntityList.size() >= 1)
        {
            int beginLine = Integer.valueOf(String.valueOf(blockEntityList.get(0).getStartLines()));
            int endLine = Integer.valueOf(String.valueOf(blockEntityList.get(blockEntityList.size() - 1).getEndLines()));
            content = trimCodeSegment(content, beginLine, endLine, dupcDefectQueryRspVO);
        }

        //获取文件的相对路径
        String relativePath = PathUtils.getRelativePath(dupcDefectEntity.getUrl(), dupcDefectEntity.getRelPath());
        dupcDefectQueryRspVO.setRelativePath(relativePath);
        //组装告警详情查询相应视图
        String filePath = dupcDefectEntity.getFilePath();
        dupcDefectQueryRspVO.setFilePath(filePath);
        int fileNameIndex = filePath.lastIndexOf("/");
        if (fileNameIndex == -1)
        {
            fileNameIndex = filePath.lastIndexOf("\\");
        }
        dupcDefectQueryRspVO.setFileName(filePath.substring(fileNameIndex + 1));
        dupcDefectQueryRspVO.setFileContent(content);
        List<CodeBlockEntity> blockList = dupcDefectEntity.getBlockList();
        if (CollectionUtils.isNotEmpty(blockList))
        {
            List<CodeBlockVO> blockVOList = new ArrayList<>();
            for (CodeBlockEntity codeBlockEntity : blockList)
            {
                CodeBlockVO codeBlockVO = new CodeBlockVO();
                BeanUtils.copyProperties(codeBlockEntity, codeBlockVO);
                blockVOList.add(codeBlockVO);
            }

            // 按其实行号对重复块列表排序
            sortBlockByStartLine(blockVOList);
            dupcDefectQueryRspVO.setBlockInfoList(blockVOList);
        }

        return dupcDefectQueryRspVO;
    }

    /**
     * 获取目标重复代码块详情
     *
     * @param taskId
     * @param dupcDefectQueryRspVO
     * @return
     */
    private CommonDefectQueryRspVO getTargetCodeBlockDetail(long taskId, DUPCDefectQueryRspVO dupcDefectQueryRspVO)
    {
        List<CodeBlockVO> sourceBlockList = dupcDefectQueryRspVO.getBlockInfoList();
        if (CollectionUtils.isNotEmpty(sourceBlockList))
        {
            List<DUPCDefectEntity> dupcDefectEntityList = dupcDefectDao.queryCodeBlocksByFingerPrint(taskId, sourceBlockList);

            if (CollectionUtils.isNotEmpty(dupcDefectEntityList))
            {
                Map<String, List<CodeBlockVO>> codeBlockVOListMap = new HashMap<>();
                sourceBlockList.forEach(sourceBlock ->
                {
                    List<CodeBlockVO> codeBlockVOList = getTargetCodeBlockVOS(sourceBlock, dupcDefectEntityList, codeBlockVOListMap);
                    sortBlockByFileName(codeBlockVOList);
                });
                dupcDefectQueryRspVO.setTargetBlockMap(codeBlockVOListMap);
            }


        }

        return dupcDefectQueryRspVO;
    }

    /**
     * 根据源代码块过去目标代码块列表
     *
     * @param dupcDefectEntityList
     * @param codeBlockVOListMap
     * @param sourceBlock
     * @return
     */
    @NotNull
    private List<CodeBlockVO> getTargetCodeBlockVOS(CodeBlockVO sourceBlock, List<DUPCDefectEntity> dupcDefectEntityList, Map<String, List<CodeBlockVO>> codeBlockVOListMap)
    {
        String fingerPrint = sourceBlock.getFingerPrint();
        String sourceFile = sourceBlock.getSourceFile();
        long startLines = sourceBlock.getStartLines();
        long endLines = sourceBlock.getEndLines();
        String blockKey = startLines + "_" + endLines;
        List<CodeBlockVO> codeBlockVOList = codeBlockVOListMap.computeIfAbsent(blockKey, k -> new ArrayList<>());
        for (DUPCDefectEntity defectEntity : dupcDefectEntityList)
        {
            List<CodeBlockEntity> blockList = defectEntity.getBlockList();
            for (CodeBlockEntity codeBlockEntity : blockList)
            {
                String tempFingerPrint = codeBlockEntity.getFingerPrint();
                String tempSourceFile = codeBlockEntity.getSourceFile();
                long tempStartLines = codeBlockEntity.getStartLines();
                long tempEndLines = codeBlockEntity.getEndLines();

                // 从重复文件里面过滤出fingerPrint相同的代码块，并且排除掉源代码块本身
                if (fingerPrint.equals(tempFingerPrint)
                        && !(sourceFile.equals(tempSourceFile) && startLines == tempStartLines && endLines == tempEndLines))
                {
                    CodeBlockVO codeBlockVO = new CodeBlockVO();
                    BeanUtils.copyProperties(codeBlockEntity, codeBlockVO);
                    String filePath = codeBlockVO.getSourceFile();
                    int fileNameIndex = filePath.lastIndexOf("/");
                    if (fileNameIndex == -1)
                    {
                        fileNameIndex = filePath.lastIndexOf("\\");
                    }
                    codeBlockVO.setFileName(filePath.substring(fileNameIndex + 1));
                    codeBlockVOList.add(codeBlockVO);
                }
            }
        }
        return codeBlockVOList;
    }

    /**
     * 获取DUPC相对路径
     *
     * @param dupcDefectModelList
     */
    private void batchGetRelativePath(List<DUPCDefectVO> dupcDefectModelList)
    {
        // 加入相对路径
        if (CollectionUtils.isNotEmpty(dupcDefectModelList))
        {
            for (DUPCDefectVO defect : dupcDefectModelList)
            {
                String relativePath = PathUtils.getRelativePath(defect.getUrl(), defect.getRelPath());
                defect.setRelPath(relativePath);
            }
        }
    }

    /**
     * 校验传入的路径是否合法（路径是否是告警对应的文件）
     * 修改原因：修复安全组检测出的通过告警详情可以获取codecc工具分析服务器的任意文件的安全漏洞：
     * 如下路径：http://xxx.xxx.com/backend/duplicatecode/targetFileContent?proj_id=12023&f=../../../../../../../../../../etc/passwd
     *
     * @param filePath
     * @param defectFilePath
     * @return
     */
    private void verifyFilePathIsValid(String filePath, String defectFilePath)
    {
        if (!filePath.equals(defectFilePath))
        {
            logger.error("传入参数错误：filePath是非法路径");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{filePath}, null);
        }
    }

    /**
     * 按其实行号对重复块列表排序
     *
     * @param blockList
     */
    private void sortBlockByStartLine(List<CodeBlockVO> blockList)
    {
        blockList.sort((o1, o2) ->
        {
            Long startLine1 = o1.getStartLines();
            Long startLine2 = o2.getStartLines();
            int compareRes = startLine1.compareTo(startLine2);
            if (compareRes == 0)
            {
                Long endLine1 = o1.getEndLines();
                Long endLine2 = o2.getEndLines();
                compareRes = endLine2.compareTo(endLine1);
            }
            return compareRes;
        });
    }

    /**
     * 按其实行号对重复块列表排序
     *
     * @param blockList
     */
    private void sortBlockByFileName(List<CodeBlockVO> blockList)
    {
        blockList.sort((o1, o2) ->
        {
            String file1 = o1.getSourceFile().substring(o1.getSourceFile().lastIndexOf("/") + 1);
            String file2 = o2.getSourceFile().substring(o2.getSourceFile().lastIndexOf("/") + 1);
            int compareRes = file1.compareToIgnoreCase(file2);
            if (compareRes == 0)
            {
                Long startLine1 = o1.getStartLines();
                Long startLine2 = o2.getStartLines();
                compareRes = startLine1.compareTo(startLine2);
                if (compareRes == 0)
                {
                    Long endLine1 = o1.getEndLines();
                    Long endLine2 = o2.getEndLines();
                    compareRes = endLine2.compareTo(endLine1);
                }
            }
            return compareRes;
        });
    }


}
