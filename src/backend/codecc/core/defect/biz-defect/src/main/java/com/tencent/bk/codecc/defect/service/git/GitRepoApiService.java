package com.tencent.bk.codecc.defect.service.git;

import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;

import java.util.Map;
import java.util.Set;

public interface GitRepoApiService {

    void addLintGitCodeAnalyzeComment(TaskDetailVO taskDetailVO, String buildId, String buildNum, String toolName, Set<String> currentFileSet);

    void addCcnGitCodeAnalyzeComment(TaskDetailVO taskDetailVO, String buildId, String buildNum, String toolName, Set<String> currentFileSet);

}
