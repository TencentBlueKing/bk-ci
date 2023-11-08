package logs

import (
	"fmt"
	"gopkg.in/natefinch/lumberjack.v2"
	"os"
	"time"
)

// DoDailySplitLog 执行一个每天分割日志的定时器
func DoDailySplitLog(filepath string, log *lumberjack.Logger) {
	for {
		next, c := initTimerChan()
		_, ok := <-c
		if !ok {
			// 当管道关闭时，重新拉起
			Logs.Error("DoDailySplitLog| timer chan close")
			continue
		}

		stat, err := os.Stat(filepath)
		if err != nil {
			// 不管是存在还是文件有问题，都继续第二天的定时
			Logs.Warn(fmt.Sprintf("DoDailySplitLog| %s stat error", filepath), err)
			continue
		}

		fileModTime := stat.ModTime()
		// 获取昨天凌晨的时间
		lastDayTime := next.AddDate(0, 0, -1)
		// 文件的最后修改时间需要时当天的文件
		if fileModTime.After(lastDayTime) {
			err = log.Rotate()
			if err != nil {
				Logs.Error(next.Format("2006-01-02 15:04:05"), "rotate log error", err)
			}
		}
	}
}

func initTimerChan() (time.Time, <-chan time.Time) {
	now := time.Now()
	//通过now偏移24小时
	next := now.Add(time.Hour * 24)
	//获取下一个凌晨的日期
	next = time.Date(next.Year(), next.Month(), next.Day(), 0, 0, 0, 0, next.Location())
	//计算当前时间到凌晨的时间间隔，设置一个定时器
	return next, time.NewTimer(next.Sub(now)).C
}
