package distcc

import (
	"fmt"
	"regexp"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpclient"
	ds "github.com/Tencent/bk-ci/src/booster/common/store/distcc_server"
	"github.com/Tencent/bk-ci/src/booster/server/config"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/rd"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/resource/crm"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/resource/crm/operator"

	"github.com/jinzhu/gorm"
)

const (
	// EngineName define the engine name
	EngineName = "distcc"

	envAllow     = "BK_DISTCC_ALLOW"
	envJob       = "BK_DISTCC_JOBS"
	portsService = "SERVICE_PORT"
	portsStats   = "STATS_PORT"

	daemonStatsURL = "http://%s:%d/"
)

// EngineConfig
type EngineConfig struct {
	engine.MySQLConf
	Rd rd.RegisterDiscover

	ClusterID              string
	CPUPerInstance         float64
	MemPerInstance         float64
	LeastJobServer         int
	JobServerTimesToCPU    float64
	Brokers                []config.EngineDistCCBrokerConfig
	QueueResourceAllocater map[string]config.ResourceAllocater
}

const distCCRunningLongestTime = 6 * time.Hour

var preferences = engine.Preferences{
	HeartbeatTimeoutTickTimes: 12,
}

// NewDistccEngine get a new distcc engine
// EngineConfig:   describe the basic config of engine including mysql config
// HandleWithUser: registered from a container resource manager, used to handle the resources and launch tasks.
func NewDistccEngine(conf EngineConfig, mgr crm.HandlerWithUser) (engine.Engine, error) {
	m, err := NewMySQL(conf.MySQLConf)
	if err != nil {
		blog.Errorf("engine(%s) get new mysql(%+v) failed: %v", EngineName, conf.MySQLConf, err)
		return nil, err
	}

	egn := &distccEngine{
		conf:        conf,
		publicQueue: engine.NewStagingTaskQueue(),
		mysql:       m,
		mgr:         mgr,
	}
	if err = egn.initBrokers(); err != nil {
		blog.Errorf("engine(%s) init brokers failed: %v", EngineName, err)
		return nil, err
	}

	_ = egn.parseAllocateConf()

	return egn, nil
}

type distccEngine struct {
	conf        EngineConfig
	publicQueue engine.StagingTaskQueue
	mysql       MySQL
	mgr         crm.HandlerWithUser
}

// Name get the engine name
func (de *distccEngine) Name() engine.TypeName {
	return EngineName
}

// SelectFirstTaskBasic select a task from given task queue group and the request is ask from the specific queue.
// commonly select a task from the queue of the given name
// if the queue if empty, then try get task from the public queue, the task is from other busy queues.
func (de *distccEngine) SelectFirstTaskBasic(tqg *engine.TaskQueueGroup, queueName string) (*engine.TaskBasic, error) {
	tb, err := tqg.GetQueue(queueName).First()
	if err == engine.ErrorNoTaskInQueue {

		// get first valid task from public queue
		// if task not exist in queue group, just delete it from public queue.
		for {
			tb, err = de.publicQueue.First()
			if err == engine.ErrorNoTaskInQueue {
				return nil, err
			}

			if tqg.Exist(tb.ID) {
				break
			}

			_ = de.publicQueue.Delete(tb.ID)
		}
	}

	if err != nil {
		blog.Errorf("engine(%s) get first from queue(%s) failed: %v", EngineName, queueName, err)
		return nil, err
	}

	return tb, nil
}

// CreateTaskExtension create task extension with extra data into db
func (de *distccEngine) CreateTaskExtension(tb *engine.TaskBasic, extra []byte) error {
	return de.createTask(tb, extra)
}

// GetTaskExtension get task extension from db
func (de *distccEngine) GetTaskExtension(taskID string) (engine.TaskExtension, error) {
	return de.getTask(taskID)
}

// LaunchTask try launching task from queue.
func (de *distccEngine) LaunchTask(tb *engine.TaskBasic, queueName string) error {
	return de.launchTask(tb, queueName)
}

// DegradeTask degrade to local compiling, keep running without any workers and set the correct client commands.
func (de *distccEngine) DegradeTask(taskID string) error {
	return de.degradeTask(taskID)
}

