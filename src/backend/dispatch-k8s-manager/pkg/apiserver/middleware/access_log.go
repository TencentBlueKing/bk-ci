package middleware

import (
	"bytes"
	"fmt"
	"github.com/gin-gonic/gin"
	"github.com/sirupsen/logrus"
	"gopkg.in/natefinch/lumberjack.v2"
	"time"
)

func InitAccessLog(filepath string) gin.HandlerFunc {
	logInfo := logrus.New()

	logInfo.Out = &lumberjack.Logger{
		Filename:  filepath,
		MaxAge:    7,
		LocalTime: true,
	}

	logInfo.SetFormatter(&AccessLogFormatter{})
	return func(c *gin.Context) {
		// 开始时间
		startTime := time.Now()

		// 处理请求
		c.Next()

		// 结束时间
		endTime := time.Now()

		// 执行时间
		latencyTime := endTime.Sub(startTime)

		// 请求方式
		reqMethod := c.Request.Method

		// 请求路由
		reqUri := c.Request.RequestURI

		// 状态码
		statusCode := c.Writer.Status()

		// 请求IP
		clientIP := c.ClientIP()

		//日志格式
		logInfo.Infof(
			"%s \"%s %s\" %d %d",
			clientIP,
			reqMethod,
			reqUri,
			statusCode,
			latencyTime,
		)
	}
}

type AccessLogFormatter struct{}

func (a AccessLogFormatter) Format(entry *logrus.Entry) ([]byte, error) {
	var b *bytes.Buffer
	if entry.Buffer != nil {
		b = entry.Buffer
	} else {
		b = &bytes.Buffer{}
	}

	timestamp := entry.Time.Format("2006.01.02 15:04:01.002")
	var newLog string

	newLog = fmt.Sprintf("[%s] %s\n", timestamp, entry.Message)

	b.WriteString(newLog)
	return b.Bytes(), nil
}
