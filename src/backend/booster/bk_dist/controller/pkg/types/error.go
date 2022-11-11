/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package types

import "fmt"

// define exported error messages
var (
	ErrWorkIDGenerateFailed         = fmt.Errorf("work id generate failed")
	ErrNoAvailableWorkFound         = fmt.Errorf("no available work found")
	ErrWorkCannotBeRegistered       = fmt.Errorf("work can not be registered")
	ErrWorkCannotBeUnregistered     = fmt.Errorf("work can not be unregistered")
	ErrHostSlotOverLimit            = fmt.Errorf("host slot over limit")
	ErrHostNoFound                  = fmt.Errorf("host no found")
	ErrWorkNoFound                  = fmt.Errorf("work no found")
	ErrNoWork                       = fmt.Errorf("not any work")
	ErrNoAvailableHostSlotFound     = fmt.Errorf("no available host slot found")
	ErrWorkResourceCannotBeSet      = fmt.Errorf("work resource can not be set")
	ErrWorkSettingsCannotBeSet      = fmt.Errorf("work settings can not be set")
	ErrWorkIsNotWorking             = fmt.Errorf("work is not working")
	ErrWorkUnknownJobUsage          = fmt.Errorf("unknown job usage")
	ErrWorkNoAvailableJobUsageFound = fmt.Errorf("no available job usage found")
	ErrWorkCannotBeStart            = fmt.Errorf("work can not be start")
	ErrWorkCannotBeEnd              = fmt.Errorf("work can not be end")
	ErrFileNotFound                 = fmt.Errorf("not found file info")
	ErrWorkCannotBeUpdatedHeartbeat = fmt.Errorf("work can not be updated heartbeat")
	ErrSendFileFailed               = fmt.Errorf("send file failed")
	ErrTaskCannotBeReleased         = fmt.Errorf("task can not be released")
	ErrTaskAlreadyReleased          = fmt.Errorf("task already released")
	ErrSlotsLockFailed              = fmt.Errorf("slots lock failed`")
	ErrNoWaitingTask                = fmt.Errorf("no waitting task")
	ErrFileLock                     = fmt.Errorf("lock file failed")
)
