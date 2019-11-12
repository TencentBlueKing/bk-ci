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

package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.dao.CommonDao;
import com.tencent.bk.codecc.task.dao.ToolMetaCache;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolMetaRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.model.BaseDataEntity;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolMetaEntity;
import com.tencent.bk.codecc.task.service.MetaService;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.vo.ToolMetaBaseVO;
import com.tencent.bk.codecc.task.vo.ToolMetaDetailVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.external.BkAuthExPermissionApi;
import com.tencent.devops.common.auth.util.AdminMemberUtils;
import com.tencent.devops.common.util.CompressionUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;

/**
 * 工具元数据业务逻辑处理类
 *
 * @version V1.0
 * @date 2019/4/25
 */
@Service
public class MetaServiceImpl implements MetaService
{
    @Autowired
    private BkAuthExPermissionApi bkAuthExPermissionApi;

    @Autowired
    private ToolMetaRepository toolMetaRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private BaseDataRepository baseDataRepository;

    @Autowired
    private ToolMetaCache toolMetaCache;

    @Autowired
    private CommonDao commonDao;

    @Override
    public List<ToolMetaBaseVO> toolList(Boolean isDetail)
    {
        long startTime = System.currentTimeMillis();
        // 1.查询工具列表
        boolean isAdmin = AdminMemberUtils.isAdminMember(bkAuthExPermissionApi);
        long isAdminTime = System.currentTimeMillis();
        System.out.println(isAdminTime - startTime);

        Map<String, ToolMetaBaseVO> toolMap = toolMetaCache.getToolMetaListFromCache(isDetail, isAdmin);
        if (toolMap.size() == 0)
        {
            toolMap = getToolMetaListFromDB(isDetail, isAdmin);
        }

        System.out.println(System.currentTimeMillis() - isAdminTime);

        // 2.对工具进行排序
        List<ToolMetaBaseVO> toolList = null;
        if (toolMap.size() > 0)
        {
            toolList = new ArrayList<>(toolMap.size());
            String orderToolIds = commonDao.getToolOrder();
            String[] toolIDArr = orderToolIds.split(",");
            for (String id : toolIDArr)
            {
                ToolMetaBaseVO toolMetaVO = toolMap.get(id);
                if (toolMetaVO != null)
                {
                    toolList.add(toolMetaVO);
                }
            }

            // 3.判断工具是否推荐
            isRecommendTool(toolList);
        }

        return toolList;
    }

    /**
     * 获取工具顺序
     *
     * @return
     */
    @Override
    public String getToolOrder()
    {
        return commonDao.getToolOrder();
    }


