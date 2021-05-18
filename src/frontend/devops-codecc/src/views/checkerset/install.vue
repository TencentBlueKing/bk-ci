<template>
    <bk-dialog v-model="visiable"
        width="650px"
        ext-cls="install-more-dialog"
        :position="{ top: positionTop }"
        :theme="'primary'"
        :close-icon="false">
        <div class="main-content" v-bkloading="{ isLoading: loading, opacity: 0.3 }">
            <div class="info-header">
                <span>{{$t('更多规则集')}}<i class="bk-icon icon-refresh checkerset-fresh" :class="fetchingList ? 'spin-icon' : ''" @click="refresh" /></span>
                <bk-select class="search-select" v-model="language" multiple style="width: 120px;" :placeholder="$t('请选择语言')">
                    <bk-option v-for="option in codeLangs"
                        :key="option.displayName"
                        :id="option.displayName"
                        :name="option.displayName">
                    </bk-option>
                </bk-select>
                <bk-input
                    class="search-input"
                    :placeholder="'快速搜索'"
                    :clearable="true"
                    :right-icon="'bk-icon icon-search'"
                    v-model="keyWord"
                    @input="handleClear"
                    @enter="handleKeyWordSearch">
                </bk-input>
            </div>
            <bk-tab class="checkerset-tab" size="small" ref="tab" :active.sync="classifyCode" type="unborder-card">
                <bk-tab-panel
                    class="checkerset-panel"
                    ref="checkersetPanel"
                    v-for="classify in classifyCodeList"
                    :key="classify.enName"
                    :name="classify.enName"
                    :label="classify.cnName"
                    render-directive="if">
                    <section ref="checkersetList">
                        <div class="info-card"
                            v-for="(checkerSet, index) in checkerSetList"
                            :key="index"
                            @mouseover="currentHoverItem = index"
                            @mouseout="currentHoverItem = -1">
                            <div :class="['checkerset-icon', getIconColorClass(checkerSet.checkerSetId)]">{{(checkerSet.checkerSetName || '')[0]}}</div>
                            <div class="info-content">
                                <p class="checkerset-main">
                                    <span class="name">{{checkerSet.checkerSetName}}</span>
                                    <span v-if="['DEFAULT', 'RECOMMEND'].includes(checkerSet.checkerSetSource)"
                                        :class="['use-mark', { 'preferred': checkerSet.checkerSetSource === 'DEFAULT', 'recommend': checkerSet.checkerSetSource === 'RECOMMEND' }]"
                                    >{{checkerSet.checkerSetSource === 'DEFAULT' ? '精选' : '推荐'}}</span>
                                    <span class="language" :title="getCodeLang(checkerSet.codeLang)">{{getCodeLang(checkerSet.codeLang)}}</span>
                                </p>
                                <p class="checkerset-desc" :title="checkerSet.description">{{checkerSet.description || '暂无描述'}}</p>
                                <p class="other-msg">
                                    <span>由 {{ checkerSet.creator }} 发布</span>
                                    <span>共 {{checkerSet.checkerCount || 0}} 条规则</span>
                                </p>
                            </div>
                            <div class="info-operate">
                                <bk-button
                                    :theme="!checkerSet.projectInstalled ? 'primary' : 'default'"
                                    size="small"
                                    class="install-btn"
                                    :disabled="checkerSet.projectInstalled"
                                    @click="install(checkerSet)"
                                >{{checkerSet.projectInstalled ? '已安装' : '安装'}}</bk-button>
                            </div>
                        </div>
                    </section>
                    <div v-if="!checkerSetList.length">
                        <div class="codecc-table-empty-text">
                            <img src="../../images/empty.png" class="empty-img">
                            <div>{{$t('暂无数据')}}</div>
                        </div>
                    </div>
                </bk-tab-panel>
            </bk-tab>
        </div>
        <div slot="footer">
            <bk-button :theme="'default'" @click="closeDialog">{{$t('关闭')}}</bk-button>
        </div>
    </bk-dialog>
</template>