// LaunchDone check if the launch is done
func (de *distccEngine) LaunchDone(taskID string) (bool, error) {
	return de.launchDone(taskID)
}

// CheckTask check task when running, failed with error, such as running timeout.
func (de *distccEngine) CheckTask(tb *engine.TaskBasic) error {
	return de.checkTask(tb)
}

// SendProjectMessage send project message usually used to handle the cmake args.
func (de *distccEngine) SendProjectMessage(projectID string, extra []byte) ([]byte, error) {
	return de.sendProjectMessage(projectID, extra)
}

// SendTaskMessage send task message, record the stats from client's report.
func (de *distccEngine) SendTaskMessage(taskID string, extra []byte) ([]byte, error) {
	return de.sendTaskMessage(taskID, extra)
}

// CollectTaskData collect the task data and the distcc daemon stats, record the stats from server side.
func (de *distccEngine) CollectTaskData(tb *engine.TaskBasic) error {
	return de.collectTaskData(tb)
}

// ReleaseTask release task, shut down workers and free the resources.
func (de *distccEngine) ReleaseTask(taskID string) error {
	return de.releaseTask(taskID)
}

// GetPreferences return the preferences
func (de *distccEngine) GetPreferences() engine.Preferences {
	return preferences
}

// GetTaskBasicTable get task basic table db operator
func (de *distccEngine) GetTaskBasicTable() *gorm.DB {
	return de.mysql.GetDB().Table(TableTask{}.TableName())
}

// GetProjectBasicTable get project basic table db operator
func (de *distccEngine) GetProjectBasicTable() *gorm.DB {
	return de.mysql.GetDB().Table(TableProjectSetting{}.TableName())
}

// GetProjectInfoBasicTable get project info basic table db operator
func (de *distccEngine) GetProjectInfoBasicTable() *gorm.DB {
	return de.mysql.GetDB().Table(TableProjectInfo{}.TableName())
}

// GetWhitelistBasicTable get whitelist basic table db operator
func (de *distccEngine) GetWhitelistBasicTable() *gorm.DB {
	return de.mysql.GetDB().Table(TableWhitelist{}.TableName())
}

func (de *distccEngine) getClient(timeoutSecond int) *httpclient.HTTPClient {
	client := httpclient.NewHTTPClient()
	client.SetTimeOut(time.Duration(timeoutSecond) * time.Second)
	return client
}

// cheack AllocateMap map ,make sure the key is the format of xx:xx:xx-xx:xx:xx and smaller time is ahead
func (de *distccEngine) parseAllocateConf() bool {
	time_fmt := regexp.MustCompile(`[0-9]+[0-9]+:+[0-9]+[0-9]+:+[0-9]+[0-9]`)

	for queue, allocater := range de.conf.QueueResourceAllocater {
		for k, v := range allocater.AllocateByTimeMap {
			mid := strings.Index(k, "-")
			if mid == -1 || !time_fmt.MatchString(k[:mid]) || !time_fmt.MatchString(k[mid+1:]) || k[:mid] > k[:mid+1] {
				blog.Errorf("wrong time format:(%s) in [%s] allocate config, expect format like 12:30:00-14:00:00,smaller time ahead", k, queue)
				return false
			}
			allocater.TimeSlot = append(allocater.TimeSlot, config.TimeSlot{
				StartTime: k[:mid],
				EndTime:   k[mid+1:],
				Value:     v,
			},
			)
		}
		de.conf.QueueResourceAllocater[queue] = allocater
	}
	return true
}

func (de *distccEngine) allocate(queueName string) float64 {
	return de.allocateByCurrentTime(queueName)
}

func (de *distccEngine) allocateByCurrentTime(queueName string) float64 {
	if allocater, ok := de.conf.QueueResourceAllocater[queueName]; !ok {
		return 1.0
	} else {
		now := time.Now().Format("15:04:05")
		for _, slot := range allocater.TimeSlot {
			if now >= slot.StartTime && now < slot.EndTime {
				return slot.Value
			}
		}
	}
	return 1.0
}

