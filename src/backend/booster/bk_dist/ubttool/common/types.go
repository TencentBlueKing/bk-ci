/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package common

// Flags to desc flag of this tool
type Flags struct {
	ActionChainFile string
	MostDepentFirst bool
}

// Action to desc single ubt action
type Action struct {
	Index       string   `json:"index"`
	Workdir     string   `json:"workdir"`
	Cmd         string   `json:"cmd"`
	Arg         string   `json:"arg"`
	Dep         []string `json:"dep"`
	FollowIndex []string `json:"followindex"` // follower index which depend on this
	Running     bool     `json:"running"`
	Finished    bool     `json:"finished"`
}

// UE4Action to desc ubt actions
type UE4Action struct {
	Actions []Action `json:"actions"`
}

// Actionresult to desc single action result
type Actionresult struct {
	Index     string
	Finished  bool
	Succeed   bool
	Outputmsg string
	Errormsg  string
	Exitcode  int
}

func uniqueAndCheck(strlist []string, allindex map[string]bool) []string {
	keys := make(map[string]bool)
	list := make([]string, 0, 0)
	for _, entry := range strlist {
		// remove "-1"
		if entry == "-1" {
			continue
		}

		// remove which not in actions list
		if _, ok := allindex[entry]; !ok {
			continue
		}

		if _, ok := keys[entry]; !ok {
			keys[entry] = true
			list = append(list, entry)
		}
	}
	return list
}

// UniqDeps to uniq depend index
func (a *UE4Action) UniqDeps() error {
	allindex := make(map[string]bool)
	for i := range a.Actions {
		allindex[a.Actions[i].Index] = true
	}

	for i := range a.Actions {
		if len(a.Actions[i].Dep) > 0 {
			a.Actions[i].Dep = uniqueAndCheck(a.Actions[i].Dep, allindex)
		}
	}

	return nil
}

// GenFolloweIndex to generate follower index
func (a *UE4Action) GenFolloweIndex() error {
	for i := range a.Actions {
		if len(a.Actions[i].Dep) > 0 {
			followindex := a.Actions[i].Index
			for _, v := range a.Actions[i].Dep {
				for j := range a.Actions {
					if a.Actions[j].Index == v {
						a.Actions[j].FollowIndex = append(a.Actions[j].FollowIndex, followindex)
						break
					}
				}
			}
		}
	}

	return nil
}
