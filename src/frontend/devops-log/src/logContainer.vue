<template>
    <article class="log-home">
        <section class="log-main">
            <header class="log-head">
                <span class="log-title"><status-icon :status="status"></status-icon>{{ title }}</span>
                <p class="log-tools">
                    <section class="tool-search">
                        <section class="searct-input">
                            <input ref="inputMain" type="text" @input="startSearch" @keyup.enter="startSearch" placeholder="Search">
                            <img class="input-tool" src="./assets/svg/spinner.svg" v-if="isSearching">
                            <template v-if="!isSearching">
                                <i class="bk-icon icon-close-circle-shape input-tool" v-if="inputStr" @click="clearSearch"></i>
                                <i class="bk-icon icon-search input-tool" v-else></i>
                            </template>
                        </section>
                        <img src="./assets/svg/icon-angle-left.svg" @click="changeSearchIndex(-1)">
                        <span class="search-num">{{`${searchIndex} / ${searchNum}`}}</span>
                        <img src="./assets/svg/icon-angle-right.svg" @click="changeSearchIndex(1)">
                    </section>
                    <bk-select v-if="![0, 1].includes(+executeCount)" :placeholder="language('重试次数')" class="log-execute" :value="currentExe" :clearable="false">
                        <bk-option v-for="execute in executeCount"
                            :key="execute"
                            :id="execute"
                            :name="execute"
                            @click.native="changeExecute(execute)"
                        >
                        </bk-option>
                    </bk-select>
                    <section class="tool-more" v-bk-clickoutside="closeShowMore">
                        <img src="./assets/svg/more.svg" class="more-icon" @click="showMore = !showMore">
                        <ul class="more-list" v-if="showMore">
                            <li class="more-button" @click="showLogTime">{{ showTime ? language('隐藏时间戳') : language('显示时间戳') }}</li>
                            <a download class="more-button" @click="downLoad" :href="downLoadLink">{{ language('下载日志') }}</a>
                        </ul>
                    </section>
                </p>
            </header>

            <slot></slot>
        </section>
    </article>
</template>

<script>
    import statusIcon from './status'
    import language from './locale'

    export default {
        components: {
            statusIcon
        },

        props: {
            downLoadLink: {
                type: String
            },
            executeCount: {
                type: Number,
                default: 0
            },
            status: {
                type: String
            },
            title: {
                type: String
            },
            showTime: {
                type: Boolean
            },
            searchStr: {
                type: String
            },
            worker: {
                type: Object
            }
        },

        data () {
            return {
                currentExe: this.executeCount,
                isSearching: false,
                showMore: false,
                searchIndex: 0,
                realSearchIndex: 0,
                searchNum: 0,
                inputStr: '',
                searchRes: []
            }
        },

        mounted () {
            document.addEventListener('mousedown', this.closeLog)
            this.worker.addEventListener('message', (event) => {
                const data = event.data
                switch (data.type) {
                    case 'completeSearch':
                        this.handleSearch(data.num, data.curSearchRes, data.noScroll, data.index)
                        break
                    case 'completeGetSearchRes':
                        this.handleSearchRes(data.searchRes)
                        break
                }
            })
        },

        beforeDestroy () {
            document.removeEventListener('mousedown', this.closeLog)
        },

        methods: {
            language,

            changeSearchIndex (dis) {
                if (this.searchRes.length <= 0) return
                // 展示的index
                let curIndex = this.searchIndex + dis
                if (curIndex <= 0) curIndex = this.searchNum
                if (curIndex > this.searchNum) curIndex = 1
                this.searchIndex = curIndex
                this.worker.postMessage({ type: 'changeSearchIndex', index: this.searchIndex - 1 })
                // 真实的index
                curIndex = this.realSearchIndex + dis
                if (curIndex < 0) curIndex = this.searchRes.length - 1
                if (curIndex >= this.searchRes.length) curIndex = 0
                if (curIndex >= 480 && curIndex <= 520) this.worker.postMessage({ type: 'getSearchRes', index: this.searchIndex - 1 })
                this.realSearchIndex = curIndex
                this.showSearchLog()
            },

            showSearchLog () {
                const curSearch = this.searchRes[this.realSearchIndex]
                if (curSearch.isInFold) {
                    this.worker.postMessage({
                        type: 'foldListData',
                        index: this.searchIndex - 1,
                        startIndex: curSearch.startIndex,
                        id: curSearch.refId
                    })
                } else {
                    this.$emit('showSearchLog', curSearch)
                }
            },

            clearSearch (event) {
                const inputEle = this.$refs.inputMain || {}
                inputEle.value = ''
                this.inputStr = ''
                this.$emit('update:searchStr', '')
                this.worker.postMessage({ type: 'search', val: '' })
            },

            startSearch (event) {
                this.isSearching = true
                window.clearTimeout(this.startSearch.timeId)
                this.startSearch.timeId = window.setTimeout(() => {
                    this.searchIndex = 1
                    this.realSearchIndex = 0
                    const target = event.target || {}
                    const val = target.value
                    this.inputStr = val
                    this.$emit('update:searchStr', val)
                    this.worker.postMessage({ type: 'search', val })
                }, 300)
            },

            handleSearchRes (searchRes = []) {
                this.searchRes = searchRes
                this.realSearchIndex = 0
            },

            handleSearch (num = 0, searchRes, noScroll) {
                this.searchRes = searchRes
                this.isSearching = false
                this.searchNum = num
                if (num <= 0) this.searchIndex = 0
                if (num <= 0 || noScroll) return
                this.showSearchLog()
            },

            showLogTime () {
                this.closeShowMore()
                this.$emit('update:showTime', !this.showTime)
            },

            closeShowMore () {
                this.showMore = false
            },

            closeLog (event) {
                const curTarget = event.target
                if (curTarget.classList.contains('log-home')) this.$emit('closeLog')
            },

            downLoad () {
                this.closeShowMore()
            },

            changeExecute (execute) {
                if (this.currentExe === execute) return
                this.currentExe = execute
                this.$emit('changeExecute', execute)
                this.clearSearch()
            }
        }
    }
