# 通知与审批管理指南

## 概述

蓝盾提供了完善的通知和审批机制，支持多种通知渠道和灵活的审批流程配置。通过合理使用通知和审批功能，可以确保关键操作得到及时通知和必要的审核，提高团队协作效率和流程管控能力。

## 通知服务

### 1. 支持的通知渠道

#### 企业微信通知
- **消息提醒**: 通过企业微信"消息提醒"应用发送
- **群机器人**: 通过企业微信群机器人发送群消息
- **服务号**: 通过蓝盾企业微信服务号发送

#### 邮件通知
- **普通邮件**: 支持HTML格式和纯文本格式
- **带附件邮件**: 支持发送构建报告、日志等附件
- **模板邮件**: 预定义邮件模板，支持变量替换

#### 其他通知方式
- **RTX通知**: 企业内部即时通讯工具
- **短信通知**: 紧急情况下的短信提醒
- **Webhook**: 自定义HTTP回调通知

### 2. 企业微信通知配置

#### 基本配置
```yaml
# 流水线中使用企业微信通知插件
- name: "发送企业微信通知"
  uses: send-wework-message@latest
  with:
    # 通知接收者（必填）
    receivers: |
      zhangsan
      lisi
      wangwu
    # 通知内容
    content: |
      🚀 **构建完成通知**
      
      项目: ${{ PROJECT_NAME }}
      分支: ${{ GIT_BRANCH }}
      构建号: ${{ BUILD_NUMBER }}
      状态: ${{ BUILD_STATUS }}
      
      构建时间: ${{ BUILD_TIME }}
      提交者: ${{ GIT_COMMITTER }}
      
      [查看详情](${{ BUILD_URL }})
    # 是否包含流水线链接
    includeLink: true
```

#### 使用变量配置接收者
```yaml
# 在流水线变量中定义通知人员
variables:
  NOTIFY_USERS:
    type: string
    default: "dev-team-lead,qa-manager"
    description: "通知接收者，多人用逗号分隔"

# 在插件中引用变量
- name: "通知相关人员"
  uses: send-wework-message@latest
  with:
    receivers: ${{ variables.NOTIFY_USERS }}
    content: "构建状态更新，请查看详情"
```

#### 群机器人通知
```yaml
# 配置企业微信群机器人
- name: "发送群消息"
  uses: send-wework-group-message@latest
  with:
    # 群聊ID（需要提前获取）
    chatId: "your-group-chat-id"
    # 消息内容
    content: |
      📢 **发布通知**
      
      应用: ${{ APP_NAME }}
      版本: ${{ VERSION }}
      环境: ${{ ENVIRONMENT }}
      
      发布状态: ✅ 成功
      发布时间: ${{ DEPLOY_TIME }}
      
      @所有人 请关注本次发布
    # 是否@所有人
    mentionAll: false
    # @特定用户
    mentionUsers: ["zhangsan", "lisi"]
```

### 3. 邮件通知配置

#### 基本邮件通知
```yaml
- name: "发送邮件通知"
  uses: send-email@latest
  with:
    # 收件人
    to: |
      developer@company.com
      qa-team@company.com
    # 抄送
    cc: "manager@company.com"
    # 邮件主题
    subject: "[蓝盾] ${{ PROJECT_NAME }} 构建 ${{ BUILD_STATUS }}"
    # 邮件内容（支持HTML）
    content: |
      <h2>构建结果通知</h2>
      
      <table border="1" style="border-collapse: collapse;">
        <tr><td><strong>项目名称</strong></td><td>${{ PROJECT_NAME }}</td></tr>
        <tr><td><strong>构建分支</strong></td><td>${{ GIT_BRANCH }}</td></tr>
        <tr><td><strong>构建号</strong></td><td>${{ BUILD_NUMBER }}</td></tr>
        <tr><td><strong>构建状态</strong></td><td>${{ BUILD_STATUS }}</td></tr>
        <tr><td><strong>构建时间</strong></td><td>${{ BUILD_TIME }}</td></tr>
      </table>
      
      <p><a href="${{ BUILD_URL }}">查看构建详情</a></p>
    # 邮件格式
    contentType: "html"
```

