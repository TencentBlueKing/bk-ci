package authz

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
)

func TestCallerFromHeaderAndQuery(t *testing.T) {
	gin.SetMode(gin.TestMode)
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)
	req := httptest.NewRequest(http.MethodGet, "/debug/pod/ctr?projectId=p-query&ticket=t", nil)
	req.Header.Set(HeaderUserID, "alice")
	req.Header.Set(HeaderTenantID, "tenant-a")
	c.Request = req

	caller := CallerFromRequest(c)
	assert.Equal(t, "alice", caller.UserID)
	assert.Equal(t, "p-query", caller.ProjectID)
	assert.Equal(t, "tenant-a", caller.TenantID)
}

func TestRequireTenantCaller(t *testing.T) {
	gin.SetMode(gin.TestMode)
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)
	c.Request = httptest.NewRequest(http.MethodGet, "/", nil)
	c.Request.Header.Set(HeaderUserID, "alice")

	_, err := RequireTenantCaller(c)
	assert.ErrorIs(t, err, ErrMissingIdentity)

	c.Request.Header.Set(HeaderProjectID, "proj-1")
	caller, err := RequireTenantCaller(c)
	assert.NoError(t, err)
	assert.Equal(t, "proj-1", caller.ProjectID)
}