func (de *distccEngine) createTask(tb *engine.TaskBasic, extra []byte) error {
	project, err := de.mysql.GetProjectSetting(tb.Client.ProjectID)
	if err != nil {
		blog.Errorf("engine(%s) try creating task(%s), get project setting(%s) failed: %v",
			EngineName, tb.ID, tb.Client.ProjectID, err)
		return err
	}

	var ev ExtraData
	if err = codec.DecJSON(extra, &ev); err != nil {
		blog.Errorf("engine(%s) try creating task(%s), decode extra(%s) failed: %v",
			EngineName, tb.ID, string(extra), err)
		return err
	}

	gccVersion := project.GccVersion
	if ev.GccVersion != "" {
		gccVersion = ev.GccVersion
	}
	gcc, err := de.mysql.GetGcc(gccVersion)
	if err != nil {
		blog.Errorf("engine(%s) try create task(%s), get gcc(%s) failed: %v", EngineName, tb.ID, gccVersion, err)
		return err
	}

	task, err := de.getTask(tb.ID)
	if err != nil {
		blog.Errorf("engine(%s) try creating task(%s), get task failed: %v", EngineName, tb.ID, err)
		return err
	}

	task.Client.City = tb.Client.QueueName
	task.Client.GccVersion = gcc.GccVersion
	task.Client.SourceIP = tb.Client.ClientIP
	task.Client.SourceCPU = tb.Client.ClientCPU
	task.Client.User = ev.User
	task.Client.Params = ev.Params
	task.Client.CCacheEnabled = project.CCacheEnabled
	if ev.CCacheEnabled != nil {
		task.Client.CCacheEnabled = *ev.CCacheEnabled
	}
	task.Client.BanDistCC = project.BanDistCC
	task.Client.BanAllBooster = project.BanAllBooster
	task.Client.RunDir = ev.RunDir
	task.Client.CommandType = ev.CommandType
	task.Client.Command = ev.Command
	task.Client.Extra.BazelRC = ev.ExtraVars.BazelRC
	task.Client.Extra.MaxJobs = ev.ExtraVars.MaxJobs
	// if ban resources, then request and least cpu is 0
	if !task.Client.BanAllBooster && !task.Client.BanDistCC {
		task.Client.RequestCPU = project.RequestCPU * de.allocate(tb.Client.QueueName)
		blog.Info("distcc: queue:[%s] project:[%s] request cpu: [%f],actual request cpu:[%f]",
			tb.Client.QueueName,
			project.ProjectID,
			project.RequestCPU,
			task.Client.RequestCPU)

		task.Client.LeastCPU = project.LeastCPU
		if task.Client.LeastCPU > task.Client.RequestCPU {
			task.Client.LeastCPU = task.Client.RequestCPU
		}
	}
	task.Operator.ClusterID = de.conf.ClusterID
	task.Operator.AppName = task.ID
	task.Operator.Namespace = EngineName
	task.Operator.Image = gcc.Image
	// if ban resources, then request and least instance is 0
	if !task.Client.BanAllBooster && !task.Client.BanDistCC {
		task.Operator.RequestInstance = (int(task.Client.RequestCPU) + int(de.conf.CPUPerInstance) - 1) /
			int(de.conf.CPUPerInstance)
		task.Operator.LeastInstance = (int(task.Client.LeastCPU) + int(de.conf.CPUPerInstance) - 1) /
			int(de.conf.CPUPerInstance)
	}
	task.Operator.RequestCPUPerUnit = de.conf.CPUPerInstance
	task.Operator.RequestMemPerUnit = de.conf.MemPerInstance

	if err = de.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try creating task, update task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}
	blog.Infof("engine(%s) success to create task(%s)", EngineName, tb.ID)
	return nil
}

// ExtraData describe the data in task creation from client.
type ExtraData struct {
	User          string `json:"user"`
	GccVersion    string `json:"gcc_version"`
	RunDir        string `json:"run_dir"`
	Params        string `json:"params"` //自定义参数
	CCacheEnabled *bool  `json:"ccache_enabled"`

	// command define the target to be called, such as make, bazel, /data/custom/make etc.
	Command     string      `json:"command,omitempty"`
	CommandType CommandType `json:"command_type,omitempty"`

	// extra_vars includes the extra params need by client
	ExtraVars ExtraVars `json:"extra_vars,omitempty"`
}

// ExtraVars describe the extra params in ExtraData
type ExtraVars struct {
	// bazelrc define the bazelrc file path
	BazelRC string `json:"bazelrc"`
	MaxJobs int    `json:"max_jobs,omitempty"`
}

