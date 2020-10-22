package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.ServiceGongfengTaskRestResource;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineReq;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineRsp;
import com.tencent.bk.codecc.task.service.GongfengPublicProjService;
import com.tencent.bk.codecc.task.service.GongfengTaskService;
import com.tencent.bk.codecc.task.service.GongfengTriggerService;
import com.tencent.bk.codecc.task.vo.CustomProjVO;
import com.tencent.bk.codecc.task.vo.GongfengPublicProjVO;
import com.tencent.bk.codecc.task.vo.gongfeng.ProjectStatVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.auth.pojo.GongfengBaseInfo;
import com.tencent.devops.common.auth.service.GongfengAuthTaskService;
import com.tencent.devops.common.web.RestResource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestResource
public class ServiceGongfengTaskRestResourceImpl implements ServiceGongfengTaskRestResource {
    @Autowired
    private GongfengPublicProjService gongfengPublicProjService;

    @Autowired
    private GongfengTriggerService gongfengTriggerService;

    @Autowired
    private GongfengAuthTaskService authTaskService;

    @Autowired
    private GongfengTaskService taskService;

    @Override
    public CodeCCResult<String> getGongfengRepoUrl(Long taskId)
    {
        return new CodeCCResult<>(gongfengPublicProjService.getGongfengUrl(taskId));
    }

    @Override
    public CodeCCResult<Map<Integer, GongfengPublicProjVO>> getGongfengProjInfo(Collection<Integer> gfProjectId)
    {
        return new CodeCCResult<>(gongfengPublicProjService.queryGongfengProjectMapById(gfProjectId));
    }

    @Override
    public CodeCCResult<Boolean> syncGongfengStatProj(Integer bgId)
    {
        return new CodeCCResult<>(gongfengPublicProjService.saveStatProject(bgId));
    }

    @Override
    public CodeCCResult<Map<Integer, ProjectStatVO>> getGongfengStatProjInfo(Integer bgId, Collection<Integer> gfProjectId)
    {
        return new CodeCCResult<>(gongfengPublicProjService.queryGongfengStatProjectById(bgId, gfProjectId));
    }

    @Override
    public CodeCCResult<TriggerPipelineRsp> triggerCustomPipelineNew(TriggerPipelineReq triggerPipelineReq, String appCode, String userId)
    {
        return new CodeCCResult<>(gongfengTriggerService.triggerCustomProjectPipeline(triggerPipelineReq, appCode, userId));
    }

    @Override
    public CodeCCResult<Page<CustomProjVO>> batchGetCustomTaskList(QueryTaskListReqVO reqVO)
    {
        return new CodeCCResult<>(gongfengPublicProjService.queryCustomTaskByPageable(reqVO));
    }

    @Override
    public CodeCCResult<GongfengBaseInfo> getGongfengBaseInfo(Long taskId)
    {
        return new CodeCCResult<>(authTaskService.getGongfengProjInfo(taskId));
    }

    @Override
    public CodeCCResult<GongfengBaseInfo> getGongfengCIBaseInfo(Integer gongfengId) {
        return new CodeCCResult<>(authTaskService.getGongfengCIProjInfo(gongfengId));
    }

    @Override
    public CodeCCResult<Map<Long, GongfengPublicProjVO>> getGongfengProjInfoByTaskId(List<Long> taskId) {
        if (CollectionUtils.isEmpty(taskId)) {
            return new CodeCCResult<>(null);
        }
        return new CodeCCResult<>(gongfengPublicProjService.queryGongfengProjectMapByTaskId(taskId));
    }

    @Override
    public CodeCCResult<List<Long>> getBkPluginTaskIds() {
        return new CodeCCResult<>(taskService.getBkPluginTaskIds());
    }
}
