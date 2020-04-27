<template>
    <p class="log-tools">
        <section class="tool-search">
            <section class="searct-input">
                <input ref="inputMain" type="text" @input="startSearch" @keyup.enter="startSearch" placeholder="Search">
                <logo class="search-icon" name="spinner" v-if="isSearching"></logo>
                <template v-if="!isSearching">
                    <i class="bk-icon icon-close-circle-shape search-icon" v-if="inputStr" @click="clearSearch"></i>
                    <i class="bk-icon icon-search search-icon" v-else></i>
                </template>
            </section>
            <logo class="icon-click" name="icon-angle-left" @click.native="changeSearchIndex(-1)"></logo>
            <span class="search-num">{{`${searchIndex} / ${searchNum}`}}</span>
            <logo class="icon-click" name="icon-angle-right" @click.native="changeSearchIndex(1)"></logo>
        </section>
        <bk-select v-if="![0, 1].includes(+executeCount)" :placeholder="$t('execDetail.execTime')" class="log-execute" ext-popover-cls="execute-option" :value="currentExe" :clearable="false">
            <bk-option v-for="execute in executeCount"
                :key="execute"
                :id="execute"
                :name="execute"
                @click.native="changeExecute(execute)"
            >
            </bk-option>
        </bk-select>
        <section class="tool-more" v-bk-clickoutside="closeShowMore">
            <logo name="more" class="more-icon" @click.native="showMore = !showMore"></logo>
            <ul class="more-list" v-if="showMore">
                <li class="more-button" @click="showLogTime">{{ showTime ? $t('execDetail.hideTime') : $t('execDetail.showTime') }}</li>
                <a download class="more-button" @click="downLoad" :href="downLoadLink">{{ $t('execDetail.downLoadLog') }}</a>
            </ul>
        </section>
    </p>
</template>

<script>
    import Logo from '@/components/Logo'

    export default {
        components: {
            Logo
        },

        props: {
            worker: {
                type: Worker
            },
            executeCount: {
                type: Number
            },
            searchStr: {
                type: String
            },
            showTime: {
                type: Boolean
            },
            downLoadLink: {
                type: String
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
                searchRes: [],
                inputStr: ''
            }
        },

        mounted () {
            this.worker.addEventListener('message', (event) => {
                const data = event.data
                switch (data.type) {
                    case 'completeSearch':
                        this.handleSearch(data.num, data.curSearchRes, data.noScroll)
                        break
                    case 'completeGetSearchRes':
                        this.handleSearchRes(data.searchRes)
                        break
                }
            })
        },

        methods: {
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

            clearSearch () {
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
        position: absolute;
        right: 20px;
        top: 13px;
        display: flex;
        align-items: center;
        line-height: 30px;
        user-select: none;
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
                .icon-search.search-icon {
                    font-size: 14px;
                    top: 7px;
                }
                .search-icon {
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
            .search-num {
                color: #fff;
            }
            .icon-click {
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
            height: 32px;
            width: 15px;
            .more-icon {
                height: 32px;
                width: 24px;
                cursor: pointer;
                transform: rotate(90deg);
            }
            .more-list {
                position: absolute;
                top: 100%;
                right: -6px;
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
                    right: 0;
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
</style>
<style lang="scss">
    .execute-option {
        color: #c2cade;
        background: #222529;
        border-color: #444d56;
        .bk-options.bk-options-single .bk-option.is-selected {
            background: #222529;
            color: #c2cade;
        }
        .bk-options.bk-options-single .bk-option:hover {
            background: #0366d6;
            color: #c2cade;
        }
    }
</style>
