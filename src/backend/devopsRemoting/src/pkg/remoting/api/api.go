package api

import (
	"context"
	"devopsRemoting/common/logs"
	apitypes "devopsRemoting/src/pkg/remoting-api/types"
	"devopsRemoting/src/pkg/remoting/config"
	"devopsRemoting/src/pkg/remoting/constant"
	"fmt"
	"net"
	"net/http"
	"net/http/httputil"
	"os"
	"reflect"
	"runtime/debug"
	"strings"
	"sync"

	"github.com/gin-gonic/gin"
	"github.com/gin-gonic/gin/binding"
	"github.com/go-playground/validator/v10"
)

func StartAPIServer(ctx context.Context, cfg *config.Config, wg *sync.WaitGroup) {
	defer wg.Done()
	defer logs.Debug("startAPIEndpoint shutdown")

	if os.Getenv(constant.DebugModEnvName) != "true" {
		gin.SetMode(gin.ReleaseMode)
	}

	router := gin.Default()

	// 日志中间件，添加日志接受error堆栈
	logRecovery := ginRecovery(true)

	// 对外暴露的链接和用户自建的明显区分下
	remoteR := router.Group("/_remoting", logRecovery)
	api := remoteR.Group("/api")
	initSSHApi(api.Group("/ssh"))
	initRemotingApi(api.Group("/remoting"))
	initCommandsApi(api.Group("/commands"))
	initTerminalApi(api.Group("/terminal"))
	initPortsApi(api.Group("/ports"))
	initTokenApi(api.Group("/token"))

	// 修改gin框架中的Validator引擎属性，实现自定制参数校验的返回信息
	if v, ok := binding.Validator.Engine().(*validator.Validate); ok {

		// 注册一个获取json tag的自定义方法
		v.RegisterTagNameFunc(func(fld reflect.StructField) string {
			name := strings.SplitN(fld.Tag.Get("json"), ",", 2)[0]
			if name == "-" {
				return ""
			}
			return name
		})
	}

	httpSrv := &http.Server{
		Addr:    fmt.Sprintf(":%d", cfg.Config.APIServerPort),
		Handler: router,
	}

	go func() {
		if err := httpSrv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logs.WithError(err).Error("Could not start api listener")
		}
	}()

	<-ctx.Done()
	logs.Info("shutting down API endpoint")
	httpSrv.Shutdown(ctx)
}

// okFail 请求是成功的但是逻辑错了
func okFail(c *gin.Context, code int, err error) {
	// 对于所有的500日志都记录下堆栈信息
	if code == http.StatusInternalServerError {
		logs.WithField("uri", c.Request.RequestURI).WithError(err).Error("request error.")
	}
	c.JSON(http.StatusOK, &apitypes.DevopsHttpResult{
		Data:    nil,
		Status:  code,
		Message: err.Error(),
	})
}

func ok(c *gin.Context, data any) {
	c.JSON(http.StatusOK, &apitypes.DevopsHttpResult{
		Data:   data,
		Status: 0,
	})
}

// ginRecovery recover掉项目可能出现的panic，并记录相关日志
func ginRecovery(stack bool) gin.HandlerFunc {
	return func(c *gin.Context) {
		defer func() {
			if err := recover(); err != nil {
				// Check for a broken connection, as it is not really a
				// condition that warrants a panic stack trace.
				var brokenPipe bool
				if ne, ok := err.(*net.OpError); ok {
					if se, ok := ne.Err.(*os.SyscallError); ok {
						if strings.Contains(strings.ToLower(se.Error()), "broken pipe") || strings.Contains(strings.ToLower(se.Error()), "connection reset by peer") {
							brokenPipe = true
						}
					}
				}

				httpRequest, _ := httputil.DumpRequest(c.Request, false)
				if brokenPipe {
					logs.Error(fmt.Sprintf("%s %v %s. ", c.Request.URL.Path, err, string(httpRequest)))
					// If the connection is dead, we can't write a status to it.
					c.Error(err.(error)) // nolint: errcheck
					c.Abort()
					return
				}

				if stack {
					logs.Error(fmt.Sprintf("[Recovery from panic] %v %s %s. ",
						err, string(httpRequest), string(debug.Stack())))
				} else {
					logs.Error(fmt.Sprintf("[Recovery from panic] %v %s. ", err, string(httpRequest)))
				}
				c.AbortWithStatus(http.StatusInternalServerError)
				c.JSON(http.StatusInternalServerError, &apitypes.DevopsHttpResult{
					Data:    nil,
					Status:  http.StatusInternalServerError,
					Message: "devops remoting server err",
				})
			}
		}()
		c.Next()
	}
}
