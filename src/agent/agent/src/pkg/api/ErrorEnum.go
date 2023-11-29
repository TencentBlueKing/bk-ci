package api

type ErrorCode int

const (
	BuildProcessRunError ErrorCode = 2128040 + iota
	RecoverRunFileError
	LoseRunFileError
	MakeTmpDirError
	BuildProcessStartError
	PrepareScriptCreateError
	DockerOsError
	DockerRunShInitError
	DockerRunShStatError
	DockerClientCreateError
	DockerImagesFetchError
	DockerImagePullError
	DockerMakeTmpDirError
	DockerMountCreateError
	DockerContainerCreateError
	DockerContainerStartError
	DockerContainerRunError
	DockerContainerDoneStatusError
	DockerChmodInitshError
	DockerCredGetError
	DockerDockerOptions
	DockerImageDebugError
)

type ErrorTypes string

const (
	System     ErrorTypes = "SYSTEM"
	User       ErrorTypes = "USER"
	ThirdParty ErrorTypes = "THIRD_PARTY"
	Plugin     ErrorTypes = "PLUGIN"
)

type ErrorEnum struct {
	Type    ErrorTypes
	Code    ErrorCode
	Message string
}

var (
	NoErrorEnum                  = &ErrorEnum{Type: "", Code: 0, Message: ""}
	BuildProcessRunErrorEnum     = &ErrorEnum{Type: User, Code: BuildProcessRunError, Message: "构建进程执行错误"}
	RecoverRunFileErrorEnum      = &ErrorEnum{Type: User, Code: RecoverRunFileError, Message: "恢复执行文件失败错误"}
	LoseRunFileErrorEnum         = &ErrorEnum{Type: User, Code: LoseRunFileError, Message: "丢失执行文件失败错误"}
	MakeTmpDirErrorEnum          = &ErrorEnum{Type: User, Code: MakeTmpDirError, Message: "创建临时目录失败"}
	BuildProcessStartErrorEnum   = &ErrorEnum{Type: User, Code: BuildProcessStartError, Message: "启动构建进程失败"}
	PrepareScriptCreateErrorEnum = &ErrorEnum{Type: User, Code: PrepareScriptCreateError, Message: "预构建脚本创建失败"}
	DockerOsErrorEnum            = &ErrorEnum{Type: User, Code: DockerOsError, Message: "目前仅支持linux系统使用第三方docker构建机"}
	DockerRunShInitErrorEnum     = &ErrorEnum{
		Type:    User,
		Code:    DockerRunShInitError,
		Message: "下载Docker构建机初始化脚本失败",
	}
	DockerRunShStatErrorEnum = &ErrorEnum{
		Type:    User,
		Code:    DockerRunShStatError,
		Message: "获取Docker构建机初始化脚本状态失败",
	}
	DockerCredGetErrorEnum = &ErrorEnum{
		Type:    User,
		Code:    DockerCredGetError,
		Message: "获取docker凭据错误",
	}
	DockerClientCreateErrorEnum = &ErrorEnum{
		Type:    User,
		Code:    DockerClientCreateError,
		Message: "获取docker客户端错误",
	}
	DockerImagesFetchErrorEnum = &ErrorEnum{
		Type:    User,
		Code:    DockerImagesFetchError,
		Message: "获取docker镜像列表错误",
	}
	DockerImagePullErrorEnum = &ErrorEnum{
		Type:    User,
		Code:    DockerImagePullError,
		Message: "拉取docker镜像失败",
	}
	DockerMakeTmpDirErrorEnum = &ErrorEnum{
		Type:    User,
		Code:    DockerMakeTmpDirError,
		Message: "创建Docker构建临时目录失败",
	}
	DockerDockerOptionsErrorEnum = &ErrorEnum{
		Type:    User,
		Code:    DockerDockerOptions,
		Message: "添加Docker options错误",
	}
	DockerMountCreateErrorEnum = &ErrorEnum{
		Type:    User,
		Code:    DockerMountCreateError,
		Message: "准备Docker挂载目录失败",
	}
	DockerContainerCreateErrorEnum = &ErrorEnum{
		Type:    User,
		Code:    DockerContainerCreateError,
		Message: "创建docker容器失败",
	}
	DockerContainerStartErrorEnum = &ErrorEnum{
		Type:    User,
		Code:    DockerContainerStartError,
		Message: "启动docker容器失败",
	}
	DockerContainerRunErrorEnum = &ErrorEnum{
		Type:    User,
		Code:    DockerContainerRunError,
		Message: "docker容器运行失败",
	}
	DockerContainerDoneStatusErrorEnum = &ErrorEnum{
		Type:    User,
		Code:    DockerContainerDoneStatusError,
		Message: "docker容器运行结束时状态码不为0",
	}
	DockerChmodInitshErrorEnum = &ErrorEnum{
		Type:    User,
		Code:    DockerChmodInitshError,
		Message: "docker校验并修改启动脚本权限失败",
	}
	DockerImageDebugErrorEnum = &ErrorEnum{
		Type:    User,
		Code:    DockerImageDebugError,
		Message: "启动docker登录调试失败",
	}
)

func (t *ThirdPartyBuildInfo) ToFinish(
	success bool,
	message string,
	errorEnum *ErrorEnum,
) *ThirdPartyBuildWithStatus {
	if success || errorEnum == NoErrorEnum {
		return &ThirdPartyBuildWithStatus{
			ThirdPartyBuildInfo: *t,
			Success:             success,
			Message:             message,
			Error:               nil,
		}
	}
	return &ThirdPartyBuildWithStatus{
		ThirdPartyBuildInfo: *t,
		Success:             success,
		Message:             message,
		Error: &Error{
			ErrorType:    errorEnum.Type,
			ErrorMessage: errorEnum.Message,
			ErrorCode:    errorEnum.Code,
		},
	}
}
