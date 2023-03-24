package ssh

import (
	"bufio"
	"bytes"
	"common/logs"
	"context"
	"fmt"
	"io/ioutil"
	"net"
	"os"
	"os/exec"
	"path/filepath"
	apitypes "remoting/api"
	"remoting/pkg/config"
	"remoting/pkg/constant"
	"remoting/pkg/modules/user"
	"strings"
	"sync"

	"github.com/pkg/errors"
)

type sshServer struct {
	ctx     context.Context
	cfg     *config.Config
	envvars []string

	sshkey string
}

func StartSSHServer(ctx context.Context, cfg *config.Config, wg *sync.WaitGroup, childProcEnvvars []string) {
	defer wg.Done()

	go func() {
		ssh, err := newSSHServer(ctx, cfg, childProcEnvvars)
		if err != nil {
			logs.WithError(err).Error("err creating SSH server")
			return
		}
		configureSSHDefaultDir(cfg)
		configureSSHMessageOfTheDay()
		err = ssh.listenAndServe()
		if err != nil {
			logs.WithError(err).Error("err starting SSH server")
		}
	}()
}

// ListenAndServe listens on the TCP network address laddr and then handle packets on incoming connections.
func (s *sshServer) listenAndServe() error {
	listener, err := net.Listen("tcp", fmt.Sprintf(":%v", s.cfg.Config.SSHPort))
	if err != nil {
		return err
	}

	for {
		conn, err := listener.Accept()
		if err != nil {
			logs.WithError(err).Error("listening for SSH connection")
			continue
		}

		go s.handleConn(s.ctx, conn)
	}
}