func (de *distccEngine) getTask(taskID string) (*distccTask, error) {
	t, err := de.mysql.GetTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) get task(%s) failed: %v", EngineName, taskID, err)
		return nil, err
	}

	return table2Task(t), nil
}

func (de *distccEngine) updateTask(task *distccTask) error {
	data, err := engine.GetMapExcludeTableTaskBasic(task2Table(task))
	if err != nil {
		blog.Errorf("engine(%s) update task(%s), get exclude map failed: %v", EngineName, task.ID, err)
		return err
	}

	if err = de.mysql.UpdateTask(task.ID, data); err != nil {
		blog.Errorf("engine(%s) update task(%s), update failed: %v", EngineName, task.ID, err)
		return err
	}

	return nil
}

func (de *distccEngine) launchTask(tb *engine.TaskBasic, city string) error {
	task, err := de.getTask(tb.ID)
	if err != nil {
		blog.Errorf("engine(%s) try launching task, get task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}

	if task.Client.BanAllBooster || task.Client.BanDistCC {
		blog.Infof("engine(%s) launch task(%s) no use resource for banning", EngineName, tb.ID)
		return nil
	}

	// init resource conditions
	task.Client.City = city
	if err = de.mgr.Init(tb.ID, crm.ResourceParam{
		City:     task.Client.City,
		Platform: "linux",
		Env: map[string]string{
			envAllow: strings.Join(append(de.getServerIPList(), task.Client.SourceIP), ","),
			envJob:   strconv.Itoa(int(task.Operator.RequestCPUPerUnit)),
		},
		Ports: map[string]string{
			portsService: "http",
			portsStats:   "http",
		},
		Image: task.Operator.Image,
	}); err != nil && err != crm.ErrorResourceAlreadyInit {
		blog.Errorf("engine(%s) try launching task(%s), init resource manager failed: %v",
			EngineName, tb.ID, err)
		return err
	}

	err = de.mgr.Launch(tb.ID, city, func(availableInstance int) (int, error) {
		if availableInstance < task.Operator.LeastInstance {
			return 0, engine.ErrorNoEnoughResources
		}

		if availableInstance > task.Operator.RequestInstance {
			availableInstance = task.Operator.RequestInstance
		}

		task.Operator.Instance = availableInstance
		return availableInstance, nil
	})
	// add task into public queue
	if err == engine.ErrorNoEnoughResources {
		de.publicQueue.Add(tb)
		return err
	}
	if err != nil {
		blog.Errorf("engine(%s) launch task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}

	if err = de.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try launching task, update task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}
	blog.Infof("engine(%s) success to launch task(%s)", EngineName, tb.ID)
	return nil
}

func (de *distccEngine) degradeTask(taskID string) error {
	task, err := de.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try degrading task, get task(%s) failed: %v", EngineName, taskID, err)
		return err
	}

	cs := CommandSetting{
		CommandType:     task.Client.CommandType,
		Command:         task.Client.Command,
		CompilerVersion: task.Client.GccVersion,
		CPUNum:          task.Client.SourceCPU,
		CustomParam:     task.Client.Params,
		CCacheEnable:    task.Client.CCacheEnabled,
		BanDistCC:       true,
		BanAllBooster:   task.Client.BanAllBooster,
		Extra:           task.Client.Extra,
	}
	task.Client.Cmd = cs.GetCommand()
	task.Client.Extra.CCCompiler, task.Client.Extra.CXXCompiler = cs.GetCompiler()

	if err = de.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try degrading task, update task(%s) failed: %v", EngineName, taskID, err)
		return err
	}
	blog.Infof("engine(%s) success to degrade task(%s)", EngineName, taskID)
	return nil
}

func (de *distccEngine) launchDone(taskID string) (bool, error) {
	task, err := de.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try checking service info, get task(%s) failed: %v", EngineName, taskID, err)
		return false, err
	}

	info := &operator.ServiceInfo{}
	if !task.Client.BanAllBooster && !task.Client.BanDistCC {
		// if still preparing, then it's not need to get service info
		isPreparing, err := de.mgr.IsServicePreparing(taskID)
		if err != nil {
			blog.Errorf("engine(%s) try checking service info, check if service preparing(%s) failed: %v",
				EngineName, taskID, err)
			return false, err
		}
		if isPreparing {
			return false, nil
		}

		info, err = de.mgr.GetServiceInfo(taskID)
		if err != nil {
			blog.Errorf("engine(%s) try checking service info, get info(%s) failed: %v", EngineName, taskID, err)
			return false, err
		}

		if info.Status == crm.ServiceStatusStaging {
			return false, nil
		}
	}

	compilerList := make([]taskCompiler, 0, 100)
	for _, endpoints := range info.AvailableEndpoints {
		servicePort, _ := endpoints.Ports[portsService]
		statsPort, _ := endpoints.Ports[portsStats]

		compilerList = append(compilerList, taskCompiler{
			CPU:       task.Operator.RequestCPUPerUnit,
			Mem:       task.Operator.RequestMemPerUnit,
			IP:        endpoints.IP,
			Port:      servicePort,
			StatsPort: statsPort,
		})
	}

	task.Compilers = compilerList
	task.Stats.CompilerCount = len(task.Compilers)
	task.Client.Env = map[string]string{
		envDistCCHostKey: getDistCCHostEnv(task.Operator.RequestCPUPerUnit, compilerList),
	}

	cs := CommandSetting{
		CommandType:     task.Client.CommandType,
		Command:         task.Client.Command,
		CompilerVersion: task.Client.GccVersion,
		CPUNum:          int(task.Operator.RequestCPUPerUnit) * task.Stats.CompilerCount,
		CustomParam:     task.Client.Params,
		CCacheEnable:    task.Client.CCacheEnabled,
		BanDistCC:       task.Client.BanDistCC,
		BanAllBooster:   task.Client.BanAllBooster,
		LeastJobServer:  de.conf.LeastJobServer,
		Extra:           task.Client.Extra,
	}

	task.Client.Cmd = cs.GetCommand()
	task.Client.Extra.CCCompiler, task.Client.Extra.CXXCompiler = cs.GetCompiler()
	task.Stats.CPUTotal = float64(task.Stats.CompilerCount) * task.Operator.RequestCPUPerUnit
	task.Stats.MemTotal = float64(task.Stats.CompilerCount) * task.Operator.RequestMemPerUnit

	if err = de.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try checking service info, update task(%s) failed: %v", EngineName, taskID, err)
		return false, err
	}
	blog.Infof("engine(%s) success to checking service info, task(%s) launching done", EngineName, taskID)
	return true, nil
}

