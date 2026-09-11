//go:build !loong64
// +build !loong64

package monitor

import (
	"context"
	"sync/atomic"
)

// inputRunner 为单个 input 提供 Telegraf 风格的不重入保护。
//
// 一个 Gather 尚未返回时，后续轮次只跳过该 input，不会再次创建 Gather
// goroutine。这样即使底层 syscall、cgo 或 fork/exec 永久阻塞，每个 input
// 实例也最多留下一个采集 goroutine，不会按采集周期持续累积。
type inputRunner struct {
	input   Input
	running atomic.Bool
}

type inputResult struct {
	name       string
	metrics    []Metric
	err        error
	panicValue any
}

func newInputRunners(inputs []Input) []*inputRunner {
	runners := make([]*inputRunner, 0, len(inputs))
	for _, input := range inputs {
		runners = append(runners, &inputRunner{input: input})
	}
	return runners
}

// start 尝试启动一次 Gather。返回 false 表示上一轮仍未完成，本轮必须跳过。
// results 必须有足够缓冲；调用轮次超时返回后，迟到的结果仍可完成发送，避免
// 采集 goroutine 因无人接收结果而产生二次泄漏。
func (r *inputRunner) start(ctx context.Context, results chan<- inputResult) bool {
	if !r.running.CompareAndSwap(false, true) {
		return false
	}

	go func() {
		result := inputResult{}
		defer func() {
			if recovered := recover(); recovered != nil {
				result.panicValue = recovered
			}
			r.running.Store(false)
			results <- result
		}()

		result.name = r.input.Name()
		result.metrics, result.err = r.input.Gather(ctx)
	}()
	return true
}
