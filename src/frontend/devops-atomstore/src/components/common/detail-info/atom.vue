<template>
    <section class="detail-title">
        <img class="detail-pic atom-logo" :src="detail.logoUrl || defaultUrl">
        <hgroup class="detail-info-group">
            <h3 class="title-with-img">
                {{detail.name}}
                <template v-if="!isEnterprise && userInfo.type !== 'ADMIN' && detail.htmlTemplateVersion !== '1.0'">
                    <h5 :title="approveMsg" :class="[{ 'not-public': approveMsg !== $t('store.协作') }]" @click="cooperation">
                        <icon class="detail-img" name="cooperation" size="16" />
                        <span>{{approveMsg}}</span>
                    </h5>
                </template>
            </h3>
            <h5 class="detail-info">
                <span> {{ $t('store.发布者：') }} </span><span>{{detail.publisher || '-'}}</span>
            </h5>
            <h5 class="detail-info">
                <span> {{ $t('store.版本：') }} </span><span>{{detail.version || '-'}}</span>
            </h5>
            <h5 class="detail-info detail-score" :title="$t('store.rateTips', [(detail.score || 0), (detail.totalNum || 0)])">
                <span> {{ $t('store.评分：') }} </span>
                <p class="score-group">
                    <comment-rate :rate="5" :width="14" :height="14" :style="{ width: starWidth }" class="score-real"></comment-rate>
                    <comment-rate :rate="0" :width="14" :height="14"></comment-rate>
                </p>
                <span class="rate-num">{{detail.totalNum || 0}}</span>
            </h5>
            <h5 class="detail-info">
                <span> {{ $t('store.Job类型：') }} </span>
                <span>
                    {{detail.jobType|atomJobType}}
                    <template v-if="detail.os && detail.os.length">
                        (<i v-for="item in getJobList(detail.os)" :class="[item.icon, 'bk-icon']" :key="item" :title="item.name"></i>)
                    </template>
                </span>
            </h5>
            <h5 class="detail-info">
                <span> {{ $t('store.分类：') }} </span><span>{{detail.classifyName || '-'}}</span>
            </h5>
            <h5 class="detail-info">
                <span> {{ $t('store.热度：') }} </span><span>{{detail.downloads || 0}}</span>
            </h5>
            <h5 class="detail-info detail-label">
                <span> {{ $t('store.功能标签：') }} </span>
                <span v-for="(label, index) in detail.labelList" :key="index" class="info-label">{{label.labelName}}</span>
                <span v-if="!detail.labelList || detail.labelList.length <= 0 ">-</span>
            </h5>
            <h5 class="detail-info detail-maxwidth" :title="detail.summary">
                <span> {{ $t('store.简介：') }} </span><span>{{detail.summary || '-'}}</span>
            </h5>
        </hgroup>

        <bk-popover placement="top" v-if="buttonInfo.disable">
            <button class="bk-button bk-primary" type="button" disabled> {{ $t('store.安装') }} </button>
            <template slot="content">
                <p>{{buttonInfo.des}}</p>
            </template>
        </bk-popover>
        <button class="detail-install" @click="goToInstall" v-else> {{ $t('store.安装') }} </button>

        <bk-dialog v-model="showCooperDialog" :title="$t('store.申请成为协作者')" width="600" :on-close="closeDialog" @confirm="confirmDialog" :loading="dialogLoading" :close-icon="false">
            <bk-form label-width="90" ref="validateForm" :model="cooperData" v-if="showCooperDialog">
                <bk-form-item :label="$t('store.申请人')">
                    <bk-input v-model="user" :disabled="true"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('store.调试项目')" :required="true" :rules="rules" property="testProjectCode" :icon-offset="30">
                    <big-select class="big-select" v-model="cooperData.testProjectCode" :searchable="true" :options="projectList" setting-key="projectCode" display-key="projectName" :loading="isLoading"></big-select>
                </bk-form-item>
                <bk-form-item :label="$t('store.申请原因')" :required="true" :rules="rules" property="applyReason">
                    <bk-input type="textarea" v-model="cooperData.applyReason" :placeholder="$t('store.请输入申请原因')"></bk-input>
                </bk-form-item>
            </bk-form>
            <form-tips :prompt-list="[$t('store.欢迎加入插件开发，成为协作者后，可以丰富插件功能，优化插件'), $t('store.调试项目用来测试插件，建议使用非正式业务项目，避免影响正式流水线')]"></form-tips>
        </bk-dialog>
    </section>
