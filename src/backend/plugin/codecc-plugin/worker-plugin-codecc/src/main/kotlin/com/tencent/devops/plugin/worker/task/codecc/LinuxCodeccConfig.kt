package com.tencent.devops.plugin.worker.task.codecc

val MOUNT_PATH = System.getProperty("devops.codecc.script.path", "/data/devops/codecc")
val SCRIPT_PATH = System.getProperty("devops.codecc.script.path", "$MOUNT_PATH/script")
val SOFTWARE_PATH = System.getProperty("devops.codecc.software.path", "$MOUNT_PATH/software")

val TOOL_SCRIT_PATH = System.getProperty("evops.codecc.script.tool.path", "build.py")
val PYLINT2_PATH = System.getProperty("devops.codecc.software.pylint2.path", "pylint2")
val PYLINT3_PATH = System.getProperty("devops.codecc.software.pylint3.path", "pylint3")
val NODE_PATH = System.getProperty("devops.codecc.software.node.path", "node/bin")
val JDK_PATH = System.getProperty("devops.codecc.software.jdk.path", "jdk/bin")
val GO_PATH = System.getProperty("devops.codecc.software.go.path", "go/bin")
val GOMETALINTER_PATH = System.getProperty("devops.codecc.software.gometalinter.path", "gometalinter/bin")
val LIBZIP_PATH = System.getProperty("devops.codecc.software.libzip.path", "libzip/bin")
val PHP_PATH = System.getProperty("devops.codecc.software.php.path", "php/bin")
val PYTHON2_PATH = System.getProperty("devops.codecc.software.python2.path", "python2/bin")
val PYTHON3_PATH = System.getProperty("devops.codecc.software.python3.path", "python3/bin")
val MONO_PATH = System.getProperty("devops.codecc.software.mono.path", "mono/bin")

val GRADLE_PATH = System.getProperty("devops.codecc.software.gradle.path", "gradle/bin")
val MAVEN_PATH = System.getProperty("devops.codecc.software.maven.path", "maven/bin")
val ANT_PATH = System.getProperty("devops.codecc.software.ant.path", "ant/bin")
