# Java 插件开发

## 目录结构

```
my-java-atom/
├── task.json
├── pom.xml
├── settings.xml
└── src/main/
    ├── java/com/example/atom/
    │   ├── AtomParam.java
    │   └── DemoAtom.java
    └── resources/META-INF/services/
        └── com.tencent.bk.devops.atom.spi.TaskAtom
```

## task.json

```json
{
    "atomCode": "myJavaAtom",
    "defaultLocaleLanguage": "zh_CN",
    "execution": {
        "language": "java",
        "minimumVersion": "1.8",
        "demands": [],
        "target": "java -jar myJavaAtom-jar-with-dependencies.jar"
    },
    "input": {
        "desc": {
            "label": "描述",
            "type": "vuex-input",
            "placeholder": "请输入描述信息",
            "required": true
        }
    },
    "output": {
        "testResult": {
            "type": "string",
            "description": "执行结果"
        }
    }
}
```

## pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.tencent.bk.devops.atom</groupId>
        <artifactId>sdk-dependencies</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>myJavaAtom</artifactId>
    <version>1.0.0</version>

    <properties>
        <sdk.version>1.1.58</sdk.version>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.tencent.bk.devops.atom</groupId>
            <artifactId>java-atom-sdk</artifactId>
            <version>${sdk.version}</version>
        </dependency>
    </dependencies>

    <build>
        <finalName>${project.name}</finalName>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

## AtomParam.java

```java
package com.example.atom;

import com.tencent.bk.devops.atom.pojo.AtomBaseParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AtomParam extends AtomBaseParam {
    private String desc;
}
```

## DemoAtom.java

```java
package com.example.atom;

import com.tencent.bk.devops.atom.AtomContext;
import com.tencent.bk.devops.atom.common.Status;
import com.tencent.bk.devops.atom.pojo.AtomResult;
import com.tencent.bk.devops.atom.pojo.StringData;
import com.tencent.bk.devops.atom.spi.AtomService;
import com.tencent.bk.devops.atom.spi.TaskAtom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AtomService(paramClass = AtomParam.class)
public class DemoAtom implements TaskAtom<AtomParam> {

    private static final Logger logger = LoggerFactory.getLogger(DemoAtom.class);

    @Override
    public void execute(AtomContext<AtomParam> atomContext) {
        AtomParam param = atomContext.getParam();
        AtomResult result = atomContext.getResult();

        logger.info("输入参数: {}", param.getDesc());

        // 参数校验 — 失败时设置 errorType 和 errorCode
        if (param.getDesc() == null || param.getDesc().isEmpty()) {
            result.setStatus(Status.failure);
            result.setMessage("描述不能为空");
            return;
        }

        // 业务逻辑
        logger.groupStart("执行任务");
        String output = "处理结果: " + param.getDesc();
        logger.info(output);
        logger.groupEnd("执行任务");

        // 设置输出
        result.setStatus(Status.success);
        result.getData().put("testResult", new StringData(output));
    }
}
```

## SPI 配置文件

`src/main/resources/META-INF/services/com.tencent.bk.devops.atom.spi.TaskAtom`:
```
com.example.atom.DemoAtom
```

## 本地调试

```bash
# 打包
mvn clean package

# 运行
java -jar target/myJavaAtom-jar-with-dependencies.jar
```

**验证检查点**:
1. 确认 `mvn clean package` 无编译错误
2. 确认 `target/` 下生成 `-jar-with-dependencies.jar`
3. 确认 `input.json` 和 `.sdk.json` 存在于执行目录
4. 执行后检查 `output.json` 中 `status` 为 `success`

## 错误处理

```java
try {
    // 业务逻辑
} catch (IOException e) {
    result.setErrorInfo(
        Status.failure,
        100002,  // 网络错误
        ErrorType.THIRD_PARTY,
        new String[]{"接口调用失败: " + e.getMessage()}
    );
}
```
