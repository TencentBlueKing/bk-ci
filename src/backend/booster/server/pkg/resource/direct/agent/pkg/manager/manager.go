/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package manager

import (
	"bytes"
	"context"
	"fmt"
	"math"
	"net/http"
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	commonHttp "github.com/Tencent/bk-ci/src/booster/common/http"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpclient"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/resource/direct/agent/config"
	register_discover "github.com/Tencent/bk-ci/src/booster/server/pkg/resource/direct/agent/pkg/register-discover"

	localCommon "github.com/Tencent/bk-ci/src/booster/server/pkg/resource/direct/agent/pkg/common"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/resource/direct/agent/pkg/types"
)

// Manager : to report resouce and manage local application
type Manager interface {
	ReportResource() error
	ExecuteCommand(res *types.NotifyAgentData) error
	// TODO : 缺少通知agent资源分配和释放的命令，先简单按资源独占处理
	Run() error
}

// define const strings
const (
	LabelKeyGOOS = "os"

	LabelValueGOOSWindows = "windows"
	LabelValueGOOSDarwin  = "darwin"
)

// NewManager ：to report resouce and manage local application
func NewManager(conf *config.ServerConfig, rd register_discover.RegisterDiscover) (Manager, error) {
	o := &processManager{
		client: httpclient.NewHTTPClient(),
		rd:     rd,
		conf:   conf,
		agent:  &types.AgentInfo{},
	}

	if err := o.init(); err != nil {
		blog.Infof("failed to init processManager,err[%v]", err)
		return nil, err
	}

	return o, nil
}

type processManager struct {
	client *httpclient.HTTPClient
	rd     register_discover.RegisterDiscover
	conf   *config.ServerConfig

	ctx    context.Context
	cancel context.CancelFunc

	serverURL string

	agent *types.AgentInfo

	usedresLock sync.RWMutex
}

// check whether application(s) are running, if existed, kill
func (o *processManager) init() error {
	blog.Infof("init...")
	// 杀掉指定进程
	err := o.waitToKillAllApplication()
	if err != nil {
		return err
	}

	// 不能通过进程名杀掉来释放资源的，执行配置的释放资源的命令
	err = o.releaseCmds()
	if err != nil {
		return err
	}

	err = o.initLocalIP()
	if err != nil {
		return err
	}

	err = o.initBaseInfo()
	if err != nil {
		return err
	}

	err = o.initTotalRes()
	if err != nil {
		return err
	}

	return nil
}

// ReportResource report resource status to logs
func (o *processManager) ReportResource() error {
	return o.reportResource()
}

// ExecuteCommand run command
func (o *processManager) ExecuteCommand(res *types.NotifyAgentData) error {
	blog.Infof("ExecuteCommand with res[%+v]", res)

	// set env
	if res.Env != nil {
		for k, v := range res.Env {
			if k == types.FbuildVersionEnvKey {
				blog.Infof("get version[%s]", v)
				continue
			}
			err := os.Setenv(k, v)
			if err != nil {
				return fmt.Errorf("set env %s=%s error: %v", k, v, err)
			}
			blog.Infof("set env [%s] to [%s]", k, v)
		}
	}

	pid := 0
	var err error
	// execute command with cmd / parameter and resources
	if res.CmdType == types.CmdLaunch {
		pid, err = o.startCommand(res.Dir, res.Path, res.Cmd, res.Parameters, int(0), int64(0), true)
		if err != nil {
			blog.Infof("failed to executeCommand,err[%v]", err)
			return err
		}

		command := &types.CommandInfo{
			ID:           strconv.Itoa(pid),
			Cmd:          res.Cmd,
			Port:         0,
			Status:       types.CommandStatusSucceed,
			UserDefineID: res.UserDefineID,
		}
		o.addCommand(res.UserID, res.ResBatchID, command)

		// ++ by tomtian 20190422, to report result immediately
		go o.reportResource()
		// --

		return nil
	} else if res.CmdType == types.CmdRelease {
		err = o.runCommand(res.Dir, res.Path, res.Cmd, res.Parameters)
		if err != nil {
			blog.Infof("failed to executeCommand,err[%v]", err)
			return err
		}
		// // free resource by taskid
		// blog.Infof("ready delete cache for taskid[%s]", res.UserDefineID)
		// o.delCommand(res.UserID, res.ResBatchID, res.ReferCmd, res.ReferID)
		go o.checkAndDelcommand(res.UserDefineID, res.UserID, res.ResBatchID, res.ReferCmd, res.ReferID)

		return nil
	}

	return fmt.Errorf("unknown cmd type[%s]", res.CmdType)
}

