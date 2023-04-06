package service

import (
	"common/logs"
	"common/types"
	"context"
	"fmt"
	"remoting/pkg/config"
	"remoting/pkg/modules/terminal"
	remoteTypes "remoting/pkg/types"
	"sync"
	"time"
)

type CommandManager struct {
	cfg        *config.Config
	termSrv    *terminal.MuxTerminalService
	cstate     ContentState
	Ready      chan struct{}
	commands   []*Command
	rwLock     sync.RWMutex
	devfileSrv *config.DevfileConfigService
}

func NewCommandManger(
	config *config.Config,
	terminalService *terminal.MuxTerminalService,
	contentState ContentState,
	devfileSrv *config.DevfileConfigService,
) *CommandManager {
	return &CommandManager{
		cfg:        config,
		termSrv:    terminalService,
		cstate:     contentState,
		Ready:      make(chan struct{}),
		devfileSrv: devfileSrv,
	}
}

func (cm *CommandManager) Status() []*remoteTypes.CommandStatus {
	cm.rwLock.RLock()
	defer cm.rwLock.RUnlock()

	return cm.getStatus()
}

// getStatus 返回命令状态列表
func (cm *CommandManager) getStatus() []*remoteTypes.CommandStatus {
	status := make([]*remoteTypes.CommandStatus, 0, len(cm.commands))
	for _, c := range cm.commands {
		status = append(status, &c.CommandStatus)
	}
	return status
}

func (cm *CommandManager) updateState(doUpdate func() (changed bool)) {
	cm.rwLock.Lock()
	defer cm.rwLock.Unlock()

	changed := doUpdate()
	if !changed {
		return
	}
}

func (cm *CommandManager) setTaskState(t *Command, newState remoteTypes.CommandState) {
	cm.updateState(func() bool {
		if t.State == newState {
			return false
		}

		t.State = newState
		return true
	})
}

// run 运行 commandManager
func (cm *CommandManager) Run(ctx context.Context, wg *sync.WaitGroup) {
	defer wg.Done()
	defer logs.Debug("commandManager shutdown")

	cm.init(ctx)
	logs.Debugf("commands: %v", cm.commands)

	for _, c := range cm.commands {
		log := logs.WithField("command", c.command)
		log.Info("starting a command terminal...")

		resp, err := cm.termSrv.OpenWithOptions(ctx, terminal.TermOptions{
			ReadTimeout: 5 * time.Second,
			Title:       string(c.commandType),
		})
		if err != nil {
			log.Error("cannot open new command terminal ", err)
			cm.setTaskState(c, remoteTypes.CommandClosed)
			continue
		}

		log = log.WithField("terminal", resp.Terminal.Alias)
		term, ok := cm.termSrv.Mux.Get(resp.Terminal.Alias)
		if !ok {
			log.Error("cannot find a command terminal ")
			cm.setTaskState(c, remoteTypes.CommandClosed)
			continue
		}

		log = log.WithField("pid", term.Command.Process.Pid)
		log.Info("command terminal has been started")
		cm.updateState(func() bool {
			c.Terminal = resp.Terminal.Alias
			c.State = remoteTypes.CommandRunning
			return true
		})

		go func(term *terminal.Term) {
			state, err := term.Wait()
			log.WithField("state", state).WithError(err).Info("command terminal has been closed")
			cm.setTaskState(c, remoteTypes.CommandClosed)
		}(term)

		term.PTY.Write([]byte(c.command + "\n"))
	}
}

type Command struct {
	remoteTypes.CommandStatus
	command     string
	commandType remoteTypes.CommandType
}

func (cm *CommandManager) init(ctx context.Context) {
	defer close(cm.Ready)

	select {
	case <-ctx.Done():
		return
	case <-cm.cstate.ContentReady():
	}

	devfile := <-cm.devfileSrv.Observe(ctx)
	if devfile == nil {
		logs.Warn("commands devfile is null")
		return
	}
	commands := devfile.Commands
	if commands == nil {
		logs.Warn("commands is null")
		return
	}

	// add command
	addCommand(cm, commands)
}

func addCommand(cm *CommandManager, commands *types.Commands) {
	if cm.cfg.WorkSpace.WorkspaceFirstCreate == "true" && commands.PostCreateCommand != "" {
		cm.commands = append(cm.commands, &Command{
			CommandStatus: remoteTypes.CommandStatus{
				Id:    remoteTypes.PostCreateCommand,
				State: remoteTypes.CommandOpening,
			},
			command:     getCommand(commands.PostCreateCommand),
			commandType: remoteTypes.PostCreateCommand,
		})
	}

	cm.commands = append(cm.commands, &Command{
		CommandStatus: remoteTypes.CommandStatus{
			Id:    remoteTypes.PostStartCommand,
			State: remoteTypes.CommandOpening,
		},
		command:     getCommand(commands.PostStartCommand),
		commandType: remoteTypes.PostStartCommand,
	})
}

func getCommand(command string) string {
	return fmt.Sprintf("\n%s\n", command)
}
