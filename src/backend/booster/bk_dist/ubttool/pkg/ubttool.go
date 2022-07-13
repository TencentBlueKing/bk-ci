/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package pkg

import (
	"context"
	"fmt"
	"os"
	"strconv"
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	v1 "github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/api/v1"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/ubttool/common"
	"github.com/Tencent/bk-ci/src/booster/common/blog"

	"github.com/google/shlex"
)

const (
	OSWindows   = "windows"
	MaxWaitSecs = 10800
	TickSecs    = 30
	DefaultJobs = 240 // ok for most machines
)

// NewUBTTool get a new UBTTool
func NewUBTTool(flagsparam *common.Flags, config dcSDK.ControllerConfig) *UBTTool {
	blog.Infof("UBTTool: new helptool with config:%+v,flags:%+v", config, *flagsparam)

	return &UBTTool{
		flags:          flagsparam,
		controller:     v1.NewSDK(config),
		allactions:     []common.Action{},
		readyactions:   []common.Action{},
		finishednumber: 0,
		runningnumber:  0,
		maxjobs:        0,
		finished:       false,
		actionchan:     nil,
		executor:       NewExecutor(),
	}
}

// UBTTool describe the ubt tool handler
type UBTTool struct {
	flags *common.Flags

	controller dcSDK.ControllerSDK

	// lock for full actions
	allactionlock sync.RWMutex
	allactions    []common.Action

	// lock for ready actions
	readyactionlock sync.RWMutex
	readyactions    []common.Action

	finishednumberlock sync.Mutex
	finishednumber     int32
	runningnumber      int32
	maxjobs            int32
	finished           bool

	actionchan chan common.Actionresult

	executor *Executor
}

// Run run the tool
func (h *UBTTool) Run(ctx context.Context) (int, error) {
	return h.run(ctx)
}

func (h *UBTTool) run(pCtx context.Context) (int, error) {
	blog.Infof("UBTTool: try to find controller or launch it")
	_, err := h.controller.EnsureServer()
	if err != nil {
		blog.Errorf("UBTTool: ensure controller failed: %v", err)
		return 1, err
	}
	blog.Infof("UBTTool: success to connect to controller")

	if !h.executor.Valid() {
		blog.Errorf("UBTTool: ensure controller failed: %v", ErrorInvalidWorkID)
		return 1, ErrorInvalidWorkID
	}

	// run actions now
	err = h.runActions()
	if err != nil {
		return 1, err
	}

	return 0, nil
}

func (h *UBTTool) runActions() error {
	blog.Infof("UBTTool: try to run actions")

	if h.flags.ActionChainFile == "" || h.flags.ActionChainFile == "nothing" {
		blog.Debugf("UBTTool: action json file not set, do nothing now")
		return nil
	}

	all, err := resolveActionJSON(h.flags.ActionChainFile)
	if err != nil {
		blog.Warnf("UBTTool: failed to resolve %s with error:%v", h.flags.ActionChainFile, err)
		return err
	}
	// for debug
	blog.Debugf("UBTTool: all actions:%+v", all)

	// execute actions here
	h.allactions = all.Actions
	// readyactions includes actions which no depend
	err = h.getReadyActions()
	if err != nil {
		blog.Warnf("UBTTool: failed to get ready actions with error:%v", err)
		return err
	}

	err = h.executeActions()
	if err != nil {
		blog.Warnf("UBTTool: failed to run actions with error:%v", err)
		return err
	}

	blog.Debugf("UBTTool: success to execute all %d actions", len(h.allactions))
	return nil
}

// execute actions got from ready queue
func (h *UBTTool) executeActions() error {
	blog.Infof("UBTTool: try to run actions")

	h.maxjobs = DefaultJobs

	// get max jobs from env
	maxjobstr := env.GetEnv(env.KeyCommonUE4MaxJobs)
	maxjobs, err := strconv.Atoi(maxjobstr)
	if err != nil {
		blog.Infof("UBTTool: failed to get jobs by UE4_MAX_PROCESS with error:%v", err)
	} else {
		h.maxjobs = int32(maxjobs)
	}

	fmt.Fprintf(os.Stderr, "UBTTool: Building %d actions with %d jobs...", len(h.allactions), h.maxjobs)

	h.dump()

	// execute actions no more than max jobs
	blog.Infof("UBTTool: try to run actions with %d jobs", h.maxjobs)
	h.actionchan = make(chan common.Actionresult, h.maxjobs)

	// execute first batch actions
	h.selectActionsToExecute()
	if h.runningnumber <= 0 {
		blog.Errorf("UBTTool: faile to execute actions with error:%v", ErrorNoActionsToRun)
		return ErrorNoActionsToRun
	}

	for {
		tick := time.NewTicker(TickSecs * time.Second)
		starttime := time.Now()
		select {
		case r := <-h.actionchan:
			blog.Infof("UBTTool: got action result:%+v", r)
			if r.Exitcode != 0 {
				err := fmt.Errorf("exit code:%d", r.Exitcode)
				blog.Errorf("UBTTool: %v", err)
				return err
			}
			h.onActionFinished(r.Index, r.Exitcode)
			if h.finished {
				blog.Infof("UBTTool: all actions finished")
				return nil
			}
			h.selectActionsToExecute()
			if h.runningnumber <= 0 {
				blog.Errorf("UBTTool: faile to execute actions with error:%v", ErrorNoActionsToRun)
				return ErrorNoActionsToRun
			}

		case <-tick.C:
			curtime := time.Now()
			blog.Infof("start time [%s] current time [%s] ", starttime, curtime)
			if curtime.Sub(starttime) > (time.Duration(MaxWaitSecs) * time.Second) {
				blog.Errorf("UBTTool: faile to execute actions with error:%v", ErrorOverMaxTime)
				return ErrorOverMaxTime
			}
			h.dump()
		}
	}
}

