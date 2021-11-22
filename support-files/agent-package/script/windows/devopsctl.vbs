myAgentHome = CreateObject("Scripting.FileSystemObject").GetFile(Wscript.ScriptFullName).ParentFolder.Path
CreateObject("Wscript.Shell").run "cmd /c " + myAgentHome + "\devopsDaemon.exe",0
