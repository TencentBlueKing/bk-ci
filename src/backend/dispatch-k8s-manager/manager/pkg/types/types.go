package types

type Result struct {
	Data    interface{} `json:"data"`
	Status  int         `json:"status"`
	Message string      `json:"message"`
}

// Registry 定义访问server的参数
type Registry struct {
	Server   string `json:"server"`
	UserName string `json:"username"`
	Password string `json:"password"`
}

// NFS 定义nfs server参数
type NFS struct {
	Server    string `json:"server"`
	Path      string `json:"path"`
	MountPath string `json:"mountPath"`
}
