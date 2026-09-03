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
	assert.NoError(t, AuthorizeDebugSession(Caller{}, ticket, "pod-1", "ctr-1", pod, now))
	assert.ErrorIs(t, AuthorizeDebugSession(Caller{UserID: "bob", ProjectID: "proj-b"}, ticket, "pod-1", "ctr-1", pod, now), ErrInvalidTicket)
	assert.ErrorIs(t, AuthorizeDebugSession(caller, "", "pod-1", "ctr-1", pod, now), ErrInvalidTicket)

	otherPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Labels: map[string]string{LabelProjectID: "proj-b"},
		},
	}
	assert.ErrorIs(t, AuthorizeDebugSession(caller, ticket, "pod-1", "ctr-1", otherPod, now), ErrObjectUnowned)
}

func TestIssueDebugTicketForPod_UsesOwnerNotClaimedCaller(t *testing.T) {
	now := time.Now()
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name:   "pod-1",
			Labels: map[string]string{LabelProjectID: "proj-a", LabelUserID: "alice"},
		},
	}
	claimed := Caller{UserID: "mallory", ProjectID: "proj-a"}
	_, err := IssueDebugTicketForPod(claimed, pod, "ctr-1", now)
	assert.ErrorIs(t, err, ErrForbidden)

	ticket, err := IssueDebugTicketForPod(Caller{UserID: "alice", ProjectID: "proj-a"}, pod, "ctr-1", now)
	assert.NoError(t, err)
	subject, err := DebugTicketSubject(ticket)
	assert.NoError(t, err)
	assert.Equal(t, "alice", subject.UserID)
	assert.Equal(t, "proj-a", subject.ProjectID)
	assert.NoError(t, AuthorizeDebugSession(Caller{}, ticket, "pod-1", "ctr-1", pod, now))
}

func TestKnownSharedTokenSignedTicketRejected(t *testing.T) {
	now := time.Now()
	SetDebugTicketSecretForTest([]byte("server-high-entropy-secret"))
	defer SetDebugTicketSecretForTest(nil)

	victim := Caller{UserID: "alice", ProjectID: "proj-a"}
	forgedToken, err := SignDebugTicketWithSecret(victim, "pod-1", "ctr-1", now, []byte("shared-devops-token"))
	assert.NoError(t, err)
	assert.ErrorIs(t, VerifyDebugTicket(forgedToken, victim, "pod-1", "ctr-1", now), ErrInvalidTicket)

	forgedHardcoded, err := SignDebugTicketWithSecret(victim, "pod-1", "ctr-1", now, []byte("dispatch-k8s-manager-debug-ticket"))
	assert.NoError(t, err)
	assert.ErrorIs(t, VerifyDebugTicket(forgedHardcoded, victim, "pod-1", "ctr-1", now), ErrInvalidTicket)

	legit, err := IssueDebugTicket(victim, "pod-1", "ctr-1", now)
	assert.NoError(t, err)
	assert.NoError(t, VerifyDebugTicket(legit, victim, "pod-1", "ctr-1", now))
}

func TestDebugTicketSecretConsistentWhenUnconfigured(t *testing.T) {
	SetDebugTicketSecretForTest(nil)
	// 模拟副本 A/B：空配置必须读到同一确定密钥，而不能是进程内随机。
	replicaA := append([]byte(nil), debugTicketSecret()...)
	replicaB := append([]byte(nil), debugTicketSecret()...)
	assert.Equal(t, DefaultDebugTicketSecret, string(replicaA))
	assert.Equal(t, replicaA, replicaB)

	now := time.Now()
	caller := Caller{UserID: "alice", ProjectID: "proj-a"}
	ticket, err := IssueDebugTicket(caller, "pod-1", "ctr-1", now)
	assert.NoError(t, err)
	assert.NoError(t, VerifyDebugTicket(ticket, caller, "pod-1", "ctr-1", now))

	// 知道公开默认值即可自签：这是多副本可用性的取舍，生产必须覆盖 Helm/config。
	forged, err := SignDebugTicketWithSecret(caller, "pod-1", "ctr-1", now, []byte(DefaultDebugTicketSecret))
	assert.NoError(t, err)
	assert.NoError(t, VerifyDebugTicket(forged, caller, "pod-1", "ctr-1", now))
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

func TestRedactDebugTicketURL(t *testing.T) {
	ticket := "e30.signedticketvalue"
	path := "/api/builders/debug/pod-1/ctr-1/" + ticket
	assert.Equal(t, "/api/builders/debug/pod-1/ctr-1/<redacted>", RedactDebugTicketURL(path))
	assert.NotContains(t, RedactDebugTicketURL("ws://h"+path+"?targetHost=x"), ticket)
	assert.Equal(t, "/api/builders/debug/pod-1/ctr-1?ticket=<redacted>",
		RedactDebugTicketURL("/api/builders/debug/pod-1/ctr-1?ticket="+ticket))
	assert.Equal(t, "/api/builders/foo/status", RedactDebugTicketURL("/api/builders/foo/status"))
}
