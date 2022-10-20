package logs

import (
	"bytes"
	"disaptch-k8s-manager/pkg/config"
	"disaptch-k8s-manager/pkg/types"
	"fmt"
	"github.com/gin-gonic/gin"
	"github.com/sirupsen/logrus"
	"gopkg.in/natefinch/lumberjack.v2"
	"net"
	"net/http"
	"net/http/httputil"
	"os"
	"path/filepath"
	"runtime/debug"
	"strings"
)

var Logs *logrus.Logger

func Init(filepath string) {
	logInfo := logrus.New()

	logInfo.Out = &lumberjack.Logger{
		Filename:  filepath,
		MaxAge:    7,
		LocalTime: true,
	}

	logInfo.SetReportCaller(true)
	logInfo.SetFormatter(&LogFormatter{})

	if config.Envs.IsDebug {
		logInfo.Level = logrus.DebugLevel
	}

	Logs = logInfo
}

// GinRecovery recover掉项目可能出现的panic，并记录相关日志
func GinRecovery(stack bool) gin.HandlerFunc {
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
					Logs.Error(fmt.Sprintf("%s %v %s. ", c.Request.URL.Path, err, string(httpRequest)))
					// If the connection is dead, we can't write a status to it.
					c.Error(err.(error)) // nolint: errcheck
					c.Abort()
					return
				}

				if stack {
					Logs.Error(fmt.Sprintf("[Recovery from panic] %v %s %s. ",
						err, string(httpRequest), string(debug.Stack())))
				} else {
					Logs.Error(fmt.Sprintf("[Recovery from panic] %v %s. ", err, string(httpRequest)))
				}
				c.AbortWithStatus(http.StatusInternalServerError)
				c.JSON(http.StatusInternalServerError, &types.Result{
					Data:    nil,
					Status:  http.StatusInternalServerError,
					Message: "kubernetes manager server err",
				})
			}
		}()
		c.Next()
	}
}

type LogFormatter struct{}

func (m *LogFormatter) Format(entry *logrus.Entry) ([]byte, error) {
	var b *bytes.Buffer
	if entry.Buffer != nil {
		b = entry.Buffer
	} else {
		b = &bytes.Buffer{}
	}

	timestamp := entry.Time.Format("2006.01.02 15:04:01.002")
	var newLog string

	//HasCaller()为true才会有调用信息
	fName := filepath.Base(entry.Caller.File)
	newLog = fmt.Sprintf("%s %s %s:%d  %s\n",
		timestamp, entry.Level, fName, entry.Caller.Line, entry.Message)

	b.WriteString(newLog)
	return b.Bytes(), nil
}

func Debug(args ...interface{}) {
	Logs.Debug(args...)
}

func Info(args ...interface{}) {
	Logs.Info(args...)
}

func Warn(args ...interface{}) {
	Logs.Warn(args...)
}

func Error(args ...interface{}) {
	Logs.Error(args...)
}
