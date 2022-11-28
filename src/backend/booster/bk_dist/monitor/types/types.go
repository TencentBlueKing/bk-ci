/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package types

type Rule struct {
	Name        string              `json:"name"`
	IntervalMS  int                 `json:"interval_ms"`
	CheckCmd    string              `json:"check_cmd"`
	RecoverCmds map[string][]string `json:"recover_cmds"`
}

type Rules struct {
	DefaultIntervalMS int    `json:"default_interval_ms"`
	AllRules          []Rule `json:"all_rules"`
}
