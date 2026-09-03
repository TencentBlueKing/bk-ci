package service

import (
	"testing"

	"disaptch-k8s-manager/pkg/apiserver/authz"
	"disaptch-k8s-manager/pkg/config"
	"disaptch-k8s-manager/pkg/kubeclient"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func TestDebugBuilderUrl_TicketSubjectIsPodOwner(t *testing.T) {
	config.Config.Gateway.Url = "127.0.0.1:8081"
	resolvePodAndContainer = func(string) (string, string, error) { return "pod-1", "ctr-1", nil }
	getPodForDebug = func(string) (*corev1.Pod, error) {
		return &corev1.Pod{
			ObjectMeta: metav1.ObjectMeta{
				Name: "pod-1",
				Labels: map[string]string{
					authz.LabelUserID:    "alice",
					authz.LabelProjectID: "proj-a",
				},
			},
		}, nil
	}
	defer func() {
		resolvePodAndContainer = getPodAndContainerName
		getPodForDebug = kubeclient.GetPod
	}()

	_, err := DebugBuilderUrl("/api/builders/debug", "b1", authz.Caller{UserID: "mallory", ProjectID: "proj-b"})
	assert.ErrorIs(t, err, authz.ErrForbidden)

	url, err := DebugBuilderUrl("/api/builders/debug", "b1", authz.Caller{UserID: "carol", ProjectID: "proj-a"})
	assert.NoError(t, err)
	ticket := authz.TicketFromRewrittenDebugURL(url)
	subject, err := authz.DebugTicketSubject(ticket)
	assert.NoError(t, err)
	assert.Equal(t, "alice", subject.UserID)
	assert.Equal(t, "proj-a", subject.ProjectID)
}
