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
            let groupItemHeight = 0
            if (this.hasDropMenu) {
                if (this.hasGroup) {
                    for (let i = 0; i < this.selectedGroupPointer; i++) {
                        groupItemHeight += this.$refs.dropMenu.children[0].children[i].offsetHeight
                    }
                    for (let j = 0; j <= this.selectedPointer; j++) {
                        pixelsToPointerTop += (this.$refs.dropMenu.children[0].children[this.selectedGroupPointer].children[1 + j].offsetHeight)
                    }
                    this.selectedGroupPointer = !this.selectedGroupPointer ? 0 : this.selectedGroupPointer
                    pixelsToPointerTop += groupItemHeight
                } else {
                    for (let i = 0; i < this.selectedPointer; i++) {
                        pixelsToPointerTop += this.$refs.dropMenu.children[0].children[i].offsetHeight
                    }
                }
            }

            return pixelsToPointerTop
        },
        selectedElementHeight () {
            let element
            if (this.hasGroup) {
                element = this.hasDropMenu ? this.$refs.dropMenu.children[0].children[this.selectedGroupPointer].children[this.selectedPointer] : false
            } else {
                element = this.hasDropMenu ? this.$refs.dropMenu.children[0].children[this.selectedPointer] : false
            }
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
        adjustViewPort (isLeapfrog) {
            const viewPort = this.getViewPort()
            if (this.pixelsToPointerTop <= viewPort.top) {
                return this.scrollTo(this.pixelsToPointerTop)
            } else if (this.pixelsToPointerBottom >= viewPort.bottom) {
                this.scrollTo(isLeapfrog ? viewPort.top + this.selectedElementHeight + 36 : viewPort.top + this.selectedElementHeight)
            }
        },

        scrollTo (position) {
            if (this.hasDropMenu) {
                this.$refs.dropMenu.scrollTop = position || null
            }
        },

        handleKeyup () {
            if (this.hasGroup) {
                if (this.selectedGroupPointer >= 0 && this.selectedPointer > 0) {
                    this.selectedPointer--
                    this.adjustViewPort()
                } else if (this.selectedGroupPointer > 0 && this.selectedPointer === 0) {
                    this.selectedGroupPointer--
                    this.selectedPointer = this.filteredList[this.selectedGroupPointer].children.length - 1
                    this.adjustViewPort(true)
                }
            } else {
                if (this.selectedPointer > 0) {
                    this.selectedPointer--
                    this.adjustViewPort()
                }
            }
        },

        handleKeydown () {
            if (!this.optionListVisible) this.optionListVisible = true
            if (this.hasGroup) {
                if (this.selectedGroupPointer <= this.filteredList.length - 1 && this.selectedPointer < this.filteredList[this.selectedGroupPointer].children.length - 1) {
                    this.selectedPointer++
                    this.adjustViewPort()
                } else if (this.selectedGroupPointer < this.filteredList.length - 1 && this.selectedPointer === this.filteredList[this.selectedGroupPointer].children.length - 1) {
                    this.selectedGroupPointer++
                    this.selectedPointer = 0
                    this.adjustViewPort(true)
                }
            } else {
                if (this.selectedPointer < this.filteredList.length - 1) {
                    this.selectedPointer++
                    this.adjustViewPort()
                }
            }
        },
        handleEnterOption () {
            let option = {}
            if (this.hasGroup) {
                option = this.filteredList[this.selectedGroupPointer].children[this.selectedPointer]
            } else {
                option = this.filteredList[this.selectedPointer]
            }
            if (option && !option.disabled) {
                this.handleChange(this.name, option.id)
                this.$nextTick(() => {
                    this.handleBlur()
                })
            }
        }
    }
}
