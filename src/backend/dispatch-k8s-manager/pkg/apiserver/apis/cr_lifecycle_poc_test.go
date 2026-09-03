package apis

import (
	"net/http"
	"testing"
	"time"

	"disaptch-k8s-manager/pkg/apiserver/authz"
	"disaptch-k8s-manager/pkg/kubeclient"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
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

// N4：登录调试常由非流水线执行人发起。只带 UID 时 STOP/DELETE 403→池位泄漏；同项目应能开关。
func TestN4_NonCreatorSameProjectCanStopDeleteDebugBuilder(t *testing.T) {
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

	c, w := newTestContext(http.MethodPut, "/api/builders/build888-debugaa/stop", map[string]string{
		authz.HeaderUserID: "carol",
	})
	assert.False(t, authorizeBuilderLifecycle(c, "build888-debugaa", true), "仅 UID 不得停他人已属主构建机")
	assert.Equal(t, http.StatusForbidden, w.Code)

	c, w = newTestContext(http.MethodPut, "/api/builders/build888-debugaa/stop", map[string]string{
		authz.HeaderUserID:    "carol",
		authz.HeaderProjectID: "proj-a",
	})
	assert.True(t, authorizeBuilderLifecycle(c, "build888-debugaa", true), "同项目非创建者应能 STOP")
	assert.NotEqual(t, http.StatusForbidden, w.Code)

	c, w = newTestContext(http.MethodDelete, "/api/builders/build888-debugaa", map[string]string{
		authz.HeaderUserID:    "carol",
		authz.HeaderProjectID: "proj-a",
	})
	assert.True(t, authorizeBuilderLifecycle(c, "build888-debugaa", true), "同项目非创建者应能 DELETE")
	assert.NotEqual(t, http.StatusForbidden, w.Code)

	c, w = newTestContext(http.MethodGet, "/api/builders/build888-debugaa/status", map[string]string{
		authz.HeaderUserID:    "carol",
		authz.HeaderProjectID: "proj-a",
	})
	assert.True(t, authorizeBuilderLifecycle(c, "build888-debugaa", false), "同项目非创建者应能探活")
}

func TestN7_StartOwnedBuilderRequiresMatchingIdentity(t *testing.T) {
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

	c, w := newTestContextUnsigned(http.MethodPut, "/api/builders/build777-victim1/start", map[string]string{
		authz.HeaderUserID:    "mallory",
		authz.HeaderProjectID: "proj-b",
	})
	c.Params = []gin.Param{{Key: "builderName", Value: "build777-victim1"}}
	startBuilder(c)
	assert.Equal(t, http.StatusForbidden, w.Code, "无签名伪造头不得 start 已属主构建机")

	c, w = newTestContext(http.MethodPut, "/api/builders/build777-victim1/start", map[string]string{
		authz.HeaderUserID:    "mallory",
		authz.HeaderProjectID: "proj-b",
	})
	c.Params = []gin.Param{{Key: "builderName", Value: "build777-victim1"}}
	startBuilder(c)
	assert.Equal(t, http.StatusForbidden, w.Code, "跨项目即使有签名也不得 start")

	c, w = newTestContext(http.MethodPut, "/api/builders/build777-victim1/start", map[string]string{
		authz.HeaderUserID:    "carol",
		authz.HeaderProjectID: "proj-a",
	})
	assert.True(t, authorizeBuilderLifecycle(c, "build777-victim1", true), "同项目已签名应能 start")
	assert.NotEqual(t, http.StatusForbidden, w.Code)
}

func TestN4_NonCreatorSameProjectCanIssueLoginDebug(t *testing.T) {
	now := time.Now()
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name:   "debug-pod",
			Labels: map[string]string{authz.LabelProjectID: "proj-a", authz.LabelUserID: "alice"},
		},
	}
	c, _ := newTestContext(http.MethodGet, "/api/builders/build888-debugaa/terminal", map[string]string{
		authz.HeaderUserID:    "carol",
		authz.HeaderProjectID: "proj-a",
	})
	caller := authz.CallerFromRequest(c)
	assert.Equal(t, "carol", caller.UserID)
	assert.Equal(t, "proj-a", caller.ProjectID)
	ticket, err := authz.IssueDebugTicketForPod(caller, pod, "ctr-1", now)
	assert.NoError(t, err, "已签名的同项目非创建者应能发起登录调试")
	subject, err := authz.DebugTicketSubject(ticket)
	assert.NoError(t, err)
	assert.Equal(t, "alice", subject.UserID, "票主体仍是 Pod 属主，不是调试发起人")
	assert.NoError(t, authz.AuthorizeDebugSession(authz.Caller{}, ticket, "debug-pod", "ctr-1", pod, now))
}