#### 带附件的邮件
```yaml
- name: "发送构建报告"
  uses: send-email-with-attachment@latest
  with:
    to: "qa-team@company.com"
    subject: "构建报告 - ${{ PROJECT_NAME }} v${{ VERSION }}"
    content: "请查看附件中的详细构建报告"
    # 附件路径
    attachments: |
      reports/test-report.html
      reports/coverage-report.html
      logs/build.log
```

### 4. 通知模板管理

#### 预定义模板
```yaml
# 成功通知模板
success_template: &success_notification
  content: |
    ✅ **构建成功**
    
    项目: ${{ PROJECT_NAME }}
    版本: ${{ VERSION }}
    分支: ${{ GIT_BRANCH }}
    
    🎉 恭喜！构建已成功完成
    
    [查看详情](${{ BUILD_URL }})

# 失败通知模板  
failure_template: &failure_notification
  content: |
    ❌ **构建失败**
    
    项目: ${{ PROJECT_NAME }}
    分支: ${{ GIT_BRANCH }}
    错误信息: ${{ ERROR_MESSAGE }}
    
    ⚠️ 请及时处理构建问题
    
    [查看详情](${{ BUILD_URL }})

# 使用模板
- name: "构建成功通知"
  if: success()
  uses: send-wework-message@latest
  with:
    receivers: ${{ variables.NOTIFY_USERS }}
    <<: *success_notification

- name: "构建失败通知"
  if: failure()
  uses: send-wework-message@latest
  with:
    receivers: ${{ variables.NOTIFY_USERS }}
    <<: *failure_notification
```

### 5. 条件化通知

#### 基于构建状态的通知
```yaml
# 只在主分支构建时通知
- name: "主分支构建通知"
  if: ${{ github.ref == 'refs/heads/main' }}
  uses: send-wework-message@latest
  with:
    receivers: "release-team"
    content: "主分支构建完成，准备发布"

# 只在构建失败时通知
- name: "失败通知"
  if: failure()
  uses: send-wework-message@latest
  with:
    receivers: ${{ variables.DEV_TEAM }}
    content: "⚠️ 构建失败，请及时处理"

# 只在首次成功时通知
- name: "首次成功通知"
  if: ${{ success() && env.PREVIOUS_BUILD_STATUS == 'FAILED' }}
  uses: send-wework-message@latest
  with:
    receivers: ${{ variables.NOTIFY_USERS }}
    content: "🎉 构建已修复，恢复正常"
```

#### 基于时间的通知
```yaml
# 工作时间通知策略
- name: "智能通知"
  uses: conditional-notify@latest
  with:
    # 工作时间立即通知
    worktime_notify:
      enabled: true
      hours: "09:00-18:00"
      timezone: "Asia/Shanghai"
      channels: ["wework", "email"]
    
    # 非工作时间汇总通知
    nonworktime_notify:
      enabled: true
      schedule: "0 9 * * 1-5"  # 每个工作日上午9点
      channels: ["email"]
      summary: true
```

## 审批管理

### 1. 人工审核插件

