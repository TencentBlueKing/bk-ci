package apis

import (
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"disaptch-k8s-manager/pkg/apiserver/authz"
	"disaptch-k8s-manager/pkg/apiserver/service"
	"disaptch-k8s-manager/pkg/kubeclient"
	"disaptch-k8s-manager/pkg/types"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func init() {
	gin.SetMode(gin.TestMode)
}

func newTestContext(method, path string, headers map[string]string) (*gin.Context, *httptest.ResponseRecorder) {
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)
	c.Request = httptest.NewRequest(method, path, nil)
	for k, v := range headers {
		c.Request.Header.Set(k, v)
	}
	return c, w
}

func TestAuthorizeDebugBuilder_BlocksCrossTenant(t *testing.T) {
	now := time.Now()
	nowFunc = func() time.Time { return now }
	defer func() { nowFunc = time.Now }()

	ownerCaller := authz.Caller{UserID: "alice", ProjectID: "proj-a"}
	ticket, err := authz.IssueDebugTicket(ownerCaller, "pod-1", "ctr-1", now)
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

	c, _ := newTestContext(http.MethodGet, "/api/builders/debug/pod-1/ctr-1?ticket="+ticket, map[string]string{
		authz.HeaderUserID:    "bob",
		authz.HeaderProjectID: "proj-b",
	})
	err = authorizeDebugBuilder(c, "pod-1", "ctr-1")
	assert.Error(t, err)

	c, _ = newTestContext(http.MethodGet, "/api/builders/debug/pod-1/ctr-1", map[string]string{
		authz.HeaderUserID:    "alice",
		authz.HeaderProjectID: "proj-a",
	})
	err = authorizeDebugBuilder(c, "pod-1", "ctr-1")
	assert.ErrorIs(t, err, authz.ErrInvalidTicket)

	c, _ = newTestContext(http.MethodGet, "/api/builders/debug/pod-1/ctr-1?ticket="+ticket, map[string]string{
		authz.HeaderUserID:    "alice",
		authz.HeaderProjectID: "proj-a",
	})
	assert.NoError(t, authorizeDebugBuilder(c, "pod-1", "ctr-1"))
}

func TestClaimBuildless_RedactsSecretKey(t *testing.T) {
	authz.DefaultNamespaceOwners.Reset()
	claimBuildLessTask = func(podId string) (*types.BuildLessTask, error) {
		return &types.BuildLessTask{
			ProjectId:      "proj-a",
			AgentId:        "agent-1",
			PipelineId:     "pipe-1",
			BuildId:        "build-1",
			VmSeqId:        1,
			SecretKey:      "should-not-leak",
			ExecutionCount: 1,
		}, nil
	}
	defer func() { claimBuildLessTask = service.ClaimBuildLessTask }()

	c, w := newTestContext(http.MethodGet, "/api/buildless/build/claim?podId=pod-1", nil)
	claimBuildless(c)

	assert.Equal(t, http.StatusOK, w.Code)
	body := w.Body.String()
	assert.NotContains(t, body, "should-not-leak")
	assert.NotContains(t, strings.ToLower(body), "secretkey")
	assert.NotContains(t, body, "agent-1")
	assert.Contains(t, body, "build-1")
}

func TestClaimBuildless_CrossProjectDenied(t *testing.T) {
	claimBuildLessTask = func(podId string) (*types.BuildLessTask, error) {
		return &types.BuildLessTask{ProjectId: "proj-a", SecretKey: "should-not-leak", BuildId: "build-1"}, nil
	}
	defer func() { claimBuildLessTask = service.ClaimBuildLessTask }()

	c, w := newTestContext(http.MethodGet, "/api/buildless/build/claim?podId=pod-1", map[string]string{
		authz.HeaderUserID:    "bob",
		authz.HeaderProjectID: "proj-b",
	})
	claimBuildless(c)
	assert.Equal(t, http.StatusForbidden, w.Code)
	assert.NotContains(t, w.Body.String(), "should-not-leak")
}

func TestGetDeployment_CrossNamespaceDenied(t *testing.T) {
	authz.DefaultNamespaceOwners.Reset()
	getNativeDeployment = func(namespace, name string) (*appsv1.Deployment, error) {
		return &appsv1.Deployment{
			ObjectMeta: metav1.ObjectMeta{
				Name:      name,
				Namespace: namespace,
				Labels:    map[string]string{authz.LabelProjectID: "proj-a", authz.LabelUserID: "alice"},
			},
		}, nil
	}
	lookupNamespace = func(namespace string) (*corev1.Namespace, error) {
		return nil, errors.New("no k8s")
	}
	defer func() {
		getNativeDeployment = kubeclient.GetNativeDeployment
		lookupNamespace = kubeclient.GetNamespace
	}()

	engine := gin.New()
	engine.GET("/namespace/:namespace/deployments/:deploymentName", getDeployment)

	req := httptest.NewRequest(http.MethodGet, "/namespace/kube-system/deployments/core-dns", nil)
	req.Header.Set(authz.HeaderUserID, "alice")
	req.Header.Set(authz.HeaderProjectID, "proj-a")
	w := httptest.NewRecorder()
	engine.ServeHTTP(w, req)
	assert.Equal(t, http.StatusForbidden, w.Code)

	req = httptest.NewRequest(http.MethodGet, "/namespace/proj-a-ns/deployments/web", nil)
	req.Header.Set(authz.HeaderUserID, "bob")
	req.Header.Set(authz.HeaderProjectID, "proj-b")
	w = httptest.NewRecorder()
	engine.ServeHTTP(w, req)
	assert.Equal(t, http.StatusForbidden, w.Code)

	req = httptest.NewRequest(http.MethodGet, "/namespace/proj-a-ns/deployments/web", nil)
	w = httptest.NewRecorder()
	engine.ServeHTTP(w, req)
	assert.Equal(t, http.StatusForbidden, w.Code)

	req = httptest.NewRequest(http.MethodGet, "/namespace/proj-a-ns/deployments/web", nil)
	req.Header.Set(authz.HeaderUserID, "alice")
	req.Header.Set(authz.HeaderProjectID, "proj-a")
	w = httptest.NewRecorder()
	engine.ServeHTTP(w, req)
	assert.Equal(t, http.StatusOK, w.Code)
}

