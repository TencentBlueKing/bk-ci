#!/usr/bin/env python3
"""
iWiki MCP Server 通用客户端脚本

支持连接 MCP Server 并调用任意 tool，以及独立上传文件导入

使用方法:
  1. 设置环境变量: export TAI_PAT_TOKEN="your_token_here"
  2. 运行脚本:
     - 列出所有工具: python connect_mcp.py list
     - 调用工具: python connect_mcp.py call <tool_name> [args_json]
     - 从文件读取参数: python connect_mcp.py call <tool_name> --file <json_file>
     - 从 stdin 读取参数: echo '{"docid":"123"}' | python connect_mcp.py call <tool_name> --stdin
     - 上传文件导入: python connect_mcp.py upload <file_path> <parent_id> [options]
     - 交互模式: python connect_mcp.py

示例:
  python connect_mcp.py list                           # 列出所有可用工具
  python connect_mcp.py call getDocument '{"docid": "4017403457"}'
  python connect_mcp.py call searchDocument '{"query": "项目文档", "offset": 20}'
  python connect_mcp.py call aiSearchDocument '{"query": "项目文档", "limit": 5}'
  python connect_mcp.py call metadata '{"docid": "4017403457"}'
  python connect_mcp.py call getSpaceInfoByKey '{"spaceKey": "devcloud"}'

  # 大参数场景（突破命令行长度限制）:
  python connect_mcp.py call updateDocument --file args.json   # 从文件读取参数
  cat args.json | python connect_mcp.py call updateDocument --stdin  # 从 stdin 读取参数

  # 文件上传导入:
  python connect_mcp.py upload ./doc.md 4017403457                     # 上传 Markdown 文件到指定父目录
  python connect_mcp.py upload ./doc.docx 4017403457 --task-type md_import  # 指定任务类型
  python connect_mcp.py upload ./doc.md 4017403457 --no-cover          # 不覆盖同名文档

MCP Server URL: https://prod.mcp.it.woa.com/app_iwiki_mcp/mcp3
"""

import os
import sys
import json
import io
import mimetypes
import uuid
import requests
from typing import Optional, Any
from urllib.parse import quote as url_quote

# 确保 stdout/stderr 使用 UTF-8 编码（解决 Windows GBK 环境下 emoji 输出报错）
if sys.stdout.encoding and sys.stdout.encoding.lower() != "utf-8":
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
if sys.stderr.encoding and sys.stderr.encoding.lower() != "utf-8":
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

# MCP Server 配置
MCP_URL = "https://prod.mcp.it.woa.com/app_iwiki_mcp/mcp3"
# 文件上传导入端点（与 MCP Server 同源的 HTTP 端点）
IMPORT_URL = "https://prod.mcp.it.woa.com/app_iwiki_mcp/import"


def parse_tool_args(argv_remaining: list) -> dict:
    """
    从命令行参数、文件或 stdin 解析工具调用参数。
    
    支持三种模式:
      1. 直接 JSON 字符串:  '{"key": "value"}'
      2. 从文件读取:        --file <path.json>
      3. 从 stdin 读取:     --stdin
    
    Args:
        argv_remaining: 工具名之后的剩余命令行参数列表
    
    Returns:
        解析后的参数字典
    """
    if not argv_remaining:
        return {}
    
    flag = argv_remaining[0]
    
    # 模式 2: 从文件读取
    if flag == "--file":
        if len(argv_remaining) < 2:
            print("❌ --file 需要指定 JSON 文件路径")
            sys.exit(1)
        file_path = argv_remaining[1]
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                return json.load(f)
        except FileNotFoundError:
            print(f"❌ 文件不存在: {file_path}")
            sys.exit(1)
        except json.JSONDecodeError as e:
            print(f"❌ 文件 JSON 解析错误: {e}")
            sys.exit(1)
    
    # 模式 3: 从 stdin 读取
    if flag == "--stdin":
        try:
            stdin_data = sys.stdin.read()
            if not stdin_data.strip():
                print("❌ stdin 无数据，请通过管道传入 JSON")
                sys.exit(1)
            return json.loads(stdin_data)
        except json.JSONDecodeError as e:
            print(f"❌ stdin JSON 解析错误: {e}")
            sys.exit(1)
    
    # 模式 1: 直接 JSON 字符串（拼接所有剩余参数，兼容 shell 拆分空格的情况）
    try:
        return json.loads(" ".join(argv_remaining))
    except json.JSONDecodeError as e:
        print(f"❌ 参数 JSON 解析错误: {e}")
        sys.exit(1)


