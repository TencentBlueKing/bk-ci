---
name: java-heap-dump-triage
description: 分析 Java heap dump、.hprof 文件、Eclipse MAT HTML 报告、Leak Suspects zip、Class Histogram、Dominator Tree、GC 异常、JVM OOM 或内存泄漏问题，并把 retained heap 证据关联回 Java/Kotlin/Spring/Quartz 项目代码，产出中文的根因判断、修复方案、验证计划和故障复盘文档。适用于用户要求分析 dump/hprof/heap dump/MAT 报告、内存泄漏、OOM、retained heap、dominator tree、RAMJobStore、ThreadLocal 泄漏、缓存泄漏、classloader 泄漏，或要求结合仓库代码优化内存问题的场景。
---

# Java Heap Dump 排查

## 目标

把 heap dump 证据转成可落地的工程诊断。优先给出有证据支撑的窄根因，不要泛泛罗列 JVM 调优建议。

## 排查流程

1. 定位分析材料。
   - 搜索 `.hprof`、`*Leak_Suspects*.zip`、`*System_Overview*.zip`、`*Top_Components*.zip`、`.threads`、`.index`、MAT 导出的 HTML 报告。
   - 记录 dump 大小、报告生成时间。
   - 如果 MAT 的 System Properties 可用，记录应用名、启动类、profile、JDK 版本、服务端口、classpath 里的模块信息。
   - 如果只有原始 `.hprof`，不要停止；先用 Eclipse MAT 命令行生成报告。

2. 如果没有 MAT 报告，从 `.hprof` 生成报告。
   - 查找 MAT 命令行工具，例如 `ParseHeapDump.sh` 或 `MemoryAnalyzer`。
   - 不要删除原始 `.hprof`；如果要模拟无报告状态，只移动或清理 `.index`、`.threads`、`*_Leak_Suspects.zip`、`*_System_Overview.zip`、`*_Top_Components.zip` 等派生文件。
   - 用 `ParseHeapDump.sh <dump.hprof> org.eclipse.mat.api:suspects org.eclipse.mat.api:overview org.eclipse.mat.api:top_components` 生成三类报告。
   - 预期产物通常与 dump 同目录同前缀：
     - `<name>_Leak_Suspects.zip`
     - `<name>_System_Overview.zip`
     - `<name>_Top_Components.zip`
     - `<name>.threads`
     - `<name>*.index`
   - 解析大 dump 时输出很多，日志可能被截断；以最终退出码和生成物为准。
   - 关键阶段包括：Parsing heap dump、Writing threads、Processing object reachability、Calculating Dominator Tree、Leak Suspects、System Overview、Top Components。

3. 优先读取 MAT 报告，而不是一上来硬啃原始 dump。
   - 用 `unzip -l` 查看报告 zip 内容。
   - 解压到唯一临时目录，不删除用户文件。
   - 重点解析 `index.html`、`pages/Class_Histogram*.html`、`pages/Top_Consumers*.html`、`pages/Thread_Overview*.html`、`pages/System_Properties*.html`、Problem Suspect 页面。
   - 从 HTML 中提取文本后，总结 top retained objects、class histogram、GC root 最短路径、线程栈、JVM/应用属性。

4. 找 retained memory 的真正持有者。
   - 优先看 retained heap 和 dominator tree，不要被 shallow heap 误导。
   - 识别一到几个主要 accumulation point，例如 cache/map、scheduler/job store、queue、ThreadLocal、classloader、ORM/session、HTTP buffer、byte array、String、reactive queue。
   - 记录具体数字：retained bytes、占比、对象数量、top classes、最短路径字段。

5. 把 dump 里的类名关联回代码。
   - 用 `rg` 搜索类名、包名、框架类型、关键字段、job/cache/listener/scheduler API、配置项。
   - 阅读创建、刷新、删除、保留可疑对象的代码路径。
   - 检查生命周期是否对称：add/remove、subscribe/unsubscribe、schedule/delete、cache put/evict、ThreadLocal set/remove、close/shutdown。
   - 检查身份标识是否对称：key name、group/namespace、task id、project id、hash/md5、tenant、channel、shard。
   - 检查启动或 reload 逻辑是否重复注册但没有清理。

6. 形成可证伪的根因。
   - 把 dump 证据和具体代码行为绑定起来。
   - 解释对象为什么增长，以及为什么 GC 无法回收。
   - 区分直接原因和长期风险。例如：`RAMJobStore` 持有 trigger 是直接证据；大规模使用 `RAMJobStore` 是扩展性风险；`deleteJob` 用错 group 才是更具体的代码缺陷。

7. 给出修复方案。
   - 如果发现明确 bug，第一修复应尽量小而精确。
   - 写清楚文件、函数、修改前行为、修改后行为。
   - 只有在有助于验证时才加日志或指标。
   - 结构性风险作为二阶段优化，不要混在立即修复里。

8. 验证。
   - 对改动模块运行最窄范围的编译或测试。
   - 能补测试时，优先补生命周期对称性的单元/集成测试。
   - 线上验证要包含重启、重新 dump、指标和日志检查。

9. 输出结论。
   - 先给根因和修复方案。
   - 附关键 MAT 数字证据表。
   - 附代码链路和问题点。
   - 如果对象保存在进程内存中，明确说明上线后是否需要重启。
   - 证据不足时标注为“假设”或“需要进一步验证”，不要过度断言。

## 常用命令

查找 heap 相关文件：

```bash
find ~/Desktop -maxdepth 3 \( -iname '*.hprof' -o -iname '*dump*' -o -iname '*heap*' -o -iname '*Leak_Suspects*.zip' -o -iname '*System_Overview*.zip' -o -iname '*Top_Components*.zip' \) -print
```

查找本机 Eclipse MAT 命令行：

