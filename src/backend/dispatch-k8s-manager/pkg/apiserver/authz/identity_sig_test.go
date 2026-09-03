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

func TestIdentitySignatureRoundTrip(t *testing.T) {
	h := http.Header{}
	h.Set(HeaderUserID, "alice")
	h.Set(HeaderProjectID, "proj-a")
	now := time.Unix(1_700_000_000, 0)
	AttachIdentitySignature(h, now)
	caller := Caller{UserID: "alice", ProjectID: "proj-a"}
	assert.NoError(t, VerifyIdentitySignature(h, caller, now))
	assert.NoError(t, VerifyIdentitySignature(h, caller, now.Add(4*time.Minute)))
	assert.ErrorIs(t, VerifyIdentitySignature(h, caller, now.Add(6*time.Minute)), ErrUntrustedIdentity)

	h.Set(HeaderIdentitySig, "tampered")
	assert.ErrorIs(t, VerifyIdentitySignature(h, caller, now), ErrUntrustedIdentity)
}

func TestCallerFromHeaderDropsUnsignedAndBadSig(t *testing.T) {
	h := http.Header{}
	h.Set(HeaderUserID, "alice")
	h.Set(HeaderProjectID, "proj-a")
	assert.True(t, CallerFromHeader(h).IsEmpty())

	AttachIdentitySignature(h, time.Now())
	got := CallerFromHeader(h)
	assert.Equal(t, "alice", got.UserID)
	assert.Equal(t, "proj-a", got.ProjectID)

	h.Set(HeaderIdentitySig, "bad")
	assert.True(t, CallerFromHeader(h).IsEmpty())
}

func TestAttachIdentitySignatureSkipsEmptyCaller(t *testing.T) {
	h := http.Header{}
	AttachIdentitySignature(h, time.Now())
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
	_, _ = mac.Write([]byte(identitySigPayload(caller, ts)))
	h.Set(HeaderIdentityTS, ts)
	h.Set(HeaderIdentitySig, base64.RawURLEncoding.EncodeToString(mac.Sum(nil)))

	SetIdentitySigningKeyForTest([]byte("server-high-entropy-identity"))
	assert.True(t, CallerFromHeader(h).IsEmpty(), "公开常量 identitySigningKey 自签必须被拒")
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
	assert.True(t, CallerFromHeader(h).IsEmpty(), "空配置自称头降级匿名")
}
