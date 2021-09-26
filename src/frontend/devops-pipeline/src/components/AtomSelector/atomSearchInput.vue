<template>
    <div class="atom-search-input">
        <bk-input
            ref="searchStr"
            :clearable="true"
            :placeholder="$t('editPage.searchTips')"
            right-icon="icon-search"
            v-model="searchKey"
            @input="handleInput"
            @enter="handleSearch" />
        <i v-if="!fetchingAtomList" class="devops-icon icon-refresh atom-fresh" @click="handleFreshAtoms"></i>
        <i v-else class="devops-icon icon-refresh atom-fresh spin-icon" @click="handleFreshAtoms"></i>
    </div>
</template>

<script>
    import { mapActions, mapGetters, mapState } from 'vuex'
    export default {
        props: {
            activeTab: {
                type: String,
                default: ''
            },
            tabName: {
                type: String,
                default: ''
            },
            container: {
                type: Object,
                default: () => ({})
            }
        },
        data () {
            return {
                searchKey: ''
            }
        },
        computed: {
            ...mapGetters('atom', [
                'isTriggerContainer',
                'getAtomClassifyMap',
                'innerActiveName'
            ]),
            ...mapState('atom', [
                'fetchingAtomList'
            ]),
            category () {
                return this.isTriggerContainer(this.container) ? 'TRIGGER' : 'TASK'
            },
            os () {
                return this.container && this.container.baseOS
            },
            classifyId () {
                return this.getAtomClassifyMap[this.innerActiveName].id
            }
        },
        watch: {
            activeTab: {
                handler (value) {
                    if (value && value === this.tabName) {
                        this.searchKey = ''
                    }
                },
                immediate: true
            },
            innerActiveName: {
                handler () {
                    if (this.tabName === 'storeAtom') {
                        this.searchKey = ''
                    }
                },
                immediate: true
            }
        },
        methods: {
            ...mapActions('atom', [
                'setProjectData',
                'setUnRecommendProjectData',
                'setStoreData',
                'setUnRecommendStoreData',
                'fetchProjectAtoms',
                'fetchStoreAtoms',
                'updateProjectAtoms',
                'updateStoreAtoms'
            ]),
            
            /**
             * 刷新插件列表
             */
            handleFreshAtoms () {
                if (this.tabName === 'projectAtom') {
                    this.updateProjectAtoms({
                        atoms: {},
                        recommend: true
                    })
                    this.updateProjectAtoms({
                        atoms: {},
                        recommend: false
                    })
                    this.searchProjectAtom()
                    document.querySelectorAll('.recommend-atom-list')[0].scrollTo(0, 0)
                } else {
                    this.updateStoreAtoms({
                        atoms: {},
                        recommend: true
                    })
                    this.updateStoreAtoms({
                        atoms: {},
                        recommend: false
                    })
                    this.searchStoreAtom()
                    document.querySelectorAll('.recommend-atom-list')[1].scrollTo(0, 0)
                }
            },

            handleInput (str) {
                if (str === '' && this.activeTab === 'projectAtom') {
                    this.searchProjectAtom()
                } else if (str === '' && this.activeTab === 'storeAtom') {
                    this.searchStoreAtom()
                }
            },
            handleSearch () {
                if (this.activeTab === 'projectAtom') {
                    this.searchProjectAtom()
                } else if (this.activeTab === 'storeAtom') {
                    this.searchStoreAtom()
                }
            },
            searchProjectAtom () {
                this.setProjectData({
                    page: 1,
                    keyword: this.searchKey
                })
                this.setUnRecommendProjectData({
                    page: 1,
                    keyword: this.searchKey
                })
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
            },
            searchStoreAtom () {
                this.setStoreData({
                    page: 1,
                    keyword: this.searchKey
                })
                this.setUnRecommendStoreData({
                    page: 1,
                    keyword: this.searchKey
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
                    classifyId: this.innerActiveName === 'all' ? undefined : this.classifyId,
                    recommendFlag: false,
                    category: this.category,
                    os: this.os,
                    queryProjectAtomFlag: false
                })
            }
        }
    }
</script>

<style lang="scss">
    .atom-search-input {
        display: flex;
        padding: 20px 10px 0 20px;
        .atom-fresh {
            display: inline-block;
            font-size: 14px;
            padding: 11px;
            color: #3c96ff;
            cursor: pointer;
        }
    }
</style>
