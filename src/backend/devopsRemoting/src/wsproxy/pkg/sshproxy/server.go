package sshproxy

import (
	"common/logs"
	"context"
	"crypto/subtle"
	"fmt"
	"net"
	"time"
	"wsproxy/pkg/clients"
	"wsproxy/pkg/constant"
	"wsproxy/pkg/proxy"

	"github.com/ci-plugins/crypto-go/ssh"
	"github.com/pkg/errors"
)

type Server struct {
	Heartbeater Heartbeat

	sshConfig             *ssh.ServerConfig
	workspaceInfoProvider proxy.WorkspaceInfoProvider
}

func New(signers []ssh.Signer, workspaceInfoProvider proxy.WorkspaceInfoProvider, heartbeat Heartbeat) *Server {
	server := &Server{
		workspaceInfoProvider: workspaceInfoProvider,
		Heartbeater:           &noHeartbeat{},
	}
	if heartbeat != nil {
		server.Heartbeater = heartbeat
	}

	server.sshConfig = &ssh.ServerConfig{
		ServerVersion: "SSH-2.0-DEVOPS-REMOTING-GATEWAY",
		NoClientAuth:  false,
		// 放开公钥限制
		// NoClientAuthCallback: func(conn ssh.ConnMetadata) (*ssh.Permissions, error) {
		// 	workspaceId := conn.User()
		// 	_, err := server.GetWorkspaceInfo(workspaceId)
		// 	if err != nil {
		// 		return nil, err
		// 	}

		// 	return &ssh.Permissions{
		// 		Extensions: map[string]string{
		// 			"workspaceId": workspaceId,
		// 		},
		// 	}, nil
		// },
		PublicKeyCallback: func(conn ssh.ConnMetadata, pk ssh.PublicKey) (perm *ssh.Permissions, err error) {
			workspaceId := conn.User()
			wsInfo, err := server.GetWorkspaceInfo(workspaceId)
			if err != nil {
				return nil, err
			}
			ctx, cancel := context.WithCancel(context.Background())
			defer cancel()
			ok, _ := server.VerifyPublicKey(ctx, wsInfo, pk)
			if !ok {
				return nil, ErrAuthFailed
			}
			return &ssh.Permissions{
				Extensions: map[string]string{
					"workspaceId": workspaceId,
				},
			}, nil
		},
	}
	for _, s := range signers {
		server.sshConfig.AddHostKey(s)
	}
	return server
}

func (s *Server) GetWorkspaceInfo(workspaceId string) (*proxy.WorkspaceInfo, error) {
	wsInfo := s.workspaceInfoProvider.WorkspaceInfo(workspaceId)
	if wsInfo == nil {
		return nil, ErrWorkspaceNotFound
	}
	return wsInfo, nil
}

func (s *Server) VerifyPublicKey(ctx context.Context, wsInfo *proxy.WorkspaceInfo, pk ssh.PublicKey) (bool, error) {
	for _, keyStr := range wsInfo.SSHPublicKeys {
		key, _, _, _, err := ssh.ParseAuthorizedKey([]byte(keyStr))
		if err != nil {
			continue
		}
		keyData := key.Marshal()
		pkd := pk.Marshal()
		if len(keyData) == len(pkd) && subtle.ConstantTimeCompare(keyData, pkd) == 1 {
			return true, nil
		}
	}
	return false, nil
}

func (s *Server) Serve(l net.Listener) error {
	for {
		conn, err := l.Accept()
		if err != nil {
			return err
		}

		go s.HandleConn(conn)
	}
}

const RemotingUsername = "root"

type Session struct {
	Conn *ssh.ServerConn

	WorkspaceID string

	PublicKey           ssh.PublicKey
	WorkspacePrivateKey ssh.Signer
}

