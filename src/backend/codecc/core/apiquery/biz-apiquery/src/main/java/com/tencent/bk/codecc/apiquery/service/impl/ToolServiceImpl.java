package com.tencent.bk.codecc.apiquery.service.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.apiquery.service.ToolService;
import com.tencent.bk.codecc.apiquery.task.dao.TaskDao;
import com.tencent.bk.codecc.apiquery.task.dao.mongotemplate.PlatformInfoDao;
import com.tencent.bk.codecc.apiquery.task.dao.mongotemplate.ToolDao;
import com.tencent.bk.codecc.apiquery.task.model.PlatformInfoModel;
import com.tencent.bk.codecc.apiquery.task.model.TaskInfoModel;
import com.tencent.bk.codecc.apiquery.task.model.ToolConfigInfoModel;
import com.tencent.bk.codecc.apiquery.utils.PageUtils;
import com.tencent.bk.codecc.apiquery.vo.ToolConfigPlatformVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OP工具管理服务接口实现
 *
 * @version V1.0
 * @date 2020/4/26
 */

@Slf4j
@Service
public class ToolServiceImpl implements ToolService
{
    @Autowired
    private ToolDao toolDao;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private PlatformInfoDao platformInfoDao;



    @Override
    public Page<ToolConfigPlatformVO> getPlatformInfoList(Long taskId, String toolName, String platformIp,
            Integer pageNum, Integer pageSize, String sortType)
    {
        // 排序分页(暂支持taskId排序)
        Pageable pageable = PageUtils.INSTANCE.convertPageSizeToPageable(pageNum, pageSize, "task_id", sortType);

        long totalCount = 0;
        int totalPage = 0;
        List<ToolConfigPlatformVO> infoList = Lists.newArrayList();

        Page<ToolConfigInfoModel> toolPage = toolDao.queryToolPlatformInfoPage(toolName, platformIp, taskId, pageable);
        if (toolPage != null)
        {
            totalCount = toolPage.getCount();
            totalPage = toolPage.getTotalPages();
            pageNum = toolPage.getPage();
            pageSize = toolPage.getPageSize();
            List<ToolConfigInfoModel> toolConfEntityList = toolPage.getRecords();

            if (CollectionUtils.isNotEmpty(toolConfEntityList))
            {
                Set<Long> taskIdSet =
                        toolConfEntityList.stream().map(ToolConfigInfoModel::getTaskId).collect(Collectors.toSet());
                List<TaskInfoModel> taskInfoModelList = taskDao.findByTaskIdIn(taskIdSet);

                Map<Long, TaskInfoModel> taskInfoEntityMap = taskInfoModelList.stream()
                        .collect(Collectors.toMap(TaskInfoModel::getTaskId, Function.identity(), (k, v) -> v));

                toolConfEntityList.forEach(entity ->
                {
                    long entityTaskId = entity.getTaskId();
                    String tool = entity.getToolName();
                    String ip = entity.getPlatformIp();

                    ToolConfigPlatformVO configPlatformVO = new ToolConfigPlatformVO();
                    configPlatformVO.setTaskId(entityTaskId);
                    configPlatformVO.setToolName(tool);

                    // 设置对应的Platform信息
                    if (StringUtils.isBlank(ip))
                    {
                        ip = "";
                    }
                    configPlatformVO.setIp(ip);

                    TaskInfoModel taskInfo = taskInfoEntityMap.get(entityTaskId);
                    configPlatformVO.setNameCn(taskInfo.getNameCn());
                    configPlatformVO.setNameEn(taskInfo.getNameEn());
                    infoList.add(configPlatformVO);
                });
            }
        }
        return new Page<>(totalCount, pageNum, pageSize, totalPage, infoList);
    }

    @Override
    public ToolConfigPlatformVO getTaskPlatformDetail(Long taskId, String toolName)
    {
        if (taskId == null || taskId == 0 || StringUtils.isBlank(toolName))
        {
            log.error("taskId or toolName is not allowed to be empty!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"taskId or toolName"}, null);
        }

        ToolConfigInfoModel toolConfigInfoModel = toolDao.findByTaskIdAndTool(taskId, toolName);
        if (toolConfigInfoModel == null)
        {
            log.error("findByTaskIdAndTool data is not found,task [{}] or tool [{}] is invalid!", taskId, toolName);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"taskId or toolName"}, null);
        }
        ToolConfigPlatformVO toolConfigPlatformVO = new ToolConfigPlatformVO();
        BeanUtils.copyProperties(toolConfigInfoModel, toolConfigPlatformVO);

        String platformIp = toolConfigInfoModel.getPlatformIp();
        String port = "";
        String userName = "";
        String passwd = "";
        if (StringUtils.isNotBlank(platformIp))
        {
            List<PlatformInfoModel> platformInfoModelList = platformInfoDao.findByToolNameAndIp(toolName, platformIp);
            if (CollectionUtils.isNotEmpty(platformInfoModelList))
            {
                PlatformInfoModel platformVO = platformInfoModelList.iterator().next();
                port = platformVO.getPort();
                userName = platformVO.getUserName();
                passwd = platformVO.getPasswd();
            }
        }
        toolConfigPlatformVO.setIp(platformIp);
        toolConfigPlatformVO.setPort(port);
        toolConfigPlatformVO.setUserName(userName);
        toolConfigPlatformVO.setPassword(passwd);

        TaskInfoModel taskInfoModel = taskDao.findTaskById(taskId);
        if (taskInfoModel != null)
        {
            toolConfigPlatformVO.setNameEn(taskInfoModel.getNameEn());
            toolConfigPlatformVO.setNameCn(taskInfoModel.getNameCn());
        }

        return toolConfigPlatformVO;
    }


}
