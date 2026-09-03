package authz

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"os"
	"strings"
	"time"

	"disaptch-k8s-manager/pkg/config"

	corev1 "k8s.io/api/core/v1"
)

const (
	debugTicketTTL = 10 * time.Minute
	// EnvDebugTicketSecret Helm Secret 注入的票据密钥。优先于 config.yaml。
	EnvDebugTicketSecret = "K8S_MANAGER_DEBUG_TICKET_SECRET"
	// DefaultDebugTicketSecret 是曾经写进仓库的公开串，仅作拒绝名单与回归靶标。
	// 绝不能再作为空配置兜底，否则读过源码即可自签票据。
	DefaultDebugTicketSecret = "bkci-k8s-manager-debug-ticket-change-in-prod"
	// UnitTestDebugTicketSecret 单测专用，禁止出现在配置与 Helm 默认值。
	UnitTestDebugTicketSecret = "unit-test-debug-ticket-secret"
)

type debugTicket struct {
	PodName       string `json:"pod"`
	ContainerName string `json:"ctr"`
	UserID        string `json:"uid"`
	ProjectID     string `json:"pid"`
	TenantID      string `json:"tid"`
	Exp           int64  `json:"exp"`
}

// IssueDebugTicket 在属主校验通过后签发短时对象绑定票据，防止仅凭共享 token + pod 名建立 Shell。
func IssueDebugTicket(caller Caller, podName, containerName string, now time.Time) (string, error) {
	if caller.IsEmpty() || podName == "" || containerName == "" {
		return "", ErrMissingIdentity
	}
	secret := debugTicketSecret()
	if len(secret) == 0 {
		return "", ErrDebugDisabled
	}
	payload, err := json.Marshal(debugTicket{
		PodName:       podName,
		ContainerName: containerName,
		UserID:        caller.UserID,
		ProjectID:     caller.ProjectID,
		TenantID:      caller.TenantID,
		Exp:           now.Add(debugTicketTTL).Unix(),
	})
	if err != nil {
		return "", err
	}
	return signDebugPayload(payload, secret), nil
}

func signDebugPayload(payload, secret []byte) string {
	mac := hmac.New(sha256.New, secret)
	_, _ = mac.Write(payload)
	return base64.RawURLEncoding.EncodeToString(payload) + "." + base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
}

// SignDebugTicketWithSecret 仅测试：用指定密钥签名，模拟攻击者持有共享 token / 硬编码密钥。
func SignDebugTicketWithSecret(caller Caller, podName, containerName string, now time.Time, secret []byte) (string, error) {
	payload, err := json.Marshal(debugTicket{
		PodName:       podName,
		ContainerName: containerName,
		UserID:        caller.UserID,
		ProjectID:     caller.ProjectID,
		TenantID:      caller.TenantID,
		Exp:           now.Add(debugTicketTTL).Unix(),
	})
	if err != nil {
		return "", err
	}
	return signDebugPayload(payload, secret), nil
}

func VerifyDebugTicket(token string, caller Caller, podName, containerName string, now time.Time) error {
	if len(debugTicketSecret()) == 0 {
		return ErrDebugDisabled
	}
	if token == "" {
		return ErrInvalidTicket
	}
	parts := strings.Split(token, ".")
	if len(parts) != 2 {
		return ErrInvalidTicket
	}
	payload, err := base64.RawURLEncoding.DecodeString(parts[0])
	if err != nil {
		return ErrInvalidTicket
	}
	sig, err := base64.RawURLEncoding.DecodeString(parts[1])
	if err != nil {
		return ErrInvalidTicket
	}
	mac := hmac.New(sha256.New, debugTicketSecret())
	_, _ = mac.Write(payload)
	if !hmac.Equal(sig, mac.Sum(nil)) {
		return ErrInvalidTicket
	}
	var ticket debugTicket
	if err := json.Unmarshal(payload, &ticket); err != nil {
		return ErrInvalidTicket
	}
	if ticket.Exp < now.Unix() {
		return ErrInvalidTicket
	}
	if ticket.PodName != podName || ticket.ContainerName != containerName {
		return ErrInvalidTicket
	}
	ticketCaller := Caller{UserID: ticket.UserID, ProjectID: ticket.ProjectID, TenantID: ticket.TenantID}
	if !caller.IsEmpty() && AuthorizeDebugIssue(caller, ticketCaller.Owner()) != nil {
		return ErrInvalidTicket
	}
	return nil
}

// IssueDebugTicketForPod 签票主体来自 Pod 属主，自称 caller 只做校验。
func IssueDebugTicketForPod(claimed Caller, pod *corev1.Pod, containerName string, now time.Time) (string, error) {
	if pod == nil {
		return "", ErrObjectUnowned
	}
	owner := OwnerFromPod(pod)
	if err := AuthorizeDebugIssue(claimed, owner); err != nil {
		return "", err
	}
	return IssueDebugTicket(owner.Caller(), pod.Name, containerName, now)
}

