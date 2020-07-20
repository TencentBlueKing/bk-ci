<template>
    <article class="detail-home" v-bkloading="{ isLoading }">
        <h3 class="market-home-title">
            <icon class="title-icon" name="color-logo-store" size="25" />
            <p class="title-name">
                <router-link :to="{ name: 'atomHome' }" class="back-home"> {{ $t('store.研发商店') }} </router-link>
                <i class="right-arrow banner-arrow"></i>
                <span class="back-home" @click="backToStore">{{type|typeFilter}}</span>
                <i class="right-arrow banner-arrow"></i>
                <span class="banner-des">{{detail.name}}</span>
            </p>
            <router-link :to="{ name: 'workList' }" class="title-work"> {{ $t('store.工作台') }} </router-link>
        </h3>

        <main class="store-main" v-if="!isLoading">
            <component :is="`${type}Info`" :detail="detail" class="detail-info" :current-tab.sync="currentTab"></component>
            <bk-tab type="unborder-card" :active.sync="currentTab" class="detail-tabs">
                <bk-tab-panel :name="tab.name" :label="tab.label" v-for="(tab, index) in tabList[type].filter(x => !x.hidden)" :key="index">
                    <component :is="tab.componentName" v-bind="tab.bindData"></component>
                </bk-tab-panel>
            </bk-tab>
        </main>
    </article>
</template>

<script>
    import { mapActions, mapGetters } from 'vuex'
    import atomInfo from '../../components/common/detail-info/atom'
    import templateInfo from '../../components/common/detail-info/template'
    import imageInfo from '../../components/common/detail-info/image'
    import detailScore from '../../components/common/detailTab/detailScore'
    import codeSection from '../../components/common/detailTab/codeSection'

    export default {
        components: {
            atomInfo,
            templateInfo,
            imageInfo,
            detailScore,
            codeSection
        },

        filters: {
            typeFilter (val) {
                const bkLocale = window.devops || {}
                let res = ''
                switch (val) {
                    case 'template':
                        res = bkLocale.$t('store.流水线模板')
                        break
                    case 'image':
                        res = bkLocale.$t('store.容器镜像')
                        break
                    default:
                        res = bkLocale.$t('store.流水线插件')
                        break
                }
                return res
            }
        },

        data () {
            return {
                isLoading: true,
                currentTab: 'des'
            }
        },

        computed: {
            ...mapGetters('store', { 'markerQuey': 'getMarketQuery', 'detail': 'getDetail' }),

            detailCode () {
                return this.$route.params.code
            },

            type () {
                return this.$route.params.type
            },

            tabList () {
                return {
                    atom: [
                        { componentName: 'detailScore', label: this.$t('概述'), name: 'des' },
                        { componentName: 'codeSection', label: this.$t('YAML片段'), name: 'YAML', bindData: { code: this.detail.codeSection, limitHeight: false }, hidden: (!this.detail.yamlFlag || !this.detail.recommendFlag) }
                    ],
                    template: [
                        { componentName: 'detailScore', label: this.$t('概述'), name: 'des' }
                    ],
                    image: [
                        { componentName: 'detailScore', label: this.$t('概述'), name: 'des' },
                        { componentName: 'codeSection', label: 'Dockerfile', name: 'Dockerfile', bindData: { code: this.detail.codeSection, limitHeight: false } }
                    ]
                }
            }
        },

        mounted () {
            this.getDetail()
        },

        beforeDestroy () {
            this.clearDetail()
        },
        
        methods: {
            ...mapActions('store', [
                'clearDetail',
                'setDetail',
                'requestAtom',
                'requestAtomStatistic',
                'requestTemplateDetail',
                'requestImage',
                'getUserApprovalInfo',
                'requestImageCategorys',
                'getAtomYaml'
            ]),

            getDetail () {
                const type = this.$route.params.type
                const funObj = {
                    atom: () => this.getAtomDetail(),
                    template: () => this.getTemplateDetail(),
                    image: () => this.getImageDetail()
                }
                const getDetailMethod = funObj[type]

                getDetailMethod().catch((err) => {
                    this.$bkMessage({ message: (err.message || err), theme: 'error' })
                }).finally(() => (this.isLoading = false))
            },

            getAtomDetail () {
                const atomCode = this.detailCode

                return Promise.all([
                    this.requestAtom({ atomCode }),
                    this.requestAtomStatistic({ atomCode }),
                    this.getUserApprovalInfo(atomCode),
                    this.getAtomYaml({ atomCode })
                ]).then(([atomDetail, atomStatic, userAppInfo, yaml]) => {
                    const detail = atomDetail || {}
                    detail.detailId = atomDetail.atomId
                    detail.downloads = atomStatic.downloads || 0
                    detail.approveStatus = (userAppInfo || {}).approveStatus
                    detail.codeSection = yaml
                    this.setDetail(detail)
                })
            },

            getTemplateDetail () {
                const templateCode = this.detailCode
                return this.requestTemplateDetail(templateCode).then((templateDetail) => {
                    const detail = templateDetail || {}
                    detail.detailId = templateDetail.templateId
                    detail.name = templateDetail.templateName
                    this.setDetail(detail)
                })
            },

            getImageDetail () {
                const imageCode = this.detailCode

                return Promise.all([
                    this.requestImageCategorys(),
                    this.requestImage({ imageCode })
                ]).then(([categorys, res]) => {
                    const detail = res || {}
                    detail.detailId = res.imageId
                    detail.name = res.imageName
                    detail.codeSection = res.dockerFileContent
                    const currentCategory = categorys.find((x) => (x.categoryCode === res.category))
                    const setting = currentCategory.settings || {}
                    detail.needInstallToProject = setting.needInstallToProject
                    this.setDetail(detail)
                })
            },

            backToStore () {
                Object.assign(this.markerQuey, { pipeType: this.type })
                this.$router.push({
                    name: 'atomHome',
                    query: this.markerQuey
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';
    .store-main {
        height: calc(100vh - 93px);
        overflow-y: scroll;
        background: $grayBackGroundColor;
    }

    .detail-home {
        overflow: hidden;
        min-height: 100%;
    }

    .detail-info {
        max-width: 1400px;
    }

    .detail-tabs {
        margin: 20px auto 30px;
        width: 95vw;
        max-width: 1400px;
        background: #fff;
        padding: 10px 32px 40px;
        box-shadow: 1px 2px 3px 0px rgba(0,0,0,0.05);
    }
</style>