    /**
     * 从数据库查询工具列表
     *
     * @param isDetail
     * @param isAdmin
     * @return
     */
    private Map<String, ToolMetaBaseVO> getToolMetaListFromDB(boolean isDetail, boolean isAdmin)
    {
        List<ToolMetaEntity> toolMetaList;
        if (isAdmin)
        {
            toolMetaList = toolMetaRepository.findAll();
        }
        else
        {
            toolMetaList = toolMetaRepository.findByStatus(TaskConstants.ToolIntegratedStatus.P.name());
        }

        Map<String, ToolMetaBaseVO> toolMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(toolMetaList))
        {
            for (ToolMetaEntity toolMetaEntity : toolMetaList)
            {
                // 列表查询不包括图文详情
                toolMetaEntity.setGraphicDetails(null);
                ToolMetaBaseVO toolMetaVO;

                if (Boolean.TRUE.equals(isDetail))
                {
                    // 图标不为空时解压图标
                    String logo = toolMetaEntity.getLogo();
                    if (StringUtils.isNotEmpty(logo))
                    {
                        byte[] compressLogoBytes = logo.getBytes(StandardCharsets.ISO_8859_1);
                        byte[] afterDecompress = CompressionUtils.decompress(compressLogoBytes);
                        if (afterDecompress != null)
                        {
                            logo = new String(afterDecompress, StandardCharsets.UTF_8);
                        }
                        toolMetaEntity.setLogo(logo);
                    }
                    toolMetaVO = new ToolMetaDetailVO();
                }
                else
                {
                    toolMetaVO = new ToolMetaBaseVO();
                }
                BeanUtils.copyProperties(toolMetaEntity, toolMetaVO);
                toolMap.put(toolMetaVO.getName(), toolMetaVO);
            }
        }
        return toolMap;
    }

    @Override
    public ToolMetaDetailVO queryToolDetail(String toolName)
    {
        ToolMetaEntity toolMetaEntity = toolMetaCache.getToolFromCache(toolName);
        if (toolMetaEntity == null)
        {
            toolMetaEntity = toolMetaRepository.findByName(toolName);

            // 解压图标和图文详情
            String logo = toolMetaEntity.getLogo();
            if (StringUtils.isNotEmpty(logo))
            {
                byte[] compressLogoBytes = logo.getBytes(StandardCharsets.ISO_8859_1);
                byte[] afterDecompress = CompressionUtils.decompress(compressLogoBytes);
                toolMetaEntity.setLogo(new String(afterDecompress, StandardCharsets.UTF_8));
            }

            String graphicDetails = toolMetaEntity.getGraphicDetails();
            if (StringUtils.isNotEmpty(graphicDetails))
            {
                byte[] compressGraphicDetailsBytes = graphicDetails.getBytes(StandardCharsets.ISO_8859_1);
                byte[] afterDecompress = CompressionUtils.decompress(compressGraphicDetailsBytes);
                toolMetaEntity.setGraphicDetails(new String(afterDecompress, StandardCharsets.UTF_8));
            }
        }

        ToolMetaDetailVO toolMetaVO = new ToolMetaDetailVO();
        BeanUtils.copyProperties(toolMetaEntity, toolMetaVO);
        return toolMetaVO;
    }

    @Override
    public Map<String, List<MetadataVO>> queryMetadatas(String metadataType)
    {
        String[] metadataTypeArr = metadataType.split(";");

        List<String> metadataTypes = Arrays.asList(metadataTypeArr);
        Map<String, List<MetadataVO>> metadataMap = new HashMap<>(metadataTypeArr.length);

        // t_base_data表与MetadataVO的映射关系，如循环里面的赋值关系。另外，元数据在前端的展示顺序映射到param_extend3
        List<BaseDataEntity> baseDataList = baseDataRepository.findByParamTypeInOrderByParamExtend3(metadataTypes);

        for (BaseDataEntity baseDataEntity : baseDataList)
        {
            MetadataVO metadataVO = new MetadataVO();
            metadataVO.setKey(baseDataEntity.getParamCode());
            metadataVO.setName(baseDataEntity.getParamName());
            metadataVO.setFullName(baseDataEntity.getParamExtend1());
            metadataVO.setStatus(baseDataEntity.getParamStatus());
            metadataVO.setAliasNames(baseDataEntity.getParamExtend2());
            metadataVO.setCreator(baseDataEntity.getCreatedBy());
            metadataVO.setCreateTime(baseDataEntity.getCreatedDate());

            List<MetadataVO> metadataList = metadataMap.get(baseDataEntity.getParamType());
            if (CollectionUtils.isEmpty(metadataList))
            {
                metadataList = new ArrayList<>();
                metadataMap.put(baseDataEntity.getParamType(), metadataList);
            }
            metadataList.add(metadataVO);

        }

        return metadataMap;
    }

    /**
     * 是否推荐该款工具
     *
     * @param toolList
     */
    private void isRecommendTool(List<ToolMetaBaseVO> toolList)
    {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String taskId = request.getHeader(AUTH_HEADER_DEVOPS_TASK_ID);
        if (StringUtils.isNotEmpty(taskId))
        {
            TaskInfoEntity taskInfoEntity = taskRepository.findCodeLangByTaskId(Long.valueOf(taskId));
            if (taskInfoEntity != null)
            {
                Long codeLang = taskInfoEntity.getCodeLang();
                if (null != codeLang)
                {
                    for (ToolMetaBaseVO tool : toolList)
                    {
                        long lang = tool.getLang();

                        // 表示的是其他语言 1073741824 2^32
                        if ((codeLang & lang) > 0 || (lang & TaskConstants.OTHER_LANG) > 0)
                        {
                            tool.setRecommend(true);
                        }
                        else
                        {
                            tool.setRecommend(false);
                        }
                    }
                }

            }
        }
    }

}
