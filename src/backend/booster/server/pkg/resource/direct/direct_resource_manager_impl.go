/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package direct

import (
	"errors"
	"fmt"
	"math/rand"
	"net/http"
	"strings"
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	commonHttp "github.com/Tencent/bk-ci/src/booster/common/http"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpclient"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpserver"
	"github.com/Tencent/bk-ci/src/booster/common/metric/controllers"
	"github.com/Tencent/bk-ci/src/booster/server/config"
	selfMetric "github.com/Tencent/bk-ci/src/booster/server/pkg/metric"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/types"
)

// const vars
const (
	AgentResourceCheckTime    = 20 * time.Second
	AgentReportInterval       = 15 // secs
	AgentReportTimeoutCounter = 3
	AgentTaskTimeoutSecs      = 30 // secs
)

// define const strings
const (
	LabelKeyGOOS = "os"

	LabelValueGOOSWindows = "windows"
	LabelValueGOOSDarwin  = "darwin"
)

// define urls
const (
	URLExecuteCommand = "http://%s:%d/api/v1/build/executecommand"
)

// vars for error define
var (
	ErrInitHTTPHandle   = fmt.Errorf("failled to init http handle")
	ErrResourceReported = fmt.Errorf("resource reported is not valid")

	LetterRunes = []rune("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
)

// ReportAgentResource : struct of report resource
type ReportAgentResource struct {
	AgentInfo
}

// NotifyAgentData : struct to notify agent
type NotifyAgentData struct {
	// 资源使用者的ID
	UserID     string `json:"user_id"`
	ResBatchID string `json:"res_batch_id"`
	// 用户自定义ID，具体含义由资源使用者自己解释
	UserDefineID string `json:"user_define_id"`
	// 命令的工作目录
	Dir string `json:"dir"`
	// 可执行文件路径，如果是相对路径，则相对于agent运行目录
	Path       string            `json:"path"`
	Cmd        string            `json:"cmd"`
	Parameters []string          `json:"parameters"`
	Env        map[string]string `json:"env"`
	Additional map[string]string `json:"additional"`
	CmdType    string            `json:"cmd_type"`
	// 保存关联的命令字和id，比如执行释放命令时，需要带上启动命令和进程id，便于agent侧执行相应的释放
	ReferCmd string `json:"refer_cmd"`
	ReferID  string `json:"refer_id"`
}

// -------------worker resource-------------------------
type oneagentResource struct {
	Agent AgentInfo
	// last report time, time.Now().Unix()
	Update int64
}

type allocatedResource struct {
	resource      *AgentResourceExternal
	allocatedTime int64
}

type userAllocated struct {
	// 记录已经预分配的资源
	allocated     map[string][]*allocatedResource
	allocatedLock sync.RWMutex
	userID        string
}

type userBatchresCallback struct {
	// 记录每批资源的通知回调函数
	batchresCallbacks     map[string]CallBack4Command
	batchresCallbacksLock sync.RWMutex
	userID                string
}

type directResourceManager struct {
	conf   *config.DirectResourceConfig
	client *httpclient.HTTPClient
	server *httpserver.HTTPServer

	roleEvent types.RoleChangeEvent
	isMaster  bool

	resource     map[string]*oneagentResource
	resourceLock sync.RWMutex

	// 防止并发注册用户
	registerLock sync.RWMutex

	// 记录已经预分配的资源
	userAllocateds []*userAllocated

	// 记录每批资源的通知回调函数
	userBatchresCallbacks []*userBatchresCallback

	// 记录用户注册的释放资源的命令
	releaseCmds map[string]*Command

	mysql MySQL
}

func (a *AgentInfo) getGOOS() string {
	// 如果没有上报os信息，则默认为windows
	if a.Base.Labels == nil {
		return LabelValueGOOSWindows
	}

	v, ok := a.Base.Labels[LabelKeyGOOS]
	if !ok {
		return LabelValueGOOSWindows
	}

	return v
}

// NewResourceManager : new ResourceManager
func NewResourceManager(conf *config.DirectResourceConfig, roleEvent types.RoleChangeEvent) (ResourceManager, error) {
	mysql, err := NewMySQL(MySQLConf{
		MySQLStorage:     conf.MySQLStorage,
		MySQLDatabase:    conf.MySQLDatabase,
		MySQLUser:        conf.MySQLUser,
		MySQLPwd:         conf.MySQLPwd,
		MysqlTableOption: conf.MysqlTableOption,
	})
	if err != nil {
		return nil, err
	}

	return &directResourceManager{
		conf:                  conf,
		mysql:                 mysql,
		client:                httpclient.NewHTTPClient(),
		server:                nil,
		roleEvent:             roleEvent,
		isMaster:              false,
		resource:              map[string]*oneagentResource{},
		userAllocateds:        []*userAllocated{},
		userBatchresCallbacks: []*userBatchresCallback{},
		releaseCmds:           map[string]*Command{},
	}, nil
}

// RegisterUser register an UserID and return a new handler
func (d *directResourceManager) RegisterUser(userID string, releaseCmd *Command) (HandleWithUser, error) {
	blog.Infof("drm: RegisterUser with userID[%s]", userID)

	d.registerLock.Lock()
	defer d.registerLock.Unlock()

	existed := false
	for _, v := range d.userAllocateds {
		if v.userID == userID {
			existed = true
			break
		}
	}
	if !existed {
		d.userAllocateds = append(d.userAllocateds, &userAllocated{
			allocated: map[string][]*allocatedResource{},
			userID:    userID,
		})
		d.userBatchresCallbacks = append(d.userBatchresCallbacks, &userBatchresCallback{
			batchresCallbacks: map[string]CallBack4Command{},
			userID:            userID,
		})
	}
	if releaseCmd != nil {
		d.releaseCmds[userID] = releaseCmd
	}
	return &handleWithUser{
		mgr:    d,
		userID: userID,
	}, nil
}

func (d *directResourceManager) getUserAllocated(userID string) *userAllocated {
	blog.Infof("drm: getUserAllocated with userID[%s]", userID)

	for _, v := range d.userAllocateds {
		if v.userID != userID {
			continue
		}
		return v
	}
	return nil
}

func (d *directResourceManager) getUserBatchresCallback(userID string) *userBatchresCallback {
	blog.Infof("drm: getUserBatchresCallback with userID[%s]", userID)

	for _, v := range d.userBatchresCallbacks {
		if v.userID != userID {
			continue
		}
		return v
	}
	return nil
}

func (d *directResourceManager) getReleaseCmd(userID string) *Command {
	blog.Infof("drm: getReleaseCmd with userID[%s]", userID)

	if v, ok := d.releaseCmds[userID]; ok && v != nil {
		return v
	}
	return nil
}

// getFreeResource : 有调用者的回调函数选择资源，如果选择成功，则设置占用标记和保存回调函数
func (d *directResourceManager) getFreeResource(
	userID string, resBatchID string, condition interface{},
	callbackSelector CallBackSelector, callBack4Command CallBack4Command) ([]*AgentResourceExternal, error) {
	blog.Infof("drm: getFreeResource with userID[%s] resBatchID[%s] condition[%+v]",
		userID, resBatchID, condition)

	d.resourceLock.Lock()
	defer d.resourceLock.Unlock()

	allfreeres, err := d.getAllFreeResource(userID)
	if err != nil {
		blog.Errorf("drm: failed to getAllFreeResource with userID(%s) for [%v]", userID, err)
		return nil, err
	}

	res, err := callbackSelector(allfreeres, condition)
	if err != nil {
		blog.Errorf("drm: failed to callbackSelector with userID(%s) for [%v]", userID, err)
		return nil, err
	}

	if res == nil {
		err := fmt.Errorf("failed to get enought resource for user[%s]", userID)
		return nil, err
	}

	if len(res) > 0 {
		d.setResourceAllocated(userID, resBatchID, res)
		// 通知agent记录该分配的资源信息
		d.setResourceCallbacks(userID, resBatchID, callBack4Command)

		d.mysql.PutAllocatedResource(d.getAllocatedResourceRecord(userID, resBatchID, false, res))

		// 更新相应的free列表，减去已分配的资源
		d.decAllocatedResource(userID, res)
	}

	blog.Infof("drm: getFreeResource with userID[%s] resBatchID[%s] condition[%+v] res[%+v]",
		userID, resBatchID, condition, res)
	return res, nil
}

// releaseResource : 通知agent释放worker；清除占用记录；清除回调记录
func (d *directResourceManager) releaseResource(userID string, resBatchID string) error {
	blog.Infof("drm: releaseResource with userID[%s] resBatchID[%s]", userID, resBatchID)

	// save to db
	rec, err := d.mysql.GetAllocatedResource(userID, resBatchID)
	if err != nil {
		blog.Warnf("drm: failed get get record with [%s %s], error:%v", userID, resBatchID, err)
		d.mysql.PutAllocatedResource(d.getAllocatedResourceRecord(userID, resBatchID, true, nil))
	} else {
		rec.Released = 1
		rec.ReleasedTime = time.Now().Unix()
		d.mysql.PutAllocatedResource(rec)
	}

	// 通知agent释放worker
	d.notifyAllAgentRelease(userID, resBatchID)

	v := d.getUserAllocated(userID)
	if v != nil {
		v.allocatedLock.Lock()
		// 清除占用记录
		if _, ok := v.allocated[resBatchID]; ok {
			delete(v.allocated, resBatchID)
		}
		v.allocatedLock.Unlock()
	}

	v2 := d.getUserBatchresCallback(userID)
	if v2 != nil {
		v2.batchresCallbacksLock.Lock()
		// 清除资源回调记录
		if _, ok := v2.batchresCallbacks[resBatchID]; ok {
			delete(v2.batchresCallbacks, resBatchID)
		}
		v2.batchresCallbacksLock.Unlock()
	}

	return nil
}

// listResource : 返回 资源id关联的资源列表
func (d *directResourceManager) listResource(userID string, resBatchID string) ([]*AgentResourceExternal, error) {
	blog.Infof("drm: listResource with userID[%s] resBatchID[%s]", userID, resBatchID)

	var ress []*AgentResourceExternal
	var resips []string
	v := d.getUserAllocated(userID)
	if v != nil {
		v.allocatedLock.RLock()
		v1, ok := v.allocated[resBatchID]
		v.allocatedLock.RUnlock()
		if ok {
			for _, r := range v1 {
				ress = append(ress, r.resource)
				resips = append(resips, r.resource.Base.IP)
			}
		}
	}

	blog.Infof("drm: listResource with userID[%s] resBatchID[%s],ress[%s]",
		userID, resBatchID, strings.Join(resips, " "))
	return ress, nil
}

// setResourceAllocated : 将资源添加到已经占用的列表里
func (d *directResourceManager) setResourceAllocated(
	userID string,
	resBatchID string,
	res []*AgentResourceExternal) error {
	blog.Infof("drm: setResourceAllocated with userID[%s] resBatchID[%s] res[%+v]", userID, resBatchID, res)

	allocatedRess := []*allocatedResource{}
	for _, r := range res {
		allocatedRess = append(allocatedRess, &allocatedResource{
			resource:      r,
			allocatedTime: time.Now().Unix(),
		})
	}

	v := d.getUserAllocated(userID)
	if v != nil {
		v.allocatedLock.Lock()
		resarray, ok := v.allocated[resBatchID]
		if !ok {
			v.allocated[resBatchID] = allocatedRess
		} else {
			blog.Errorf("drm: setResourceAllocated with userID[%s], found same resBatchID[%s]!!!",
				userID, resBatchID)
			v.allocated[resBatchID] = append(resarray, allocatedRess...)
		}
		v.allocatedLock.Unlock()
	}

	return nil
}

// setResourceCallbacks : 记录已分配资源的回调函数
func (d *directResourceManager) setResourceCallbacks(
	userID string, resBatchID string, callBack4Command CallBack4Command) error {
	blog.Infof("drm: setResourceCallbacks with userID[%s] resBatchID[%s]", userID, resBatchID)

	if callBack4Command == nil {
		blog.Infof("drm: setResourceCallbacks with userID[%s] resBatchID[%s] callBack4Command is nil",
			userID, resBatchID)
		return nil
	}

	v := d.getUserBatchresCallback(userID)
	if v != nil {
		v.batchresCallbacksLock.Lock()
		_, ok := v.batchresCallbacks[resBatchID]
		if !ok {
			v.batchresCallbacks[resBatchID] = callBack4Command
			v.batchresCallbacksLock.Unlock()
			return nil
		}

		blog.Errorf("drm: setResourceCallbacks with userID[%s], "+
			"found same resBatchID[%s],will reset the callbask func!!!", userID, resBatchID)
		v.batchresCallbacks[resBatchID] = callBack4Command
		v.batchresCallbacksLock.Unlock()
	}

	return nil
}

func (d *directResourceManager) executeCommand(userID string, ip string, resBatchID string, cmd *Command) error {
	blog.Infof("drm: executeCommand with userID[%s] ip[%s] resbatchid[%s] cmd[%+v]", userID, ip, resBatchID, cmd)

	port, err := d.getAgentPort(ip, resBatchID)
	if err != nil {
		return err
	}
	if port <= 0 {
		return fmt.Errorf("failed to get agent port with [%s][%s]", ip, resBatchID)
	}
	uri := fmt.Sprintf(URLExecuteCommand, ip, port)
	jsonData, err := d.getCommandJSON(userID, resBatchID, cmd)
	if err != nil {
		blog.Errorf("drm: executeCommand[%+v] get json failed: %v", cmd, err)
		return err
	}

	blog.Infof("drm: executeCommand: try to request %s json: [%s]", uri, jsonData)
	if _, _, err = d.post(uri, nil, []byte(jsonData)); err != nil {
		blog.Errorf("drm: executeCommand[%+v] failed: %v", cmd, err)
		return err
	}
	blog.Infof("drm: executeCommand: success to request %s json: [%s]", uri, jsonData)
	return nil
}

// get agent port
func (d *directResourceManager) getAgentPort(ip string, resBatchID string) (int, error) {
	blog.Infof("drm: getAgentPort with ip[%s] resbatchid[%s] ", ip, resBatchID)

	port := 0
	d.resourceLock.RLock()
	for _, v := range d.resource {
		if v.Agent.Base.IP == ip {
			port = v.Agent.Base.Port
			break
		}
	}
	d.resourceLock.RUnlock()

	return port, nil
}

func (d *directResourceManager) getCommandJSON(userID string, resBatchID string, command *Command) (string, error) {
	obj := NotifyAgentData{
		UserID:       userID,
		ResBatchID:   resBatchID,
		UserDefineID: command.UserDefineID,
		Dir:          command.Dir,
		Path:         command.Path,
		Cmd:          command.Cmd,
		Parameters:   command.Parameters,
		Env:          command.Env,
		Additional:   command.Additional,
		CmdType:      string(command.CmdType),
		ReferCmd:     command.ReferCmd,
		ReferID:      command.ReferID,
	}

	var data []byte
	_ = codec.EncJSON(obj, &data)

	return (string)(data), nil
}

// listWorkers : 返回资源id关联的worker列表
func (d *directResourceManager) listCommands(userID string, resBatchID string) ([]*CommandResultInfo, error) {
	blog.Infof("drm: listWorkers with userID[%s] resBatchID[%s]", userID, resBatchID)

	var ress []*CommandResultInfo
	d.resourceLock.RLock()
	for _, v := range d.resource {
		for _, r := range v.Agent.Allocated {
			if r.UserID == userID && r.ResBatchID == resBatchID {
				for _, c := range r.Commands {
					ress = append(ress, &CommandResultInfo{
						IP:           v.Agent.Base.IP,
						ID:           c.ID,
						Cmd:          c.Cmd,
						Status:       c.Status,
						UserDefineID: c.UserDefineID,
					})
				}
			}
		}
	}
	d.resourceLock.RUnlock()

	return ress, nil
}

func (d *directResourceManager) onResourceReport(resource *ReportAgentResource) error {
	blog.Infof("drm: onResourceReport with cluster[%s] ip[%s]...", resource.Base.Cluster, resource.Base.IP)
	d.resourceLock.Lock()

	if !d.isMaster {
		blog.Errorf("drm: not master now while received cluster[%s] ip[%s] agent report",
			resource.Base.Cluster, resource.Base.IP)
		d.resourceLock.Unlock()
		return errors.New("drm: not master now,do nothing")
	}

	// 更新空闲资源
	adjustedagentres, err := d.getAndUpdate(resource)
	if err != nil {
		blog.Infof("drm: failed to update report resource to oneagentResource")
		d.resourceLock.Unlock()
		return ErrResourceReported
	}

	// record the metric data
	go recordResource(adjustedagentres)

	// // 更新到map中
	d.resource[resource.Base.IP] = adjustedagentres
	d.resourceLock.Unlock()

	// 记录到数据库中
	go d.mysql.PutAgentResource(d.getAgentResourceRecord(adjustedagentres))

	// 将agent侧占用的资源信息同步给 taskResourceHandle ，以便 taskResourceHandle 刷新任务资源状态
	if len(resource.Allocated) > 0 {
		blog.Infof("drm: found UsedResource, ready to trigger callback")
		for _, v := range resource.Allocated {
			resBatchID := v.ResBatchID
			userID := v.UserID
			v1 := d.getUserBatchresCallback(userID)
			if v1 != nil {
				v1.batchresCallbacksLock.RLock()
				callback, ok := v1.batchresCallbacks[resBatchID]
				v1.batchresCallbacksLock.RUnlock()
				if ok {
					results := []*CommandResultInfo{}
					for _, c := range v.Commands {
						results = append(results, &CommandResultInfo{
							IP:           resource.Base.IP,
							ID:           c.ID,
							Cmd:          c.Cmd,
							Status:       c.Status,
							UserDefineID: c.UserDefineID,
						})
					}
					callback(results)
				}
			}

			// 如果该资源不在当前使用列表内，并且已经标记为释放，则需要通知agent释放
			if !d.resInAllocated(userID, resBatchID) {
				rec, err := d.mysql.GetAllocatedResource(userID, resBatchID)
				if err != nil {
					blog.Warnf("drm: failed get get record with [%s %s], error:%v", userID, resBatchID, err)
					continue
				}

				if rec != nil {
					blog.Debugf("drm: succeed get record with [%s %s],erc:[%+v]", userID, resBatchID, rec)
					if rec.Released == 1 {
						blog.Warnf("drm: found released rec with [%s %s],erc:[%+v], release it again!",
							userID, resBatchID, rec)
						for _, c := range v.Commands {
							d.notifyAgentRelease(userID, resBatchID, resource.Base.IP, c.Cmd, c.ID, resource.getGOOS())
						}
					}
				}
			}
		}
	}

	return nil
}

func (d *directResourceManager) resInAllocated(userID, resBatchID string) bool {
	flag := false
	v := d.getUserAllocated(userID)
	if v != nil {
		v.allocatedLock.RLock()
		_, ok := v.allocated[resBatchID]
		v.allocatedLock.RUnlock()
		if ok {
			flag = true
		}
	}

	return flag
}

func (d *directResourceManager) notifyAllAgentRelease(userID, resBatchID string) error {
	blog.Infof("drm: notifyAllAgentRelease for [%s][%s]", userID, resBatchID)

	d.resourceLock.RLock()
	for ip, agent := range d.resource {
		blog.Infof("drm: check for ip [%s]", ip)
		if len(agent.Agent.Allocated) > 0 {
			for _, v := range agent.Agent.Allocated {
				if v.UserID == userID && v.ResBatchID == resBatchID {
					for _, c := range v.Commands {
						d.notifyAgentRelease(userID, resBatchID, ip, c.Cmd, c.ID, agent.Agent.getGOOS())
					}
				}
			}
		}
	}
	d.resourceLock.RUnlock()

	return nil
}

func (d *directResourceManager) notifyAgentRelease(userID, resBatchID, ip, cmd, pid, goOS string) error {
	blog.Infof("drm: notifyAgentRelease for userID[%s] resBatchID[%s] ip[%s] cmd[%s] pid[%s]",
		userID, resBatchID, ip, cmd, pid)

	releaseCmd := d.getReleaseCmd(userID)
	if releaseCmd != nil {
		return d.notifyAgentReleaseByCmd(userID, resBatchID, ip, cmd, pid, releaseCmd)
	}

	if goOS == LabelValueGOOSWindows {
		return d.notifyAgentReleaseWindows(userID, resBatchID, ip, cmd, pid)
	} else if goOS == LabelValueGOOSDarwin {
		return d.notifyAgentReleaseUnix(userID, resBatchID, ip, cmd, pid)
	} else {
		return fmt.Errorf("unknow goos[%s] when notifyAgentRelease", goOS)
	}
}

func (d *directResourceManager) notifyAgentReleaseByCmd(
	userID, resBatchID, ip, cmd, pid string, command *Command) error {
	blog.Infof("drm: notifyAgentReleaseByCmd for userID[%s] resBatchID[%s] ip[%s]", userID, resBatchID, ip)

	tempcommand := *command
	tempcommand.ReferCmd = cmd
	tempcommand.ReferID = pid

	go d.executeCommand(userID, ip, resBatchID, &tempcommand)
	return nil
}

func (d *directResourceManager) notifyAgentReleaseWindows(userID, resBatchID, ip, cmd, pid string) error {
	blog.Infof("drm: notifyAgentReleaseWindows for userID[%s] resBatchID[%s] ip[%s] cmd[%s] pid[%s]",
		userID, resBatchID, ip, cmd, pid)

	cmdparameter := []string{"/f", "/t"}
	cmdparameter = append(cmdparameter, "/fi")
	condition1 := fmt.Sprintf("imagename eq %s", cmd)
	cmdparameter = append(cmdparameter, fmt.Sprintf("%s", condition1))

	cmdparameter = append(cmdparameter, "/fi")
	condition2 := fmt.Sprintf("pid eq %s", pid)
	cmdparameter = append(cmdparameter, fmt.Sprintf("%s", condition2))

	command := &Command{
		Cmd:        "taskkill",
		Parameters: cmdparameter,
		CmdType:    CmdRelease,
		ReferCmd:   cmd,
		ReferID:    pid,
	}
	go d.executeCommand(userID, ip, resBatchID, command)

	return nil
}

func (d *directResourceManager) notifyAgentReleaseUnix(userID, resBatchID, ip, cmd, pid string) error {
	blog.Infof("drm: notifyAgentReleaseUnix for userID[%s] resBatchID[%s] ip[%s] cmd[%s] pid[%s]",
		userID, resBatchID, ip, cmd, pid)

	cmdparameter := []string{"-c"}
	condition1 := fmt.Sprintf("kill -9 %s", pid)
	cmdparameter = append(cmdparameter, fmt.Sprintf("%s", condition1))

	command := &Command{
		Cmd:        "/bin/bash",
		Parameters: cmdparameter,
		CmdType:    CmdRelease,
		ReferCmd:   cmd,
		ReferID:    pid,
	}
	go d.executeCommand(userID, ip, resBatchID, command)

	return nil
}

func (d *directResourceManager) getAgentResourceRecord(r *oneagentResource) *AgentResource {
	return &AgentResource{
		Cluster:     r.Agent.Base.Cluster,
		IP:          r.Agent.Base.IP,
		TotalCPU:    (float32)(r.Agent.Total.CPU),
		TotalMemory: (float32)(r.Agent.Total.Mem),
		TotalDisk:   (float32)(r.Agent.Total.Disk),
		FreeCPU:     (float32)(r.Agent.Free.CPU),
		FreeMemory:  (float32)(r.Agent.Free.Mem),
		FreeDisk:    (float32)(r.Agent.Free.Disk),
	}
}

func (d *directResourceManager) getAllocatedResourceRecord(userID string,
	resourceBatchID string,
	released bool,
	agents []*AgentResourceExternal) *AllocatedResource {

	nowsecs := time.Now().Unix()
	AllocatedAgent := ""
	if agents != nil {
		for _, v := range agents {
			AllocatedAgent += fmt.Sprintf("%s:%d|", v.Base.IP, v.Base.Port)
		}
	}

	if !released {
		return &AllocatedResource{
			UserID:          userID,
			ResourceBatchID: resourceBatchID,
			Released:        0,
			AllocatedTime:   nowsecs,
			ReleasedTime:    0,
			AllocatedAgent:  AllocatedAgent,
			Message:         "",
		}
	}

	return &AllocatedResource{
		UserID:          userID,
		ResourceBatchID: resourceBatchID,
		Released:        1,
		AllocatedTime:   0,
		ReleasedTime:    nowsecs,
		AllocatedAgent:  AllocatedAgent,
		Message:         "",
	}
}

// getAllFreeResource : 获取所有的空闲资源
func (d *directResourceManager) getAllFreeResource(userID string) ([]*AgentResourceExternal, error) {
	//blog.Infof("getAllFreeResource with userID[%s]", userID)

	ress := []*AgentResourceExternal{}
	for _, v := range d.resource {
		// 需要确认下 free 里面的字段是否完整，如果不完整，需要补齐
		externalagent := v.Agent.FreeToExternal()
		if externalagent.Resource.CPU > 0 {
			ress = append(ress, externalagent)
		}
	}

	return ress, nil
}

// decAllocatedResource : 从空闲资源中减去已经分配的资源
func (d *directResourceManager) decAllocatedResource(userID string, allocated []*AgentResourceExternal) error {
	blog.Infof("drm: decAllocatedResource with userID[%s]", userID)

	for _, allocatedres := range allocated {
		allocatedip := allocatedres.Base.IP
		for _, v := range d.resource {
			freeip := v.Agent.Base.IP
			if allocatedip == freeip {
				if d.conf.Agent4OneTask {
					v.Agent.Free.Dec2(v.Agent.Free.CPU, v.Agent.Free.Mem, v.Agent.Free.Disk)
				} else {
					v.Agent.Free.Dec(&allocatedres.Resource)
				}
			}
		}
	}

	return nil
}

// update agent resource
// 1. update total resource
// 2. update used resource
// 3. update free resource, to dec resource in tasks
func (d *directResourceManager) getAndUpdate(resource *ReportAgentResource) (*oneagentResource, error) {
	var oneagentres = oneagentResource{
		Agent:  resource.AgentInfo,
		Update: time.Now().Unix(),
	}

	oneagentres.Agent.Free = oneagentres.Agent.Total

	// 减去agent侧已经占用的资源
	var oktaskid = make(map[string]string, 100)
	for _, usedres := range resource.Allocated {
		oneagentres.Agent.Free.Dec(&usedres.AllocatedResource)
		blog.Infof("drm: after dec used, free[%v]", oneagentres.Agent.Free)
		oktaskid[usedres.ResBatchID] = usedres.UserID
		blog.Infof("drm: add UserID[%s] for ResBatchID[%s]", usedres.UserID, usedres.ResBatchID)
	}

	// 减去标记为占用的资源（不在agent占用列表内）
	for _, v := range d.userAllocateds {
		v.allocatedLock.RLock()
		var agentip = resource.Base.IP
		for resBatchID, v1 := range v.allocated {
			for _, agent := range v1 {
				if agent.resource.Base.IP == agentip {
					blog.Debugf("drm: deal for res[%v]", agentip)
					if _, ok := oktaskid[resBatchID]; !ok {
						blog.Infof("drm: not in reported list for resBatchID[%s],need dec", resBatchID)
						// 如果资源使用策略是agent只运行一个worker，则空闲资源直接置为0
						if d.conf.Agent4OneTask {
							_ = oneagentres.Agent.Free.Dec2(
								oneagentres.Agent.Free.CPU, oneagentres.Agent.Free.Mem, oneagentres.Agent.Free.Disk)
							blog.Infof("drm: after dec allocated, free[%v]", oneagentres.Agent.Free)
						} else {
							_ = oneagentres.Agent.Free.Dec2(
								agent.resource.Resource.CPU, agent.resource.Resource.Mem, 0)
							blog.Infof("drm: after dec allocated, free[%v]", oneagentres.Agent.Free)
						}
					}
				}
			}
		}
		v.allocatedLock.RUnlock()
	}

	return &oneagentres, nil
}

// Run brings up direct resource manager
func (d *directResourceManager) Run() error {
	blog.Infof("drm: Run...")

	err := d.startHTTPServer()
	if err != nil {
		blog.Errorf("drm: failed to Run for [%v]", err)
		return err
	}

	go d.tick()

	for {
		blog.Infof("drm: ready to receive role change event")
		select {
		case e := <-d.roleEvent:
			blog.Infof("drm: directResourceManager: receive new role change event: %s", e)
			switch e {
			case types.ServerMaster:
				d.isMaster = true
			case types.ServerSlave, types.ServerUnknown:
				d.isMaster = false
			default:
				blog.Warnf("drm:  unknown role, will not change manager state: %s", e)
			}
		}
	}
}

func (d *directResourceManager) tick() {
	blog.Infof("drm: tick...")

	resourceCheckTick := time.NewTicker(AgentResourceCheckTime)
	defer resourceCheckTick.Stop()

	for {
		select {
		case <-resourceCheckTick.C:
			d.resourceCheck()
		}
	}
}

// 检查资源（如果资源超过3个周期未上报/更新，则去掉该资源；检查资源对应的任务是否有效，如果已经无效，则通知资源释放）
func (d *directResourceManager) resourceCheck() {
	blog.Infof("drm: resourceCheck...")

	d.resourceLock.Lock()
	allagent := []string{}

	for ip, agent := range d.resource {
		blog.Infof("drm: check for ip [%s]", ip)
		allagent = append(allagent, ip)
		// over period not update, delete
		if time.Now().Unix()-agent.Update > AgentReportInterval*AgentReportTimeoutCounter {
			blog.Infof("drm: found agent[%s:%d] timeout,delte it", agent.Agent.Base.IP, agent.Agent.Base.Port)
			delete(d.resource, ip)
			continue
		}
	}
	d.resourceLock.Unlock()

	// TODO : 对于已经分配的资源，需要加个最大使用时长，避免无限期被占用
	// 先不处理，不确定具体业务会占用资源多长时间

	blog.Infof("drm: all_agent_ips=%s", strings.Join(allagent, " "))
}

func (d *directResourceManager) query(
	uri string, header http.Header) (resp *commonHttp.APIResponse, raw []byte, err error) {
	return d.request("GET", uri, header, nil)
}

func (d *directResourceManager) post(uri string, header http.Header, data []byte) (
	resp *commonHttp.APIResponse, raw []byte, err error) {
	return d.request("POST", uri, header, data)
}

func (d *directResourceManager) delete(uri string, header http.Header, data []byte) (
	resp *commonHttp.APIResponse, raw []byte, err error) {
	return d.request("DELETE", uri, header, data)
}

func (d *directResourceManager) request(method, uri string, header http.Header, data []byte) (
	resp *commonHttp.APIResponse, raw []byte, err error) {
	var r *httpclient.HttpResponse

	switch strings.ToUpper(method) {
	case "GET":
		if r, err = d.client.Get(uri, header, data); err != nil {
			return
		}
	case "POST":
		if r, err = d.client.Post(uri, header, data); err != nil {
			return
		}
	case "PUT":
		if r, err = d.client.Put(uri, header, data); err != nil {
			return
		}
	case "DELETE":
		if r, err = d.client.Delete(uri, header, data); err != nil {
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

// +++++++++++++++++++++++http server+++++++++++++++++++++++++++
func (d *directResourceManager) startHTTPServer() error {
	blog.Infof("drm: std.tHTTPServer...")

	// 需要指定另外的配置项，针对agent上报的
	// init Http server
	d.server = httpserver.NewHTTPServer(d.conf.ListenPort, d.conf.ListenIP, "")
	if d.conf.ServerCert.IsSSL {
		d.server.SetSSL(
			d.conf.ServerCert.CAFile, d.conf.ServerCert.CertFile, d.conf.ServerCert.KeyFile, d.conf.ServerCert.CertPwd)
	}

	var err error
	handle, err := NewResourceHTTPHandle(d.conf, d)
	if handle == nil || err != nil {
		return ErrInitHTTPHandle
	}

	d.server.RegisterWebServer(PathV1, nil, handle.GetActions())
	go d.server.ListenAndServe()

	return nil
}

// -----------------------http server----------------------------

type handleWithUser struct {
	mgr    *directResourceManager
	userID string
}

// NewResourceHandleWithUser : new handleWithUser
func NewResourceHandleWithUser(mgr *directResourceManager, userID string) (HandleWithUser, error) {
	return &handleWithUser{
		mgr: mgr,
	}, nil
}

// GetFreeResource 获取空闲的资源
func (h *handleWithUser) GetFreeResource(
	resBatchID string,
	condition interface{},
	callbackSelector CallBackSelector,
	callBack4Command CallBack4Command) ([]*AgentResourceExternal, error) {
	if h.mgr != nil {
		return h.mgr.getFreeResource(h.userID, resBatchID, condition, callbackSelector, callBack4Command)
	}

	return nil, nil
}

// ReleaseResource 释放batchID所对应的一组资源
func (h *handleWithUser) ReleaseResource(resBatchID string) error {
	if h.mgr != nil {
		return h.mgr.releaseResource(h.userID, resBatchID)
	}

	return fmt.Errorf("drm: direct resource manager is nil")
}

// ListResource list所有资源
func (h *handleWithUser) ListResource(resBatchID string) ([]*AgentResourceExternal, error) {
	if h.mgr != nil {
		return h.mgr.listResource(h.userID, resBatchID)
	}

	return nil, fmt.Errorf("drm: direct resource manager is nil")
}

// ExecuteCommand 在指定的batchID对应的资源上, 执行指令
func (h *handleWithUser) ExecuteCommand(ip string, resBatchID string, cmd *Command) error {
	if h.mgr != nil {
		return h.mgr.executeCommand(h.userID, ip, resBatchID, cmd)
	}

	return fmt.Errorf("drm: direct resource manager is nil")
}

// ListCommands list 指定的batchID所对应资源上的命令信息
func (h *handleWithUser) ListCommands(resBatchID string) ([]*CommandResultInfo, error) {
	if h.mgr != nil {
		return h.mgr.listCommands(h.userID, resBatchID)
	}

	return nil, fmt.Errorf("drm: direct resource manager is nil")
}

func randomString(length uint16) string {
	b := make([]rune, length)
	for i := range b {
		b[i] = LetterRunes[rand.Intn(len(LetterRunes))]
	}
	return string(b)
}

func generateUniqID() string {
	return fmt.Sprintf("%s_%d", randomString(16), time.Now().Unix())
}

func recordResource(agent *oneagentResource) {
	metricLabels := controllers.ResourceStatusLabels{
		IP:   agent.Agent.Base.IP,
		Zone: fmt.Sprintf("direct_%s", agent.Agent.Base.Cluster),
	}
	selfMetric.ResourceStatusController.UpdateCPUTotal(metricLabels, agent.Agent.Total.CPU)
	selfMetric.ResourceStatusController.UpdateCPUUsed(metricLabels, agent.Agent.Total.CPU-agent.Agent.Free.CPU)
	selfMetric.ResourceStatusController.UpdateMemTotal(metricLabels, agent.Agent.Total.Mem)
	selfMetric.ResourceStatusController.UpdateMemUsed(metricLabels, agent.Agent.Total.Mem-agent.Agent.Free.Mem)
	selfMetric.ResourceStatusController.UpdateDiskTotal(metricLabels, agent.Agent.Total.Disk)
	selfMetric.ResourceStatusController.UpdateDiskUsed(metricLabels, agent.Agent.Total.Disk-agent.Agent.Free.Disk)
}
