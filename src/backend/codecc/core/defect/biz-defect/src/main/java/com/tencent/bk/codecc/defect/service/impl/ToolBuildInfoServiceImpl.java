package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import com.tencent.devops.common.api.CodeRepoVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
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
        Integer scanType = analyzeConfigInfoVO.getScanType() == null ? ComConstants.ScanType.FULL.code : analyzeConfigInfoVO.getScanType();

        // 把接口传进来的scanType置空，确保返回的scanType一定表示的是强制全量，而不是用户设置的全量
        analyzeConfigInfoVO.setScanType(null);

        Long taskId = analyzeConfigInfoVO.getTaskId();
        String toolName = analyzeConfigInfoVO.getMultiToolType();
        String buildId = analyzeConfigInfoVO.getBuildId();
        ToolBuildInfoEntity toolBuildInfoEntity = toolBuildInfoRepository.findFirstByTaskIdAndToolName(taskId, toolName);
        if (null == toolBuildInfoEntity)
        {
            return analyzeConfigInfoVO;
        }

        // 加入上次扫描的仓库列表
        CodeRepoInfoEntity codeRepoInfoEntity = codeRepoRepository.findFirstByTaskIdAndBuildId(taskId, toolBuildInfoEntity.getDefectBaseBuildId());
        Set<String> lastRepoWhiteList = Sets.newHashSet();
        Set<String> lastRepoIds = Sets.newHashSet();
        Set<String> lastRepoUrls = Sets.newHashSet();
        if (codeRepoInfoEntity != null)
        {
            if (CollectionUtils.isNotEmpty(codeRepoInfoEntity.getRepoList()))
            {
                analyzeConfigInfoVO.setLastCodeRepos(Lists.newArrayList());
                for (CodeRepoEntity codeRepoEntity : codeRepoInfoEntity.getRepoList())
                {
                    CodeRepoVO codeRepoVO = new CodeRepoVO();
                    BeanUtils.copyProperties(codeRepoEntity, codeRepoVO);
                    analyzeConfigInfoVO.getLastCodeRepos().add(codeRepoVO);
                    lastRepoIds.add(codeRepoEntity.getRepoId());
                    lastRepoUrls.add(PathUtils.formatRepoUrlToHttp(codeRepoEntity.getUrl()));
                }
            }
            if (CollectionUtils.isNotEmpty(codeRepoInfoEntity.getRepoWhiteList()))
            {
                lastRepoWhiteList.addAll(codeRepoInfoEntity.getRepoWhiteList());
            }
        }

        // 如果设置了强制全量扫描标志，则本次全量扫描
        if (ComConstants.CommonJudge.COMMON_Y.value().equals(toolBuildInfoEntity.getForceFullScan()))
        {
            analyzeConfigInfoVO.setScanType(ComConstants.ScanType.FULL.code);
        }
        // 流水线任务增量要判断代码库和白名单是否有变化, 如果修改过代码仓库列表或修改过扫描目录白名单，则本次全量扫描
        else
        {
            String atomCode = analyzeConfigInfoVO.getAtomCode();
            List<String> repoWhiteList = CollectionUtils.isEmpty(analyzeConfigInfoVO.getRepoWhiteList()) ? Lists.newArrayList() : analyzeConfigInfoVO.getRepoWhiteList();
            if (!CollectionUtils.isEqualCollection(lastRepoWhiteList, repoWhiteList))
            {
                log.info("repoWhiteList has changed, taskId: {}, toolName: {}, buildId: {}", taskId, toolName, buildId);
                analyzeConfigInfoVO.setScanType(ComConstants.ScanType.FULL.code);
            }
            // V1、V2插件通过"repoIds"参数传递"代码仓库repoId列表"
            else if (StringUtils.isEmpty(atomCode) || ComConstants.AtomCode.CODECC_V2.code().equalsIgnoreCase(atomCode))
            {
                List<String> repoIds = CollectionUtils.isEmpty(analyzeConfigInfoVO.getRepoIds()) ? Lists.newArrayList() : analyzeConfigInfoVO.getRepoIds();
                if (!CollectionUtils.isEqualCollection(lastRepoIds, repoIds))
                {
                    log.info("repoIds has changed, taskId: {}, toolName: {}, buildId: {}", taskId, toolName, buildId);
                    analyzeConfigInfoVO.setScanType(ComConstants.ScanType.FULL.code);
                }
            }
            // V3插件通过参数"codeRepos"传递"本次扫描的代码仓库列表"
            else
            {
                Set<String> reqRepoUrls = CollectionUtils.isEmpty(analyzeConfigInfoVO.getCodeRepos()) ? Sets.newHashSet() :
                        analyzeConfigInfoVO.getCodeRepos().stream().map(codeRepoVO -> PathUtils.formatRepoUrlToHttp(codeRepoVO.getUrl())).collect(Collectors.toSet());
                if (!CollectionUtils.isEqualCollection(lastRepoUrls, reqRepoUrls))
                {
                    log.info("reqRepoUrls has changed, taskId: {}, toolName: {}, buildId: {}", taskId, toolName, buildId);
                    analyzeConfigInfoVO.setScanType(ComConstants.ScanType.FULL.code);
                }
            }
        }

        scanType = analyzeConfigInfoVO.getScanType() == null ? scanType : analyzeConfigInfoVO.getScanType();
        ToolBuildStackEntity toolBuildStackEntity = toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, analyzeConfigInfoVO.getBuildId());
        if (toolBuildStackEntity == null)
        {
            // 保存构建运行时栈表
            log.info("set tool build stack, taskId:{}, toolNames:{}, scanType: {}", analyzeConfigInfoVO.getTaskId(), analyzeConfigInfoVO.getMultiToolType(), scanType);
            toolBuildStackEntity = new ToolBuildStackEntity();
            toolBuildStackEntity.setTaskId(taskId);
            toolBuildStackEntity.setToolName(toolName);
            toolBuildStackEntity.setBuildId(analyzeConfigInfoVO.getBuildId());
            toolBuildStackEntity.setBaseBuildId(toolBuildInfoEntity.getDefectBaseBuildId());
            toolBuildStackEntity.setFullScan(scanType == ComConstants.ScanType.FULL.code || scanType == ComConstants.ScanType.DIFF_MODE.code);
            toolBuildStackDao.upsert(toolBuildStackEntity);
        }

        log.info("get build info finish, taskId: {}, toolName: {}, buildId: {}, scanType: {}", taskId, toolName, buildId, analyzeConfigInfoVO.getScanType());
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

        setForceFullScan(taskId, toolNames);

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