</script>

<style lang="scss" scoped>
    .log-tools {
        display: flex;
        align-items: center;
        line-height: 30px;
        .tool-search {
            font-size: 12px;
            display: flex;
            height: 30px;
            align-items: center;
            justify-content: space-around;
            .searct-input {
                position: relative;
                height: 26px;
                margin-right: 5px;
                input {
                    vertical-align: super;
                    border: 0;
                    box-shadow: none;
                    width: 150px;
                    padding-right: 30px;
                    padding-left: 8px;
                    line-height: 26px;
                    border-radius: 3px;
                    color: #f6f8fa;
                    background-color: hsla(0,0%,100%,.125);
                }
                .input-tool {
                    position: absolute;
                    height: 20px;
                    width: 20px;
                    top: 4px;
                    right: 5px;
                    &.icon-search {
                        font-size: 16px;
                        top: 6px;
                    }
                    &.icon-close-circle-shape {
                        cursor: pointer;
                        font-size: 14px;
                        top: 7px;
                        &:hover {
                            color: #979ba5;
                        }
                    }
                }
            }
            >img {
                cursor: pointer;
                width: 25px;
                height: 25px;
                &:hover {
                    background-color: #2f363d;
                }
            }
        }
        .tool-more {
            position: relative;
            height: 24px;
            width: 32px;
            .more-icon {
                height: 24px;
                width: 32px;
                cursor: pointer;
            }
            .more-list {
                position: absolute;
                top: 100%;
                right: 0;
                left: auto;
                width: 180px;
                color: #fff;
                background: #2f363d;
                border-color: #444d56;
                box-shadow: 0 1px 15px rgba(27,31,35,.15);
                border: 1px solid #444d56;
                border-radius: 4px;
                margin: 5px 0;
                z-index: 101;
                &:before {
                    position: absolute;
                    display: inline-block;
                    content: "";
                    border: 8px solid transparent;
                    top: -16px;
                    right: 9px;
                    left: auto;
                    border-bottom-color: #444d56;
                }
                .more-button {
                    cursor: pointer;
                    color: #fff;
                    width: 100%;
                    text-align: left;
                    display: block;
                    padding: 4px 8px 4px 22px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    font-size: 12px;
                    line-height: 26px;
                    &:hover {
                        background: #0366d6;
                    }
                    &:not(:last-child) {
                        border-bottom: 1px solid #444D56;
                    }
                }
            }
        }
        .log-execute {
            width: 100px;
            margin-right: 10px;
            color: #c2cade;
            background: #222529;
            border-color: #444d56;
            font-size: 14px;
            &:hover {
                color: #fff;
                background: #292c2d;
            }
        }
    }

    /deep/ .log-folder {
        background-image: url("./assets/png/down.png");
        display: inline-block;
        height: 16px;
        width: 16px;
        position: absolute;
        cursor: pointer;
        transform: rotate(0deg);
        transition: transform 200ms;
        top: 0;
        right: -20px;
        &.show-all {
            transform: rotate(-90deg);
        }
    }
    .log-home {
        position: fixed;
        top: 0;
        left: 0;
        bottom: 0;
        right: 0;
        background-color: rgba(0, 0, 0, .2);
        z-index: 1000;
        .scroll-loading {
            position: absolute;
            bottom: 0;
            width: 100%;
            height: 16px;
        }
        .log-main {
            position: relative;
            width: 80%;
            height: calc(100% - 32px);
            float: right;
            display: flex;
            flex-direction: column;
            margin: 16px;
            border-radius: 6px;
            overflow: hidden;
            transition-property: transform, opacity;
            transition: transform 200ms cubic-bezier(.165,.84,.44,1), opacity 100ms cubic-bezier(.215,.61,.355,1);
            background: #1e1e1e;
            .log-head {
                line-height: 48px;
                padding: 5px 20px;
                background-color: #252935;
                border-bottom: 1px solid;
                border-bottom-color: #2b2b2b;
                display: flex;
                align-items: center;
                justify-content: space-between;
                color: #d4d4d4;
                .log-title {
                    display: flex;
                    align-items: center;
                }
            }
        }
    }
</style>