class MCPClient:
    """MCP 客户端，提供通用的工具调用能力"""
    
    def __init__(self, token: str, url: str = MCP_URL):
        self.token = token
        self.url = url
        self.request_id = 0
        self.tools_cache: Optional[list] = None
        self.initialized = False
    
    def _next_request_id(self) -> int:
        """生成下一个请求 ID"""
        self.request_id += 1
        return self.request_id
    
    def _send_request(self, method: str, params: dict = None) -> dict:
        """
        发送 MCP 请求
        
        Args:
            method: MCP 方法名
            params: 请求参数
        
        Returns:
            响应数据
        """
        if params is None:
            params = {}
        
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {self.token}",
            "Accept": "application/json, text/event-stream"
        }
        
        payload = {
            "jsonrpc": "2.0",
            "id": self._next_request_id(),
            "method": method,
            "params": params
        }
        
        response = requests.post(self.url, headers=headers, json=payload, timeout=60)
        
        # 检查 HTTP 状态码
        if response.status_code != 200:
            return {
                "error": {
                    "code": response.status_code,
                    "message": f"HTTP {response.status_code}: {response.text[:500]}"
                }
            }
        
        # 处理 SSE 格式的响应
        content_type = response.headers.get("Content-Type", "")
        content = response.text
        
        if "text/event-stream" in content_type or (
            content.lstrip().startswith("event:") or content.lstrip().startswith("data:")
        ):
            last_parsed = None
            # SSE 可能包含多个事件块，每块以空行分隔
            # 每个事件块内可能有多个 data: 行，需要拼接
            events = content.split("\n\n")
            for event in events:
                data_parts = []
                for line in event.split("\n"):
                    if line.startswith("data:"):
                        data_parts.append(line[5:].strip())
                if data_parts:
                    json_str = "".join(data_parts)
                    if json_str:
                        try:
                            parsed = json.loads(json_str)
                            last_parsed = parsed
                            # 优先返回包含 result 或 error 的 JSON-RPC 响应
                            if isinstance(parsed, dict) and ("result" in parsed or "error" in parsed):
                                return parsed
                        except json.JSONDecodeError:
                            continue
            
            # 回退：返回最后一个成功解析的事件
            if last_parsed is not None:
                return last_parsed
        
        return response.json()
    
    def initialize(self) -> dict:
        """初始化 MCP 连接"""
        response = self._send_request(
            "initialize",
            {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {
                    "name": "iwiki-mcp-client",
                    "version": "1.0.0"
                }
            }
        )
        
        if "error" not in response:
            self.initialized = True
        
        return response
    
    def list_tools(self, force_refresh: bool = False) -> list:
        """
        获取所有可用工具列表
        
        Args:
            force_refresh: 是否强制刷新缓存
        
        Returns:
            工具列表
        """
        if self.tools_cache and not force_refresh:
            return self.tools_cache
        
        response = self._send_request("tools/list", {})
        
        if "result" in response and "tools" in response["result"]:
            self.tools_cache = response["result"]["tools"]
            return self.tools_cache
        
        return []
    
    def get_tool_info(self, tool_name: str) -> Optional[dict]:
        """
        获取指定工具的信息
        
        Args:
            tool_name: 工具名称
        
        Returns:
            工具信息，如果不存在返回 None
        """
        tools = self.list_tools()
        for tool in tools:
            if tool["name"] == tool_name:
                return tool
        return None
    
    def call_tool(self, tool_name: str, arguments: dict = None) -> dict:
        """
        调用指定工具
        
        Args:
            tool_name: 工具名称
            arguments: 工具参数
        
        Returns:
            工具调用结果
        """
        if arguments is None:
            arguments = {}
        
        return self._send_request(
            "tools/call",
            {
                "name": tool_name,
                "arguments": arguments
            }
        )
    
    def __getattr__(self, name: str):
        """
        动态方法调用，支持 client.tool_name(args) 语法
        
        例如:
            client.getDocument(docid="123")
            client.aiSearchDocument(query="test", limit=5)
        """
        if name.startswith("_"):
            raise AttributeError(f"'{type(self).__name__}' object has no attribute '{name}'")
        def tool_caller(**kwargs):
            return self.call_tool(name, kwargs)
        return tool_caller
    
    def upload_file(
        self,
        file_path: str,
        parent_id: int,
        task_type: str = "md_import",
        cover: bool = True,
        import_url: str = IMPORT_URL,
    ) -> dict:
        """
        上传文件并导入到 iWiki 指定目录。
        
        该方法调用 MCP Server 提供的 /import HTTP 端点，完整流程包括:
          1. 上传文件到服务端
          2. 服务端获取预签名 URL 并上传至 COS
          3. 服务端启动导入任务并轮询等待完成
        
        Args:
            file_path:   本地文件路径
            parent_id:   父文档/目录 ID，文件将导入到该目录下
            task_type:   导入任务类型，默认 'md_import'
            cover:       是否覆盖同名文档，默认 True
            import_url:  导入端点 URL，默认使用 IMPORT_URL
        
        Returns:
            服务端返回的 JSON 结果，包含 success、msg、data 字段
        """
        if not os.path.isfile(file_path):
            return {"success": False, "msg": f"文件不存在: {file_path}"}
        
        file_size = os.path.getsize(file_path)
        if file_size > 50 * 1024 * 1024:
            return {"success": False, "msg": f"文件过大 ({file_size / 1024 / 1024:.1f}MB)，最大支持 50MB"}
        
        filename = os.path.basename(file_path)
        content_type = mimetypes.guess_type(file_path)[0] or "application/octet-stream"
        
        headers = {
            "Authorization": f"Bearer {self.token}",
        }
        
        boundary = f"iwiki-mcp-{uuid.uuid4().hex}"
        body = io.BytesIO()
        
        def write_text(s: str):
            body.write(s.encode("utf-8"))
        
        # 普通字段 parent_id
        write_text(f"--{boundary}\r\n")
        write_text('Content-Disposition: form-data; name="parent_id"\r\n\r\n')
        write_text(str(parent_id))
        write_text("\r\n")
        
        # 普通字段 task_type
        write_text(f"--{boundary}\r\n")
        write_text('Content-Disposition: form-data; name="task_type"\r\n\r\n')
        write_text(task_type)
        write_text("\r\n")
        
        # 普通字段 cover（小写 true/false）
        if cover is not None:
            write_text(f"--{boundary}\r\n")
            write_text('Content-Disposition: form-data; name="cover"\r\n\r\n')
            write_text("true" if cover else "false")
            write_text("\r\n")
        
        encoded_filename = url_quote(filename, safe='')
        with open(file_path, "rb") as f:
            write_text(f"--{boundary}\r\n")
            write_text(
                'Content-Disposition: form-data; name="file"; ' +
                f'filename="{filename}"; filename*=UTF-8\'\'{encoded_filename}\r\n'
            )
            write_text(f"Content-Type: {content_type}\r\n\r\n")
            # 写入文件内容
            body.write(f.read())
            write_text("\r\n")
        
        # 结束边界
        write_text(f"--{boundary}--\r\n")
        
        response = requests.post(
            import_url,
            headers={
                **headers,
                "Content-Type": f"multipart/form-data; boundary={boundary}",
            },
            data=body.getvalue(),
            timeout=300,
        )
        
        if response.status_code != 200:
            return {
                "success": False,
                "msg": f"HTTP {response.status_code}: {response.text[:500]}",
            }
        
        try:
            return response.json()
        except json.JSONDecodeError:
            return {"success": False, "msg": f"响应解析失败: {response.text[:500]}"}


