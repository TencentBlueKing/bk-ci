<template>
    <article class="log-home">
        <section :class="[currentTab === 'log' ? 'black-theme over-hidden' : 'white-theme', 'log-main']">
            <header class="log-head">
                <span class="log-title"><status-icon :status="status"></status-icon>{{ title }}</span>
                <slot name="tab"></slot>
                <p :class="[{ hidden: currentTab !== 'log' }, 'log-tools']">
                    <section class="tool-search">
                        <section class="searct-input">
                            <input type="text" @input="startSearch" placeholder="Search">
                            <img src="./assets/svg/spinner.svg" v-if="isSearching">
                        </section>
                        <img src="./assets/svg/icon-angle-left.svg" @click="changeSearchIndex(-1)">
                        <span class="search-num">{{`${searchIndex} / ${searchNum}`}}</span>
                        <img src="./assets/svg/icon-angle-right.svg" @click="changeSearchIndex(1)">
                    </section>
                    <bk-select v-if="![0, 1].includes(+executeCount)" :placeholder="'重试次数'" class="log-execute" :value="currentExe" :clearable="false">
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
                            <li class="more-button" @click="showLogTime">显示时间</li>
                            <a download class="more-button" @click="downLoad" :href="downLoadLink">下载日志</a>
                        </ul>
                    </section>
                </p>
            </header>

            <slot name="content"></slot>
        </section>
    </article>
</template>

<script>
    import statusIcon from './status'
    // eslint-disable-next-line
    const Worker = require('worker-loader!./worker.js')

    export default {
        provide () {
            return {
                worker: this.worker
            }
        },

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
            currentTab: {
                type: String
            }
        },

        data () {
            return {
                worker: new Worker(),
                currentExe: this.executeCount,
                isSearching: false,
                showMore: false,
                searchIndex: 0,
                realSearchIndex: 0,
                searchNum: 0,
                searchRes: []
            }
        },

        mounted () {
            document.addEventListener('mousedown', this.closeLog)
            this.worker.addEventListener('message', (event) => {
                const data = event.data
                switch (data.type) {
                    case 'completeSearch':
                        this.handleSearch(data.num, data.curSearchRes)
                        break
                    case 'completeGetSearchRes':
                        this.handleSearchRes(data.searchRes)
                        break
                }
            })
        },

        beforeDestroy () {
            document.removeEventListener('mousedown', this.closeLog)
            this.worker.terminate()
        },

        methods: {
            changeSearchIndex (dis) {
                if (this.searchRes.length <= 0) return
                // 展示的index
                let curIndex = this.searchIndex + dis
                if (curIndex <= 0) curIndex = this.searchNum
                if (curIndex > this.searchNum) curIndex = 1
                this.searchIndex = curIndex
                // 真实的index
                curIndex = this.realSearchIndex + dis
                if (curIndex < 0) curIndex = this.searchRes.length - 1
                if (curIndex >= this.searchRes.length) curIndex = 0
                if (curIndex >= 480 && curIndex <= 520) this.worker.postMessage({ type: 'getSearchRes', index: this.searchIndex - 1 })
                const curSearch = this.searchRes[curIndex]
                this.realSearchIndex = curIndex
                this.$emit('showSearchLog', curSearch)
            },

            startSearch (event) {
                this.isSearching = true
                window.clearTimeout(this.startSearch.timeId)
                this.startSearch.timeId = window.setTimeout(() => {
                    const target = event.target || {}
                    const val = target.value
                    this.$emit('update:searchStr', val)
                    this.worker.postMessage({ type: 'search', val })
                }, 300)
            },

            handleSearchRes (searchRes = []) {
                this.searchRes = searchRes
                this.realSearchIndex = 0
            },

            handleSearch (num = 0, searchRes) {
                this.handleSearchRes(searchRes)
                this.isSearching = false
                this.searchNum = num
                this.searchIndex = num > 0 ? 1 : 0
                if (num > 0) this.$emit('showSearchLog', searchRes[0])
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
            }
        }
    }
</script>

<style lang="scss" scoped>
    .log-tools {
        display: flex;
        align-items: center;
        line-height: 30px;
        &.hidden {
            z-index: -1;
        }
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
                img {
                    position: absolute;
                    height: 20px;
                    width: 20px;
                    top: 4px;
                    right: 5px;
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
                width: 210px;
                color: #fff;
                background: #2f363d;
                border-color: #444d56;
                box-shadow: 0 1px 15px rgba(27,31,35,.15);
                border: 1px solid #444d56;
                border-radius: 4px;
                margin: 5px 0;
                z-index: 2;
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
                    padding: 4px 8px 4px 32px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
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

    /deep/ .head-tab {
        font-size: 14px;
        span {
            cursor: pointer;
            font-weight: normal;
            padding: 4px 12px;
            color: #999999;
            background: #2e3342;
            &.active {
                color: #fff;
                background: #1a6df3;
                border: 1px solid #1a6df3;
            }
            &:first-child {
                border-radius: 3px 0 0 3px;
            }
            &:last-child {
                border-radius: 0 3px 3px 0;
            }
        }
    }

    /deep/ .log-folder {
        background-image: url("../../images/down.png");
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
            transition-property: transform, opacity;
            transition: transform 200ms cubic-bezier(.165,.84,.44,1), opacity 100ms cubic-bezier(.215,.61,.355,1);
            &.over-hidden {
                overflow: hidden;
            }
            .log-head {
                line-height: 52px;
                padding: 10px 20px 8px;
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
            &.black-theme {
                background: #1e1e1e;
            }
            &.white-theme {
                background: #fff;
                color: #222222;
                &.log-main .log-head {
                    color: #222222;
                    border-bottom: none;
                    background: #f4f5f7;
                }
                /deep/ .head-tab {
                    span {
                        cursor: pointer;
                        font-weight: normal;
                        border: 1px solid #dcdee5;
                        color: #222222;
                        background: #f4f5f7;
                        &.active {
                            color: #1a6df3;
                            background: #f4f5f7;
                            border-color: #1a6df3;
                        }
                        &:first-child {
                            border-radius: 3px 0 0 3px;
                        }
                        &:last-child {
                            border-radius: 0 3px 3px 0;
                        }
                        &:not(:last-child):not(.active) {
                            border-color: #dcdee5;
                        }
                    }
                }
            }
        }
    }
</style>
