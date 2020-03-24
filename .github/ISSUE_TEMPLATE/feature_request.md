---
name: Feature request
about: Suggest an idea for this project
title: ''
labels: ''
assignees: ''

---

### 作为
DevOps 团队里的Dev，要实践持续部署流水线

### 我希望
用 git 提交代码并实现 单一主干发布分支

### 以便于
- 让 Ops 通过 Master 分支发布
- 让 Dev 在开发分支上开发
- 让 QA 可以选择对应的提交进行部署测试

### 期望的验收标准
- 创建一个 Job，跟踪代码库 Master 分支的变化
- Master 分支如果有变化，自动立即构建并部署到开发环境。
- 测试环境和生产环境不能直接部署。 
