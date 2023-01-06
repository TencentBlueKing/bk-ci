/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package remote

import (
	"context"
	"fmt"
	"runtime/debug"
	"sync"
	"sync/atomic"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcProtocol "github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/config"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/types"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/worker/pkg/client"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

// NewMgr get a new Remote Mgr
func NewMgr(pCtx context.Context, work *types.Work) types.RemoteMgr {
	ctx, _ := context.WithCancel(pCtx)

	return &Mgr{
		ctx:                   ctx,
		work:                  work,
		resource:              newResource(nil),
		remoteWorker:          client.NewCommonRemoteWorker(),
		checkSendFileTick:     100 * time.Millisecond,
		fileSendMap:           make(map[string]*fileSendMap),
		fileCollectionSendMap: make(map[string]*[]*types.FileCollectionInfo),
		fileMessageBank:       newFileMessageBank(),
		conf:                  work.Config(),
		resourceCheckTick:     5 * time.Second,
		sendCorkTick:          10 * time.Millisecond,
		// corkSize:              1024 * 512, // 512KB, it will delay much if too big
		corkSize:      1024 * 10,
		corkMaxSize:   1024 * 1024 * 100,
		corkFiles:     make(map[string]*[]*corkFile, 0),
		memSlot:       newMemorySlot(0),
		largeFileSize: 1024 * 1024 * 100, // 100MB
	}
}

const (
	syncHostTimeTimes = 3
)

// Mgr describe the remote manager
// provides the actions handler to remote workers
type Mgr struct {
	ctx context.Context

	work         *types.Work
	resource     *resource
	remoteWorker dcSDK.RemoteWorker

	memSlot *memorySlot

	checkSendFileTick time.Duration

	fileSendMutex sync.RWMutex
	fileSendMap   map[string]*fileSendMap

	fileCollectionSendMutex sync.RWMutex
	fileCollectionSendMap   map[string]*[]*types.FileCollectionInfo

	fileMessageBank *fileMessageBank

	// initCancel context.CancelFunc

	conf *config.ServerConfig

	resourceCheckTick time.Duration
	lastUsed          uint64 // only accurate to second now
	lastApplied       uint64 // only accurate to second now
	remotejobs        int64  // save job number which using remote worker

	sendCorkTick time.Duration
	sendCorkChan chan bool
	corkMutex    sync.RWMutex
	corkSize     int64
	corkMaxSize  int64
	corkFiles    map[string]*[]*corkFile

	largeFileSize int64
}

type fileSendMap struct {
	sync.Mutex
	cache map[string]*[]*types.FileInfo
}

func (fsm *fileSendMap) matchOrInsert(desc dcSDK.FileDesc) (*types.FileInfo, bool) {
	fsm.Lock()
	defer fsm.Unlock()

	if fsm.cache == nil {
		fsm.cache = make(map[string]*[]*types.FileInfo)
	}

	info := &types.FileInfo{
		FullPath:           desc.FilePath,
		Size:               desc.FileSize,
		LastModifyTime:     desc.Lastmodifytime,
		Md5:                desc.Md5,
		TargetRelativePath: desc.Targetrelativepath,
		FileMode:           desc.Filemode,
		LinkTarget:         desc.LinkTarget,
		SendStatus:         types.FileSending,
	}

	c, ok := fsm.cache[desc.FilePath]
	if !ok || c == nil || len(*c) == 0 {
		infoList := []*types.FileInfo{info}
		fsm.cache[desc.FilePath] = &infoList
		return info, false
	}

	for _, ci := range *c {
		if ci.Match(desc) {
			return ci, true
		}
	}

	*c = append(*c, info)
	return info, false
}

func (fsm *fileSendMap) matchOrInserts(descs []*dcSDK.FileDesc) []matchResult {
	fsm.Lock()
	defer fsm.Unlock()

	if fsm.cache == nil {
		fsm.cache = make(map[string]*[]*types.FileInfo)
	}

	result := make([]matchResult, 0, len(descs))
	for _, desc := range descs {
		info := &types.FileInfo{
			FullPath:           desc.FilePath,
			Size:               desc.FileSize,
			LastModifyTime:     desc.Lastmodifytime,
			Md5:                desc.Md5,
			TargetRelativePath: desc.Targetrelativepath,
			FileMode:           desc.Filemode,
			LinkTarget:         desc.LinkTarget,
			SendStatus:         types.FileSending,
		}

		c, ok := fsm.cache[desc.FilePath]
		if !ok || c == nil || len(*c) == 0 {
			infoList := []*types.FileInfo{info}
			fsm.cache[desc.FilePath] = &infoList
			result = append(result, matchResult{
				info:  info,
				match: false,
			})
			continue
		}

		matched := false
		for _, ci := range *c {
			if ci.Match(*desc) {
				result = append(result, matchResult{
					info:  ci,
					match: true,
				})
				matched = true
				break
			}
		}
		if matched {
			continue
		}

		*c = append(*c, info)
		result = append(result, matchResult{
			info:  info,
			match: false,
		})
	}

	return result
}

func (fsm *fileSendMap) updateStatus(desc dcSDK.FileDesc, status types.FileSendStatus) {
	fsm.Lock()
	defer fsm.Unlock()

	if fsm.cache == nil {
		fsm.cache = make(map[string]*[]*types.FileInfo)
	}

	info := &types.FileInfo{
		FullPath:           desc.FilePath,
		Size:               desc.FileSize,
		LastModifyTime:     desc.Lastmodifytime,
		Md5:                desc.Md5,
		TargetRelativePath: desc.Targetrelativepath,
		FileMode:           desc.Filemode,
		LinkTarget:         desc.LinkTarget,
		SendStatus:         status,
	}

	c, ok := fsm.cache[desc.FilePath]
	if !ok || c == nil || len(*c) == 0 {
		infoList := []*types.FileInfo{info}
		fsm.cache[desc.FilePath] = &infoList
		return
	}

	for _, ci := range *c {
		if ci.Match(desc) {
			ci.SendStatus = status
			return
		}
	}

	*c = append(*c, info)
	return
}

// Init do the initialization for remote manager
// !! only call once !!
func (m *Mgr) Init() {
	blog.Infof("remote: init for work:%s", m.work.ID())

	// settings := m.work.Basic().Settings()
	// m.resource = newResource(m.syncHostTimeNoWait(m.work.Resource().GetHosts()), settings.UsageLimit)
	m.resource = newResource(m.syncHostTimeNoWait(m.work.Resource().GetHosts()))

	// if m.initCancel != nil {
	// 	m.initCancel()
	// }
	ctx, _ := context.WithCancel(m.ctx)
	// m.initCancel = cancel

	m.resource.Handle(ctx)

	m.memSlot.Handle(ctx)

	// register call back for resource changed
	m.work.Resource().RegisterCallback(m.callback4ResChanged)

	if m.conf.AutoResourceMgr {
		go m.resourceCheck(ctx)
	}

	if m.conf.SendCork {
		m.sendCorkChan = make(chan bool, 1000)
		go m.sendFilesWithCorkTick(ctx)
	}
}

func (m *Mgr) Start() {
	blog.Infof("remote: start for work:%s", m.work.ID())
}

func (m *Mgr) callback4ResChanged() error {
	blog.Infof("remote: resource changed call back for work:%s", m.work.ID())

	hl := m.work.Resource().GetHosts()
	m.resource.Reset(hl)
	if hl != nil && len(hl) > 0 {
		m.setLastApplied(uint64(time.Now().Local().Unix()))
		m.syncHostTimeNoWait(hl)
	}

	// if all workers released, we shoud clean the cache now
	if hl == nil || len(hl) == 0 {
		m.cleanFileCache()

		// TODO : do other cleans here
	}
	return nil
}

func (m *Mgr) cleanFileCache() {
	blog.Infof("remote: clean all file cache when all resource released for work:%s", m.work.ID())

	m.fileSendMutex.Lock()
	m.fileSendMap = make(map[string]*fileSendMap)
	m.fileSendMutex.Unlock()

	m.fileCollectionSendMutex.Lock()
	m.fileCollectionSendMap = make(map[string]*[]*types.FileCollectionInfo)
	m.fileCollectionSendMutex.Unlock()

	m.corkMutex.Lock()
	m.corkFiles = make(map[string]*[]*corkFile, 0)
	m.corkMutex.Unlock()
}

func (m *Mgr) setLastUsed(v uint64) {
	atomic.StoreUint64(&m.lastUsed, v)
}

func (m *Mgr) getLastUsed() uint64 {
	return atomic.LoadUint64(&m.lastUsed)
}

func (m *Mgr) setLastApplied(v uint64) {
	atomic.StoreUint64(&m.lastApplied, v)
}

func (m *Mgr) getLastApplied() uint64 {
	return atomic.LoadUint64(&m.lastApplied)
}

// IncRemoteJobs inc remote jobs
func (m *Mgr) IncRemoteJobs() {
	atomic.AddInt64(&m.remotejobs, 1)
}

// DecRemoteJobs dec remote jobs
func (m *Mgr) DecRemoteJobs() {
	atomic.AddInt64(&m.remotejobs, -1)
}

func (m *Mgr) getRemoteJobs() int64 {
	return atomic.LoadInt64(&m.remotejobs)
}

func (m *Mgr) resourceCheck(ctx context.Context) {
	blog.Infof("remote: run remote resource check tick for work: %s", m.work.ID())
	ticker := time.NewTicker(m.resourceCheckTick)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			blog.Infof("remote: run remote resource check for work(%s) canceled by context", m.work.ID())
			return

		case <-ticker.C:
			if m.getRemoteJobs() <= 0 { // no remote worker in use
				needfree := false
				// 从最近一次使用后的时间开始计算空闲时间
				lastused := m.getLastUsed()
				if lastused > 0 {
					nowsecs := time.Now().Local().Unix()
					if int(uint64(nowsecs)-lastused) > m.conf.ResIdleSecsForFree {
						blog.Infof("remote: ready release remote resource for work(%s) %d"+
							" seconds no used since last used %d",
							m.work.ID(), int(uint64(nowsecs)-lastused), lastused)

						needfree = true
					}
				} else {
					// 从资源申请成功的时间开始计算空闲时间
					lastapplied := m.getLastApplied()
					if lastapplied > 0 {
						nowsecs := time.Now().Local().Unix()
						if int(uint64(nowsecs)-lastapplied) > m.conf.ResIdleSecsForFree {
							blog.Infof("remote: ready release remote resource for work(%s) %d"+
								" seconds no used since last applied %d",
								m.work.ID(), int(uint64(nowsecs)-lastapplied), lastapplied)

							needfree = true
						}
					}
				}

				if needfree {
					// disable all workers and release
					m.resource.disableAllWorker()
					// clean file cache
					m.cleanFileCache()
					// notify resource release
					m.work.Resource().Release(nil)
					// send and reset stat data
					m.work.Resource().SendAndResetStats(false, []int64{0})

					// 重置最近一次使用时间
					m.setLastUsed(0)
					// 重置资源申请成功的时间
					m.setLastApplied(0)
				}
			}
		}
	}
}

