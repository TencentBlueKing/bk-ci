package service

import (
	"testing"

	"disaptch-k8s-manager/pkg/apiserver/authz"

	"github.com/stretchr/testify/assert"
)

func TestMergeStartOwnerLabels_KeepsExistingAndBackfillsMissing(t *testing.T) {
	dispatch := map[string]string{"workload": "build1"}
	existing := authz.Owner{UserID: "alice", ProjectID: "proj-a"}
	claimed := authz.Owner{UserID: "mallory", ProjectID: "proj-b"}

	got := mergeStartOwnerLabels(dispatch, existing, claimed)
	assert.Equal(t, "alice", got[authz.LabelUserID], "已有属主不得被 start 请求者覆盖")
	assert.Equal(t, "proj-a", got[authz.LabelProjectID])
	assert.Equal(t, "build1", got["workload"])

	got = mergeStartOwnerLabels(map[string]string{"workload": "build1"}, authz.Owner{}, claimed)
	assert.Equal(t, "mallory", got[authz.LabelUserID], "缺失才补请求者")
	assert.Equal(t, "proj-b", got[authz.LabelProjectID])

	got = mergeStartOwnerLabels(map[string]string{"workload": "build1"}, authz.Owner{}, authz.Owner{})
	assert.Empty(t, got[authz.LabelUserID])
	assert.Empty(t, got[authz.LabelProjectID])
}
