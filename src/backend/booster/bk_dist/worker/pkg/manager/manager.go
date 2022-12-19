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
	"fmt"
	"io/ioutil"
	"math"
	"net"
	"os"
	"path/filepath"
	"runtime"
	"runtime/debug"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	dcProtocol "github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/worker/config"
	pbcmd "github.com/Tencent/bk-ci/src/booster/bk_dist/worker/pkg/cmd_handler"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/worker/pkg/protocol"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	commonUtil "github.com/Tencent/bk-ci/src/booster/common/util"
	"github.com/shirou/gopsutil/cpu"
	"github.com/shirou/gopsutil/mem"
)

// Manager manager hosts
type Manager interface {
	DealTCPConn(conn *net.TCPConn) error
	Run()
}

// define const vars
const (
	maxjobs4worker = 144

	cmdCheckIntervalTime     = 100 // time.Millisecond
	maxWaitConnectionSeconds = 600

	maxCpuUsed      = 90.00
	maxCpuSampleNum = 20
)

// NewManager return Manager interface
func NewManager(conf *config.ServerConfig) (Manager, error) {
	o := &tcpManager{
		conf:         conf,
		buffedcmds:   nil,
		curjobs:      0,
		whiteips:     make([]string, 0),
		allowallips:  false,
		memperjob:    1024 * 1024 * 1024 * 2, // 2G
		cpusamplenum: maxCpuSampleNum,
		cpusample:    make([]float64, maxCpuSampleNum),
		cpuindex:     0,
		cpulast:      0.0,
	}

	if err := o.init(); err != nil {
		blog.Errorf("failed to init bk-dist-Manager,err[%v]", err)
		return nil, err
	}

	go o.Run()

	return o, nil
}

type buffedcmd struct {
	client       *protocol.TCPClient
	head         *dcProtocol.PBHead
	receivedtime time.Time
	handler      pbcmd.Handler
	body         interface{}
	// basedir to save all files which belong this task and not absolute path
	// this dir will be removed when task finished
	basedir string

	// // commondir to save all files which belong this task and with absolute path
	// // we will not remove this dir when task finished, only specified file type will be deleted
	// commondir   string
	// commontypes []string
}

type tcpManager struct {
	conf *config.ServerConfig

	bufflock   sync.RWMutex
	buffedcmds []*buffedcmd

	joblock    sync.RWMutex
	curjobs    int
	maxjobs    int
	memperjob  uint64 // memory size for each job
	maxprocess int

	whiteips    []string
	allowallips bool

	// commonFileDir   string
	// commonFileTypes []string

	// to recieve create file path
	filepathchan    chan string
	filelist        []string
	filelistlogfile string
	defaultworkdir  string

	// to smooth cpu
	cpusample    []float64
	cpulast      float64
	cpuindex     int32
	cpusamplenum int32
}