func (de *distccEngine) collectTaskData(tb *engine.TaskBasic) error {
	task, err := de.getTask(tb.ID)
	if err != nil {
		blog.Errorf("engine(%s) try collecting task data, get task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}

	// collect task data from distcc daemon stats
	var wg sync.WaitGroup
	var wlock sync.Mutex
	for i, c := range task.Compilers {
		wg.Add(1)
		func(index int, compiler taskCompiler) {
			defer wg.Done()

			uri := fmt.Sprintf(daemonStatsURL, compiler.IP, compiler.StatsPort)
			blog.Infof("collect task ID(%s) stats info: %s", task.ID, uri)

			stats, err := de.collectStatsInfo(uri)

			// lock for stats record
			wlock.Lock()
			defer wlock.Unlock()

			if err != nil {
				blog.Warnf("collect task ID(%s) stats failed url(%s): %v", task.ID, uri, err)
				task.Compilers[index].Message = err.Error()
			}
			task.Compilers[index].Stats = stats

			if stats != nil {
				task.Stats.CompileFilesOK += stats.CompileOK
				task.Stats.CompileFilesErr += stats.CompileErr
				task.Stats.CompileFilesTimeout += stats.CompileTimeout
			}
		}(i, c)
	}
	wg.Wait()

	if err = de.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try collecting task data, update task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}

	if err = de.mysql.AddProjectInfoStats(tb.Client.ProjectID, DeltaInfoStats{
		CompileFilesOK:      task.Stats.CompileFilesOK,
		CompileFilesErr:     task.Stats.CompileFilesErr,
		CompileFilesTimeout: task.Stats.CompileFilesTimeout,
		CompileUnits:        float64(tb.Status.EndTime.Unix()-tb.Status.StartTime.Unix()) * task.Stats.CPUTotal,
	}); err != nil {
		blog.Errorf("engine(%s) try collecting task(%s) data, add project(%s) info stats failed: %v",
			EngineName, tb.ID, tb.Client.ProjectID, err)
		return err
	}

	blog.Infof("engine(%s) success to collect task(%s) data", EngineName, tb.ID)
	return nil
}

func (de *distccEngine) collectStatsInfo(uri string) (*ds.StatsInfo, error) {
	data, err := de.getClient(1).GET(uri, nil, nil)
	if err != nil {
		blog.Warnf("failed to fetch stats %s: %v", uri, err)
		return nil, err
	}

	lines := strings.Split(string(data), "\n")
	stats := new(ds.StatsInfo)
	for _, line := range lines {
		stats.Parse(line)
	}
	return stats, nil
}

func (de *distccEngine) releaseTask(taskID string) error {
	blog.Infof("engine(%s) try to release task(%s)", EngineName, taskID)
	if err := de.mgr.Release(taskID); err != nil && err != crm.ErrorResourceNoExist {
		blog.Errorf("engine(%s) try releasing task, release task(%s) failed: %v", EngineName, taskID, err)
		return err
	}

	blog.Infof("engine(%s) success to release task(%s)", EngineName, taskID)
	return nil
}

func (de *distccEngine) checkTask(tb *engine.TaskBasic) error {
	if tb.Status.Status == engine.TaskStatusRunning &&
		tb.Status.StartTime.Add(distCCRunningLongestTime).Before(time.Now().Local()) {
		return fmt.Errorf("distcc running since(%s) reaches the timeout(%s)",
			tb.Status.StartTime.String(), distCCRunningLongestTime.String())
	}

	return nil
}

func (de *distccEngine) sendProjectMessage(projectID string, extra []byte) ([]byte, error) {
	var msg Message
	if err := codec.DecJSON(extra, &msg); err != nil {
		blog.Errorf("engine(%s) try sending project(%s) message, decode message(%s) failed: %v",
			EngineName, projectID, string(extra), err)
		return nil, err
	}

	switch msg.Type {
	case MessageTypeGetCMakeArgs:
		return de.sendMessageGetCMakeArgs(projectID, msg.MessageGetCMakeArgs)
	default:
		err := engine.ErrorUnknownMessageType
		blog.Errorf("engine(%s) try sending project(%s) message type(%d) failed: %v",
			EngineName, projectID, msg.Type, err)
		return nil, err
	}
}

func (de *distccEngine) sendTaskMessage(taskID string, extra []byte) ([]byte, error) {
	var msg Message
	if err := codec.DecJSON(extra, &msg); err != nil {
		blog.Errorf("engine(%s) try sending task(%s) message, decode message(%s) failed: %v",
			EngineName, taskID, string(extra), err)
		return nil, err
	}

	switch msg.Type {
	case MessageTypeRecordStats:
		return de.sendMessageRecordStats(taskID, msg.MessageRecordStats)
	default:
		err := engine.ErrorUnknownMessageType
		blog.Errorf("engine(%s) try sending task(%s) message type(%d) failed: %v",
			EngineName, taskID, msg.Type, err)
		return nil, err
	}
}

func (de *distccEngine) sendMessageRecordStats(taskID string, msg MessageRecordStats) ([]byte, error) {
	task, err := de.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try sending task message, get task(%s) failed: %v", EngineName, taskID, err)
		return nil, err
	}

	task.Client.Message = msg.Message
	task.Stats.CCacheInfo = string(msg.CCacheStats.Dump())
	task.Stats.CacheDirectHit = int64(msg.CCacheStats.DirectHit)
	task.Stats.CachePreprocessedHit = int64(msg.CCacheStats.PreprocessedHit)
	task.Stats.CacheMiss = int64(msg.CCacheStats.CacheMiss)
	task.Stats.FilesInCache = int64(msg.CCacheStats.FilesInCache)
	task.Stats.CacheSize = msg.CCacheStats.CacheSize
	task.Stats.MaxCacheSize = msg.CCacheStats.MaxCacheSize

	if err = de.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try sending task message, update task(%s) failed: %v", EngineName, taskID, err)
		return nil, err
	}

	blog.Infof("engine(%s) success to sending task(%s) message", EngineName, taskID)
	return (&MessageResponse{}).Dump(), nil
}

