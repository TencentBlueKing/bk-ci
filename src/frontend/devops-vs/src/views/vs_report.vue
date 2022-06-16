<template>
    <div class="vs-report-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
        <content-header>
            <div slot="left" class="title">
                <i class="devops-icon icon-arrows-left" @click="toVsList"></i>
                <span>查看报告</span>
                <span v-if="reportDetail.id">
                    <span>{{ reportDetail.fileName }}</span>&nbsp;<span>（{{ reportDetail.version }}）</span>
                </span>
            </div>
            <p slot="right">本服务由金刚团队（企业微信：KingKong）提供后台支持</p>
        </content-header>

        <section class="sub-view-port" v-if="showContent">
            <template>
                <div class="report-main-container">
                    <div class="report-box" v-if="reportDetail.id">
                        <div class="report-header">
                            金刚漏洞扫描报告
                            <!-- <bk-button type="primary" class="download-btn">下载报告</bk-button> -->
                        </div>
                        <div class="report-info">
                            <div class="item-card">
                                <div class="card-title">
                                    <i class="devops-icon icon-vs-result"></i><span>报告结论</span>
                                </div>
                                <div class="card-content">
                                    <div class="item-info-inner">
                                        <label class="info-label">威胁等级：</label>
                                        <div class="info-value">
                                            <span class="level-icon" :class="{ 'highlight-icon': reportResult.conclusion.risk_level > 0 }"></span>
                                            <span class="level-icon" :class="{ 'highlight-icon': reportResult.conclusion.risk_level >= 2 }"></span>
                                            <span class="level-icon" :class="{ 'highlight-icon': reportResult.conclusion.risk_level >= 3 }"></span>
                                            <span class="level-icon" :class="{ 'highlight-icon': reportResult.conclusion.risk_level >= 4 }"></span>
                                            <span class="level-icon" :class="{ 'highlight-icon': reportResult.conclusion.risk_level >= 5 }"></span>
                                        </div>
                                    </div>
                                    <div class="item-info-inner vulnerability-desc">
                                        <label class="info-label">漏洞概述：</label>
                                        <div class="info-value">{{ reportResult.conclusion.result }}</div>
                                    </div>
                                </div>
                            </div>
                            <div class="item-card">
                                <div class="card-title">
                                    <i class="devops-icon icon-txt"></i><span>基本信息</span>
                                </div>
                                <div class="card-content">
                                    <div class="item-info-inner">
                                        <label class="info-label">文件名：</label>
                                        <div class="info-value">{{ reportResult.app_info.filename }}</div>
                                    </div>
                                    <div class="item-info-inner">
                                        <label class="info-label">版本号：</label>
                                        <div class="info-value">{{ reportResult.app_info.version }}</div>
                                    </div>
                                    <div class="item-info-inner">
                                        <label class="info-label">上传时间：</label>
                                        <div class="info-value">{{ reportResult.app_info.uploadtime }}</div>
                                    </div>
                                    <div class="item-info-inner">
                                        <label class="info-label">审计耗时：</label>
                                        <div class="info-value">{{ reportResult.app_info.usetime }}</div>
                                    </div>
                                    <div class="item-info-inner">
                                        <label class="info-label">上传人：</label>
                                        <div class="info-value">{{ reportDetail.responseuser }}</div>
                                    </div>
                                </div>
                            </div>
                            <div class="item-card">
                                <div class="card-title">
                                    <i class="devops-icon icon-exclamation-triangle"></i><span>漏洞详情</span>
                                </div>
                                <div class="card-content hole-content">
                                    <div class="steps-item" v-for="(entry, index) in vulnerabilityDetail" :key="index">
                                        <div class="steps-item-header">
                                            <div class="header-info">
                                                <i class="devops-icon icon-plus-square" v-if="!entry.isDisplayMsg"
                                                    @click="toggleDisplay(entry, 'vulnerability')"></i>
                                                <i class="devops-icon icon-minus-square" v-else
                                                    @click="toggleDisplay(entry, 'vulnerability')"></i>
                                                <label @click="toggleDisplay(entry, 'vulnerability')">{{ entry.name }}</label>
                                            </div>
                                            <div class="marking-tips">
                                                <span class="marking-icon danger-icon" v-if="entry.result.indexOf('风险') === 1"></span>
                                                <span class="marking-icon risky-icon" v-if="!entry.result.indexOf('发现')"></span>
                                                <span class="marking-icon safe-icon" v-if="!entry.result.indexOf('安全')"></span>
                                                <span class="marking-text">{{ entry.result }}</span>
                                            </div>
                                        </div>
                                        <div class="steps-item-wrapper" v-if="entry.isDisplayMsg">
                                            <div class="item-info-inner">
                                                <label class="info-label">详细信息：</label>
                                                <div class="info-value">{{ entry.item.detail }}</div>
                                            </div>
                                            <div class="item-info-inner">
                                                <label class="info-label">修复建议：</label>
                                                <div class="info-value">{{ entry.item.suggest }}</div>
                                            </div>
                                            <div class="item-info-inner">
                                                <label class="info-label">知识直达区：</label>
                                                <div class="info-value">无</div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="item-card">
                                <div class="card-title">
                                    <i class="devops-icon icon-risk"></i><span>风险详情</span>
                                </div>
                                <div class="card-content hole-content">
                                    <div class="steps-item" v-for="(entry, index) in riskDetails" :key="index">
                                        <div class="steps-item-header">
                                            <div class="header-info">
                                                <i class="devops-icon icon-plus-square" v-if="!entry.isDisplayMsg"
                                                    @click="toggleDisplay(entry, 'detail')"></i>
                                                <i class="devops-icon icon-minus-square" v-else
                                                    @click="toggleDisplay(entry , 'detail')"></i>
                                                <label @click="toggleDisplay(entry , 'detail')">{{ entry.name }}</label>
                                            </div>
                                            <div class="marking-tips">
                                                <span class="marking-icon danger-icon" v-if="entry.result.indexOf('风险') === 1"></span>
                                                <span class="marking-icon risky-icon" v-if="!entry.result.indexOf('发现')"></span>
                                                <span class="marking-icon safe-icon" v-if="!entry.result.indexOf('安全')"></span>
                                                <span class="marking-text">{{ entry.result }}</span>
                                            </div>
                                        </div>
                                        <div class="steps-item-wrapper" v-if="entry.isDisplayMsg">
                                            <div class="item-info-inner">
                                                <label class="info-label">详细信息：</label>
                                                <div class="info-value">{{ entry.item.detail }}</div>
                                            </div>
                                            <div class="item-info-inner">
                                                <label class="info-label">修复建议：</label>
                                                <div class="info-value">{{ entry.item.suggest }}</div>
                                            </div>
                                            <div class="item-info-inner">
                                                <label class="info-label">知识直达区：</label>
                                                <div class="info-value">无</div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="item-card">
                                <div class="card-title">
                                    <i class="devops-icon icon-safety"></i><span>安全提示</span>
                                </div>
                                <div class="card-content hole-content">
                                    <div class="steps-item" v-for="(entry, index) in riskWarn" :key="index">
                                        <div class="steps-item-header">
                                            <div class="header-info">
                                                <i class="devops-icon icon-plus-square" v-if="!entry.isDisplayMsg"
                                                    @click="toggleDisplay(entry, 'tips')"></i>
                                                <i class="devops-icon icon-minus-square" v-else
                                                    @click="toggleDisplay(entry, 'tips')"></i>
                                                <label @click="toggleDisplay(entry, 'tips')">{{ entry.name }}</label>
                                            </div>
                                            <div class="marking-tips">
                                                <span class="marking-icon danger-icon" v-if="entry.result.indexOf('风险') === 1"></span>
                                                <span class="marking-icon risky-icon" v-if="!entry.result.indexOf('发现')"></span>
                                                <span class="marking-icon safe-icon" v-if="!entry.result.indexOf('安全')"></span>
                                                <span class="marking-text">{{ entry.result }}</span>
                                            </div>
                                        </div>
                                        <div class="steps-item-wrapper" v-if="entry.isDisplayMsg">
                                            <div class="item-info-inner">
                                                <label class="info-label">详细信息：</label>
                                                <div class="info-value">{{ entry.item.detail }}</div>
                                            </div>
                                            <div class="item-info-inner">
                                                <label class="info-label">修复建议：</label>
                                                <div class="info-value">{{ entry.item.suggest }}</div>
                                            </div>
                                            <div class="item-info-inner">
                                                <label class="info-label">知识直达区：</label>
                                                <div class="info-value">无</div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="no-report-tips" v-else>
                        <img src="../images/500.png">
                        <p>暂无扫描报告</p>
                    </div>
                </div>
            </template>
        </section>
    </div>