def print_tools(tools: list):
    """格式化打印工具列表"""
    print(f"\n📋 可用工具列表 (共 {len(tools)} 个):")
    print("=" * 80)
    
    for tool in tools:
        name = tool.get("name", "")
        desc = tool.get("description", "")[:60]
        print(f"  • {name}")
        print(f"    {desc}...")
        
        # 打印参数信息
        if "inputSchema" in tool and "properties" in tool["inputSchema"]:
            props = tool["inputSchema"]["properties"]
            required = tool["inputSchema"].get("required", [])
            if props:
                params = []
                for pname, pinfo in props.items():
                    req_mark = "*" if pname in required else ""
                    params.append(f"{pname}{req_mark}")
                print(f"    参数: {', '.join(params)}")
        print()


def print_tool_detail(tool: dict):
    """打印工具详细信息"""
    print(f"\n📝 工具详情: {tool['name']}")
    print("=" * 80)
    print(f"描述: {tool.get('description', 'N/A')}")
    
    if "inputSchema" in tool and "properties" in tool["inputSchema"]:
        print("\n参数:")
        props = tool["inputSchema"]["properties"]
        required = tool["inputSchema"].get("required", [])
        
        for pname, pinfo in props.items():
            req_mark = " (必填)" if pname in required else " (可选)"
            ptype = pinfo.get("type", "any")
            pdesc = pinfo.get("description", "")
            print(f"  • {pname}{req_mark}")
            print(f"    类型: {ptype}")
            if pdesc:
                print(f"    说明: {pdesc}")


