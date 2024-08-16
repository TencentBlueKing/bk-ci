<template>
    <main class="code-check-main" v-bkloading="{ isLoading }">
        <template v-if="!apiErr">
            <component :is="status"
                :codecc-url="codeScore.codeccUrl"
                :commit-id="codeScore.commitId"
                :repo-url="codeScore.repoUrl"
                :start-checking="startChecking"
                :code-style-qualified-score="codeScore.codeStyleQualifiedScore"
                :code-security-qualified-score="codeScore.codeSecurityQualifiedScore"
                :code-measure-qualified-score="codeScore.codeMeasureQualifiedScore"
                :message="message"
                :last-analysis-time="codeScore.lastAnalysisTime"
                @startCodeCC="startCodeCC"
            ></component>
            <template v-if="['success', 'unqualified'].includes(status)">
                <section class="code-check-detail">
                    <h3 class="detail-title score">
                        {{ $t('store.评分') }}<a class="score-rule"><a class="g-title-work" :href="rateCalcDocUrl" target="_blank">{{ $t('store.计算公式') }}</a><icon name="tiaozhuan" :size="12" class="score-icon"></icon></a>
                    </h3>
                    <ul class="score-list float-left">
                        <li v-for="scoreItem in scoreList" :key="scoreItem.name" class="score-detail">
                            <p class="score-circle">
                                <span class="circle-main"></span>
                                <span class="sector-group">
                                    <span class="circle-sector"
                                        v-for="(item, index) in getColorList(scoreItem.score)"
                                        :key="item.color"
                                        :style="{
                                            color: item.color,
                                            transform: `${stratTransition ? `rotate(${item.deg}deg)` : ''}`,
                                            transition: `transform 5.555ms linear ${5.555 * index}ms`,
                                            zIndex: 36 - index
                                        }">
                                    </span>
                                </span>
                            </p>
                            <p class="score-rate">
                                <span class="score-num"><bk-animate-number :value="scoreItem.score" :digits="2"></bk-animate-number></span>
                                <span class="score-title">{{ scoreItem.name }}</span>
                            </p>
                        </li>
                    </ul>
                </section>
                <section class="code-check-detail problem-detail">
                    <h3 class="detail-title">{{ $t('store.总览') }}</h3>
                    <section class="float-left problem-list">
                        <a v-for="analysisResult in codeScore.lastAnalysisResultList" :key="analysisResult.toolName" class="problem-item" :href="analysisResult.defectUrl" target="_blank">
                            <p class="problem-desc">
                                <span class="english-name">{{ analysisResult.displayName }}</span>
                                <span class="problem-name" :style="{ color: getToolColor(analysisResult.toolName) }">{{ analysisResult.type }}</span>
                            </p>
                            <p class="problem-num">
                                <span class="num">{{ analysisResult.defectCount }}</span>
                                <span class="unit">{{ getToolUnit(analysisResult.toolName) }}</span>
                            </p>
                        </a>
                        <bk-exception class="exception-wrap-item exception-part" type="empty" scene="part" v-if="!codeScore.lastAnalysisResultList || codeScore.lastAnalysisResultList.length <= 0">
                            {{ $t('store.未发现代码质量问题') }}
                        </bk-exception>
                    </section>
                </section>
            </template>
        </template>
        <bk-exception class="exception-wrap-item" type="500" v-else>
            <span>{{apiErrMessage}}</span>
        </bk-exception>
    </main>
</template>

