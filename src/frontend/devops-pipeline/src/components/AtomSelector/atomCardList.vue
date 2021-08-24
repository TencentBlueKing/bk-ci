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
            <atom-card
                v-for="(recommendAtom, key, index) in curRecommendAtomMap"
                :key="index"
                :class="{ 'recommend-atom-item': true, 'active': atomCode === recommendAtom.atomCode }"
                @close="close"
                :atom="recommendAtom"
                :container="container"
                :element-index="elementIndex"
                :delete-reasons="deleteReasons"
                :is-project-atom="isProjectAtom"
                :atom-code="atomCode" />
            <div v-if="isMoreLoading" class="loading-more" slot="append"><i class="devops-icon icon-circle-2-1 spin-icon"></i><span>{{ $t('loadingTips') }}</span></div>
            <template v-if="RecommendAtomLength">
                <p v-if="isProjectPageOver && tabName === 'projectAtom'" class="page-over">{{ $t('editPage.loadedAllAtom') }}</p>
                <p v-if="isStorePageOver && tabName === 'storeAtom'" class="page-over">{{ $t('editPage.loadedAllAtom') }}</p>
            </template>
            <template v-else>
                <p class="page-empty">{{ $t('empty') }}</p>
            </template>
        </div>
        <div v-if="category !== 'TRIGGER'" :class="{ 'fixed-tool': true, 'active': isToolActive }" @click="isToolActive = !isToolActive">
            {{ $t('editPage.fixedTips') }} ({{ unRecommendAtomLength }})
            <span class="devops-icon icon-angle-right"></span>
        </div>
        <div v-if="unRecommendAtomLength" :class="{ 'unRecommend-atom-list': true, 'show-unRecommend': isToolActive }">
            <atom-card
                class="unRecommend-atom-item"
                v-for="(unRecommendAtom, index) in curUnRecommendAtomMap"
                :key="index"
                :atom="unRecommendAtom"
                :recommend="false" />
        </div>
    </section>
</template>

