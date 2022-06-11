package logs

import (
	"gopkg.in/natefinch/lumberjack.v2"
	"os"
	"time"
)

// DoDailySplitLog 执行一个每天分割日志的定时器
func DoDailySplitLog(filepath string, log *lumberjack.Logger) {
	for {
		now := time.Now()
		//通过now偏移24小时
		next := now.Add(time.Hour * 24)
		//获取下一个凌晨的日期
		next = time.Date(next.Year(), next.Month(), next.Day(), 0, 0, 0, 0, next.Location())
		//计算当前时间到凌晨的时间间隔，设置一个定时器
		t := time.NewTimer(next.Sub(now))

		<-t.C

		stat, err := os.Stat(filepath)
		if err != nil {
			// 不管是存在还是文件有问题，都继续第二天的定时
			continue
		}

		fileModTime := stat.ModTime()
		// 获取昨天凌晨的时间
		lastDayTime := next.AddDate(0, 0, -1)
		// 文件的最后修改时间需要时当天的文件
		if fileModTime.After(lastDayTime) {
			err = log.Rotate()
			if err != nil {
				logs.Error(next.Format("2006-01-02 15:04:05"), "rotate log error", err)
			}
		}
	}
}
