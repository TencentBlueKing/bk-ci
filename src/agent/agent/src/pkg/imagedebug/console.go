package imagedebug

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/exec"
	"time"

	"github.com/creack/pty"
	"github.com/gorilla/websocket"
)

// ConsoleCopywritingFailed is a response string
var ConsoleCopywritingFailed = []string{
	"#######################################################################\r\n",
	"#                    Welcome To BKDevOps Console                      #\r\n",
	"#                该环境已经处于调试状态,禁止同时连接多个会话          #\r\n",
	"#######################################################################\r\n",
}

const (
	writeWait  = 10 * time.Second
	pongWait   = 60 * time.Second
	pingPeriod = (pongWait * 9) / 10
)

type errMsg struct {
	Msg string `json:"msg,omitempty"`
}

type wsConn struct {
	conn *websocket.Conn
}

func newWsConn(conn *websocket.Conn) *wsConn {
	return &wsConn{
		conn: conn,
	}
}

// Read 用于常见IO
func (c *wsConn) Read(p []byte) (n int, err error) {
	_, rc, err := c.conn.NextReader()
	if err != nil {
		return 0, err
	}
	return rc.Read(p)
}

// Write 用于常见IO
func (c *wsConn) Write(p []byte) (n int, err error) {
	wc, err := c.conn.NextWriter(websocket.BinaryMessage)
	if err != nil {
		return 0, err
	}
	defer wc.Close()
	return wc.Write(p)
}

// ResponseJSON response to client
func ResponseJSON(w http.ResponseWriter, status int, v interface{}) error {
	w.Header().Set("Content-Type", "application/json")

	w.WriteHeader(status)
	data, err := json.Marshal(v)
	if err != nil {
		return err
	}
	_, err = w.Write(data)
	return err
}

// StartExec start a websocket exec
func (m *manager) StartExec(w http.ResponseWriter, r *http.Request, conf *WebSocketConfig) {
	imageDebugLogs.Debug(fmt.Sprintf("start exec for container exec_id %s", conf.ExecID))

	upgrader := websocket.Upgrader{
		EnableCompression: true,
	}
	upgrader.CheckOrigin = func(r *http.Request) bool {
		return true
	}

	if !websocket.IsWebSocketUpgrade(r) {
		ResponseJSON(w, http.StatusBadRequest, nil)
		return
	}

	ws, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		ResponseJSON(w, http.StatusBadRequest, errMsg{err.Error()})
		return
	}

	defer func() {
		ws.Close()
		m.doneChan.SafeClose()
	}()

	if m.conf.IsOneSeesion {
		m.Lock()
		_, ok := m.connectedContainers[conf.ContainerID]
		if ok {
			imageDebugLogs.Warnf("container %s has established connection", conf.ContainerID)

			for _, i := range ConsoleCopywritingFailed {
				err := ws.WriteMessage(websocket.TextMessage, []byte(i))
				if err != nil {
					m.Unlock()
					ResponseJSON(w, http.StatusInternalServerError, errMsg{err.Error()})
					return
				}

			}
			m.Unlock()
			err = fmt.Errorf("container %s has established connection", conf.ContainerID)
			ResponseJSON(w, http.StatusBadRequest, errMsg{err.Error()})
			return
		}

		m.connectedContainers[conf.ContainerID] = true
		m.Unlock()
		defer func() {
			m.Lock()
			delete(m.connectedContainers, conf.ContainerID)
			m.Unlock()
		}()
	}

	ws.SetCloseHandler(nil)
	ws.SetPingHandler(nil)
	// ws.SetReadDeadline(time.Now().Add(pongWait))
	// ws.SetPongHandler(func(string) error {
	//	ws.SetReadDeadline(time.Now().Add(pongWait))
	//	return nil
	// })

	ticker := time.NewTicker(pingPeriod)
	defer ticker.Stop()
	go func() {
		for {
			select {
			case <-ticker.C:
				if err := ws.WriteMessage(websocket.PingMessage, []byte{}); err != nil {
					return
				}
			}
		}
	}()

	err = m.startExec(newWsConn(ws), conf)
	if err != nil {
		imageDebugLogs.WithError(err).Warnf("start exec failed for container %s", conf.ContainerID)
		ResponseJSON(w, http.StatusBadRequest, errMsg{err.Error()})
		return
	}

	ResponseJSON(w, http.StatusSwitchingProtocols, nil)
}

