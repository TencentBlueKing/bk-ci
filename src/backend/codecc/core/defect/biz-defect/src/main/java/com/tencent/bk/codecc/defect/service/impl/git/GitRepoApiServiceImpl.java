package com.tencent.bk.codecc.defect.service.impl.git;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.codecc.defect.component.ScmJsonComponent;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.model.BuildDefectEntity;
import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.bk.codecc.defect.service.git.GitRepoApiService;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.util.DateTimeUtil;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.pipeline.enums.ChannelCode;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.GitUtil;
import com.tencent.devops.common.util.HttpPathUrlUtil;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.util.OkhttpUtils;
import com.tencent.devops.process.api.service.ServiceBuildResource;
import com.tencent.devops.process.pojo.BuildHistoryVariables;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GitRepoApiServiceImpl implements GitRepoApiService {

    @Value("${git.host:}")
    private String gitHost;

    @Value("${scm.git.file.token:1719ff6db1b1afb00e4f01c839260eab}")
    private String gitFileToken;

    @Value("${bkci.public.url:#{null}}")
    protected String devopsHost;

    private static final String GIT_CODECC_TYPE = "ieg_codecc";

    private String CCN_CHECKER = "CCN_threshold";

    private int CODECC_REQUEST_BATCH_SIZE = 500;

    private int MAX_TITLE_LENGTH = 255;

    // git severity : 1-ERROR 2-WARN 4-INFO
    private static Map<Integer, Integer> codeccLintGitSeverityMap = new HashMap<Integer, Integer>()
    {{
        put(ComConstants.SERIOUS, 1);
        put(ComConstants.NORMAL, 2);
        put(ComConstants.PROMPT_IN_DB, 4);
        put(ComConstants.PROMPT, 4);
    }};

    private static Map<Integer, Integer> codeccCcnGitSeverityMap = new HashMap<Integer, Integer>()
    {{
        put(ComConstants.RiskFactor.SH.value(), 1);
        put(ComConstants.RiskFactor.H.value(), 2);
        put(ComConstants.RiskFactor.M.value(), 4);
        put(ComConstants.RiskFactor.L.value(), 4);
    }};

    @Autowired
    private ScmJsonComponent scmJsonComponent;

    @Autowired
    private CheckerRepository checkerRepository;

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;

    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Autowired
    private BuildDefectRepository buildDefectRepository;

    @Autowired
    private TaskLogService taskLogService;

    @Autowired
    private Client client;

    @Autowired
    private CheckerService checkerService;

    @Override
    @Async("asyncTaskExecutor")
    public void addLintGitCodeAnalyzeComment(TaskDetailVO taskDetailVO, String buildId, String buildNum, String toolName, Set<String> currentFileSet)
    {
        if (!preCheck(taskDetailVO, buildId, toolName, currentFileSet))
        {
            return;
        }

        Set<String> defectIds = getDefectIdsByBuildId(taskDetailVO.getTaskId(), toolName, buildId);

        List<LintDefectV2Entity> currentLintFileList = lintDefectV2Repository.findByEntityIdIn(defectIds);

        String displayName = toolMetaCacheService.getToolDisplayName(toolName);

        // 获取规则列表
        List<CheckerDetailEntity> checkerDetailEntityList = checkerRepository.findByToolName(toolName);
        Map<String, String> checkerTypeMap = checkerDetailEntityList.stream()
            .filter((it) -> it != null && it.getCheckerKey() != null && it.getCheckerType() != null)
            .collect(Collectors.toMap(CheckerDetailEntity::getCheckerKey, CheckerDetailEntity::getCheckerType));

        List<ScanFileRequest> requestList = new ArrayList<>();
        currentLintFileList.forEach(defectEntity ->
            {
                if (preEntityCheck(defectEntity.getUrl(), defectEntity.getRelPath(), taskDetailVO.getTaskId()))
                {
                    ScanFileRequest request = getScanFileRequest(defectEntity.getUrl(),
                        defectEntity.getRelPath(),
                        defectEntity.getBranch(),
                        defectEntity.getAuthor(),
                        toolName,
                        Integer.parseInt(buildNum));
                    String title = escapeHtml(defectEntity.getMessage());

                    request.setStartLine(defectEntity.getLineNum());
                    request.setEndLine(defectEntity.getLineNum());
                    request.setTitle(title.substring(0, Math.min(MAX_TITLE_LENGTH, title.length())));
                    request.setDescription(getCheckerCodeDesc(defectEntity, displayName, checkerTypeMap, buildNum));
                    request.setTargetUrl(HttpPathUrlUtil.getLintTargetUrl(devopsHost, taskDetailVO.getProjectId(), taskDetailVO.getTaskId(),
                        toolName, defectEntity.getEntityId()));
                    request.setSeverity(getLintSeverity(defectEntity.getSeverity()));
                    request.setCommitId(getMrCommit(taskDetailVO.getNameEn(), buildId));

                    requestList.add(request);

                    if (requestList.size() == CODECC_REQUEST_BATCH_SIZE)
                    {
                        doMrCommentHttp(taskDetailVO, buildId, requestList);
                        requestList.clear();
                    }
                }
            });

        if (CollectionUtils.isNotEmpty(requestList))
        {
            doMrCommentHttp(taskDetailVO, buildId, requestList);
        }
    }

    @Override
    @Async("asyncTaskExecutor")
    public void addCcnGitCodeAnalyzeComment(TaskDetailVO taskDetailVO, String buildId, String buildNum, String toolName, Set<String> currentFileSet)
    {
        if (!preCheck(taskDetailVO, buildId, toolName, currentFileSet))
        {
            return;
        }

        Set<String> defectIds = getCcnDefectIdsByBuildId(taskDetailVO.getTaskId(), toolName, buildId);

        List<CCNDefectEntity> currentLintFileList = ccnDefectRepository.findByEntityIdIn(defectIds);

        ToolConfigInfoVO toolConfigInfoVO = taskDetailVO.getToolConfigInfoList()
                .stream()
                .filter(toolConfig -> toolConfig.getToolName().equalsIgnoreCase(ComConstants.Tool.CCN.name()))
                .findAny()
                .orElseGet(ToolConfigInfoVO::new);
        int ccnThreshold = checkerService.getCcnThreshold(toolConfigInfoVO);

        List<ScanFileRequest> requestList = new ArrayList<>();
        currentLintFileList.forEach((defectEntity) -> {
            if (preEntityCheck(defectEntity.getUrl(), defectEntity.getRelPath(), taskDetailVO.getTaskId()))
            {
                ScanFileRequest request = getScanFileRequest(defectEntity.getUrl(),
                    defectEntity.getRelPath(),
                    defectEntity.getBranch(),
                    defectEntity.getAuthor(),
                    toolName,
                    Integer.parseInt(buildNum));
                request.setStartLine(defectEntity.getStartLines());
                request.setEndLine(defectEntity.getEndLines());
                request.setTitle(String.format("圈复杂度为%s，超过%s的建议值，请进行函数功能拆分降低代码复杂度。", defectEntity.getCcn(), ccnThreshold));
                request.setDescription(getCcnCheckerCodeDesc(defectEntity, buildNum));
                request.setTargetUrl(HttpPathUrlUtil.getCcnTargetUrl(devopsHost, taskDetailVO.getProjectId(), taskDetailVO.getTaskId(),
                    toolName, defectEntity.getEntityId(), defectEntity.getFilePath()));
                request.setSeverity(getCcnSeverity(defectEntity.getRiskFactor()));
                request.setCommitId(getMrCommit(taskDetailVO.getNameEn(), buildId));
                requestList.add(request);

                if (requestList.size() == CODECC_REQUEST_BATCH_SIZE)
                {
                    doMrCommentHttp(taskDetailVO, buildId, requestList);
                    requestList.clear();
                }
            }
        });

        if (CollectionUtils.isNotEmpty(requestList))
        {
            doMrCommentHttp(taskDetailVO, buildId, requestList);
        }
    }

    private String getMrCommit(String nameEn, String buildId) {
        TaskDetailVO taskInfo = client.get(ServiceTaskRestResource.class).getTaskInfo(nameEn).getData();
        BuildHistoryVariables vars = client.getDevopsService(ServiceBuildResource.class).getBuildVars("ADMIN", taskInfo.getProjectId(), taskInfo.getPipelineId(), buildId, ChannelCode.CODECC).getData();
        if (vars != null) {
            return vars.getVariables().get("BK_CI_REPO_GIT_WEBHOOK_COMMITID");
        }
        log.error("can not find the BK_CI_REPO_GIT_WEBHOOK_COMMITID for task: {}, {}", nameEn, buildId);
        return "";
    }

    private String escapeHtml(String message) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < message.length(); i++)
        {
            // 只对ASCII 特殊字符做转换
            char ch = message.charAt(i);
            if (ch < '0' || ch > '9' && ch < 'A' || ch > 'Z' && ch <'a' || ch > 'z' && ch <= '~')
            {
                sb.append(StringEscapeUtils.escapeHtml(String.valueOf(ch)));
            }
            else
            {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    protected Set<String> getDefectIdsByBuildId(long taskId, String toolName, String buildId)
    {
        Set<String> defectIdSet = new HashSet<>();
        List<BuildDefectEntity> buildFiles = buildDefectRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        if (CollectionUtils.isNotEmpty(buildFiles))
        {
            for (BuildDefectEntity buildDefectEntity : buildFiles)
            {
                if (CollectionUtils.isNotEmpty(buildDefectEntity.getFileDefectIds()))
                {
                    defectIdSet.addAll(buildDefectEntity.getFileDefectIds());
                }
            }
        }
        return defectIdSet;
    }

    protected Set<String> getCcnDefectIdsByBuildId(long taskId, String toolName, String buildId)
    {
        Set<String> defectIdSet = new HashSet<>();
        List<BuildDefectEntity> buildFiles = buildDefectRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        if (CollectionUtils.isNotEmpty(buildFiles))
        {
            for (BuildDefectEntity buildDefectEntity : buildFiles)
            {
                defectIdSet.add(buildDefectEntity.getDefectId());
            }
        }
        return defectIdSet;
    }

    private boolean preEntityCheck(String url, String relPath, Long taskId) {
        if (!url.contains(".git")) {
            log.info("url is not a git repo, ignore: {}, {}", url, taskId);
            return false;
        }

        if (StringUtils.isBlank(relPath)) {
            log.warn("add git code analyze comment rel path is blank for {}, {}", taskId, relPath);
            return false;
        }

        return true;
    }

    private ScanFileRequest getScanFileRequest(String url, String relPath, String branch, String author, String toolName, int buildNo) {
        ScanFileRequest request = new ScanFileRequest();

        String filePath = relPath;
        while (filePath.startsWith("/"))
        {
            filePath = StringUtils.removeStart(filePath, "/");
        }

        request.setProjectPath(GitUtil.INSTANCE.getProjectName(url));
        request.setFilePath(filePath);
        request.setBranch(branch);
        request.setType(GIT_CODECC_TYPE);
        request.setTypeFrom(toolName);
        request.setOwners(author);
        request.setVersion(buildNo);
        request.setScope("line");
        return request;
    }

    private Boolean preCheck(TaskDetailVO taskDetailVO, String buildId, String toolName, Set<String> currentFileSet) {
        if (taskDetailVO.getMrCommentEnable() != null && !taskDetailVO.getMrCommentEnable())
        {
            log.info("no enable mr comment, do not add git code analyze comment, {}, {}", taskDetailVO.getTaskId(), buildId);
            return false;
        }

        if (taskDetailVO.getScanType() != null && taskDetailVO.getScanType() != ComConstants.ScanType.DIFF_MODE.code) {
            log.info("no diff mode, do not add git code analyze comment, {}, {}", taskDetailVO.getTaskId(), buildId);
            return false;
        }

        log.info("start to add {} git code analyze comment: {}, {}, {}", toolName, taskDetailVO.getTaskId(), buildId, currentFileSet.size());
        return true;
    }

    private int getLintSeverity(int severity) {
        if (codeccLintGitSeverityMap.containsKey(severity)) return codeccLintGitSeverityMap.get(severity);
        return severity;
    }

    private int getCcnSeverity(int severity) {
        if (codeccCcnGitSeverityMap.containsKey(severity)) return codeccCcnGitSeverityMap.get(severity);
        return severity;
    }

    private void doMrCommentHttp(TaskDetailVO taskDetailVO, String buildId, List<ScanFileRequest> scanFileRequests) {
        if (CollectionUtils.isEmpty(scanFileRequests))
        {
            log.info("scan file request is empty for task: {}, {}", taskDetailVO.getTaskId(), buildId);
            return;
        }
        log.info("do add git code analyze comment! task id: {}, build id: {}, body: {}", taskDetailVO.getTaskId(), buildId, scanFileRequests.get(0));

        String url = String.format("%s/api/v3/scan/files/sfz6emok?access_token=%s", gitHost, gitFileToken);
        String json = JsonUtil.INSTANCE.toJson(scanFileRequests);
        String responseBody = OkhttpUtils.INSTANCE.doHttpPost(url, json, new HashMap<>());
        log.info("do add git code analyze comment success! response: {}, {}", taskDetailVO.getTaskId(), responseBody);
    }

    private String getCheckerCodeDesc(LintDefectV2Entity defect, String toolName, Map<String, String> checkerTypeMap, String buildNum)
    {
        return escapeHtml(String.format("%s | %s | %s",
            defect.getChecker(),
            checkerTypeMap.get(defect.getChecker()),
            toolName)) +
            String.format("&nbsp;&nbsp;&nbsp;&nbsp;%s&nbsp;&nbsp;&nbsp;&nbsp;%s #%s创建",
            defect.getAuthor(),
            DateTimeUtil.INSTANCE.formatDate(defect.getCreateTime(), "yyyy-MM-dd"),
            buildNum);
    }

    private String getCcnCheckerCodeDesc(CCNDefectEntity defectEntity, String buildNum)
    {
        return String.format("圈复杂度&nbsp;&nbsp;&nbsp;&nbsp;%s&nbsp;&nbsp;&nbsp;&nbsp;%s #%s创建",
            defectEntity.getAuthor(),
            DateTimeUtil.INSTANCE.formatDate(defectEntity.getCreateTime(), "yyyy-MM-dd"),
            buildNum);
    }

    @Data
    static class ScanFileRequest
    {
        private String platform;

        @JsonProperty("project_path")
        private String projectPath;

        @JsonProperty("file_path")
        private String filePath;

        @JsonProperty("start_line")
        private Integer startLine;

        @JsonProperty("end_line")
        private Integer endLine;

        @JsonProperty("function_name")
        private String functionName;

        private String title;

        private String description;

        private String branch;

        @JsonProperty("commit_id")
        private String commitId;

        private String type; // codecc

        @JsonProperty("type_from")
        private String typeFrom; // 工具名称

        @JsonProperty("target_url")
        private String targetUrl;

        private String owners;

        @JsonProperty("line_owners")
        private String lineOwners;

        private String scope; // file, line

        private Integer version;

        private Integer severity;
    }
}
