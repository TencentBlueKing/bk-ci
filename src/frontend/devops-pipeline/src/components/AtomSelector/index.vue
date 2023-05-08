<template>
    <portal to="atom-selector-popup">
        <transition name="selector-slide">
            <div v-if="showAtomSelectorPopup" class="atom-selector-popup">
                <header class="atom-selector-header">
                    <h3>{{ $t('editPage.chooseAtom') }}<i @click="freshAtomList(searchKey)" class="devops-icon icon-refresh atom-fresh" :class="fetchingAtomList ? &quot;spin-icon&quot; : &quot;&quot;" /></h3>
                    <bk-input class="atom-search-input" ref="searchStr" :clearable="true" :placeholder="$t('editPage.searchTips')" right-icon="icon-search" :value="searchKey" @input="handleClear" @enter="handleSearch"></bk-input>
                </header>
                <bk-tab v-if="!searchKey" class="atom-tab" size="small" ref="tab" :active.sync="classifyCode" type="unborder-card" v-bkloading="{ isLoading: fetchingAtomList }">
                    <bk-tab-panel
                        ref="atomListDom"
                        v-for="classify in classifyCodeList"
                        :key="classify"
                        :name="classify"
                        @scroll.native.passive="scrollLoadMore(classify, $event)"
                        :label="atomClassifyMap[classify].classifyName"
                        render-directive="if"
                        :class="[{ [getClassifyCls(classify)]: true }, 'tab-section']"
                    >
                        <atom-card v-for="(atom) in curTabList"
                            :key="atom.atomCode"
                            :atom="atom"
                            :container="container"
                            :element-index="elementIndex"
                            :atom-code="atomCode"
                            :active-atom-code="activeAtomCode"
                            @close="close"
                            @click="activeAtom(atom.atomCode)"
                            :class="{
                                selected: atom.atomCode === atomCode,
                                [getAtomClass(atom.atomCode)]: true
                            }"
                        ></atom-card>
                        <div class="empty-atom-list" v-if="curTabList.length <= 0 && !fetchingAtomList">
                            <empty-tips type="no-result"></empty-tips>
                        </div>
                    </bk-tab-panel>
                </bk-tab>
                <section v-else class="search-result" ref="searchResult" v-bkloading="{ isLoading: fetchingAtomList }">
                    <h3 v-if="installArr.length" class="search-title">{{ $t('newlist.installed') }}（{{installArr.length}}）</h3>
                    <atom-card v-for="atom in installArr"
                        :key="atom.atomCode"
                        :disabled="atom.disabled"
                        :atom="atom"
                        :container="container"
                        :element-index="elementIndex"
                        :atom-code="atomCode"
                        :active-atom-code="activeAtomCode"
                        @close="close"
                        @click="activeAtom(atom.atomCode)"
                        :class="{
                            selected: atom.atomCode === atomCode
                        }"
                    ></atom-card>

                    <h3 v-if="uninstallArr.length" class="search-title gap-border">{{ $t('editPage.notInstall') }}（{{uninstallArr.length}}）</h3>
                    <atom-card v-for="atom in uninstallArr"
                        :key="atom.atomCode"
                        :disabled="atom.disabled"
                        :atom="atom"
                        :container="container"
                        :element-index="elementIndex"
                        :atom-code="atomCode"
                        :active-atom-code="activeAtomCode"
                        @installAtomSuccess="installAtomSuccess"
                        @close="close"
                        @click="activeAtom(atom.atomCode)"
                        :class="{
                            selected: atom.atomCode === atomCode
                        }"
                    ></atom-card>
                    <div class="empty-atom-list" v-if="curTabList.length <= 0 && !fetchingAtomList">
                        <empty-tips type="no-result"></empty-tips>
                    </div>
                </section>
            </div>
        </transition>
    </portal>
</template>

