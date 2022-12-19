package task

import (
	"disaptch-k8s-manager/pkg/types"
	"github.com/stretchr/testify/assert"
	"testing"
)

func Test_parseTaskLabelValue(t *testing.T) {
	sample := "t-1654567330641390000-xsActwac-job-create"

	assertTrue := assert.New(t)
	taskId, labelType, action, ok := parseTaskLabelValue(sample)
	assertTrue.Equal(ok, true)
	assertTrue.Equal(taskId, "t-1654567330641390000-xsActwac")
	assertTrue.Equal(labelType, types.JobTaskLabel)
	assertTrue.Equal(action, types.TaskActionCreate)

	sample2 := "job-create"

	_, _, _, ok = parseTaskLabelValue(sample2)
	assertTrue.Equal(ok, false)
}
