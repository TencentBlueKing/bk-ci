package com.tencent.devops.remotedev.utils

import org.junit.jupiter.api.Test

internal class DevfileUtilTest {

    @Test
    fun parseDevfile() {
        val yaml = """
version: 1.0.0

image: mirror.tencent.com/bkci/worksapce-full:1.0.0

vscode:
  extensions:
  - golang.go
  - eamodio.gitlens
  - GitHub.vscode-pull-request-github

envs:
  NAME: xxxx
  MY_PASSWORD: 1

ports:
- name: local-web
  desc: 应用预览端口
  port: 8080

commands:
  postCreateCommand: "go mod tidy;echo 'go init dome'"

        """
        val devfile = DevfileUtil.parseDevfile(yaml)
        println(devfile)
    }
}