func (o *processManager) addCommand(userID, resBatchID string, cmd *types.CommandInfo) error {
	blog.Infof("addCommand with [%s][%s][%+v]", userID, resBatchID, cmd)

	// 执行命令时现在没有带资源信息，统一从 已分配的资源中获取，但现在没有实现 通知agent分配资源的接口，所以临时用独占的方式
	res := &o.agent.Total

	o.usedresLock.Lock()
	existedAllocated := false
	if len(o.agent.Allocated) > 0 {
		for _, v := range o.agent.Allocated {
			if v.UserID == userID && v.ResBatchID == resBatchID {
				existedAllocated = true
				v.AllocatedResource.Add(res)
				v.Commands = append(v.Commands, cmd)
				break
			}
		}
	}

	if !existedAllocated {
		o.agent.Allocated = append(o.agent.Allocated, &types.AllocatedInfo{
			AllocatedResource: *res,
			UserID:            userID,
			ResBatchID:        resBatchID,
			Commands:          []*types.CommandInfo{cmd},
		})
	}
	o.usedresLock.Unlock()

	return nil
}

func (o *processManager) delCommand(userID, resBatchID, referCmd, referID string) error {
	blog.Infof("delCommand with [%s][%s][%s][%s],len(o.agent.Allocated)=%d",
		userID, resBatchID, referCmd, referID, len(o.agent.Allocated))

	o.usedresLock.Lock()
	for i1, v := range o.agent.Allocated {
		blog.Infof("delCommand with [%s][%s][%s][%s] Allocated[%+v]", userID, resBatchID, referCmd, referID, *v)
		if v.UserID == userID && v.ResBatchID == resBatchID {
			for i2, c := range v.Commands {
				blog.Infof("command [%s][%s][%s][%s]", v.UserID, v.ResBatchID, c.Cmd, c.ID)
				if c.ID == referID && c.Cmd == referCmd {
					blog.Infof("ready delete cmd [%s][%s][%s]", resBatchID, referCmd, referID)
					v.Commands = append(v.Commands[:i2], v.Commands[i2+1:]...)
				}
			}
			if len(v.Commands) == 0 {
				blog.Infof("ready delete Allocated [%s][%s][%s]", resBatchID, referCmd, referID)
				o.agent.Allocated = append(o.agent.Allocated[:i1], o.agent.Allocated[i1+1:]...)
			}
		}
	}
	o.usedresLock.Unlock()

	return nil
}

func (o *processManager) getCommandAndPID(userID, resBatchID, referCmd, referID string) (string, string, error) {
	blog.Infof("get command and pid with [%s][%s][%s][%s]", userID, resBatchID, referCmd, referID)

	o.usedresLock.RLock()
	defer o.usedresLock.RUnlock()
	for _, v := range o.agent.Allocated {
		if v.UserID == userID && v.ResBatchID == resBatchID {
			for _, c := range v.Commands {
				if c.ID == referID && c.Cmd == referCmd {
					return c.Cmd, c.ID, nil
				}
			}
		}
	}

	return "", "", fmt.Errorf("not found command with [%s][%s][%s][%s]", userID, resBatchID, referCmd, referID)
}

func (o *processManager) checkAndDelcommand(userdefineid, userID, resBatchID, referCmd, referID string) error {
	blog.Infof("ready check and delete cache for taskid[%s]", userdefineid)

	cmd, pid, err := o.getCommandAndPID(userID, resBatchID, referCmd, referID)
	if err != nil {
		blog.Infof("failed to get taskid[%s] from cache with error[%v], delete from cache again anyway",
			userdefineid, err)
		return o.delCommand(userID, resBatchID, referCmd, referID)
	}

	if cmd == "" || pid == "" {
		blog.Infof("found empty cmd or pid with taskid[%s] in cache, delete from cache again anyway", userdefineid)
		return o.delCommand(userID, resBatchID, referCmd, referID)
	}

	retry := 0
	warnThreshold := 60
	for {
		time.Sleep(time.Duration(1) * time.Second)

		if !o.processExistedByNameAndPid(cmd, pid) {
			blog.Infof("application %s pid %s has been killed", cmd, pid)
			return o.delCommand(userID, resBatchID, referCmd, referID)
		}

		retry++
		if retry >= warnThreshold {
			blog.Warnf("application %s pid %s existed after %d checks", cmd, pid, retry)
		}
	}
}