func TestGetSecret_RedactsAndAuthorizes(t *testing.T) {
	authz.DefaultNamespaceOwners.Reset()
	getNativeSecret = func(namespace, name string) (*corev1.Secret, error) {
		return &corev1.Secret{
			ObjectMeta: metav1.ObjectMeta{
				Name:      name,
				Namespace: namespace,
				Labels:    map[string]string{authz.LabelProjectID: "proj-a", authz.LabelUserID: "alice"},
			},
			Data:       map[string][]byte{".dockerconfigjson": []byte("real-credential")},
			StringData: map[string]string{"password": "p@ss"},
		}, nil
	}
	lookupNamespace = func(namespace string) (*corev1.Namespace, error) {
		return nil, errors.New("no k8s")
	}
	defer func() {
		getNativeSecret = kubeclient.GetNativeSecret
		lookupNamespace = kubeclient.GetNamespace
	}()

	engine := gin.New()
	engine.GET("/namespace/:namespace/secrets/:secretName", getSecret)

	req := httptest.NewRequest(http.MethodGet, "/namespace/other-ns/secrets/regcred", nil)
	req.Header.Set(authz.HeaderUserID, "bob")
	req.Header.Set(authz.HeaderProjectID, "proj-b")
	w := httptest.NewRecorder()
	engine.ServeHTTP(w, req)
	assert.Equal(t, http.StatusForbidden, w.Code)
	assert.NotContains(t, w.Body.String(), "real-credential")
	assert.NotContains(t, w.Body.String(), "p@ss")

	req = httptest.NewRequest(http.MethodGet, "/namespace/proj-a-ns/secrets/regcred", nil)
	req.Header.Set(authz.HeaderUserID, "alice")
	req.Header.Set(authz.HeaderProjectID, "proj-a")
	w = httptest.NewRecorder()
	engine.ServeHTTP(w, req)
	assert.Equal(t, http.StatusOK, w.Code)
	assert.NotContains(t, w.Body.String(), "real-credential")
	assert.NotContains(t, w.Body.String(), "p@ss")

	var result types.Result
	assert.NoError(t, json.Unmarshal(w.Body.Bytes(), &result))
}

func TestCreateDeployment_BindsNamespaceOwner(t *testing.T) {
	authz.DefaultNamespaceOwners.Reset()
	created := false
	getNativeDeployment = func(namespace, name string) (*appsv1.Deployment, error) {
		return nil, errors.New("not found")
	}
	createNativeDeployment = func(namespace string, deployment *appsv1.Deployment) error {
		created = true
		assert.Equal(t, "alice", deployment.Labels[authz.LabelUserID])
		assert.Equal(t, "proj-a", deployment.Labels[authz.LabelProjectID])
		return nil
	}
	lookupNamespace = func(namespace string) (*corev1.Namespace, error) {
		return nil, errors.New("no k8s")
	}
	defer func() {
		getNativeDeployment = kubeclient.GetNativeDeployment
		createNativeDeployment = kubeclient.CreateNativeDeployment
		lookupNamespace = kubeclient.GetNamespace
	}()

	engine := gin.New()
	engine.POST("/namespace/:namespace/deployments", createDeployment)

	body := `{"metadata":{"name":"web"}}`
	req := httptest.NewRequest(http.MethodPost, "/namespace/proj-a-ns/deployments", strings.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set(authz.HeaderUserID, "alice")
	req.Header.Set(authz.HeaderProjectID, "proj-a")
	w := httptest.NewRecorder()
	engine.ServeHTTP(w, req)
	assert.Equal(t, http.StatusOK, w.Code)
	assert.True(t, created)

	req = httptest.NewRequest(http.MethodPost, "/namespace/proj-a-ns/deployments", strings.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set(authz.HeaderUserID, "bob")
	req.Header.Set(authz.HeaderProjectID, "proj-b")
	w = httptest.NewRecorder()
	engine.ServeHTTP(w, req)
	assert.Equal(t, http.StatusForbidden, w.Code)
}