</template>

<script>
    import commentRate from '../comment-rate'
    import formTips from '@/components/common/formTips/index'

    export default {
        components: {
            commentRate,
            formTips
        },

        filters: {
            atomJobType (val) {
                const local = window.devops || {}
                switch (val) {
                    case 'AGENT':
                        return local.$t('store.编译环境')
                    case 'AGENT_LESS':
                        return local.$t('store.无编译环境')
                }
            }
        },

        props: {
            detail: Object
        },

        data () {
            return {
                defaultUrl: 'http://radosgw.open.oa.com/paas_backend/ieod/dev/file/png/random_15647373141529070794466428255950.png?v=1564737314',
                showCooperDialog: false,
                user: JSON.parse(localStorage.getItem('_cache_userInfo')).username,
                cooperData: {
                    testProjectCode: '',
                    applyReason: ''
                },
                projectList: [],
                isLoading: false,
                dialogLoading: false,
                userInfo: {
                    type: ''
                },
                rules: [{ required: true, message: this.$t('store.必填项'), trigger: 'change' }]
            }
        },

        computed: {
            starWidth () {
                const integer = Math.floor(this.detail.score)
                const fixWidth = 17 * integer
                const rateWidth = 14 * (this.detail.score - integer)
                return `${fixWidth + rateWidth}px`
            },

            approveMsg () {
                const key = `${typeof this.userInfo.type}-${this.detail.approveStatus}`
                const mapStatus = {
                    'undefined-WAIT': this.$t('store.审批中'),
                    'undefined-PASS': this.$t('store.协作'),
                    'undefined-undefined': this.$t('store.协作'),
                    'undefined-REFUSE': this.$t('store.协作')
                }
                const res = mapStatus[key] || this.$t('store.已协作')
                return res
            },

            buttonInfo () {
                const info = {}
                info.disable = this.detail.defaultFlag || !this.detail.flag
                if (this.detail.defaultFlag) info.des = `${this.$t('store.通用流水线插件，所有项目默认可用，无需安装')}`
                if (!this.detail.flag) info.des = `${this.$t('store.你没有该流水线插件的安装权限，请联系流水线插件发布者')}`
                return info
            },

            isEnterprise () {
                return VERSION_TYPE === 'ee'
            }
        },

        watch: {
            'cooperData.testProjectCode' () {
                this.$refs.validateForm.validate().catch(() => {})
            }
        },

        methods: {
            initData () {
                if (this.type === 'atom') {
                    this.$store.dispatch('store/getMemberInfo', this.$route.params.code).then((res = {}) => {
                        this.userInfo = res
                    })
                }
            },

            closeDialog () {
                this.clearFormData()
            },

            confirmDialog () {
                this.dialogLoading = true
                this.$refs.validateForm.validate().then((validator) => {
                    const data = Object.assign({}, this.cooperData, { atomCode: this.$route.params.code })
                    this.$store.dispatch('store/applyCooperation', data).then((res) => {
                        if (res) {
                            this.clearFormData()
                            this.$bkMessage({ message: this.$t('store.申请成功'), theme: 'success' })
                            this.showCooperDialog = false
                            this.detail.approveStatus = res.approveStatus
                        }
                    }).catch(err => this.$bkMessage({ message: (err.message || err), theme: 'error' })).finally(() => {
                        this.dialogLoading = false
                    })
                }, () => this.$nextTick(() => (this.dialogLoading = false)))
            },

            clearFormData () {
                this.cooperData.testProjectCode = ''
                this.cooperData.applyReason = ''
            },

            cooperation () {
                if (this.approveMsg !== this.$t('store.协作')) return
                this.showCooperDialog = true
                this.getProjectList()
            },

            getProjectList () {
                this.isLoading = true
                this.$store.dispatch('store/requestProjectList').then((res) => {
                    this.projectList = res
                }).catch(err => this.$bkMessage({ message: (err.message || err), theme: 'error' })).finally(() => (this.isLoading = false))
            },

            getJobList (os) {
                const jobList = []
                os.forEach((item) => {
                    switch (item) {
                        case 'LINUX':
                            jobList.push({ icon: 'icon-linux-view', name: 'Linux' })
                            break
                        case 'WINDOWS':
                            jobList.push({ icon: 'icon-windows', name: 'Windows' })
                            break
                        case 'MACOS':
                            jobList.push({ icon: 'icon-macos', name: 'macOS' })
                            break
                    }
                })
                return jobList
            },

            goToInstall () {
                this.$router.push({
                    name: 'install',
                    query: {
                        code: this.detail.atomCode,
                        type: 'atom',
                        from: 'details'
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .detail-title {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin: 47px auto 30px;
        width: 1200px;
        .detail-pic {
            width: 130px;
        }
        .atom-icon {
            height: 160px;
            width: 160px;
        }
        .detail-install {
            width: 89px;
            height: 36px;
            background: $primaryColor;
            border-radius: 2px;
            border: none;
            font-size: 14px;
            color: $white;
            line-height: 36px;
            text-align: center;
            &.opicity-hidden {
                opacity: 0;
                user-select: none;
            }
            &:active {
                transform: scale(.97)
            }
        }
        .bk-tooltip button {
            width: 89px;
        }
    }
    .detail-info-group {
        width: 829px;
        margin: 0 76px;
        
        h3 {
            font-size: 22px;
            line-height: 29px;
            color: $fontBlack;
        }
        .detail-score {
            display: flex;
            align-items: center;
            .score-group {
                position: relative;
                .score-real {
                    position: absolute;
                    overflow: hidden;
                    left: 0;
                    top: 0;
                    height: 14px;
                    display: flex;
                    .yellow {
                        min-width: 14px;
                    }
                }
            }
            .rate-num {
                margin-top: 2px;
                margin-left: 6px;
                color: $fontWeightColor;
            }
        }
        .detail-info {
            float: left;
            display: flex;
            padding-top: 7px;
            width: 33.33%;
            font-size: 14px;
            font-weight: normal;
            line-height: 19px;
            color: $fontBlack;
            span:nth-child(1) {
                color: $fontWeightColor;
                display: inline-block;
                width: 90px;
                padding-right: 10px;
                text-align: right;
            }
            span:nth-child(2) {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                display: inline-block;
                width: calc(100% - 90px);
            }
        }
        .title-with-img {
            display: flex;
            align-items: center;
            h5 {
                cursor: pointer;
            }
            span {
                margin-left: -2px;
                font-size: 14px;
                color: $fontLightGray;
                line-height: 19px;
                font-weight: normal;
            }
            .detail-img {
                margin-left: 12px;
                vertical-align: middle;
            }
            .not-public {
                cursor: auto;
            }
        }
        .detail-info.detail-label {
            width: 829px;
            padding-left: 90px;
            display: inline-block;
            position: relative;
            span {
                overflow: inherit;
                margin-bottom: 7px;
            }
            span:first-child {
                position: absolute;
                left: 0;
            }
            span.info-label {
                display: inline-block;
                width: auto;
                height: 19px;
                padding: 0 7px;
                border: 1px solid $laberColor;
                border-radius: 20px;
                margin-right: 8px;
                line-height: 17px;
                text-align: center;
                font-size: 12px;
                color: $laberColor;
                background-color: $laberBackColor;
            }
        }
        .detail-maxwidth {
            max-width: 100%;
            width: auto;
            padding-top: 0;
        }
    }
    /deep/ .is-error .big-select {
        border: 1px solid $dangerColor;
    }
    /deep/ .bk-dialog-body .tips-body {
        text-align: left;
    }
</style>
