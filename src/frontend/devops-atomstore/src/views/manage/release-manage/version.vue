<template>
    <article class="release-version">
        <main v-bkloading="{ isLoading }" class="version-main">
            <component :is="`${$route.params.type}Version`"
                v-if="!isLoading"
                class="release-show"
                :detail="detail"
                :version-list="versionList"
                :pagination="pagination"
                @pageChanged="pageChanged"
                @pageLimitChanged="pageLimitChanged"
            ></component>
        </main>
    </article>
</template>

<script>
    import { mapGetters } from 'vuex'
    import atomVersion from '@/components/manage/release-manage/version/atom.vue'
    import imageVersion from '@/components/manage/release-manage/version/image.vue'
    import serviceVersion from '@/components/manage/release-manage/version/service.vue'

    export default {
        components: {
            atomVersion,
            imageVersion,
            serviceVersion
        },

        data () {
            return {
                isLoading: true,
                versionList: [],
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

        created () {
            this.getVersionList()
        },

        methods: {
            pageLimitChanged (currentLimit, prevLimit) {
                if (currentLimit === this.pagination.limit) return

                this.pagination.current = 1
                this.pagination.limit = currentLimit
                this.getVersionList()
            },

            pageChanged (page) {
                if (page) this.pagination.current = page
                this.getVersionList()
            },

            getVersionList () {
                const methodMap = {
                    atom: this.getAtomVersion,
                    image: this.getImageVersion,
                    service: this.getServiceVersion
                }
                const type = this.$route.params.type
                const currentMethod = methodMap[type]
                this.isLoading = true
                currentMethod().catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            getAtomVersion () {
                return this.$store.dispatch('store/requestVersionList', {
                    atomCode: this.detail.atomCode,
                    page: this.pagination.current,
                    pageSize: this.pagination.limit
                }).then((res) => {
                    this.versionList = res.records || []
                    this.pagination.count = res.count
                    const lastestVersion = this.versionList[0] || {}
                    const lastestStatus = lastestVersion.atomStatus
                    this.showEdit = ['AUDIT_REJECT', 'RELEASED', 'GROUNDING_SUSPENSION', 'UNDERCARRIAGED'].includes(lastestStatus)
                })
            },

            getImageVersion () {
                const postData = {
                    imageCode: this.detail.imageCode,
                    page: this.pagination.current,
                    pageSize: this.pagination.limit
                }
                return this.$store.dispatch('store/requestImageVersionList', postData).then((res) => {
                    this.versionList = res.records || []
                    this.pagination.count = res.count
                    const lastestVersion = this.versionList[0] || {}
                    const lastestStatus = lastestVersion.imageStatus
                    this.showEdit = ['AUDIT_REJECT', 'RELEASED', 'GROUNDING_SUSPENSION', 'UNDERCARRIAGED'].includes(lastestStatus)
                })
            },

            getServiceVersion () {
                const postData = {
                    serviceCode: this.detail.serviceCode,
                    page: this.pagination.current,
                    pageSize: this.pagination.limit
                }
                return this.$store.dispatch('store/requestServiceVersionList', postData).then((res) => {
                    this.versionList = res.records || []
                    this.pagination.count = res.count
                    const lastestVersion = this.versionList[0] || {}
                    const lastestStatus = lastestVersion.serviceStatus
                    this.showEdit = ['AUDIT_REJECT', 'RELEASED', 'GROUNDING_SUSPENSION', 'UNDERCARRIAGED'].includes(lastestStatus)
                })
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