func (o *tcpManager) init() error {
	blog.Infof("init...")

	// init max process
	o.maxprocess = 8
	envmaxcpus := env.GetEnv(env.KeyWorkerMaxProcess)
	if envmaxcpus != "" {
		intval, err := strconv.ParseInt(envmaxcpus, 10, 64)
		if err == nil && intval > 0 {
			o.maxprocess = int(intval)
		}
	}
	blog.Infof("default NumCPU: %d, GOMAXPROCS: %d", runtime.NumCPU(), runtime.GOMAXPROCS(-1))
	blog.Infof("ready set max process %d", o.maxprocess)
	runtime.GOMAXPROCS(o.maxprocess)

	// init max jobs
	o.maxjobs = int(float32(o.maxprocess) * 1.5)
	envjobs := env.GetEnv(env.KeyWorkerMaxJobs)
	if envjobs != "" {
		intval, err := strconv.ParseInt(envjobs, 10, 64)
		if err == nil && intval > 0 && intval < maxjobs4worker {
			o.maxjobs = int(intval)
		}
	}
	blog.Infof("set max job %d", o.maxjobs)

	envmemperjob := env.GetEnv(env.KeyWorkerMemPerJob)
	if envmemperjob != "" {
		intval, err := strconv.ParseInt(envmemperjob, 10, 64)
		if err == nil && intval > 0 {
			o.memperjob = uint64(intval)
			// set o.maxjobs by o.memperjob
			// it's maybe system memory, not docker, but harmless
			v, err := mem.VirtualMemory()
			if err == nil {
				maxjobsbymem := v.Total / o.memperjob
				o.maxjobs = int(math.Min(float64(o.maxjobs), float64(maxjobsbymem)))
				blog.Infof("set mem per job %d,max job %d", o.memperjob, o.maxjobs)
			}
		}
	}
	blog.Infof("set mem per job %d", o.memperjob)

	// init file path chan
	o.filepathchan = make(chan string, o.maxjobs)
	o.filelist = make([]string, 0, 1)
	o.filelistlogfile = "./created_abs_files.txt"
	_ = o.cleanCreatedFiles(o.filelistlogfile)

	// init white ips
	envwhiteips := env.GetEnv(env.KeyWorkerWhiteIP)
	if envwhiteips != "" {
		ipfields := strings.Fields(envwhiteips)
		if len(ipfields) > 0 {
			o.whiteips = ipfields
		}
		for _, v := range o.whiteips {
			if v == "0.0.0.0" {
				o.allowallips = true
				break
			}
		}
	}
	blog.Infof("set white ip list %v,allowallips %t", o.whiteips, o.allowallips)

	// chdir
	if o.conf.DefaultWorkDir != "" {
		_ = os.MkdirAll(o.conf.DefaultWorkDir, os.ModePerm)
		err := os.Chdir(o.conf.DefaultWorkDir)
		if err != nil {
			blog.Errorf("failed to chdir,error: %v", err)
			return err
		}

		blog.Infof("set work dir to %s", o.conf.DefaultWorkDir)
	} else {
		err := fmt.Errorf("default work dir can't be empty, ensure default_work_dir is specified in conf file")
		blog.Errorf("%v", err)
		return err
	}

	o.defaultworkdir = filepath.Base(o.conf.DefaultWorkDir)

	// init handlers
	pbcmd.InitHandlers()

	return nil
}

// Run brings up the worker
func (o *tcpManager) Run() {
	blog.Infof("manager run")

	// TODO : start signal handler

	cmdCheckTick := time.NewTicker(cmdCheckIntervalTime * time.Millisecond)
	for {
		select {
		case <-cmdCheckTick.C:
			o.checkCmds()
		case f := <-o.filepathchan:
			blog.Infof("got message of file[%s] created", f)
			// TODO : save f to local json file
		}
	}
}

func (o *tcpManager) onFileCreated(f string) {
	if strings.Contains(f, o.defaultworkdir) {
		blog.Infof("file[%s] is in work dir, do nothing now", f)
		return
	}

	for _, v := range o.filelist {
		if v == f {
			blog.Infof("file[%s] is already in list, do nothing now", f)
			return
		}
	}

	// do not check exist now
	// existed, _, _, _, _ := dcUtil.FileInfo(f)
	// if !existed {
	// 	blog.Infof("file[%s] is not existed, do nothing now", f)
	// 	return
	// }

	o.filelist = append(o.filelist, f)
	// write to file
	data := []byte(strings.Join(o.filelist, "\n"))
	_ = ioutil.WriteFile(f, data, os.ModePerm)
	blog.Infof("wrote [%s] into file[%s]", data, f)
}

func (o *tcpManager) cleanCreatedFiles(f string) error {
	// TODO : clean DefaultWorkDir

	data, err := ioutil.ReadFile(f)
	if err != nil {
		blog.Warnf("failed to read file[%s] with error:%v", err)
		return err
	}

	files := strings.Split(string(data), "\n")
	for _, v := range files {
		if dcFile.Stat(v).Exist() {
			blog.Infof("ready to remove file[%s]", v)
			_ = os.Remove(v)
		}
	}

	err = os.Truncate(f, 0)
	if err != nil {
		blog.Warnf("failed to truncate file[%s] with error:%v", err)
		return err
	}
	blog.Infof("truncated file[%s]", f)

	return nil
}

// DealTCPConn deal with new tcp connection
func (o *tcpManager) DealTCPConn(conn *net.TCPConn) error {
	go func() {
		_ = o.dealTCPConn(conn)
	}()

	return nil
}