// ExecuteTask run the task in remote worker and ensure the dependent files
func (m *Mgr) ExecuteTask(req *types.RemoteTaskExecuteRequest) (*types.RemoteTaskExecuteResult, error) {
	if m.TotalSlots() <= 0 {
		return nil, types.ErrNoAvailableWorkFound
	}

	if req.Sandbox == nil {
		req.Sandbox = &dcSyscall.Sandbox{}
	}

	blog.Infof("remote: try to execute remote task for work(%s) from pid(%d) with timeout(%d)",
		m.work.ID(), req.Pid, req.IOTimeout)
	defer m.work.Basic().UpdateJobStats(req.Stats)

	dcSDK.StatsTimeNow(&req.Stats.RemoteWorkEnterTime)
	defer dcSDK.StatsTimeNow(&req.Stats.RemoteWorkLeaveTime)
	m.work.Basic().UpdateJobStats(req.Stats)

	// 如果有超过100MB的大文件，则在选择host时，作为选择条件
	fpath, _ := getMaxSizeFile(req, m.largeFileSize)
	req.Server = m.lockSlots(dcSDK.JobUsageRemoteExe, fpath)
	blog.Infof("remote: selected host(%s) with large file(%s)",
		req.Server.Server, fpath)

	dcSDK.StatsTimeNow(&req.Stats.RemoteWorkLockTime)
	defer dcSDK.StatsTimeNow(&req.Stats.RemoteWorkUnlockTime)
	defer m.unlockSlots(dcSDK.JobUsageRemoteExe, req.Server)
	m.work.Basic().UpdateJobStats(req.Stats)

	handler := m.remoteWorker.Handler(req.IOTimeout, req.Stats, func() {
		m.work.Basic().UpdateJobStats(req.Stats)
	}, req.Sandbox)

	m.IncRemoteJobs()
	defer func() {
		m.setLastUsed(uint64(time.Now().Local().Unix()))
		m.DecRemoteJobs()
	}()

	// 1. send toolchain if required  2. adjust exe remote path for req
	err := m.sendToolchain(handler, req)
	if err != nil {
		blog.Errorf("remote: execute remote task for work(%s) from pid(%d) to server(%s), "+
			"ensure tool chain failed: %v, going to disable host(%s)",
			m.work.ID(), req.Pid, req.Server.Server, err, req.Server.Server)

		m.resource.disableWorker(req.Server)
		return nil, err
	}

	remoteDirs, err := m.ensureFilesWithPriority(handler, req.Pid, req.Sandbox, getFileDetailsFromExecuteRequest(req))
	if err != nil {
		blog.Errorf("remote: execute remote task for work(%s) from pid(%d) to server(%s), "+
			"ensure files failed: %v", m.work.ID(), req.Pid, req.Server.Server, err)
		return nil, err
	}
	if err = updateTaskRequestInputFilesReady(req, remoteDirs); err != nil {
		blog.Errorf("remote: execute remote task for work(%s) from pid(%d) to server(%s), "+
			"update task input files ready failed: %v", m.work.ID(), req.Pid, req.Server.Server, err)
		return nil, err
	}

	dcSDK.StatsTimeNow(&req.Stats.RemoteWorkStartTime)
	m.work.Basic().UpdateJobStats(req.Stats)

	var result *dcSDK.BKDistResult
	if !req.Req.CustomSave {
		result, err = handler.ExecuteTask(req.Server, req.Req)
	} else {
		result, err = handler.ExecuteTaskWithoutSaveFile(req.Server, req.Req)
	}

	dcSDK.StatsTimeNow(&req.Stats.RemoteWorkEndTime)
	if err != nil {
		blog.Errorf("remote: execute remote task for work(%s) from pid(%d) to server(%s), "+
			"remote execute failed: %v", m.work.ID(), req.Pid, req.Server.Server, err)
		return nil, err
	}

	req.Stats.RemoteWorkSuccess = true
	blog.Infof("remote: success to execute remote task for work(%s) from pid(%d) to server(%s)",
		m.work.ID(), req.Pid, req.Server.Server)
	return &types.RemoteTaskExecuteResult{
		Result: result,
	}, nil
}