// CreateExec xxx
func (m *manager) CreateExec(w http.ResponseWriter, r *http.Request, conf *WebSocketConfig) {
	imageDebugLogs.Debug(fmt.Sprintf("start create exec for container %s", conf.ContainerID))
	execRef, err := m.CreateExecNoHttp(conf)
	if err != nil {
		ResponseJSON(w, http.StatusBadRequest, errMsg{err.Error()})
		return
	}
	ResponseJSON(w, http.StatusOK, execRef)
}

func (m *manager) CreateExecNoHttp(conf *WebSocketConfig) (*ExecRef, error) {
	if conf == nil || conf.ContainerID == "" {
		return nil, fmt.Errorf("container id is required")
	}
	id := fmt.Sprintf("exec-%d", time.Now().UnixNano())
	m.Lock()
	m.execSessions[id] = &execSession{conf: conf}
	m.Unlock()
	return &ExecRef{ID: id}, nil
}

func (m *manager) startExec(ws io.ReadWriter, conf *WebSocketConfig) error {
	m.RLock()
	session, ok := m.execSessions[conf.ExecID]
	m.RUnlock()
	if !ok || session == nil || session.conf == nil {
		return fmt.Errorf("exec session %s not found", conf.ExecID)
	}

	args := []string{"exec"}
	if m.conf.Privilege {
		args = append(args, "--privileged")
	}
	if m.conf.Tty {
		args = append(args, "-it")
	} else {
		args = append(args, "-i")
	}
	if session.conf.User != "" {
		args = append(args, "-u", session.conf.User)
	}
	args = append(args, session.conf.ContainerID)
	args = append(args, session.conf.Cmd...)

	cmd := exec.Command(m.runner.Binary(), args...)

	f, err := pty.Start(cmd)
	if err != nil {
		return err
	}

	m.Lock()
	session.cmd = cmd
	session.pty = f
	m.Unlock()
	defer func() {
		f.Close()
		m.Lock()
		delete(m.execSessions, conf.ExecID)
		m.Unlock()
	}()

	errCh := make(chan error, 2)
	go func() {
		_, err := io.Copy(f, ws)
		errCh <- err
	}()
	go func() {
		_, err := io.Copy(ws, f)
		errCh <- err
	}()

	select {
	case err := <-errCh:
		return err
	case <-m.doneChan.C:
		return nil
	}
}

// ResizeExec xxx
func (m *manager) ResizeExec(w http.ResponseWriter, r *http.Request, conf *WebSocketConfig) {
	imageDebugLogs.Debug(fmt.Sprintf("start resize for container exec_id %s", conf.ExecID))
	m.RLock()
	session, ok := m.execSessions[conf.ExecID]
	m.RUnlock()
	if !ok || session == nil || session.pty == nil {
		ResponseJSON(w, http.StatusBadRequest, errMsg{"exec session not found"})
		return
	}
	ptmx, ok := session.pty.(*os.File)
	if !ok {
		ResponseJSON(w, http.StatusBadRequest, errMsg{"exec session pty invalid"})
		return
	}
	err := pty.Setsize(ptmx, &pty.Winsize{Rows: uint16(conf.Height), Cols: uint16(conf.Width)})
	if err != nil {
		ResponseJSON(w, http.StatusBadRequest, errMsg{err.Error()})
		return
	}

	ResponseJSON(w, http.StatusOK, nil)
}
