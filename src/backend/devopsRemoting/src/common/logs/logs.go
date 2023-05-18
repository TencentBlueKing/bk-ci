package logs

import (
	"fmt"
	"io"
	"os"
	"time"

	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"gopkg.in/natefinch/lumberjack.v2"
)

var Logs *zap.Logger

// DefaultInitFileLog 使用默认配置初始化，落盘的日志
func DefaultInitFileLog(serviceLogPath, errorLogPath, service, version string, isDebug bool) error {
	var tops = []TeeOption{
		{
			Lef: func(lvl zapcore.Level) bool {
				if isDebug {
					return lvl <= zapcore.WarnLevel
				} else {
					return lvl > zapcore.DebugLevel && lvl <= zapcore.WarnLevel
				}
			},
			Writer: &lumberjack.Logger{
				Filename:   serviceLogPath,
				MaxSize:    100,
				MaxBackups: 7,
				MaxAge:     7,
			},
		},
		{
			Lef: func(lvl zapcore.Level) bool {
				return lvl > zapcore.WarnLevel
			},
			Writer: &lumberjack.Logger{
				Filename:   errorLogPath,
				MaxSize:    100,
				MaxBackups: 7,
				MaxAge:     7,
			},
		},
	}
	return Init(tops, service, version)
}

// DeafultInitStd 初始化控制台日志
func DeafultInitStd(service, version string, isDebug bool) error {
	var tops = []TeeOption{
		{
			Lef: func(lvl zapcore.Level) bool {
				if isDebug {
					return lvl <= zapcore.WarnLevel
				} else {
					return lvl > zapcore.DebugLevel && lvl <= zapcore.WarnLevel
				}
			},
			Writer: os.Stdout,
		},
		{
			Lef: func(lvl zapcore.Level) bool {
				return lvl > zapcore.WarnLevel
			},
			Writer: os.Stderr,
		},
	}
	return Init(tops, service, version)
}

// Init 自定初始化
func Init(tops []TeeOption, service, version string) error {
	var cores []zapcore.Core
	cfg := zap.NewProductionConfig()
	cfg.EncoderConfig.TimeKey = "time"
	cfg.EncoderConfig.EncodeTime = func(t time.Time, enc zapcore.PrimitiveArrayEncoder) {
		enc.AppendString(t.Format(time.RFC3339Nano))
	}

	for _, top := range tops {
		top := top

		lv := zap.LevelEnablerFunc(func(lvl zapcore.Level) bool {
			return top.Lef(lvl)
		})

		w := zapcore.AddSync(top.Writer)

		core := zapcore.NewCore(
			zapcore.NewJSONEncoder(cfg.EncoderConfig),
			zapcore.AddSync(w),
			lv,
		)
		cores = append(cores, core)
	}

	Logs = zap.New(zapcore.NewTee(cores...), zap.AddCaller()).With(zap.Object("serviceContext", &ServiceContext{service, version}))

	return nil
}

type LevelEnablerFunc func(lvl zapcore.Level) bool

type TeeOption struct {
	Lef    LevelEnablerFunc
	Writer io.Writer
}

type ServiceContext struct {
	Service string `json:"service"`
	Version string `json:"version"`
}

func (s *ServiceContext) MarshalLogObject(enc zapcore.ObjectEncoder) error {
	enc.AddString("service", s.Service)
	enc.AddString("version", s.Version)
	return nil
}

var (
	DebugLevel = zap.DebugLevel

	String  = zap.String
	Strings = zap.Strings
	Int     = zap.Int
	Uint32  = zap.Uint32
	Int64   = zap.Int64
	Bool    = zap.Bool
	Err     = zap.Error
	Any     = zap.Any
	Bytes   = zap.ByteString
)

func Debug(msg string, fields ...zap.Field) {
	Logs.Debug(msg, fields...)
}

func Debugf(template string, args ...interface{}) {
	Logs.Sugar().Debugf(template, args...)
}

func Debugfw(template string, args []any, fields ...zap.Field) {
	Logs.Debug(fmt.Sprintf(template, args...), fields...)
}

func Info(msg string, fields ...zap.Field) {
	Logs.Info(msg, fields...)
}

func Infof(template string, args ...interface{}) {
	Logs.Sugar().Infof(template, args...)
}

func Infofw(template string, args []any, fields ...zap.Field) {
	Logs.Info(fmt.Sprintf(template, args...), fields...)
}

func Warn(msg string, fields ...zap.Field) {
	Logs.Warn(msg, fields...)
}

func Warnf(template string, args ...interface{}) {
	Logs.Sugar().Warnf(template, args...)
}

func Warnfw(template string, args []any, fields ...zap.Field) {
	Logs.Warn(fmt.Sprintf(template, args...), fields...)
}

func Error(msg string, fields ...zap.Field) {
	Logs.Error(msg, fields...)
}

func Errorf(template string, args ...interface{}) {
	Logs.Sugar().Errorf(template, args...)
}

func Errorfw(template string, args []any, fields ...zap.Field) {
	Logs.Error(fmt.Sprintf(template, args...), fields...)
}

func Fatal(msg string, fields ...zap.Field) {
	Logs.Fatal(msg, fields...)
}

func Fatalf(template string, args ...interface{}) {
	Logs.Sugar().Fatalf(template, args...)
}

func With(fields ...zap.Field) *zap.Logger {
	return Logs.With(fields...)
}

func Sync() error {
	return Logs.Sync()
}