func (o *tcpManager) dealTCPConn(conn *net.TCPConn) error {
	remoteip := conn.RemoteAddr().String()
	blog.Infof("deal tcp conn %s", remoteip)

	if !o.validIP(remoteip) {
		_ = conn.Close()
		blog.Errorf("remote ip %s not in white list", remoteip)
		return fmt.Errorf("remote ip %s not in white list", remoteip)
	}

	// receive head
	client := protocol.NewTCPClientWithConn(conn)
	head, err := protocol.ReceiveBKCommonHead(client)
	if err != nil {
		blog.Errorf("failed to receive head for client %s error:%v", remoteip, err)
		_ = client.Close()
		return err
	}

	if head == nil {
		err := fmt.Errorf("head is nil")
		blog.Errorf("%v", err)
		_ = client.Close()
		return err
	}

	switch head.GetCmdtype() {
	case dcProtocol.PBCmdType_DISPATCHTASKREQ:
		return o.dealRemoteTaskCmd(client, head)
	case dcProtocol.PBCmdType_SYNCTIMEREQ:
		return o.dealSyncTimeCmd(client, head)
	case dcProtocol.PBCmdType_SENDFILEREQ:
		return o.dealSendFileCmd(client, head)
	case dcProtocol.PBCmdType_CHECKCACHEREQ:
		return o.dealCheckCacheCmd(client, head)
	default:
		err := fmt.Errorf("unknow cmd %s", head.GetCmdtype())
		blog.Warnf("%v", err)
		// client.Close()
		// return err
		return o.dealUnknownCmd(client, head)
	}
}

func (o *tcpManager) dealRemoteTaskCmd(client *protocol.TCPClient, head *dcProtocol.PBHead) error {
	handler := pbcmd.GetHandler(head.GetCmdtype())
	if handler == nil {
		err := fmt.Errorf("failed to get handler for cmd %s", head.GetCmdtype())
		blog.Errorf("%v", err)
		_ = client.Close()
		return err
	}

	// receive body
	basedir := commonUtil.RandomString(16)
	body, err := handler.ReceiveBody(client, head, basedir, o.filepathchan)
	if err != nil {
		blog.Errorf("failed to receive body error: %v", err)
		_ = client.Close()
		return err
	}

	if body == nil {
		err := fmt.Errorf("body is nil")
		blog.Errorf("%v", err)
		_ = client.Close()
		return err
	}

	debug.FreeOSMemory() // free memory anyway

	curcmd := buffedcmd{
		client:       client,
		head:         head,
		receivedtime: time.Now(),
		handler:      handler,
		body:         body,
		basedir:      basedir,
		// commondir:    o.commonFileDir,
		// commontypes:  o.commonFileTypes,
	}
	if o.obtainChance() {
		go func() {
			_ = o.dealBufferedCmd(&curcmd)
		}()
		return nil
	}

	o.appendcmd(&curcmd)
	o.checkCmds()

	return nil
}

func (o *tcpManager) dealSyncTimeCmd(client *protocol.TCPClient, head *dcProtocol.PBHead) error {
	handler := pbcmd.GetHandler(head.GetCmdtype())
	if handler == nil {
		err := fmt.Errorf("failed to get handler for cmd %s", head.GetCmdtype())
		blog.Errorf("%v", err)
		_ = client.Close()
		return err
	}

	return handler.Handle(client, head, nil, time.Now(), "", nil)
}

func (o *tcpManager) dealUnknownCmd(client *protocol.TCPClient, head *dcProtocol.PBHead) error {
	handler := pbcmd.GetHandler(head.GetCmdtype())
	if handler == nil {
		err := fmt.Errorf("failed to get handler for cmd %s", head.GetCmdtype())
		blog.Errorf("%v", err)
		_ = client.Close()
		return err
	}

	_, _ = handler.ReceiveBody(client, head, "", nil)

	return handler.Handle(client, head, nil, time.Now(), "", nil)
}

func (o *tcpManager) dealSendFileCmd(client *protocol.TCPClient, head *dcProtocol.PBHead) error {
	handler := pbcmd.GetHandler(head.GetCmdtype())
	if handler == nil {
		err := fmt.Errorf("failed to get handler for cmd %s", head.GetCmdtype())
		blog.Errorf("%v", err)
		_ = client.Close()
		return err
	}

	// receive body
	basedir := commonUtil.RandomString(16)
	body, err := handler.ReceiveBody(client, head, basedir, o.filepathchan)
	if err != nil {
		blog.Errorf("failed to receive body error: %v", err)
		_ = client.Close()
		return err
	}

	if body == nil {
		err := fmt.Errorf("body is nil")
		blog.Errorf("%v", err)
		_ = client.Close()
		return err
	}

	debug.FreeOSMemory() // free memory anyway

	defer func() {
		blog.Infof("ready to close tcp connection after deal this cmd")
		_ = client.Close()
		debug.FreeOSMemory() // free memory anyway
	}()

	//
	err = handler.Handle(client, head, body, time.Now(), basedir, nil)
	return err
}

