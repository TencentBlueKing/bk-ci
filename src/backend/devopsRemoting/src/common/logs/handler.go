package logs

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"

	"github.com/sirupsen/logrus"
)

type logLevel struct {
	Level string `json:"level"`
}

func LevelHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method == http.MethodGet {
		reportLogLevel(Logs.Logger.Level.String(), w, r)
		return
	}

	if r.Method != http.MethodPost && r.Method != http.MethodPut {
		http.Error(w, r.Method+" unsupported", http.StatusBadRequest)
		return
	}

	body, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, fmt.Sprintf("cannot decode request: %v", err), http.StatusPreconditionFailed)
		return
	}

	if len(body) == 0 {
		http.Error(w, "invalid request", http.StatusPreconditionFailed)
		return
	}

	var req logLevel
	err = json.Unmarshal(body, &req)
	if err != nil {
		http.Error(w, fmt.Sprintf("cannot decode request: %v", err), http.StatusPreconditionFailed)
		return
	}

	newLevel, err := logrus.ParseLevel(req.Level)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	Logs.Logger.SetLevel(newLevel)
	reportLogLevel(Logs.Logger.Level.String(), w, r)
}

func reportLogLevel(level string, w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	//nolint
	w.Write([]byte(fmt.Sprintf(`{"level": "%v"}`, level)))
}
