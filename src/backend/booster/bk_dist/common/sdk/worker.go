package sdk

import (
	"fmt"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcProtocol "github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
)

// RemoteWorker describe the remote worker SDK
type RemoteWorker interface {
	Handler(
		ioTimeout int,
		stats *ControllerJobStats,
		updateJobStatsFunc func(),
		sandbox *syscall.Sandbox) RemoteWorkerHandler
}

// RemoteWorkerHandler describe the remote worker handler SDK
type RemoteWorkerHandler interface {
	ExecuteSyncTime(server string) (int64, error)
	ExecuteTask(server *dcProtocol.Host, req *BKDistCommand) (*BKDistResult, error)
	ExecuteTaskWithoutSaveFile(server *dcProtocol.Host, req *BKDistCommand) (*BKDistResult, error)
	ExecuteSendFile(server *dcProtocol.Host, req *BKDistFileSender, sandbox *syscall.Sandbox) (*BKSendFileResult, error)
	ExecuteCheckCache(server *dcProtocol.Host, req *BKDistFileSender, sandbox *syscall.Sandbox) ([]bool, error)
}

// FileDescPriority from 0 ~ 100, from high to low
type FileDescPriority int

const (
	MinFileDescPriority FileDescPriority = 100
	MaxFileDescPriority FileDescPriority = 0
)

// FileDesc desc file base info
type FileDesc struct {
	FilePath           string                `json:"file_path"`
	FileSize           int64                 `json:"file_size"`
	Lastmodifytime     int64                 `json:"last_modify_time"`
	Md5                string                `json:"md5"`
	Compresstype       protocol.CompressType `json:"compress_type"`
	Buffer             []byte                `json:"buffer"`
	CompressedSize     int64                 `json:"compressed_size"`
	Targetrelativepath string                `json:"target_relative_path"`
	Filemode           uint32                `json:"file_mode"`
	LinkTarget         string                `json:"link_target"`
	NoDuplicated       bool                  `json:"no_duplicated"`
	AllDistributed     bool                  `json:"all_distributed"`
	Priority           FileDescPriority      `json:"priority"`
}

// UniqueKey define the file unique key
func (f *FileDesc) UniqueKey() string {
	return fmt.Sprintf("%s_%d_%d", f.FilePath, f.FileSize, f.Lastmodifytime)
}

// FileResult desc file base info
type FileResult struct {
	FilePath           string `json:"file_path"`
	RetCode            int32  `json:"ret_code"`
	Targetrelativepath string `json:"target_relative_path"`
}

// BKCommand command to execute
type BKCommand struct {
	WorkDir         string     `json:"work_dir"`
	ExePath         string     `json:"exe_path"`
	ExeName         string     `json:"exe_name"`
	ExeToolChainKey string     `json:"exe_toolchain_key"`
	Params          []string   `json:"params"`
	Inputfiles      []FileDesc `json:"input_files"`
	ResultFiles     []string   `json:"result_files"`
	Env             []string   `json:"env"`
}

// Result result after execute command
type Result struct {
	RetCode       int32      `json:"ret_code"`
	OutputMessage []byte     `json:"output_message"`
	ErrorMessage  []byte     `json:"error_message"`
	ResultFiles   []FileDesc `json:"result_files"`
}

// BKDistCommand set by handler
type BKDistCommand struct {
	Commands []BKCommand `json:"commands"`

	// messages are the raw command ready-to-send data
	Messages []protocol.Message `json:"messages"`

	CustomSave bool `json:"custom_save"` // whether save result file custom
}

// BKDistFileSender describe the files sending to worker
type BKDistFileSender struct {
	Files []FileDesc `'json:"file"`

	Messages []protocol.Message `json:"messages"`
}

// BKDistResult return to handler
type BKDistResult struct {
	Results []Result `json:"results"`
}

// BKSendFileResult return to handler
type BKSendFileResult struct {
	Results []FileResult `json:"file_results"`
}

// LocalTaskResult
type LocalTaskResult struct {
	ExitCode int    `json:"exit_code"`
	Stdout   []byte `json:"stdout"`
	Stderr   []byte `json:"stderr"`
	Message  string `json:"message"`
}