// AuthorizeDebugSession 必须在 WebSocket Upgrade 之前调用。
// 身份以票据内绑定的 caller 为准（服务端密钥签发）；请求头若存在则须与票据一致。
func AuthorizeDebugSession(caller Caller, ticket string, podName, containerName string, pod *corev1.Pod, now time.Time) error {
	if err := VerifyDebugTicket(ticket, caller, podName, containerName, now); err != nil {
		return err
	}
	claims, err := parseDebugTicket(ticket)
	if err != nil {
		return ErrInvalidTicket
	}
	ticketCaller := Caller{UserID: claims.UserID, ProjectID: claims.ProjectID, TenantID: claims.TenantID}
	if _, err := requireDebugCaller(ticketCaller); err != nil {
		return err
	}
	return AuthorizeDebugIssue(ticketCaller, OwnerFromPod(pod))
}

func DebugTicketSubject(token string) (Caller, error) {
	ticket, err := parseDebugTicket(token)
	if err != nil {
		return Caller{}, err
	}
	return Caller{UserID: ticket.UserID, ProjectID: ticket.ProjectID, TenantID: ticket.TenantID}, nil
}

func parseDebugTicket(token string) (debugTicket, error) {
	parts := strings.Split(token, ".")
	if len(parts) != 2 {
		return debugTicket{}, ErrInvalidTicket
	}
	payload, err := base64.RawURLEncoding.DecodeString(parts[0])
	if err != nil {
		return debugTicket{}, ErrInvalidTicket
	}
	var ticket debugTicket
	if err := json.Unmarshal(payload, &ticket); err != nil {
		return debugTicket{}, ErrInvalidTicket
	}
	return ticket, nil
}

func requireDebugCaller(caller Caller) (Caller, error) {
	if caller.UserID == "" || !caller.HasTenantScope() {
		return Caller{}, ErrMissingIdentity
	}
	return caller, nil
}

var testTicketSecret []byte

// SetDebugTicketSecretForTest 注入独立测试密钥；传 nil 恢复配置/默认密钥。
func SetDebugTicketSecretForTest(secret []byte) {
	if secret == nil {
		testTicketSecret = nil
		return
	}
	testTicketSecret = append([]byte(nil), secret...)
}

func DebugTicketConfigured() bool {
	return len(debugTicketSecret()) > 0
}

func debugTicketSecret() []byte {
	if len(testTicketSecret) > 0 {
		return testTicketSecret
	}
	candidates := []string{os.Getenv(EnvDebugTicketSecret)}
	if config.Config != nil {
		candidates = append(candidates, config.Config.ApiServer.Auth.DebugTicketSecret)
	}
	for _, raw := range candidates {
		raw = strings.TrimSpace(raw)
		if raw == "" || raw == DefaultDebugTicketSecret {
			continue
		}
		return []byte(raw)
	}
	return nil
}

// FormatDebugBuilderURL 把票据放在 path 最后一段，避免 WebConsole 追加 ?targetHost= 时出现第二个 '?'。
func FormatDebugBuilderURL(gateway, prefix, podName, containerName, ticket string) string {
	return "ws://" + gateway + prefix + "/" + podName + "/" + containerName + "/" + ticket
}

// RedactDebugTicketURL 去掉 path 末段票据和 query ticket=，供日志使用。
func RedactDebugTicketURL(raw string) string {
	if raw == "" {
		return raw
	}
	path, query := raw, ""
	if i := strings.Index(raw, "?"); i >= 0 {
		path, query = raw[:i], raw[i:]
	}
	if i := strings.Index(path, "/debug/"); i >= 0 {
		rest := path[i+len("/debug/"):]
		parts := strings.Split(rest, "/")
		if len(parts) >= 3 && parts[2] != "" {
			parts[2] = "<redacted>"
			path = path[:i+len("/debug/")] + strings.Join(parts, "/")
		}
	}
	if i := strings.Index(query, "ticket="); i >= 0 {
		start := i + len("ticket=")
		if end := strings.IndexAny(query[start:], "&"); end >= 0 {
			query = query[:start] + "<redacted>" + query[start+end:]
		} else {
			query = query[:start] + "<redacted>"
		}
	}
	return path + query
}

// DebugTicketFromRequest 优先读 path，其次 query，兼容旧客户端。
func DebugTicketFromRequest(pathTicket, queryTicket string) string {
	if strings.TrimSpace(pathTicket) != "" {
		return strings.TrimSpace(pathTicket)
	}
	return strings.TrimSpace(queryTicket)
}

// RewriteWebConsoleProxy 复现 Kotlin getDebugWebsocketUrl 的拆散重组。
func RewriteWebConsoleProxy(wsURL, proxy string) string {
	list := strings.Split(wsURL, "/")
	if len(list) < 4 {
		return wsURL
	}
	targetHost := list[2]
	rest := strings.Join(list[3:], "/")
	sep := "?"
	if strings.Contains(rest, "?") {
		sep = "&"
	}
	return strings.TrimRight(proxy, "/") + "/" + rest + sep + "targetHost=" + targetHost
}

// TicketFromRewrittenDebugURL 从重组后的 URL 取出 path 末段票据。
func TicketFromRewrittenDebugURL(rewritten string) string {
	withoutScheme := rewritten
	if i := strings.Index(rewritten, "://"); i >= 0 {
		withoutScheme = rewritten[i+3:]
	}
	path := withoutScheme
	if i := strings.Index(path, "?"); i >= 0 {
		path = path[:i]
	}
	path = strings.Trim(path, "/")
	if path == "" {
		return ""
	}
	parts := strings.Split(path, "/")
	return parts[len(parts)-1]
}
