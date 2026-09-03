package authz

import (
	"encoding/json"
	"strings"
	"testing"

	"disaptch-k8s-manager/pkg/types"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func sampleBuildLessTask() *types.BuildLessTask {
	return &types.BuildLessTask{
		ProjectId:      "proj-a",
		AgentId:        "agent-secret-id",
		PipelineId:     "p-1",
		BuildId:        "b-1",
		VmSeqId:        1,
		SecretKey:      "super-secret-key",
		ExecutionCount: 2,
	}
}

func samplePoolPod(name, randomStr string) *corev1.Pod {
	return &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name:   name,
			Labels: map[string]string{BuildLessPoolLabelKey: BuildLessPoolLabelVal},
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{{
				Env: []corev1.EnvVar{
					{Name: EnvJobPool, Value: BuildLessJobPoolValue},
					{Name: EnvRandomStr, Value: randomStr},
					{Name: EnvPodName, Value: name},
				},
			}},
		},
	}
}

func TestParseBuildLessPodID(t *testing.T) {
	name, nonce, ok := ParseBuildLessPodID("buildless-pool-abc-0123456789abcdef")
	assert.True(t, ok)
	assert.Equal(t, "buildless-pool-abc", name)
	assert.Equal(t, "0123456789abcdef", nonce)

	_, _, ok = ParseBuildLessPodID("")
	assert.False(t, ok)
	_, _, ok = ParseBuildLessPodID("onlyname")
	assert.False(t, ok)
	_, _, ok = ParseBuildLessPodID("pod-short")
	assert.False(t, ok)
	_, _, ok = ParseBuildLessPodID("pod-not_valid_chars!!")
	assert.False(t, ok)
}

func TestVerifyBuildLessPodBinding(t *testing.T) {
	pod := samplePoolPod("buildless-pool-abc", "0123456789abcdef")
	assert.True(t, VerifyBuildLessPodBinding("buildless-pool-abc-0123456789abcdef", pod))

	assert.False(t, VerifyBuildLessPodBinding("buildless-pool-abc-0123456789abcdef", nil))
	assert.False(t, VerifyBuildLessPodBinding("other-pod-0123456789abcdef", pod))
	assert.False(t, VerifyBuildLessPodBinding("buildless-pool-abc-ffffffffeeeeaaaa", pod))

	builderPod := samplePoolPod("buildless-pool-abc", "0123456789abcdef")
	builderPod.Labels = map[string]string{"app": "builder"}
	assert.False(t, VerifyBuildLessPodBinding("buildless-pool-abc-0123456789abcdef", builderPod))

	wrongPool := samplePoolPod("buildless-pool-abc", "0123456789abcdef")
	wrongPool.Spec.Containers[0].Env[0].Value = "DOCKER"
	assert.False(t, VerifyBuildLessPodBinding("buildless-pool-abc-0123456789abcdef", wrongPool))
}

func TestShouldRevealBuildLessCredentials(t *testing.T) {
	task := sampleBuildLessTask()
	pod := samplePoolPod("buildless-pool-abc", "0123456789abcdef")
	podID := "buildless-pool-abc-0123456789abcdef"

	assert.True(t, ShouldRevealBuildLessCredentials(Caller{}, task, podID, pod), "合法池化 pod 无身份头应下发凭据")
	assert.True(t, ShouldRevealBuildLessCredentials(Caller{UserID: "alice", ProjectID: "proj-a"}, task, podID, pod))

	assert.False(t, ShouldRevealBuildLessCredentials(Caller{}, task, "forged-pod-0123456789abcdef", pod), "伪造 podId")
	assert.False(t, ShouldRevealBuildLessCredentials(Caller{}, task, podID, nil), "未绑定/不存在的 pod")
	assert.False(t, ShouldRevealBuildLessCredentials(Caller{UserID: "bob", ProjectID: "proj-b"}, task, podID, pod), "跨租户")
	assert.False(t, ShouldRevealBuildLessCredentials(Caller{}, nil, podID, pod))
}

func TestBuildLessClaimPayloadRevealAndSanitize(t *testing.T) {
	task := sampleBuildLessTask()

	revealed, err := json.Marshal(BuildLessClaimPayload(task, true))
	assert.NoError(t, err)
	assert.Contains(t, string(revealed), "super-secret-key")
	assert.Contains(t, string(revealed), "agent-secret-id")

	redacted, err := json.Marshal(BuildLessClaimPayload(task, false))
	assert.NoError(t, err)
	assert.NotContains(t, string(redacted), "super-secret-key")
	assert.NotContains(t, string(redacted), "agent-secret-id")
	assert.NotContains(t, strings.ToLower(string(redacted)), "secretkey")
	assert.Nil(t, BuildLessClaimPayload(nil, true))
}