// SendFiles send the specific files to remote workers
func (m *Mgr) SendFiles(req *types.RemoteTaskSendFileRequest) ([]string, error) {
	return m.ensureFilesWithPriority(
		m.remoteWorker.Handler(
			0,
			req.Stats,
			func() {
				m.work.Basic().UpdateJobStats(req.Stats)
			},
			nil,
		),
		req.Pid,
		req.Sandbox,
		getFileDetailsFromSendFileRequest(req),
	)
}

func (m *Mgr) ensureFilesWithPriority(
	handler dcSDK.RemoteWorkerHandler,
	pid int,
	sandbox *dcSyscall.Sandbox,
	fileDetails []*types.FilesDetails) ([]string, error) {

	fileMap := make(map[dcSDK.FileDescPriority]*[]*types.FilesDetails)
	posMap := make(map[dcSDK.FileDescPriority]*[]int)
	var maxP dcSDK.FileDescPriority = 0

	for index, fd := range fileDetails {
		p, _ := posMap[fd.File.Priority]
		f, ok := fileMap[fd.File.Priority]
		if !ok {
			fp := make([]*types.FilesDetails, 0, 10)
			f = &fp
			fileMap[fd.File.Priority] = f

			pp := make([]int, 0, 10)
			p = &pp
			posMap[fd.File.Priority] = p
		}

		*f = append(*f, fd)
		*p = append(*p, index)
		if fd.File.Priority > maxP {
			maxP = fd.File.Priority
		}
	}

	result := make([]string, len(fileDetails))
	for i := dcSDK.MaxFileDescPriority; i <= maxP; i++ {
		p, _ := posMap[i]
		f, ok := fileMap[i]
		if !ok {
			continue
		}

		blog.Infof("remote: try to ensure priority(%d) files(%d) for work(%s) from pid(%d) dir(%s) to server",
			i, len(*f), m.work.ID(), pid, sandbox.Dir)
		r, err := m.ensureFiles(handler, pid, sandbox, *f)
		if err != nil {
			return nil, err
		}

		for index, path := range r {
			result[(*p)[index]] = path
		}
	}
	return result, nil
}

// ensureFiles 确保提供的文件被传输到目标worker的目标目录上
// 同时结合settings.FilterRules来防止相同的文件被重复传输
// 返回一个列表, 表示文件在远程的目标目录
func (m *Mgr) ensureFiles(
	handler dcSDK.RemoteWorkerHandler,
	pid int,
	sandbox *dcSyscall.Sandbox,
	fileDetails []*types.FilesDetails) ([]string, error) {

	settings := m.work.Basic().Settings()
	blog.Infof("remote: try to ensure multi %d files for work(%s) from pid(%d) dir(%s) to server",
		len(fileDetails), m.work.ID(), pid, sandbox.Dir)
	blog.Debugf("remote: try to ensure multi %d files for work(%s) from pid(%d) dir(%s) to server: %v",
		len(fileDetails), m.work.ID(), pid, sandbox.Dir, fileDetails)
	rules := settings.FilterRules

	// TODO : pump模式下，一次编译依赖的可能有上千个文件，现在的流程会随机的添加到cork发送队列
	// 需要保证一次编译的依赖同时插入到cork发送队列，这样可以尽快的启动远程编译，避免远程编译等待太久
	var err error
	wg := make(chan error, len(fileDetails)+1)
	count := 0
	r := make([]string, 0, 10)
	cleaner := make([]dcSDK.FileDesc, 0, 10)
	corkFiles := make(map[string]*[]*corkFile, 0)
	allServerCorkFiles := make(map[string]*[]*corkFile, 0)
	filesNum := len(fileDetails)
	for _, fd := range fileDetails {
		// 修改远程目录
		f := fd.File
		if f.Targetrelativepath == "" {
			f.Targetrelativepath = m.getRemoteFileBaseDir()
		}
		sender := &dcSDK.BKDistFileSender{Files: []dcSDK.FileDesc{f}}

		_, t, _ := rules.Satisfy(fd.File.FilePath)

		blog.Debugf("remote: ensure file %s and match rule %d", fd.File.FilePath, t)
		if f.AllDistributed {
			t = dcSDK.FilterRuleHandleAllDistribution
		}
		if f.NoDuplicated {
			t = dcSDK.FilterRuleHandleDeduplication
		}
		if f.CompressedSize == -1 || f.FileSize == -1 {
			t = dcSDK.FilterRuleHandleDefault
		}

		servers := make([]*dcProtocol.Host, 0, 0)
		switch t {
		case dcSDK.FilterRuleHandleDefault:
			r = append(r, "")
			continue

		case dcSDK.FilterRuleHandleAllDistribution:
			cleaner = append(cleaner, f)
			if err = m.fileMessageBank.ensure(sender, sandbox); err != nil {
				return nil, err
			}

			// 该文件需要被分发到所有的机器上
			servers = m.work.Resource().GetHosts()
		}
		r = append(r, f.Targetrelativepath)

		for _, s := range fd.Servers {
			if s == nil {
				continue
			}
			count++
			if !m.conf.SendCork {
				go func(err chan<- error, host *dcProtocol.Host, req *dcSDK.BKDistFileSender) {
					t := time.Now().Local()
					err <- m.ensureSingleFile(handler, host, req, sandbox)
					d := time.Now().Local().Sub(t)
					if d > 200*time.Millisecond {
						blog.Debugf("remote: single file cost time for work(%s) from pid(%d) to server(%s): %s, %s",
							m.work.ID(), pid, host.Server, d.String(), req.Files[0].FilePath)
					}
				}(wg, s, sender)
			} else {
				// TODO : for send cork
				cf := &corkFile{
					handler:    handler,
					host:       s,
					sandbox:    sandbox,
					file:       &f,
					resultchan: nil,
				}
				l, ok := corkFiles[s.Server]
				if !ok {
					// 预先分配好队列，避免频繁内存分配
					// newl := []*corkFile{cf}
					newl := make([]*corkFile, 0, filesNum)
					newl = append(newl, cf)
					corkFiles[s.Server] = &newl
				} else {
					*l = append(*l, cf)
				}
			}
		}

		// 分发额外的内容
		for _, s := range servers {
			if !m.conf.SendCork {
				go func(host *dcProtocol.Host, req *dcSDK.BKDistFileSender) {
					t := time.Now().Local()
					_ = m.ensureSingleFile(handler, host, req, sandbox)
					d := time.Now().Local().Sub(t)
					if d > 200*time.Millisecond {
						blog.Debugf("remote: single file cost time for work(%s) from pid(%d) to server(%s): %s, %s",
							m.work.ID(), pid, host.Server, d.String(), req.Files[0].FilePath)
					}
				}(s, sender)
			} else {
				// TODO : for send cork
				cf := &corkFile{
					handler:    handler,
					host:       s,
					sandbox:    sandbox,
					file:       &f,
					resultchan: nil,
				}
				l, ok := allServerCorkFiles[s.Server]
				if !ok {
					// 预先分配好队列，避免频繁内存分配
					// newl := []*corkFile{cf}
					newl := make([]*corkFile, 0, filesNum)
					newl = append(newl, cf)
					allServerCorkFiles[s.Server] = &newl
				} else {
					*l = append(*l, cf)
				}
			}
		}
	}

	if m.conf.SendCork {
		blog.Debugf("remote: ready to ensure multi %d cork files for work(%s) from pid(%d) to server",
			count, m.work.ID(), pid)

		for server, fs := range corkFiles {
			totalFileNum := len(*fs)
			descs := make([]*dcSDK.FileDesc, 0, totalFileNum)
			for _, v := range *fs {
				descs = append(descs, v.file)
			}
			results := m.checkOrLockCorkFiles(server, descs)
			blog.Debugf("remote: got %d results for %d cork files count:%d for work(%s) from pid(%d) to server",
				len(results), len(descs), count, m.work.ID(), pid)
			needSendCorkFiles := make([]*corkFile, 0, totalFileNum)
			for i, v := range results {
				if v.match {
					// 已发送完成的不启动协程了
					if v.info.SendStatus == types.FileSendSucceed {
						wg <- nil
						continue
					} else if v.info.SendStatus == types.FileSendFailed {
						wg <- types.ErrSendFileFailed
						continue
					}
				} else {
					// 不在缓存，意味着之前没有发送过
					(*fs)[i].resultchan = make(chan corkFileResult, 1)
					needSendCorkFiles = append(needSendCorkFiles, (*fs)[i])
				}

				// 启动协程跟踪未发送完成的文件
				c := (*fs)[i]
				go func(err chan<- error, c *corkFile, r matchResult) {
					err <- m.ensureSingleCorkFile(c, r)
				}(wg, c, v)
			}

			// TODO : 检查是否在server端有缓存了，如果有，则无需发送，调用 checkBatchCache

			blog.Infof("total %d cork files, need send %d files", totalFileNum, len(needSendCorkFiles))
			// append to cork files queue
			_ = m.appendCorkFiles(server, needSendCorkFiles)

			// notify send
			m.sendCorkChan <- true
		}

		// same with corkFiles, but do not notify wg
		for server, fs := range allServerCorkFiles {
			totalFileNum := len(*fs)
			descs := make([]*dcSDK.FileDesc, 0, totalFileNum)
			for _, v := range *fs {
				descs = append(descs, v.file)
			}
			results := m.checkOrLockCorkFiles(server, descs)
			needSendCorkFiles := make([]*corkFile, 0, totalFileNum)
			for i, v := range results {
				if v.match {
					// 已发送完成的不启动协程了
					if v.info.SendStatus == types.FileSendSucceed {
						continue
					} else if v.info.SendStatus == types.FileSendFailed {
						continue
					}
				} else {
					// 不在缓存，意味着之前没有发送过
					(*fs)[i].resultchan = make(chan corkFileResult, 1)
					needSendCorkFiles = append(needSendCorkFiles, (*fs)[i])
				}

				// 启动协程跟踪未发送完成的文件
				c := (*fs)[i]
				go func(c *corkFile, r matchResult) {
					_ = m.ensureSingleCorkFile(c, r)
				}(c, v)
			}

			blog.Infof("total %d cork files, need send %d files", totalFileNum, len(needSendCorkFiles))
			// append to cork files queue
			_ = m.appendCorkFiles(server, needSendCorkFiles)

			// notify send
			m.sendCorkChan <- true
		}
	}

	for i := 0; i < count; i++ {
		if err = <-wg; err != nil {
			blog.Infof("remote: failed to ensure multi %d files for work(%s) from pid(%d) to server with err:%v",
				count, m.work.ID(), pid, err)
			return nil, err
		}
	}
	blog.Infof("remote: success to ensure multi %d files for work(%s) from pid(%d) to server",
		count, m.work.ID(), pid)
	for _, f := range cleaner {
		go m.fileMessageBank.clean(f)
	}

	return r, nil
}

