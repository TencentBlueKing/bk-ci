<template>
    <portal to="atom-selector-popup">
        <transition name="selector-slide">
            <div v-if="showAtomSelectorPopup" class="atom-selector-popup">
                <header class="atom-selector-header">
                    <h3>{{ $t('editPage.chooseAtom') }}<i @click="freshAtomList(searchKey)" class="devops-icon icon-refresh atom-fresh" :class="fetchingAtomList ? &quot;spin-icon&quot; : &quot;&quot;" /></h3>
                    <bk-input class="atom-search-input" ref="searchStr" :clearable="true" :placeholder="$t('editPage.searchTips')" right-icon="icon-search" :value="searchKey" @input="handleClear" @enter="handleSearch"></bk-input>
                </header>
                <bk-tab v-bkloading="{ isLoading: fetchingAtomList }" class="atom-tab" size="small" ref="tab" :active.sync="classifyCode" type="unborder-card" v-if="!searchKey">
                    <bk-tab-panel
                        v-for="classify in classifyCodeList"
                        :key="classify"
                        :name="classify"
                        @scroll.native.passive="scrollLoadMore(classify, $event)"
                        :label="atomTree[classify].classifyName"
                        render-directive="if"
                        :class="[{ [getClassifyCls(classify)]: true }, 'tab-section']"
                    >
                        <atom-card v-for="atom in curTabList"
                            :key="atom.atomCode"
                            :disabled="atom.disabled"
                            :atom="atom"
                            :container="container"
                            :element-index="elementIndex"
                            :atom-code="atomCode"
                            :active-atom-code="activeAtomCode"
                            @close="close"
                            @click.native="activeAtom(atom.atomCode)"
                            :class="{
                                selected: atom.atomCode === atomCode,
                                [getAtomClass(atom.atomCode)]: true
                            }"
                        ></atom-card>
                        <unrecommend :show-unrecommend="curTabUncomList.length"
                            :unrecommend-arr="curTabUncomList"
                            @close="close"
                            @choose="activeAtom"
                            :atom-code="atomCode"
                            :active-atom-code="activeAtomCode"
                            :container="container"
                            :element-index="elementIndex"
                        ></unrecommend>
                        <div class="empty-atom-list" v-if="atomTree[classify].children.length === 0">
                            <empty-tips type="no-result"></empty-tips>
                        </div>
                    </bk-tab-panel>
                </bk-tab>
                <section v-else class="search-result">
                    <h3 v-if="!searchResultEmpty" class="search-title">{{ $t('newlist.installed') }}（{{installArr.length}}）</h3>
                    <atom-card v-for="atom in installArr"
                        :key="atom.atomCode"
                        :disabled="atom.disabled"
                        :atom="atom"
                        :container="container"
                        :element-index="elementIndex"
                        :atom-code="atomCode"
                        :active-atom-code="activeAtomCode"
                        @close="close"
                        @click.native="activeAtom(atom.atomCode)"
                        :class="{
                            selected: atom.atomCode === atomCode
                        }"
                    ></atom-card>

                    <h3 v-if="!searchResultEmpty" class="search-title gap-border">{{ $t('editPage.notInstall') }}（{{uninstallArr.length}}）</h3>
                    <atom-card v-for="atom in uninstallArr"
                        :key="atom.atomCode"
                        :disabled="atom.disabled"
                        :atom="atom"
                        :container="container"
                        :element-index="elementIndex"
                        :atom-code="atomCode"
                        :active-atom-code="activeAtomCode"
                        @close="close"
                        @click.native="activeAtom(atom.atomCode)"
                        :class="{
                            selected: atom.atomCode === atomCode
                        }"
                    ></atom-card>
                    <unrecommend :show-unrecommend="!searchResultEmpty"
                        :unrecommend-arr="unRecommendArr"
                        @close="close"
                        @choose="activeAtom"
                        :atom-code="atomCode"
                        :active-atom-code="activeAtomCode"
                        :container="container"
                        :element-index="elementIndex"
                    ></unrecommend>
                    <div class="empty-atom-list" v-if="searchResultEmpty">
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
    import unrecommend from './unRecommend'
    import EmptyTips from '../common/empty'

    const RD_STORE_CODE = 'rdStore'

    export default {
        name: 'atom-selector',
        components: {
            atomCard,
            unrecommend,
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
            elementIndex: Number,
            freshAtomList: Function
        },
        data () {
            return {
                searchKey: '',
                classifyCode: 'all',
                activeAtomCode: '',
                curTabList: [],
                curTabUncomList: []
            }
        },

        computed: {
            ...mapGetters('atom', [
                'getAtomTree',
                'getAtomCodeListByCategory',
                'classifyCodeListByCategory',
                'isTriggerContainer'
            ]),
            ...mapState('atom', [
                'fetchingAtomList',
                'showAtomSelectorPopup',
                'isPropertyPanelVisible',
                'atomClassifyMap',
                'atomCodeList',
                'storeAtomData',
                'atomClassifyCodeList',
                'atomMap',
                'atomModalMap'
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

            atomTree () {
                const { container, getAtomTree, getAtomFromStore, category, searchKey } = this
                const atomTree = getAtomTree(container.baseOS, category, searchKey)
                getAtomFromStore(atomTree)
                return atomTree
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
            },

            installArr () {
                const installed = this.atomTree.rdStore ? this.atomTree.rdStore.children : []
                return installed.filter((item) => (item.hasInstalled && item.recommendFlag !== false))
            },

            uninstallArr () {
                const storeList = this.atomTree.rdStore ? this.atomTree.rdStore.children : []
                return storeList.filter((item) => (!item.hasInstalled && item.recommendFlag !== false))
            },

            unRecommendArr () {
                const storeList = this.atomTree.rdStore ? this.atomTree.rdStore.children : []
                return storeList.filter(item => item.recommendFlag === false)
            },

            searchResultEmpty () {
                const all = this.installArr || []
                const rdStore = this.uninstallArr || []
                const unRecommend = this.unRecommendArr || []
                return all.length <= 0 && rdStore.length <= 0 && unRecommend.length <= 0
            }
        },

        watch: {
            showAtomSelectorPopup: {
                handler (visible) {
                    const { atomCode, firstClassify, atomMap } = this
                    if (visible) {
                        this.classifyCode = atomMap[atomCode] ? atomMap[atomCode].classifyCode : firstClassify
                        this.activeAtomCode = atomCode
                        this.fetchAtoms({ projectCode: this.$route.params.projectId })
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
                    const classifyObject = this.atomTree[val] || {}
                    const children = classifyObject.children || []
                    this.curTabList = children.filter(item => item.recommendFlag !== false)
                    this.curTabUncomList = children.filter(item => item.recommendFlag === false)
                },
                immediate: true
            },

            atomTree: {
                handler (val) {
                    const classifyObject = val[this.classifyCode] || {}
                    const children = classifyObject.children || []
                    this.curTabList = children.filter(item => item.recommendFlag !== false)
                    this.curTabUncomList = children.filter(item => item.recommendFlag === false)
                },
                immediate: true
            }
        },

        created () {
            const pageIndex = this.storeAtomData.page || 1
            if (pageIndex <= 1) this.addStoreAtom()
        },

        methods: {
            ...mapActions('atom', [
                'toggleAtomSelectorPopup',
                'addStoreAtom',
                'clearStoreAtom',
                'setStoreSearch',
                'fetchAtoms'
            ]),

            getAtomFromStore (atomTree) {
                const storeList = (this.storeAtomData.data || []).filter((item) => {
                    let res = true
                    if (this.category === 'TRIGGER') res = item.classifyCode === 'trigger'
                    else res = item.classifyCode !== 'trigger'
                    return res
                })
                const allAtom = atomTree.all || {}
                const allInstalledAtom = allAtom.children || []
                const codes = []
                allInstalledAtom.forEach((atom) => {
                    const code = atom.atomCode
                    codes.push(code)
                    const index = storeList.findIndex(x => x.code === code)
                    if (index < 0) {
                        atom.code = code
                        atom.rdType = atom.atomType
                        storeList.push(atom)
                    }
                })

                const baseOs = this.container.baseOS
                const rdStoreList = storeList.map((store) => {
                    store.atomCode = store.code
                    store.atomType = store.rdType

                    const os = store.os || []
                    const isInOs = (!os.length && store.buildLessRunFlag) || (!os.length && !baseOs) || os.findIndex((x) => (x === baseOs)) > -1

                    store.disabled = !isInOs
                    store.notShowSelect = true
                    store.isInOs = isInOs

                    const code = store.code
                    const index = codes.findIndex((x) => (x === code))
                    const hasInstalled = index > -1
                    if (hasInstalled) codes.splice(index, 1)
                    store.hasInstalled = hasInstalled

                    if (isInOs) {
                        store.notShowSelect = !hasInstalled && !store.publicFlag

                        if (!store.flag) store.tips = this.$t('editPage.noPermToInstall')
                        else store.tips = ''
                    }

                    return store
                })
                atomTree.rdStore = { children: rdStoreList, classifyCode: RD_STORE_CODE, classifyName: this.$t('store'), level: 0 }
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
                this.searching = true
                this.clearStoreAtom()
                this.setStoreSearch(this.searchKey)
                this.addStoreAtom()
            },

            handleClear (str) {
                if (str === '') {
                    this.clearSearch()
                    this.activeAtomCode = this.atomCode
                }
            },

            clearSearch () {
                const input = this.$refs.searchStr || {}
                input.curValue = ''
                this.searching = false
                this.searchKey = ''
                this.clearStoreAtom()
                this.setStoreSearch()
                this.addStoreAtom()
            },
            close () {
                this.toggleAtomSelectorPopup(false)
                this.clearSearch()
            },

            scrollLoadMore (classify, $event) {
                if (classify !== RD_STORE_CODE) return

                const target = event.target
                const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
                if (bottomDis <= 400) this.addStoreAtom()
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
        z-index: 2500;
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
                    color: $fontLigtherColor
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
                    color: $fontLigtherColor;
                }
            }
            .atom-logo {
                width: 50px;
                height: 50px;
                font-size: 50px;
                line-height: 50px;
                margin-right: 15px;
                color: $fontLigtherColor;
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
                .atom-name {
                    display: flex;
                    align-items: center;
                    .allow-os-list {
                        margin-left: 10px;
                        .os-tag {
                            color: $fontLigtherColor;
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
                width: 60px;
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
