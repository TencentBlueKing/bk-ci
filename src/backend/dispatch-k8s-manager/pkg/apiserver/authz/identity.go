package authz

import (
	"errors"
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
)

// BK-CI 标准身份头。共享 Devops-Token 只能证明“来自内部调用”，不能做对象/租户隔离。
const (
	HeaderUserID    = "X-DEVOPS-UID"
	HeaderProjectID = "X-DEVOPS-PROJECT-ID"
	HeaderTenantID  = "X-DEVOPS-TENANT-ID"

	QueryUserID    = "userId"
	QueryProjectID = "projectId"
	QueryTenantID  = "tenantId"
	QueryTicket    = "ticket"

	LabelUserID    = "bkci.dispatch.kubernetes/user-id"
	LabelProjectID = "bkci.dispatch.kubernetes/project-id"
	LabelTenantID  = "bkci.dispatch.kubernetes/tenant-id"

	EnvProjectID = "devops_project_id"
	EnvUserID    = "devops_user_id"
	EnvTenantID  = "devops_tenant_id"
)

var (
	ErrMissingIdentity = errors.New("missing caller identity: require X-DEVOPS-UID and X-DEVOPS-PROJECT-ID or X-DEVOPS-TENANT-ID")
	ErrForbidden       = errors.New("forbidden: object owner or tenant mismatch")
	ErrObjectUnowned   = errors.New("forbidden: target object has no owner, deny by default")
	ErrInvalidTicket   = errors.New("forbidden: debug ticket invalid or expired")
	ErrNamespaceDenied = errors.New("forbidden: namespace is not owned by caller or is a system namespace")
)

// Caller 是请求侧身份，来自 Header（WebSocket 也可走 Query）。
type Caller struct {
	UserID    string
	ProjectID string
	TenantID  string
}

func (c Caller) IsEmpty() bool {
	return c.UserID == "" && c.ProjectID == "" && c.TenantID == ""
}

func (c Caller) HasTenantScope() bool {
	return c.ProjectID != "" || c.TenantID != ""
}

func (c Caller) Owner() Owner {
	return Owner{UserID: c.UserID, ProjectID: c.ProjectID, TenantID: c.TenantID}
}

// Owner 是资源属主（label / annotation / env）。
type Owner struct {
	UserID    string
	ProjectID string
	TenantID  string
}

func (o Owner) IsEmpty() bool {
	return o.UserID == "" && o.ProjectID == "" && o.TenantID == ""
}

func (o Owner) Fill(other Owner) Owner {
	if o.UserID == "" {
		o.UserID = other.UserID
	}
	if o.ProjectID == "" {
		o.ProjectID = other.ProjectID
	}
	if o.TenantID == "" {
		o.TenantID = other.TenantID
	}
	return o
}

func CallerFromRequest(c *gin.Context) Caller {
	if c == nil || c.Request == nil {
		return Caller{}
	}
	return CallerFromHeader(c.Request.Header).FillFromQuery(c.Request.URL.Query())
}

func CallerFromHeader(h http.Header) Caller {
	if h == nil {
		return Caller{}
	}
	return Caller{
		UserID:    firstNonEmpty(h.Get(HeaderUserID), h.Get(http.CanonicalHeaderKey(HeaderUserID))),
		ProjectID: firstNonEmpty(h.Get(HeaderProjectID), h.Get(http.CanonicalHeaderKey(HeaderProjectID))),
		TenantID:  firstNonEmpty(h.Get(HeaderTenantID), h.Get(http.CanonicalHeaderKey(HeaderTenantID))),
	}
}

func (c Caller) FillFromQuery(q map[string][]string) Caller {
	if c.UserID == "" {
		c.UserID = firstQuery(q, QueryUserID)
	}
	if c.ProjectID == "" {
		c.ProjectID = firstQuery(q, QueryProjectID)
	}
	if c.TenantID == "" {
		c.TenantID = firstQuery(q, QueryTenantID)
	}
	return c
}

// RequireTenantCaller 要求具备用户身份，以及项目或租户之一，避免共享 token 匿名越权。
func RequireTenantCaller(c *gin.Context) (Caller, error) {
	caller := CallerFromRequest(c)
	if caller.UserID == "" || !caller.HasTenantScope() {
		return Caller{}, ErrMissingIdentity
	}
	return caller, nil
}

func firstNonEmpty(values ...string) string {
	for _, v := range values {
		if strings.TrimSpace(v) != "" {
			return strings.TrimSpace(v)
		}
	}
	return ""
}

func firstQuery(q map[string][]string, key string) string {
	if q == nil {
		return ""
	}
	vals := q[key]
	if len(vals) == 0 {
		return ""
	}
	return strings.TrimSpace(vals[0])
}