// ensureSingleFile 保证给到的第一个文件被正确分发到目标机器上, 若给到的文件多于一个, 多余的部分会被忽略
func (m *Mgr) ensureSingleFile(
	handler dcSDK.RemoteWorkerHandler,
	host *dcProtocol.Host,
	req *dcSDK.BKDistFileSender,
	sandbox *dcSyscall.Sandbox) (err error) {
	if len(req.Files) == 0 {
		return fmt.Errorf("empty files")
	}
	req.Files = req.Files[:1]
	desc := req.Files[0]
	blog.Debugf("remote: try to ensure single file(%s) for work(%s) to server(%s)",
		desc.FilePath, m.work.ID(), host.Server)

	status, ok := m.checkOrLockSendFile(host.Server, desc)

	// 已经有人发送了文件, 等待文件就绪
	if ok {
		blog.Debugf("remote: try to ensure single file(%s) for work(%s) to server(%s), "+
			"some one is sending this file", desc.FilePath, m.work.ID(), host.Server)
		tick := time.NewTicker(m.checkSendFileTick)
		defer tick.Stop()

		for status == types.FileSending {
			select {
			case <-tick.C:
				status, _ = m.checkOrLockSendFile(host.Server, desc)
			}
		}

		switch status {
		case types.FileSendFailed:
			blog.Errorf("remote: failed to ensure single file(%s) for work(%s) to server(%s), "+
				"file already sent and failed", desc.FilePath, m.work.ID(), host.Server)
			return types.ErrSendFileFailed
		case types.FileSendSucceed:
			blog.Debugf("remote: success to ensure single file(%s) for work(%s) to server(%s)",
				desc.FilePath, m.work.ID(), host.Server)
			return nil
		default:
			return fmt.Errorf("unknown file send status: %s", status.String())
		}
	}

	if m.checkSingleCache(handler, host, desc, sandbox) {
		m.updateSendFile(host.Server, desc, types.FileSendSucceed)
		return nil
	}

	// // send like tcp cork
	// if m.conf.SendCork {
	// 	retcode, err := m.sendFileWithCork(handler, &desc, host, sandbox)
	// 	if err != nil || retcode != 0 {
	// 		blog.Warnf("remote: execute send cork file(%s) for work(%s) to server(%s) failed: %v, retcode:%d",
	// 			desc.FilePath, m.work.ID(), host.Server, err, retcode)
	// 	} else {
	// 		blog.Debugf("remote: execute send cork file(%s) for work(%s) to server(%s) succeed",
	// 			desc.FilePath, m.work.ID(), host.Server)
	// 		return nil
	// 	}
	// }

	blog.Debugf("remote: try to ensure single file(%s) for work(%s) to server(%s), going to send this file",
		desc.FilePath, m.work.ID(), host.Server)
	req.Messages = m.fileMessageBank.get(desc)

	// 同步发送文件
	t := time.Now().Local()
	result, err := handler.ExecuteSendFile(host, req, sandbox)
	defer func() {
		status := types.FileSendSucceed
		if err != nil {
			status = types.FileSendFailed
		}
		m.updateSendFile(host.Server, desc, status)
	}()
	d := time.Now().Local().Sub(t)
	if d > 200*time.Millisecond {
		blog.Infof("remote: single file real sending file for work(%s) to server(%s): %s, %s",
			m.work.ID(), host.Server, d.String(), req.Files[0].FilePath)
	}

	if err != nil {
		blog.Errorf("remote: execute send file(%s) for work(%s) to server(%s) failed: %v",
			desc.FilePath, m.work.ID(), host.Server, err)
		return err
	}

	if retCode := result.Results[0].RetCode; retCode != 0 {
		return fmt.Errorf("remote: send files(%s) for work(%s) to server(%s) failed, got retCode %d",
			desc.FilePath, m.work.ID(), host.Server, retCode)
	}

	blog.Debugf("remote: success to execute send file(%s) for work(%s) to server(%s)",
		desc.FilePath, m.work.ID(), host.Server)
	return nil
}