</template>

<script>
    export default {
        data () {
            return {
                showContent: false,
                reportResult: {},
                reportDetail: {
                    fileName: '',
                    version: '',
                    result: ''
                },
                loading: {
                    isLoading: false,
                    title: ''
                },
                vulnerabilityDetail: [],
                riskDetails: [],
                riskWarn: []
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            recordId () {
                return this.$route.params.vsId
            }
        },
        watch: {
            projectId (val) {
                this.$router.push({
                    name: 'vsList',
                    params: {
                        projectId: this.projectId
                    }
                })
            }
        },
        async mounted () {
            await this.init()
        },
        methods: {
            async init () {
                const {
                    loading
                } = this

                loading.isLoading = true
                loading.title = '数据加载中，请稍候'

                try {
                    await this.requestVsDetail()
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    setTimeout(() => {
                        loading.isLoading = false
                    }, 1000)
                }
            },
            /**
             * 获取扫描报告
             */
            async requestVsDetail () {
                const {
                    projectId,
                    recordId
                } = this

                try {
                    const res = await this.$store.dispatch('vs/requestVsReport', {
                        projectId,
                        recordId
                    })
                    if (res) {
                        const result = res.result.leakcheck_result

                        this.reportDetail = Object.assign({}, res)
                        this.reportResult = Object.assign({}, res.result.leakcheck_result)
                        this.vulnerabilityDetail = result.leak_info.group
                        this.riskDetails = result.risk_info.group
                        this.riskWarn = result.notice_info.group
                        this.vulnerabilityDetail.map(item => (item.isDisplayMsg = false))
                        this.riskDetails.map(item => (item.isDisplayMsg = false))
                        this.riskWarn.map(item => (item.isDisplayMsg = false))
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })

                    this.loading.isLoading = false
                }

                this.showContent = true
            },
            toggleDisplay (entry, type) {
                if (type === 'vulnerability') {
                    this.vulnerabilityDetail.forEach(item => {
                        if (item.name === entry.name) item.isDisplayMsg = !item.isDisplayMsg
                    })
                    this.vulnerabilityDetail = this.vulnerabilityDetail.concat([])
                } else if (type === 'detail') {
                    this.riskDetails.forEach(item => {
                        if (item.name === entry.name) item.isDisplayMsg = !item.isDisplayMsg
                    })
                    this.riskDetails = this.riskDetails.concat([])
                } else if (type === 'tips') {
                    this.riskWarn.forEach(item => {
                        if (item.name === entry.name) item.isDisplayMsg = !item.isDisplayMsg
                    })
                    this.riskWarn = this.riskWarn.concat([])
                }
            },
            toVsList () {
                this.$router.push({
                    name: 'vsList',
                    params: {
                        projectId: this.projectId
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import './../scss/conf';

    .vs-report-wrapper {
        height: 100%;
        overflow: auto;

        .title {
            font-size: 16px;
            .icon-arrows-left {
                margin-right: 4px;
                cursor: pointer;
                color: $iconPrimaryColor;
                font-size: 16px;
                font-weight: 600;
            }
        }
        .report-main-container {
            padding: 20px;
        }

        .report-box {
            margin: auto;
            width: 88%;
            min-width: 880px;
        }

        .report-header {
            height: 50px;
            line-height: 50px;
            text-align: center;
            background-color: $primaryColor;
            font-size: 18px;
            color: #fff;
        }

        .download-btn {
            float: right;
            position: relative;
            right: 20px;
            top: 10px;
            height: 32px;
            line-height: 30px;
            border-color: #fff;
        }

        .report-info {
            padding-bottom: 20px;
            border: 1px solid #E0E6EC;
            background-color: #fff;
        }

        .item-card {
            margin: 20px 20px 0;
            border: 1px solid $borderWeightColor;
        }

        .card-title {
            padding-left: 20px;
            height: 42px;
            line-height: 42px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: $bgHoverColor;

            span {
                margin-left: 8px;
                color: $fontWeightColor;
                font-size: 16px;
                font-weight: bold;
            }

            i {
                position: relative;
                top: 4px;
                color: $primaryColor;
                font-size: 22px;
            }
        }

        .card-content {
            padding: 14px 0 24px;
        }

        .hole-content {
            padding: 14px 0 0;
        }

        .item-info-inner {
            display: flex;
            margin: 20px 48px 0;
            color: #666;

            .info-label {
                display: inline-block;
                text-align: right;
                min-width: 74px;
                font-weight: bold;
            }

            .info-value {
                display: inline-block;
                margin-left: 14px;
                word-break: break-all;
            }

            .level-icon {
                display: inline-block;
                margin-right: 2px;
                width: 10px;
                height: 10px;
                background-color: #C7C8C8;
            }

            .highlight-icon {
                background-color: #F72239;
            }
        }

        .steps-item {
            margin: 0 20px;
            border-bottom: 1px solid $borderWeightColor;

            &:last-child {
                border: none;
            }
        }

        .steps-item-header {
            padding: 16px 36px;

            label {
                margin-left: 8px;
                color: #666;
                font-weight: bold;
            }

            .header-info {
                display: inline-block;
                width: 88%;

                &:hover {
                    color: #0082FF;
                    cursor: pointer;

                    label {
                        color: #0082FF;
                        cursor: pointer;
                    }
                }
            }
        }

        .icon-plus-square,
        .icon-minus-square {
            cursor: pointer;
        }

        .marking-tips {
            display: inline-block;
        }

        .marking-icon {
            display: inline-block;
            width: 10px;
            height: 10px;
        }

        .marking-text {
            margin-left: 6px;
            color: #666;
        }

        .danger-icon {
            background-color: #F72239;
        }

        .risky-icon {
            background-color: #FF9600;
        }

        .safe-icon {
            background-color: #00C873;
        }

        .steps-item-wrapper {
            padding: 0 80px 30px 10px;

            .item-info-inner {
                margin: 4px 20px 0 52px;

                &:first-child {
                    margin-top: 8px;
                }
            }

            .info-label {
                line-height: 32px;
                text-align: left;
                color: #666;
            }

            .info-value {
                margin-left: 4px;
                line-height: 32px;
            }
        }

        .no-report-tips {
            margin-top: 180px;
            text-align: center;

            p {
                margin-top: 12px;
                font-size: 16px;
            }
        }
    }
</style>