func (s *sshServer) handleConn(ctx context.Context, conn net.Conn) {
	bin, err := os.Executable()
	if err != nil {
		return
	}

	defer conn.Close()

	openssh := filepath.Join(filepath.Dir(bin), "ssh", "sshd")
	if _, err := os.Stat(openssh); err != nil {
		return
	}

	args := []string{
		"-ieD", "-f/dev/null",
		"-oProtocol 2",
		"-oAllowUsers root",
		"-oPasswordAuthentication no",
		"-oChallengeResponseAuthentication no",
		"-oPermitRootLogin yes",
		"-oLoginGraceTime 20",
		"-oPrintLastLog no",
		"-oPermitUserEnvironment yes",
		"-oHostKey " + s.sshkey,
		"-oPidFile none",
		"-oUseDNS no", // Disable DNS lookups.
		"-oSubsystem sftp internal-sftp",
		"-oStrictModes no", // don't care for home directory and file permissions
		"-oLogLevel DEBUG", // enabled DEBUG mode by default
	}

	envs := make([]string, 0)
	for _, env := range s.envvars {
		s := strings.SplitN(env, "=", 2)
		if len(s) != 2 {
			continue
		}
		envs = append(envs, fmt.Sprintf("%s=%s", s[0], fmt.Sprintf("\"%s\"", strings.ReplaceAll(strings.ReplaceAll(s[1], `\`, `\\`), `"`, `\"`))))
	}
	if len(envs) > 0 {
		args = append(args, fmt.Sprintf("-oSetEnv %s", strings.Join(envs, " ")))
	}

	socketFD, err := conn.(*net.TCPConn).File()
	if err != nil {
		logs.WithError(err).Error("cannot start SSH server")
		return
	}
	defer socketFD.Close()

	logs.Debugf("sshd flags args: %v", args)
	cmd := exec.CommandContext(ctx, openssh, args...)
	cmd = user.RunAsDevopsRemotingUser(cmd)
	cmd.Env = s.envvars
	cmd.ExtraFiles = []*os.File{socketFD}
	cmd.Stderr = os.Stderr
	cmd.Stdin = bufio.NewReader(socketFD)
	cmd.Stdout = bufio.NewWriter(socketFD)

	err = cmd.Start()
	if err != nil {
		logs.WithError(err).Error("cannot start SSH server")
		return
	}

	done := make(chan error, 1)
	go func() {
		done <- cmd.Wait()
	}()

	logs.Debug("sshd started")

	select {
	case <-ctx.Done():
		if cmd.Process != nil {
			_ = cmd.Process.Kill()
		}
		return
	case err = <-done:
		if err != nil {
			logs.WithError(err).Error("SSH server stopped ")
		}
	}
}

func newSSHServer(ctx context.Context, cfg *config.Config, envvars []string) (*sshServer, error) {
	bin, err := os.Executable()
	if err != nil {
		return nil, errors.Errorf("cannot find executable path: %s", err.Error())
	}

	sshkey := filepath.Join(filepath.Dir(bin), "ssh", "sshkey")
	if _, err := os.Stat(sshkey); err != nil {
		err := prepareSSHKey(ctx, sshkey)
		if err != nil {
			return nil, errors.Errorf("unexpected error creating SSH key: %s", err.Error())
		}
	}
	err = ensureSSHDir(cfg)
	if err != nil {
		return nil, errors.Errorf("unexpected error creating SSH dir: %s", err.Error())
	}

	return &sshServer{
		ctx:     ctx,
		cfg:     cfg,
		sshkey:  sshkey,
		envvars: envvars,
	}, nil
}

type SSHService struct {
	privateKey string
	publicKey  string
}

// createSSHKeyPair 创建ssh登录使用的公私钥
func (s *SSHService) CreateSSHKeyPair() (response *apitypes.CreateSSHKeyPairResp, err error) {
	home := constant.RemotingUserHome
	if s.privateKey != "" && s.publicKey != "" {
		checkKey := func() error {
			data, err := os.ReadFile(filepath.Join(home, ".ssh/authorized_keys"))
			if err != nil {
				return errors.Wrap(err, "cannot read file ~/.ssh/authorized_keys")
			}
			if !bytes.Contains(data, []byte(s.publicKey)) {
				return errors.Errorf("not found special publickey")
			}
			return nil
		}
		err := checkKey()
		if err == nil {
			return &apitypes.CreateSSHKeyPairResp{
				PrivateKey: s.privateKey,
			}, nil
		}
		logs.WithError(err).Error("check authorized_keys failed, will recreate")
	}

	dir, err := os.MkdirTemp(os.TempDir(), "ssh-key-*")
	if err != nil {
		return nil, errors.Errorf("cannot create tmpfile: %s", err.Error())
	}

	err = prepareSSHKey(context.Background(), filepath.Join(dir, "ssh"))
	if err != nil {
		return nil, errors.Errorf("cannot create ssh key pair: %s", err.Error())
	}

	bPublic, err := os.ReadFile(filepath.Join(dir, "ssh.pub"))
	if err != nil {
		return nil, errors.Errorf("cannot read publickey: %s", err.Error())
	}

	bPrivate, err := os.ReadFile(filepath.Join(dir, "ssh"))
	if err != nil {
		return nil, errors.Errorf("cannot read privatekey: %s", err.Error())
	}

	err = os.MkdirAll(filepath.Join(home, ".ssh"), 0o700)
	if err != nil {
		return nil, errors.Errorf("cannot create dir ~/.ssh/: %s", err.Error())
	}

	f, err := os.OpenFile(filepath.Join(home, ".ssh/authorized_keys"), os.O_APPEND|os.O_CREATE|os.O_RDWR, 0o600)
	if err != nil {
		return nil, errors.Errorf("cannot open file ~/.ssh/authorized_keys: %s", err.Error())
	}

	_, err = f.Write(bPublic)
	if err != nil {
		return nil, errors.Errorf("cannot write file ~.ssh/authorized_keys: %s", err.Error())
	}

	// TODO: 暂时使用root
	// err = os.Chown(filepath.Join(home, ".ssh/authorized_keys"), devopsRemotingUID, devopsRemotingGID)
	// if err != nil {
	// 	return nil, errors.Errorf("cannot chown SSH authorized_keys file: %s", err.Error())
	// }

	s.privateKey = string(bPrivate)
	s.publicKey = string(bPublic)
	return &apitypes.CreateSSHKeyPairResp{PrivateKey: s.privateKey}, err
}

// prepareSSHKey 生成ssh公钥和私钥
func prepareSSHKey(_ context.Context, sshkey string) error {
	bin, err := os.Executable()
	if err != nil {
		return errors.Errorf("cannot find executable path: %s", err.Error())
	}

	openssh := filepath.Join(filepath.Dir(bin), "ssh", "sshd")
	if _, err := os.Stat(openssh); err != nil {
		return errors.Errorf("cannot locate sshd binary in path %v", openssh)
	}

	sshkeygen := filepath.Join(filepath.Dir(bin), "ssh", "ssh-keygen")
	if _, err := os.Stat(sshkeygen); err != nil {
		return errors.Errorf("cannot locate ssh-keygen (path %v)", sshkeygen)
	}

	keycmd := exec.Command(sshkeygen, "-t", "ecdsa", "-q", "-N", "", "-f", sshkey)
	// We need to force HOME because the DevopsRemoting user might not have existed at the start of the container
	// which makes the container runtime set an invalid HOME value.
	keycmd.Env = func() []string {
		env := os.Environ()
		res := make([]string, 0, len(env))
		for _, e := range env {
			if strings.HasPrefix(e, "HOME=") {
				e = "HOME=/root"
			}
			res = append(res, e)
		}
		return res
	}()

	_, err = keycmd.CombinedOutput()
	if err != nil {
		return errors.Errorf("cannot create SSH hostkey file: %s", err.Error())
	}

	// TODO: 暂时使用root
	// err = os.Chown(sshkey, devopsRemotingUID, devopsRemotingGID)
	if err != nil {
		return errors.Errorf("cannot chown SSH hostkey file: %s", err.Error())
	}

	return nil
}

func ensureSSHDir(_ *config.Config) error {
	home := constant.RemotingUserHome

	d := filepath.Join(home, ".ssh")
	err := os.MkdirAll(d, 0o700)
	if err != nil {
		return errors.Errorf("cannot create $HOME/.ssh: %s", err.Error())
	}
	// TODO: 暂时使用root
	// _ = exec.Command("chown", "-R", fmt.Sprintf("%d:%d", devopsRemotingUID, devopsRemotingGID), d).Run()

	return nil
}

func configureSSHDefaultDir(cfg *config.Config) {
	if cfg.WorkSpace.GitRepoRootPath == "" {
		logs.Error("cannot configure ssh default dir with empty repo root")
		return
	}
	file, err := os.OpenFile(fmt.Sprintf("%s/.bash_profile", constant.RemotingUserHome), os.O_WRONLY|os.O_APPEND|os.O_CREATE, 0o644)
	if err != nil {
		logs.WithError(err).Error("cannot write .bash_profile", err)
	}
	defer file.Close()
	if _, err := file.WriteString(fmt.Sprintf("\nif [[ -n $SSH_CONNECTION ]]; then cd \"%s\"; fi\n", cfg.WorkSpace.GitRepoRootPath)); err != nil {
		logs.WithError(err).Error("write .bash_profile failed", err)
	}
}

func configureSSHMessageOfTheDay() {
	msg := []byte("Welcome to DevopsRemoting~ \n")

	if err := ioutil.WriteFile("/etc/motd", msg, 0o644); err != nil {
		logs.WithError(err).Error("write /etc/motd failed")
	}
}
