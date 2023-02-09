package sshproxy

import (
	"context"
	"devopsRemoting/common/logs"
	"io"
	"sync"
	"time"

	"github.com/ci-plugins/crypto-go/ssh"
)

func (s *Server) ChannelForward(ctx context.Context, session *Session, targetConn ssh.Conn, originChannel ssh.NewChannel) {
	targetChan, targetReqs, err := targetConn.OpenChannel(originChannel.ChannelType(), originChannel.ExtraData())
	if err != nil {
		logs.WithField("workspaceId", session.WorkspaceID).Error("open target channel error")
		originChannel.Reject(ssh.ConnectionFailed, "open target channel error")
		return
	}
	defer targetChan.Close()

	originChan, originReqs, err := originChannel.Accept()
	if err != nil {
		logs.WithField("workspaceId", session.WorkspaceID).Error("accept origin channel failed")
		return
	}
	if originChannel.ChannelType() == "session" {
		originChan = startHeartbeatingChannel(originChan, s.Heartbeater, session)
	}
	defer originChan.Close()

	maskedReqs := make(chan *ssh.Request, 1)

	go func() {
		for req := range originReqs {
			switch req.Type {
			case "pty-req", "shell":
				logs.WithField("workspaceId", session.WorkspaceID).Debugf("forwarding %s request", req.Type)
				if channel, ok := originChan.(*heartbeatingChannel); ok && req.Type == "pty-req" {
					channel.mux.Lock()
					channel.requestedPty = true
					channel.mux.Unlock()
				}
			}
			maskedReqs <- req
		}
		close(maskedReqs)
	}()

	go func() {
		io.Copy(targetChan, originChan)
		targetChan.CloseWrite()
	}()

	go func() {
		io.Copy(originChan, targetChan)
		originChan.CloseWrite()
	}()

	wg := sync.WaitGroup{}
	forward := func(sourceReqs <-chan *ssh.Request, targetChan ssh.Channel) {
		defer wg.Done()
		for ctx.Err() == nil {
			select {
			case req, ok := <-sourceReqs:
				if !ok {
					targetChan.Close()
					return
				}
				b, err := targetChan.SendRequest(req.Type, req.WantReply, req.Payload)
				_ = req.Reply(b, nil)
				if err != nil {
					return
				}
			case <-ctx.Done():
				return
			}
		}
	}

	wg.Add(2)
	go forward(maskedReqs, targetChan)
	go forward(targetReqs, originChan)

	wg.Wait()
	logs.WithField("workspaceId", session.WorkspaceID).Debug("session forward stop")
}

type heartbeatingChannel struct {
	ssh.Channel

	mux         sync.Mutex
	sawActivity bool
	t           *time.Ticker

	cancel context.CancelFunc

	requestedPty bool
}

func startHeartbeatingChannel(c ssh.Channel, heartbeat Heartbeat, session *Session) ssh.Channel {
	ctx, cancel := context.WithCancel(context.Background())
	res := &heartbeatingChannel{
		Channel: c,
		t:       time.NewTicker(30 * time.Second),
		cancel:  cancel,
	}
	go func() {
		for {
			select {
			case <-res.t.C:
				res.mux.Lock()
				if !res.sawActivity || !res.requestedPty {
					res.mux.Unlock()
					continue
				}
				res.sawActivity = false
				res.mux.Unlock()
				heartbeat.SendHeartbeat(session.WorkspaceID, false, false)
			case <-ctx.Done():
				if res.requestedPty {
					heartbeat.SendHeartbeat(session.WorkspaceID, true, false)
					logs.WithField("instanceId", session.WorkspaceID).Info("send closed heartbeat")
				}
				return
			}
		}
	}()

	return res
}

func (c *heartbeatingChannel) Read(data []byte) (written int, err error) {
	written, err = c.Channel.Read(data)
	if err == nil && written != 0 {
		c.mux.Lock()
		c.sawActivity = true
		c.mux.Unlock()
	}
	return
}

func (c *heartbeatingChannel) Close() error {
	c.t.Stop()
	c.cancel()
	return c.Channel.Close()
}
