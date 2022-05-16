package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.mongorepository.CodeRepoInfoRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildStackRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.ToolBuildStackDao;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.service.UploadRepositoriesService;
import com.tencent.devops.common.api.CodeRepoVO;
import com.tencent.bk.codecc.defect.vo.coderepository.UploadRepositoriesVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 上报仓库信息服务实现类
 *
 * @version V1.0
 * @date 2019/11/15
 */
@Slf4j
@Service("uploadRepositoriesService")
public class UploadRepositoriesServiceImpl implements UploadRepositoriesService
{
    @Autowired
    private CodeRepoInfoRepository codeRepoRepository;
    @Autowired
    private ToolBuildStackRepository toolBuildStackRepository;
    @Autowired
    private ToolBuildInfoRepository toolBuildInfoRepository;
    @Autowired
    private ToolBuildStackDao toolBuildStackDao;

    /**
     * 上报仓库信息
     *
     * @param uploadRepositoriesVO
     * @return
     */
    @Override
    public Result uploadRepositories(UploadRepositoriesVO uploadRepositoriesVO)
    {
        log.info("upload repo info, task id: {}, tool name: {}", uploadRepositoriesVO.getTaskId(), uploadRepositoriesVO.getToolName());
        long taskId = uploadRepositoriesVO.getTaskId();
        String toolName = uploadRepositoriesVO.getToolName();
        String buildId = uploadRepositoriesVO.getBuildId();
        List<CodeRepoVO> codeRepoList = uploadRepositoriesVO.getRepoList();
        List<String> deleteFiles = uploadRepositoriesVO.getDeleteFiles();
        List<String> repoWhiteList = uploadRepositoriesVO.getRepoWhiteList();

        // 更新构建运行时栈表
        ToolBuildStackEntity toolBuildStackEntity = toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        if (toolBuildStackEntity == null)
        {
            ToolBuildInfoEntity toolBuildInfoEntity = toolBuildInfoRepository.findFirstByTaskIdAndToolName(taskId, toolName);
            toolBuildStackEntity = new ToolBuildStackEntity();
            toolBuildStackEntity.setTaskId(taskId);
            toolBuildStackEntity.setToolName(toolName);
            toolBuildStackEntity.setBuildId(buildId);
            toolBuildStackEntity.setBaseBuildId(toolBuildInfoEntity != null ? toolBuildInfoEntity.getDefectBaseBuildId() : "");
            toolBuildStackEntity.setFullScan(true);
        }
        toolBuildStackEntity.setDeleteFiles(deleteFiles);
        toolBuildStackDao.upsert(toolBuildStackEntity);

        // 校验构建号对应的仓库信息是否已存在
        CodeRepoInfoEntity codeRepoInfo = codeRepoRepository.findFirstByTaskIdAndBuildId(taskId, buildId);
        if (codeRepoInfo != null)
        {
            return new Result(0, CommonMessageCode.SUCCESS, "repo info of this build id is already exist.");
        }

        // 更新仓库列表和构建ID
        List<CodeRepoEntity> codeRepoEntities = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(codeRepoList))
        {
            for (CodeRepoVO codeRepoVO : codeRepoList)
            {
                CodeRepoEntity codeRepoEntity = new CodeRepoEntity();
                BeanUtils.copyProperties(codeRepoVO, codeRepoEntity);
                codeRepoEntities.add(codeRepoEntity);
            }
        }
        codeRepoInfo = new CodeRepoInfoEntity(taskId, buildId, codeRepoEntities, repoWhiteList, deleteFiles);
        Long currentTime = System.currentTimeMillis();
        codeRepoInfo.setUpdatedDate(currentTime);
        codeRepoInfo.setCreatedDate(currentTime);
        codeRepoRepository.save(codeRepoInfo);

        return new Result(0, CommonMessageCode.SUCCESS, "upload repo info success.");
    }
}
