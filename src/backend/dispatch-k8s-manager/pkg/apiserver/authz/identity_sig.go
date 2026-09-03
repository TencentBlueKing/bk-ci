package authz

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"net/http"
	"strconv"
	"strings"
	"time"

	"disaptch-k8s-manager/pkg/config"
)

const (
	HeaderIdentitySig = "X-DEVOPS-IDENTITY-SIG"
	HeaderIdentityTS  = "X-DEVOPS-IDENTITY-TS"

	identitySigTTL = 5 * time.Minute
	// DefaultIdentitySigningKey 仅 dispatch 与 manager 持有，禁止注入构建容器。
	// 仓库默认公开，生产必须覆盖。空配置也用它，避免无签名自称头被接受。
	DefaultIdentitySigningKey = "bkci-k8s-manager-identity-sig-change-in-prod"
)

var testIdentitySigningKey []byte

func SetIdentitySigningKeyForTest(secret []byte) {
	if secret == nil {
		testIdentitySigningKey = nil
		return
	}
	testIdentitySigningKey = append([]byte(nil), secret...)
}

func identitySigningKey() []byte {
	if len(testIdentitySigningKey) > 0 {
		return testIdentitySigningKey
	}
	if config.Config != nil && strings.TrimSpace(config.Config.ApiServer.Auth.IdentitySigningKey) != "" {
		return []byte(config.Config.ApiServer.Auth.IdentitySigningKey)
	}
	return []byte(DefaultIdentitySigningKey)
}

func identitySigPayload(caller Caller, ts string) string {
	return caller.UserID + "|" + caller.ProjectID + "|" + caller.TenantID + "|" + ts
}

func SignIdentity(caller Caller, now time.Time) (ts, sig string) {
	ts = strconv.FormatInt(now.Unix(), 10)
	mac := hmac.New(sha256.New, identitySigningKey())
	_, _ = mac.Write([]byte(identitySigPayload(caller, ts)))
	sig = base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
	return ts, sig
}

// AttachIdentitySignature 给已有身份头补 TS/SIG。无身份头则不动。
func AttachIdentitySignature(h http.Header, now time.Time) {
	if h == nil {
		return
	}
	caller := Caller{
		UserID:    strings.TrimSpace(h.Get(HeaderUserID)),
		ProjectID: strings.TrimSpace(h.Get(HeaderProjectID)),
		TenantID:  strings.TrimSpace(h.Get(HeaderTenantID)),
	}
	if caller.IsEmpty() {
		return
	}
	ts, sig := SignIdentity(caller, now)
	h.Set(HeaderIdentityTS, ts)
	h.Set(HeaderIdentitySig, sig)
}

func VerifyIdentitySignature(h http.Header, caller Caller, now time.Time) error {
	if caller.IsEmpty() {
		return nil
	}
	if h == nil {
		return ErrUntrustedIdentity
	}
	ts := strings.TrimSpace(h.Get(HeaderIdentityTS))
	sig := strings.TrimSpace(h.Get(HeaderIdentitySig))
	if ts == "" || sig == "" {
		return ErrUntrustedIdentity
	}
	unix, err := strconv.ParseInt(ts, 10, 64)
	if err != nil {
		return ErrUntrustedIdentity
	}
	skew := now.Sub(time.Unix(unix, 0))
	if skew < 0 {
		skew = -skew
	}
	if skew > identitySigTTL {
		return ErrUntrustedIdentity
	}
	mac := hmac.New(sha256.New, identitySigningKey())
	_, _ = mac.Write([]byte(identitySigPayload(caller, ts)))
	want := base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
	if !hmac.Equal([]byte(sig), []byte(want)) {
		return ErrUntrustedIdentity
	}
	return nil
}
