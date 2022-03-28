/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package link

import (
	"fmt"
)

// errors for link.exe
var (
	ErrorMissingOption      = fmt.Errorf("missing option/operand")
	ErrorInvalidOption      = fmt.Errorf("invalid option/operand")
	ErrorUnrecognizedOption = fmt.Errorf("unrecognized option")
	ErrorNoAvailable4Remote = fmt.Errorf("no available for remote")
	ErrorFileNotExist       = fmt.Errorf("file/path not exist")
	ErrorFileInvalid        = fmt.Errorf("file/path invalid")
	ErrorInvalidParam       = fmt.Errorf("param is invalid")
)
