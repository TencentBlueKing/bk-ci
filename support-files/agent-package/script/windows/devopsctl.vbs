myAgentHome = CreateObject("Scripting.FileSystemObject").GetFile(Wscript.ScriptFullName).ParentFolder.Path
CreateObject("Wscript.Shell").run "cmd /c " + myAgentHome + "\devopsDaemon.exe",0
CreateObject("Wscript.Shell").run "cmd /c wmic process where name='devopsDaemon.exe' CALL setpriority 'normal'",0
CreateObject("Wscript.Shell").run "cmd /c wmic process where name='devopsAgent.exe' CALL setpriority 'normal'",0
