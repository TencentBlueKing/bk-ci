<template>
    <div class="cc-checkersets">
        <div class="cc-side">
            <div class="cc-filter">
                <span class="cc-filter-txt">{{$t('过滤器')}}</span>
                <span class="cc-filter-clear fr" @click="handleClear">
                    <i class="codecc-icon icon-filter-2 cc-link"></i>
                </span>
            </div>
            <bk-input
                class="checker-search"
                :placeholder="'请输入，按enter键搜索'"
                :clearable="true"
                :right-icon="'bk-icon icon-search'"
                v-model="keyWord"
                @enter="handleKeyWordSearch"
                @clear="handleKeyWordSearch">
            </bk-input>
            <div class="cc-filter-content">
                <cc-collapse
                    ref="searchParams"
                    :is-ckecker-set="true"
                    :search="search"
                    :active-name="activeName"
                    @updateActiveName="updateActiveName"
                    @handleSelect="handleSelect">
                </cc-collapse>
            </div>
        </div>
        <div class="cc-main" v-bkloading="{ isLoading: pageLoading, opacity: 0.6 }">
            <div class="list-tool-bar">
                <section class="bar-info">
                    <bk-button icon="plus" theme="primary" @click="handleCreate">{{$t('创建规则集')}}</bk-button>
                    <bk-button :text="true" icon-right="icon-plus-circle" @click="installMore">{{$t('更多规则集')}}</bk-button>
                </section>
                <span class="total-count">共<span v-if="checkersetList">{{checkersetList.length}}</span>{{$t('个规则集')}}</span>
            </div>
            <main class="checkerset-content" v-show="isFetched">
                <bk-virtual-scroll v-if="isFetched && checkersetList.length" ref="domVirtualScroll"
                    class="dom-virtual-scroll"
                    :item-height="100">
                    <template slot-scope="item">
                        <card
                            :checkerset="item.data"
                            :permission-list="permissionList"
                            :handle-mannge="handleCheckerset">
                        </card>
                    </template>
                </bk-virtual-scroll>
                <!-- <card v-for="(checkerset, index) in checkersetList" :key="index"
                    :checkerset="checkerset"
                    :permission-list="permissionList"
                    :handle-mannge="handleCheckerset"
                ></card> -->
                <div v-if="!checkersetList.length">
                    <div class="codecc-table-empty-text">
                        <img src="../../images/empty.png" class="empty-img">
                        <div>{{$t('暂无数据')}}</div>
                    </div>
                </div>
            </main>
        </div>
        <bk-dialog
            v-model="delVisiable"
            :theme="'primary'"
            :mask-close="false"
            @cancel="delVisiable = false"
            @confirm="handleDelete"
            :title="curHandleItem.projectId === projectId ? '删除规则集' : '卸载规则集'">
            <span v-if="curHandleItem.projectId === projectId">{{`删除【${curHandleItem.checkerSetName}】规则集后无法恢复。本项目中将无法使用该规则集，其他项目中若已安装该规则集仍可继续使用。`}}</span>
            <span v-else>{{`【卸载${curHandleItem.checkerSetName}】规则集后，本项目将无法使用该规则集。可通过“更多规则集”重新安装。`}}</span>
        </bk-dialog>
        <create
            :visiable.sync="sliderVisiable"
            :is-edit.sync="isEdit"
            :edit-obj="editObj"
            :refresh-detail="refreshList"
        ></create>
        <install
            :visiable.sync="dialogVisiable"
            :refresh-list="refreshList"
        ></install>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import ccCollapse from '@/components/cc-collapse'
    import card from './card'
    import create from './create'
    import install from './install'

    export default {
        components: {
            ccCollapse,
            card,
            create,
            install
        },
        data () {
            return {
                activeName: [],
                search: [],
                editObj: {},
                selectParams: {},
                curHandleItem: '',
                delVisiable: false,
                sliderVisiable: false,
                dialogVisiable: false,
                isEdit: false,
                keyWord: undefined,
                checkersetList: [],
                permissionList: [],
                pageLoading: false,
                isFetched: false
            }
        },
        computed: {
            ...mapState([
                'toolMeta'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            queryOptStorage () {
                return localStorage.getItem('checkerSetQueryObj') ? JSON.parse(localStorage.getItem('checkerSetQueryObj')) : {}
            }
        },
        watch: {
            selectParams: {
                handler (value) {
                    this.initSearch(false)
                    this.fetchList(false, value)
                    this.setLocalQueryOpt(value)
                },
                deep: true
            },
            checkersetList (value) {
                this.addVirtualScroll()
            }
        },
        created () {
            this.checkPermission()
            this.initSearch(true)
            this.fetchList(true)
        },
        mounted () {
            if (this.$route.query.installMore) this.dialogVisiable = true
            if (this.$route.hash === '#new') {
                setTimeout(() => {
                    this.handleCreate()
                }, 800)
            }
        },
        methods: {
            async checkPermission () {
                const params = {
                    projectId: this.projectId,
                    user: this.$store.state.user.username
                }
                const res = await this.$store.dispatch('checkerset/permission', params)
                this.permissionList = res.data
            },
            async initSearch (isInit) {
                const data = Object.assign(this.selectParams, { projectId: this.projectId })
                const res = await this.$store.dispatch('checkerset/count', data)
                this.search = res
                if (isInit) {
                    this.$nextTick(() => {
                        if (Object.keys(this.queryOptStorage).length) {
                            this.activeName = Object.keys(this.queryOptStorage)
                        } else {
                            this.activeName = ['checkerSetLanguage', 'checkerSetCategory']
                        }
                    })
                }
            },
            async fetchList (isInit, selectParams = this.selectParams) {
                this.pageLoading = true
                const params = Object.assign(selectParams, { projectId: this.projectId })
                const res = await this.$store.dispatch('checkerset/list', params)
                this.pageLoading = false
                if (isInit) {
                    if (!Object.keys(this.queryOptStorage).length) {
                        this.checkersetList = res || []
                    }
                    this.isFetched = true
                    this.addVirtualScroll()
                } else {
                    this.checkersetList = res || []
                }
            },
            setLocalQueryOpt (query) {
                const checkerSetQueryObj = {}
                Object.keys(query).forEach(item => {
                    if (!['projectId', 'keyWord'].includes(item) && query[item].length) {
                        checkerSetQueryObj[item] = query[item]
                    }
                })
                localStorage.setItem('checkerSetQueryObj', JSON.stringify(checkerSetQueryObj))
            },
            handleSelect (value) {
                this.selectParams = Object.assign({}, this.selectParams, value)
            },
            handleKeyWordSearch (value) {
                this.selectParams = Object.assign({}, this.selectParams, { keyWord: value })
            },
            handleClear () {
                this.keyWord = ''
                this.selectParams = {}
                this.$refs.searchParams.handleClear()
            },
            handleCreate () {
                this.sliderVisiable = true
            },
            installMore () {
                this.dialogVisiable = true
            },
            refreshList () {
                this.initSearch(false)
                this.fetchList(false)
            },
            getCodeLang (codeLang) {
                const names = this.toolMeta.LANG.map(lang => {
                    if (lang.key & codeLang) {
                        return lang.name
                    }
                }).filter(name => name)
                return names.join('、')
            },
            async handleDelete () {
                const params = {
                    projectId: this.projectId,
                    checkerSetId: this.curHandleItem.checkerSetId
                }
                this.curHandleItem.projectId === this.projectId ? params.deleteCheckerSet = true : params.uninstallCheckerSet = true
                this.handleManage(params)
            },
            async setDeafault (checkerset) {
                const params = {
                    projectId: this.projectId,
                    checkerSetId: checkerset.checkerSetId,
                    defaultCheckerSet: !checkerset.defaultCheckerSet
                }
                this.handleManage(params)
            },
            async setScope (checkerset) {
                const params = {
                    projectId: this.projectId,
                    checkerSetId: checkerset.checkerSetId,
                    scope: checkerset.scope === 1 ? 2 : 1
                }
                this.handleManage(params)
            },
            handleManage (params) {
                this.$store.dispatch('checkerset/manage', params).then(res => {
                    if (res.code === '0') {
                        this.$bkMessage({ theme: 'success', message: '操作成功' })
                        this.initSearch(false)
                        this.fetchList(false)
                    }
                }).catch(e => {
                    console.error(e)
                })
            },
            handleCheckerset (checkerset, type, version) {
                if (type === 'edit') {
                    const link = {
                        name: 'checkerset-manage',
                        params: {
                            projectId: this.projectId,
                            checkersetId: checkerset.checkerSetId,
                            version: checkerset.version
                        }
                    }
                    this.$router.push(link)
                } else if (type === 'copy') {
                    const catagories = checkerset.catagories.map(category => category.enName)
                    const { checkerSetName, checkerSetId, codeLang, description } = checkerset
                    this.editObj = {
                        checkerSetName: `${checkerSetName}_copy`,
                        checkerSetId: `${checkerSetId}_copy`,
                        description,
                        codeLang,
                        catagories,
                        baseCheckerSetId: checkerSetId,
                        baseCheckerSetVersion: checkerset.version
                    }
                    this.isEdit = true
                    this.handleCreate()
                } else if (type === 'delete') {
                    this.curHandleItem = checkerset
                    this.delVisiable = true
                } else if (['setDefault', 'setPublish'].includes(type)) {
                    const that = this
                    let titleTxt, subTitleTxt
                    if (type === 'setDefault') {
                        titleTxt = !checkerset.defaultCheckerSet ? '设置规则集为默认' : '取消规则集为默认'
                        subTitleTxt = !checkerset.defaultCheckerSet
                            ? `设置【${checkerset.checkerSetName}】规则集为默认后，接入${this.getCodeLang(checkerset.codeLang)}语言任务时该规则集将会被自动选中。`
                            : `取消【${checkerset.checkerSetName}】规则集为默认后，接入${this.getCodeLang(checkerset.codeLang)}语言任务时该规则集将不会被自动选中。`
                    } else {
                        titleTxt = checkerset.scope === 2 ? '设为公开规则集' : '设为私密规则集'
                        subTitleTxt = checkerset.scope === 2
                            ? `将【${checkerset.checkerSetName}】规则集设为公开后，其他项目可以安装并使用该规则集。`
                            : `将【${checkerset.checkerSetName}】规则集设为私密后，其他项目不可以安装且使用该规则集。`
                    }
                    this.$bkInfo({
                        title: titleTxt,
                        subTitle: subTitleTxt,
                        maskClose: true,
                        confirmFn (name) {
                            type === 'setDefault' ? that.setDeafault(checkerset) : that.setScope(checkerset)
                        }
                    })
                } else if (type === 'version') {
                    const params = {
                        projectId: this.projectId,
                        checkerSetId: checkerset.checkerSetId,
                        versionSwitchTo: version
                    }
                    this.handleManage(params)
                }
            },
            updateActiveName (activeName) {
                this.activeName = activeName
            },
            addVirtualScroll () {
                setTimeout(() => {
                    if (this.checkersetList.length && this.$refs.domVirtualScroll) {
                        this.$refs.domVirtualScroll.scrollPageByIndex(0)
                        this.$refs.domVirtualScroll.setListData(this.checkersetList)
                        this.$refs.domVirtualScroll.getListData()
                    }
                })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .cc-checkersets {
        padding: 0 40px;
        display: flex;
        min-width: 1244px;
        .cc-side {
            background: #fff;
            height: 100%;
            display: block;
            border: 1px solid #dcdee5;
        }
        .cc-side {
            margin-right: 16px;
            padding: 0 16px;
            width: 240px;
            .cc-filter {
                height: 52px;
                line-height: 52px;
                border-bottom: 1px solid #dcdee5;
                .cc-filter-txt {
                    font-size: 14px;
                    color: #333333;
                }
                .cc-filter-select {
                    float: right;
                }
                .cc-filter-clear {
                    float: right;
                    position: relative;
                    padding-left: 10px;
                    cursor: pointer;
                    /* &::before {
                        content: "";
                        position: absolute;
                        width: 1px;
                        height: 18px;
                        background-color: #dcdee5;
                        left: 0;
                        top: 18px;
                    } */
                }
            }
            .cc-filter-content {
                overflow-y: scroll;
                max-height: calc(100% - 108px);
                margin: 0 -10px;
                &::-webkit-scrollbar {
                    width: 4px;
                }
                &::-webkit-scrollbar-thumb {
                    border-radius: 13px;
                    background-color: #d4dae3;
                }
            }
            .checker-search {
                padding: 8px 0;
            }
        }
        .cc-main {
            width: calc(100% - 250px);
            height: 100%;
            display: block;
        }
        .list-tool-bar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding-bottom: 12px;
            margin-bottom: 12px;
            border-bottom: 1px solid #dcdee5;
        }
        .bar-info {
            button:last-child {
                margin-left: 14px;
            }
        }
        .total-count {
            color: #737987;
            font-size: 12px;
        }
        .codecc-table-empty-text {
            text-align: center;
            padding-top: 200px;
        }
        .checkerset-content {
            height: calc(100% - 40px);
            overflow: auto;
            &::-webkit-scrollbar {
                width: 4px;
            }
            &::-webkit-scrollbar-thumb {
                border-radius: 13px;
                background-color: #d4dae3;
            }
        }
        >>> .bk-button-text .bk-icon {
            &.icon-plus-circle {
                top: 0;
            }
        }
    }
</style>
