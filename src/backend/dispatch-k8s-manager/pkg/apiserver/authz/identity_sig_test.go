package authz

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"net/http"
	"strconv"
	"testing"
	"time"

	"disaptch-k8s-manager/pkg/config"

	"github.com/stretchr/testify/assert"
)

const (
	testSigMethod = http.MethodGet
	testSigPath   = "/api/builders/foo/status"
)

func TestIdentitySignatureRoundTrip(t *testing.T) {
	h := http.Header{}
	h.Set(HeaderUserID, "alice")
	h.Set(HeaderProjectID, "proj-a")
	now := time.Unix(1_700_000_000, 0)
	AttachIdentitySignature(h, now, testSigMethod, testSigPath)
	caller := Caller{UserID: "alice", ProjectID: "proj-a"}
	assert.NoError(t, VerifyIdentitySignature(h, caller, now, testSigMethod, testSigPath))
	assert.NoError(t, VerifyIdentitySignature(h, caller, now.Add(30*time.Second), testSigMethod, testSigPath))
	assert.ErrorIs(t, VerifyIdentitySignature(h, caller, now.Add(61*time.Second), testSigMethod, testSigPath), ErrUntrustedIdentity)

	h.Set(HeaderIdentitySig, "tampered")
	assert.ErrorIs(t, VerifyIdentitySignature(h, caller, now, testSigMethod, testSigPath), ErrUntrustedIdentity)
}

func TestN11_SignatureBoundToMethodAndPath(t *testing.T) {
	h := http.Header{}
	h.Set(HeaderUserID, "alice")
	h.Set(HeaderProjectID, "proj-a")
	now := time.Now()
	AttachIdentitySignature(h, now, http.MethodGet, "/api/builders/foo/status")
	caller := Caller{UserID: "alice", ProjectID: "proj-a"}
	assert.NoError(t, VerifyIdentitySignature(h, caller, now, http.MethodGet, "/api/builders/foo/status"))
	assert.ErrorIs(t, VerifyIdentitySignature(h, caller, now, http.MethodPut, "/api/builders/foo/start"), ErrUntrustedIdentity,
		"status 签名不得重放到 start")
	assert.ErrorIs(t, VerifyIdentitySignature(h, caller, now, http.MethodGet, "/api/builders/foo/terminal"), ErrUntrustedIdentity,
		"path 不同不得重放")
}

func TestCallerFromHeaderDropsUnsignedAndBadSig(t *testing.T) {
	h := http.Header{}
	h.Set(HeaderUserID, "alice")
	h.Set(HeaderProjectID, "proj-a")
	assert.True(t, CallerFromHeader(h, testSigMethod, testSigPath).IsEmpty())

	AttachIdentitySignature(h, time.Now(), testSigMethod, testSigPath)
	got := CallerFromHeader(h, testSigMethod, testSigPath)
	assert.Equal(t, "alice", got.UserID)
	assert.Equal(t, "proj-a", got.ProjectID)

	h.Set(HeaderIdentitySig, "bad")
	assert.True(t, CallerFromHeader(h, testSigMethod, testSigPath).IsEmpty())
}

func TestAttachIdentitySignatureSkipsEmptyCaller(t *testing.T) {
	h := http.Header{}
	AttachIdentitySignature(h, time.Now(), testSigMethod, testSigPath)
	assert.Empty(t, h.Get(HeaderIdentitySig))
}

func TestPublishedDefaultIdentityKeyRejected(t *testing.T) {
	defer SetIdentitySigningKeyForTest([]byte(UnitTestIdentitySigningKey))
	now := time.Now()
	caller := Caller{UserID: "alice", ProjectID: "proj-a"}
	h := http.Header{}
	h.Set(HeaderUserID, caller.UserID)
	h.Set(HeaderProjectID, caller.ProjectID)
	ts := strconv.FormatInt(now.Unix(), 10)
	mac := hmac.New(sha256.New, []byte(DefaultIdentitySigningKey))
	_, _ = mac.Write([]byte(identitySigPayload(caller, ts, testSigMethod, testSigPath)))
	h.Set(HeaderIdentityTS, ts)
	h.Set(HeaderIdentitySig, base64.RawURLEncoding.EncodeToString(mac.Sum(nil)))

	SetIdentitySigningKeyForTest([]byte("server-high-entropy-identity"))
	assert.True(t, CallerFromHeader(h, testSigMethod, testSigPath).IsEmpty(), "公开常量 identitySigningKey 自签必须被拒")
}

// TestN10_PublicDefaultSigningKeyRejected 对应 C-5 TestN10_PublicDefaultSigningKeyPlusProjectOrReopensPoC6。
func TestN10_PublicDefaultSigningKeyRejected(t *testing.T) {
	TestPublishedDefaultIdentityKeyRejected(t)
}

func TestPublishedDefaultIdentityKeyInConfigIsIgnored(t *testing.T) {
	SetIdentitySigningKeyForTest(nil)
	defer SetIdentitySigningKeyForTest([]byte(UnitTestIdentitySigningKey))
	old := config.Config.ApiServer.Auth.IdentitySigningKey
	config.Config.ApiServer.Auth.IdentitySigningKey = DefaultIdentitySigningKey
	defer func() { config.Config.ApiServer.Auth.IdentitySigningKey = old }()
	assert.Empty(t, identitySigningKey(), "配置里的公开常量必须当作未配置")

	h := http.Header{}
	h.Set(HeaderUserID, "alice")
	h.Set(HeaderProjectID, "proj-a")
	assert.True(t, CallerFromHeader(h, testSigMethod, testSigPath).IsEmpty(), "空配置自称头降级匿名")
}
