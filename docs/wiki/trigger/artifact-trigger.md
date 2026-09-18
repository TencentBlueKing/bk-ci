# 制品到达触发

当制品库中出现符合条件的**文件、目录或镜像**时，自动启动流水线。

- 入口：**流水线编辑 → 触发器 → 新增「制品到达触发」**
- 可视化配置与 PAC YAML `on.artifact.arrived` 双向同步
- 仅在**同一项目内**生效，暂不支持跨项目

## 能力概览

先按"制品在哪个仓库"选择监听类型，仓库类型已隐含了制品形态：

| 监听仓库 | 监听对象 | 制品形态 | 典型场景 |
|----------|----------|----------|----------|
| 流水线仓库 | 上游流水线归档的产物 | 文件 / 目录 | 上游打包完成 → 下游自动部署 |
| 自定义仓库 | 自定义仓库某个根路径下的产物 | 文件 / 目录 | 流水线/外部上传到指定目录 → 触发处理 |
| 镜像仓库 | 镜像 push | 镜像 | 镜像发布 → 触发部署 |

---

## 配置触发器

### 通用说明（三种仓库都适用）

- **监听流水线**：**流水线仓库必填**；自定义仓库、镜像仓库可选（留空 = 不限定来源）。选定后仅该流水线产出的制品可触发，可视化下拉不会列出本流水线。
- **本流水线自产的制品不会触发本流水线**（系统自动防循环，无需配置）。
- **监听范围（`kind`）**：`file`（单个文件）/ `folder`（整个目录）。镜像仓库不涉及此项。
- 匹配类字段留空 = 全部命中；排除类字段留空 = 不排除；语法见 [通配符语法](#通配符语法)。
- **监听范围为「整个目录」时，匹配/排除只能写目录，不能写文件名。**

### 流水线仓库

监听上游归档到流水线仓库的文件或目录。

| 字段 | 说明 | 示例 |
|------|------|------|
| 监听流水线 | **必填**，见通用说明。指定上游流水线，仅其归档的制品可触发 | — |
| 监听范围 | **单个文件**：每归档一个文件产生一次事件，命中即启动一次构建；**整个目录**：目录归档完成时启动一次 | — |
| 匹配名称 | **通配符**（见通配符语法）。单个文件写文件名（如 `*.msi`）；整个目录写归档目录名（如 `win*`），**不能写文件名**。留空 = 全部 | `*.msi` / `win*` |
| 排除名称 | **通配符**（见通配符语法）。命中则不触发。整个目录时同样只能写目录名 | `*_unsigned.exe` / `*-tmp` |

### 自定义仓库

监听自定义仓库中一个**已存在的根路径**，再在其下匹配文件或子目录。

| 字段 | 说明 | 示例 |
|------|------|------|
| 监听根路径 | 必填。仓库里已有的父路径，**须以 `/` 开头、以 `/` 结尾**，不能写成仓库根 `/` | `/release/` |
| 监听流水线 | 可选，见通用说明 | — |
| 监听范围 | 单个文件 / 整个目录，含义与流水线仓库相同 | — |
| 匹配路径 | **通配符**（见通配符语法），相对根路径。单个文件写文件路径（如 `**/setup.msi`）；整个目录写目录路径（如 `win/**`），**不能写文件名**。留空 = 根路径下全部 | `**/setup.msi` / `win/**` |
| 排除路径 | **通配符**（见通配符语法）。命中则不触发。整个目录时同样只能写目录路径 | `**/*.tmp` / `*-tmp/**` |

### 镜像仓库

| 字段 | 说明 | 示例 |
|------|------|------|
| 镜像名 | 必填。制品库中展示的名称，不含 registry 前缀 | `bk-ci/backend` |
| 监听流水线 | 可选，见通用说明 | — |
| 匹配 Tag | **通配符**（见通配符语法）。留空 = 任意 tag | `v*,release-*` |
| 排除 Tag | **通配符**（见通配符语法）。命中则不触发 | `*-dev,*-snapshot` |

### 通配符语法

匹配/排除字段（匹配名称、匹配路径、匹配 Tag）统一使用以下通配符，**大小写敏感**：

| 通配符 | 含义 |
|--------|------|
| `*` | 匹配一段文字，但不跨目录（`/`） |
| `**` | 可跨多层目录 |
| `?` | 匹配单个字符 |

**常见写法：**

| 你想匹配 | 可以这样写 | 用在哪个字段 |
|----------|------------|--------------|
| 所有 `.msi` 安装包（单个文件） | `*.msi` | 匹配名称 / 匹配路径 |
| `.msi` 或 `.exe`（单个文件） | `*.msi,*.exe`（不要写 `*.{msi,exe}`） | 匹配名称 |
| 任意目录里的 `setup.msi`（单个文件） | `**/setup.msi` | 匹配路径 |
| `win` 开头的归档目录（整个目录） | `win*` | 匹配名称 |
| `win` 目录下任意层级（整个目录） | `win/**` | 匹配路径 |
| `v` 开头的 Tag | `v*` | 匹配 Tag |

### 元数据过滤

在「更多过滤条件」里按制品元数据再筛一层：**不同键之间为「且」，同一键多条为「或」**；目录场景下，同一次归档里任一制品满足该键即命中。存在/不存在运算符不必填值。

| 运算符 | 含义 | 制品上没有该键时 |
|--------|------|------------------|
| 等于 `EQ` | 值完全相等 | 不命中 |
| 不等于 `NE` | 值不相等 | 命中 |
| 包含 `CONTAINS` | 值包含子串 | 不命中 |
| 存在 `EXISTS` | 键存在 | 不命中 |
| 不存在 `NOT_EXISTS` | 键不存在 | 命中 |

> 若要表达「打了 `env` 且不等于 `test`」，需同时配「存在 `env`」和「`env` 不等于 `test`」两条。

---

## 输出变量

触发成功后，可在流水线中通过 `${{ ci.artifact_* }}` 引用本次制品信息。触发人可用通用的 `${{ ci.actor }}`。

### 公共（所有形态）

| 变量 | 说明 |
|------|------|
| `${{ ci.artifact_repo_type }}` | 仓库类型：`pipeline` / `custom` / `image` |
| `${{ ci.artifact_kind }}` | 触发形态：`file` / `folder` / `image` |
| `${{ ci.artifact_count }}` | 命中数量，单文件/镜像为 `1` |
| `${{ ci.artifact_source_pipeline }}` | 来源流水线 ID（由流水线归档时有值） |
| `${{ ci.artifact_source_build_id }}` | 来源构建 ID（由流水线归档时有值） |

### 文件形态（`kind=file`，流水线/自定义仓库）

| 变量 | 说明 |
|------|------|
| `${{ ci.artifact_name }}` | 文件名 |
| `${{ ci.artifact_path }}` | 制品库路径 |
| `${{ ci.artifact_sha256 }}` | 内容指纹 |
| `${{ ci.artifact_size }}` | 文件大小（字节） |

### 目录形态（`kind=folder`，流水线/自定义仓库）

| 变量 | 说明 |
|------|------|
| `${{ ci.artifact_dir }}` | 本次归档的实际目录路径 |

### 镜像形态（镜像仓库）

| 变量 | 说明 |
|------|------|
| `${{ ci.artifact_image_name }}` | 镜像名 |
| `${{ ci.artifact_image_tag }}` | Tag |
| `${{ ci.artifact_image_digest }}` | Digest（有则填） |

---

## PAC YAML 配置

PAC v3.0 关键字为 `on.artifact.arrived`。字段用小写 kebab-case，匹配类字段写成列表，空值和默认值可省略。

> **必填约束**：流水线仓库必填 `watch-pipeline`，自定义仓库必填 `watch-root-path`，镜像仓库必填 `image`；这三个字段不支持写成流水线变量。

### 流水线仓库 · 单个文件

```yaml
on:
  artifact:
    arrived:
      name: 制品到达触发
      repository: pipeline
      kind: file
      watch-pipeline: p-xxxxx
      artifacts-name:
        - "*.msi"
        - "setup-*.exe"
      artifacts-name-ignore:
        - "*_unsigned.exe"
      metadata:
        - key: quality-gate
          operator: eq
          value: passed
```

### 流水线仓库 · 整个目录

```yaml
on:
  artifact:
    arrived:
      name: 制品到达触发
      repository: pipeline
      kind: folder
      watch-pipeline: p-xxxxx
      artifacts-name:
        - "win*"
        - "release-*"
      artifacts-name-ignore:
        - "*-tmp"
```

### 自定义仓库 · 单个文件

```yaml
on:
  artifact:
    arrived:
      name: 制品到达触发
      repository: custom
      kind: file
      watch-root-path: /release/
      watch-pipeline: p-xxxxx      # 可省略 = 任意来源
      paths:
        - "**/setup.msi"
      paths-ignore:
        - "**/*.tmp"
      metadata:
        - key: quality-gate
          operator: eq
          value: passed
```

### 自定义仓库 · 整个目录

```yaml
on:
  artifact:
    arrived:
      name: 制品到达触发
      repository: custom
      kind: folder
      watch-root-path: /release/
      watch-pipeline: p-xxxxx      # 可省略 = 任意来源
      paths:
        - "win/**"
      paths-ignore:
        - "*-tmp/**"
      metadata:
        - key: quality-gate
          operator: eq
          value: passed
```

### 镜像仓库

```yaml
on:
  artifact:
    arrived:
      name: 制品到达触发
      repository: image
      image: bk-ci/backend
      tags:
        - "v*"
        - "release-*"
      tags-ignore:
        - "*-dev"
        - "*-snapshot"
```

### 多条触发器

与其他触发并列时，`on` 写成列表，制品触发用 `type: artifact`：

```yaml
on:
  - manual: true
  - type: artifact
    arrived:
      name: MSI 归档触发
      repository: pipeline
      kind: file
      watch-pipeline: p-xxxxx
      artifacts-name:
        - "*.msi"
  - type: artifact
    arrived:
      name: 镜像到达触发
      repository: image
      image: bk-ci/backend
      tags:
        - "v*"
```

---

## 常见问题

**Q：配了触发器却没有启动？**
进入**流水线详情 → 触发事件**标签页，查看对应事件的未匹配原因。常见：来源流水线与「监听流水线」不一致、自定义仓库路径不在监听根路径下、镜像名带了 registry 前缀、匹配规则未命中或被排除、本流水线自产制品被系统忽略。

**Q：整个目录一直不触发？**
目录要等上游归档完成才会触发；只上传了部分文件、没有完成标记时，中间文件会被跳过。请确认上游使用蓝盾归档能力将整个目录归档完成。

**Q：一次归档命中多个制品，会触发几次？**
制品库的事件粒度是**一个文件一次事件**，监听范围决定哪些事件会启动构建：

| 场景 | 触发次数 |
|------|----------|
| 单个文件，一次归档 N 个命中文件 | N 次构建，每次 `${{ ci.artifact_path }}` 不同 |
| 整个目录 | 中间文件不启动构建，目录归档完成时启动 1 次 |
| 镜像一次 push 多个命中 tag | 每个 tag 各 1 次 |
| 同一事件命中本流水线多条触发器 | 只触发 1 次，按排列顺序取第一条命中的 |

上游一次归档 20 个 `.msi`，选「单个文件」时下游就是 20 次构建。需要「一次归档只跑一次」时，改用「整个目录」或把匹配规则收窄到单个文件名。

**Q：保存时报「路径不能为仓库根目录」？**
自定义仓库的监听根路径不能是 `/`，请写成已存在的子目录，如 `/release/`。

**Q：同一事件配了多条制品触发器，会启动几次？**
同一条流水线内只启动一次，按触发器排列顺序取第一条命中的。
