package authz

import (
	"strings"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func TestDebugTicketBindAndExpire(t *testing.T) {
	now := time.Date(2026, 9, 3, 12, 0, 0, 0, time.UTC)
	caller := Caller{UserID: "alice", ProjectID: "proj-a"}

	ticket, err := IssueDebugTicket(caller, "pod-1", "ctr-1", now)
	assert.NoError(t, err)
	assert.NoError(t, VerifyDebugTicket(ticket, caller, "pod-1", "ctr-1", now.Add(time.Minute)))
	assert.ErrorIs(t, VerifyDebugTicket(ticket, caller, "pod-2", "ctr-1", now), ErrInvalidTicket)
	assert.ErrorIs(t, VerifyDebugTicket(ticket, Caller{UserID: "bob", ProjectID: "proj-b"}, "pod-1", "ctr-1", now), ErrInvalidTicket)
	assert.ErrorIs(t, VerifyDebugTicket("forged.ticket", caller, "pod-1", "ctr-1", now), ErrInvalidTicket)
	assert.ErrorIs(t, VerifyDebugTicket(ticket, caller, "pod-1", "ctr-1", now.Add(11*time.Minute)), ErrInvalidTicket)
	assert.ErrorIs(t, VerifyDebugTicket("", caller, "pod-1", "ctr-1", now), ErrInvalidTicket)
}

func TestAuthorizeDebugSession(t *testing.T) {
	now := time.Now()
	caller := Caller{UserID: "alice", ProjectID: "proj-a"}
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Labels: map[string]string{LabelProjectID: "proj-a", LabelUserID: "alice"},
		},
	}
	ticket, err := IssueDebugTicket(caller, "pod-1", "ctr-1", now)
	assert.NoError(t, err)

	assert.NoError(t, AuthorizeDebugSession(caller, ticket, "pod-1", "ctr-1", pod, now))
	assert.ErrorIs(t, AuthorizeDebugSession(Caller{UserID: "alice"}, ticket, "pod-1", "ctr-1", pod, now), ErrMissingIdentity)
	assert.ErrorIs(t, AuthorizeDebugSession(caller, "", "pod-1", "ctr-1", pod, now), ErrInvalidTicket)

	otherPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Labels: map[string]string{LabelProjectID: "proj-b"},
		},
	}
	assert.ErrorIs(t, AuthorizeDebugSession(caller, ticket, "pod-1", "ctr-1", otherPod, now), ErrForbidden)
}

func TestDebugTicketSurvivesWebConsoleProxyRewrite(t *testing.T) {
	now := time.Now()
	caller := Caller{UserID: "alice", ProjectID: "proj-a"}
	ticket, err := IssueDebugTicket(caller, "pod-1", "ctr-1", now)
	assert.NoError(t, err)

	raw := FormatDebugBuilderURL("127.0.0.1:8081", "/api/builders/debug", "pod-1", "ctr-1", ticket)
	assert.NotContains(t, raw, "?ticket=")

	rewritten := RewriteWebConsoleProxy(raw, "wss://web-console.example/proxy")
	assert.Contains(t, rewritten, "?targetHost=127.0.0.1:8081")
	assert.Equal(t, 1, strings.Count(rewritten, "?"))
	got := TicketFromRewrittenDebugURL(rewritten)
	assert.Equal(t, ticket, got)
	assert.NoError(t, VerifyDebugTicket(got, caller, "pod-1", "ctr-1", now))

	// 旧 query 写法：未修复的 Kotlin 会拼出第二个 '?'；修复后的 Rewrite 用 '&'。
	legacy := "ws://127.0.0.1:8081/api/builders/debug/pod-1/ctr-1?ticket=" + ticket
	fixedLegacy := RewriteWebConsoleProxy(legacy, "wss://web-console.example/proxy")
	assert.Equal(t, 1, strings.Count(fixedLegacy, "?"))
	assert.Contains(t, fixedLegacy, "&targetHost=127.0.0.1:8081")
}