// Run brings up process manager
func (o *processManager) Run() error {
	blog.Infof("manager start handler")
	if o.ctx != nil {
		blog.Errorf("manager has already started")
		return nil
	}

	o.ctx, o.cancel = context.WithCancel(context.Background())
	go o.start(o.ctx)

	return nil
}

//
func (o *processManager) start(pCtx context.Context) {
	blog.Infof("selfresourcehandle start")
	ctx, cancel := context.WithCancel(pCtx)
	defer cancel()
	resourceReportTick := time.NewTicker(types.AgentReportIntervalTime)
	defer resourceReportTick.Stop()

	for {
		select {
		case <-ctx.Done():
			blog.Infof("clean shut down")
			return
		case <-resourceReportTick.C:
			o.reportResource()
		}
	}
}

func (o *processManager) waitToKillOneApplication(processName string) error {
	blog.Infof("wait application %s to kill...", processName)

	if runtime.GOOS == LabelValueGOOSWindows {
		localCommon.KillApplicationByNameWindows(processName)
	} else {
		localCommon.KillApplicationByNameUnix(processName)
	}

	retry := 0
	// retryMax := 600
	for {
		time.Sleep(time.Duration(1) * time.Second)

		if !o.processExistedByPrefix(processName) {
			blog.Infof("application %s has been killed", processName)
			return nil
		}

		blog.Infof("wait application %s to kill, for %d checks", processName, retry)

		retry++
		// if retry >= retryMax {
		// 	return fmt.Errorf("failed to kill %s for %d times", processName, retry)
		// }
		if runtime.GOOS == LabelValueGOOSWindows {
			localCommon.KillApplicationByNameWindows(processName)
		} else {
			localCommon.KillApplicationByNameUnix(processName)
		}
	}
}

// check whether application(s) are running, if existed, kill
func (o *processManager) waitToKillAllApplication() error {
	blog.Infof("waitToKillAllApplication...")

	for _, agentcmd := range o.conf.AgentRemouteCmd {
		processName := strings.TrimSpace(agentcmd)
		if processName == "" {
			blog.Infof("found processName empty,check the config_json file")
			continue
		}

		if err := o.waitToKillOneApplication(processName); err != nil {
			return err
		}
	}

	return nil
}

// check whether application(s) are running, if existed, kill
func (o *processManager) releaseCmds() error {
	blog.Infof("releaseCmds...")

	for _, agentcmd := range o.conf.AgentReleaseCmds {
		cmd := exec.Command("/bin/bash", "-c", agentcmd)
		dir, _ := os.Getwd()
		cmd.Dir = dir
		cmd.Stdout = os.Stdout
		cmd.Stderr = os.Stderr

		err := cmd.Run()
		if err != nil {
			return err
		}
	}

	return nil
}

func (o *processManager) processExisted(processName string, output string) bool {
	if runtime.GOOS == LabelValueGOOSWindows {
		return o.processExistedWindows(processName, output)
	}

	return o.processExistedUnix(processName, output)
}

func (o *processManager) processExistedWindows(processName string, output string) bool {
	blog.Infof("processExisted...")

	arr := strings.Split(output, "\n")
	for _, str := range arr {
		blog.Infof("ready to deal line [%s]", str)
		str = strings.TrimSpace(str)
		if str == "" {
			continue
		}

		kv := strings.Fields(str)

		if len(kv) < 2 {
			continue
		}
		key := strings.TrimSpace(kv[0])
		if key != processName {
			continue
		}

		pid := strings.TrimSpace(kv[1])
		blog.Infof("found pid[%s] for process[%s]", pid, key)

		return true
	}

	return false
}

func (o *processManager) processExistedUnix(processName string, output string) bool {
	blog.Infof("processExistedUnix...")

	return output != ""
}

