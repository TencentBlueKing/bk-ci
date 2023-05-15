package logs

import (
	"fmt"
	"path"
	"runtime"
	"strings"
	"time"

	"github.com/sirupsen/logrus"
)

var Logs = logrus.WithFields(logrus.Fields{})

type ServiceContext struct {
	Service string `json:"service"`
	Version string `json:"version"`
}

func Init(service, version string, json, debug bool) {
	logInfo := logrus.WithFields(logrus.Fields{
		"serviceContext": ServiceContext{service, version},
	})

	if json {
		Logs.Logger.SetFormatter(&LogJsonFormatter{
			logrus.JSONFormatter{
				FieldMap: logrus.FieldMap{
					logrus.FieldKeyMsg: "message",
				},
				CallerPrettyfier: func(f *runtime.Frame) (string, string) {
					s := strings.Split(f.Function, ".")
					funcName := s[len(s)-1]
					return funcName, fmt.Sprintf("%s:%d", path.Base(f.File), f.Line)
				},
				TimestampFormat: time.RFC3339Nano,
			},
		})
	} else {
		Logs.Logger.SetFormatter(&logrus.TextFormatter{
			TimestampFormat: time.RFC3339Nano,
			FullTimestamp:   true,
		})
	}

	if debug {
		Logs.Logger.SetLevel(logrus.DebugLevel)
	}

	Logs = logInfo
}

type LogJsonFormatter struct {
	logrus.JSONFormatter
}

func (m *LogJsonFormatter) Format(entry *logrus.Entry) ([]byte, error) {
	// 打印error
	for k, v := range entry.Data {
		switch v := v.(type) {
		case error:
			entry.Data[k] = fmt.Sprintf("%+v", v)
		}
	}

	return m.JSONFormatter.Format(entry)
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

func Warnf(format string, args ...interface{}) {
	Logs.Warnf(format, args...)
}

func Error(args ...interface{}) {
	Logs.Error(args...)
}

func Errorf(format string, args ...interface{}) {
	Logs.Errorf(format, args...)
}

func Infof(format string, args ...interface{}) {
	Logs.Infof(format, args...)
}

func Debugf(format string, args ...interface{}) {
	Logs.Debugf(format, args...)
}

func Printf(format string, args ...interface{}) {
	Logs.Printf(format, args...)
}

func Fatal(args ...interface{}) {
	Logs.Fatal(args...)
}

func WithError(err error) *logrus.Entry {
	return Logs.WithError(err)
}

func WithField(key string, value interface{}) *logrus.Entry {
	return Logs.WithField(key, value)
}

func WithFields(fields logrus.Fields) *logrus.Entry {
	return Logs.WithFields(fields)
}
