/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package cl

import (
	"fmt"
)

// errors for cl.exe
var (
	ErrorMissingOption          = fmt.Errorf("missing option/operand")
	ErrorInvalidOption          = fmt.Errorf("invalid option/operand")
	ErrorUnrecognizedOption     = fmt.Errorf("unrecognized option")
	ErrorNoAvailable4Remote     = fmt.Errorf("no available for remote")
	ErrorFileNotExist           = fmt.Errorf("file/path not exist")
	ErrorFileInvalid            = fmt.Errorf("file/path invalid")
	ErrorInvalidParam           = fmt.Errorf("param is invalid")
	ErrorNoResultFile           = fmt.Errorf("no result file")
	ErrorNotSupportE            = fmt.Errorf("/E /EP /P must be local")
	ErrorNotSupportYc           = fmt.Errorf("/Yc must be local")
	ErrorNotSupportYcStart      = fmt.Errorf("option start with /Yc must be local")
	ErrorNotSupportOutputStdout = fmt.Errorf("output to stdout, must be local")
	ErrorNoPumpHeadFile         = fmt.Errorf("pump head file not exist")
	ErrorNoDependFile           = fmt.Errorf("depend file not exist")
	ErrorInvalidDependFile      = fmt.Errorf("depend file invalid")
	ErrorNotRemoteTask          = fmt.Errorf("not remote task")
	ErrorNotSupportRemote       = fmt.Errorf("not support to remote execute")
	ErrorInPumpBlack            = fmt.Errorf("in pump black list")
)