```bash
command -v ParseHeapDump.sh || true
command -v MemoryAnalyzer || true
find /Applications "$HOME" -maxdepth 5 \( -iname 'ParseHeapDump.sh' -o -iname 'MemoryAnalyzer' \) -print 2>/dev/null
```

从原始 hprof 生成 MAT 报告：

```bash
/Applications/MemoryAnalyzer.app/Contents/Eclipse/ParseHeapDump.sh \
  /path/to/heap_dump.hprof \
  org.eclipse.mat.api:suspects \
  org.eclipse.mat.api:overview \
  org.eclipse.mat.api:top_components
```

生成后确认产物：

```bash
find /path/to/dump-dir -maxdepth 1 \( \
  -name 'heap_dump*.index' \
  -o -name 'heap_dump.threads' \
  -o -name 'heap_dump_*Suspects.zip' \
  -o -name 'heap_dump_*Overview.zip' \
  -o -name 'heap_dump_*Components.zip' \
\) -print | sort
```

查看 MAT 报告 zip：

```bash
unzip -l /path/to/heap_dump_Leak_Suspects.zip | sed -n '1,160p'
```

安全提取 MAT HTML 文本：

```bash
TMP=/tmp/heap_report_$$
mkdir -p "$TMP/leak" "$TMP/overview" "$TMP/top"
unzip -q /path/to/heap_dump_Leak_Suspects.zip -d "$TMP/leak"
unzip -q /path/to/heap_dump_System_Overview.zip -d "$TMP/overview"
unzip -q /path/to/heap_dump_Top_Components.zip -d "$TMP/top"
python3 - "$TMP" <<'PY'
from pathlib import Path
import html, re, sys
base = Path(sys.argv[1])
for p in base.rglob("*.html"):
    if p.name.startswith(("Class_Histogram", "Top_Consumers", "Thread_Overview", "System_Properties")) or p.name == "index.html":
        s = p.read_text(errors="ignore")
        s = re.sub(r"<script.*?</script>|<style.*?</style>", "", s, flags=re.S|re.I)
        s = re.sub(r"<[^>]+>", "\n", s)
        lines = [html.unescape(x.strip()) for x in s.splitlines() if x.strip()]
        print(f"\n===== {p.relative_to(base)} =====")
        print("\n".join(lines[:180]))
PY
```

按可疑类和 API 回查代码：

```bash
rg -n "Quartz|RAMJobStore|CronTrigger|scheduleJob|deleteJob|ThreadLocal|Cache|put\\(|evict|shutdown|close\\(" src -g '*.kt' -g '*.java' -g '*.yml' -g '*.yaml' -g '*.properties'
```

## 实测生成流程记录

本技能沉淀自一次从原始 dump 重新生成报告的实测流程：

1. 原始输入只保留 `/Users/greysonfang/Desktop/heap_dump.hprof`，大小约 `2.6G`。
2. 先将已有 MAT 派生物移走，包括 `heap_dump*.index`、`heap_dump.threads`、`heap_dump_Leak_Suspects.zip`、`heap_dump_System_Overview.zip`、`heap_dump_Top_Components.zip`。
3. 找到本机 MAT 命令：`/Applications/MemoryAnalyzer.app/Contents/Eclipse/ParseHeapDump.sh`。
4. 执行：

```bash
/Applications/MemoryAnalyzer.app/Contents/Eclipse/ParseHeapDump.sh \
  /Users/greysonfang/Desktop/heap_dump.hprof \
  org.eclipse.mat.api:suspects \
  org.eclipse.mat.api:overview \
  org.eclipse.mat.api:top_components
```

5. MAT 先解析 hprof、生成 `.threads` 和索引，再计算 Dominator Tree，最后输出三个报告 zip。
6. 本次生成的报告大小约为：
   - `heap_dump_Leak_Suspects.zip`: `168K`
   - `heap_dump_System_Overview.zip`: `140K`
   - `heap_dump_Top_Components.zip`: `772K`
7. `Top Components` 阶段日志可能极多，终端输出被截断是正常的；只要进程退出码为 `0` 且报告 zip 存在即可继续分析。

## 诊断经验

- 如果一个对象占据极高 retained heap，占用链路应从它的 dominator path 开始。
- 如果 Class Histogram 里有大量集合 entry，先找持有这些 entry 的上层集合，不要优化 entry 类型本身。
- 如果框架内部 store 占大头，重点查业务代码如何使用这个框架的生命周期 API。
- 如果代码里有删除逻辑但对象仍堆积，优先比较创建、检查、删除时使用的 key/group/namespace 是否完全一致。
- 如果 dump 显示很多线程，但 retained heap 主要在别处，不要把线程数当成根因，除非 ThreadLocal 或线程栈直接保留了大对象。
- 如果 System Properties 能看到应用名、profile、classpath，要用它定位具体服务模块。

## 输出模板

除非用户要求其他格式，使用下面结构输出：

```markdown
# Java Heap Dump 排查结论

## 现象
- Dump / MAT 报告来源：
- 主要 retained heap：
- 关键对象数量：

## 根因
一句话结论。

## 证据链
| 证据 | 数值/位置 | 说明 |
| --- | --- | --- |

## 代码定位
- 文件：
- 调用链：
- 问题点：

## 修复方案
1. 立即修复：
2. 验证：
3. 上线/重启：

## 长期优化
- 监控：
- 架构/存储：
- 测试：
```

## 案例参考

如果 MAT 中出现 Quartz `RAMJobStore`、`QuartzSchedulerResources`、`CronTriggerImpl`、`CronExpression`、`TriggerWrapper`、`TreeMap$Entry` 或 `JobKey` 占据主要 retained heap，读取 `references/quartz-ramjobstore.md`。
