<template>
    <div class="install-atom-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">

        <h3 class="market-home-title">
            <icon class="title-icon" name="color-logo-store" size="25" />
            <p class="title-name">
                <router-link :to="{ name: 'atomHome' }" class="back-home">研发商店</router-link>
                <i class="right-arrow banner-arrow"></i>
                <span class="back-home" @click="backToStore">{{isInstallAtom|isInstallAtomFilter}}</span>
                <i class="right-arrow banner-arrow"></i>
                <span class="banner-des back-home" @click="toBack">{{ secondaryTitle }}</span>
                <i class="right-arrow banner-arrow"></i>
                <span v-if="isInstallAtom">安装流水线插件</span>
                <span v-else>安装模板</span>
            </p>
        </h3>

        <div class="install-atom-content" v-if="showContent">
            <div class="sub-view-port" v-if="!isINstallSuccess">
                <div class="atom-name">{{ formName }}</div>
                <div class="title">请选择项目：</div>
                <bk-select v-model="project" searchable multiple show-select-all>
                    <bk-option v-for="(option, index) in projectList"
                        :key="index"
                        :id="option.project_code"
                        :name="option.project_name"
                        @click.native="selectedProject"
                        @toggle="getProjectList"
                        @selected="selectProject"
                        :placeholder="'请选择'"
                    >
                    </bk-option>
                    <div slot="extension" style="cursor: pointer;">
                        <a :href="itemUrl" target="_blank">
                            <i class="bk-icon icon-plus-circle" />
                            {{ itemText }}
                        </a>
                    </div>
                </bk-select>
                <div v-if="installError" class="error-tips">项目不能为空</div>
                <!-- <label class="bk-form-radio p0">
                    <input type="radio" value="true" class="standard-radio"
                        v-model="isAgree">
                    <span class='bk-radio-text'>同意</span>
                </label>
                <a class="link-text" @click="toLink()">《蓝盾流水线插件使用规范》</a> -->
                <div class="form-footer">
                    <button class="bk-button bk-primary" type="button" :disabled="!isAgree" @click="confirm()">安装</button>
                    <button class="bk-button bk-default" type="button" @click="toBack()">取消</button>
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
                    <bk-button class="bk-button bk-primary" size="small" @click="backConsole()">工作台</bk-button>
                    <bk-button class="bk-button bk-default" size="small" @click="backToStore()">研发商店</bk-button>
                    <bk-button class="bk-button bk-default" size="small" @click="toPipeline()">流水线</bk-button>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'

    export default {

        filters: {
            isInstallAtomFilter (val) {
                if (val) return '流水线插件'
                else return '流水线模板'
            }
        },
        data () {
            return {
                itemUrl: '/console/pm',
                itemText: '新建项目',
                showContent: false,
                installError: false,
                isINstallSuccess: false,
                isAgree: true,
                atomId: '',
                templateId: '',
                project: [],
                projectList: window.projectList,
                installedProject: [],
                formName: '',
                loading: {
                    isLoading: false,
                    title: ''
                }
            }
        },

        computed: {
            ...mapGetters('store', { 'markerQuey': 'getMarketQuery' }),

            routerParams () {
                return this.$route.params
            },
            atomCode () {
                return this.routerParams.atomCode
            },
            templateCode () {
                return this.routerParams.templateCode
            },
            originType () {
                return this.$route.hash === '#MYATOM' ? 'myAtom' : 'storeAtom'
            },
            isInstallAtom () {
                return this.$route.name === 'installAtom'
            },
            secondaryTitle () {
                return this.originType === 'myAtom' ? '工作台' : this.formName
            }
        },
        async created () {
            await this.requestAtomDetail()
            await this.requestRelativeProject()
            if (this.$route.query.projectCode) {
                this.project.push(this.$route.query.projectCode)
            }
        },
        methods: {
            async requestAtomDetail () {
                this.loading.isLoading = true

                try {
                    let res = {}
                    if (this.isInstallAtom) {
                        res = await this.$store.dispatch('store/requestAtom', {
                            atomCode: this.atomCode
                        })
                    } else {
                        res = await this.$store.dispatch('store/requestTemplate', {
                            templateCode: this.templateCode
                        })
                    }
        
                    this.formName = this.isInstallAtom ? res.name : res.templateName
                    this.isInstallAtom ? this.atomId = res.atomId : this.templateId = res.templateId
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                        this.showContent = true
                    }, 100)
                }
            },
            async requestRelativeProject () {
                this.loading.isLoading = true

                try {
                    let res
                    if (this.isInstallAtom) {
                        res = await this.$store.dispatch('store/requestRelativeProject', {
                            atomCode: this.atomCode
                        })
                    } else {
                        res = await this.$store.dispatch('store/requestRelativeTplProject', {
                            templateCode: this.templateCode
                        })
                    }
            
                    res.map(item => {
                        this.installedProject.push(item)
                        this.projectList = this.projectList.filter(val => {
                            return val.project_code !== item.projectCode
                        })
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                    }, 1000)
                }
            },
            toBack () {
                if (this.originType === 'myAtom') {
                    this.backConsole()
                } else {
                    this.$router.push({
                        name: 'details',
                        params: {
                            code: this.isInstallAtom ? this.atomCode : this.templateCode,
                            type: this.isInstallAtom ? 'atom' : 'template'
                        }
                    })
                }
            },
            backConsole () {
                this.$router.push({
                    name: 'atomList',
                    params: {
                        type: this.isInstallAtom ? 'atom' : 'template'
                    }
                })
            },
            backToStore () {
                const pipeType = this.isInstallAtom ? 'atom' : 'template'
                Object.assign(this.markerQuey, { pipeType })
                this.$router.push({
                    name: 'atomHome',
                    query: this.markerQuey
                })
            },
            toLink () {
                window.open(`${DOCS_URL_PREFIX}/`, '_target')
            },
            toPipeline () {
                window.open('/console/pipeline', '_self')
            },
            getProjectList () {

            },
            selectProject (data) {
                this.project = data
                this.installError = false
            },
            async confirm () {
                if (!this.project.length) {
                    this.installError = true
                } else {
                    let message, theme, res
                    const params = {}
                    
                    if (this.isInstallAtom) {
                        params.atomCode = this.atomCode
                        params.projectCode = this.project
                    } else {
                        params.templateCode = this.templateCode
                        params.projectCodeList = this.project
                    }

                    this.loading.isLoading = true
                    try {
                        if (this.isInstallAtom) {
                            res = await this.$store.dispatch('store/installAtom', { params })
                        } else {
                            res = await this.$store.dispatch('store/installTemplate', { params })
                        }
                        
                        if (res) {
                            message = '安装成功'
                            theme = 'success'
                        }
                    } catch (err) {
                        if (err.httpStatus === 200) {
                            const h = this.$createElement
                            const subtitle = h('p', {
                                style: {
                                    textAlign: 'left',
                                    padding: '20px 30px 0'
                                }
                            }, err.message ? err.message : err)

                            this.$bkInfo({
                                type: 'error',
                                clsName: 'error-commit',
                                delay: 10000,
                                statusOpts: {
                                    title: '安装失败',
                                    subtitle
                                }
                            })
                        } else {
                            message = err.message ? err.message : err
                            theme = 'error'
                        }
                    } finally {
                        this.loading.isLoading = false
                        if (theme === 'success') {
                            this.isINstallSuccess = true
                        } else if (theme === 'error') {
                            this.$bkMessage({
                                message,
                                theme
                            })
                        }
                    }
                }
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
            .icon-close {
                position: absolute;
                top: 0;
                right: 0;
                margin-top: 14px;
                margin-right: 14px;
                font-size: 12px;
                color: $fontLigtherColor;
                cursor: pointer;
            }
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
