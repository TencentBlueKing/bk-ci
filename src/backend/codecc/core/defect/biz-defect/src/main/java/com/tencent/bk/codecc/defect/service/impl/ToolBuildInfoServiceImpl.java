package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.mongorepository.CodeRepoInfoRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildStackRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.ToolBuildStackDao;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.service.ToolBuildInfoService;
import com.tencent.bk.codecc.defect.vo.SetForceFullScanReqVO;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.pipeline.CodeRepoInfoVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工具构建信息服务实现类
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Service
@Slf4j
public class ToolBuildInfoServiceImpl implements ToolBuildInfoService
{
    @Autowired
    private CodeRepoInfoRepository codeRepoRepository;

    @Autowired
    private ToolBuildInfoRepository toolBuildInfoRepository;
    @Autowired
    private ToolBuildStackRepository toolBuildStackRepository;
    @Autowired
    private ToolBuildInfoDao toolBuildInfoDao;

    @Autowired
    private ToolBuildStackDao toolBuildStackDao;

    /**
     * 查询工具构建信息
     *
     * @param analyzeConfigInfoVO
     * @return
     */
    @Override
    public AnalyzeConfigInfoVO getBuildInfo(AnalyzeConfigInfoVO analyzeConfigInfoVO)
    {
        Long taskId = analyzeConfigInfoVO.getTaskId();
        String toolName = analyzeConfigInfoVO.getMultiToolType();
        ToolBuildInfoEntity toolBuildInfoEntity = toolBuildInfoRepository.findByTaskIdAndToolName(taskId, toolName);
        if (null == toolBuildInfoEntity)
        {
            return analyzeConfigInfoVO;
        }

        // 加入上次扫描的仓库列表
        CodeRepoInfoEntity codeRepoInfoEntity = codeRepoRepository.findByTaskIdAndBuildId(taskId, toolBuildInfoEntity.getDefectBaseBuildId());
        List<String> lastRepoWhiteList = null;
        List<String> lastRepos = Lists.newArrayList();
        if (codeRepoInfoEntity != null)
        {
            if (CollectionUtils.isNotEmpty(codeRepoInfoEntity.getRepoList()))
            {
                analyzeConfigInfoVO.setLastCodeRepos(Lists.newArrayList());
                for (CodeRepoEntity codeRepoEntity : codeRepoInfoEntity.getRepoList())
                {
                    CodeRepoInfoVO codeRepoInfoVO = new CodeRepoInfoVO();
                    BeanUtils.copyProperties(codeRepoEntity, codeRepoInfoVO);
                    analyzeConfigInfoVO.getLastCodeRepos().add(codeRepoInfoVO);
                    lastRepos.add(codeRepoEntity.getRepoId());
                }
            }
            lastRepoWhiteList = codeRepoInfoEntity.getRepoWhiteList();
        }

        Integer scanType = analyzeConfigInfoVO.getScanType();
        // 如果设置了强制全量扫描标志，则本次全量扫描
        if (ComConstants.CommonJudge.COMMON_Y.value().equals(toolBuildInfoEntity.getForceFullScan()))
        {
            scanType = ComConstants.ScanType.FULL.code;
        }
        // 流水线任务增量要判断代码库和白名单是否有变化, 如果修改过代码仓库列表或修改过扫描目录白名单，则本次全量扫描
        else if (ComConstants.ScanType.INCREMENTAL.code == scanType && analyzeConfigInfoVO.isPipelineTask())
        {
            List<String> reqRepoIds = analyzeConfigInfoVO.getRepoIds() == null ? Lists.newArrayList() : analyzeConfigInfoVO.getRepoIds();
            lastRepoWhiteList = lastRepoWhiteList == null ? Lists.newArrayList() : lastRepoWhiteList;
            List<String> reqRepoWhiteList = analyzeConfigInfoVO.getRepoWhiteList() == null ? Lists.newArrayList() : analyzeConfigInfoVO.getRepoWhiteList();
            if (!ListUtils.isEqualList(lastRepos, reqRepoIds) || !ListUtils.isEqualList(lastRepoWhiteList, reqRepoWhiteList))
            {
                scanType = ComConstants.ScanType.FULL.code;
            }
        }
        analyzeConfigInfoVO.setScanType(scanType);

        ToolBuildStackEntity toolBuildStackEntity = toolBuildStackRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, analyzeConfigInfoVO.getBuildId());
        if (toolBuildStackEntity == null)
        {
            // 保存构建运行时栈表
            log.info("set force full scan, taskId:{}, toolNames:{}", analyzeConfigInfoVO.getTaskId(), analyzeConfigInfoVO.getMultiToolType());
            toolBuildStackEntity = new ToolBuildStackEntity();
            toolBuildStackEntity.setTaskId(taskId);
            toolBuildStackEntity.setToolName(toolName);
            toolBuildStackEntity.setBuildId(analyzeConfigInfoVO.getBuildId());
            toolBuildStackEntity.setBaseBuildId(toolBuildInfoEntity.getDefectBaseBuildId());
            toolBuildStackEntity.setFullScan(scanType == ComConstants.ScanType.FULL.code);
            toolBuildStackDao.upsert(toolBuildStackEntity);
        }


        return analyzeConfigInfoVO;
    }

    /**
     * 更新运行时栈表中的全量扫描标志位
     */
    @Override
    public Boolean setToolBuildStackFullScan(Long taskId, SetForceFullScanReqVO setForceFullScanReqVO)
    {
        log.info("begin setToolBuildStackFullScan: taskId={}, {}", taskId, setForceFullScanReqVO);
        String buildId = setForceFullScanReqVO.getLandunBuildId();
        List<String> toolNames = setForceFullScanReqVO.getToolNames();
        List<ToolBuildStackEntity> toolBuildStackEntitys = toolBuildStackRepository.findByTaskIdAndToolNameInAndBuildId(taskId, toolNames, buildId);
        if (CollectionUtils.isNotEmpty(toolBuildStackEntitys))
        {
            toolBuildStackEntitys = toolBuildStackEntitys.stream().filter(toolBuildStackEntity -> !toolBuildStackEntity.isFullScan())
                    .map(toolBuildStackEntity ->
                    {
                        toolBuildStackEntity.setFullScan(true);
                        return toolBuildStackEntity;
                    }).collect(Collectors.toList());
            toolBuildStackDao.batchUpsert(toolBuildStackEntitys);
        }
        log.info("end setToolBuildStackFullScan.");
        return true;
    }

    /**
     * 更新强制全量扫描标志位
     */
    @Override
    public Boolean setForceFullScan(Long taskId, List<String> toolNames)
    {
        if (CollectionUtils.isNotEmpty(toolNames))
        {
            for (String toolName : toolNames)
            {
                toolBuildInfoDao.setForceFullScan(taskId, toolName);
            }
        }
        return true;
    }
}
