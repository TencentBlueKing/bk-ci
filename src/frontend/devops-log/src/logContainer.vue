<template>
    <article class="log-home">
        <section class="log-main">
            <header class="log-head">
                <span class="log-title"><status-icon :status="status"></status-icon>{{ title }}</span>
                <p class="log-tools">
                    <section class="tool-search">
                        <section class="searct-input">
                            <input type="text" class="">
                            <img src="./assets/svg/spinner.svg" v-if="isSearching">
                        </section>
                        <img src="./assets/svg/icon-angle-left.svg">
                        <span class="search-num">2</span>
                        <img src="./assets/svg/icon-angle-right.svg">
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
                            <li class="more-button" @click="showLogTime">{{ language('显示时间') }}</li>
                            <li class="more-button" @click="closeShowMore">{{ language('下载日志') }}</li>
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
            downLoadName: {
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
            }
        },

        data () {
            return {
                searchResult: [],
                showSearchIndex: 0,
                currentExe: this.executeCount,
                isSearching: false,
                showMore: false
            }
        },

        mounted () {
            document.addEventListener('mousedown', this.closeLog)
        },

        beforeDestroy () {
            document.removeEventListener('mousedown', this.closeLog)
        },

        methods: {
            language,

            showLogTime () {
                this.closeShowMore()
                this.$emit('update:showTime', !this.showTime)
            },

            closeShowMore () {
                this.showMore = false
            },

            closeLog (event) {
                let curTarget = event.target
                if (curTarget.classList.contains('log-home')) this.$emit('closeLog')
            },

            downLoad () {
                fetch(this.downLoadLink, {
                    method: 'GET',
                    headers: {
                        'content-type': 'application/json'
                    },
                    credentials: 'include'
                }).then((res) => {
                    if (res.status >= 200 && res.status < 300) {
                        return res
                    } else {
                        throw new Error(res.statusText)
                    }
                }).then(res => res.blob()).then((blob) => {
                    const a = document.createElement('a')
                    const url = window.URL || window.webkitURL || window.moxURL
                    a.href = url.createObjectURL(blob)
                    a.download = this.downLoadName + '.log'
                    document.body.appendChild(a)
                    a.click()
                    document.body.removeChild(a)
                }).catch((err) => {
                    console.error(err.message || err)
                }).finally(() => {
                    this.fileLoadPending = false
                })
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
        .tool-search {
            font-size: 12px;
            width: 250px;
            display: flex;
            height: 30px;
            align-items: center;
            justify-content: space-around;
            .searct-input {
                position: relative;
                height: 26px;
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
        }
    }
</style>