func (o *tcpManager) dealCheckCacheCmd(client *protocol.TCPClient, head *dcProtocol.PBHead) error {
	handler := pbcmd.GetHandler(head.GetCmdtype())
	if handler == nil {
		err := fmt.Errorf("failed to get handler for cmd %s", head.GetCmdtype())
		blog.Errorf("%v", err)
		_ = client.Close()
		return err
	}

	body, err := handler.ReceiveBody(client, head, "", o.filepathchan)
	if err != nil {
		blog.Errorf("failed to receive body error: %v", err)
		_ = client.Close()
		return err
	}

	if body == nil {
		err := fmt.Errorf("body is nil")
		blog.Errorf("%v", err)
		_ = client.Close()
		return err
	}

	debug.FreeOSMemory() // free memory anyway

	defer func() {
		blog.Infof("ready to close tcp connection after deal this cmd")
		_ = client.Close()
		debug.FreeOSMemory() // free memory anyway
	}()

	//
	err = handler.Handle(client, head, body, time.Now(), "", nil)
	return err
}

func (o *tcpManager) validIP(clientip string) bool {
	if o.allowallips {
		return true
	}

	if clientip == "" {
		blog.Warnf("client ip is empty when valid ip")
		return false
	}

	if len(o.whiteips) == 0 {
		blog.Warnf("failed to valid client ip %s for target white ip is empty", clientip)
		return false
	}

	fields := strings.Split(clientip, ":")
	realip := fields[0]
	for _, v := range o.whiteips {
		if v == realip {
			blog.Infof("found client ip %s in white list", realip)
			return true
		}
	}

	blog.Infof("not found client ip %s in white list", realip)
	return false
}

func waitConnection(client *protocol.TCPClient, timeoutsecs int) {
	blog.Infof("wait connection[%s] to closed or timeout", client.RemoteAddr())

	tick := time.NewTicker(1 * time.Second)
	starttime := time.Now()
	for {
		select {
		case <-tick.C:
			if client.Closed() {
				blog.Infof("connection[%s] closed by remote", client.RemoteAddr())
				return
			}

			curtime := time.Now()
			if curtime.Sub(starttime) > (time.Duration(timeoutsecs) * time.Second) {
				blog.Infof("connection[%s] timeout with [%d]seconds", client.RemoteAddr(), timeoutsecs)
				return
			}
		}
	}
}

func (o *tcpManager) dealBufferedCmd(cmd *buffedcmd) error {
	// blog.Infof("deal cmd in...")
	defer func() {
		debug.FreeOSMemory() // free memory anyway
		// wait until connection closed by client or timeout
		waitConnection(cmd.client, maxWaitConnectionSeconds)

		// blog.Infof("ready to close tcp connection after deal this cmd")
		_ = cmd.client.Close()
		if o.conf.CleanTempFiles {
			// remove basedir here
			_ = os.RemoveAll(cmd.basedir)
			blog.Infof("deal cmd out, removed base dir:%s", cmd.basedir)
		}

		debug.FreeOSMemory() // free memory anyway
	}()

	_ = os.MkdirAll(cmd.basedir, os.ModePerm)
	err := cmd.handler.Handle(cmd.client, cmd.head, cmd.body, cmd.receivedtime, cmd.basedir, o.conf.CmdReplaceRules)

	// nextcmd := o.popcmd()
	// if nextcmd != nil {
	// 	go func() {
	// 		_ = o.dealBufferedCmd(nextcmd)
	// 	}()
	// } else {
	// 	o.freeChance()
	// }

	// to ensure check chance before select new cmd
	o.freeChance()
	o.checkCmds()

	return err
}

