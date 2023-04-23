/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

// If your plugin is direct dependent to the html webpack plugin:
const HtmlWebpackPlugin = require('html-webpack-plugin')

class assetsPlugin {
    apply (compiler) {
        compiler.hooks.compilation.tap('assetsPlugin', async (compilation) => {
            console.log('The compiler is starting a new compilation...')
            let assets
            // Static Plugin interface |compilation |HOOK NAME | register listener
            await HtmlWebpackPlugin.getHooks(compilation).beforeAssetTagGeneration.tapAsync(
                'assetsPlugin', // <-- Set a meaningful name here for stacktraces
                (data, cb) => {
                    // Manipulate the content
                    assets = data.assets
                    
                    // Tell webpack to move on
                    cb(null, data)
                }
            )

            HtmlWebpackPlugin.getHooks(compilation).beforeEmit.tapAsync(
                'assetsPlugin', // <-- Set a meaningful name here for stacktraces
                (data, cb) => {
                    // Manipulate the content
                    const assetsPos = data.html.indexOf('<!-- end devops:assets -->')
                    if (assetsPos > -1) {
                        data.html = `${data.html.slice(0, assetsPos)} 
                        <script type='text/javascript' src='${assets.js[0]}'></script>
                        <script type='text/javascript'>window.jsAssets = ${JSON.stringify(assets.js.slice(1))};</script>\n${data.html.slice(assetsPos)}`
                    }
                    // Tell webpack to move on
                    cb(null, data)
                }
            )
        })
    }
}

module.exports = assetsPlugin
