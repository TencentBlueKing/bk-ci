/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package types

// BoosterType string
type BoosterType string

// define task types
var (
	BoosterCC      BoosterType = "cc"
	BoosterFind    BoosterType = "find"
	BoosterTC      BoosterType = "tc"
	BoosterCL      BoosterType = "cl"
	BoosterShader  BoosterType = "shader"
	BoosterUE4     BoosterType = "ue4"
	BoosterClangCl BoosterType = "clang-cl"
	BoosterEcho    BoosterType = "echo"
	BoosterCustom  BoosterType = "custom"
	BoosterUnknown BoosterType = "unknown"

)

// String return the string of BoosterType
func (t BoosterType) String() string {
	return string(t)
}

var (
	str2taskType = map[string]BoosterType{
		BoosterCC.String():     BoosterCC,
		BoosterFind.String():   BoosterFind,
		BoosterTC.String():     BoosterTC,
		BoosterCL.String():     BoosterCL,
		BoosterShader.String(): BoosterShader,
		BoosterUE4.String():    BoosterUE4,
		BoosterClangCl.String() : BoosterClangCl,
		BoosterEcho.String():   BoosterEcho,
		BoosterCustom.String(): BoosterCustom,
	}
)

// GetBoosterType get task type by string
func GetBoosterType(key string) BoosterType {
	if v, ok := str2taskType[key]; ok {
		return v
	}

	return BoosterUnknown
}