func (s *Server) HandleConn(c net.Conn) {
	clientConn, clientChans, clientReqs, err := ssh.NewServerConn(c, s.sshConfig)
	if err != nil {
		c.Close()
		logs.WithError(err).Error("ssh newServerConn error")
		return
	}
	defer clientConn.Close()

	if clientConn.Permissions == nil || clientConn.Permissions.Extensions == nil || clientConn.Permissions.Extensions["workspaceId"] == "" {
		return
	}
	workspaceId := clientConn.Permissions.Extensions["workspaceId"]
	wsInfo := s.workspaceInfoProvider.WorkspaceInfo(workspaceId)
	if wsInfo == nil {
		return
	}
	ctx, cancel := context.WithTimeout(context.Background(), time.Second*5)
	key, err := s.GetWorkspaceSSHKey(ctx, wsInfo.IPAddress)
	if err != nil {
		cancel()
		logs.WithField("workspaceId", wsInfo.WorkspaceID).WithError(err).Error("failed to create private pair in workspace")
		return
	}
	cancel()

	session := &Session{
		Conn:                clientConn,
		WorkspaceID:         workspaceId,
		WorkspacePrivateKey: key,
	}
	remoteAddr := fmt.Sprintf("%s:%d", wsInfo.IPAddress, constant.RemotingSSHPort)
	conn, err := net.Dial("tcp", remoteAddr)
	if err != nil {
		logs.WithField("workspaceId", wsInfo.WorkspaceID).WithField("workspaceIP", wsInfo.IPAddress).WithError(err).Error("dail failed")
		return
	}
	defer conn.Close()

	workspaceConn, workspaceChans, workspaceReqs, err := ssh.NewClientConn(conn, remoteAddr, &ssh.ClientConfig{
		HostKeyCallback: ssh.InsecureIgnoreHostKey(),
		User:            RemotingUsername,
		Auth: []ssh.AuthMethod{
			ssh.PublicKeysCallback(func() (signers []ssh.Signer, err error) {
				return []ssh.Signer{key}, nil
			}),
		},
		Timeout: 10 * time.Second,
	})
	if err != nil {
		logs.WithField("workspaceId", wsInfo.WorkspaceID).WithField("workspaceIP", wsInfo.IPAddress).WithError(err).Error("connect failed")
		return
	}

	s.Heartbeater.SendHeartbeat(wsInfo.WorkspaceID, false, true)
	ctx, cancel = context.WithCancel(context.Background())

	forwardRequests := func(reqs <-chan *ssh.Request, targetConn ssh.Conn) {
		for req := range reqs {
			result, payload, err := targetConn.SendRequest(req.Type, req.WantReply, req.Payload)
			if err != nil {
				continue
			}
			_ = req.Reply(result, payload)
		}
	}
	// client -> workspace global request forward
	go forwardRequests(clientReqs, workspaceConn)
	// workspce -> client global request forward
	go forwardRequests(workspaceReqs, clientConn)

	go func() {
		for newChannel := range workspaceChans {
			go s.ChannelForward(ctx, session, clientConn, newChannel)
		}
	}()

	go func() {
		for newChannel := range clientChans {
			go s.ChannelForward(ctx, session, workspaceConn, newChannel)
		}
	}()

	go func() {
		clientConn.Wait()
		cancel()
	}()
	go func() {
		workspaceConn.Wait()
		cancel()
	}()
	<-ctx.Done()
	workspaceConn.Close()
	clientConn.Close()
	cancel()
}

func (s *Server) GetWorkspaceSSHKey(ctx context.Context, workspaceIP string) (ssh.Signer, error) {
	keyInfo, err := clients.Remoting.CreateSSHKeyPair(ctx, workspaceIP)
	if err != nil {
		return nil, errors.Errorf("failed getting ssh key pair info from devops remotiong: %s", err.Error())
	}
	key, err := ssh.ParsePrivateKey([]byte(keyInfo.PrivateKey))
	if err != nil {
		return nil, errors.Errorf("failed parse private key: %s", err.Error())
	}
	return key, nil
}