// ensureSingleCorkFile 保证给到的第一个文件被正确分发到目标机器上, 若给到的文件多于一个, 多余的部分会被忽略
func (m *Mgr) ensureSingleCorkFile(c *corkFile, r matchResult) (err error) {
	status := r.info.SendStatus
	host := c.host
	desc := c.file

	blog.Debugf("remote: start ensure single cork file(%s) for work(%s) to server(%s)",
		desc.FilePath, m.work.ID(), host.Server)

	// 已经有人发送了文件, 等待文件就绪
	if r.match {
		blog.Debugf("remote: try to ensure single cork file(%s) for work(%s) to server(%s), "+
			"some one is sending this file", desc.FilePath, m.work.ID(), host.Server)
		tick := time.NewTicker(m.checkSendFileTick)
		defer tick.Stop()

		for status == types.FileSending {
			select {
			case <-tick.C:
				status, _ = m.checkOrLockSendFile(host.Server, *desc)
			}
		}

		switch status {
		case types.FileSendFailed:
			blog.Errorf("remote: end ensure single cork file(%s) for work(%s) to server(%s), "+
				"file already sent and failed", desc.FilePath, m.work.ID(), host.Server)
			return types.ErrSendFileFailed
		case types.FileSendSucceed:
			blog.Debugf("remote: end ensure single cork file(%s) for work(%s) to server(%s) succeed",
				desc.FilePath, m.work.ID(), host.Server)
			return nil
		default:
			blog.Errorf("remote: end ensure single cork file(%s) for work(%s) to server(%s), "+
				" with unknown status", desc.FilePath, m.work.ID(), host.Server)
			return fmt.Errorf("unknown cork file send status: %s", status.String())
		}
	}

	// send like tcp cork
	blog.Debugf("remote: start wait result for send single cork file(%s) for work(%s) to server(%s)",
		desc.FilePath, m.work.ID(), host.Server)
	retcode, err := m.waitCorkFileResult(c)
	// blog.Debugf("remote: end wait result for send single cork file(%s) for work(%s) to server(%s)",
	// 	desc.FilePath, m.work.ID(), host.Server)
	if err != nil {
		blog.Warnf("remote: end ensure single cork file(%s) for work(%s) to server(%s) failed: %v, retcode:%d",
			desc.FilePath, m.work.ID(), host.Server, err, retcode)
		return err
	} else if retcode != 0 {
		blog.Warnf("remote: end ensure single cork file(%s) for work(%s) to server(%s) failed: %v, retcode:%d",
			desc.FilePath, m.work.ID(), host.Server, err, retcode)
		return fmt.Errorf("remote: send cork files(%s) for work(%s) to server(%s) failed, got retCode %d",
			desc.FilePath, m.work.ID(), host.Server, retcode)
	} else {
		blog.Debugf("remote: end ensure single cork file(%s) for work(%s) to server(%s) succeed",
			desc.FilePath, m.work.ID(), host.Server)
		return nil
	}
}

func (m *Mgr) checkSingleCache(
	handler dcSDK.RemoteWorkerHandler,
	host *dcProtocol.Host,
	desc dcSDK.FileDesc,
	sandbox *dcSyscall.Sandbox) bool {
	if !workerSideCache(sandbox) {
		return false
	}

	blog.Debugf("remote: try to check cache for single file(%s) for work(%s) to server(%s)",
		desc.FilePath, m.work.ID(), host.Server)
	r, err := handler.ExecuteCheckCache(host, &dcSDK.BKDistFileSender{Files: []dcSDK.FileDesc{desc}}, sandbox)
	if err != nil {
		blog.Warnf("remote: try to check cache for single file(%s) for work(%s) to server(%s) failed: %v",
			desc.FilePath, m.work.ID(), host.Server, err)
		return false
	}

	if len(r) == 0 || !r[0] {
		blog.Debugf("remote: try to check cache for single file(%s) for work(%s) to server(%s) not hit cache",
			desc.FilePath, m.work.ID(), host.Server)
		return false
	}

	blog.Debugf("remote: success to check cache for single file(%s) for work(%s) to server(%s) and hit cache",
		desc.FilePath, m.work.ID(), host.Server)
	return true
}

func (m *Mgr) checkBatchCache(
	handler dcSDK.RemoteWorkerHandler,
	host *dcProtocol.Host,
	desc []dcSDK.FileDesc,
	sandbox *dcSyscall.Sandbox) []bool {
	r := make([]bool, 0, len(desc))
	for i := 0; i < len(desc); i++ {
		r = append(r, false)
	}

	if !workerSideCache(sandbox) {
		return r
	}

	blog.Debugf("remote: try to check cache for batch file for work(%s) to server(%s)", m.work.ID(), host.Server)
	result, err := handler.ExecuteCheckCache(host, &dcSDK.BKDistFileSender{Files: desc}, sandbox)
	if err != nil {
		blog.Warnf("remote: try to check cache for batch file for work(%s) to server(%s) failed: %v",
			m.work.ID(), host.Server, err)
		return r
	}

	blog.Debugf("remote: success to check cache for batch file for work(%s) to server(%s) and get result",
		m.work.ID(), host.Server)
	return result
}

// checkOrLockFile 检查目标file的sendStatus, 如果已经被发送, 则返回当前状态和true; 如果没有被发送过, 则将其置于sending, 并返回false
func (m *Mgr) checkOrLockSendFile(server string, desc dcSDK.FileDesc) (types.FileSendStatus, bool) {
	t1 := time.Now().Local()
	m.fileSendMutex.Lock()

	t2 := time.Now().Local()
	if d1 := t2.Sub(t1); d1 > 50*time.Millisecond {
		blog.Debugf("check cache lock wait too long server(%s): %s", server, d1.String())
	}

	defer func() {
		if d2 := time.Now().Local().Sub(t2); d2 > 50*time.Millisecond {
			blog.Debugf("check cache process wait too long server(%s): %s", server, d2.String())
		}
	}()

	target, ok := m.fileSendMap[server]
	if !ok {
		target = &fileSendMap{}
		m.fileSendMap[server] = target
	}
	m.fileSendMutex.Unlock()

	info, match := target.matchOrInsert(desc)
	return info.SendStatus, match
}

type matchResult struct {
	info  *types.FileInfo
	match bool
}

// checkOrLockCorkFiles 批量检查目标file的sendStatus, 如果已经被发送, 则返回当前状态和true; 如果没有被发送过, 则将其置于sending, 并返回false
func (m *Mgr) checkOrLockCorkFiles(server string, descs []*dcSDK.FileDesc) []matchResult {
	m.fileSendMutex.Lock()
	target, ok := m.fileSendMap[server]
	if !ok {
		target = &fileSendMap{}
		m.fileSendMap[server] = target
	}
	m.fileSendMutex.Unlock()

	return target.matchOrInserts(descs)
}

func (m *Mgr) updateSendFile(server string, desc dcSDK.FileDesc, status types.FileSendStatus) {
	m.fileSendMutex.Lock()
	target, ok := m.fileSendMap[server]
	if !ok {
		target = &fileSendMap{}
		m.fileSendMap[server] = target
	}
	m.fileSendMutex.Unlock()

	target.updateStatus(desc, status)
	return
}

func (m *Mgr) sendToolchain(handler dcSDK.RemoteWorkerHandler, req *types.RemoteTaskExecuteRequest) error {
	fileCollections := m.getToolChainFromExecuteRequest(req)
	if fileCollections != nil && len(fileCollections) > 0 {
		err := m.sendFileCollectionOnce(handler, req.Pid, req.Sandbox, req.Server, fileCollections)
		if err != nil {
			blog.Errorf("remote: execute remote task for work(%s) from pid(%d) to server(%s), "+
				"ensure tool chain files failed: %v", m.work.ID(), req.Pid, req.Server.Server, err)
			return err
		}

		// reset tool chain info if changed, then send again until old tool chain finished sending,
		// to avoid write same file on remote worker
		toolChainChanged, _ := m.isToolChainChanged(req, req.Server.Server)
		finished, _ := m.isToolChainFinished(req, req.Server.Server)
		for toolChainChanged || !finished {
			blog.Infof("remote: found tool chain changed, ready clear tool chain status")
			m.clearOldFileCollectionFromCache(req.Server.Server, fileCollections)
			fileCollections = m.getToolChainFromExecuteRequest(req)
			if fileCollections != nil && len(fileCollections) > 0 {
				blog.Infof("remote: found tool chain changed, send toolchain to server[%s] again",
					req.Server.Server)
				err = m.sendFileCollectionOnce(handler, req.Pid, req.Sandbox, req.Server, fileCollections)
				if err != nil {
					blog.Errorf("remote: execute remote task for work(%s) from pid(%d) to server(%s), "+
						"ensure tool chain files failed: %v", m.work.ID(), req.Pid, req.Server.Server, err)
					return err
				}
			}
			toolChainChanged, _ = m.isToolChainChanged(req, req.Server.Server)
			finished, _ = m.isToolChainFinished(req, req.Server.Server)
		}

		_ = m.updateToolChainPath(req)
	}

	return nil
}

