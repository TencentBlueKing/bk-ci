<template>
    <portal to="atom-selector-popup">
        <div :class="showAtomSelectorPopup ? &quot;visible&quot; : &quot;hide&quot;" class="atom-selector-popup">
            <header class="atom-selector-header">
                <h3>请选择一个插件<i @click="freshAtomList(searchKey)" class="bk-icon icon-refresh atom-fresh" :class="fetchingAtomList ? &quot;spin-icon&quot; : &quot;&quot;" /></h3>
                <bk-input class="atom-search-input" v-bk-focus="1" clearable placeholder="快速搜索" right-icon="bk-icon icon-search" :value="searchKey" @input="handleSearch"></bk-input>
            </header>
            <bk-tab v-bkloading="{ isLoading: fetchingAtomList }" class="atom-tab" size="small" ref="tab" :active.sync="classifyCode" type="unborder-card">
                <bk-tab-panel
                    v-for="classify in classifyCodeList"
                    :key="classify"
                    :name="classify"
                    @scroll.native.passive="scrollLoadMore(classify, $event)"
                    :label="atomTree[classify].classifyName"
                    :class="[{ [getClassifyCls(classify)]: true }, 'tab-section']"
                >
                    <div v-for="atom in getAtomList(classify)" :disabled="atom.disabled" :key="atom.atomCode" @click="activeAtom(atom)" :class="{
                        &quot;atom-item&quot;: true,
                        &quot;active&quot;: atom.atomCode === activeAtomCode,
                        &quot;selected&quot;: atom.atomCode === atomCode,
                        [getAtomClass(atom.atomCode)]: true
                    }">
                        <div class="atom-item-main">
                            <div class="atom-logo">
                                <img v-if="atom.logoUrl" :src="atom.logoUrl" />
                                <logo v-else class="bk-icon" :name="getIconByCode(atom.atomCode)" size="50" />
                            </div>
                            <div class="atom-info-content">
                                <p class="atom-name">
                                    {{atom.name}}
                                    <span class="allow-os-list">
                                        <template v-if="atom.os && atom.os.length > 0">
                                            <template v-for="os in atom.os">
                                                <bk-popover :content="`${jobConst[os]}编译环境下可用`" :key="os">
                                                    <i :class="`os-tag bk-icon icon-${os.toLowerCase()}`"></i>
                                                </bk-popover>
                                            </template>
                                        </template>
                                        <bk-popover v-else :content="`无编译环境下可用`">
                                            <i :class="`os-tag bk-icon icon-none`"></i>
                                        </bk-popover>
                                    </span>
                                </p>
                                <p class="desc">{{atom.summary || '暂无描述'}}</p>
                                <p class="atom-from">{{`由${atom.publisher}提供`}}</p>
                            </div>
                            <div class="atom-operate">
                                <bk-button class="select-atom-btn"
                                    :class="atom.disabled ? &quot;disabled&quot; : &quot;&quot;"
                                    size="small"
                                    @click="handleUpdateAtomType(atom.atomCode)"
                                    :disabled="atom.disabled || atom.atomCode === atomCode"
                                    v-if="!atom.notShowSelect"
                                >{{atom.atomCode === atomCode ? '已选' : '选择'}}
                                </bk-button>
                                <bk-button class="select-atom-btn"
                                    size="small"
                                    @click="handleInstallStoreAtom(atom.atomCode)"
                                    :class="{ 'disabled': atom.disabled }"
                                    :disabled="!atom.flag"
                                    :title="atom.tips"
                                    v-else
                                >安装
                                </bk-button>
                                <a v-if="atom.docsLink" target="_blank" class="atom-link" :href="atom.docsLink">了解更多</a>
                            </div>
                        </div>
                    </div>
                    <div class="empty-atom-list" v-if="atomTree[classify].children.length === 0">
                        <img src="../../images/no_result.png" />
                    </div>
                </bk-tab-panel>
            </bk-tab>
        </div>
    </portal>
</template>

