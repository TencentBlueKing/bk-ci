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
	"math/rand"
	"os"
	"path/filepath"
	"strings"
	"time"

	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/monitor/types"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/shirou/gopsutil/process"
)

const (
	RulesFileName            = "bk_monitor_rules.json"
	DevOPSProcessTreeKillKey = "DEVOPS_DONT_KILL_PROCESS_TREE"

	DefautInterval = 20000

	BlockSecondsWhenRecover = 240

	MacroKillTree = "${BK_KILL_TREE}"
)

// NewMonitor get a new idle loop instance
func NewMonitor(rf string) *Monitor {
	return &Monitor{rulesfile: rf}
}

// Monitor to check other bk-dist process status
type Monitor struct {
	selfexepath string
	rules       *types.Rules
	rulesfile   string
}

// Run brings up idle loop
func (m *Monitor) Run(ctx context.Context) (int, error) {
	return m.run(ctx)
}

func (m *Monitor) run(pCtx context.Context) (int, error) {
	defer blog.CloseLogs()

	m.intEnv()
	err := m.initRules()
	if err != nil {
		blog.Errorf("monitor: failed to init rules with err:%v", err)
		return 1, nil
	}

	go m.runMonitors(pCtx)

	m.runIdleLoop(pCtx)

	return 0, nil
}

func (m *Monitor) runIdleLoop(ctx context.Context) {
	for {
		time.Sleep(3600 * time.Second)
	}
}

func (m *Monitor) runMonitors(ctx context.Context) {
	for _, v := range m.rules.AllRules {
		go m.runMonitor(m.rules.DefaultIntervalMS, v, ctx)
		time.Sleep(time.Duration(rand.Intn(3)) * time.Second)
	}
}

func (m *Monitor) runMonitor(interval int, r types.Rule, ctx context.Context) {
	if r.IntervalMS <= 0 {
		r.IntervalMS = interval
	}

	if r.IntervalMS <= 0 {
		r.IntervalMS = DefautInterval
	}

	ticker := time.NewTicker(time.Duration(r.IntervalMS) * time.Millisecond)
	for {
		select {
		case <-ctx.Done():
			blog.Debugf("monitor: controller check canceled by context")
			return

		case <-ticker.C:
			blog.Debugf("monitor: check controller unexpected fork now")
			m.runRule(r)
		}
	}
}

func (m *Monitor) runRule(r types.Rule) {
	blog.Infof("monitor: ready run check cmd:[%s]", r.CheckCmd)
	// sandbox := dcSyscall.Sandbox{}
	exitcode, outmsg, errmsg, err := m.runCommand(r.CheckCmd)
	if exitcode != 0 {
		blog.Errorf("monitor: failed to execute cmd:[%s] with exit code:%d, err:%v", r.CheckCmd, exitcode, err)
		return
	}
	blog.Infof("monitor: succeed to execute cmd:[%s] with exit code:%d,output:%s,errmsg:%s, err:%v",
		r.CheckCmd, exitcode, outmsg, errmsg, err)

	if len(r.RecoverCmds) > 0 {
		if v, ok := r.RecoverCmds[string(outmsg)]; ok {
			// for _, c := range v {
			// 	blog.Infof("monitor: ready run recover cmd:[%s]", c)
			// 	exitcode, outmsg, errmsg, err = m.runCommand(c)
			// 	if exitcode != 0 {
			// 		blog.Errorf("monitor: failed to execute cmd:[%s] with exit code:%d, err:%v", c, exitcode, err)
			// 		return
			// 	}
			// 	blog.Infof("monitor: succeed to execute cmd:[%s] with exit code:%d,output:%s,errmsg:%s, err:%v",
			// 		c, exitcode, outmsg, errmsg, err)
			// }
			go m.runRecoverCommands(v)
			blog.Infof("monitor: ready sleep %d second to wait execute recover commands", BlockSecondsWhenRecover)
			time.Sleep(BlockSecondsWhenRecover * time.Second)
		} else {
			trimkey := strings.Trim(string(outmsg), "\r\n \t")
			if trimkey != string(outmsg) {
				if v, ok := r.RecoverCmds[trimkey]; ok {
					// for _, c := range v {
					// 	blog.Infof("monitor: ready run recover cmd:[%s]", c)
					// 	exitcode, outmsg, errmsg, err = m.runCommand(c)
					// 	if exitcode != 0 {
					// 		blog.Errorf("monitor: failed to execute cmd:[%s] with exit code:%d, err:%v", c, exitcode, err)
					// 		return
					// 	}
					// 	blog.Infof("monitor: succeed to execute cmd:[%s] with exit code:%d,output:%s,errmsg:%s, err:%v",
					// 		c, exitcode, outmsg, errmsg, err)
					// }
					go m.runRecoverCommands(v)
					blog.Infof("monitor: ready sleep %d second to wait execute recover commands", BlockSecondsWhenRecover)
					time.Sleep(BlockSecondsWhenRecover * time.Second)
				}
			}
		}
	}
}