func (de *distccEngine) sendMessageGetCMakeArgs(projectID string, msg MessageGetCMakeArgs) ([]byte, error) {
	project, err := de.mysql.GetProjectSetting(projectID)
	if err != nil {
		blog.Errorf("engine(%s) try sending task message, get project(%s) settings failed: %v",
			EngineName, projectID, err)
		return nil, err
	}

	cacheEnabled := project.CCacheEnabled
	if msg.CCacheEnabled != nil {
		cacheEnabled = *msg.CCacheEnabled
	}
	cs := CommandSetting{
		CommandType:     CommandCmake,
		CompilerVersion: project.GccVersion,
		CCacheEnable:    cacheEnabled,
		BanDistCC:       project.BanDistCC,
		BanAllBooster:   project.BanAllBooster,
	}

	resp := &MessageResponse{
		CMakeArgs: cs.GetCommand(),
	}
	return resp.Dump(), nil
}

// Message describe the data format from SendMessage caller.
type Message struct {
	Type MessageType `json:"type"`

	MessageRecordStats  MessageRecordStats  `json:"record_stats"`
	MessageGetCMakeArgs MessageGetCMakeArgs `json:"get_cmake_args"`
}

type MessageType int

const (
	// MessageTypeRecordStats means this message is about record stats from client.
	MessageTypeRecordStats MessageType = iota

	// MessageTypeGetCMakeArgs means this message is about to get cmake args.
	MessageTypeGetCMakeArgs
)

