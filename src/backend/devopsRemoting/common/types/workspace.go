package types

// 工作空间的Yaml配置文件
type Devfile struct {
	Vscode          *Vscode   `json:"vscode" yaml:"vscode"`
	Commands        *Commands `json:"commands" yaml:"commands"`
	Ports           []*Port   `json:"ports" yaml:"ports"`
	WorkspaceFolder string    `json:"workspaceFolder" yaml:"workspaceFolder"`
}

// vscode相关配置
type Vscode struct {
	// 需要的vscode插件Id列表
	Extensions []string `json:"extensions" yaml:"extensions"`
}

// 命令相关配置
type Commands struct {
	// 工作空间创建后需要执行的命令
	PostCreateCommand string `json:"postCreateCommand" yaml:"postCreateCommand"`
}

type Port struct {
	Name       string `json:"name" yaml:"name"`
	Desc       string `json:"desc" yaml:"desc"`
	Port       int    `json:"port" yaml:"port"`
	OnOpen     string `json:"onOpen" yaml:"onOpen"`
	Visibility string `json:"visibility" yaml:"visibility"`
}

type WorkspaceInstancePort struct {
	Port       float64 `json:"port,omitempty"`
	URL        string  `json:"url,omitempty"`
	Visibility string  `json:"visibility,omitempty"`
}
