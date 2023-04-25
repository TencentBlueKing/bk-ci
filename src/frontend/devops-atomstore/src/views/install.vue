<template>
    <div class="install-atom-wrapper" v-bkloading="{ isLoading }">
        <bread-crumbs :bread-crumbs="navList" :type="type"></bread-crumbs>

        <div class="install-atom-content" v-if="!isLoading">
            <div class="sub-view-port" v-if="!isINstallSuccess">
                <div class="atom-name">{{ name }}</div>
                <div class="title"> {{ $t('store.请选择项目：') }} </div>
                <bk-select v-model="project"
                    searchable
                    multiple
                    show-select-all
                    @selected="selectProject"
                    :placeholder="$t('store.请选择')"
                    :loading="projectListLoading"
                    :enable-virtual-scroll="projectList && projectList.length > 3000"
                    :list="projectList"
                    id-key="projectCode"
                    display-key="projectName"
                >
                    <bk-option
                        v-for="item in projectList"
                        :key="item.projectCode"
                        :id="item.projectCode"
                        :name="item.projectName"
                    >
                    </bk-option>
                    <div slot="extension" style="cursor: pointer;">
                        <a href="/console/pm" target="_blank"><i class="devops-icon icon-plus-circle" /> {{ $t('store.新建项目') }} </a>
                    </div>
                </bk-select>
                <p class="template-tip" v-if="type === 'template'">{{ $t('store.若模版中有未安装的插件，将自动安装') }}</p>
                <div v-if="installError" class="error-tips"> {{ $t('store.项目不能为空') }} </div>
                <div class="form-footer">
                    <button class="bk-button bk-primary" type="button" @click="confirm"> {{ $t('store.安装') }} </button>
                    <button class="bk-button bk-default" type="button" @click="toBack"> {{ $t('store.取消') }} </button>
                </div>
                <section v-if="installedProject.length">
                    <p class="project-title">
                        {{ $t('store.该 {0} 已安装至以下项目：', [type || typeFilter]) }}
                    </p>
                    <table class="bk-table project-table">
                        <thead>
                        </thead>
                        <tbody>
                            <tr v-for="(row, index) in installedProject" :key="index">
                                <td>{{ row.projectName }}</td>
                            </tr>
                        </tbody>
                    </table>
                </section>
            </div>
            <div class="install-success-tips" v-else>
                <i class="devops-icon icon-check-circle"></i>
                <h3> {{ $t('store.恭喜，已安装成功！') }} </h3>
                <div class="handle-btn">
                    <bk-button class="bk-button bk-primary" size="small" @click="backConsole"> {{ $t('store.工作台') }} </bk-button>
                    <bk-button class="bk-button bk-default" size="small" @click="backToStore"> {{ $t('store.研发商店') }} </bk-button>
                    <bk-button class="bk-button bk-default" size="small" @click="toPipeline" v-if="['atom', 'template', 'image'].includes(type)"> {{ $t('store.流水线') }} </bk-button>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'
    import breadCrumbs from '@/components/bread-crumbs.vue'

    export default {
        components: {
            breadCrumbs
        },

        data () {
            return {
                type: this.$route.query.type,
                code: this.$route.query.code,
                from: this.$route.query.from,
                name: '',
                id: '',
                installError: false,
                projectListLoading: false,
                isINstallSuccess: false,
                project: [],
                projectList: [],
                installedProject: [],
                isLoading: false
            }
        },

        computed: {
            ...mapGetters('store', { markerQuey: 'getMarketQuery' }),

            navList () {
                let name
                switch (this.type) {
                    case 'atom':
                        name = this.$t('store.流水线插件')
                        break
                    case 'template':
                        name = this.$t('store.流水线模板')
                        break
                    default:
                        name = this.$t('store.容器镜像')
                        break
                }

                Object.assign(this.markerQuey, { pipeType: this.type })
                return [
                    { name, to: { name: 'atomHome', query: this.markerQuey } },
                    { name: this.fromFilter(this.from), to: { name: this.from, params: { type: this.type, code: this.code } } },
                    { name: this.$t('store.安装') + name }
                ]
            }
        },

        created () {
            this.isLoading = true
            Promise.all([this.requestDetail(), this.requestRelativeProject()]).then(() => {
                this.requestProjectList()
            }).catch((err) => {
                this.$bkMessage({ message: err.messgae || err, theme: 'error' })
            }).finally(() => {
                this.isLoading = false
            })
        },

        methods: {
            fromFilter (val) {
                let res = ''
                switch (val) {
                    case 'details':
                        res = this.name
                        break
                    default:
                        res = this.$t('store.工作台')
                        break
                }
                return res
            },
    
            requestDetail () {
                const methods = {
                    atom: this.getAtomDetail,
                    template: this.getTemplateDetail,
                    image: this.getImageDetail
                }

                return methods[this.type]()
            },

            getAtomDetail () {
                return this.$store.dispatch('store/requestAtom', this.code).then((res) => {
                    this.name = res.name
                    this.id = res.atomId
                })
            },

            getTemplateDetail () {
                return this.$store.dispatch('store/requestTemplate', this.code).then((res) => {
                    this.name = res.templateName
                    this.id = res.templateId
                })
            },

            getImageDetail () {
                return this.$store.dispatch('store/requestImageDetailByCode', this.code).then((res) => {
                    this.name = res.imageName
                    this.id = res.imageId
                })
            },

            requestRelativeProject () {
                const methods = {
                    atom: 'store/requestRelativeProject',
                    template: 'store/requestRelativeTplProject',
                    image: 'store/requestRelativeImageProject'
                }

                return this.$store.dispatch(methods[this.type], this.code).then((res) => {
                    this.installedProject = res
                })
            },

            requestProjectList () {
                this.projectListLoading = true
                this.$store.dispatch('store/requestProjectList').then((res) => {
                    res.forEach((item) => {
                        const isInstalled = this.installedProject.some(project => project.projectCode === item.projectCode)
                        if (!isInstalled) this.projectList.push(item)
                    })
                }).catch((err) => {
                    this.$bkMessage({ message: err.messgae || err, theme: 'error' })
                }).finally(() => (this.projectListLoading = false))
            },

            toBack () {
                this.$router.push({
                    name: this.from,
                    params: {
                        type: this.type,
                        code: this.code
                    }
                })
            },

            backConsole () {
                const name = `${this.type}Work`
                this.$router.push({
                    name
                })
            },

            backToStore () {
                const pipeType = this.type
                Object.assign(this.markerQuey, { pipeType })
                this.$router.push({
                    name: 'atomHome',
                    query: this.markerQuey
                })
            },

            toPipeline () {
                window.open('/console/pipeline', '_self')
            },

            selectProject (data) {
                this.installError = !(Array.isArray(data) && data.length > 0)
            },

            confirm () {
                if (!this.project.length) {
                    this.installError = true
                    return
                }

                const methods = {
                    atom: this.installAtom,
                    template: this.installTemplate,
                    image: this.installImage
                }

                this.isLoading = true
                methods[this.type]().then(() => {
                    this.isINstallSuccess = true
                    this.$bkMessage({ message: this.$t('store.安装成功'), theme: 'success' })
                }).catch((err) => {
                    if (err.httpStatus === 200) {
                        const h = this.$createElement
                        const subHeader = h('p', {
                            style: {
                                textAlign: 'left',
                                'text-overflow': 'ellipsis',
                                'white-space': 'nowrap',
                                overflow: 'hidden'
                            },
                            attrs: {
                                title: err.message || err
                            }
                        }, err.message || err)

                        this.$bkInfo({
                            type: 'error',
                            title: this.$t('store.安装失败'),
                            subHeader
                        })
                    } else {
                        this.$bkMessage({ message: err.message || err, theme: 'error' })
                    }
                }).finally(() => (this.isLoading = false))
            },

            installAtom () {
                const params = {
                    atomCode: this.code,
                    projectCode: this.project
                }
                return this.$store.dispatch('store/installAtom', { params })
            },

            installTemplate () {
                const params = {
                    templateCode: this.code,
                    projectCodeList: this.project
                }
                return this.$store.dispatch('store/installTemplate', { params })
            },

            installImage () {
                const params = {
                    imageCode: this.code,
                    projectCodeList: this.project
                }
                return this.$store.dispatch('store/installImage', params)
            }
        }
    }
