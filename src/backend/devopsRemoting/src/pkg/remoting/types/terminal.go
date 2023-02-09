package types

type Terminal struct {
	Alias          string            `json:"alias,omitempty"`
	Command        []string          `json:"command,omitempty"`
	Title          string            `json:"title,omitempty"`
	Pid            int64             `json:"pid,omitempty"`
	InitialWorkdir string            `json:"initialWorkdir,omitempty"`
	CurrentWorkdir string            `json:"currentWorkdir,omitempty"`
	Annotations    map[string]string `json:"annotations,omitempty"`
}

type OpenTerminalResponse struct {
	Terminal *Terminal `json:"terminal,omitempty"`
	// StarterToken 可以被用来改变窗口大小
	StarterToken string `json:"starterToken,omitempty"`
}

type ListenTerminalResponse struct {
	Output ListenTerminalResponseOutput `json:"output"`
}

type ListenTerminalResponseOutput interface {
	isListenTerminalResponseOutput()
}

type ListenTerminalResponseOutputData struct {
	Data []byte `json:"data"`
}

type ListenTerminalResponseOutputTitle struct {
	Title string `json:"title"`
}

type ListenTerminalResponseOutputExitCode struct {
	ExitCode int32 `json:"exitCode"`
}

func (*ListenTerminalResponseOutputData) isListenTerminalResponseOutput() {}

func (*ListenTerminalResponseOutputTitle) isListenTerminalResponseOutput() {}

func (*ListenTerminalResponseOutputExitCode) isListenTerminalResponseOutput() {}

type WriteTerminalResponse struct {
	BytesWritten uint32 `json:"bytes_written,omitempty"`
}
