package authz

import (
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