func (o *processManager) processExistedByPrefix(processPrefix string) bool {
	blog.Infof("processExistedByPrefix...")

	if runtime.GOOS == LabelValueGOOSWindows {
		return o.processExistedByPrefixWindows(processPrefix)
	}

	return o.processExistedByPrefixUnix(processPrefix)
}

func (o *processManager) processExistedByPrefixWindows(processPrefix string) bool {
	blog.Infof("processExistedByPrefixWindows...")

	likename := processPrefix + "*"
	output, err := localCommon.ListApplicationByNameWindows(likename)
	if err != nil {
		blog.Infof("failed to listApplicationByName for [%s]", likename)
		return false
	}

	arr := strings.Split(output, "\n")
	for _, str := range arr {
		blog.Infof("ready to deal line [%s]", str)
		str = strings.TrimSpace(str)
		if str == "" {
			continue
		}

		kv := strings.Fields(str)

		if len(kv) < 2 {
			continue
		}

		key := strings.TrimSpace(kv[0])
		if !strings.HasPrefix(key, processPrefix) {
			continue
		}

		pid := strings.TrimSpace(kv[1])
		blog.Infof("found pid[%s] for process[%s]", pid, key)

		return true
	}

	return false
}

func (o *processManager) processExistedByPrefixUnix(processPrefix string) bool {
	blog.Infof("processExistedByPrefixUnix for process[%s]", processPrefix)

	_, err := localCommon.ListApplicationByNameUnix(processPrefix)
	if err != nil {
		blog.Infof("failed to listApplicationByName for [%s]", processPrefix)
		return false
	}
	return false
}

func (o *processManager) processExistedByNameAndPid(processName string, processID string) bool {
	blog.Infof("processExistedByNameAndPid...")

	output := ""
	var err error
	if runtime.GOOS == LabelValueGOOSWindows {
		output, err = localCommon.ListApplicationByNameAndPidWindows(processName, processID)
		if err != nil {
			// TODO: not sure what to do here
			blog.Infof("failed to enum process by name[%s] pid[%s]", processName, processID)
			return false
		}
	} else {
		output, err = localCommon.ListApplicationByNameAndPidUnix(processName, processID)
		if err != nil {
			// TODO: not sure what to do here
			blog.Infof("failed to enum process by name[%s] pid[%s]", processName, processID)
			return false
		}
	}

	return o.processExisted(processName, output)
}

// return pid,error
func (o *processManager) startCommand(dir, cmdPath, processName string, params []string,
	cpu int, mem int64, waitpid bool) (int, error) {
	blog.Infof("startCommand for [%s %v]", processName, params)

	// check whether process has existed
	if o.processExistedByPrefix(processName) {
		err := fmt.Errorf("failed to run[%s %s] for [%s*] has existed", processName, params, processName)
		blog.Infof("%v", err)
		return 0, err
	}

	fullCmd := processName
	if cmdPath != "" {
		// 非绝对路径
		if !filepath.IsAbs(cmdPath) {
			cmdPath, _ = filepath.Abs(cmdPath)
		}
		fullCmd = path.Join(cmdPath, processName)
		exist, err := localCommon.Exist(fullCmd)
		if !exist {
			blog.Warn("failed to startCommand for[%s] not existed,err[%v]", fullCmd, err)
			return 0, err
		}
	}

	cmd := exec.Command(fullCmd, params...)
	if dir != "" {
		cmd.Dir = dir
	}

	blog.Infof("ready to start cmd[%+v]", cmd)
	err := cmd.Start()
	if err != nil {
		err = fmt.Errorf("failed to run[%s %s] for err[%v]", processName, params, err)
		blog.Infof("%v", err)
		return 0, err
	}

	if cmd.Process.Pid > 0 {
		blog.Infof("succeed to run[%s %s] ,pid[%d]", processName, params, cmd.Process.Pid)
	}

	if waitpid {
		maxcounter := 3
		index := 0
		for {
			if index >= maxcounter {
				err = fmt.Errorf("failed to get pid after run[%s %s] for [%d] try", processName, params, maxcounter)
				blog.Infof("%v", err)
				return 0, err
			}
			time.Sleep(time.Duration(1) * time.Second)
			if cmd.Process.Pid > 0 {
				blog.Infof("succeed to run[%s %s] ,pid[%d]", processName, params, cmd.Process.Pid)
				break
			}
			index++
		}

		// // to ensure the process has running
		// processID := strconv.Itoa(cmd.Process.Pid)
		// existed := o.processExistedByNameAndPid(processName, processID)
		// if existed {
		// 	return cmd.Process.Pid, nil
		// }

		// err = fmt.Errorf("not found running process for[%s %s]", processName, processID)
		// blog.Infof("%v", err)
		// return 0, err
		// do not check process name now, for it does not exist for *.bat
		return 0, err
	}
	return 0, nil
}

