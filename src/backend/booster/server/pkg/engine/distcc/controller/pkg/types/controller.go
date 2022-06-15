package types

import (
	"time"
)

var (
	InspectRunningTaskTimeGap     = 500 * time.Millisecond
	InspectDistCCStatTimeGap      = 5 * time.Second
	CheckProjectTimeGap           = 5 * time.Minute
	LastSuggestionAcceptedTimeGap = 7 * 24 * time.Hour
)

const (
	IgnoreTaskAfterRunningTimeSecond = 20
	LastFinishedTaskTimeRangeSecond  = 60 * 15
	MaxJobsSampleLimitMaxTimes       = 5

	ListKeyTaskID     = "task_id"
	ListKeyProjectID  = "project_id"
	ListKeyObserved   = "observed"
	ListKeyReleased   = "released"
	ListKeyStatus     = "status"
	ListKeyMaxJobs    = "max_jobs"
	ListKeyCPUTotal   = "cpu_total"
	ListKeyCreateTime = "create_time"
	ListKeyRequestCPU = "request_cpu"
	ListKeySuggestCPU = "suggest_cpu"
	ListKeyAcTime     = "accepted_time"
)
