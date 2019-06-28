# 代码提交规范

## 代码检查建议
BK-CI工程一共涉及 JavaScript/Golang/Kotlin/Java/Lua 5种语言，我们开发团队默认使用如下工具进行代码检查，为了提高Merge效率，请提交前自行检测。
- ESLint
- Gometalinter
- detekt


## commit相关格式

个人分支提交commit消息格式

```
type:messsge issue
```

* type 范围信息
  * feature 新功能
  * fix 错误修复
  * docs 文档更改
  * style （格式化，缺少半冒号等;没有代码更改）
  * refactor 代码重构重构
  * test 添加缺失的测试，重构测试;没有生产代码更改
  * chore 构建脚本，任务等相关代码
* message 本次提交的描述 
* issue 本次提交关联的issue id

## Merge Request/Pull Request建议

开发者在各自fork分支可能会存在一些简单commit信息，提交Merge Request前建议使用git rebase进行commit精简。精简信息请参照上一
节。相关操作请参照如下流程：

```shell
#使用新分支做特性开发
git checkout feature1-pick
#多次调试与提交
git commit -m "xxx"
git commit -m "yyy"
git commit -m "zzz"
#如有引入新的第三方引用，使用dep管理引用
dep ensure -v -add github.com/org/project

#变基操作，合并多次变化（以下为3次），在feature1-pick分支下
#重新填写标准commit信息
git rebase -i HEAD~3

#推送到仓库远端
git push origin feature1-pick:feature1-pick

#提交PR/MR，等待合并
#......................
#......................

#PR/MR合并完成后，本地master分支跟进
git fetch upstream
git rebase upstream/master
```