#### 基本配置
```yaml
- name: "生产发布审核"
  uses: manual-review@latest
  with:
    # 审核人（支持多人，任一人审核即可）
    reviewers: |
      tech-lead
      ops-manager
      product-owner
    
    # 审核说明
    description: |
      **生产环境发布审核**
      
      请确认以下内容：
      1. 功能测试是否完成
      2. 性能测试是否通过
      3. 安全扫描是否无高危漏洞
      4. 数据库变更是否已执行
      5. 回滚方案是否准备就绪
      
      **发布信息：**
      - 版本: ${{ VERSION }}
      - 分支: ${{ GIT_BRANCH }}
      - 提交: ${{ GIT_COMMIT }}
    
    # 超时时间（小时）
    timeout: 24
    
    # 自定义变量（审核时可修改）
    variables:
      deploy_strategy:
        type: "select"
        label: "部署策略"
        options: ["蓝绿部署", "滚动更新", "金丝雀发布"]
        default: "滚动更新"
      
      rollback_enabled:
        type: "boolean"
        label: "启用自动回滚"
        default: true
      
      notification_level:
        type: "select"
        label: "通知级别"
        options: ["仅核心团队", "全部相关人员", "全公司"]
        default: "仅核心团队"
```

#### 多级审核流程
```yaml
# 第一级：技术审核
- name: "技术审核"
  uses: manual-review@latest
  with:
    reviewers: "tech-lead"
    description: "技术方案审核，确认代码质量和架构合理性"
    variables:
      tech_approved:
        type: "boolean"
        label: "技术审核通过"
        default: false

# 第二级：业务审核
- name: "业务审核"
  if: ${{ steps.tech-review.outputs.tech_approved == 'true' }}
  uses: manual-review@latest
  with:
    reviewers: "product-manager"
    description: "业务功能审核，确认需求实现正确"
    variables:
      business_approved:
        type: "boolean"
        label: "业务审核通过"
        default: false

# 第三级：发布审核
- name: "发布审核"
  if: ${{ steps.business-review.outputs.business_approved == 'true' }}
  uses: manual-review@latest
  with:
    reviewers: ["ops-manager", "release-manager"]
    description: "最终发布审核，确认发布时机和策略"
```

### 2. Stream YAML审批配置

#### Stage准入审批
```yaml
version: v2.0

stages:
- name: "生产部署"
  check-in:
    # 人工审核配置
    reviews:
      flows:
        - name: "技术负责人审核"
          reviewers: ["tech-lead"]
        - name: "运维团队审核"
          reviewers: ["ops-team-lead", "ops-engineer"]
      
      # 审核变量
      variables:
        deploy_time:
          label: "部署时间"
          type: "INPUT"
          default: "${{ env.CURRENT_TIME }}"
        
        maintenance_window:
          label: "维护窗口"
          type: "SELECTOR"
          values: ["工作时间", "非工作时间", "周末"]
          default: "非工作时间"
        
        rollback_plan:
          label: "回滚计划"
          type: "TEXTAREA"
          description: "描述回滚步骤和预计时间"
      
      description: |
        **生产环境部署审核**
        
        请仔细审核本次部署：
        1. 确认功能测试完成
        2. 确认性能测试通过
        3. 确认安全扫描无问题
        4. 确认部署时间合适
        5. 确认回滚方案可行
    
    # 超时设置
    timeout-hours: 48
  
  jobs:
    deploy:
      steps:
      - name: "部署到生产环境"
        run: |
          echo "部署策略: ${{ variables.deploy_strategy }}"
          echo "部署时间: ${{ variables.deploy_time }}"
          # 执行部署逻辑
```

#### Stage准出审批
```yaml
- name: "测试验证"
  check-out:
    reviews:
      flows:
        - name: "QA验收"
          reviewers: ["qa-lead", "qa-engineer"]
      
      variables:
        test_result:
          label: "测试结果"
          type: "SELECTOR"
          values: ["通过", "有问题但可接受", "不通过"]
        
        issue_count:
          label: "发现问题数"
          type: "INPUT"
          default: "0"
        
        next_action:
          label: "下一步操作"
          type: "SELECTOR"
          values: ["继续部署", "修复后重新测试", "回滚"]
          default: "继续部署"
      
      description: |
        **测试验收审核**
        
        请确认测试结果：
        - 功能测试是否完成
        - 是否发现阻塞性问题
        - 是否可以继续后续流程
```

### 3. 审批通知配置