func (m *Mgr) sendFileCollectionOnce(
	handler dcSDK.RemoteWorkerHandler,
	pid int,
	sandbox *dcSyscall.Sandbox,
	server *dcProtocol.Host,
	filecollections []*types.FileCollectionInfo) error {
	blog.Infof("remote: try to send %d file collection for work(%s) from pid(%d) dir(%s) to server",
		len(filecollections), m.work.ID(), pid, sandbox.Dir)

	var err error
	wg := make(chan error, len(filecollections)+1)
	count := 0
	for _, fc := range filecollections {
		count++
		go func(err chan<- error, host *dcProtocol.Host, filecollection *types.FileCollectionInfo) {
			err <- m.ensureOneFileCollection(handler, host, filecollection, sandbox)
		}(wg, server, fc)
	}

	for i := 0; i < count; i++ {
		if err = <-wg; err != nil {
			return err
		}
	}
	blog.Infof("remote: success to send %d file collection for work(%s) from pid(%d) to server",
		count, m.work.ID(), pid)

	return nil
}

// ensureOneFileCollection 保证给到的第一个文件集合被正确分发到目标机器上
func (m *Mgr) ensureOneFileCollection(
	handler dcSDK.RemoteWorkerHandler,
	host *dcProtocol.Host,
	fc *types.FileCollectionInfo,
	sandbox *dcSyscall.Sandbox) (err error) {
	blog.Infof("remote: try to ensure one file collection(%s) for work(%s) to server(%s)",
		fc.UniqID, m.work.ID(), host.Server)

	status, ok := m.checkOrLockFileCollection(host.Server, fc)

	// 已经有人发送了文件, 等待文件就绪
	if ok {
		blog.Infof("remote: try to ensure one file collection(%s) timestamp(%d) for work(%s) to server(%s), "+
			"dealing(dealed) by other", fc.UniqID, fc.Timestamp, m.work.ID(), host.Server)
		tick := time.NewTicker(m.checkSendFileTick)
		defer tick.Stop()

		for status == types.FileSending {
			select {
			case <-tick.C:
				status, _ = m.getCachedToolChainStatus(host.Server, fc.UniqID)
				// FileSendUnknown means this collection cleard from cache, just wait for sending again
				if status == types.FileSendUnknown {
					status = types.FileSending
				}
			}
		}

		switch status {
		case types.FileSendFailed:
			return types.ErrSendFileFailed
		case types.FileSendSucceed:
			blog.Infof("remote: success to ensure one file collection(%s) timestamp(%d) "+
				"for work(%s) to server(%s)", fc.UniqID, fc.Timestamp, m.work.ID(), host.Server)
			return nil
		default:
			return fmt.Errorf("unknown file send status: %s", status.String())
		}
	}

	needSentFiles := make([]dcSDK.FileDesc, 0, len(fc.Files))
	hit := 0
	for i, b := range m.checkBatchCache(handler, host, fc.Files, sandbox) {
		if b {
			hit++
			continue
		}

		needSentFiles = append(needSentFiles, fc.Files[i])
	}

	blog.Infof("remote: try to ensure one file collection(%s) timestamp(%d) filenum(%d) cache-hit(%d) "+
		"for work(%s) to server(%s), going to send this collection",
		fc.UniqID, fc.Timestamp, len(needSentFiles), hit, m.work.ID(), host.Server)
	req := &dcSDK.BKDistFileSender{Files: needSentFiles}
	if req.Messages, err = client.EncodeSendFileReq(req, sandbox); err != nil {
		return err
	}

	// 同步发送文件
	result, err := handler.ExecuteSendFile(host, req, sandbox)
	defer func() {
		status := types.FileSendSucceed
		if err != nil {
			status = types.FileSendFailed
		}
		m.updateFileCollectionStatus(host.Server, fc, status)
	}()

	if err != nil {
		blog.Errorf("remote: execute send file collection(%s) for work(%s) to server(%s) failed: %v",
			fc.UniqID, m.work.ID(), host.Server, err)
		return err
	}

	if retCode := result.Results[0].RetCode; retCode != 0 {
		return fmt.Errorf("remote: send files collection(%s) for work(%s) to server(%s) failed, got retCode %d",
			fc.UniqID, m.work.ID(), host.Server, retCode)
	}

	blog.Infof("remote: success to execute send file collection(%s) timestamp(%d) filenum(%d) "+
		"for work(%s) to server(%s)", fc.UniqID, fc.Timestamp, len(fc.Files), m.work.ID(), host.Server)
	return nil
}

// checkOrLockFileCollection 检查目标file collection的sendStatus, 如果已经被发送, 则返回当前状态和true; 如果没有被发送过,
// 则将其置于sending, 并返回false
func (m *Mgr) checkOrLockFileCollection(server string, fc *types.FileCollectionInfo) (types.FileSendStatus, bool) {
	m.fileCollectionSendMutex.Lock()
	defer m.fileCollectionSendMutex.Unlock()

	target, ok := m.fileCollectionSendMap[server]
	if !ok {
		filecollections := make([]*types.FileCollectionInfo, 0, 10)
		m.fileCollectionSendMap[server] = &filecollections
		target = m.fileCollectionSendMap[server]
	}

	for _, f := range *target {
		if f.UniqID == fc.UniqID {
			return f.SendStatus, true
		}
	}

	fc.SendStatus = types.FileSending
	*target = append(*target, fc)

	return types.FileSending, false
}

func (m *Mgr) updateFileCollectionStatus(server string, fc *types.FileCollectionInfo, status types.FileSendStatus) {
	m.fileCollectionSendMutex.Lock()
	defer m.fileCollectionSendMutex.Unlock()

	blog.Infof("remote: ready add collection(%s) server(%s) timestamp(%d) status(%d) to cache",
		fc.UniqID, server, fc.Timestamp, status)

	target, ok := m.fileCollectionSendMap[server]
	if !ok {
		filecollections := make([]*types.FileCollectionInfo, 0, 10)
		m.fileCollectionSendMap[server] = &filecollections
		target = m.fileCollectionSendMap[server]
	}

	for _, f := range *target {
		if f.UniqID == fc.UniqID {
			f.SendStatus = status
			return
		}
	}

	fc.SendStatus = status
	*target = append(*target, fc)
	blog.Infof("remote: finishend add collection(%s) server(%s) timestamp(%d) status(%d) to cache",
		fc.UniqID, server, fc.Timestamp, status)

	return
}

