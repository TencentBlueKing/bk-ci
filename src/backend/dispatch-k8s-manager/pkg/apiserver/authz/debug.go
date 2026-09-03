package authz

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"strings"
	"time"

	"disaptch-k8s-manager/pkg/config"

	corev1 "k8s.io/api/core/v1"
)

const (
	debugTicketTTL = 10 * time.Minute
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
	mac := hmac.New(sha256.New, debugTicketSecret())
	_, _ = mac.Write(payload)
	return base64.RawURLEncoding.EncodeToString(payload) + "." + base64.RawURLEncoding.EncodeToString(mac.Sum(nil)), nil
}

func VerifyDebugTicket(token string, caller Caller, podName, containerName string, now time.Time) error {
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
	if AuthorizeObject(caller, ticketCaller.Owner()) != nil {
		return ErrInvalidTicket
	}
	return nil
}

// AuthorizeDebugSession 必须在 WebSocket Upgrade 之前调用：身份 + 票据 + 对象属主。
func AuthorizeDebugSession(caller Caller, ticket string, podName, containerName string, pod *corev1.Pod, now time.Time) error {
	if _, err := requireDebugCaller(caller); err != nil {
		return err
	}
	if err := VerifyDebugTicket(ticket, caller, podName, containerName, now); err != nil {
		return err
	}
	return AuthorizeObject(caller, OwnerFromPod(pod))
}

func requireDebugCaller(caller Caller) (Caller, error) {
	if caller.UserID == "" || !caller.HasTenantScope() {
		return Caller{}, ErrMissingIdentity
	}
	return caller, nil
}

func debugTicketSecret() []byte {
	if config.Config != nil && config.Config.ApiServer.Auth.ApiToken.Value != "" {
		return []byte(config.Config.ApiServer.Auth.ApiToken.Value)
	}
	return []byte("dispatch-k8s-manager-debug-ticket")
}
