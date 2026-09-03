package apis

import (
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
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

// C-5 最终 CR 可复跑 PoC：修复后这些绕过必须失败。

func TestPoC_R1_KnownSharedTokenForgedTicketRejected(t *testing.T) {
	now := time.Now()
	nowFunc = func() time.Time { return now }
	defer func() { nowFunc = time.Now }()
	authz.SetDebugTicketSecretForTest([]byte("server-high-entropy-secret"))
	defer authz.SetDebugTicketSecretForTest(nil)

	victim := authz.Caller{UserID: "alice", ProjectID: "proj-a"}
	forged, err := authz.SignDebugTicketWithSecret(victim, "pod-1", "ctr-1", now, []byte("shared-devops-token"))
	assert.NoError(t, err)

	loadDebugPod = func(podName string) (*corev1.Pod, error) {
		return &corev1.Pod{
			ObjectMeta: metav1.ObjectMeta{
				Name:   podName,
				Labels: map[string]string{authz.LabelProjectID: "proj-a", authz.LabelUserID: "alice"},
			},
		}, nil
	}
	defer func() { loadDebugPod = kubeclient.GetPod }()

	c, _ := newTestContext(http.MethodGet, "/api/builders/debug/pod-1/ctr-1/"+forged, map[string]string{
		authz.HeaderUserID:    "alice",
		authz.HeaderProjectID: "proj-a",
	})
	c.Params = gin.Params{
		{Key: "podName", Value: "pod-1"},
		{Key: "containerName", Value: "ctr-1"},
		{Key: "ticket", Value: forged},
	}
	assert.ErrorIs(t, authorizeDebugBuilder(c, "pod-1", "ctr-1"), authz.ErrInvalidTicket)
}

func TestPoC_R2_QueryIdentityCannotIssueOrDebug(t *testing.T) {
	now := time.Now()
	nowFunc = func() time.Time { return now }
	defer func() { nowFunc = time.Now }()

	loadDebugPod = func(podName string) (*corev1.Pod, error) {
		return &corev1.Pod{
			ObjectMeta: metav1.ObjectMeta{
				Name:   podName,
				Labels: map[string]string{authz.LabelProjectID: "proj-a", authz.LabelUserID: "alice"},
			},
		}, nil
	}
	defer func() { loadDebugPod = kubeclient.GetPod }()

	c, w := newTestContext(http.MethodGet, "/api/builders/victim/terminal?userId=alice&projectId=proj-a", nil)
	_, ok := requireTenantCaller(c)
	assert.False(t, ok)
	assert.Equal(t, http.StatusForbidden, w.Code)

	legit, err := authz.IssueDebugTicket(authz.Caller{UserID: "alice", ProjectID: "proj-a"}, "pod-1", "ctr-1", now)
	assert.NoError(t, err)
	c, _ = newTestContext(http.MethodGet, "/api/builders/debug/pod-1/ctr-1/"+legit+"?userId=alice&projectId=proj-a", nil)
	c.Params = gin.Params{
		{Key: "podName", Value: "pod-1"},
		{Key: "containerName", Value: "ctr-1"},
		{Key: "ticket", Value: legit},
	}
	assert.ErrorIs(t, authorizeDebugBuilder(c, "pod-1", "ctr-1"), authz.ErrUntrustedIdentity)
}

func TestPoC_R3_CannotSeizeIstioSystem(t *testing.T) {
	authz.DefaultNamespaceOwners.Reset()
	created := false
	getNativeDeployment = func(namespace, name string) (*appsv1.Deployment, error) {
		return nil, errors.New("not found")
	}
	createNativeDeployment = func(namespace string, deployment *appsv1.Deployment) error {
		created = true
		return nil
	}
	lookupNamespace = func(namespace string) (*corev1.Namespace, error) {
		return &corev1.Namespace{ObjectMeta: metav1.ObjectMeta{Name: namespace}}, nil
	}
	defer func() {
		getNativeDeployment = kubeclient.GetNativeDeployment
		createNativeDeployment = kubeclient.CreateNativeDeployment
		lookupNamespace = kubeclient.GetNamespace
	}()

	engine := gin.New()
	engine.POST("/namespace/:namespace/deployments", createDeployment)
	body := `{"metadata":{"name":"evil"},"spec":{"template":{"spec":{"containers":[{"name":"x","image":"evil:latest","command":["id"]}]}}}}`
	req := httptest.NewRequest(http.MethodPost, "/namespace/istio-system/deployments", strings.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set(authz.HeaderUserID, "alice")
	req.Header.Set(authz.HeaderProjectID, "proj-a")
	authz.AttachIdentitySignature(req.Header, time.Now())
	w := httptest.NewRecorder()
	engine.ServeHTTP(w, req)
	assert.Equal(t, http.StatusForbidden, w.Code)
	assert.False(t, created)
	_, seized := authz.DefaultNamespaceOwners.Get("istio-system")
	assert.False(t, seized)
}

func TestPoC6_ForgedProjectOnlyHeaderCannotIssueTicket(t *testing.T) {
	now := time.Now()
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name:   "victim-pod",
			Labels: map[string]string{authz.LabelProjectID: "proj-a", authz.LabelUserID: "alice"},
		},
	}
	// 构建容器只有共享 token、无签名密钥：伪造头在 CallerFromHeader 被丢弃，签不出票。
	c, _ := newTestContextUnsigned(http.MethodGet, "/api/builders/victim/terminal", map[string]string{
		authz.HeaderUserID:    "mallory",
		authz.HeaderProjectID: "proj-a",
	})
	caller := authz.CallerFromRequest(c)
	assert.True(t, caller.IsEmpty())
	_, err := authz.IssueDebugTicketForPod(caller, pod, "ctr-1", now)
	assert.ErrorIs(t, err, authz.ErrMissingIdentity)

	ticket, err := authz.IssueDebugTicketForPod(authz.Caller{UserID: "alice", ProjectID: "proj-a"}, pod, "ctr-1", now)
	assert.NoError(t, err)
	subject, err := authz.DebugTicketSubject(ticket)
	assert.NoError(t, err)
	assert.Equal(t, "alice", subject.UserID)
	assert.Equal(t, "proj-a", subject.ProjectID)
	assert.NoError(t, authz.AuthorizeDebugSession(authz.Caller{}, ticket, "victim-pod", "ctr-1", pod, now))
}

func TestPoC6_UnsignedForgedHeadersAreDropped(t *testing.T) {
	c, _ := newTestContextUnsigned(http.MethodGet, "/api/builders/victim/terminal", map[string]string{
		authz.HeaderUserID:    "alice",
		authz.HeaderProjectID: "proj-a",
	})
	caller := authz.CallerFromRequest(c)
	assert.True(t, caller.IsEmpty(), "无 HMAC 的自称头必须丢弃，关闭仅凭共享 token 伪造身份")
	_, err := authz.RequireTenantCaller(c)
	assert.ErrorIs(t, err, authz.ErrMissingIdentity)
}
