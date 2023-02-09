/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package cc

import (
	"fmt"
)

// define errors
var (
	ErrorMissingOption          = fmt.Errorf("missing option/operand")
	ErrorInvalidOption          = fmt.Errorf("invalid option/operand")
	ErrorUnrecognizedOption     = fmt.Errorf("unrecognized option")
	ErrorNoAvailable4Remote     = fmt.Errorf("no available for remote")
	ErrorFileNotExist           = fmt.Errorf("file/path not exist")
	ErrorFileInvalid            = fmt.Errorf("file/path invalid")
	ErrorInvalidParam           = fmt.Errorf("param is invalid")
	ErrorNotSupportE            = fmt.Errorf("-E must be local")
	ErrorNotSupportMarchNative  = fmt.Errorf("-march=native must be local")
	ErrorNotSupportMtuneNative  = fmt.Errorf("-mtune=native must be local")
	ErrorNotSupportCoverage     = fmt.Errorf("[-fprofile-arcs|-ftest-coverage|--coverage] must be local")
	ErrorNotSupportFrepo        = fmt.Errorf("-frepo must be local")
	ErrorNotSupportM            = fmt.Errorf("-M must be local")
	ErrorNotSupportWa           = fmt.Errorf("-Wa[,-a|--MD] must be local")
	ErrorNotSupportSpecs        = fmt.Errorf("-specs= must be local")
	ErrorNotSupportX            = fmt.Errorf("-x must be local")
	ErrorNotSupportDr           = fmt.Errorf("-dr must be local")
	ErrorNotSupportFsanitize    = fmt.Errorf("-fsanitize must be local")
	ErrorNotSupportConftest     = fmt.Errorf("tmp.conftest. must be local")
	ErrorNotSupportOutputStdout = fmt.Errorf("output with - to stdout, must be local")
	ErrorNotSupportGch          = fmt.Errorf("output with .gch, must be local")
	ErrorNoPumpHeadFile         = fmt.Errorf("pump head file not exist")
	ErrorNoDependFile           = fmt.Errorf("depend file not exist")
	ErrorNotSupportRemote       = fmt.Errorf("not support to remote execute")
	ErrorInPumpBlack            = fmt.Errorf("in pump black list")
)
