package distcc

import (
	"fmt"

	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
)

const (
	mysqlTableTask            = "task_records"
	mysqlTableProjectSettings = "project_settings"
	mysqlTableProjectInfo     = "project_records"
	mysqlTableWhitelist       = "whitelist_settings"
	mysqlTableGcc             = "gcc_settings"
)

// TableTask describe the db columns of Task.
// It will inherit the TaskBasic.
type TableTask struct {
	engine.TableTaskBasic

	// client
	City          string  `gorm:"column:city" json:"city"`
	SourceIP      string  `gorm:"column:source_ip" json:"source_ip"`
	SourceCPU     int     `gorm:"column:source_cpu" json:"source_cpu"`
	GccVersion    string  `gorm:"column:gcc_version" json:"gcc_version"`
	User          string  `gorm:"column:user" json:"user"`
	Params        string  `gorm:"column:params" sql:"type:text" json:"params"`
	Cmd           string  `gorm:"column:cmd" sql:"type:text" json:"cmd"`
	Env           string  `gorm:"column:env" sql:"type:text" json:"env"`
	RequestCPU    float64 `gorm:"column:request_cpu" json:"request_cpu"`
	LeastCPU      float64 `gorm:"column:least_cpu" json:"least_cpu"`
	CCacheEnabled bool    `gorm:"column:ccache_enabled" json:"ccache_enabled"`
	BanDistCC     bool    `gorm:"column:ban_distcc;default:false" json:"ban_distcc"`
	BanAllBooster bool    `gorm:"column:ban_all_booster;default:false" json:"ban_all_booster"`
	RunDir        string  `gorm:"column:run_dir" sql:"type:text" json:"run_dir"`
	CommandType   string  `gorm:"column:command_type" json:"command_type"`
	Command       string  `gorm:"column:command" sql:"type:text" json:"command"`
	Extra         string  `gorm:"column:extra" sql:"type:text" json:"extra"`

	// compilers
	Compilers string `gorm:"column:compilers" sql:"type:text" json:"compilers"`

	// status
	CompileFilesOK       int64   `gorm:"column:compile_files_ok" json:"compile_files_ok"`
	CompileFilesErr      int64   `gorm:"column:compile_files_err" json:"compile_files_err"`
	CompileFilesTimeout  int64   `gorm:"column:compile_files_timeout" json:"compile_files_timeout"`
	CompilerCount        int     `gorm:"column:compiler_count" json:"compiler_count"`
	CPUTotal             float64 `gorm:"column:cpu_total" json:"cpu_total"`
	MemTotal             float64 `gorm:"column:mem_total" json:"mem_total"`
	CCacheInfo           string  `gorm:"column:ccache_info" sql:"type:text" json:"ccache_info"`
	CacheDirectHit       int64   `gorm:"column:cache_direct_hit" json:"cache_direct_hit"`
	CachePreprocessedHit int64   `gorm:"column:cache_preprocessed_hit" json:"cache_preprocessed_hit"`
	CacheMiss            int64   `gorm:"column:cache_miss" json:"cache_miss"`
	FilesInCache         int64   `gorm:"column:files_in_cache" json:"files_in_cache"`
	CacheSize            string  `gorm:"column:cache_size" json:"cache_size"`
	MaxCacheSize         string  `gorm:"column:max_cache_size" json:"max_cache_size"`

	// operator
	ClusterID         string  `gorm:"column:cluster_id" json:"cluster_id"`
	AppName           string  `gorm:"column:app_name" json:"app_name"`
	Namespace         string  `gorm:"column:namespace" json:"namespace"`
	Image             string  `gorm:"column:image" sql:"type:text" json:"image"`
	Instance          int     `gorm:"column:instance" json:"instance"`
	RequestInstance   int     `gorm:"column:request_instance" json:"request_instance"`
	LeastInstance     int     `gorm:"column:least_instance" json:"least_instance"`
	RequestCPUPerUnit float64 `gorm:"column:request_cpu_per_unit" json:"request_cpu_per_unit"`
	RequestMemPerUnit float64 `gorm:"column:request_mem_per_unit" json:"request_mem_per_unit"`

	// extra field, no includes under the distcc-server management, but for distcc-controller
	MaxJobs  int64 `gorm:"column:max_jobs;default:0" json:"max_jobs"`
	Observed bool  `gorm:"column:observed;default:false;index" json:"observed"`
}

// TableName specific table name.
func (tt TableTask) TableName() string {
	return mysqlTableTask
}

// TableProjectSetting describe the db columns of project setting.
// It will inherit the ProjectBasic.
type TableProjectSetting struct {
	engine.TableProjectBasic

	RequestCPU    float64 `gorm:"column:request_cpu" json:"request_cpu"`
	LeastCPU      float64 `gorm:"column:least_cpu" json:"least_cpu"`
	SuggestCPU    float64 `gorm:"column:suggest_cpu;default:0" json:"suggest_cpu"`
	AcceptedTime  int64   `gorm:"column:accepted_time;default:0" json:"accepted_time"`
	City          string  `gorm:"column:city" json:"city"`
	GccVersion    string  `gorm:"column:gcc_version" json:"gcc_version"`
	CCacheEnabled bool    `gorm:"column:ccache_enabled" json:"ccache_enabled"`
	BanDistCC     bool    `gorm:"column:ban_distcc;default:false" json:"ban_distcc"`
	BanAllBooster bool    `gorm:"column:ban_all_booster;default:false" json:"ban_all_booster"`

	// extra field, no includes under the distCC-server management, but for distCC-controller
	BanController bool `gorm:"column:ban_controller;default:false" json:"ban_controller"`
}

// TableName specific table name.
func (tps TableProjectSetting) TableName() string {
	return mysqlTableProjectSettings
}

// TableProjectInfo describe the db columns of project info.
// It will inherit the ProjectInfoBasic.
type TableProjectInfo struct {
	engine.TableProjectInfoBasic

	CompileFilesOK      int64   `gorm:"column:compile_files_ok" json:"compile_files_ok"`
	CompileFilesErr     int64   `gorm:"column:compile_files_err" json:"compile_files_err"`
	CompileFilesTimeout int64   `gorm:"column:compile_files_timeout" json:"compile_files_timeout"`
	CompileUnits        float64 `gorm:"column:compile_units" json:"compile_units"`
}

// TableName specific table name.
func (tpi TableProjectInfo) TableName() string {
	return mysqlTableProjectInfo
}

// TableWhitelist describe the db columns of whitelist.
// It will inherit the WhitelistBasic.
type TableWhitelist struct {
	engine.TableWhitelistBasic
}

// TableName specific table name.
func (twl TableWhitelist) TableName() string {
	return mysqlTableWhitelist
}

// TableGcc describe the db columns of gcc.
// It inherit directly from Basic.
type TableGcc struct {
	engine.TableBasic

	GccVersion string `gorm:"column:gcc_version;primary_key" json:"gcc_version"`
	Image      string `gorm:"column:image" sql:"type:text" json:"image"`
}

// TableName specific table name.
func (tg TableGcc) TableName() string {
	return mysqlTableGcc
}

// CheckData check if gcc data valid.
func (tg *TableGcc) CheckData() error {
	if tg.GccVersion == "" {
		return fmt.Errorf("gcc version empty")
	}
	if tg.Image == "" {
		return fmt.Errorf("image empty")
	}
	return nil
}
