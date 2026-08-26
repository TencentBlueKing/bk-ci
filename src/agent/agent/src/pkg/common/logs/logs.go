package logs

import (
	"bytes"
	"fmt"
	"io"
	"os"

	"github.com/sirupsen/logrus"
	lumberjack "gopkg.in/natefinch/lumberjack.v2"
)

const (
	ErrorNoStackKey = "error_no_stack_key"
)

var Logs *logrus.Entry

var logWriter io.Closer

func Init(filepath string, isDebug bool, logStd bool) error {
	// 默认轮转策略：保留 7 天 / 7 个备份，与历史行为一致。
	return InitWithRotate(filepath, isDebug, logStd, 7, 7)
}

// InitWithRotate 与 Init 相同，但允许自定义 lumberjack 的 MaxAge / MaxBackups，
// 便于单独的日志文件（如 monitor 子进程的 devopsMonitor.log 只保留 1 天）
// 使用不同于主日志的保留策略。
func InitWithRotate(filepath string, isDebug bool, logStd bool, maxAge int, maxBackups int) error {
	logInfo := logrus.WithFields(logrus.Fields{})

	lumLog := &lumberjack.Logger{
		Filename:   filepath,
		MaxAge:     maxAge,
		MaxBackups: maxBackups,
		LocalTime:  true,
	}
	logWriter = lumLog

	// 同时写入到 std
	if logStd {
		logInfo.Logger.Out = io.MultiWriter(lumLog, os.Stdout)
	} else {
		logInfo.Logger.Out = lumLog
	}

	logInfo.Logger.SetFormatter(&MyFormatter{})

	go DoDailySplitLog(filepath, lumLog)

	if isDebug {
		logInfo.Logger.SetLevel(logrus.DebugLevel)
	}

	Logs = logInfo

	return nil
}

// Close flushes and closes the underlying log file. Safe to call before
// os.Exit to ensure all buffered log entries are written to disk.
func Close() {
	if logWriter != nil {
		logWriter.Close()
	}
}

// SetDebugMode toggles the log level at runtime without restarting.
func SetDebugMode(debug bool) {
	if Logs == nil {
		return
	}
	if debug {
		Logs.Logger.SetLevel(logrus.DebugLevel)
	} else {
		Logs.Logger.SetLevel(logrus.InfoLevel)
	}
}

// UNTestDebugInit DebugInit 初始化为debug模式下的log，将日志输出到标准输出流，只是为了单元测试使用
func UNTestDebugInit() {
	logInfo := logrus.WithFields(logrus.Fields{})
	logInfo.Logger.SetFormatter(&MyFormatter{})
	logInfo.Logger.SetOutput(os.Stdout)
	logInfo.Logger.SetLevel(logrus.DebugLevel)
	Logs = logInfo
}

type MyFormatter struct{}

func (m *MyFormatter) Format(entry *logrus.Entry) ([]byte, error) {
	var b *bytes.Buffer
	if entry.Buffer != nil {
		b = entry.Buffer
	} else {
		b = &bytes.Buffer{}
	}

	timestamp := entry.Time.Format("2006-01-02 15:04:05.000")

	newLog := fmt.Sprintf("%s|%s|%s", timestamp, entry.Level, entry.Message)
	b.WriteString(newLog)

	for k, v := range entry.Data {
		if k == ErrorNoStackKey {
			b.WriteString(fmt.Sprintf("|error: %v", v))
			continue
		}
		switch v := v.(type) {
		case error:
			// Otherwise errors are ignored by `encoding/json`
			// https://github.com/sirupsen/logrus/issues/137
			//
			// Print errors verbosely to get stack traces where available
			b.WriteString(fmt.Sprintf("|%s: %+v", k, v))
		default:
			b.WriteString(fmt.Sprintf("|%s: %v", k, v))
		}
	}

	b.WriteString("\n")

	return b.Bytes(), nil
}
