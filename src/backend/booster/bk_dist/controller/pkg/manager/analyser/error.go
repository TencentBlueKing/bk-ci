/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package analyser

import "fmt"

var (
	ErrAnalyserInvalidParam            = fmt.Errorf("invalid param")
	ErrAnalyserFileNotFound            = fmt.Errorf("file not found")
	ErrAnalyserNodeTypeNoSupport       = fmt.Errorf("node type no support")
	ErrAnalyserGiveUpAnalysing         = fmt.Errorf("give up analysing")
	ErrAnalyserSystemIncludeDirUnknown = fmt.Errorf("system include dir unknown")
)