<script>
    import { mapGetters } from 'vuex'
    import api from '../../api'
    import fail from './fail'
    import uncheck from './uncheck'
    import success from './success'
    import doing from './doing'
    import unqualified from './unqualified'

    const statusMap = {
        0: 'success',
        1: 'fail',
        2: 'uncheck',
        3: 'doing'
    }

    export default {
        components: {
            fail,
            uncheck,
            success,
            doing,
            unqualified
        },

        props: {
            code: String,
            type: String,
            id: String
        },

        data () {
            return {
                status: '',
                message: '',
                stratTransition: false,
                codeScore: {},
                isLoading: false,
                startChecking: false,
                statusData: {},
                scoreList: [],
                apiErr: false,
                apiErrMessage: ''
            }
        },

        computed: {
            ...mapGetters('store', {
                detail: 'getDetail'
            }),
            rateCalcDocUrl () {
                return `${IWIKI_DOCS_URL}/x/kvMMBw`
            },
            storeType () {
                const typeMap = {
                    atom: 'ATOM',
                    image: 'IMAGE',
                    service: 'SERVICE'
                }
                const type = this.$route.params.type
                return typeMap[type] || this.type
            },

            storeCode () {
                const keyMap = {
                    atom: 'atomCode',
                    image: 'imageCode',
                    service: 'serviceCode'
                }
                const type = this.$route.params.type
                const key = keyMap[type]
                return this.detail[key] || this.code
            },

            storeId () {
                const keyMap = {
                    atom: 'atomId',
                    image: 'imageId',
                    service: 'serviceId'
                }
                const type = this.$route.params.type
                const key = keyMap[type]
                return this.detail[key] || this.id
            }
        },

        mounted () {
            this.isLoading = true
            this.getCodeScore().finally(() => {
                this.isLoading = false
                setTimeout(() => {
                    this.stratTransition = true
                }, 10)
            })
        },

        methods: {
            startCodeCC () {
                this.startChecking = true
                const params = [this.storeType, this.storeCode]
                if (this.$route.name !== 'check') params.push(this.storeId)
                return api.startCodecc(...params).then((res) => {
                    this.$bkMessage({ message: this.$t('store.启动插件扫描成功'), theme: 'success' })
                    this.$emit('startCodeCC')
                    return this.getCodeScore(res)
                }).catch((err) => {
                    this.apiErr = true
                    this.apiErrMessage = err.message
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.startChecking = false
                })
            },

            getCodeScore (buildId) {
                if (!this.storeType || !this.storeCode) return
                const params = [this.storeType, this.storeCode]
                if (this.$route.name !== 'check') params.push({ storeId: this.storeId })
                else params.push({ buildId })
                return api.getCodeScore(...params).then((res = {}) => {
                    this.codeScore = res || {}
                    this.message = res.message || ''
                    this.scoreList = [
                        { name: this.$t('store.代码安全'), score: this.codeScore.codeSecurityScore },
                        { name: this.$t('store.代码规范'), score: this.codeScore.codeStyleScore },
                        { name: this.$t('store.代码度量'), score: this.codeScore.codeMeasureScore }
                    ]
                    // 设置当前扫描状态
                    this.status = statusMap[this.codeScore.status]
                    if (!res.qualifiedFlag && this.status === 'success') this.status = 'unqualified'
                    // 如果执行中，则轮询状态
                    if (this.status === 'doing') {
                        setTimeout(() => {
                            this.getCodeScore(buildId)
                        }, 30000)
                    }
                }).catch((err) => {
                    this.apiErr = true
                    this.apiErrMessage = err.message
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                })
            },

            getToolColor (toolName) {
                const toolColorMap = {
                    CLOC: '#979ba5',
                    'WOODPECKER-SENSITIVE': '#ea3636'
                }

                return toolColorMap[toolName] || '#ff9c01'
            },

            getToolUnit (toolName) {
                const toolUnitMap = {
                    CLOC: this.$t('store.行')
                }

                return toolUnitMap[toolName] || this.$t('store.个')
            },

            getColorList (score) {
                function getRgb (score) {
                    const colorMap = {
                        max: {
                            start: { r: 66, g: 214, b: 179 },
                            end: { r: 171, g: 249, b: 176 }
                        },
                        success: {
                            start: { r: 247, g: 107, b: 28 },
                            end: { r: 250, g: 217, b: 97 }
                        },
                        fail: {
                            start: { r: 234, g: 54, b: 54 },
                            end: { r: 253, g: 156, b: 156 }
                        }
                    }

                    if (score >= 100) return colorMap.max
                    if (score >= 90 && score < 100) return colorMap.success
                    if (score < 90) return colorMap.fail
                }

                function getColor (curScore) {
                    const rate = curScore / score
                    const rgb = []
                    const { start = {}, end = {} } = getRgb(+score) || {};
                    ['r', 'g', 'b'].forEach((key) => {
                        const colorNum = (end[key] - start[key]) * rate + start[key]
                        rgb.push(colorNum)
                    })
                    return `rgb(${rgb.join(', ')})`
                }

                let curScore = 0
                const colorList = []
                while (curScore < score) {
                    const color = getColor(curScore)
                    const dis = (curScore + 2.777) > score ? score - curScore : 2.777
                    curScore += dis
                    const deg = (curScore - 2.777) * 3.6
                    colorList.push({ color, deg })
                }
                return colorList
            }
        }
    }
</script>