def print_result(result: dict):
    """格式化打印调用结果"""
    print("\n📤 调用结果:")
    print("=" * 80)
    
    if "error" in result:
        print(f"❌ 错误: {result['error']}")
    elif "result" in result:
        res = result["result"]
        if "content" in res and isinstance(res["content"], list):
            for item in res["content"]:
                if item.get("type") == "text":
                    text = item.get("text", "")
                    # 尝试格式化 JSON
                    try:
                        data = json.loads(text)
                        print(json.dumps(data, indent=2, ensure_ascii=False))
                    except json.JSONDecodeError:
                        print(text)
        else:
            print(json.dumps(res, indent=2, ensure_ascii=False))
    else:
        print(json.dumps(result, indent=2, ensure_ascii=False))


def print_upload_result(result: dict):
    """格式化打印上传导入结果"""
    print("\n📤 上传导入结果:")
    print("=" * 80)
    
    if result.get("success"):
        print("✅ 导入成功!")
        if "data" in result and result["data"]:
            print(json.dumps(result["data"], indent=2, ensure_ascii=False))
    else:
        print(f"❌ 导入失败: {result.get('msg', '未知错误')}")
        if "data" in result and result["data"]:
            print(json.dumps(result["data"], indent=2, ensure_ascii=False))


def interactive_mode(client: MCPClient):
    """交互模式"""
    print("\n🎮 进入交互模式 (输入 'help' 查看帮助, 'quit' 退出)")
    print("=" * 80)
    
    while True:
        try:
            user_input = input("\n> ").strip()
            
            if not user_input:
                continue
            
            if user_input.lower() in ["quit", "exit", "q"]:
                print("👋 再见!")
                break
            
            if user_input.lower() == "help":
                print("""
可用命令:
  list                                    - 列出所有工具
  info <tool_name>                        - 查看工具详情
  call <tool_name> [args_json]            - 调用工具
  call <tool_name> --file <json_file>     - 从文件读取参数调用工具
  <tool_name> [args_json]                 - 调用工具 (简写)
  <tool_name> --file <json_file>          - 从文件读取参数 (简写)
  upload <file_path> <parent_id> [options] - 上传文件导入到指定目录
  help                                    - 显示帮助
  quit                                    - 退出

上传选项:
  --task-type <type>   任务类型，默认 md_import
  --no-cover           不覆盖同名文档

示例:
  list
  info getDocument
  call getDocument {"docid": "123456"}
  getDocument {"docid": "123456"}
  aiSearchDocument {"query": "项目文档", "limit": 5}
  call updateDocument --file /tmp/update_args.json
  upload ./README.md 4017403457
  upload ./doc.md 4017403457 --task-type md_import
  upload ./doc.md 4017403457 --no-cover
""")
                continue
            
            if user_input.lower() == "list":
                tools = client.list_tools(force_refresh=True)
                print_tools(tools)
                continue
            
            # 处理 upload 命令
            if user_input.lower().startswith("upload "):
                upload_parts = user_input.split()
                if len(upload_parts) < 3:
                    print("❌ 用法: upload <file_path> <parent_id> [--task-type <type>] [--no-cover]")
                    continue
                
                up_file_path = upload_parts[1]
                try:
                    up_parent_id = int(upload_parts[2])
                except ValueError:
                    print(f"❌ parent_id 必须为整数: {upload_parts[2]}")
                    continue
                
                up_task_type = "md_import"
                up_cover = True
                i = 3
                while i < len(upload_parts):
                    if upload_parts[i] == "--task-type" and i + 1 < len(upload_parts):
                        up_task_type = upload_parts[i + 1]
                        i += 2
                    elif upload_parts[i] == "--no-cover":
                        up_cover = False
                        i += 1
                    else:
                        print(f"❌ 未知选项: {upload_parts[i]}")
                        break
                else:
                    file_size_mb = os.path.getsize(up_file_path) / 1024 / 1024 if os.path.isfile(up_file_path) else 0
                    print(f"📁 上传文件: {up_file_path} ({file_size_mb:.2f}MB)")
                    print(f"   父目录ID: {up_parent_id}")
                    print(f"   任务类型: {up_task_type}")
                    print(f"   覆盖同名: {'是' if up_cover else '否'}")
                    
                    result = client.upload_file(up_file_path, up_parent_id, up_task_type, up_cover)
                    print_upload_result(result)
                continue
            
            if user_input.lower().startswith("info "):
                tool_name = user_input[5:].strip()
                tool = client.get_tool_info(tool_name)
                if tool:
                    print_tool_detail(tool)
                else:
                    print(f"❌ 工具 '{tool_name}' 不存在")
                continue
            
            # 解析调用命令
            if user_input.lower().startswith("call "):
                remainder = user_input[5:].strip()
            else:
                remainder = user_input
            
            # 只拆分出 tool_name，剩余部分整体保留
            parts = remainder.split(None, 1)
            tool_name = parts[0]
            args = {}
            
            if len(parts) > 1:
                arg_str = parts[1].strip()
                if arg_str.startswith("--file"):
                    file_parts = arg_str.split(None, 1)
                    if len(file_parts) < 2:
                        print("❌ --file 需要指定 JSON 文件路径")
                        continue
                    try:
                        with open(file_parts[1].strip(), "r", encoding="utf-8") as f:
                            args = json.load(f)
                    except FileNotFoundError:
                        print(f"❌ 文件不存在: {file_parts[1].strip()}")
                        continue
                    except json.JSONDecodeError as e:
                        print(f"❌ 文件 JSON 解析错误: {e}")
                        continue
                else:
                    try:
                        args = json.loads(arg_str)
                    except json.JSONDecodeError as e:
                        print(f"❌ 参数 JSON 解析错误: {e}")
                        continue
            
            # 验证工具是否存在
            tool = client.get_tool_info(tool_name)
            if not tool:
                print(f"❌ 工具 '{tool_name}' 不存在，使用 'list' 查看可用工具")
                continue
            
            print(f"🔧 调用工具: {tool_name}")
            if args:
                print(f"   参数: {json.dumps(args, ensure_ascii=False)}")
            
            result = client.call_tool(tool_name, args)
            print_result(result)
            
        except KeyboardInterrupt:
            print("\n👋 再见!")
            break
        except Exception as e:
            print(f"❌ 错误: {e}")


