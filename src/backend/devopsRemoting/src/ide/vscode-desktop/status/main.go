package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"net/url"
	"os"
)

func main() {
	if len(os.Args) < 2 {
		fmt.Printf("Usage: %s <port> [<link label>] [<schema>]\n", os.Args[0])
		os.Exit(1)
	}
	port := os.Args[1]

	label := "Open in VS Code Desktop"
	if len(os.Args) > 2 {
		label = os.Args[2]
	}

	schema := "vscode"
	if len(os.Args) > 3 {
		schema = os.Args[3]
	}

	http.HandleFunc("/status", func(w http.ResponseWriter, _ *http.Request) {

		// TODO: 目前先写死用来调试
		workspaceLocation := "/data/landun/workspace"
		link := url.URL{
			Scheme: schema,
			Host:   "remoting.remoting-desktop",
			Path:   workspaceLocation,
		}

		response := make(map[string]string)
		response["link"] = link.String()
		response["label"] = label
		response["clientID"] = schema
		response["kind"] = "code-desktop"
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(response)
	})

	fmt.Printf("Starting status proxy for desktop IDE at port %s\n", port)
	if err := http.ListenAndServe(fmt.Sprintf(":%s", port), nil); err != nil {
		log.Fatal(err)
	}
}
