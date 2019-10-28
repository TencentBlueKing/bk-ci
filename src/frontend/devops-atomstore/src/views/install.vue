<template>
    <div class="install-atom-wrapper" v-bkloading="{ isLoading }">

        <h3 class="market-home-title">
            <icon class="title-icon" name="color-logo-store" size="25" />
            <p class="title-name">
                <router-link :to="{ name: 'atomHome' }" class="back-home">研发商店</router-link>
                <i class="right-arrow banner-arrow"></i>
                <span class="back-home" @click="backToStore">{{ type|typeFilter }}</span>
                <i class="right-arrow banner-arrow"></i>
                <span class="banner-des back-home" @click="toBack">{{ fromFilter(from) }}</span>
                <i class="right-arrow banner-arrow"></i>
                <span>安装{{ type|typeFilter }}</span>
            </p>
        </h3>

        <div class="install-atom-content" v-if="!isLoading">
            <div class="sub-view-port" v-if="!isINstallSuccess">
                <div class="atom-name">{{ name }}</div>
                <div class="title">请选择项目：</div>
                <big-select v-model="project" :loading="projectListLoading" :searchable="true" :multiple="true" :show-select-all="true" :options="projectList" setting-key="project_code" display-key="project_name" @selected="selectProject" placeholder="请选择">
                    <div slot="extension" style="cursor: pointer;">
                        <a href="/console/pm" target="_blank"><i class="bk-icon icon-plus-circle" />新建项目</a>
                    </div>
                </big-select>
                <div v-if="installError" class="error-tips">项目不能为空</div>
                <div class="form-footer">
                    <button class="bk-button bk-primary" type="button" @click="confirm">安装</button>
                    <button class="bk-button bk-default" type="button" @click="toBack">取消</button>
                </div>
                <section v-if="installedProject.length">
                    <p class="project-title">该{{ isInstallAtom ? '流水线插件' : '模板' }}已安装至以下项目：</p>
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
                <i class="bk-icon icon-check-circle"></i>
                <h3>恭喜，已安装成功！</h3>
                <div class="handle-btn">
                    <bk-button class="bk-button bk-primary" size="small" @click="backConsole">工作台</bk-button>
                    <bk-button class="bk-button bk-default" size="small" @click="backToStore">研发商店</bk-button>
                    <bk-button class="bk-button bk-default" size="small" @click="toPipeline">流水线</bk-button>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'

    export default {
        filters: {
            typeFilter (val) {
                let res = ''
                switch (val) {
                    case 'atom':
                        res = '流水线插件'
                        break
                    case 'template':
                        res = '流水线模板'
                        break
                    default:
                        res = '镜像'
                        break
                }
                return res
            }
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
            ...mapGetters('store', { 'markerQuey': 'getMarketQuery' })
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
                        res = '工作台'
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
                return this.$store.dispatch('store/requestAtom', { atomCode: this.code }).then((res) => {
                    this.name = res.name
                    this.id = res.atomId
                })
            },

            getTemplateDetail () {
                return this.$store.dispatch('store/requestTemplate', { templateCode: this.code }).then((res) => {
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

                this.$store.dispatch(methods[this.type], this.code).then((res) => {
                    this.installedProject = res
                })
            },

            requestProjectList () {
                this.projectListLoading = true
                this.$store.dispatch('store/requestProjectList').then((res) => {
                    res.forEach((item) => {
                        const isInstalled = this.installedProject.some(project => project.projectCode === item.project_code)
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
                this.$router.push({
                    name: 'atomList',
                    params: {
                        type: this.type
                    }
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
                    this.$bkMessage({ message: '安装成功', theme: 'success' })
                }).catch((err) => {
                    if (err.httpStatus === 200) {
                        const h = this.$createElement
                        const subHeader = h('p', {
                            style: {
                                textAlign: 'left',
                                padding: '20px 30px 0'
                            }
                        }, err.message ? err.message : err)

                        this.$bkInfo({
                            type: 'error',
                            title: '安装失败',
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
        .info-header {
            display: flex;
            justify-content: space-between;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid #DDE4EB;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .sub_header_left {
                display: flex;
                padding: 18px 24px;
                .title {
                    display: flex;
                    align-items: center;
                }
                .first-level,
                .secondary {
                    color: $primaryColor;
                    cursor: pointer;
                }
                .third-leve {
                    color: $fontWeightColor;
                }
                .nav-icon {
                    width: 24px;
                    height: 24px;
                    margin-right: 10px;
                }
                .right-arrow {
                    display :inline-block;
                    position: relative;
                    width: 19px;
                    height: 36px;
                    margin-right: 4px;
                }
                .right-arrow::after {
                    display: inline-block;
                    content: " ";
                    height: 4px;
                    width: 4px;
                    border-width: 1px 1px 0 0;
                    border-color: $lineColor;
                    border-style: solid;
                    transform: matrix(0.71, 0.71, -0.71, 0.71, 0, 0);
                    position: absolute;
                    top: 50%;
                    right: 6px;
                    margin-top: -9px;
                }
            }
        }
        .install-atom-content {
            padding: 20px 0 40px;
            height: calc(100% - 50px);
            overflow: auto;
            .sub-view-port,
            .install-success-tips {
                margin: 20px auto;
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
