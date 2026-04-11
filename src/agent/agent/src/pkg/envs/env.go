package envs

import (
	"fmt"
	"os"
	"strings"
	"sync"
)

// GApiEnvVars 来自页面配置的环境变量
var GApiEnvVars *GEnvVarsT

type GEnvVarsT struct {
	envs map[string]string
	lock sync.RWMutex
}

func Init() {
	GApiEnvVars = &GEnvVarsT{
		envs: make(map[string]string),
		lock: sync.RWMutex{},
	}

}

func (e *GEnvVarsT) Get(key string) (string, bool) {
	e.lock.RLock()
	defer e.lock.RUnlock()
	res, ok := e.envs[key]
	return res, ok
}

func (e *GEnvVarsT) GetAll() map[string]string {
	e.lock.RLock()
	defer e.lock.RUnlock()
	res := e.envs
	return res
}

func (e *GEnvVarsT) SetEnvs(envs map[string]string) {
	e.lock.Lock()
	defer e.lock.Unlock()
	e.envs = envs
}

func (e *GEnvVarsT) RangeDo(do func(k, v string) bool) {
	e.lock.RLock()
	defer e.lock.RUnlock()
	for k, v := range e.envs {
		ok := do(k, v)
		if !ok {
			return
		}
	}
}

func (e *GEnvVarsT) Size() int {
	e.lock.RLock()
	defer e.lock.RUnlock()
	return len(e.envs)
}

// Envs 获取系统环境变量，代替 os.Environ()
func Envs() []string {
	envs := os.Environ()
	pollingEnvs := FetchEnvFromPolling()
	if len(pollingEnvs) == 0 {
		return envs
	}
	envMap := make(map[string]string, len(envs)+len(pollingEnvs))
	for _, envStr := range envs {
		parts := strings.SplitN(envStr, "=", 2)
		if len(parts) == 2 {
			envMap[parts[0]] = parts[1]
		}
	}
	for key, value := range pollingEnvs {
		envMap[key] = value
	}
	resEnvs := make([]string, 0, len(envMap))
	for key, value := range envMap {
		resEnvs = append(resEnvs, fmt.Sprintf("%s=%s", key, value))
	}
	return resEnvs
}

// FetchEnvAndCheck 查询是否有某个环境变量，同时校验是否符合要求
func FetchEnvAndCheck(key string, checkValue string) bool {
	v, ok := FetchEnv(key)
	if !ok {
		return checkValue == ""
	}
	return v == checkValue
}

// FetchEnv 查询是否有某个环境变量，需要同时查询系统和后台变量
func FetchEnv(key string) (string, bool) {
	// 优先使用后台配置的
	v, ok := GApiEnvVars.Get(key)
	if ok {
		return v, true
	}

	for _, envStr := range Envs() {
		parts := strings.SplitN(envStr, "=", 2)
		if len(parts) == 2 && parts[0] == key {
			return parts[1], true
		}
	}

	return "", false
}