#### 审批消息通知
```yaml
# 审批开始通知
- name: "审批开始通知"
  uses: send-wework-message@latest
  with:
    receivers: ${{ env.REVIEWERS }}
    content: |
      📋 **审批请求**
      
      流水线: ${{ PIPELINE_NAME }}
      申请人: ${{ CI_ACTOR }}
      审批类型: 生产发布审核
      
      请及时处理审批请求
      [立即审核](${{ REVIEW_URL }})

# 审批超时提醒
- name: "审批超时提醒"
  if: ${{ env.REVIEW_TIMEOUT == 'true' }}
  uses: send-wework-message@latest
  with:
    receivers: |
      ${{ env.REVIEWERS }}
      ops-manager
    content: |
      ⏰ **审批超时提醒**
      
      审批已等待 ${{ env.WAIT_HOURS }} 小时
      申请人: ${{ CI_ACTOR }}
      
      请尽快处理，避免影响发布计划
```

#### 审批结果通知
```yaml
# 审批通过通知
- name: "审批通过通知"
  if: ${{ env.REVIEW_RESULT == 'APPROVED' }}
  uses: send-wework-message@latest
  with:
    receivers: |
      ${{ CI_ACTOR }}
      dev-team
    content: |
      ✅ **审批已通过**
      
      审批人: ${{ env.REVIEWER }}
      审批时间: ${{ env.REVIEW_TIME }}
      
      流水线将继续执行

# 审批拒绝通知
- name: "审批拒绝通知"
  if: ${{ env.REVIEW_RESULT == 'REJECTED' }}
  uses: send-wework-message@latest
  with:
    receivers: |
      ${{ CI_ACTOR }}
      dev-team
    content: |
      ❌ **审批已拒绝**
      
      审批人: ${{ env.REVIEWER }}
      拒绝原因: ${{ env.REJECT_REASON }}
      
      请根据反馈修改后重新提交
```

### 4. 高级审批功能

#### 条件化审批
```yaml
# 根据分支决定是否需要审批
- name: "条件审批"
  if: ${{ contains(github.ref, 'refs/heads/main') || contains(github.ref, 'refs/heads/release') }}
  uses: manual-review@latest
  with:
    reviewers: "release-manager"
    description: "主分支或发布分支需要审批"

# 根据变更范围决定审批级别
- name: "变更范围审批"
  uses: conditional-review@latest
  with:
    conditions:
      - if: ${{ env.CHANGED_FILES_COUNT > 100 }}
        reviewers: ["tech-lead", "architect"]
        description: "大规模变更需要架构师审批"
      
      - if: ${{ contains(env.CHANGED_FILES, 'database/') }}
        reviewers: ["dba", "ops-lead"]
        description: "数据库变更需要DBA审批"
      
      - if: ${{ contains(env.CHANGED_FILES, 'config/') }}
        reviewers: ["ops-lead"]
        description: "配置变更需要运维审批"
```

#### 审批委托和授权
```yaml
# 审批委托配置
delegation_config:
  # 主审批人不在时的委托关系
  delegations:
    tech-lead:
      delegates: ["senior-developer-1", "senior-developer-2"]
      auto_delegate: true
      delegate_condition: "out_of_office"
    
    ops-manager:
      delegates: ["ops-lead"]
      auto_delegate: false
      require_confirmation: true

# 紧急审批授权
emergency_approval:
  enabled: true
  authorized_users: ["cto", "ops-director"]
  conditions:
    - "production_incident"
    - "security_vulnerability"
  notification:
    - type: "wework"
      receivers: ["all-leads"]
    - type: "email"
      receivers: ["management@company.com"]
```

## 通知和审批集成

### 1. 质量红线集成