func (h *UBTTool) selectActionsToExecute() error {
	h.readyactionlock.Lock()
	defer h.readyactionlock.Unlock()

	for h.runningnumber < h.maxjobs {
		index := h.selectReadyAction()
		if index < 0 { // no tasks to run
			return nil
		}

		h.readyactions[index].Running = true
		h.runningnumber++
		_, _ = fmt.Fprintf(os.Stdout, "[bk_ubt_tool] finished:%d,running:%d,total:%d, current action: %s %s\n",
			h.finishednumber, h.runningnumber, len(h.allactions),
			h.readyactions[index].Cmd, h.readyactions[index].Arg)
		go h.executeOneAction(h.readyactions[index], h.actionchan)
	}

	return nil
}

func (h *UBTTool) selectReadyAction() int {
	index := -1
	followers := -1
	// select ready action which is not running and has most followers
	if h.flags.MostDepentFirst {
		for i := range h.readyactions {
			if !h.readyactions[i].Running {
				curfollowers := len(h.readyactions[i].FollowIndex)
				if curfollowers > followers {
					index = i
					followers = curfollowers
				}
			}
		}
	} else { // select first action which is not running
		for i := range h.readyactions {
			if !h.readyactions[i].Running {
				index = i
				break
			}
		}
	}

	if index >= 0 {
		blog.Infof("UBTTool: selected global index %s with %d followers", h.readyactions[index].Index, followers)
	}
	return index
}

func (h *UBTTool) executeOneAction(action common.Action, actionchan chan common.Actionresult) error {
	blog.Infof("UBTTool: ready execute actions:%+v", action)

	fullargs := []string{action.Cmd}
	args, _ := shlex.Split(replaceWithNextExclude(action.Arg, '\\', "\\\\", []byte{'"'}))
	fullargs = append(fullargs, args...)

	//exitcode, err := h.executor.Run(fullargs, action.Workdir)
	// try again if failed after sleep some time
	var exitcode int
	waitsecs := 5
	var err error
	for try := 0; try < 6; try++ {
		exitcode, err = h.executor.Run(fullargs, action.Workdir)
		if err != nil {
			blog.Warnf("UBTTool: failed to execute action with error [%+v] for %d times, actions:%+v", err, try, action)
			time.Sleep(time.Duration(waitsecs) * time.Second)
			waitsecs = waitsecs * 2
			continue
		}
		break
	}

	r := common.Actionresult{
		Index:     action.Index,
		Finished:  true,
		Succeed:   err == nil,
		Outputmsg: "",
		Errormsg:  "",
		Exitcode:  exitcode,
	}

	actionchan <- r

	return nil
}

// get ready actions from all actions
func (h *UBTTool) getReadyActions() error {
	blog.Infof("UBTTool: try to get ready actions")

	h.allactionlock.Lock()
	defer h.allactionlock.Unlock()

	h.readyactionlock.Lock()
	defer h.readyactionlock.Unlock()

	// copy actions which no depend from all to ready
	for i, v := range h.allactions {
		if !v.Running && !v.Finished && len(v.Dep) == 0 {
			h.readyactions = append(h.readyactions, v)
			h.allactions[i].Running = true
		}
	}

	return nil
}

// update all actions and ready actions
func (h *UBTTool) onActionFinished(index string, exitcode int) error {
	blog.Infof("UBTTool: action %s finished with exitcode %d", index, exitcode)

	h.finishednumberlock.Lock()
	h.finishednumber++
	h.runningnumber--
	blog.Infof("UBTTool: running : %d, finished : %d, total : %d", h.runningnumber, h.finishednumber, len(h.allactions))
	if h.finishednumber >= int32(len(h.allactions)) {
		h.finishednumberlock.Unlock()
		blog.Infof("UBTTool: finishend")
		h.finished = true
		return nil
	}
	h.finishednumberlock.Unlock()

	// update with index
	h.allactionlock.Lock()
	defer h.allactionlock.Unlock()

	h.readyactionlock.Lock()
	defer h.readyactionlock.Unlock()

	// delete from ready array
	for i, v := range h.readyactions {
		if v.Index == index {
			h.readyactions = removeaction(h.readyactions, i)
			break
		}
	}

	// update status in allactions
	for i, v := range h.allactions {
		// update status
		if v.Index == index {
			h.allactions[i].Finished = true
			break
		}
	}

	// update depend in allactions if current action succeed
	if exitcode == 0 {
		for i, v := range h.allactions {
			if v.Finished {
				continue
			}

			// update depend
			for i1, v1 := range v.Dep {
				if v1 == index {
					h.allactions[i].Dep = remove(h.allactions[i].Dep, i1)
					break
				}
			}

			// copy to ready if no depent
			if !v.Running && !v.Finished && len(h.allactions[i].Dep) == 0 {
				h.readyactions = append(h.readyactions, v)
				h.allactions[i].Running = true
			}
		}
	}

	return nil
}

func (h *UBTTool) dump() {
	blog.Infof("UBTTool: +++++++++++++++++++dump start+++++++++++++++++++++")
	blog.Infof("UBTTool: finished:%d,running:%d,total:%d", h.finishednumber, h.runningnumber, len(h.allactions))

	for _, v := range h.allactions {
		blog.Infof("UBTTool: action:%+v", v)
	}
	blog.Infof("UBTTool: -------------------dump end-----------------------")
}