<script>
    import { mapGetters, mapActions, mapState } from 'vuex'
    import atomCard from './atomCard'

    export default {
        components: {
            atomCard
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
                // innerActiveName: 'all',
                curRecommendAtomMap: {},
                curUnRecommendAtomMap: {},
                isThrottled: false
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
                'innerActiveName'
            ]),
            ...mapState('atom', [
                'fetchingAtomList',
                'isMoreLoading',
                'isProjectPageOver',
                'isStorePageOver',
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
                'setProjectPageOver',
                'fetchProjectAtoms',
                'fetchStoreAtoms',
                'fetchClassify',
                'setInnerActiveName',
                'setStoreData'
            ]),

            initData () {
                this.$nextTick(() => {
                    this.$refs.atomListDom.scrollTo(0, 0)
                })
                if (this.activeTab && this.activeTab === this.tabName) {
                    if (this.tabName === 'projectAtom') {
                        this.setProjectData({
                            page: 1
                        })
                        this.setProjectPageOver(false)
                        this.fetchProjectAtoms({
                            projectCode: this.$route.params.projectId,
                            category: this.category,
                            recommendFlag: true,
                            os: this.os
                        })
                        this.fetchProjectAtoms({
                            projectCode: this.$route.params.projectId,
                            category: this.category,
                            recommendFlag: false,
                            os: this.os
                        })
                        // 延迟300ms获取dom元素
                        setTimeout(() => {
                            const dom = document.getElementsByClassName('recommend-atom-list')[0]
                            dom.addEventListener('scroll', this.scrollLoading)
                        }, 300)
                    }
                    if (this.tabName === 'storeAtom') {
                        this.fetchClassify()
                        this.fetchStoreAtoms({
                            classifyId: this.innerActiveName === 'all' ? undefined : this.classifyId,
                            recommendFlag: true,
                            category: this.category,
                            os: this.os
                        })
                        this.fetchStoreAtoms({
                            classifyId: this.innerActiveName === 'all' ? undefined : this.classifyId,
                            recommendFlag: false,
                            category: this.category,
                            os: this.os
                        })
                        setTimeout(() => {
                            const dom = document.getElementsByClassName('recommend-atom-list')[1]
                            dom.addEventListener('scroll', this.scrollLoading)
                        }, 300)
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
                    classifyId: this.innerActiveName === 'all' ? undefined : this.classifyId,
                    category: this.category,
                    recommendFlag: true,
                    os: this.os
                })
                this.$nextTick(() => {
                    this.$refs.atomListDom.scrollTo(0, 0)
                })
            },
            
            // 滚动加载
            scrollLoading () {
                if (this.activeTab === 'projectAtom') {
                    if (!this.isProjectPageOver && !this.isThrottled && !this.isMoreLoading) {
                        this.isThrottled = true
                        this.timer = setTimeout(() => {
                            this.isThrottled = false
                            if (this.activeTab === 'projectAtom') {
                                const dom = document.querySelectorAll('.recommend-atom-list')[0]
                                const scrollHeight = dom.scrollHeight
                                const innerHeight = dom.offsetHeight
                                const scrollY = dom.scrollTop
                                if (scrollHeight - innerHeight - scrollY < 60) {
                                    this.fetchProjectAtoms(
                                        {
                                            projectCode: this.$route.params.projectId,
                                            category: this.category,
                                            recommendFlag: true,
                                            os: this.os
                                        }
                                    )
                                    clearTimeout(this.timer)
                                }
                            }
                        }, 1000)
                    }
                } else {
                    if (!this.isStorePageOver && !this.isThrottled && !this.isMoreLoading) {
                        this.isThrottled = true
                        this.timer = setTimeout(() => {
                            this.isThrottled = false
                            if (this.activeTab === 'storeAtom') {
                                const dom = document.querySelectorAll('.recommend-atom-list')[1]
                                const scrollHeight = dom.scrollHeight
                                const innerHeight = dom.offsetHeight
                                const scrollY = dom.scrollTop
                                console.log(scrollY)
                                if (scrollHeight - innerHeight - scrollY < 60) {
                                    this.fetchStoreAtoms({
                                        classifyId: this.innerActiveName === 'all' ? undefined : this.classifyId,
                                        recommendFlag: true,
                                        category: this.category,
                                        os: this.os
                                    })
                                    clearTimeout(this.timer)
                                }
                            }
                        }, 1000)
                    }
                }
            },
            close () {
                this.toggleAtomSelectorPopup(false)
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
                width: 0;
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
            height: 75px;
            width: 100%;
            overflow: hidden;
            cursor: pointer;
            transition: height .3s;
            &:hover {
                height: 150px;
                background-color: #F5F6FA;
                .atom-info-content .atom-update-time {
                    opacity: 1;
                }
                .atom-info-content .atom-name .atom-link .jump-icon {
                    opacity: 1;
                }
                .atom-info-content .remove-atom,
                .atom-info-content .install-atom {
                    opacity: 1;
                }
            }
            &.active {
                background-color: rgba(58, 132, 255, 0.1);
                &:hover {
                    height: 150px;
                    background-color: rgba($color: #3A84FF, $alpha: 0.1);
                    .atom-update-time {
                        opacity: 1;
                    }
                }
            }
            .atom-logo {
                background-color: skyblue;
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
                    .remove-icon {
                        position: relative;
                        right: -5px;
                        top: 3px;
                    }
                }
                .remove-atom {
                    cursor: not-allowed;
                }
                .install-atom {
                    background-color: #3A84FF;
                }
                .un-remove {
                    background-color: #EA3636;
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
                    height: 32px;
                    font-size: 12px;
                    color: #63656E;
                    display: -webkit-box;
                    overflow: hidden;
                    word-wrap: break-word;
                    -webkit-line-clamp: 2;
                    -webkit-box-orient: vertical;
                    margin-bottom: 10px;
                }
                .atom-label {
                    display: inline-block;
                    height: 46px;
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
                    top: 15px;
                }
                .atom-update-time {
                    display: flex;
                    flex-direction: row-reverse;
                    left: 340px;
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
   }
    .unInstall-tips,
    .install-tips {
        z-index: 99999 !important;
    }
</style>