#### 质量检查失败通知
```yaml
# 质量红线失败时的通知配置
quality_gate_notification:
  on_fail:
    immediate:
      - type: "wework"
        receivers: ["${{ ci.actor }}"]
        content: |
          ⚠️ **质量检查失败**
          
          项目: ${{ PROJECT_NAME }}
          分支: ${{ GIT_BRANCH }}
          
          失败原因:
          ${{ QUALITY_GATE_FAILURES }}
          
          请修复问题后重新提交
    
    escalation:
      delay: "2h"
      receivers: ["tech-lead"]
      content: "质量检查持续失败，需要关注"

# 质量红线需要人工审核时
quality_gate_manual_review:
  reviewers: ["qa-lead", "tech-lead"]
  description: |
    **质量检查需要人工审核**
    
    以下指标未达标但可能需要例外处理：
    ${{ QUALITY_ISSUES }}
    
    请评估是否可以接受这些问题
  
  variables:
    accept_risk:
      label: "接受风险"
      type: "BOOLEAN"
      default: false
    
    mitigation_plan:
      label: "缓解措施"
      type: "TEXTAREA"
      description: "如果接受风险，请描述缓解措施"
```

### 2. 部署审批流程

#### 多环境部署审批
```yaml
environments:
  test:
    approval_required: false
    auto_deploy: true
    notification:
      - type: "wework"
        receivers: ["dev-team"]
  
  staging:
    approval_required: true
    reviewers: ["qa-lead"]
    auto_deploy_after_approval: true
    notification:
      - type: "wework"
        receivers: ["qa-team", "dev-team"]
  
  production:
    approval_required: true
    reviewers: ["ops-manager", "release-manager"]
    multi_stage_approval: true
    approval_stages:
      - name: "技术审核"
        reviewers: ["tech-lead"]
      - name: "业务审核"
        reviewers: ["product-manager"]
      - name: "发布审核"
        reviewers: ["ops-manager"]
    
    notification:
      - type: "wework"
        receivers: ["all-teams"]
      - type: "email"
        receivers: ["management@company.com"]
```

### 3. 事件驱动的通知

#### 基于构建事件的通知
```yaml
event_notifications:
  build_started:
    condition: ${{ github.ref == 'refs/heads/main' }}
    notification:
      - type: "wework"
        receivers: ["dev-team"]
        content: "🚀 主分支构建开始"
  
  build_completed:
    notification:
      - type: "wework"
        receivers: ["${{ ci.actor }}"]
        content: |
          {% if success() %}
          ✅ 构建成功完成
          {% else %}
          ❌ 构建失败
          {% endif %}
  
  deployment_started:
    condition: ${{ env.ENVIRONMENT == 'production' }}
    notification:
      - type: "wework"
        receivers: ["ops-team", "dev-team"]
        content: "🚀 生产环境部署开始"
      - type: "email"
        receivers: ["stakeholders@company.com"]
  
  deployment_completed:
    notification:
      - type: "wework"
        receivers: ["all-teams"]
        content: |
          {% if success() %}
          🎉 部署成功完成
          环境: ${{ env.ENVIRONMENT }}
          版本: ${{ env.VERSION }}
          {% else %}
          💥 部署失败，请立即处理
          {% endif %}
```

## 最佳实践

### 1. 通知策略设计

#### 分层通知策略
```yaml
notification_strategy:
  # 开发阶段 - 轻量通知
  development:
    channels: ["wework"]
    frequency: "on_failure_only"
    recipients: ["developer"]
  
  # 测试阶段 - 标准通知
  testing:
    channels: ["wework", "email"]
    frequency: "on_status_change"
    recipients: ["developer", "qa-team"]
  
  # 生产阶段 - 全面通知
  production:
    channels: ["wework", "email", "sms"]
    frequency: "all_events"
    recipients: ["all-stakeholders"]
    escalation: true
```

#### 智能通知过滤
```yaml
smart_notification:
  # 避免通知疲劳
  deduplication:
    enabled: true
    window: "5m"  # 5分钟内相同通知只发送一次
  
  # 批量通知
  batching:
    enabled: true
    window: "10m"  # 10分钟内的通知合并发送
    max_batch_size: 5
  
  # 静默时间
  quiet_hours:
    enabled: true
    start: "22:00"
    end: "08:00"
    timezone: "Asia/Shanghai"
    emergency_override: true
```