<script>
    import { mapGetters, mapActions, mapState } from 'vuex'
    import { jobConst, RD_STORE_CODE } from '@/utils/pipelineConst'
    import Logo from '@/components/Logo'

    export default {
        name: 'atom-selector',
        components: {
            Logo
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
                searching: false,
                classifyCode: 'all',
                activeAtomCode: ''
            }
        },

        computed: {
            ...mapGetters('atom', [
                'getAtomTree',
                'getAtomCodeListByCategory',
                'getAtomModal',
                'getDefaultVersion',
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

            jobConst () {
                return jobConst
            },

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
                const { searchKey, container, getAtomTree, getAtomFromStore, category } = this
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
            }
        },

        watch: {
            showAtomSelectorPopup (visible) {
                const { atomCode, getClassifyCls, getAtomClass, firstClassify, atomMap } = this
                if (visible) {
                    this.classifyCode = atomMap[atomCode] ? atomMap[atomCode].classifyCode : firstClassify
                    this.$refs.tab.calcActiveName = this.classifyCode // hack local data

                    if (atomCode) {
                        this.activeAtomCode = atomCode

                        this.$nextTick(() => {
                            const el = document.querySelector(`.${getClassifyCls(this.classifyCode)} .${getAtomClass(atomCode)}`)
                            const codeEle = document.querySelector(`.${getClassifyCls(this.classifyCode)}`)
                            const scrollBox = codeEle ? codeEle.parentElement : null
                            if (scrollBox && el) {
                                scrollBox.scrollTop = el.offsetTop - scrollBox.offsetTop
                            }
                        })
                    }
                }
            }
        },

        created () {
            const pageIndex = this.storeAtomData.page || 1
            if (pageIndex <= 1) this.addStoreAtom()
        },

        methods: {
            ...mapActions('atom', [
                'toggleAtomSelectorPopup',
                'updateAtomType',
                'fetchAtomModal',
                'addStoreAtom',
                'clearStoreAtom',
                'setStoreSearch'
            ]),

            getAtomFromStore (atomTree) {
                const allAtom = atomTree.all || {}
                const allInstalledAtom = allAtom.children || []
                const codes = allInstalledAtom.map(atom => atom.atomCode)

                const storeList = this.storeAtomData.data || []
                const baseOs = this.container.baseOS
                const rdStoreList = storeList.map((store) => {
                    store.atomCode = store.code
                    store.atomType = store.rdType

                    const os = store.os || []
                    const isInOs = (!os.length && store.buildLessRunFlag) || (!os.length && !baseOs) || os.findIndex((x) => (x === baseOs)) > -1

                    store.disabled = !isInOs || store.classifyCode === 'trigger'
                    store.notShowSelect = true

                    if (isInOs) {
                        const code = store.code
                        const index = codes.findIndex((x) => (x === code))
                        const hasInstalled = index > -1
                        if (hasInstalled) codes.splice(index, 1)

                        store.notShowSelect = !hasInstalled && !store.publicFlag

                        if (!store.flag) store.tips = '无权限安装该插件'
                        else store.tips = ''
                    }

                    return store
                })
                const sortStoreList = rdStoreList.sort(atom => atom.flag ? 1 : -1).sort(atom => atom.notShowSelect ? 1 : -1).sort(atom => atom.disabled ? 1 : -1)
                atomTree.rdStore = { children: sortStoreList, classifyCode: RD_STORE_CODE, classifyName: '研发商店', level: 0 }
            },

            getClassifyCls (classifyCode) {
                return `classify-${classifyCode}-cls`
            },
            getAtomClass (atomCode) {
                return `atom-${atomCode}-cls`
            },
            getIconByCode (atomCode) {
                const svg = document.getElementById(atomCode)
                return svg ? atomCode : 'placeholder'
            },
            activeAtom (atom) {
                this.activeAtomCode = atom.atomCode
            },
            getAtomList (classify) {
                const classifyObject = this.atomTree[classify]
                return classifyObject && Array.isArray(classifyObject.children) ? classifyObject.children : []
            },
            handleSearch (value) {
                this.searchKey = value.trim()
                if (!this.searching) {
                    const rdStoreTabIndex = this.classifyCodeList.indexOf(RD_STORE_CODE)
                    this.classifyCode = rdStoreTabIndex > -1 ? RD_STORE_CODE : this.firstClassify
                    this.searching = true
                    // this.classifyCode(rdStoreTabIndex > -1 ? rdStoreTabIndex : 0)
                }

                clearTimeout(this.handleSearch.id)
                this.handleSearch.id = setTimeout(() => {
                    this.clearStoreAtom()
                    this.setStoreSearch(this.searchKey)
                    this.addStoreAtom()
                }, 400)
            },
            clearSearch () {
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
            handleUpdateAtomType (atomCode) {
                const { elementIndex, container, updateAtomType, getAtomModal, fetchAtomModal, getDefaultVersion } = this
                const version = getDefaultVersion(atomCode)
                const atomModal = getAtomModal({
                    atomCode,
                    version
                })

                const fn = atomModal ? updateAtomType : fetchAtomModal
                fn({
                    projectCode: this.$route.params.projectId,
                    container,
                    version,
                    atomCode,
                    atomIndex: elementIndex
                })
                this.close()
            },

            handleInstallStoreAtom (atomCode) {
                window.open(`${WEB_URL_PIRFIX}/store/${atomCode}/install/atom?projectCode=${this.$route.params.projectId}#MARKET`)
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
        transition: all .2s ease-in-out;
        opacity: 1;
        right: 660px;
        position: absolute;
        width: 600px;
        height: calc(100% - 20px);
        background: white;
        z-index: 9999;
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
        &.hide {
            opacity: 0;
            transform: skewY(-5deg) translateX(300px) scaleY(0);
            transform-origin: 100% 20%;
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
                font-size: 12px;
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
                    .empty-atom-list {
                        display: flex;
                        height: 100%;
                        align-items: center;
                        justify-content: center;
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
                                &.active {
                                    font-weight: bold;
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
                    align-items: center;
                    background: #E9F4FF;
                    .atom-info-content .atom-from,
                    .atom-operate > .atom-link {
                        opacity: 1;
                    }
                }

                &:not(.active):hover {
                    background: #FAFBFD;
                }
                &[disabled] {
                    .atom-info-content,
                    .atom-info-content .desc {
                        color: $fontLigtherColor;
                    }
                }
                .atom-item-main {
                    display: flex;
                    height: 100%;
                }
                .atom-logo {
                    width: 50px;
                    height: 50px;
                    font-size: 50px;
                    line-height: 50px;
                    margin-right: 15px;
                    color: $fontLigtherColor;
                    .bk-icon {
                        fill: currentColor
                    }
                    > img {
                        width: 100%;
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
                    .select-atom-btn[disabled] {
                        cursor: not-allowed !important;
                    }
                    .select-atom-btn.disabled {
                        opacity: 0;
                    }
                    .select-atom-btn:hover {
                        background-color: $primaryColor;
                        color: white;
                    }
                    .atom-link {
                        opacity: 0;
                        color: $primaryColor;
                    }
                }
            }
        }
    }
</style>
