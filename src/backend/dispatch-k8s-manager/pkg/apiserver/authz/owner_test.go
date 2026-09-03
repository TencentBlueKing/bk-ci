package authz

import (
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func TestAuthorizeObject(t *testing.T) {
	owner := Owner{UserID: "alice", ProjectID: "proj-a", TenantID: "t-a"}

	assert.ErrorIs(t, AuthorizeObject(Caller{}, owner), ErrMissingIdentity)
	assert.ErrorIs(t, AuthorizeObject(Caller{UserID: "alice", ProjectID: "proj-a"}, Owner{}), ErrObjectUnowned)
	assert.ErrorIs(t, AuthorizeObject(Caller{UserID: "bob", ProjectID: "proj-b", TenantID: "t-b"}, owner), ErrForbidden)
	assert.ErrorIs(t, AuthorizeObject(Caller{UserID: "alice", ProjectID: "proj-b"}, owner), ErrForbidden)
	assert.NoError(t, AuthorizeObject(Caller{UserID: "alice", ProjectID: "proj-a"}, owner))
	assert.NoError(t, AuthorizeObject(Caller{UserID: "other", TenantID: "t-a"}, owner))
	assert.NoError(t, AuthorizeObject(Caller{UserID: "carol", ProjectID: "proj-a"}, owner))
}

func TestAuthorizeDebugIssue_RequiresUserAndProject(t *testing.T) {
	owner := Owner{UserID: "alice", ProjectID: "proj-a"}
	assert.ErrorIs(t, AuthorizeDebugIssue(Caller{UserID: "alice"}, owner), ErrMissingIdentity)
	assert.ErrorIs(t, AuthorizeDebugIssue(Caller{UserID: "mallory", ProjectID: "proj-b"}, owner), ErrForbidden)
	assert.ErrorIs(t, AuthorizeDebugIssue(Caller{UserID: "alice", ProjectID: "proj-a"}, Owner{}), ErrObjectUnowned)
	assert.NoError(t, AuthorizeDebugIssue(Caller{UserID: "alice", ProjectID: "proj-a"}, owner))
	assert.NoError(t, AuthorizeDebugIssue(Caller{UserID: "carol", ProjectID: "proj-a"}, owner), "同项目非创建者可签发登录调试票")
}

func TestAuthorizeBuilderObserveAndMutate(t *testing.T) {
	owner := Owner{UserID: "alice", ProjectID: "proj-a"}
	assert.NoError(t, AuthorizeBuilderObserve(Caller{}, Owner{}))
	assert.NoError(t, AuthorizeBuilderMutate(Caller{}, Owner{}))
	assert.NoError(t, AuthorizeBuilderObserve(Caller{}, owner))
	assert.ErrorIs(t, AuthorizeBuilderMutate(Caller{}, owner), ErrMissingIdentity)
	assert.NoError(t, AuthorizeBuilderObserve(Caller{UserID: "alice", ProjectID: "proj-a"}, owner))
	assert.NoError(t, AuthorizeBuilderMutate(Caller{UserID: "alice", ProjectID: "proj-a"}, owner))
	assert.ErrorIs(t, AuthorizeBuilderObserve(Caller{UserID: "bob", ProjectID: "proj-b"}, owner), ErrForbidden)
	assert.ErrorIs(t, AuthorizeBuilderMutate(Caller{UserID: "bob", ProjectID: "proj-b"}, owner), ErrForbidden)
}

func TestN4_UserOnlyCallerCannotMutateProjectOwnedBuilder(t *testing.T) {
	owner := Owner{UserID: "alice", ProjectID: "proj-a"}
	// 只带 UID：项目级匹配永远命不中，退化成用户名必须相等。
	assert.ErrorIs(t, AuthorizeBuilderMutate(Caller{UserID: "carol"}, owner), ErrForbidden)
	assert.NoError(t, AuthorizeBuilderMutate(Caller{UserID: "carol", ProjectID: "proj-a"}, owner))
}

func TestOwnerFromPod(t *testing.T) {
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Labels:      map[string]string{LabelProjectID: "from-label"},
			Annotations: map[string]string{LabelUserID: "alice"},
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{{
				Env: []corev1.EnvVar{{Name: EnvProjectID, Value: "from-env"}},
			}},
		},
	}
	owner := OwnerFromPod(pod)
	assert.Equal(t, "alice", owner.UserID)
	assert.Equal(t, "from-label", owner.ProjectID)
}

func TestSanitizeLabelValue(t *testing.T) {
	assert.Equal(t, "proj-a_1", SanitizeLabelValue("proj-a_1"))
	assert.Equal(t, "proj-a-1", SanitizeLabelValue("proj/a 1"))
}
