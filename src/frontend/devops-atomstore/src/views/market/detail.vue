<template>
    <article class="detail-home" v-bkloading="{ isLoading }">
        <bread-crumbs :bread-crumbs="navList" :type="type">
            <router-link :to="{ name: 'atomWork' }" class="g-title-work"> {{ $t('store.工作台') }} </router-link>
        </bread-crumbs>

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
    import api from '@/api'
    import { mapActions, mapGetters } from 'vuex'
    import breadCrumbs from '@/components/bread-crumbs.vue'
    import atomInfo from '../../components/common/detail-info/atom'
    import templateInfo from '../../components/common/detail-info/template'
    import imageInfo from '../../components/common/detail-info/image'
    import detailScore from '../../components/common/detailTab/detailScore'
    import codeSection from '../../components/common/detailTab/codeSection'
    import yamlDetail from '../../components/common/detailTab/yamlDetail'
    import outputDetail from '../../components/common/detailTab/outputDetail'
    import qualityDetail from '../../components/common/detailTab/qualityDetail'

    export default {
        components: {
            atomInfo,
            templateInfo,
            imageInfo,
            detailScore,
            codeSection,
            breadCrumbs,
            yamlDetail,
            outputDetail,
            qualityDetail
        },

        data () {
            return {
                isLoading: true,
                currentTab: 'des'
            }
        },

        computed: {
            ...mapGetters('store', { markerQuey: 'getMarketQuery', detail: 'getDetail' }),

            detailCode () {
                return this.$route.params.code
            },

            type () {
                return this.$route.params.type
            },

            tabList () {
                return {
                    atom: [
                        { componentName: 'detailScore', label: this.$t('store.概述'), name: 'des' },
                        { componentName: 'yamlDetail', label: this.$t('store.YAMLV2'), name: 'YAMLV2', bindData: { code: this.detail.codeSectionV2, limitHeight: false, name: 'YAMLV2', currentTab: this.currentTab, getDataFunc: this.getAtomYamlV2 }, hidden: (!this.detail.yamlFlag || !this.detail.recommendFlag) },
                        { componentName: 'outputDetail', label: this.$t('store.输出参数'), name: 'output', bindData: { outputData: this.detail.outputData, name: 'output', currentTab: this.currentTab, classifyCode: this.detail.classifyCode } },
                        { componentName: 'qualityDetail', label: this.$t('store.质量红线指标'), name: 'quality', bindData: { qualityData: this.detail.qualityData }, hidden: this.detail.qualityData && !this.detail.qualityData.length }
                    ],
                    template: [
                        { componentName: 'detailScore', label: this.$t('store.概述'), name: 'des' }
                    ],
                    image: [
                        { componentName: 'detailScore', label: this.$t('store.概述'), name: 'des' },
                        { componentName: 'codeSection', label: 'Dockerfile', name: 'Dockerfile', bindData: { code: this.detail.codeSection, limitHeight: false } }
                    ]
                }
            },

            navList () {
                let name
                switch (this.type) {
                    case 'template':
                        name = this.$t('store.流水线模板')
                        break
                    case 'image':
                        name = this.$t('store.容器镜像')
                        break
                    default:
                        name = this.$t('store.流水线插件')
                        break
                }
                Object.assign(this.markerQuey, { pipeType: this.type })
                return [
                    { name, to: { name: 'atomHome', query: this.markerQuey } },
                    { name: this.detail.name }
                ]
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
                'getAtomYamlV2'
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
                    this.requestAtom(atomCode),
                    this.requestAtomStatistic({ storeCode: atomCode, storeType: 'ATOM' }),
                    this.getUserApprovalInfo(atomCode),
                    this.getQualityData(atomCode)
                ]).then(([atomDetail, atomStatic, userAppInfo, quality]) => {
                    const detail = atomDetail || {}
                    detail.detailId = atomDetail.atomId
                    detail.recentExecuteNum = atomStatic.recentExecuteNum || 0
                    detail.hotFlag = atomStatic.hotFlag
                    detail.approveStatus = (userAppInfo || {}).approveStatus
                    detail.qualityData = quality
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

            getQualityData () {
                return api.requestAtomQuality(this.detailCode)
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';
    .store-main {
        height: calc(94.4vh - 50px);
        overflow-y: scroll;
    }

    .detail-home {
        overflow: hidden;
        min-height: 100%;
        background: $grayBackGroundColor;
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
