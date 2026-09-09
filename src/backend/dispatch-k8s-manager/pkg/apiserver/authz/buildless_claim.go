package authz

import (
	"regexp"
	"strings"

	"disaptch-k8s-manager/pkg/types"

	corev1 "k8s.io/api/core/v1"
)

const (
	BuildLessPoolLabelKey = "bkci.dispatch.kubenetes/buildless"
	BuildLessPoolLabelVal = "buildless-pool"
	BuildLessJobPoolValue = "K8S_BUILD_LESS"
	EnvPodName            = "pod_name"
	EnvRandomStr          = "random_str"
	EnvJobPool            = "JOB_POOL"
	buildLessRandomLen    = 16
)

var buildLessRandomPattern = regexp.MustCompile(`^[A-Za-z0-9]{16}$`)

// ParseBuildLessPodID 解析 claim 使用的 podId = {pod.metadata.name}-{random_str}。
func ParseBuildLessPodID(podID string) (podName, randomStr string, ok bool) {
	podID = strings.TrimSpace(podID)
	if podID == "" {
		return "", "", false
	}
	idx := strings.LastIndex(podID, "-")
	if idx <= 0 || idx == len(podID)-1 {
		return "", "", false
	}
	podName = podID[:idx]
	randomStr = podID[idx+1:]
	if podName == "" || !buildLessRandomPattern.MatchString(randomStr) {
		return "", "", false
	}
	return podName, randomStr, true
}

// VerifyBuildLessPodBinding 校验调用方是否为当前预热池中的合法认领 pod。
// 依据：pod 存在、名称匹配、buildless-pool 标签、JOB_POOL，以及 env.random_str 与 podId 后缀一致。
func VerifyBuildLessPodBinding(podID string, pod *corev1.Pod) bool {
	podName, randomStr, ok := ParseBuildLessPodID(podID)
	if !ok || pod == nil {
		return false
	}
	if pod.Name != podName {
		return false
	}
	if pod.Labels[BuildLessPoolLabelKey] != BuildLessPoolLabelVal {
		return false
	}
	env := envMapFromPod(pod)
	if env[EnvJobPool] != "" && env[EnvJobPool] != BuildLessJobPoolValue {
		return false
	}
	if env[EnvRandomStr] != randomStr {
		return false
	}
	if env[EnvPodName] != "" && env[EnvPodName] != podName {
		return false
	}
	return true
}

// ShouldRevealBuildLessCredentials 仅当强绑定通过且租户/项目一致（若调用方声明了身份）时下发凭据。
func ShouldRevealBuildLessCredentials(caller Caller, task *types.BuildLessTask, podID string, pod *corev1.Pod) bool {
	if task == nil {
		return false
	}
	if !VerifyBuildLessPodBinding(podID, pod) {
		return false
	}
	if err := AuthorizeBuildLessClaim(caller, task); err != nil {
		return false
	}
	return true
}

// BuildLessClaimPayload 合法认领返回完整任务（含 agentId/secretKey）；否则只给脱敏字段。
func BuildLessClaimPayload(task *types.BuildLessTask, reveal bool) interface{} {
	if task == nil {
		return nil
	}
	if reveal {
		return task
	}
	return SanitizeBuildLessTask(task)
}

func envMapFromPod(pod *corev1.Pod) map[string]string {
	out := map[string]string{}
	if pod == nil {
		return out
	}
	for _, c := range pod.Spec.Containers {
		for _, e := range c.Env {
			if e.Value != "" {
				out[e.Name] = e.Value
			}
		}
	}
	return out
}