func (m *Monitor) runRecoverCommands(cmds []string) {
	blog.Infof("monitor: ready run recover cmds:%+v", cmds)

	for _, c := range cmds {
		blog.Infof("monitor: ready run recover cmd:[%s]", c)
		exitcode, outmsg, errmsg, err := m.runCommand(c)
		if exitcode != 0 {
			blog.Errorf("monitor: failed to execute cmd:[%s] with exit code:%d, err:%v", c, exitcode, err)
			return
		}
		blog.Infof("monitor: succeed to execute cmd:[%s] with exit code:%d,output:%s,errmsg:%s, err:%v",
			c, exitcode, outmsg, errmsg, err)
	}
}

func (m *Monitor) runCommand(c string) (int, []byte, []byte, error) {
	if strings.Contains(c, MacroKillTree) {
		return m.killTree(c)
	} else {
		sandbox := dcSyscall.Sandbox{}
		return sandbox.ExecScriptsWithMessage(c)
	}
}

func (m *Monitor) killTree(c string) (int, []byte, []byte, error) {
	args := strings.Split(c, " ")
	if len(args) != 2 {
		return -1, []byte(""), []byte(""), fmt.Errorf("[%s] is not valid kill tree args", c)
	}

	procs, err := dcUtil.ListProcess(args[1])
	if err != nil {
		return -1, []byte(""), []byte(""), err
	}

	if len(procs) == 0 {
		blog.Infof("monitor: not found any process with name:%s", args[1])
		return 0, []byte(""), []byte(""), nil
	}

	// 如果是 bk-dist-monitor 自己拉起来的，还需要主动释放下，避免留下脏进程
	proc1 := []*process.Process{}
	for _, v := range procs {
		newp, err := m.searchRootProc(v)
		if err == nil {
			proc1 = append(proc1, newp)
		} else {
			proc1 = append(proc1, v)
		}
	}

	for _, v := range proc1 {
		name, _ := v.Name()
		blog.Infof("monitor: ready kill process %s %d", name, int32(v.Pid))
		KillChildren(v)
		err := KillProcess(v)
		if err != nil {
			blog.Infof("monitor: kill process %s %d failed with err:%v", name, int32(v.Pid), err)
		}
	}

	return 0, []byte(""), []byte(""), nil
}

// 如果有祖先进程是 bk-dist-monitor 自己拉起来的，则返回该祖先，否则返回自己
func (m *Monitor) searchRootProc(p *process.Process) (*process.Process, error) {
	selfpid := os.Getpid()
	newp := p
	ppid, err := newp.Ppid()
	if err != nil {
		blog.Infof("monitor: get parent pid with error:%v", err)
		return p, err
	}

	for {
		if ppid == int32(selfpid) {
			return newp, nil
		}

		newp, err = process.NewProcess(ppid)
		if err != nil {
			blog.Infof("monitor: get parent process with pid:%d with error:%v", ppid, err)
			return p, err
		}

		ppid, err = newp.Ppid()
		if err != nil {
			blog.Infof("monitor: get parent pid with error:%v", err)
			return p, err
		}
	}

}

func (m *Monitor) initRules() error {
	f, err := m.getRulesFile()
	if err != nil {
		return err
	}

	m.rules, err = resolveRules(f)
	if err != nil {
		return err
	}

	blog.Infof("monitor: loaded rules:%+v", *m.rules)

	os.Setenv(DevOPSProcessTreeKillKey, "true")

	return nil
}

func (m *Monitor) getRulesFile() (string, error) {
	if dcFile.Stat(m.rulesfile).Exist() {
		return m.rulesfile, nil
	}

	if m.selfexepath != "" {
		jsonfile := filepath.Join(m.selfexepath, RulesFileName)
		fmt.Printf("monitor: check rules file:%s\n", jsonfile)
		if dcFile.Stat(jsonfile).Exist() {
			return jsonfile, nil
		}
	}

	return "", ErrorRuleSettingNotExisted
}

func (m *Monitor) intEnv() error {
	m.selfexepath = dcUtil.GetExcPath()
	fmt.Printf("monitor: get self exe path:%s\n", m.selfexepath)
	if m.selfexepath != "" {
		blog.Infof("monitor: get self exe path:%s", m.selfexepath)
		os.Chdir(m.selfexepath)
		dcSyscall.AddPath2Env(m.selfexepath)
	} else {
		blog.Infof("monitor: not found self exe path:%s", m.selfexepath)
	}

	return nil
}