// return error
func (o *processManager) runCommand(dir, cmdPath, processName string, params []string) error {
	blog.Infof("runCommand for [%s %v]", processName, params)

	fullCmd := processName
	if cmdPath != "" {
		// 非绝对路径
		if !filepath.IsAbs(cmdPath) {
			cmdPath, _ = filepath.Abs(cmdPath)
		}
		fullCmd = path.Join(cmdPath, processName)
		exist, err := localCommon.Exist(fullCmd)
		if !exist {
			blog.Warn("failed to startCommand for command[%s] not existed,err[%v]", fullCmd, err)
			return err
		}
	}

	cmd := exec.Command(fullCmd, params...)
	if dir != "" {
		cmd.Dir = dir
	}
	var out bytes.Buffer
	var stderr bytes.Buffer
	cmd.Stdout = &out
	cmd.Stderr = &stderr

	blog.Infof("ready to run cmd[%v]", cmd)
	err := cmd.Run()
	if err != nil {
		blog.Errorf("failed to run[%v] for err[%v] stderr[%s]", cmd, err, stderr.String())
		return err
	}
	blog.Infof("succeed to run[%v] for err[%v]", cmd, err)
	return nil
}

func (o *processManager) getServer() ([]string, error) {
	blog.Infof("getServer...")
	servers, err := o.rd.GetServers()
	if err != nil || len(servers) == 0 {
		blog.Errorf("failed to get server by etcd for[%v]", err)
		return nil, types.ErrNotFoundServer
	}

	ns := make([]string, 0, 5)
	for _, s := range servers {
		ns = append(ns, fmt.Sprintf(types.URLReportResource, s.IP, s.ResourcePort))
	}
	return ns, nil
}

func (o *processManager) initLocalIP() error {
	blog.Infof("initLocalIp...")
	if o.conf.LocalConfig.LocalIP == "" {
		ip, err := localCommon.GetLocalIP()
		if err != nil {
			blog.Infof("failed to get local ip")
			return err
		}

		blog.Infof("succeed to get localip[%s]", ip)
		o.conf.LocalConfig.LocalIP = ip
		return nil
	}

	return nil
}

func (o *processManager) initBaseInfo() error {
	blog.Infof("initBaseInfo for city[%s] ip[%s] os[%s]...", o.conf.City, o.conf.LocalConfig.LocalIP, runtime.GOOS)
	o.agent.Base = types.AgentBase{
		IP:      o.conf.LocalConfig.LocalIP,
		Port:    int(o.conf.Port),
		Message: "",
		Cluster: o.conf.City,
		Labels: map[string]string{
			LabelKeyGOOS: runtime.GOOS,
		},
	}

	return nil
}

func (o *processManager) initTotalRes() error {
	blog.Infof("initTotalRes for city[%s] ip[%s]...", o.conf.City, o.conf.LocalConfig.LocalIP)

	totalcpu := runtime.NumCPU()
	blog.Infof("initTotalRes totalcpu[%d]...", totalcpu)
	prescribedCPUNum := o.conf.PrescribedCPUNum
	if prescribedCPUNum > 0 && prescribedCPUNum < totalcpu {
		totalcpu = prescribedCPUNum
	}

	totalmemorykb, err := localCommon.GetTotalMemory()
	if err != nil {
		blog.Infof("failed to get total memory for err[%v]", err)
		return err
	}

	o.agent.Total = types.Resource{
		CPU:  (float64)(totalcpu),
		Mem:  (float64)(totalmemorykb / 1024), // to MB
		Disk: 0.0,
	}

	blog.Infof("agent[%+v]", o.agent)

	return nil
}

