<template>
    <article class="release-version">
        <main
            v-bkloading="{ isLoading }"
            class="version-main"
        >
            <component
                :is="`${$route.params.type}Version`"
                v-if="!isLoading"
                class="release-show"
                :detail="detail"
                :version-list="versionList"
                :has-promission="hasPromission"
                :pagination="pagination"
                :version-type="versionType"
                :loading="listLoading"
                :can-add-main-version="canAddMainVersion"
                @pageChanged="pageChanged"
                @pageLimitChanged="pageLimitChanged"
                @versionTypeChanged="versionTypeChanged"
            ></component>
        </main>
    </article>
</template>

<script>
    import atomVersion from '@/components/manage/release-manage/version/atom.vue'
    import imageVersion from '@/components/manage/release-manage/version/image.vue'
    import serviceVersion from '@/components/manage/release-manage/version/service.vue'
    import templateVersion from '@/components/manage/release-manage/version/template.vue'
    import devxVersion from '@/components/manage/release-manage/version/DEVX.vue'
    import { mapActions, mapGetters } from 'vuex'

    export default {
        components: {
            atomVersion,
            imageVersion,
            templateVersion,
            serviceVersion,
            devxVersion
        },

        props:{
            hasPromission: {
                type: Boolean,
                default: false
            },
        },

        data () {
            return {
                isLoading: true,
                listLoading: false,
                versionList: [],
                versionType: 'FORMAL',
                canAddMainVersion: true,
                pagination: {
                    current: 1,
                    count: 1,
                    limit: 10
                }
            }
        },

        computed: {
            ...mapGetters('store', {
                detail: 'getDetail'
            })
        },

        watch: {
            'detail.atomCode': {
                immediate: true,
                handler (atomCode) {
                    if (atomCode) this.checkCanAddMainVersion()
                }
            }
        },

        created () {
            this.getVersionList()
        },

        methods: {
            ...mapActions('store', [
                'requestVersionList',
                'requestImageVersionList',
                'requestTemplateReleasedList',
                'requestServiceVersionList',
                'getRelaseVersionList',
                'requestCanAddMainVersion'
            ]),
            pageLimitChanged (currentLimit, prevLimit) {
                if (currentLimit === this.pagination.limit) return

                this.pagination.current = 1
                this.pagination.limit = currentLimit
                this.getVersionList(false)
            },

            pageChanged (page) {
                if (page) this.pagination.current = page
                this.getVersionList(false)
            },

            versionTypeChanged (versionType) {
                this.versionType = versionType
                this.pagination.current = 1
                this.getVersionList(false)
            },

            checkCanAddMainVersion () {
                const atomCode = this.detail?.atomCode
                if (!atomCode) return
                this.requestCanAddMainVersion({ atomCode }).then((res) => {
                    this.canAddMainVersion = res?.canAddMainVersion ?? res
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                })
            },

            async getVersionList (showLoading = true) {
                try {
                    if (showLoading) {
                        this.isLoading = true
                    } else {
                        this.listLoading = true
                    }
                    const type = this.$route.params.type
                    const apiMethodMap = {
                        atom: this.requestVersionList,
                        image: this.requestImageVersionList,
                        template: this.requestTemplateReleasedList,
                        service: this.requestServiceVersionList,
                        devx: this.getRelaseVersionList
                    }

                    if (!Object.keys(apiMethodMap).includes(type) || typeof apiMethodMap[type] !== 'function') {
                        this.$bkMessage({ message: this.$t('store.typeError'), theme: 'error' })
                        return
                    }

                    // devx 类型使用 storeCode,其他类型使用 {type}Code
                    const codeParam = type === 'devx'
                        ? { storeCode: this.detail.storeCode }
                        : { [`${type}Code`]: this.detail[`${type}Code`] }
                    
                    const res = await apiMethodMap[type]({
                        ...codeParam,
                        page: this.pagination.current,
                        pageSize: this.pagination.limit,
                        ...(type === 'atom' && this.versionType !== 'ALL' ? { versionType: this.versionType } : {})
                    })
                    this.versionList = res.records || []
                    this.pagination.count = res.count
                    const lastestStatus = this.versionList?.[0]?.[`${type}Status`]
                    this.showEdit = ['AUDIT_REJECT', 'RELEASED', 'GROUNDING_SUSPENSION', 'UNDERCARRIAGED'].includes(lastestStatus)
                } catch (err) {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                } finally {
                    this.isLoading = false
                    this.listLoading = false
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .release-version {
        background: #fff;
        padding: 3.2vh;
        .version-main {
            height: 100%;
        }
        ::v-deep .show-version {
            .version-button {
                margin-bottom: 3.2vh;
            }
            .bk-table-pagination-wrapper {
                padding-bottom: 0;
            }
        }
    }
</style>
