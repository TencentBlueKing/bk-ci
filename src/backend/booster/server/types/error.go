package types

import (
	"fmt"
)

var (
	ErrorManagerNotRunning     = fmt.Errorf("manager is not running")
	ErrorInvalidIPV4           = fmt.Errorf("invalid ip v4")
	ErrorIPNotAllowed          = fmt.Errorf("ip not allowed")
	ErrorConcurrencyLimit      = fmt.Errorf("the task concurrency reaches the limits")
	ErrorGenerateTaskIDFailed  = fmt.Errorf("generate task id failed")
	ErrorTaskAlreadyTerminated = fmt.Errorf("task is already in terminated status")
	ErrorLeaderNoFound         = fmt.Errorf("leader no found")
)