func (o *processManager) reportResource() error {
	blog.Infof("ReportResource for city[%s] ip[%s]...", o.conf.City, o.conf.LocalConfig.LocalIP)

	o.updateTotalResCPU()

	o.usedresLock.RLock()
	var reportobj = types.ReportAgentResource{
		AgentInfo: *o.agent,
	}
	o.usedresLock.RUnlock()

	// get report url
	uriList, err := o.getServer()
	if err != nil {
		blog.Errorf("failed to get server for err[%v]", err)
		return err
	}

	var data []byte
	_ = codec.EncJSON(reportobj, &data)

	if o.serverURL == "" {
		for _, uri := range uriList {
			blog.Debugf("report resource: report to %s, data: %s", uri, (string)(data))
			if _, _, err = o.post(uri, o.getHeader(""), data); err != nil {
				blog.Warnf("failed to report resource to server %s with data %s, error:%v", uri, (string)(data), err)
				continue
			}

			blog.Infof("report resource: %s json: [%s]", uri, (string)(data))
			o.serverURL = uri
			return nil
		}

		blog.Errorf("report resource: no master server available")
		return fmt.Errorf("no master server available")
	}

	if _, _, err = o.post(o.serverURL, o.getHeader(""), data); err != nil {
		o.serverURL = ""
		blog.Errorf("report resource: report failed: %v", err)
		return err
	}

	blog.Infof("report resource: %s json: [%s]", o.serverURL, (string)(data))
	return nil
}

func (o *processManager) updateTotalResCPU() error {
	if !o.conf.UpdateCPURealtime {
		blog.Infof("do not need update cpu resource by cpu usage")
		return nil
	}

	usage, err := localCommon.GetTotalCPUUsage()
	if err == nil {
		blog.Infof("cpu usage[%f]", usage)
		var totalcpu = float64(runtime.NumCPU())
		o.agent.Total.CPU = (float64)(math.Floor(totalcpu * (100.0 - usage) / 100.0))

		prescribedCPUNum := float64(o.conf.PrescribedCPUNum)
		if prescribedCPUNum > 0 && prescribedCPUNum < o.agent.Total.CPU {
			o.agent.Total.CPU = prescribedCPUNum
		}
	}

	return err
}

func (o *processManager) query(uri string, header http.Header) (resp *commonHttp.APIResponse, raw []byte, err error) {
	return o.request("GET", uri, header, nil)
}

func (o *processManager) post(uri string, header http.Header, data []byte) (
	resp *commonHttp.APIResponse, raw []byte, err error) {
	return o.request("POST", uri, header, data)
}

func (o *processManager) delete(uri string, header http.Header, data []byte) (
	resp *commonHttp.APIResponse, raw []byte, err error) {
	return o.request("DELETE", uri, header, data)
}

func (o *processManager) request(method, uri string, header http.Header, data []byte) (
	resp *commonHttp.APIResponse, raw []byte, err error) {
	var r *httpclient.HttpResponse

	switch strings.ToUpper(method) {
	case "GET":
		if r, err = o.client.Get(uri, header, data); err != nil {
			return
		}
	case "POST":
		if r, err = o.client.Post(uri, header, data); err != nil {
			return
		}
	case "PUT":
		if r, err = o.client.Put(uri, header, data); err != nil {
			return
		}
	case "DELETE":
		if r, err = o.client.Delete(uri, header, data); err != nil {
			return
		}
	}

	if r.StatusCode != http.StatusOK {
		err = fmt.Errorf("failed to request, http(%d)%s: %s", r.StatusCode, r.Status, uri)
		return
	}

	if err = codec.DecJSON(r.Reply, &resp); err != nil {
		err = fmt.Errorf("%v: %s", err, string(r.Reply))
		return
	}

	if resp.Code != common.RestSuccess {
		err = fmt.Errorf("failed to request, resp(%d)%s: %s", resp.Code, resp.Message, uri)
		return
	}

	if err = codec.EncJSON(resp.Data, &raw); err != nil {
		return
	}
	return
}

func (o *processManager) fetch(uri string) (*types.StatsInfo, error) {
	o.client.SetTimeOut(10 * time.Second)
	_, err := o.client.GET(uri, nil, nil)
	if err != nil {
		blog.Errorf("failed to fetch stats %s: %v", uri, err)
		return nil, err
	}

	stats := new(types.StatsInfo)
	return stats, nil
}

func (o *processManager) getHeader(clusterID string) http.Header {
	header := http.Header{}
	header.Set("BCS-ClusterID", clusterID)
	return header
}
