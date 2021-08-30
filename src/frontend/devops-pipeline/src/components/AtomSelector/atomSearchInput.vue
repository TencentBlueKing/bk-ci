<template>
    <div class="atom-search-input">
        <bk-input
            ref="searchStr"
            :clearable="true"
            :placeholder="$t('editPage.searchTips')"
            right-icon="icon-search"
            v-model="searchKey"
            @input="handleClear"
            @enter="handleSearch" />
    </div>
</template>

<script>
    import { mapActions, mapGetters } from 'vuex'
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
                        setTimeout(() => {
                            this.$refs.searchStr.focus()
                        }, 0)
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
                'setStoreData',
                'fetchProjectAtoms',
                'fetchStoreAtoms'
            ]),
            handleClear (str) {
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
                this.fetchProjectAtoms({
                    projectCode: this.$route.params.projectId,
                    category: this.category,
                    recommendFlag: true,
                    os: this.os
                })
            },
            searchStoreAtom () {
                this.setStoreData({
                    page: 1,
                    keyword: this.searchKey
                })
                this.fetchStoreAtoms({
                    classifyId: this.innerActiveName === 'all' ? undefined : this.classifyId,
                    recommendFlag: true,
                    category: this.category,
                    os: this.os
                })
            }
        }
    }
</script>

<style lang="scss">
    .atom-search-input {
        padding: 20px 20px 0;
        .bk-form-input {
            background-color: #F0F1F5;
        }
    }
</style>