<script>
    import { mapState } from 'vuex'

    export default {
        props: {
            visiable: {
                type: Boolean,
                default: false
            },
            refreshList: Function
        },
        data () {
            return {
                fetchingList: false,
                loading: false,
                loadEnd: false,
                pageChange: false,
                isLoadingMore: false,
                isOpen: false,
                currentHoverItem: -1,
                keyWord: '',
                language: [],
                params: {
                    quickSearch: '',
                    checkerSetCategory: [],
                    checkerSetLanguage: [],
                    pageNum: 1,
                    pageSize: 20
                },
                classifyCode: 'all',
                classifyCodeList: [{ cnName: '所有', enName: 'all' }],
                checkerSetList: [],
                codeLangs: []
            }
        },
        computed: {
            ...mapState([
                'toolMeta'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            renderList () {
                let target = []
                if (this.classifyCode === 'all') {
                    target = [...this.checkerSetList]
                } else {
                    target = this.checkerSetList.filter(item => item.catagories.some(val => val.enName === this.classifyCode))
                }
                return target || []
            },
            positionTop () {
                const top = (window.innerHeight - 693) / 2
                return top > 0 ? top : 0
            }
        },
        watch: {
            visiable (newVal) {
                if (newVal) {
                    this.isOpen = true
                    this.keyWord = ''
                    this.language = []
                    this.params = {
                        quickSearch: '',
                        checkerSetCategory: [],
                        checkerSetLanguage: [],
                        pageNum: 1,
                        pageSize: 20
                    }
                    this.classifyCode = 'all'
                    this.requestList(true)
                    this.addScrollLoadMore()
                } else {
                    this.classifyCode = ''
                }
            },
            classifyCode (newVal) {
                if (this.visiable && !this.isOpen) {
                    this.removeScrollLoadMore()
                    this.params.pageNum = 1
                    this.params.checkerSetCategory = ['all'].includes(newVal) ? [] : [newVal]
                    this.requestList(true)
                    this.addScrollLoadMore()
                }
            },
            language (newVal) {
                if (!this.isOpen) {
                    this.pageChange = false
                    this.params.checkerSetLanguage = newVal
                    this.resetScroll()
                    this.requestList(false, this.params)
                }
            }
        },
        created () {
            this.getFormParams()
        },
        mounted () {
            // this.addScrollLoadMore()
        },
        beforeDestroy () {
            this.removeScrollLoadMore()
        },
        methods: {
            closeDialog () {
                this.$emit('update:visiable', false)
            },
            async getFormParams () {
                const res = await this.$store.dispatch('checkerset/params')
                this.classifyCodeList = [...this.classifyCodeList, ...res.catatories]
                this.codeLangs = res.codeLangs
            },
            async requestList (isInit, params = this.params) {
                this.loading = true
                this.isLoadingMore = true
                params.projectInstalled = this.classifyCode === 'store' ? false : undefined
                const res = await this.$store.dispatch('checkerset/otherList', params).finally(() => {
                    this.loading = false
                    this.isOpen = false
                })
                this.checkerSetList = this.pageChange ? this.checkerSetList.concat(res.content) : res.content
                this.loadEnd = res.last
                this.pageChange = false
                this.isLoadingMore = false
            },
            getIconColorClass (checkerSetId) {
                return checkerSetId ? `c${(checkerSetId[0].charCodeAt() % 6) + 1}` : 'c1'
            },
            getCodeLang (codeLang) {
                const names = this.toolMeta.LANG.map(lang => {
                    if (lang.key & codeLang) {
                        return lang.name
                    }
                }).filter(name => name)
                return names.join('、')
            },
            handleKeyWordSearch (value) {
                this.keyWord = value.trim()
                this.params.quickSearch = this.keyWord
                this.resetScroll()
                this.requestList(false, this.params)
            },
            handleClear (str) {
                if (str === '') {
                    this.keyValue = ''
                    this.handleKeyWordSearch('')
                }
            },
            refresh () {
                if (this.keyWord === '') {
                    this.requestList(true)
                } else {
                    this.keyValue = this.keyValue.trim()
                    const params = { quickSearch: this.keyWord }
                    this.requestList(true, params)
                }
            },
            resetScroll () {
                const target = document.querySelector('.checkerset-panel')
                if (target) target.scrollTop = 0
                this.params.pageNum = 1
            },
            scrollLoadMore (event) {
                const target = event.target
                const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
                if (bottomDis < 10 && !this.loadEnd && !this.isLoadingMore) {
                    this.params.pageNum++
                    this.pageChange = true
                    this.requestList(false, this.params)
                }
            },
            addScrollLoadMore () {
                this.$nextTick(() => {
                    const mainBody = document.querySelector(`.checkerset-panel`)
                    if (mainBody) mainBody.addEventListener('scroll', this.scrollLoadMore, { passive: true })
                })
            },
            removeScrollLoadMore () {
                const mainBody = document.querySelector('.checkerset-panel')
                if (mainBody) mainBody.removeEventListener('scroll', this.scrollLoadMore, { passive: true })
            },
            resetInsatllStatus (checkerSetId) {
                this.checkerSetList = this.checkerSetList.map(checker => {
                    return {
                        ...checker,
                        projectInstalled: checker.checkerSetId === checkerSetId ? true : checker.projectInstalled
                    }
                })
            },
            install (checkerSet) {
                const params = {
                    type: 'PROJECT',
                    projectId: this.projectId,
                    checkerSetId: checkerSet.checkerSetId,
                    version: checkerSet.version
                }
                this.$store.dispatch('checkerset/install', params).then(res => {
                    if (res.code === '0') {
                        this.$bkMessage({ theme: 'success', message: '安装成功' })
                        this.refreshList()
                        this.resetInsatllStatus(checkerSet.checkerSetId)
                    }
                }).catch(e => {
                    console.error(e)
                })
            }
        }
    }
</script>
<style lang="postcss">
    .install-more-dialog {
        .bk-dialog-tool {
            display: none;
        }
        .bk-dialog-body {
            padding: 20px 24px 16px 32px;
        }
        .main-content {
            height: 600px;
        }
        .info-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            span {
                color: #222222;
                font-size: 14px;
            }
        }
        .checkerset-fresh {
            cursor: pointer;
            display: inline-block;
            font-size: 14px;
            padding: 4px;
            margin-left: 3px;
            position: relative;
            color: #3c96ff;;
            &.spin-icon {
                color: #c3cdd7;
            }
        }
        .search-input {
            width: 180px;
        }
        .search-select {
            width: 120px;
            margin-left: 150px;
        }
        .checkerset-tab {
            margin-top: 10px;
            height: calc(100% - 40px);
            border: 0;
            font-size: 12px;
            font-weight: 500;
            overflow: hidden;
            div.bk-tab-section {
                height: calc(100% - 42px);
                overflow-y: hiden;
                padding: 0;
                .bk-tab-content {
                    height: 100%;
                    overflow: auto;
                    &::-webkit-scrollbar {
                        width: 6px;
                    }
                }
            }
            .bk-tab-header {
                .bk-tab-label-wrapper {
                    .bk-tab-label-list {
                        .bk-tab-label-item {
                            padding: 0 15px;
                            min-width: auto;
                            .bk-tab-label {
                                font-size: 12px;
                                color: #63656e;
                                &.active {
                                    font-weight: bold;
                                }
                            }
                        }
                    }
                }
            }
        }
        .info-card {
            display: flex;
            align-items: center;
            margin: 0 0 6px;
            padding: 0 10px 0 8px;
            height: 80px;
            &:first-child {
                margin-top: 18px;
            }
            .checkerset-icon {
                width: 48px;
                height: 48px;
                font-size: 24px;
                font-weight: bold;
                margin-right: 14px;
                text-align: center;
                line-height: 48px;
                color: #fff;
                border-radius: 8px;
                &.c1 { background: #37dab9; }
                &.c2 { background: #7f6efa; }
                &.c3 { background: #ffca2b; }
                &.c4 { background: #fe8f65; },
                &.c5 { background: #f787d9; },
                &.c6 { background: #5e7bff; }
            }
            .logo {
                width: 50px;
                height: 50px;
                font-size: 50px;
                line-height: 50px;
                margin-right: 15px;
                color: #c3cdd7;
            }
            .checkerset-main {
                display: flex;
                align-items: center;
                line-height: 14px;
            }
            .use-mark {
                margin-left: 8px;
                font-size: 12px;
                height: 20px;
                display: inline-block;
                padding: 2px 10px;
                border-radius: 2px;
                white-space: nowrap;
                &.preferred {
                    background-color: rgba(134, 223, 38, 0.3);
                    color: rgba(53, 99, 22, 0.8);
                    border: 1px solid rgba(102, 197, 1, 0.3);
                }
                &.recommend {
                    background-color: rgba(211, 224, 255, 0.3);
                    color: rgba(61, 76, 138, 0.8);
                    border: 1px solid rgba(187, 204, 244, 0.3);
                }
            }
            .info-content {
                padding: 24px 0 20px;
                flex: 1;
                color: #4A4A4A;
                font-size: 14px;
                padding-right: 10px;
                flex-direction: column;
                justify-content: space-between;
                overflow: hidden;
                .name {
                    max-width: 240px;
                    white-space: nowrap;
                    overflow: hidden;
                    color: #222222;
                    font-size: 14px;
                    font-weight: bold;
                    text-overflow: ellipsis;
                }
                .language {
                    max-width: 300px;
                    white-space: nowrap;
                    overflow: hidden;
                    display: inline;
                    margin-left: 8px;
                    padding-left: 8px;
                    font-size: 12px;
                    color: #63656e;
                    border-left: 1px solid #d8d8d8;
                    text-overflow: ellipsis;
                }
            }
            .checkerset-desc {
                text-overflow: ellipsis;
                overflow: hidden;
                width: 100%;
                white-space: nowrap;
                font-size: 12px;
                color: #666666;
                position: relative;
                top: 2px;
                font-weight: normal;
            }
            .other-msg {
                font-size: 12px;
                color: #bbbbbb;
                span:first-child {
                    width: 120px;
                    display: inline-block;
                }
                span:last-child {
                    margin-left: 10px;
                }
            }
            .install-btn {
                line-height: 22px;
                font-weight: normal;
            }
        }
        .info-card:hover {
            background-color: #f3f7fe;
        }
        .search-result {
            .info-card:first-child {
                margin-top: 0;
            }
            &::-webkit-scrollbar {
                width: 6px;
            }
            height: calc(100% - 60px);
            margin-top: 20px;
            overflow: auto;
        }
        .codecc-table-empty-text {
            text-align: center;
            margin-top: 180px;
        }
    }
</style>
