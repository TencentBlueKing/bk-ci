<template>
    <article class="manage-detail" v-bkloading="{ isLoading }">
        <header class="manage-detail-header">
            <router-link :to="showEdit ? { name: 'edit' } : ''"
                :class="{ disable: !showEdit }"
                :title="!showEdit && $t('store.只有处于审核驳回、已发布、上架中止和已下架的状态才允许修改基本信息')"
            >{{ $t('store.编辑') }}</router-link>
        </header>
        <component :is="`${$route.params.type}Show`"
            v-if="!isLoading"
            class="detail-show"
            :detail="detail"
            :version-list="versionList"
            @pageChanged="pageChanged"
            @pageLimitChanged="pageLimitChanged"
        ></component>
    </article>
</template>

<script>
    import { mapGetters } from 'vuex'
    import atomShow from '@/components/manage/detail/atom-detail/show.vue'
    import imageShow from '@/components/manage/detail/image-detail/show.vue'

    export default {
        components: {
            atomShow,
            imageShow
        },

        data () {
            return {
                showEdit: false,
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
                'detail': 'getDetail'
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
                    image: this.getImageVersion
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
                    atomCode: this.detail.atomCode
                }).then((res) => {
                    this.versionList = res.records || []
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
            }
        }
    }
</script>

<style lang="scss" scoped>
    .manage-detail {
        background: #fff;
        .manage-detail-header {
            position: absolute;
            right: 32px;
            top: -58px;
            a {
                cursor: pointer;
                color: #1592ff;
            }
            .disable {
                cursor: not-allowed;
                color: #999;
            }
        }
        .detail-show {
            padding: 32px;
            height: 100%;
            overflow-y: auto;
        }
        /deep/ .show-detail {
            display: flex;
            align-items: flex-start;
            position: relative;
            .detail-img {
                width: 100px;
                height: 100px;
                margin-right: 32px;
            }
            .detail-items {
                flex: 1;
                max-width: calc(100% - 132px);
                overflow-x: hidden;
            }
            .detail-item {
                font-size: 14px;
                line-height: 18px;
                display: flex;
                align-items: flex-start;
                &:not(:nth-child(1)) {
                    margin-top: 18px;
                }
            }
            .detail-label {
                color: #999;
                min-width: 100px;
            }
            .item-name {
                font-size: 20px;
                line-height: 24px;
            }
            .overflow {
                max-height: 290px;
                overflow: hidden;
            }
            .summary-all {
                cursor: pointer;
                color: #1592ff;
                font-size: 14px;
                line-height: 20px;
                display: block;
                text-align: center;
                position: absolute;
                bottom: -22px;
                left: 50%;
                transform: translateX(-50%);
                &::before {
                    content: '';
                    position: absolute;
                    top: 4px;
                    left: calc(50% - 50px);
                    width: 6px;
                    height: 6px;
                    display: block;
                    transform: rotate(-45deg);
                    border-left: 2px solid #1592ff;
                    border-bottom: 2px solid #1592ff;
                }
            }
        }
        /deep/ .show-version {
            margin-top: 40px;
            .version-label {
                color: #999;
                display: flex;
                align-items: center;
                &::after {
                    content: '';
                    height: 1px;
                    background: #ebedf0;
                    margin-left: 16px;
                    flex: 1;
                }
            }
            .version-button {
                margin: 25px 0;
            }
        }
    }
</style>