func (o *tcpManager) checkCmds() {
	o.bufflock.Lock()
	defer func() {
		o.bufflock.Unlock()
	}()

	newbuff := o.buffedcmds
	for i, v := range o.buffedcmds {
		if o.obtainChance() {
			go func(vv *buffedcmd) {
				_ = o.dealBufferedCmd(vv)
			}(v)

			// blog.Infof("remove job from waiting jobs...")
			// delete from array
			newbuff = o.buffedcmds[i+1:]
		} else {
			break
		}
	}
	o.buffedcmds = newbuff
}

func (o *tcpManager) obtainChance() bool {
	// blog.Infof("obtain chance in...")
	o.joblock.Lock()
	defer func() {
		o.joblock.Unlock()
		// blog.Infof("obtain chance out...")
	}()

	if o.curjobs < o.maxjobs {
		// we will launch docker with 8 jobs on linux
		if o.curjobs < int(math.Min(float64(o.maxjobs/3), 4)) {
			// blog.Infof("got chance directly for current running jobs less than min jobs")
			o.curjobs++
			blog.Infof("current running jobs %d after direct got chance", o.curjobs)
			return true
		} else {
			var available, total uint64
			var cpuper float64
			// check avaiable resource now
			// it's maybe system resource, not docker, but harmless
			v, err := mem.VirtualMemory()
			if err == nil {
				available = v.Available
				total = v.Total
				maybetotalused := uint64(o.curjobs) * o.memperjob
				if maybetotalused >= v.Total {
					blog.Infof("ignore for current total used mem:%d greater than total mem:%d", maybetotalused, v.Total)
					return false
				}

				if v.Available < o.memperjob {
					blog.Infof("ignore for current available mem:%d less than mem per job:%d", v.Available, o.memperjob)
					return false
				}

				avgmem := (v.Total - v.Free) / uint64(o.curjobs)
				if v.Available < avgmem {
					blog.Infof("ignore for current available mem:%d less than avg mem:%d", v.Available, avgmem)
					return false
				}
			}

			per, err := cpu.Percent(0, false)
			if err == nil {
				cpuper = per[0]
				scpu := o.smoothcpu(per[0])
				if per[0] <= 0.0 {
					per[0] = o.cpulast
				} else {
					o.cpulast = per[0]
				}
				curcpu := math.Max(scpu, per[0])
				if curcpu > maxCpuUsed {
					blog.Infof("ignore for current smooth cpu usage:%f(or curcpu:%f) over max allowed cpu usage:%f", scpu, per[0], maxCpuUsed)
					return false
				}
			}

			// failed to get resource info or has enought resource, return ok
			// blog.Infof("got chance for enought resource cpu:%f total mem:%d,available mem:%d", cpuper, total, available)
			o.curjobs++
			blog.Infof("current running jobs %d for enought resource cpu:%f total mem:%d available mem:%d", o.curjobs, cpuper, total, available)
			return true
		}
	}

	return false
}

func (o *tcpManager) freeChance() {
	// blog.Infof("free chance in...")
	o.joblock.Lock()
	defer func() {
		o.joblock.Unlock()
		// blog.Infof("free chance out...")
	}()

	o.curjobs--
	blog.Infof("current running jobs %d after one job finished", o.curjobs)
	if o.curjobs < 0 {
		o.curjobs = 0
	}
}

func (o *tcpManager) appendcmd(cmd *buffedcmd) {
	// blog.Infof("append cmd in...")
	o.bufflock.Lock()
	defer func() {
		blog.Infof("current waiting jobs %d after append one job", len(o.buffedcmds))
		o.bufflock.Unlock()
	}()

	o.buffedcmds = append(o.buffedcmds, cmd)
}

func (o *tcpManager) popcmd() *buffedcmd {
	blog.Infof("pop cmd in...")
	o.bufflock.Lock()
	defer func() {
		o.bufflock.Unlock()
	}()

	var cmd *buffedcmd
	if len(o.buffedcmds) > 0 {
		cmd = o.buffedcmds[0]
		o.buffedcmds = o.buffedcmds[1:]
	}

	return cmd
}

// return average of last 10 cpu
func (o *tcpManager) smoothcpu(now float64) float64 {
	o.cpusample[o.cpuindex] = now
	o.cpuindex = (o.cpuindex + 1) % o.cpusamplenum

	var total float64
	num := 0
	for _, v := range o.cpusample {
		if v > 0 {
			total += v
			num++
		}
	}

	if num > 0 {
		return total / float64(num)
	}

	return 0.0
}
