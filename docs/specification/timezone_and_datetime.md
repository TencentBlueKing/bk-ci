# 时区与时刻字段契约

本文约定 bk-ci 对人端 API 的时刻字段语义，以及时区展示分工。实现与改造须遵循本规范。

## 目标

- 后端存储与传输**绝对时刻**（Instant），不绑定展示时区。
- 前端按蓝鲸用户管理返回的 IANA `time_zone`，将绝对时刻格式化为用户当地时间。
- 前端展示时区以 project 模板接口 `tenantInfoForDisplay.timeZone` 为统一入口（与多租户 `tenantId` 同接口获取）；暂默认 `Asia/Shanghai`，待蓝鲸 API 就绪后替换。
- 不要求把 DB 列全面改为 Unix BIGINT；`datetime` / `timestamp` 可继续使用，读写须不丢绝对时刻。

## API 出站契约（对人端）

| 规则 | 说明 |
|------|------|
| 类型 | 时刻字段使用 `Long`，单位为**毫秒** Unix epoch |
| 单位 | 禁止对新接口使用秒级时间戳；存量秒级须迁移为毫秒或在文档中明确标注并限期统一 |
| 禁止 | 禁止对人端 VO 返回无时区的 `yyyy-MM-dd HH:mm:ss` / ISO `LocalDateTime`（无 offset） |
| Schema | Swagger 须标注「毫秒时间戳」 |
| 空值 | 无时刻用 `null`，不要用空字符串 `""` |

推荐写法（Kotlin）：

```kotlin
import com.tencent.devops.common.api.util.timestampmilli

createTime = record.createTime?.timestampmilli()
```

禁止对人端：

```kotlin
DateTimeUtil.toDateTime(record.createTime) // 仅允许通知文案、日志、内部运维展示
```

## API 入参契约（按日 / 按区间）

- 日历日字段可继续用 `yyyy-MM-dd` 字符串，但必须同时约定时区：
  - 优先：请求体携带 IANA `timeZone`（与用户配置一致）
  - 缺省：服务端使用 `ZoneId.systemDefault()`，并在接口文档写明
- 服务端将「用户时区下的自然日」换算为绝对时刻区间后再查库，工具见 `DateTimeUtil.calendarDateRangeToLocalDateTime`。

## 按日查询约定

- `yyyy-MM-dd` + `timeZone`：表示用户时区下的自然日。
- **事件时刻列**（如构建 start_time）：用 `DateTimeUtil.calendarDateRangeToLocalDateTime` / `TimeZoneDayRangeUtils` 换成绝对时刻区间再查。
- **日历日桶列**（如 Metrics `STATISTICS_TIME`）：按日历日标签匹配，不要做 Instant 换算；`timeZone` 主要用于默认「今天」等相对日计算。

详见 `QueryParamCheckUtil.calendarDateRangeToLocalDateTime`。

## 存储层

- MySQL `datetime` / `timestamp` + 应用层 `LocalDateTime` 可保留。
- `LocalDateTime` ↔ epoch 统一按 **`ZoneId.systemDefault()`** 解释（与 JVM/部署时区一致），禁止业务代码硬编码 `UTC+8`。
- 一期不强制 DDL 改为 BIGINT Unix。

## 前后端分工

| 场景 | 负责方 |
|------|--------|
| 页面列表 / 详情展示 | **前端** `formatByUserTz(ms, timeZone)` / `<time-display>` |
| 用户时区来源（展示基准） | **多租户统一入口** `GET /project/api/user/users/tenantInfoForDisplay` 的 `timeZone`（与 `tenantId` / `apiBaseUrl` 同接口）；前端经 `applyTenantDisplayInfo` 写入 `window.tenantInfoForDisplay` / `window.userInfo.timeZone`。暂默认 `Asia/Shanghai`，待蓝鲸用户管理 API 提供后替换 |
| 兼容读取顺序 | `window.tenantInfoForDisplay.timeZone` → `window.userInfo.timeZone` → 浏览器 `Intl` → `Asia/Shanghai` |
| 邮件 / 企业微信等通知正文中的时间 | 后端可按用户时区格式化字符串 |
| OpenAPI / 第三方 | 返回毫秒时间戳，由消费方自行格式化 |

## 存量债务治理顺序

1. Environment / Store / Metrics / Quality / Project 等字符串出站字段改为毫秒 `Long`
2. Auth / Notify 等秒级字段统一为毫秒
3. 修正 `DateTimeUtil` 中与 `systemDefault` 不一致的硬编码偏移
4. 按日查询接入 `timeZone` 日界换算

## 前端公共工具

- [`src/frontend/common-lib/time.js`](../../src/frontend/common-lib/time.js)：`applyTenantDisplayInfo` / `getUserTimeZone` / `formatByUserTz` / `convertTime` / `formatTimezoneTooltip` / `formatDuration` / `calendarDateRangeToEpochMilli` / `recentDaysRangeInUserTz` / `userTzTodayRange` 等
- [`src/frontend/common-lib/time-display.vue`](../../src/frontend/common-lib/time-display.vue)：列表/详情时刻展示组件（含时区 hover tooltip），表格列推荐直接使用
- 展示时区入口：各模块 `TenantSingleton.init()` → `tenantInfoForDisplay` → `applyTenantDisplayInfo`
- **日历入参 / 快捷区间 /「现在」预览**：统一按用户 IANA 时区计算自然日与展示，禁止再用浏览器本地 `Date#getHours` / 裸 `dayjs()` / `moment()` 解释绝对时刻
- **相对时长**：展示用 `formatDuration`（与时区无关的 elapsed ms），但实现收口到同一时间工具，避免各处自行依赖 moment/dayjs duration