// MessageRecordStats describe the message data of type record stats.
type MessageRecordStats struct {
	Message     string      `json:"message"`
	CCacheStats CCacheStats `json:"ccache_stats"`
}

// MessageGetCMakeArgs describe the message data of type cmake args.
type MessageGetCMakeArgs struct {
	CCacheEnabled *bool `json:"ccache_enabled"`
}

// MessageResponse describe the return data of SendMessage
type MessageResponse struct {
	CMakeArgs string `json:"cmake_args"`
}

// Dump dump the struct dat into byte
func (mr *MessageResponse) Dump() []byte {
	var data []byte
	_ = codec.EncJSON(mr, &data)
	return data
}

func (de *distccEngine) getServerIPList() []string {
	r := make([]string, 0, 100)
	l, _ := de.conf.Rd.GetServers()
	for _, i := range l {
		r = append(r, i.IP)
	}
	return r
}

func (de *distccEngine) initBrokers() error {
	for _, broker := range de.conf.Brokers {
		brokerName := brokerName(broker)

		gcc, err := de.mysql.GetGcc(broker.GccVersion)
		if err != nil {
			blog.Errorf("engine(%s) init broker(%s) get gcc(%s) failed: %v",
				EngineName, brokerName, broker.GccVersion, err)
			return err
		}

		if err = de.mgr.AddBroker(
			brokerName, crm.StrategyConst, crm.NewConstBrokerStrategy(broker.ConstNum), crm.BrokerParam{
				Param: crm.ResourceParam{
					City:     broker.City,
					Platform: "linux",
					Env: map[string]string{
						envAllow: broker.Allow,
						envJob:   fmt.Sprintf("%d", broker.JobPerInstance),
					},
					Ports: map[string]string{
						portsService: "http",
						portsStats:   "http",
					},
					Image:      gcc.Image,
					BrokerName: brokerName,
				},
				Instance: broker.Instance,
				FitFunc: func(brokerParam, requestParam crm.ResourceParam) bool {
					return brokerParam.City == requestParam.City && brokerParam.Image == requestParam.Image
				},
			}); err != nil {
			blog.Errorf("engine(%s) init broker(%s) add broker failed: %v", EngineName, brokerName, err)
			return err
		}

		blog.Infof("engine(%s) success to init broker(%s)", EngineName, brokerName)
	}

	return nil
}

func brokerName(conf config.EngineDistCCBrokerConfig) string {
	return strings.ReplaceAll(fmt.Sprintf("%s-%s", conf.City, conf.GccVersion), ".", "-")
}
