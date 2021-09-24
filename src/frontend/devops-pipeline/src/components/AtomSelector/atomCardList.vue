<template>
    <section class="atom-item-main" v-bkloading="{ isLoading: fetchingAtomList }">
        <div v-if="showInnerTab" class="classify-tab">
            <span
                v-for="(classify, index) in classifyCodeList"
                :key="index"
                :class="{ 'inner-tab-item': true, 'active': innerActiveName === classify }"
                @click="handleToggleClassify(classify)">
                {{ atomClassifyMap[classify].classifyName }}
            </span>
        </div>
        <div ref="atomListDom" class="recommend-atom-list">
            <!-- 插件卡片 -->
            <atom-card
                v-for="(recommendAtom, key, index) in curRecommendAtomMap"
                :key="index"
                :class="{ 'recommend-atom-item': true, 'active': atomCode === recommendAtom.atomCode, 'not-installFlag': !recommendAtom.installFlag && !isProjectAtom }"
                @close="close"
                :atom="recommendAtom"
                :atom-index="index"
                :container="container"
                :element-index="elementIndex"
                :delete-reasons="deleteReasons"
                :is-project-atom="isProjectAtom"
                :atom-code="atomCode"
                @update-atoms="handelUpdateAtom" />
            <div v-if="isRecommendMoreLoading" class="loading-more" slot="append"><i class="devops-icon icon-circle-2-1 spin-icon"></i><span>{{ $t('loadingTips') }}</span></div>
            <!-- 全部加载完毕 -->
            <template v-if="RecommendAtomLength && !fetchingAtomList">
                <p v-if="isProjectPageOver && tabName === 'projectAtom'" class="page-over">{{ $t('editPage.loadedAllAtom') }}</p>
                <p v-if="isStorePageOver && tabName === 'storeAtom'" class="page-over">{{ $t('editPage.loadedAllAtom') }}</p>
            </template>
            <!-- 空数据 -->
            <template v-if="!RecommendAtomLength && !fetchingAtomList">
                <empty v-if="!projectAtomKeyWord && !storeAtomKeyWord" size="small" />
                <empty v-else size="small" type="no-atoms" />
            </template>
        </div>
        <div v-if="category !== 'TRIGGER'" :class="{ 'fixed-tool': true, 'active': isToolActive }" @click="handleToggleShowUnRecommend">
            {{ $t('editPage.fixedTips') }} ({{ unRecommendAtomLength }})
            <span class="devops-icon icon-angle-right"></span>
        </div>
        <!-- 不适用插件 -->
        <div :class="{ 'unRecommend-atom-list': true, 'show-unRecommend': isToolActive }">
            <atom-card
                class="unRecommend-atom-item"
                v-for="(unRecommendAtom, index) in curUnRecommendAtomMap"
                :key="index + tabName"
                :is-project-atom="isProjectAtom"
                :atom="unRecommendAtom"
                :is-recommend="false" />
            <template v-if="isUnRecommendMoreLoading" class="loading-more" slot="append"><i class="devops-icon icon-circle-2-1 spin-icon"></i><span>{{ $t('loadingTips') }}</span></template>
            <template v-if="unRecommendAtomLength">
                <p v-if="isUnRecommendProjectPageOver && tabName === 'projectAtom'" class="page-over">{{ $t('editPage.loadedAllAtom') }}</p>
                <p v-if="isUnRecommendStorePageOver && tabName === 'storeAtom'" class="page-over">{{ $t('editPage.loadedAllAtom') }}</p>
            </template>
            <template v-else>
                <empty v-if="!projectAtomKeyWord && !storeAtomKeyWord" size="small" />
                <empty v-else size="small" type="no-atoms" />
            </template>
        </div>
    </section>
</template>