### 2. 审批流程优化

#### 并行审批
```yaml
parallel_approval:
  # 技术和业务并行审批
  - name: "并行审批阶段"
    parallel:
      - name: "技术审批"
        reviewers: ["tech-lead", "architect"]
        focus: "技术方案和代码质量"
      
      - name: "业务审批"
        reviewers: ["product-manager", "business-analyst"]
        focus: "业务需求和用户体验"
    
    # 所有并行审批完成后进入下一阶段
    require_all: true

# 最终发布审批
- name: "发布审批"
  depends_on: ["技术审批", "业务审批"]
  reviewers: ["release-manager"]
  description: "最终发布确认"
```

#### 自动化审批
```yaml
automated_approval:
  # 低风险变更自动审批
  conditions:
    - name: "文档更新"
      pattern: "docs/**"
      auto_approve: true
      notification: "dev-team"
    
    - name: "测试文件修改"
      pattern: "**/*test*"
      auto_approve: true
      notification: "qa-team"
    
    - name: "配置文件小幅修改"
      pattern: "config/**"
      max_lines_changed: 10
      auto_approve: true
      notification: "ops-team"
      post_approval_check: true
```

### 3. 监控和度量

#### 通知效果监控
```yaml
notification_metrics:
  # 通知送达率
  delivery_rate:
    target: 99%
    alert_threshold: 95%
  
  # 通知响应时间
  response_time:
    target: "5m"
    alert_threshold: "15m"
  
  # 通知有效性
  effectiveness:
    click_through_rate: 80%
    action_completion_rate: 90%
```

#### 审批效率监控
```yaml
approval_metrics:
  # 审批响应时间
  response_time:
    target: "2h"
    alert_threshold: "24h"
  
  # 审批通过率
  approval_rate:
    target: 85%
    min_threshold: 70%
  
  # 审批瓶颈识别
  bottleneck_detection:
    enabled: true
    threshold: "48h"
    auto_escalation: true
```

## 故障排查

### 1. 通知问题排查

#### 企业微信通知失败
```bash
# 检查企业微信权限
curl -X GET "https://open.woa.com/api/user/info" \
  -H "Authorization: Bearer $TOKEN"

# 检查接收者是否有效
curl -X POST "https://open.woa.com/api/user/validate" \
  -d '{"users": ["zhangsan", "lisi"]}'

# 查看通知发送日志
grep "wework-notification" /var/log/bkci/notify.log
```

#### 邮件通知失败
```bash
# 检查邮件服务状态
systemctl status postfix

# 查看邮件队列
mailq

# 检查邮件日志
tail -f /var/log/mail.log
```

### 2. 审批问题排查

#### 审批插件无法添加审批人
```bash
# 检查域名权限
curl -I "https://open.woa.com/"

# 申请域名权限
# 参考: https://iwiki.woa.com/p/15106859
```

#### 审批超时处理
```bash
# 查询审批状态
curl -X GET "https://devops.oa.com/ms/process/api/user/builds/$BUILD_ID/review" \
  -H "Authorization: Bearer $TOKEN"

# 手动完成超时审批
curl -X POST "https://devops.oa.com/ms/process/api/user/builds/$BUILD_ID/review/timeout" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"action": "CONTINUE"}'
```

## 总结

通知和审批是CI/CD流程中的重要环节，合理的配置可以：

1. **提高协作效率**: 及时通知相关人员，减少等待时间
2. **保障流程合规**: 通过审批确保关键操作得到授权
3. **降低操作风险**: 在关键节点设置人工检查点
4. **提升用户体验**: 通过智能通知减少信息过载

建议根据团队规模、项目特点和合规要求，设计合适的通知和审批策略，并在实践中持续优化。