<style lang="scss" scoped>
    ::v-deep .bk-exception-text {
        margin-top: -50px;
    }
    .code-check-main {
        height: 100%;
        padding-bottom: 20px;
        overflow-y: auto;
    }
    .code-check-detail {
        padding: 40px 3.2vh 0;
        .detail-title {
            font-size: 14px;
            line-height: 20px;
            color: #313238;
        }
        .score {
            margin-bottom: 26px;
        }
        .score-rule {
            font-weight: normal;
            font-size: 12px;
            color: #3a84ff;
            margin-left: 10px;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
        }
        .score-icon {
            margin-left: 5.8px;
        }
    }
    .float-left {
        &::after {
            content: '';
            display: table;
            clear: both;
        }
        > * {
            float: left;
        }
    }
    .score-list {
        .score-detail {
            height: 60px;
            display: flex;
            margin-right: 60px;
            &:last-child {
                margin: 0;
            }
            .score-circle {
                position: relative;
                width: 60px;
                height: 60px;
                border-radius: 50%;
                overflow: hidden;
                background: #f0f1f5;
                margin-right: 20px;
                .circle-main {
                    position: absolute;
                    top: 20px;
                    left: 20px;
                    width: 20px;
                    height: 20px;
                    border-radius: 50%;
                    background: #fff;
                    z-index: 55;
                }
                .sector-group {
                    width: 100%;
                    height: 100%;
                    position: absolute;
                    left: 0;
                    top: 0;
                }
                .circle-sector {
                    width: 100%;
                    height: 100%;
                    position: absolute;
                    clip: rect(30px 60px 60px 0px);
                    overflow: hidden;
                }
                .circle-sector:after {
                    content: '';
                    width: 100%;
                    height: 100%;
                    background: currentColor;
                    position: absolute;
                    clip: rect(0 60px 30px 0);
                    transform: rotate(12deg);
                    border-radius: 50%;
                }
            }
            .score-rate {
                display: flex;
                flex-direction: column;
                justify-content: center;
                align-items: center;
                width: 116px;
                .score-num {
                    line-height: 45px;
                    font-size: 32px;
                    color: #313238;
                }
                .score-title {
                    font-size: 12px;
                    color: #63656e;
                    line-height: 17px;
                }
            }
        }
    }
    .problem-item {
        box-sizing: border-box;
        width: 25%;
        height: 80px;
        border: 1px solid #dcdee5;
        border-right: none;
        padding: 19px 15px 18px 30px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        cursor: pointer;
        margin-top: 16px;
        .problem-desc {
            display: flex;
            align-items: center;
            flex-direction: column;
            .english-name {
                line-height: 22px;
                font-size: 16px;
                color: #63656e;
                margin-bottom: 4px;
            }
            .problem-name {
                font-size: 12px;
                line-height: 17px;
                color: #979ba5;
            }
        }
        .problem-num {
            .num {
                font-size: 32px;
                line-height: 45px;
                color: #313238;
            }
            .unit {
                font-size: 12px;
                line-height: 17px;
                color: #979ba5;
            }
        }
        &:last-child, &:nth-child(4n) {
            border-right: 1px solid #dcdee5;
        }
        &:hover {
            border: 1px solid #3a84ff;
            border-right: none;
            position: relative;
            &::after {
                content: '';
                position: absolute;
                right: -1px;
                top: -1px;
                background: #3a84ff;
                height: 80px;
                width: 1px;
            }
            &:last-child, &:nth-child(4n) {
                border-right: 1px solid #3a84ff;
            }
            .english-name, .num, .unit {
                color: #3a84ff;
            }
        }
    }
    ::v-deep .exception-wrap-item {
        margin-top: 16px;
        .exception-image {
            object-fit: none;
        }
    }
    ::v-deep .code-check-tip {
        margin: 30px 3.2vh 0;
        background: #f0f8ff;
        border: 1px solid #c5daff;
        border-radius: 2px;
        line-height: 30px;
        color: #63656e;
        font-size: 12px;
        .icon-info-circle {
            margin: 0 9px 0 11px;
            color: #419bf9;
            font-size: 14px;
        }
    }
    ::v-deep .code-ckeck-status {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        padding: 3.2vh;
        border-bottom: 1px solid #f0f1f5;
        .status-icon {
            font-size: 46px;
            margin-right: 21px;
        }
        .code-check-summary {
            flex: 1;
            color: #63656e;
            font-size: 12px;
            line-height: 17px;
            .summary-head {
                color: #313238;
                line-height: 28px;
                font-size: 20px;
                font-weight: normal;
            }
            .summary-desc {
                display: inline-block;
                margin: 3px 0 5px;
                font-weight: normal;
            }
            .summary-link {
                opacity: 0.9;
                font-weight: normal;
                line-height: 16px;
            }
            .link-txt {
                color: #3a84ff;
                cursor: pointer;
                margin: 0 8px;
            }
        }
        .code-check-button {
            margin-top: 10px;
        }
    }
</style>