</script>

<style lang="scss">
    @import '@/assets/scss/conf.scss';
    .install-atom-wrapper {
        height: 100%;
        .install-atom-content {
            padding: 20px 0 40px;
            height: calc(100vh - 5.6vh);
            overflow: auto;
            .template-tip {
                margin-top: 10px;
            }
            .sub-view-port,
            .install-success-tips {
                margin: 0 auto 20px;
                padding: 20px;
                width: 60%;
                min-width: 960px;
                min-height: 100%;
                background: #fff;
            }
            .install-success-tips {
                padding-top: 60px;
                text-align: center;
                .icon-check-circle {
                    font-size: 80px;
                    color: #30D878;
                }
                h3 {
                    margin: 18px auto 38px;
                    font-size: 20px;
                }
            }
            .bk-selector {
                .bk-form-checkbox {
                    display: block;
                    padding: 12px 0;
                }
            }
            // .icon-close {
            //     position: absolute;
            //     top: 0;
            //     right: 0;
            //     margin-top: 14px;
            //     margin-right: 14px;
            //     font-size: 12px;
            //     color: $fontLigtherColor;
            //     cursor: pointer;
            // }
            .atom-name {
                margin-bottom: 20px;
                text-align: center;
                font-size: 22px;
            }
            .title {
                margin-bottom: 14px;
                font-size: 16px;
                font-weight: bold;
                color: $fontWeightColor;
            }
            .bk-form-radio {
                margin-top: 14px;
                margin-right: 0;
            }
            .link-text {
                color: $primaryColor;
                cursor: pointer;
            }
            .form-footer {
                margin: 20px auto 30px;
                padding: 10px 0 10px 10px;
                text-align: right;
            }
            .project-table {
                margin-top: 12px;
                margin-bottom: 40px;
                border: 1px solid #e9edee;
                td {
                    height: 42px;
                    font-weight: normal;
                }
            }
        }
    }
    .error-commit {
        .bk-dialog-default-status {
            padding-top: 10px;
        }
    }
</style>
