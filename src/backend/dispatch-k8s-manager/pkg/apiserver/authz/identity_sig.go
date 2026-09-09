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

	identitySigTTL = 60 * time.Second
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

func IdentitySigningKeyConfigured() bool {
	return len(identitySigningKey()) > 0
}

func normalizeIdentityPath(path string) string {
	if i := strings.Index(path, "?"); i >= 0 {
		path = path[:i]
	}
	path = strings.TrimSpace(path)
	if path == "" {
		return "/"
	}
	if !strings.HasPrefix(path, "/") {
		path = "/" + path
	}
	if len(path) > 1 {
		path = strings.TrimRight(path, "/")
	}
	return path
}

func identitySigPayload(caller Caller, ts, method, path string) string {
	return caller.UserID + "|" + caller.ProjectID + "|" + caller.TenantID + "|" + ts + "|" +
		strings.ToUpper(strings.TrimSpace(method)) + "|" + normalizeIdentityPath(path)
}

func SignIdentity(caller Caller, now time.Time, method, path string) (ts, sig string) {
	key := identitySigningKey()
	if len(key) == 0 {
		return "", ""
	}
	ts = strconv.FormatInt(now.Unix(), 10)
	mac := hmac.New(sha256.New, key)
	_, _ = mac.Write([]byte(identitySigPayload(caller, ts, method, path)))
	sig = base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
	return ts, sig
}

// AttachIdentitySignature 给已有身份头补 TS/SIG，并绑定 method+path，防止 60s 窗内跨接口重放。
func AttachIdentitySignature(h http.Header, now time.Time, method, path string) {
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
	ts, sig := SignIdentity(caller, now, method, path)
	if ts == "" || sig == "" {
		return
	}
	h.Set(HeaderIdentityTS, ts)
	h.Set(HeaderIdentitySig, sig)
}

func AttachIdentitySignatureOnRequest(r *http.Request, now time.Time) {
	if r == nil || r.URL == nil {
		return
	}
	AttachIdentitySignature(r.Header, now, r.Method, r.URL.Path)
}

func VerifyIdentitySignature(h http.Header, caller Caller, now time.Time, method, path string) error {
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
	_, _ = mac.Write([]byte(identitySigPayload(caller, ts, method, path)))
	want := base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
	if !hmac.Equal([]byte(sig), []byte(want)) {
		return ErrUntrustedIdentity
	}
	return nil
}
