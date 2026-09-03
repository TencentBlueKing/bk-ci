package authz

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	"disaptch-k8s-manager/pkg/config"
)

const (
	HeaderIdentitySig = "X-DEVOPS-IDENTITY-SIG"
	HeaderIdentityTS  = "X-DEVOPS-IDENTITY-TS"

	identitySigTTL = 5 * time.Minute
	// EnvIdentitySigningKey Helm Secret 注入。优先于 config.yaml。
	EnvIdentitySigningKey = "K8S_MANAGER_IDENTITY_SIGNING_KEY"
	// DefaultIdentitySigningKey 是曾经写进仓库的公开串，仅作拒绝名单。绝不能再当兜底。
	DefaultIdentitySigningKey = "bkci-k8s-manager-identity-sig-change-in-prod"
	// UnitTestIdentitySigningKey 单测专用。
	UnitTestIdentitySigningKey = "unit-test-identity-signing-key"
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
	candidates := []string{os.Getenv(EnvIdentitySigningKey)}
	if config.Config != nil {
		candidates = append(candidates, config.Config.ApiServer.Auth.IdentitySigningKey)
	}
	for _, raw := range candidates {
		raw = strings.TrimSpace(raw)
		if raw == "" || raw == DefaultIdentitySigningKey {
			continue
		}
		return []byte(raw)
	}
	return nil
}

func identitySigPayload(caller Caller, ts string) string {
	return caller.UserID + "|" + caller.ProjectID + "|" + caller.TenantID + "|" + ts
}

func SignIdentity(caller Caller, now time.Time) (ts, sig string) {
	key := identitySigningKey()
	if len(key) == 0 {
		return "", ""
	}
	ts = strconv.FormatInt(now.Unix(), 10)
	mac := hmac.New(sha256.New, key)
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
	if ts == "" || sig == "" {
		return
	}
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
	key := identitySigningKey()
	if len(key) == 0 || ts == "" || sig == "" {
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
	mac := hmac.New(sha256.New, key)
	_, _ = mac.Write([]byte(identitySigPayload(caller, ts)))
	want := base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
	if !hmac.Equal([]byte(sig), []byte(want)) {
		return ErrUntrustedIdentity
	}
	return nil
}
