/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
    constructor (props) {
        this.isDev = props.isDev ?? false
    }
    apply (compiler) {
        console.log('[assets-webpack-plugin] Plugin initialized')
        
        compiler.hooks.emit.tapAsync('assetsPlugin', (compilation, callback) => {
            console.log('[assets-webpack-plugin] emit hook triggered')
            
            // Find the main HTML file (frontend#console#index.html)
            const htmlFileName = Object.keys(compilation.assets).find(name =>
                name.includes('frontend#console#index.html')
                || (name.endsWith('index.html') && !name.includes('static/'))
            )
            
            console.log('[assets-webpack-plugin] HTML file:', htmlFileName)
            
            if (!htmlFileName) {
                console.log('[assets-webpack-plugin] No main HTML file found')
                callback()
                return
            }
            
            // Fallback: get JS files from assets
            const jsFiles = Object.keys(compilation.assets)
                .filter(name => name.endsWith('.js') && !name.includes('static/'))
                .filter(name => name.includes('main.') || name.includes('bk-magic-vue-chunk'))
                .sort((a, b) => {
                    // Put dll first, bk-magic-vue-chunk second, then main
                    if (a.includes('dll') && !b.includes('dll')) return -1
                    if (!a.includes('dll') && b.includes('dll')) return 1
                    if (a.includes('bk-magic-vue-chunk') && !b.includes('bk-magic-vue-chunk')) return -1
                    if (!a.includes('bk-magic-vue-chunk') && b.includes('bk-magic-vue-chunk')) return 1
                    return 0
                }).map(name => `/console/${name}`)
            
            
            console.log('[assets-webpack-plugin] JS files:', jsFiles)
            
            if (jsFiles.length > 0) {
                const htmlAsset = compilation.assets[htmlFileName]
                let html = htmlAsset.source()
                
                const assetsPos = html.indexOf('<!-- end devops:assets -->')
                console.log('[assets-webpack-plugin] assetsPos:', assetsPos)
                
                if (assetsPos > -1) {
                    const devBundle = this.isDev ? `
                        <script type="text/javascript" src="${'//dev.devops.woa.com/assetsBundles.js'}"></script>
                    ` : ''
                    
                    const injectedCode = `${devBundle}<script type='text/javascript' src='${jsFiles[0]}'></script>
                    <script type='text/javascript'>window.jsAssets = ${JSON.stringify(jsFiles.slice(1))};</script>\n`
                    
                    html = `${html.slice(0, assetsPos)} 
                    ${injectedCode}${html.slice(assetsPos)}`
                    
                    // Update the asset
                    compilation.assets[htmlFileName] = {
                        source: () => html,
                        size: () => html.length
                    }
                    
                    console.log('[assets-webpack-plugin] HTML modified successfully')
                }
            }
            
            callback()
        })
    }
}

module.exports = assetsPlugin