def load_token_from_env_file():
    """尝试从 .env 文件加载 TAI_PAT_TOKEN"""
    env_file_paths = [
        os.path.join(os.getcwd(), ".env"),
        os.path.join(os.path.dirname(__file__), ".env"),
        os.path.join(os.path.dirname(os.path.dirname(__file__)), ".env"),
    ]
    
    for env_file in env_file_paths:
        if os.path.isfile(env_file):
            try:
                with open(env_file, "r", encoding="utf-8") as f:
                    for line in f:
                        line = line.strip()
                        if line.startswith("TAI_PAT_TOKEN="):
                            token = line.split("=", 1)[1].strip().strip('"').strip("'")
                            if token:
                                return token
            except Exception:
                continue
    return None


def main():
    print("🔧 iWiki MCP Server 通用客户端")
    print("=" * 80)
    
    # 优先从环境变量读取 TAI_PAT_TOKEN，其次从 .env 文件读取
    token = os.environ.get("TAI_PAT_TOKEN")
    if not token:
        token = load_token_from_env_file()
    
    
    if not token:
        print("❌ 错误: 未设置 TAI_PAT_TOKEN 环境变量")
        print()
        print("请先设置 TAI_PAT_TOKEN:")
        print('  export TAI_PAT_TOKEN="your_tai_pat_token_here"')
        print()
        print("获取 Token 方式:")
        print("  1. 登录 太湖个人令牌 (https://tai.it.woa.com/user/pat)")
        print("  2. 创建 API Token，选择iWiki官方MCP或全部应用")
        sys.exit(1)
    
    # upload 命令直接走 HTTP 端点，无需初始化 MCP 连接
    if len(sys.argv) > 1 and sys.argv[1].lower() == "upload":
        if len(sys.argv) < 4:
            print("❌ 用法: python connect_mcp.py upload <file_path> <parent_id> [--task-type <type>] [--no-cover]")
            sys.exit(1)
        
        up_file_path = sys.argv[2]
        try:
            up_parent_id = int(sys.argv[3])
        except ValueError:
            print(f"❌ parent_id 必须为整数: {sys.argv[3]}")
            sys.exit(1)
        
        up_task_type = "md_import"
        up_cover = True
        i = 4
        while i < len(sys.argv):
            if sys.argv[i] == "--task-type" and i + 1 < len(sys.argv):
                up_task_type = sys.argv[i + 1]
                i += 2
            elif sys.argv[i] == "--no-cover":
                up_cover = False
                i += 1
            else:
                print(f"❌ 未知选项: {sys.argv[i]}")
                sys.exit(1)
        
        client = MCPClient(token)
        file_size_mb = os.path.getsize(up_file_path) / 1024 / 1024 if os.path.isfile(up_file_path) else 0
        print(f"\n📁 上传文件: {up_file_path} ({file_size_mb:.2f}MB)")
        print(f"   父目录ID: {up_parent_id}")
        print(f"   任务类型: {up_task_type}")
        print(f"   覆盖同名: {'是' if up_cover else '否'}")
        
        result = client.upload_file(up_file_path, up_parent_id, up_task_type, up_cover)
        print_upload_result(result)
        print("\n🎉 完成!")
        return
    
    print(f"MCP Server URL: {MCP_URL}")
    
    # 创建客户端
    client = MCPClient(token)
    
    try:
        # 初始化连接
        print("\n📡 初始化 MCP 连接...")
        init_response = client.initialize()
        
        if "error" in init_response:
            print(f"❌ 初始化失败: {init_response['error']}")
            sys.exit(1)
        
        print("✅ MCP Server 连接成功!")
        
        # 获取工具列表
        tools = client.list_tools()
        print(f"📋 发现 {len(tools)} 个可用工具")
        
        # 根据命令行参数执行操作
        if len(sys.argv) < 2:
            # 无参数：进入交互模式
            interactive_mode(client)
        elif sys.argv[1].lower() == "list":
            # 列出所有工具
            print_tools(tools)
        elif sys.argv[1].lower() == "info" and len(sys.argv) > 2:
            # 查看工具详情
            tool = client.get_tool_info(sys.argv[2])
            if tool:
                print_tool_detail(tool)
            else:
                print(f"❌ 工具 '{sys.argv[2]}' 不存在")
        else:
            # 调用工具：支持 "call <tool> [args]" 和 "<tool> [args]" 两种形式
            if sys.argv[1].lower() == "call" and len(sys.argv) > 2:
                tool_name = sys.argv[2]
                args = parse_tool_args(sys.argv[3:])
            else:
                tool_name = sys.argv[1]
                args = parse_tool_args(sys.argv[2:])
            
            tool = client.get_tool_info(tool_name)
            if not tool:
                print(f"❌ 工具 '{tool_name}' 不存在")
                print("使用 'python connect_mcp.py list' 查看可用工具")
                sys.exit(1)
            
            print(f"\n🔧 调用工具: {tool_name}")
            if args:
                print(f"   参数: {json.dumps(args, ensure_ascii=False)[:200]}...")
            
            result = client.call_tool(tool_name, args)
            print_result(result)
        
        print("\n🎉 完成!")
        
    except requests.exceptions.RequestException as e:
        print(f"❌ 请求失败: {e}")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"❌ JSON 解析失败: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
