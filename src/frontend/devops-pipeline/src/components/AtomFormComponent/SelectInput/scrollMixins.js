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

export default {
    computed: {
        hasDropMenu () {
            return this.$refs.dropMenu ? this.$refs.dropMenu : false
        },
        pixelsToPointerTop () {
            let pixelsToPointerTop = 0
            if (this.hasDropMenu) {
                for (let i = 0; i < this.selectedPointer; i++) {
                    pixelsToPointerTop += this.$refs.dropMenu.children[0].children[i].offsetHeight
                }
            }
            return pixelsToPointerTop
        },
        selectedElementHeight () {
            const element = this.hasDropMenu ? this.$refs.dropMenu.children[0].children[this.selectedPointer] : false
            return element ? element.offsetHeight : 0
        },
        pixelsToPointerBottom () {
            return this.pixelsToPointerTop + this.selectedElementHeight
        }
    },
    methods: {
        getViewPort () {
            let top = 0
            let bottom = 0
            if (this.hasDropMenu) {
                const dropMenu = this.$refs.dropMenu
                top = dropMenu.scrollTop
                bottom = dropMenu.scrollTop + dropMenu.offsetHeight
            }
            return {
                top,
                bottom
            }
        },
        adjustViewPort () {
            const viewPort = this.getViewPort()
            if (this.pixelsToPointerTop <= viewPort.top) {
                return this.scrollTo(this.pixelsToPointerTop)
            } else if (this.pixelsToPointerBottom >= viewPort.bottom) {
                this.scrollTo(viewPort.top + this.selectedElementHeight)
            }
        },

        scrollTo (position) {
            if (this.hasDropMenu) {
                this.$refs.dropMenu.scrollTop = position || null
            }
        },

        handleKeyup () {
            if (this.selectedPointer > 0) {
                this.selectedPointer--
                this.adjustViewPort()
            }
        },

        handleKeydown () {
            if (!this.optionListVisible) this.optionListVisible = true
            if (this.selectedPointer < this.filteredList.length - 1) {
                this.selectedPointer++
                this.adjustViewPort()
            }
        },
        handleEnterOption () {
            const option = this.filteredList[this.selectedPointer]
            if (option && !option.disabled) {
                this.handleChange(this.name, option.id)
                this.handleBlur()
            }
        }
    }
}
