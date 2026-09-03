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
}

func TestAuthorizeObjectIfOwned(t *testing.T) {
	owner := Owner{UserID: "alice", ProjectID: "proj-a"}
	// 灰度豁免：无属主、无自称都不拦截。
	assert.NoError(t, AuthorizeObjectIfOwned(Caller{UserID: "alice", ProjectID: "proj-a"}, Owner{}))
	assert.NoError(t, AuthorizeObjectIfOwned(Caller{}, owner))
	assert.NoError(t, AuthorizeObjectIfOwned(Caller{UserID: "alice", ProjectID: "proj-a"}, owner))
	assert.ErrorIs(t, AuthorizeObjectIfOwned(Caller{UserID: "bob", ProjectID: "proj-b"}, owner), ErrForbidden)
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
