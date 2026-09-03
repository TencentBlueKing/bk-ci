package apis

import (
	"net/http"
	"testing"

	"disaptch-k8s-manager/pkg/apiserver/authz"
	"disaptch-k8s-manager/pkg/kubeclient"

	"github.com/stretchr/testify/assert"
	appsv1 "k8s.io/api/apps/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// C-5 PoC5：不带身份头即可 stop/delete 已打属主的他人构建机。修复后必须拒绝。

func TestPoC_R12_AnonymousCannotStopOrDeleteOwnedBuilder(t *testing.T) {
	listBuilderDeployment = func(workloadCoreLabel string) ([]*appsv1.Deployment, error) {
		return []*appsv1.Deployment{{
			ObjectMeta: metav1.ObjectMeta{
				Name: workloadCoreLabel,
				Labels: map[string]string{
					authz.LabelProjectID: "proj-a",
					authz.LabelUserID:    "alice",
				},
			},
		}}, nil
	}
	defer func() { listBuilderDeployment = kubeclient.ListDeployment }()

	c, w := newTestContext(http.MethodGet, "/api/builders/build999-ownedaa/status", nil)
	assert.True(t, authorizeBuilderLifecycle(c, "build999-ownedaa", false))
	assert.NotEqual(t, http.StatusForbidden, w.Code)

	c, w = newTestContext(http.MethodPut, "/api/builders/build999-ownedaa/stop", nil)
	assert.False(t, authorizeBuilderLifecycle(c, "build999-ownedaa", true))
	assert.Equal(t, http.StatusForbidden, w.Code)

	c, w = newTestContext(http.MethodDelete, "/api/builders/build999-ownedaa", nil)
	assert.False(t, authorizeBuilderLifecycle(c, "build999-ownedaa", true))
	assert.Equal(t, http.StatusForbidden, w.Code)
}

func TestPoC_R12_UnownedBuilderStillExempt(t *testing.T) {
	listBuilderDeployment = func(workloadCoreLabel string) ([]*appsv1.Deployment, error) {
		return []*appsv1.Deployment{{
			ObjectMeta: metav1.ObjectMeta{Name: workloadCoreLabel},
		}}, nil
	}
	defer func() { listBuilderDeployment = kubeclient.ListDeployment }()

	c, w := newTestContext(http.MethodPut, "/api/builders/build111-legacy1/stop", nil)
	assert.True(t, authorizeBuilderLifecycle(c, "build111-legacy1", true))
	assert.NotEqual(t, http.StatusForbidden, w.Code)
}