<script>
    import { mapGetters, mapActions, mapState } from 'vuex'
    import Empty from '@/components/common/empty'
    import atomCard from './atomCard'

    export default {
        components: {
            atomCard,
            Empty
        },
        props: {
            // 选中的Tab(项目插件/研发商店)
            activeTab: {
                type: String,
                default: ''
            },
            // 当前展示组件的Tab名字
            tabName: {
                type: String,
                default: ''
            },
            container: {
                type: Object,
                default: () => ({})
            },
            elementIndex: Number,
            atomCode: {
                type: String
            },
            deleteReasons: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                isToolActive: false,
                curRecommendAtomMap: {},
                curUnRecommendAtomMap: {},
                isRecommendThrottled: false,
                isUnRecommendThrottled: false
            }
        },
        computed: {
            ...mapGetters('atom', [
                'classifyCodeListByCategory',
                'isTriggerContainer',
                'getProjectRecommendAtomMap',
                'getProjectUnRecommendAtomMap',
                'getStoreRecommendAtomMap',
                'getStoreUnRecommendAtomMap',
                'getAtomClassifyMap',
                'innerActiveName',
                'projectAtomKeyWord',
                'storeAtomKeyWord'
            ]),
            ...mapState('atom', [
                'fetchingAtomList',
                'isRecommendMoreLoading',
                'isUnRecommendMoreLoading',
                'isProjectPageOver',
                'isUnRecommendProjectPageOver',
                'isStorePageOver',
                'isUnRecommendStorePageOver',
                'showAtomSelectorPopup',
                'atomClassifyMap',
                'atomClassifyCodeList',
                'atomMap',
                'atomModalMap'
            ]),
            showInnerTab () {
                return this.activeTab === 'storeAtom'
            },
            category () {
                return this.isTriggerContainer(this.container) ? 'TRIGGER' : 'TASK'
            },
            classifyCodeList () {
                const atomClassifyCodeList = this.classifyCodeListByCategory(this.category)
                if (this.category !== 'TRIGGER') {
                    atomClassifyCodeList.unshift('all')
                }
                return atomClassifyCodeList
            },
            RecommendAtomLength () {
                let result
                if (this.curRecommendAtomMap) {
                    result = Object.keys(this.curRecommendAtomMap).length
                }
                return result
            },
            unRecommendAtomLength () {
                let result
                if (this.curUnRecommendAtomMap) {
                    result = Object.keys(this.curUnRecommendAtomMap).length
                }
                return result
            },
            classifyId () {
                return this.getAtomClassifyMap[this.innerActiveName].id
            },
            os () {
                return this.container && this.container.baseOS
            },
            isProjectAtom () {
                return this.activeTab === 'projectAtom'
            }
        },
        watch: {
            activeTab: {
                handler () {
                    this.initData()
                }
            },
            getProjectRecommendAtomMap: {
                handler (atoms) {
                    if (this.tabName === 'projectAtom') {
                        this.curRecommendAtomMap = atoms
                    }
                },
                immediate: true,
                deep: true
            },
            getProjectUnRecommendAtomMap: {
                handler (atoms) {
                    if (this.tabName === 'projectAtom') {
                        this.curUnRecommendAtomMap = atoms
                    }
                },
                immediate: true,
                deep: true
            },
            getStoreRecommendAtomMap: {
                handler (atoms) {
                    if (this.tabName === 'storeAtom') {
                        this.curRecommendAtomMap = atoms
                    }
                },
                immediate: true,
                deep: true
            },
            getStoreUnRecommendAtomMap: {
                handler (atoms) {
                    if (this.tabName === 'storeAtom') {
                        this.curUnRecommendAtomMap = atoms
                    }
                },
                immediate: true,
                deep: true
            }
        },
        created () {
            if (this.activeTab === 'projectAtom') {
                this.setProjectData({
                    page: 1
                })
                this.setProjectPageOver(false)
                this.initData()
            }
        },
        methods: {
            ...mapActions('atom', [
                'toggleAtomSelectorPopup',
                'setProjectData',
                'setUnRecommendProjectData',
                'setStoreData',
                'setUnRecommendStoreData',
                'setProjectPageOver',
                'fetchProjectAtoms',
                'fetchStoreAtoms',
                'fetchClassify',
                'setInnerActiveName',
                'updateProjectAtoms',
                'updateStoreAtoms'
            ]),

            initData () {
                this.$nextTick(() => {
                    this.$refs.atomListDom.scrollTo(0, 0)
                })
                this.setInnerActiveName('all')
                this.curRecommendAtomMap = {}
                this.curUnRecommendAtomMap = {}
                if (this.activeTab && this.activeTab === this.tabName) {
                    if (this.tabName === 'projectAtom') {
                        this.setProjectData({
                            page: 1,
                            keyword: ''
                        })
                        this.setUnRecommendProjectData({
                            page: 1,
                            keyword: ''
                        })
                        this.setProjectPageOver(false)
                        this.fetchProjectAtoms({
                            projectCode: this.$route.params.projectId,
                            category: this.category,
                            recommendFlag: true,
                            os: this.os,
                            queryProjectAtomFlag: true
                        })
                        this.fetchProjectAtoms({
                            projectCode: this.$route.params.projectId,
                            category: this.category,
                            recommendFlag: false,
                            os: this.os,
                            queryProjectAtomFlag: true
                        })
                        setTimeout(() => {
                            const recommendDom = document.getElementsByClassName('recommend-atom-list')[0]
                            recommendDom.addEventListener('scroll', this.recommendScrollLoading)

                            const unRecommendDomR = document.getElementsByClassName('unRecommend-atom-list')[0]
                            unRecommendDomR.addEventListener('scroll', this.unRecommendScrollLoading)
                        }, 200)
                    }
                    if (this.tabName === 'storeAtom') {
                        this.fetchClassify()
                        this.setStoreData({
                            page: 1,
                            keyword: ''
                        })
                        this.setUnRecommendStoreData({
                            page: 1,
                            keyword: ''
                        })
                        this.fetchStoreAtoms({
                            projectCode: this.$route.params.projectId,
                            classifyId: this.innerActiveName === 'all' ? undefined : this.classifyId,
                            recommendFlag: true,
                            category: this.category,
                            os: this.os,
                            queryProjectAtomFlag: false
                        })
                        this.fetchStoreAtoms({
                            projectCode: this.$route.params.projectId,
                            classifyId: undefined,
                            recommendFlag: false,
                            category: this.category,
                            os: this.os,
                            queryProjectAtomFlag: false
                        })
                        setTimeout(() => {
                            const recommendDom = document.getElementsByClassName('recommend-atom-list')[1]
                            recommendDom.addEventListener('scroll', this.recommendScrollLoading)

                            const unRecommendDomR = document.getElementsByClassName('unRecommend-atom-list')[1]
                            unRecommendDomR.addEventListener('scroll', this.unRecommendScrollLoading)
                        }, 200)
                    }
                }
            },
            
            handleToggleClassify (classify) {
                if (this.innerActiveName === classify) return
                this.setInnerActiveName(classify)
                this.setStoreData({
                    keyword: ''
                })
                this.fetchStoreAtoms({
                    projectCode: this.$route.params.projectId,
                    classifyId: this.innerActiveName === 'all' ? undefined : this.classifyId,
                    category: this.category,
                    recommendFlag: true,
                    os: this.os,
                    queryProjectAtomFlag: false
                })
                this.$nextTick(() => {
                    this.$refs.atomListDom.scrollTo(0, 0)
                })
            },

            // 移除插件更新数据
            handelUpdateAtom (payload) {
                const { isRecommend, atomCode } = payload
                const atoms = isRecommend ? this.curRecommendAtomMap : this.curUnRecommendAtomMap
                for (const key in atoms) {
                    if (key === atomCode && this.isProjectAtom) this.$delete(atoms, atomCode)
                }

                this.updateProjectAtoms({
                    atoms: atoms,
                    recommend: isRecommend
                })
            },
            
            // 适用插件滚动加载
            recommendScrollLoading () {
                if (this.activeTab === 'projectAtom') {
                    console.log(this.isProjectPageOver)
                    if (!this.isProjectPageOver && !this.isRecommendThrottled && !this.isRecommendMoreLoading) {
                        this.isRecommendThrottled = true
                        this.recommendTimer = setTimeout(() => {
                            this.isRecommendThrottled = false
                            if (this.activeTab === 'projectAtom') {
                                // 项目适用插件列表滚动加载
                                const recommendDom = document.querySelectorAll('.recommend-atom-list')[0]
                                const scrollHeight = recommendDom.scrollHeight
                                const innerHeight = recommendDom.offsetHeight
                                const scrollY = recommendDom.scrollTop
                                if (scrollHeight - innerHeight - scrollY < 100) {
                                    this.fetchProjectAtoms(
                                        {
                                            projectCode: this.$route.params.projectId,
                                            category: this.category,
                                            recommendFlag: true,
                                            os: this.os,
                                            queryProjectAtomFlag: true
                                        }
                                    )
                                    clearTimeout(this.recommendTimer)
                                }
                            }
                        }, 1000)
                    }
                } else {
                    if (!this.isStorePageOver && !this.isRecommendThrottled && !this.isRecommendMoreLoading) {
                        this.isRecommendThrottled = true
                        this.unRecommendTimer = setTimeout(() => {
                            this.isRecommendThrottled = false
                            if (this.activeTab === 'storeAtom') {
                                const recommendDom = document.querySelectorAll('.recommend-atom-list')[1]
                                const scrollHeight = recommendDom.scrollHeight
                                const innerHeight = recommendDom.offsetHeight
                                const scrollY = recommendDom.scrollTop
                                if (scrollHeight - innerHeight - scrollY < 100) {
                                    this.fetchStoreAtoms({
                                        projectCode: this.$route.params.projectId,
                                        classifyId: this.innerActiveName === 'all' ? undefined : this.classifyId,
                                        recommendFlag: true,
                                        category: this.category,
                                        os: this.os,
                                        queryProjectAtomFlag: false
                                    })
                                    clearTimeout(this.unRecommendTimer)
                                }
                            }
                        }, 1000)
                    }
                }
            },
            // 不适用插件滚动加载
            unRecommendScrollLoading () {
                if (this.activeTab === 'projectAtom') {
                    if (!this.isUnRecommendProjectPageOver && !this.isUnRecommendThrottled && !this.isUnRecommendMoreLoading) {
                        this.isUnRecommendThrottled = true
                        this.unRecommendTimer = setTimeout(() => {
                            this.isUnRecommendThrottled = false
                            if (this.activeTab === 'projectAtom') {
                                // 项目不适用插件列表滚动加载
                                const unRecommendDom = document.querySelectorAll('.unRecommend-atom-list')[0]
                                const unRecommendDomScrollHeight = unRecommendDom.scrollHeight
                                const unRecommendDomInnerHeight = unRecommendDom.offsetHeight
                                const unRecommendDomScrollY = unRecommendDom.scrollTop
                                if (unRecommendDomScrollHeight - unRecommendDomInnerHeight - unRecommendDomScrollY < 100) {
                                    this.fetchProjectAtoms(
                                        {
                                            projectCode: this.$route.params.projectId,
                                            category: this.category,
                                            recommendFlag: false,
                                            os: this.os,
                                            queryProjectAtomFlag: true
                                        }
                                    )
                                    clearTimeout(this.timer)
                                }
                            }
                        }, 1000)
                        const unRecommendDom = document.querySelectorAll('.unRecommend-atom-list')[0]
                        const atomMain = document.getElementsByClassName('atom-item-main')[0]
                        unRecommendDom.style.height = atomMain.getBoundingClientRect().height + 'px'
                    }
                } else {
                    if (!this.isUnRecommendStorePageOver && !this.isUnRecommendThrottled && !this.isUnRecommendMoreLoading) {
                        this.isUnRecommendThrottled = true
                        this.unRecommendTimer = setTimeout(() => {
                            this.isUnRecommendThrottled = false
                            if (this.activeTab === 'storeAtom') {
                                const unRecommendDom = document.querySelectorAll('.unRecommend-atom-list')[1]
                                const unRecommendDomScrollHeight = unRecommendDom.scrollHeight
                                const unRecommendDomInnerHeight = unRecommendDom.offsetHeight
                                const unRecommendDomScrollY = unRecommendDom.scrollTop
                                if (unRecommendDomScrollHeight - unRecommendDomInnerHeight - unRecommendDomScrollY < 100) {
                                    this.fetchStoreAtoms({
                                        projectCode: this.$route.params.projectId,
                                        classifyId: undefined,
                                        recommendFlag: false,
                                        category: this.category,
                                        os: this.os,
                                        queryProjectAtomFlag: false
                                    })
                                    clearTimeout(this.unRecommendTimer)
                                }
                            }
                        }, 1000)
                    }
                    const unRecommendDom = document.querySelectorAll('.unRecommend-atom-list')[1]
                    const atomMain = document.getElementsByClassName('atom-item-main')[1]
                    unRecommendDom.style.height = atomMain.getBoundingClientRect().height + 'px'
                }
            },
            close () {
                this.toggleAtomSelectorPopup(false)
            },
            handleToggleShowUnRecommend () {
                this.isToolActive = !this.isToolActive
                const unProjectRecommendDom = document.querySelectorAll('.unRecommend-atom-list')[0]
                const unStoreRecommendDom = document.querySelectorAll('.unRecommend-atom-list')[1]
                if (this.RecommendAtomLength < 3) {
                    if (this.isToolActive) {
                        const projectAtomMain = document.querySelectorAll('.atom-item-main')[0]
                        unProjectRecommendDom.style.height = projectAtomMain.getBoundingClientRect().height + 'px'

                        const storeAtomMain = document.querySelectorAll('.atom-item-main')[1]
                        unStoreRecommendDom.style.height = storeAtomMain.getBoundingClientRect().height + 'px'
                    } else {
                        unProjectRecommendDom.style.height = ''
                        unStoreRecommendDom.style.height = ''
                    }
                } else {
                    if (unProjectRecommendDom.style.height) {
                        unProjectRecommendDom.style.height = ''
                    }
                    if (unStoreRecommendDom.style.height) {
                        unStoreRecommendDom.style.height = ''
                    }
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '../../scss/conf';
   .atom-item-main {
        display: flex;
        flex-direction: column;
        margin-top: 10px;
        padding-bottom: 20px;
        overflow: hidden;
        .classify-tab {
            padding: 8px 20px 15px;
            .inner-tab-item {
                display: inline-block;
                height: 24px;
                padding: 0 15px;
                line-height: 22px;
                color: #63656E;
                border: 1px solid #F0F1F5;
                text-align: center;
                font-size: 12px;
                background: #F0F1F5;
                border-radius: 2px;
                margin-right: 4px;
                cursor: pointer;
                &.active {
                    background: #E1ECFF;
                    border: 1px solid #3A84FF;
                    color: #3A84FF;
                }
            }
        }
        .recommend-atom-list {
            height: 100%;
            overflow: scroll;
        }
        .unRecommend-atom-list {
            height: 0;
            overflow: scroll;
            transition: all .3s;
        }
            ::-webkit-scrollbar {
                width: 5px;
            }
        .show-unRecommend {
            height: 290px;
            transition: all .3s;
        }
        .loading-more {
            display: flex;
            height: 36px;
            font-size: 12px;
            justify-content: center;
            align-items: center;
            .devops-icon {
                margin-right: 8px;
            }
        }
        .page-over {
            text-align: center;
            font-size: 12px;
        }
        .page-empty {
            text-align: center;
            font-size: 12px;
            margin-top: 50%;
        }
        .fixed-tool {
            position: relative;
            left: 200px;
            width: 208px;
            height: 24px;
            background: #F0F1F5;
            border-radius: 12px;
            font-size: 12px;
            line-height: 24px;
            text-align: center;
            color: #63656E;
            cursor: pointer;
            transition: all .5s;
            margin-bottom: 10px;
            &.active {
                span {
                    transform: rotate(-90deg);
                    transition: all .5s;
                }
            }
            span {
                display: inline-block;
                margin-left: 5px;
                transform: rotate(90deg);
                transition: all .5s;
            }
        }
        .recommend-atom-item,
        .unRecommend-atom-item {
            display: flex;
            padding: 10px 20px;
            max-height: 80px;
            width: 100%;
            overflow: hidden;
            cursor: pointer;
            transition: all .2s ease-out;
            &:hover {
                max-height: 150px;
                background-color: #F5F6FA;
                transition-timing-function: ease-in;
                .atom-info-content .allow-os-list,
                .atom-info-content .atom-update-time {
                    opacity: 1;
                    transition: all .2s ease-out;
                }
                .atom-info-content .atom-name .atom-link .jump-icon {
                    opacity: 1;
                }
                .atom-info-content .remove-atom,
                .atom-info-content .un-remove,
                .atom-info-content .install-atom,
                .atom-info-content .atom-label {
                    opacity: 1;
                }
            }
            &.active {
                background-color: rgba(58, 132, 255, 0.1);
                &:hover {
                    max-height: 150px;
                    background-color: rgba($color: #3A84FF, $alpha: 0.1);
                    transition-timing-function: ease-in;
                    .atom-update-time {
                        opacity: 1;
                        transition: all .3s ease-out;
                    }
                }
            }
            &.not-installFlag {
                cursor: not-allowed;
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
                    width: 50px;
                    border-radius: 4px;
                }
            }
            .atom-info-content {
                position: relative;
                width: 494px;
                font-weight: bold;
                font-size: 14px;
                padding-right: 10px;
                .remove-atom,
                .un-remove,
                .install-atom {
                    display: inline-block;
                    width: 25px;
                    height: 25px;
                    position: absolute;
                    left: 465px;
                    top: -10px;
                    background: #C4C6CC;
                    color: #fff;
                    border-radius: 0 0 100% 100%/100%;
                    opacity: 0;
                    z-index: 99;
                    .remove-icon {
                        position: relative;
                        right: -5px;
                        top: 4px;
                    }
                }
                .un-remove {
                    cursor: not-allowed;
                }
                .remove-atom,
                .install-atom {
                    &:hover {
                        background-color: #3a84ff;
                    }
                }
                .install-disabled {
                    cursor: not-allowed;
                    &:hover {
                        background-color: #C4C6CC;
                    }
                }
                .un-remove {
                    background-color: #C4C6CC;
                    &:hover {
                        background-color: #EA3636;
                    }
                }
                .atom-name {
                    display: flex;
                    align-items: center;
                    color: #000;
                    margin-bottom: 6px;
                    .atom-link {
                        color: #000;
                        .jump-icon {
                            opacity: 0;
                        }
                    }
                    .fire-num {
                        margin-left: 20px;
                        font-size: 12px;
                        color: #979BA5;
                    }
                    .atom-rate {
                        display: inline-block;
                        position: relative;
                        top: 2px;
                        left: 20px;
                    }
                }
                .desc {
                    display: inline-block;
                    font-size: 12px;
                    color: #63656E;
                    display: -webkit-box;
                    overflow: hidden;
                    word-wrap: break-word;
                    -webkit-line-clamp: 2;
                    -webkit-box-orient: vertical;
                }
                .desc-height {
                    margin-bottom: 8px;
                }
                .atom-label {
                    display: inline-block;
                    opacity: 0;
                    padding: 10px 0;
                    span {
                        display: inline-block;
                        height: 20px;
                        line-height: 20px;
                        text-align: center;
                        padding: 0 10px;
                        color: #63656E;
                        font-size: 12px;
                        margin-right: 4px;
                        margin-bottom: 3px;
                        background-color: #fff;
                    }
                }
                .allow-os-list {
                    position: relative;
                    right: 65px;
                    top: 4px;
                    opacity: 0;
                }
                .atom-update-time {
                    display: flex;
                    flex-direction: row-reverse;
                    position: absolute;
                    right: 10px;
                    bottom: -2px;
                    padding-top: 2px;
                    font-size: 12px;
                    color: #C5C7D1;
                    opacity: 0;
                }
                .atom-active {
                    position: absolute;
                    right: -20px;
                    bottom: -8px;
                    border-width: 13px;
                    border-style: solid;
                    border-top-left-radius: 3px;
                    border-color: transparent #3A84FF #3A84FF transparent;
                    .devops-icon {
                        position: absolute;
                        top: -1px;
                        left: -1px;
                        color: #fff;
                        border-radius: 10px;
                        font-size: 12px;
                        background-color: #3A84FF;
                    }
                }
            }
        }
        .unRecommend-atom-item {
            cursor: not-allowed;
        }
   }
    .unInstall-tips,
    .install-tips {
        z-index: 99999 !important;
    }
</style>