// to ensure clear only once
func (m *Mgr) clearOldFileCollectionFromCache(server string, fcs []*types.FileCollectionInfo) {
	m.fileCollectionSendMutex.Lock()
	defer m.fileCollectionSendMutex.Unlock()

	target, ok := m.fileCollectionSendMap[server]
	if !ok {
		return
	}

	i := 0
	needdelete := false
	for _, f := range *target {
		needdelete = false
		for _, fc := range fcs {
			if f.UniqID == fc.UniqID {
				timestamp, _ := m.work.Basic().GetToolChainTimestamp(f.UniqID)
				if f.Timestamp != timestamp {
					blog.Infof("remote: clear collection(%s) server(%s) timestamp(%d) "+
						"new timestamp(%d) from cache", f.UniqID, server, f.Timestamp, timestamp)
					needdelete = true
					break
				}
			}
		}
		// save values not delete
		if !needdelete {
			(*target)[i] = f
			i++
		}
	}

	// Prevent memory leak by erasing truncated values
	for j := i; j < len(*target); j++ {
		(*target)[j] = nil
	}
	*target = (*target)[:i]

	return
}

func (m *Mgr) getCachedToolChainTimestamp(server string, toolChainKey string) (int64, error) {
	m.fileCollectionSendMutex.RLock()
	defer m.fileCollectionSendMutex.RUnlock()

	target, ok := m.fileCollectionSendMap[server]
	if !ok {
		return 0, fmt.Errorf("toolchain [%s] not existed in cache", toolChainKey)
	}

	for _, f := range *target {
		if f.UniqID == toolChainKey {
			return f.Timestamp, nil
		}
	}

	return 0, fmt.Errorf("toolchain [%s] not existed in cache", toolChainKey)
}

func (m *Mgr) getCachedToolChainStatus(server string, toolChainKey string) (types.FileSendStatus, error) {
	m.fileCollectionSendMutex.RLock()
	defer m.fileCollectionSendMutex.RUnlock()

	target, ok := m.fileCollectionSendMap[server]
	if !ok {
		return types.FileSendUnknown, nil
	}

	for _, f := range *target {
		if f.UniqID == toolChainKey {
			return f.SendStatus, nil
		}
	}

	return types.FileSendUnknown, nil
}

func (m *Mgr) lockSlots(usage dcSDK.JobUsage, f string) *dcProtocol.Host {
	return m.resource.Lock(usage, f)
}

func (m *Mgr) unlockSlots(usage dcSDK.JobUsage, host *dcProtocol.Host) {
	m.resource.Unlock(usage, host)
}

// TotalSlots return available total slots
func (m *Mgr) TotalSlots() int {
	return m.resource.TotalSlots()
}

func (m *Mgr) getRemoteFileBaseDir() string {
	return fmt.Sprintf("common_%s", m.work.ID())
}

func (m *Mgr) syncHostTimeNoWait(hostList []*dcProtocol.Host) []*dcProtocol.Host {
	go m.syncHostTime(hostList)
	return hostList
}

func (m *Mgr) syncHostTime(hostList []*dcProtocol.Host) []*dcProtocol.Host {
	blog.Infof("remote: try to sync time for hosts list")

	handler := m.remoteWorker.Handler(0, nil, nil, nil)
	counter := make(map[string]int64, 50)
	div := make(map[string]int64, 50)
	var lock sync.Mutex

	var wg sync.WaitGroup
	for _, host := range hostList {
		ip := getIPFromServer(host.Server)
		if _, ok := counter[ip]; ok {
			continue
		}

		counter[ip] = 0
		div[ip] = 0

		for i := 0; i < syncHostTimeTimes; i++ {
			wg.Add(1)
			go func(h *dcProtocol.Host) {
				defer wg.Done()

				t1 := time.Now().Local().UnixNano()
				remoteTime, err := handler.ExecuteSyncTime(h.Server)
				if err != nil {
					blog.Warnf("remote: try to sync time for host(%s) failed: %v", h.Server, err)
					return
				}
				t2 := time.Now().Local().UnixNano()

				deltaTime := remoteTime - (t1+t2)/2
				lock.Lock()
				counter[getIPFromServer(h.Server)] += deltaTime
				div[ip]++
				lock.Unlock()
				blog.Debugf("remote: success to sync time from host(%s), get delta time: %d",
					h.Server, deltaTime)
			}(host)
		}
	}
	wg.Wait()

	for _, host := range hostList {
		ip := getIPFromServer(host.Server)
		if _, ok := counter[ip]; !ok {
			continue
		}

		if div[ip] <= 0 {
			continue
		}
		host.TimeDelta = counter[ip] / div[ip]
		blog.Infof("remote: success to sync time for host(%s), set delta time: %d", host.Server, host.TimeDelta)
	}

	blog.Infof("remote: success to sync time for hosts list")
	return hostList
}

func (m *Mgr) getToolChainFromExecuteRequest(req *types.RemoteTaskExecuteRequest) []*types.FileCollectionInfo {
	blog.Debugf("remote: get toolchain with req:[%+v]", *req)
	fd := make([]*types.FileCollectionInfo, 0, 2)
	for _, c := range req.Req.Commands {
		blog.Debugf("remote: ready get toolchain with key:[%s]", c.ExeToolChainKey)
		if c.ExeToolChainKey != "" {
			toolchainfiles, timestamp, err := m.work.Basic().GetToolChainFiles(c.ExeToolChainKey)
			if err == nil && len(toolchainfiles) > 0 {
				fd = append(fd, &types.FileCollectionInfo{
					UniqID:     c.ExeToolChainKey,
					Files:      toolchainfiles,
					SendStatus: types.FileSending,
					Timestamp:  timestamp,
				})
			}
		}
	}
	return fd
}

func (m *Mgr) isToolChainChanged(req *types.RemoteTaskExecuteRequest, server string) (bool, error) {
	blog.Debugf("remote: check tool chain changed with req:[%+v]", *req)

	for _, c := range req.Req.Commands {
		blog.Debugf("remote: ready check toolchain changed with key:[%s]", c.ExeToolChainKey)
		if c.ExeToolChainKey != "" {
			timestamp, _ := m.work.Basic().GetToolChainTimestamp(c.ExeToolChainKey)
			timestampcached, _ := m.getCachedToolChainTimestamp(server, c.ExeToolChainKey)
			if timestamp != timestampcached {
				blog.Infof("remote: found collection(%s) server(%s) cached timestamp(%d) "+
					"newly timestamp(%d) changed", c.ExeToolChainKey, server, timestampcached, timestamp)
				return true, nil
			}
		}
	}

	return false, nil
}

func (m *Mgr) isToolChainFinished(req *types.RemoteTaskExecuteRequest, server string) (bool, error) {
	blog.Debugf("remote: check tool chain finished with req:[%+v]", *req)

	allfinished := true
	for _, c := range req.Req.Commands {
		blog.Debugf("remote: ready check toolchain finished with key:[%s]", c.ExeToolChainKey)
		if c.ExeToolChainKey != "" {
			status, _ := m.getCachedToolChainStatus(server, c.ExeToolChainKey)
			if status != types.FileSendSucceed && status != types.FileSendFailed {
				blog.Infof("remote: found collection(%s) server(%s) status(%d) not finished",
					c.ExeToolChainKey, server, status)
				allfinished = false
				return allfinished, nil
			}
		}
	}

	return allfinished, nil
}

func (m *Mgr) updateToolChainPath(req *types.RemoteTaskExecuteRequest) error {
	for i, c := range req.Req.Commands {
		if c.ExeToolChainKey != "" {
			remotepath, err := m.work.Basic().GetToolChainRemotePath(c.ExeToolChainKey)
			if err != nil {
				return fmt.Errorf("not found remote path for toolchain %s", c.ExeToolChainKey)
			}
			blog.Debugf("remote: before update toolchain with key:[%s],remotepath:[%s],inputfiles:%+v",
				c.ExeToolChainKey, remotepath, req.Req.Commands[i].Inputfiles)
			req.Req.Commands[i].Inputfiles = append(req.Req.Commands[i].Inputfiles, dcSDK.FileDesc{
				FilePath:           c.ExeName,
				Compresstype:       protocol.CompressLZ4,
				FileSize:           -1,
				Lastmodifytime:     0,
				Md5:                "",
				Targetrelativepath: remotepath,
			})
			blog.Debugf("remote: after update toolchain with key:[%s],remotepath:[%s],inputfiles:%+v",
				c.ExeToolChainKey, remotepath, req.Req.Commands[i].Inputfiles)
		}
	}
	return nil
}

