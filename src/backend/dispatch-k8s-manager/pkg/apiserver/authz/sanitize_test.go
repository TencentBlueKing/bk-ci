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

func TestSanitizeBuildLessTaskDropsCredentials(t *testing.T) {
	task := &types.BuildLessTask{
		ProjectId:      "proj-a",
		AgentId:        "agent-secret-id",
		PipelineId:     "p-1",
		BuildId:        "b-1",
		VmSeqId:        1,
		SecretKey:      "super-secret-key",
		ExecutionCount: 2,
	}
	view := SanitizeBuildLessTask(task)
	body, err := json.Marshal(view)
	assert.NoError(t, err)
	assert.NotContains(t, string(body), "super-secret-key")
	assert.NotContains(t, string(body), "agent-secret-id")
	assert.NotContains(t, strings.ToLower(string(body)), "secretkey")
	assert.NotContains(t, strings.ToLower(string(body)), "agentid")
	assert.Equal(t, "proj-a", view.ProjectID)
	assert.Equal(t, "b-1", view.BuildID)
}

func TestAuthorizeBuildLessClaim(t *testing.T) {
	task := &types.BuildLessTask{ProjectId: "proj-a"}
	assert.NoError(t, AuthorizeBuildLessClaim(Caller{}, task))
	assert.NoError(t, AuthorizeBuildLessClaim(Caller{UserID: "alice", ProjectID: "proj-a"}, task))
	assert.ErrorIs(t, AuthorizeBuildLessClaim(Caller{UserID: "bob", ProjectID: "proj-b"}, task), ErrForbidden)
}

func TestSanitizeSecretDropsData(t *testing.T) {
	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Name: "s1", Namespace: "ns-a"},
		Data:       map[string][]byte{"token": []byte("abcd")},
		StringData: map[string]string{"password": "p@ss"},
	}
	out := SanitizeSecret(secret)
	assert.Nil(t, out.Data)
	assert.Nil(t, out.StringData)
	assert.Equal(t, "s1", out.Name)
	assert.Equal(t, "true", out.Annotations["bkci.dispatch.kubernetes/secret-data-redacted"])
	assert.Equal(t, []byte("abcd"), secret.Data["token"])
}
