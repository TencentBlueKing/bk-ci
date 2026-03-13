# NodeJS 插件开发

## 目录结构

```
my-nodejs-atom/
├── task.json
├── package.json
├── rollup.config.js
├── index.js
└── .gitignore
```

## task.json

```json
{
    "atomCode": "myNodejsAtom",
    "execution": {
        "language": "nodejs",
        "demands": [],
        "target": "node dist/bundle.js"
    },
    "input": {
        "inputDemo": {
            "label": "输入示例",
            "type": "vuex-input",
            "required": true
        }
    },
    "output": {
        "outputDemo": {
            "type": "string",
            "description": "输出示例"
        }
    }
}
```

## package.json

```json
{
    "name": "myNodejsAtom",
    "version": "1.0.0",
    "main": "./dist/bundle.js",
    "dependencies": {
        "@tencent/nodejs_atom_sdk": "^1.1.12"
    },
    "devDependencies": {
        "@babel/core": "^7.4.5",
        "@babel/preset-env": "^7.4.5",
        "rollup": "^1.16.2",
        "rollup-plugin-babel": "^4.3.3"
    }
}
```

## rollup.config.js

```javascript
import babel from 'rollup-plugin-babel'

export default {
    input: 'index.js',
    output: {
        file: 'dist/bundle.js',
        format: 'cjs'
    },
    plugins: [babel()]
}
```

## index.js

```javascript
import {
    getInputParams,
    setOutput,
    BK_ATOM_STATUS,
    BK_OUTPUT_TEMPLATE_TYPE
} from '@tencent/nodejs_atom_sdk'

const params = getInputParams()
console.log('输入参数:', params)

const inputDemo = params.inputDemo || ''

// 参数校验
if (!inputDemo) {
    setOutput({
        "type": BK_OUTPUT_TEMPLATE_TYPE.DEFAULT,
        "status": BK_ATOM_STATUS.FAILURE,
        "errorCode": 100004,
        "errorType": 1,
        "message": "缺少必要参数: inputDemo",
        "data": {}
    })
    process.exit(1)
}

const result = `处理结果: ${inputDemo}`

setOutput({
    "type": BK_OUTPUT_TEMPLATE_TYPE.DEFAULT,
    "status": BK_ATOM_STATUS.SUCCESS,
    "data": {
        "outputDemo": {
            "type": "string",
            "value": result
        }
    }
})
```

## 本地调试

```bash
# 安装依赖
npm install

# 打包
rollup -c rollup.config.js

# 运行（需要先在根目录创建 mock 目录，将 input.json 放入）
node dist/bundle.js
```

**验证检查点**:
1. 确认 `npm install` 无错误
2. 确认 `dist/bundle.js` 已生成
3. 确认 `input.json` 和 `.sdk.json` 存在于执行目录
4. 执行后检查 `output.json` 中 `status` 为 `success`