type corkFileResult struct {
	retcode int32
	err     error
}

type corkFile struct {
	handler    dcSDK.RemoteWorkerHandler
	host       *dcProtocol.Host
	sandbox    *dcSyscall.Sandbox
	file       *dcSDK.FileDesc
	resultchan chan corkFileResult
}

func (m *Mgr) sendFileWithCork(handler dcSDK.RemoteWorkerHandler,
	f *dcSDK.FileDesc,
	host *dcProtocol.Host,
	sandbox *dcSyscall.Sandbox) (int32, error) {
	cf := corkFile{
		handler:    handler,
		host:       host,
		sandbox:    sandbox,
		file:       f,
		resultchan: make(chan corkFileResult, 1),
	}

	// append to file queue
	m.corkMutex.Lock()
	if l, ok := m.corkFiles[host.Server]; ok {
		*l = append(*l, &cf)
	} else {
		newl := []*corkFile{&cf}
		m.corkFiles[host.Server] = &newl
	}
	m.corkMutex.Unlock()

	// notify send
	m.sendCorkChan <- true

	// wait for result
	msg := <-cf.resultchan
	return msg.retcode, msg.err
}

func (m *Mgr) appendCorkFiles(server string, cfs []*corkFile) error {
	// append to file queue
	m.corkMutex.Lock()
	if l, ok := m.corkFiles[server]; ok {
		*l = append(*l, cfs...)
	} else {
		// 队列分配大点，避免频繁分配
		newlen := len(cfs) * 10
		newl := make([]*corkFile, 0, newlen)
		newl = append(newl, cfs...)
		m.corkFiles[server] = &newl
	}
	m.corkMutex.Unlock()

	// for _, v := range cfs {
	// 	blog.Infof("remote: appended cork file[%s] ", v.file.FilePath)
	// }

	// notify send
	m.sendCorkChan <- true

	return nil
}

// wait for result
func (m *Mgr) waitCorkFileResult(cf *corkFile) (int32, error) {
	msg := <-cf.resultchan
	return msg.retcode, msg.err
}

func (m *Mgr) getCorkFiles(sendanyway bool) []*[]*corkFile {
	m.corkMutex.Lock()
	defer m.corkMutex.Unlock()

	result := make([]*[]*corkFile, 0)
	for _, v := range m.corkFiles {
		// srcfiles := *v
		var totalsize int64
		index := -1
		if sendanyway {
			// index = len(*v) - 1
			for index = range *v {
				totalsize += (*v)[index].file.FileSize
				// 如果数据包超过 m.corkMaxSize 了，则停止获取，下次再发
				if totalsize > m.corkMaxSize {
					break
				}
			}
		} else {
			for index = range *v {
				totalsize += (*v)[index].file.FileSize
				// 如果数据包超过 m.corkSize 了，则停止获取，下次再发
				if totalsize > m.corkSize {
					break
				}
			}
		}

		if index >= 0 {
			if sendanyway || (!sendanyway && totalsize > m.corkSize) {
				// get files
				num := index + 1
				start := 0
				dstfiles := make([]*corkFile, num)
				copy(dstfiles, (*v)[start:start+num])

				// remove files
				if num < len(*v) {
					*v = (*v)[start+num:]
				} else {
					*v = make([]*corkFile, 0)
				}

				result = append(result, &dstfiles)
			}
		}
	}

	// for _, sv := range result {
	// 	for _, v := range *sv {
	// 		blog.Infof("remote: selected cork file[%s] ", v.file.FilePath)
	// 	}
	// }

	return result
}

func (m *Mgr) sendFilesWithCorkTick(ctx context.Context) {
	blog.Infof("remote: start send files with cork tick for work: %s", m.work.ID())
	ticker := time.NewTicker(m.sendCorkTick)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			blog.Infof("remote: run send files with cork for work(%s) canceled by context", m.work.ID())
			return

		case <-ticker.C:
			// check whether have files to send
			files := m.getCorkFiles(true)
			if len(files) > 0 {
				m.sendFilesWithCork(files)
			}
		case <-m.sendCorkChan:
			// check whether enought files to send
			files := m.getCorkFiles(false)
			if len(files) > 0 {
				m.sendFilesWithCork(files)
			}
		}
	}
}

func (m *Mgr) sendFilesWithCork(files []*[]*corkFile) {
	for _, v := range files {
		if v != nil && len(*v) > 0 {
			go m.sendFilesWithCorkSameHost(*v)
		}
	}
}

func (m *Mgr) sendFilesWithCorkSameHost(files []*corkFile) {

	var totalsize int64
	req := &dcSDK.BKDistFileSender{}
	req.Files = make([]dcSDK.FileDesc, 0, len(files))
	for _, v := range files {
		req.Files = append(req.Files, *v.file)
		totalsize += v.file.FileSize
	}
	host := files[0].host
	sandbox := files[0].sandbox
	handler := files[0].handler

	blog.Infof("remote: start send cork %d files with size:%d to server %s for work: %s",
		len(files), totalsize, host.Server, m.work.ID())

	// in queue to limit total size of sending files, maybe we should deal if failed
	// blog.Infof("remote: try to get memory lock with file size:%d", totalsize)
	if m.memSlot.Lock(totalsize) {
		// blog.Infof("remote: succeed to get memory lock with file size:%d", totalsize)
		defer func() {
			m.memSlot.Unlock(totalsize)
			// total, occu := m.memSlot.GetStatus()
			// blog.Infof("remote: succeed to free memory lock with file size:%d,occupy:%d, total:%d", totalsize, occu, total)
		}()
	} else {
		blog.Infof("remote: failed to get memory lock with file size:%d", totalsize)
	}

	// add retry here
	var result *dcSDK.BKSendFileResult
	waitsecs := 5
	var err error
	for i := 0; i < 4; i++ {
		result, err = handler.ExecuteSendFile(host, req, sandbox)
		if err == nil {
			break
		} else {
			blog.Warnf("remote: failed to send cork %d files with size:%d to server:%s for the %dth times with error:%v",
				len(files), totalsize, host.Server, i, err)
			time.Sleep(time.Duration(waitsecs) * time.Second)
			waitsecs = waitsecs * 2
		}
	}

	// free memory anyway after sent file
	debug.FreeOSMemory()

	status := types.FileSendSucceed
	if err != nil {
		status = types.FileSendFailed
	}

	var retcode int32
	if err != nil {
		retcode = -1
	} else {
		retcode = result.Results[0].RetCode
	}
	resultchan := corkFileResult{
		retcode: retcode,
		err:     err,
	}

	for _, v := range files {
		m.updateSendFile(host.Server, *v.file, status)
		blog.Debugf("remote: ready ensure single cork file(%s) to status(%s) for work(%s) to server(%s)",
			v.file.FilePath, status, m.work.ID(), host.Server)
		v.resultchan <- resultchan
		// blog.Infof("remote: end send file[%s] with cork to server %s tick for work: %s with err:%v, retcode:%d",
		// 	v.file.FilePath, host.Server, m.work.ID(), err, retcode)
	}

	blog.Infof("remote: end send %d files with cork to server %s tick for work: %s with err:%v, retcode:%d",
		len(files), host.Server, m.work.ID(), err, retcode)
}