<script>
    import { mapGetters, mapActions, mapState } from 'vuex'
    import atomCard from './atomCard'
    import EmptyTips from '../common/empty'

    const RD_STORE_CODE = 'rdStore'

    export default {
        name: 'atom-selector',
        components: {
            atomCard,
            EmptyTips
        },
        props: {
            container: {
                type: Object,
                default: () => ({})
            },
            element: {
                type: Object,
                default: () => ({})
            },
            elementIndex: Number
        },
        data () {
            return {
                searchKey: '',
                classifyCode: 'all',
                activeAtomCode: '',
                curTabList: [],
                installArr: [],
                uninstallArr: [],
                isThrottled: false
            }
        },

        computed: {
            ...mapGetters('atom', [
                'classifyCodeListByCategory',
                'isTriggerContainer'
            ]),
            ...mapState('atom', [
                'fetchingAtomList',
                'showAtomSelectorPopup',
                'atomClassifyMap',
                'atomClassifyCodeList',
                'atomMap',
                'atomList',
                'fetchingAtomMoreLoading',
                'isAtomPageOver',
                'isCommendAtomPageOver'
            ]),

            atomCode () {
                if (this.element) {
                    const isThird = this.element.atomCode && this.element['@type'] !== this.element.atomCode
                    if (isThird) {
                        return this.element.atomCode
                    } else {
                        return this.element['@type']
                    }
                }
                return null
            },

            category () {
                return this.isTriggerContainer(this.container) ? 'TRIGGER' : 'TASK'
            },

            baseOS () {
                return this.container.baseOS
            },

            projectCode () {
                return this.$route.params.projectId
            },

            classifyId () {
                return this.atomClassifyMap[this.classifyCode] && this.atomClassifyMap[this.classifyCode].id
            },

            classifyCodeList () {
                const atomClassifyCodeList = this.classifyCodeListByCategory(this.category)
                if (this.category !== 'TRIGGER') {
                    atomClassifyCodeList.unshift('all')
                    atomClassifyCodeList.push(RD_STORE_CODE)
                }
                return atomClassifyCodeList
            },

            firstClassify () {
                return Array.isArray(this.classifyCodeList) ? this.classifyCodeList[0] : 'all'
            }
        },

        watch: {
            showAtomSelectorPopup: {
                handler (visible) {
                    const { atomCode, firstClassify, atomMap } = this
                    if (visible) {
                        this.classifyCode = atomMap[atomCode] ? atomMap[atomCode].classifyCode : firstClassify
                        this.activeAtomCode = atomCode
                        this.fetchClassify()
                        this.fetchAtomList()
                        setTimeout(() => {
                            this.$refs.searchStr.focus()
                        }, 0)
                    } else {
                        this.clearSearch()
                    }
                },
                immediate: true
            },

            classifyCode: {
                handler (val) {
                    this.freshRequestAtomData()
                    this.fetchAtomList()
                }
            },

            atomList: {
                handler (val) {
                    this.curTabList = val
                    if (this.searchKey) {
                        this.uninstallArr = val.filter(item => !item.defaultFlag && !item.installed)
                        this.installArr = val.filter(item => item.defaultFlag || item.installed)
                    }
                },
                immediate: true
            },

            fetchingAtomList: {
                handler () {
                    // 如果获取完可用插件, 就请求一页不可用插件数据
                    if (this.isCommendAtomPageOver) {
                        this.fetchAtomList()
                    }
                },
                immediate: true
            }
        },

        methods: {
            ...mapActions('atom', [
                'toggleAtomSelectorPopup',
                'setRequestAtomData',
                'fetchAtoms',
                'fetchClassify',
                'setAtomPageOver',
                'clearAtomData'
            ]),

            /**
             * 获取插件列表数据
             */
            fetchAtomList () {
                if (!this.fetchingAtomMoreLoading && !this.isThrottled && !this.isAtomPageOver) {
                    this.isThrottled = true
                    this.timer = setTimeout(async () => {
                        this.isThrottled = false
                        const queryProjectAtomFlag = this.classifyCode !== 'rdStore' // 是否查询项目插件标识
        
                        let jobType // job类型 => 触发器插件无需传jobType
                        if (this.category === 'TRIGGER') {
                            jobType = undefined
                        } else {
                            jobType = ['WINDOWS', 'MACOS', 'LINUX'].includes(this.baseOS) ? 'AGENT' : 'AGENT_LESS'
                        }
                        
                        await this.fetchAtoms({
                            projectCode: this.projectCode,
                            category: this.category,
                            classifyId: this.classifyId,
                            os: this.baseOS,
                            jobType: jobType,
                            searchKey: this.searchKey,
                            queryProjectAtomFlag
                        })
                    }, 100)
                }
            },

            getClassifyCls (classifyCode) {
                return `classify-${classifyCode}-cls`
            },
            getAtomClass (atomCode) {
                return `atom-${atomCode}-cls`
            },

            activeAtom (code) {
                this.activeAtomCode = code
            },
            handleSearch (value) {
                this.searchKey = value.trim()
                this.freshRequestAtomData()
                this.fetchAtomList()
            },

            handleClear (str) {
                if (str === '') {
                    this.clearSearch()
                    this.activeAtomCode = this.atomCode
                }
            },
            freshRequestAtomData () {
                this.setAtomPageOver()
                this.setRequestAtomData({
                    page: 1,
                    pageSize: 50,
                    recommendFlag: true,
                    keyword: this.searchKey
                })
                this.clearAtomData()
            },
            clearSearch () {
                const input = this.$refs.searchStr || {}
                input.curValue = ''
                this.searchKey = ''
                this.setAtomPageOver()
                this.freshRequestAtomData()
                this.fetchAtomList()
            },
            close () {
                this.toggleAtomSelectorPopup(false)
                this.clearSearch()
            },

            freshAtomList (searchKey) {
                if (this.fetchingAtomList) return
                if (this.searchKey) {
                    this.$refs.searchResult.scrollTo(0, 0)
                } else {
                    const curDom = this.$refs.atomListDom.find(item => item.name === this.classifyCode)
                    !this.searchKey && curDom.$el.scrollTo(0, 0)
                }
                this.freshRequestAtomData()
                this.fetchAtomList()
            },

            scrollLoadMore (classify, $event) {
                const target = event.target
                const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
                if (bottomDis <= 600) this.fetchAtomList()
            },

            installAtomSuccess (atom) {
                const curAtom = this.curTabList.find(item => item.atomCode === atom.atomCode)
                this.installArr.push(curAtom)
                this.uninstallArr = this.uninstallArr.filter(item => item.atomCode !== atom.atomCode)
            }
        }
    }
