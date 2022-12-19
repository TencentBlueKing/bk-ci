package config

type ConfigYaml struct {
	Server            Server            `json:"server"`
	Mysql             Mysql             `json:"mysql"`
	Redis             Redis             `json:"redis"`
	Kubernetes        Kubernetes        `json:"kubernetes"`
	Gateway           Gateway           `json:"gateway"`
	Dispatch          Dispatch          `json:"dispatch"`
	BuildAndPushImage BuildAndPushImage `json:"buildAndPushImage"`
	ApiServer         ApiServer         `json:"apiServer"`
}

type Server struct {
	Port string `json:"port"`
}

type Mysql struct {
	DataSourceName  string `json:"dataSourceName"`
	ConnMaxLifetime int    `json:"connMaxLifetime"`
	MaxOpenConns    int    `json:"maxOpenConns"`
	MaxIdleConns    int    `json:"maxIdleConns"`
}

type Redis struct {
	Addr     string `json:"addr"`
	Password string `json:"password"`
	Db       int    `json:"db"`
}

type Kubernetes struct {
	NameSpace string `json:"nameSpace"`
}

type Gateway struct {
	Url string `json:"url"`
}

type Dispatch struct {
	Label          string           `json:"label"`
	Watch          Watch            `json:"watch"`
	Builder        Builder          `json:"builder"`
	VolumeMount    VolumeMount      `json:"volumeMount"`
	Volume         Volume           `json:"volume"`
	PrivateMachine DedicatedMachine `json:"privateMachine"`
	SpecialMachine DedicatedMachine `json:"specialMachine"`
}

type DedicatedMachine struct {
	Label string `json:"label"`
}

type Watch struct {
	Task WatchTask `json:"task"`
}

type WatchTask struct {
	Label string `json:"label"`
}

type Builder struct {
	NodeSelector    NodeSelector        `json:"nodeSelector"`
	NodesAnnotation string              `json:"nodesAnnotation"`
	RealResource    BuilderRealResource `json:"realResource"`
}

type NodeSelector struct {
	Label string `json:"label"`
	Value string `json:"value"`
}

type BuilderRealResource struct {
	PrometheusUrl          string `json:"prometheusUrl"`
	RealResourceAnnotation string `json:"realResourceAnnotation"`
}

type VolumeMount struct {
	DataPath             string   `json:"dataPath"`
	LogPath              string   `json:"logPath"`
	BuilderConfigMapPath string   `json:"builderConfigMapPath"`
	Cfs                  CfsMount `json:"cfs"`
}

type Volume struct {
	BuilderConfigMap ConfigMap `json:"builderConfigMap"`
	HostPath         HostPath  `json:"hostPath"`
	Cfs              Cfs       `json:"cfs"`
}

type ConfigMap struct {
	Name  string        `json:"name"`
	Items []KeyPathItem `json:"items"`
}

type KeyPathItem struct {
	Key  string `json:"key"`
	Path string `json:"path"`
}

type HostPath struct {
	DataHostDir string `json:"dataHostDir"`
	LogsHostDir string `json:"logsHostDir"`
}

type Cfs struct {
	Path string `json:"path"`
}

type CfsMount struct {
	Path     string `json:"path"`
	ReadOnly bool   `json:"readOnly"`
}

type BuildAndPushImage struct {
	Image             string   `json:"image"`
	PullImageRegistry Registry `json:"pullImageRegistry"`
}

type Registry struct {
	Server   string `json:"server"`
	Username string `json:"username"`
	Password string `json:"password"`
}

type ApiServer struct {
	Auth Auth `json:"auth"`
}

type Auth struct {
	ApiToken      ApiToken `json:"apiToken"`
	RsaPrivateKey string   `json:"rsaPrivateKey"`
}

type ApiToken struct {
	Key   string `json:"key"`
	Value string `json:"value"`
}
