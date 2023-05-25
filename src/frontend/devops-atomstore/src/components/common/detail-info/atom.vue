<template>
    <section class="detail-title">
        <honer-img
            class="detail-pic atom-logo"
            :detail="detail"
            :is-big="true"
        />
        <hgroup class="detail-info-group">
            <h3 class="title-with-img">
                <span
                    :class="{ 'not-recommend': detail.recommendFlag === false }"
                    :title="detail.recommendFlag === false ? $t('store.该插件不推荐使用') : ''"
                >{{detail.name}}</span>
                <honer-tag :detail="detail" :max-num="2" class="ml16"></honer-tag>
                <img
                    v-for="indexInfo in detail.indexInfos"
                    v-bk-tooltips="{
                        allowHTML: true,
                        content: indexInfo.hover
                    }"
                    :key="indexInfo.indexCode"
                    :src="indexInfo.iconUrl"
                    :style="{
                        color: indexInfo.iconColor,
                        height: '16px',
                        width: '16px',
                        marginRight: '8px',
                        cursor: 'pointer'
                    }"
                >
                <section class="num-wraper ml16">
                    <p class="score-group">
                        <comment-rate
                            :max-stars="1"
                            :rate="1"
                            :width="16"
                            :height="16"
                            :style="{ width: starWidth }"
                            class="score-real"
                        />
                        <comment-rate
                            :max-stars="1"
                            :rate="0"
                            :width="16"
                            :height="16"
                        />
                    </p>
                    <span class="ml6">{{ detail.score }}</span>
                </section>
                <section class="num-wraper">
                    <img v-if="detail.hotFlag" class="hot-icon" src="../../../images/hot-red.png">
                    <img v-else class="hot-icon" src="../../../images/hot.png">
                    <span class="ml3">{{ getShowNum(detail.recentExecuteNum) }}</span>
                </section>
            </h3>
            <h5 class="detail-info">
                <span> {{ $t('store.发布者：') }} </span><span>{{detail.publisher || '-'}}</span>
            </h5>
            <h5 class="detail-info">
                <span> {{ $t('store.版本：') }} </span><span>{{detail.version || '-'}}</span>
            </h5>
            <h5 class="detail-info">
                <span> {{ $t('store.Job类型：') }} </span>
                <span>
                    {{detail.jobType|atomJobType}}
                    <template v-if="detail.os && detail.os.length">
                        (<i v-for="item in getJobList(detail.os)" :class="[item.icon, 'devops-icon']" :key="item" :title="item.name"></i>)
                    </template>
                </span>
            </h5>
            <h5 class="detail-info">
                <span> {{ $t('store.分类：') }} </span><span>{{detail.classifyName || '-'}}</span>
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

        <section>
            <bk-popover placement="top" v-if="buttonInfo.disable">
                <button class="bk-button bk-primary" type="button" disabled> {{ detail.defaultFlag ? $t('store.已安装') : $t('store.安装')}} </button>
                <template slot="content">
                    <p>{{buttonInfo.des}}</p>
                </template>
            </bk-popover>
            <button class="detail-install" @click="goToInstall" v-else> {{ $t('store.安装') }} </button>

            <section class="click-area">
                <template v-if="userInfo.type !== 'ADMIN' && detail.htmlTemplateVersion !== '1.0'">
                    <h5 :title="approveTip" :class="[{ 'not-public': approveMsg !== $t('store.协作') }, 'click-button']" @click="cooperation">
                        <icon class="detail-img mr4" name="cooperation" size="16" />
                        <span class="approve-msg">{{approveMsg}}</span>
                    </h5>
                </template>
            </section>
        </section>

        <bk-dialog v-model="showCooperDialog" :title="$t('store.申请成为协作者')" width="600" :on-close="closeDialog" @confirm="confirmDialog" :loading="dialogLoading" :close-icon="false">
            <bk-form label-width="90" ref="validateForm" :model="cooperData" v-if="showCooperDialog">
                <bk-form-item :label="$t('store.申请人')">
                    <bk-input v-model="user" :disabled="true"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('store.调试项目')" :required="true" :rules="rules" property="testProjectCode" :icon-offset="30">
                    <bk-select class="big-select"
                        v-model="cooperData.testProjectCode"
                        searchable
                        :loading="isLoading"
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
                    </bk-select>
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
    import HonerImg from '../../honer-img.vue'
    import HonerTag from '../../honer-tag.vue'
    import api from '@/api'

    export default {
        components: {
            commentRate,
            formTips,
            HonerImg,
            HonerTag
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
            detail: Object,
            currentTab: String
        },

        data () {
            return {
                showCooperDialog: false,
                user: window.userInfo.username,
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
                if (this.detail.score >= 5) {
                    return '16px'
                } else if (this.detail.score <= 0) {
                    return '0px'
                } else {
                    return '8px'
                }
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

            approveTip () {
                let res = this.approveMsg
                if (res === this.$t('store.协作')) res = this.$t('store.点击申请成为协作者')
                return res
            },

            buttonInfo () {
                const info = {}
                info.disable = this.detail.defaultFlag || !this.detail.flag
                if (this.detail.defaultFlag) info.des = `${this.$t('store.通用流水线插件，所有项目默认可用，无需安装')}`
                if (!this.detail.flag) info.des = `${this.$t('store.你没有该流水线插件的安装权限，请联系流水线插件发布者')}`
                return info
            }
        },

        watch: {
            'cooperData.testProjectCode' () {
                this.$refs.validateForm.validate().catch(() => {})
            }
        },

        mounted () {
            this.initData()
        },

        methods: {
            initData () {
                const data = {
                    storeCode: this.$route.params.code,
                    storeType: 'ATOM'
                }
                api.getMemberView(data).then((res = {}) => {
                    this.userInfo = res
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                })
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
            },

            getShowNum (num) {
                if (+num > 10000) {
                    return Math.floor(+num / 10000) + 'W+'
                } else {
                    return num
                }
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
        margin: 26px auto 0;
        width: 95vw;
        background: $white;
        box-shadow: 1px 2px 3px 0px rgba(0,0,0,0.05);
        padding: 32px;
        .detail-pic {
            width: 130px;
            height: 130px;
        }
        .atom-icon {
            height: 160px;
            width: 160px;
        }
        button {
            border-radius: 4px;
            width: 120px;
            height: 40px;
        }
    }
    .detail-install {
        background: $primaryColor;
        border: none;
        font-size: 14px;
        color: $white;
        line-height: 40px;
        text-align: center;
        &.opicity-hidden {
            opacity: 0;
            user-select: none;
        }
        &:active {
            transform: scale(.97)
        }
    }
    .detail-info-group {
        flex: 1;
        margin: 0 32px;
        max-width: calc(100% - 314px);
        .canvas-contain {
            height: 20px;
            width: 64px;
            margin: 0 12px;
            padding: 2px 2px 0;
            background: #F9F9F9;
            display: flex;
            align-items: center;
            justify-content: center;
            position: relative;
            &:after {
                content: '';
                position: absolute;
                right: -12px;
                top: 0;
                width: 1px;
                height: 100%;
                background: #eff1f5;
            }
            .chart-empty {
                color: #e2e2e2;
            }
            ::v-deep div[data-bkcharts-tooltips] {
                min-width: 140px;
            }
            &:last-child:after {
                display: none;
            }
        }
        h3 {
            font-size: 22px;
            line-height: 29px;
            color: $fontBlack;
        }
        .score-group {
            position: relative;
            .score-real {
                position: absolute;
                overflow: hidden;
                left: 0;
                top: 0;
                display: flex;
                .yellow {
                    min-width: 14px;
                }
            }
        }
        .ml4 {
            margin-left: 4px;
        }
        .rate-num {
            margin-left: 6px;
            color: $fontWeightColor;
        }
        .detail-info {
            float: left;
            display: flex;
            padding-top: 7px;
            width: 25%;
            font-size: 14px;
            font-weight: normal;
            line-height: 20px;
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
            margin-bottom: 15px;
            .not-recommend {
                text-decoration: line-through;
            }
            >h5 {
                margin-left: 12px;
                line-height: 14px;
                padding: 2px 5px;
                cursor: pointer;
                background: rgba(21, 146, 255, 0.08);
                color: #1592ff;
                span {
                    font-weight: normal;
                    font-size: 12px;
                    line-height: 14px;
                }
            }
            >span {
                font-size: 20px;
                color: $fontLightGray;
                line-height: 20px;
                font-weight: normal;
            }
            h5.nomal-title {
                cursor: auto;
                background: none;
                color: #333C48;
            }
            .num-wraper {
                height: 22px;
                background: #F5F7FA;
                padding: 0 8px;
                margin-right: 8px;
                display: flex;
                align-items: center;
                font-weight: normal;
                font-size: 14px;
                line-height: 18px;
            }
            .ml16 {
                margin-left: 16px;
            }
            .ml6 {
                margin-left: 6px;
            }
            .ml3 {
                margin-left: 3px;
            }
            .hot-icon {
                height: 18px;
                width: 18px;
            }
        }
        .detail-info.detail-label {
            width: 994px;
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
    .click-area {
        display: flex;
        align-items: center;
        color: #63656E;
        margin-top: 16px;
        .click-button {
            display: flex;
            align-items: center;
            cursor: pointer;
            font-weight: normal;
            &.not-public {
                cursor: auto;
                background: none;
                color: #9e9e9e;
            }
        }
        .mr4 {
            margin-right: 4px;
        }
    }
    .mb16 {
        margin-bottom: 16px;
    }
    .mr8 {
        margin-right: 8px;
    }
    ::v-deep .is-error .big-select {
        border: 1px solid $dangerColor;
    }
    ::v-deep .bk-dialog-body .tips-body {
        text-align: left;
    }
</style>