</script>

<style lang="scss">
    @import '../../scss/conf';
    .atom-selector-popup {
        right: 660px;
        position: absolute;
        width: 600px;
        height: calc(100% - 20px);
        background: white;
        z-index: 10000;
        border: 1px solid $borderColor;
        border-radius: 5px;
        top: 0;
        margin: 10px 0;
        &:before {
            content: '';
            display: block;
            position: absolute;
            width: 10px;
            height: 10px;
            background: white;
            border: 1px solid $borderColor;
            border-left-color: white;
            border-bottom-color: white;
            transform: rotate(45deg);
            right: -6px;
            top: 136px;
        }
        .not-recommend {
            text-decoration: line-through;
        }
        .atom-selector-header {
            position: relative;
            height: 36px;
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            align-items: center;
            margin: 24px 21px 10px 21px;
            .atom-fresh {
                cursor: pointer;
                display: inline-block;
                font-size: 14px;
                padding: 4px;
                margin-left: 3px;
                color: $primaryColor;
                &.spin-icon {
                    color: $fontLighterColor
                }
            }
            > h3 {
                font-size: 14px;
                margin: 0;
            }
            .atom-search-input {
                width: 200px;
            }
        }
        .search-result {
            height: calc(100% - 70px);
            overflow-y: auto;
            margin: 0 11px 0 21px;
            padding-right: 10px;
            padding-bottom: 10px;
            .search-title {
                line-height:16px;
                font-weight:bold;
                font-size: 12px;
                margin: 9px 0;
                &.gap-border {
                    padding-top: 10px;
                    border-top: 1px solid #ebf0f5;
                }
            }
        }
        .atom-tab {
            height: calc(100% - 70px);
            border: 0;
            font-size: 12px;
            color: $fontWeightColor;
            font-weight: 500;
            padding: 0 10px 10px 10px;
            overflow: hidden;
            div.bk-tab-section {
                height: calc(100% - 42px);
                overflow-y: hiden;
                padding: 0;
                .bk-tab-content {
                    height: 100%;
                    overflow: auto;
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
                                &.active {
                                    font-weight: bold;
                                }
                            }
                        }
                    }
                }
            }
        }
        .atom-item {
            padding: 20px 20px 9px 20px;
            height: 100px;
            width: 100%;
            &:hover,
            &.active {
                background: #E9F4FF;
                .atom-info-content .atom-from,
                .atom-operate > .atom-link {
                    opacity: 1;
                }
            }

            &:not(.active):hover {
                background: #FAFBFD;
            }
            &.disabled {
                .atom-info-content,
                .atom-info-content .desc {
                    color: $fontLighterColor;
                }
            }
            .atom-logo {
                width: 50px;
                height: 50px;
                font-size: 50px;
                line-height: 50px;
                margin-right: 15px;
                color: $fontLighterColor;
                .devops-icon {
                    fill: currentColor
                }
                > img {
                    width: 100%;
                    border-radius: 4px;
                }
            }
            .atom-info-content {
                flex: 1;
                color: #4A4A4A;
                font-weight: bold;
                font-size: 14px;
                padding-right: 10px;
                display: flex;
                flex-direction: column;
                justify-content: space-between;
                width: calc(100% - 143px);
                max-width: 345px;
                .atom-name {
                    display: flex;
                    align-items: center;
                    .allow-os-list {
                        margin-left: 10px;
                        .os-tag {
                            color: $fontLighterColor;
                            font-size: 14px;
                            padding-right: 4px;
                            vertical-align: top;
                        }
                    }
                }
                .desc {
                    font-size: 12px;
                    color: $fontWeightColor;
                    display: -webkit-box;
                    overflow: hidden;
                    -webkit-line-clamp: 2;
                    -webkit-box-orient: vertical;
                }
                .atom-from {
                    font-size: 12px;
                    color: #C5C7D1;
                    opacity: 0;
                }
            }
            .atom-operate {
                width: 88px;
                display: flex;
                flex-direction: column;
                justify-content: space-between;
                position: relative;

                button.select-atom-btn[disabled] {
                    cursor: not-allowed !important;
                    background-color: #fff;
                    color: #c4c6cc;
                }
                .select-atom-btn.disabled {
                    opacity: 0;
                }
                .select-atom-btn:hover {
                    background-color: $primaryColor;
                    color: white;
                }
                .atom-link {
                    font-size: 12px;
                    opacity: 0;
                    color: $primaryColor;
                    position: absolute;
                    bottom: 0;
                }
            }
        }
    }

    .empty-atom-list {
        display: flex;
        height: 100%;
        align-items: center;
        justify-content: center;
    }

    .atom-item-main {
        display: flex;
        height: 100%;
    }

    .selector-slide-enter-active, .selector-slide-leave-active {
        transition: transform .2s linear, opacity .2s cubic-bezier(1, -0.05, .94, .17);
    }

    .selector-slide-enter {
        -webkit-transform: translate3d(600px, 0, 0);
        transform: translateX(600px);
        opacity: 0;
    }

    .selector-slide-leave-active {
        display: none;
    }
</style>
