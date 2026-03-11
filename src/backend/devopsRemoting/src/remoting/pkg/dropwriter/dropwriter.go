package dropwriter

import (
	"io"
	"sync"
	"time"
)

// Clock 为桶限制器提取时间
type Clock func() time.Time

// NewBucket 创建一个带有实时时钟的新桶限制器
func NewBucket(capacity, refillRatePerSec int64) *Bucket {
	return NewBucketClock(capacity, refillRatePerSec, time.Now)
}

// NewBucketClock 生成带有自定义时钟的新桶限制器。 对测试很有用
func NewBucketClock(capacity, refillRatePerSec int64, clock Clock) *Bucket {
	return &Bucket{
		clock:      clock,
		capacity:   capacity,
		refillRate: refillRatePerSec,
	}
}

// Bucket 实现令牌桶限制器
type Bucket struct {
	clock Clock

	// capacity 是这个桶的总tokens容量
	capacity int64

	// refillRate 持有我们每秒重新填充的tokens数量
	refillRate int64

	// mu 同步存储桶访问
	mu sync.Mutex

	// availableTokens 是当前可用的token总数
	availableTokens int64

	// lastTick 上次我们调整可用token数
	lastTick time.Time
}

func (b *Bucket) adjustTokens() {
	b.mu.Lock()
	defer b.mu.Unlock()

	now := b.clock()
	defer func() {
		b.lastTick = now
	}()

	if b.lastTick.IsZero() {
		// 第一次调整, 将 availableTokens 设置为容量
		b.availableTokens = b.capacity
		return
	}

	b.availableTokens += int64(now.Sub(b.lastTick).Seconds() * float64(b.refillRate))
	if b.availableTokens > b.capacity {
		b.availableTokens = b.capacity
	}
}

// TakeAvailable 尝试从桶中删除请求令牌。 如果可用的token较少，所有剩余的标记都被移除并返回
func (b *Bucket) TakeAvailable(req int64) int64 {
	b.adjustTokens()

	b.mu.Lock()
	defer b.mu.Unlock()

	grant := req
	if grant > b.availableTokens {
		grant = b.availableTokens
	}
	b.availableTokens -= grant

	return grant
}

type writer struct {
	w      io.Writer
	bucket *Bucket
}

func (w *writer) Write(buf []byte) (n int, err error) {
	grant := w.bucket.TakeAvailable(int64(len(buf)))
	n, err = w.w.Write(buf[:grant])
	if err != nil {
		return
	}

	// 我们表现得好像我们已经写了整个缓冲区。 这才是真正实现的
	// 桶限制器强加的字节丢弃。 如果我们返回正确的字节数
	// 这里调用者可能会使用 ErrShortWrite 出错或者只是再试一次。
	n = len(buf)

	return
}

// Writer produces a new rate limited dropping writer.
func Writer(dst io.Writer, b *Bucket) io.Writer {
	return &writer{w: dst, bucket: b}
}
