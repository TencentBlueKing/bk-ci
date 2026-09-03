package authz

import (
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

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
	AttachIdentitySignatureOnRequest(req, time.Now())
	c.Request = req

	caller := CallerFromRequest(c)
	assert.Equal(t, "alice", caller.UserID)
	assert.Equal(t, "", caller.ProjectID, "query 不得补身份")
	assert.Equal(t, "tenant-a", caller.TenantID)
	assert.True(t, HasQueryIdentity(c))
}

func TestRequireTenantCaller(t *testing.T) {
	gin.SetMode(gin.TestMode)
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)
	c.Request = httptest.NewRequest(http.MethodGet, "/", nil)
	c.Request.Header.Set(HeaderUserID, "alice")
	AttachIdentitySignatureOnRequest(c.Request, time.Now())

	_, err := RequireTenantCaller(c)
	assert.ErrorIs(t, err, ErrMissingIdentity)

	c.Request.Header.Set(HeaderProjectID, "proj-1")
	AttachIdentitySignatureOnRequest(c.Request, time.Now())
	caller, err := RequireTenantCaller(c)
	assert.NoError(t, err)
	assert.Equal(t, "proj-1", caller.ProjectID)

	c.Request = httptest.NewRequest(http.MethodGet, "/builders/x/terminal?userId=alice&projectId=proj-a", nil)
	_, err = RequireTenantCaller(c)
	assert.ErrorIs(t, err, ErrUntrustedIdentity)
}
