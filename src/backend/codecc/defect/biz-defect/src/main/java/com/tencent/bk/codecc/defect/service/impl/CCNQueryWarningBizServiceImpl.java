/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.CCNFileQueryRspEntity;
import com.tencent.bk.codecc.defect.service.AbstractQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.PipelineService;
import com.tencent.bk.codecc.defect.service.TreeService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.CCNDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.CCNDefectVO;
import com.tencent.bk.codecc.defect.vo.CCNFileQueryRspVO;
import com.tencent.bk.codecc.defect.vo.TreeNodeVO;
import com.tencent.bk.codecc.defect.vo.common.*;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.util.PathUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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
@Service("CCNQueryWarningBizService")
public class CCNQueryWarningBizServiceImpl extends AbstractQueryWarningBizService
{
    private static Logger logger = LoggerFactory.getLogger(CCNQueryWarningBizServiceImpl.class);

    @Autowired
    private CCNDefectDao ccnDefectDao;

    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Autowired
    private BizServiceFactory<TreeService> treeServiceBizServiceFactory;

    @Autowired
    private PipelineService pipelineService;

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
        Map<String, String> riskConfigMap = thirdPartySystemCaller.getRiskFactorConfig(ComConstants.Tool.CCN.name());

        //查询数值
        CCNFileQueryRspEntity ccnFileQueryRspEntity = ccnDefectDao.findCCNFileByParam(taskId, queryWarningReq.getFileList(), queryWarningReq.getAuthor(),
                queryWarningReq.getSeverity(), riskConfigMap, pageable);

        List<CCNDefectEntity> ccnDefectEntityList = ccnFileQueryRspEntity.getDefectList().getContent();
        List<CCNDefectVO> ccnDefectVOS = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ccnDefectEntityList))
        {
            //获取相对路径
            batchGetRelativePath(ccnDefectEntityList);
            for (CCNDefectEntity defect : ccnDefectEntityList)
            {
                CCNDefectVO ccnDefectVO = new CCNDefectVO();
                BeanUtils.copyProperties(defect, ccnDefectVO);
                ccnDefectVOS.add(ccnDefectVO);
            }
        }
        Page<CCNDefectVO> ccnDefectVOPage = new PageImpl<>(ccnDefectVOS, pageable, ccnFileQueryRspEntity.getDefectList().getTotalElements());
        CCNFileQueryRspVO ccnFileQueryRspVO = new CCNFileQueryRspVO();
        BeanUtils.copyProperties(ccnFileQueryRspEntity, ccnFileQueryRspVO, "defectList");
        ccnFileQueryRspVO.setDefectList(ccnDefectVOPage);
        return ccnFileQueryRspVO;
    }

    @Override
    public CommonDefectQueryRspVO processQueryWarningDetailRequest(long taskId, CommonDefectQueryReqVO queryWarningDetailReq, String sortField, Sort.Direction sortType)
    {
        CCNDefectQueryRspVO ccnDefectQueryRspVO = new CCNDefectQueryRspVO();

        //查询告警信息
        CCNDefectEntity ccnDefectEntity = ccnDefectRepository.findByEntityId(queryWarningDetailReq.getEntityId());

        // 校验传入的路径是否合法（路径是否是告警对应的文件）
        verifyFilePathIsValid(queryWarningDetailReq.getFilePath(), ccnDefectEntity.getFilePath());

        //根据文件路径从分析集群获取文件内容
        String content = pipelineService.getFileContent(taskId, ccnDefectEntity.getRepoId(), ccnDefectEntity.getRelPath(),
                ccnDefectEntity.getRevision(), ccnDefectEntity.getBranch(), ccnDefectEntity.getSubModule());

        content = trimCodeSegment(content, ccnDefectEntity.getStartLines(),
                ccnDefectEntity.getEndLines(), ccnDefectQueryRspVO);


        //获取文件的相对路径
        String relativePath = PathUtils.getRelativePath(ccnDefectEntity.getUrl(), ccnDefectEntity.getRelPath());
        ccnDefectQueryRspVO.setRelativePath(relativePath);
        //组装告警详情查询相应视图
        String filePath = ccnDefectEntity.getFilePath();
        ccnDefectQueryRspVO.setFilePath(filePath);
        int fileNameIndex = filePath.lastIndexOf("/");
        if (fileNameIndex == -1)
        {
            fileNameIndex = filePath.lastIndexOf("\\");
        }
        ccnDefectQueryRspVO.setFileName(filePath.substring(fileNameIndex + 1));
        ccnDefectQueryRspVO.setFileContent(content);

        return ccnDefectQueryRspVO;
    }

    @Override
    public QueryWarningPageInitRspVO processQueryWarningPageInitRequest(Long taskId, String toolName)
    {
        List<CCNDefectEntity> ccnDefectEntityList = ccnDefectRepository.findByTaskIdAndStatus(
                taskId, DefectConstants.DefectStatus.NEW.value());

        Set<String> authorSet = new TreeSet<>();
        Set<String> defectPaths = new TreeSet<>();
        if (CollectionUtils.isNotEmpty(ccnDefectEntityList))
        {
            ccnDefectEntityList.forEach(defect ->
            {
                // 设置作者
                authorSet.add(defect.getAuthor());

                // 获取所有警告文件的相对路径
                String relativePath = PathUtils.getRelativePath(defect.getUrl(), defect.getRelPath());
                if (StringUtils.isNotBlank(relativePath))
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


    /**
     * 获取CCN相对路径
     *
     * @param ccnDefectModelList
     */
    private void batchGetRelativePath(List<CCNDefectEntity> ccnDefectModelList)
    {
        // 加入相对路径
        if (CollectionUtils.isNotEmpty(ccnDefectModelList))
        {
            for (CCNDefectEntity defect : ccnDefectModelList)
            {
                String relativePath = PathUtils.getRelativePath(defect.getUrl(), defect.getRelPath());
                defect.setRelPath(relativePath);
            }
        }
    }


    /**
     * 校验传入的路径是否合法（路径是否是告警对应的文件）
     * 修改原因：修复安全组检测出的通过告警详情可以获取codecc工具分析服务器的任意文件的安全漏洞：
     *
     * @param filePath
     * @param defectFilePath
     * @return
     * @date 2019/1/15
     * @version V3.4.1
     */
    private void verifyFilePathIsValid(String filePath, String defectFilePath)
    {
        if (!filePath.equals(defectFilePath))
        {
            logger.error("传入参数错误：filePath是非法路径");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{filePath}, null);
        }
    }
}
