package authz

import (
	"disaptch-k8s-manager/pkg/types"

	corev1 "k8s.io/api/core/v1"
)

// BuildLessTaskView 是 claim 接口对外视图：只保留非敏感调度字段。
type BuildLessTaskView struct {
	ProjectID      string `json:"projectId"`
	PipelineID     string `json:"pipelineId"`
	BuildID        string `json:"buildId"`
	VmSeqID        int    `json:"vmSeqId"`
	ExecutionCount int    `json:"executionCount"`
}

// SanitizeBuildLessTask 去掉 secretKey、agentId 等凭据，阻断跨租户字段级泄露。
func SanitizeBuildLessTask(task *types.BuildLessTask) *BuildLessTaskView {
	if task == nil {
		return nil
	}
	return &BuildLessTaskView{
		ProjectID:      task.ProjectId,
		PipelineID:     task.PipelineId,
		BuildID:        task.BuildId,
		VmSeqID:        task.VmSeqId,
		ExecutionCount: task.ExecutionCount,
	}
}

// AuthorizeBuildLessClaim 若调用方声明了项目/租户，则必须与任务属主一致。
func AuthorizeBuildLessClaim(caller Caller, task *types.BuildLessTask) error {
	if task == nil {
		return nil
	}
	if caller.IsEmpty() {
		return nil
	}
	return AuthorizeObject(caller, Owner{ProjectID: task.ProjectId})
}

// SanitizeSecret 去掉 Data / StringData，避免完整凭据出站。
func SanitizeSecret(secret *corev1.Secret) *corev1.Secret {
	if secret == nil {
		return nil
	}
	out := secret.DeepCopy()
	out.Data = nil
	out.StringData = nil
	if out.Annotations == nil {
		out.Annotations = map[string]string{}
	}
	out.Annotations["bkci.dispatch.kubernetes/secret-data-redacted"] = "true"
	return out
}
