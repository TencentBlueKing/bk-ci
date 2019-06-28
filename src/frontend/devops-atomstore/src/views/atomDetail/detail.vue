<template>
    <div class="atom-information-wrapper">
        <div class="inner-header">
            <div class="title">插件详情</div>
        </div>

        <section
            class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <div class="atom-information-content" v-if="showContent">
                <div class="atom-form">
                    <div class="item-form item-form-left">
                        <div class="detail-form-item multi-item">
                            <div class="detail-form-item">
                                <div class="info-label">名称：</div>
                                <div class="info-value">{{ atomDetail.name }}</div>
                            </div>
                            <div class="detail-form-item">
                                <div class="info-label">标识：</div>
                                <div class="info-value">{{ atomDetail.atomCode }}</div>
                            </div>
                        </div>
                        <div class="detail-form-item multi-item">
                            <div class="detail-form-item">
                                <div class="info-label">范畴：</div>
                                <div class="info-value">{{ categoryMap[atomDetail.category] }}</div>
                            </div>
                            <div class="detail-form-item">
                                <div class="info-label">分类：</div>
                                <div class="info-value">{{ atomDetail.classifyName }}</div>
                            </div>
                        </div>
                        <div class="detail-form-item multi-item">
                            <div class="detail-form-item">
                                <div class="info-label">操作系统：</div>
                                <!-- <div class="info-value" v-if="atomDetail.os">{{ atomOs(atomDetail.os) }}</div> -->
                                <div class="info-value" v-if="atomDetail.os">
                                    <span v-if="atomDetail.jobType === 'AGENT'">
                                        <i class="bk-icon icon-linux-view" v-if="atomDetail.os.indexOf('LINUX') !== -1"></i>
                                        <i class="bk-icon icon-windows" v-if="atomDetail.os.indexOf('WINDOWS') !== -1"></i>
                                        <i class="bk-icon icon-macos" v-if="atomDetail.os.indexOf('MACOS') !== -1"></i>
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label">发布包：</div>
                            <div class="info-value">{{ atomDetail.pkgName }}</div>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label">功能标签：</div>
                            <div class="info-value feature-label">
                                <div class="label-card" v-for="(label, index) in atomDetail.labels" :key="index">{{ label }}</div>
                            </div>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label">简介：</div>
                            <div class="info-value">{{ atomDetail.summary }}</div>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label">详细描述：</div>
                            <div class="info-value markdown-editor-show" ref="editor" :class="{ 'overflow': !isDropdownShow }">
                                <mavon-editor
                                    :editable="false"
                                    default-open="preview"
                                    :subfield="false"
                                    :toolbars-flag="false"
                                    :external-link="false"
                                    :box-shadow="false"
                                    v-model="atomDetail.description"
                                />
                            </div>
                        </div>
                        <div class="toggle-btn" v-if="isOverflow" @click="toggleShow()">{{ isDropdownShow ? '收起' : '展开' }}
                            <i :class="['bk-icon icon-angle-down', { 'icon-flip': isDropdownShow }]"></i>
                        </div>
                    </div>
                    <div class="item-form item-form-right">
                        <img :src="atomDetail.logoUrl" v-if="atomDetail.logoUrl">
                        <i class="bk-icon icon-placeholder atom-logo" v-else></i>
                    </div>
                </div>
                <div class="version-content">
                    <div class="version-info-header">
                        <div class="info-title">版本列表</div>
                        <button class="bk-button bk-primary"
                            type="button"
                            :disabled="upgradeStatus.indexOf(versionList[0].atomStatus) === -1"
                            @click="editAtom('upgradeAtom', versionList[0].atomId)"
                        >新增版本</button>
                    </div>
                    <bk-table style="margin-top: 15px;"
                        :data="versionList"
                    >
                        <bk-table-column label="版本" prop="version"></bk-table-column>
                        <bk-table-column label="状态" prop="atomStatus" :formatter="statusFormatter"></bk-table-column>
                        <bk-table-column label="创建人" prop="creator"></bk-table-column>
                        <bk-table-column label="创建时间" prop="createTime"></bk-table-column>
                        <bk-table-column label="操作" width="120" class-name="handler-btn">
                            <template slot-scope="props">
                                <section v-show="!index">
                                    <span class="update-btn" v-if="props.row.atomStatus === 'INIT'" @click="editAtom('shelfAtom', props.row.atomId)">上架</span>
                                    <span class="update-btn"
                                        v-if="progressStatus.indexOf(props.row.atomStatus) > -1" @click="routerProgress(props.row.atomId)">进度</span>
                                </section>
                            </template>
                        </bk-table-column>
                    </bk-table>
                </div>
            </div>
        </section>
    </div>
</template>

<script>
    import { atomStatusMap } from '@/store/constants'
    import mavonEditor from 'mavon-editor'
    import 'mavon-editor/dist/css/index.css'

    const Vue = window.Vue
    Vue.use(mavonEditor)

    export default {
        filters: {
            levelFilter (val) {
                if (val === 'LOGIN_PUBLIC') return '是'
                else return '否'
            }
        },
        data () {
            return {
                showContent: false,
                isDropdownShow: false,
                isOverflow: false,
                versionList: [],
                progressStatus: ['COMMITTING', 'BUILDING', 'BUILD_FAIL', 'TESTING', 'AUDITING'],
                upgradeStatus: ['AUDIT_REJECT', 'RELEASED', 'GROUNDING_SUSPENSION'],
                atomDetail: {
                    visibilityLevel: 'LOGIN_PUBLIC'
                },
                osMap: {
                    'LINUX': 'Linux',
                    'WINDOWS': 'Windows',
                    'MACOS': 'macOS',
                    'NONE': '无构建环境'
                },
                categoryMap: {
                    'TASK': '流水线插件',
                    'TRIGGER': '流水线触发器'
                },
                jobTypeMap: {
                    'AGENT': '编译环境',
                    'AGENT_LESS': '无编译环境'
                },
                loading: {
                    isLoading: false,
                    title: ''
                }
            }
        },
        computed: {
            atomCode () {
                return this.$route.params.atomCode
            },
            atomStatusList () {
                return atomStatusMap
            }
        },

        async created () {
            await this.requestVersionList()
            await this.requestAtomDetail()
        },
        methods: {
            statusFormatter (row, column, cellValue, index) {
                return this.atomStatusList[cellValue]
            },

            async requestAtomDetail () {
                this.loading.isLoading = true
                this.loading.title = '数据加载中，请稍候'

                try {
                    const res = await this.$store.dispatch('store/requestAtom', {
                        atomCode: this.atomCode
                    })

                    Object.assign(this.atomDetail, res)
                    this.atomDetail.labels = res.labelList.map(item => {
                        return item.labelName
                    })
                    this.$store.dispatch('store/updateCurrentaAtom', { res })
                    this.$nextTick(() => {
                        setTimeout(() => {
                            this.isOverflow = this.$refs.editor.scrollHeight > 180
                        }, 1000)
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
                    this.showContent = true
                }
            },
            async requestVersionList () {
                try {
                    const res = await this.$store.dispatch('store/requestVersionList', {
                        atomCode: this.atomCode
                    })
                    
                    this.versionList.splice(0, this.versionList.length)
                    res.records.map(item => {
                        this.versionList.push(item)
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            routerProgress (id) {
                this.$router.push({
                    name: 'releaseProgress',
                    params: {
                        releaseType: 'upgrade',
                        atomId: id
                    }
                })
            },
            editAtom (routerName, id) {
                this.$router.push({
                    name: routerName,
                    params: {
                        atomId: id
                    }
                })
            },
            atomOs (os) {
                const target = []
                os.forEach(item => {
                    target.push(this.osMap[item])
                })
                return target.join('，')
            },
            toggleShow () {
                this.isDropdownShow = !this.isDropdownShow
            }
        }
    }
</script>

<style lang="scss">
    @import './../../assets/scss/conf';

    %flex {
        display: flex;
        justify-content: space-between;
    }
    .atom-information-wrapper {
        overflow: auto;
        .inner-header {
            @extend %flex;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .title {
                font-size: 16px;
            }
        }
        .atom-information-content {
            height: 100%;
            padding: 20px;
            padding-right: 26px;
            overflow: auto;
        }
        .atom-form {
            display: flex;
            justify-content: space-between;
        }
        .item-form-left {
            min-width: 840px;
        }
        .item-form-right {
            margin-left: 20px;
            width: 100px;
            height: 100px;
            img {
                position: relative;
                width: 100px;
                height: 100px;
                z-index: 99;
                object-fit: cover;
            }
             .atom-logo {
                font-size: 100px;
                display: block;
                transform: scale(1.2, 1.2);
                color: #C3CDD7;
            }
        }
        .detail-form-item {
            display: flex;
            margin-top: 18px;
            width: 100%;
        }
        .is-open {
            height: 21px;
            pointer-events: none;
            .bk-form-radio {
                padding: 0;
            }
        }
        .info-label {
            width: 100px;
            min-width: 100px;
            text-align: right;
        }
        .info-value {
            margin-left: 16px;
            line-height: 1.5;
            color: #333C48;
        }
        .label-card {
            float: left;
            margin-bottom: 4px;
            margin-right: 4PX;
            padding: 2px 7px;
            font-size: 12px;
            border: 1px solid $borderWeightColor;
            background-color: #F0F1F3;
            color: $fontColor;
        }
        .multi-item {
            margin-top: 0;
        }
        .editor-item {
            margin-bottom: 10px;
        }
        .overflow {
            max-height: 180px;
            overflow: hidden;
        }
        .toggle-btn {
            margin-left: 117px;
            font-size: 12px;
            color: $primaryColor;
            text-align: right;
            cursor: pointer;
            .bk-icon {
                display: inline-block;
                margin-left: 2px;
                transition: all ease 0.2s;
                &.icon-flip {
                    transform: rotate(180deg);
                }
            }
        }
        .version-info-header {
            @extend %flex;
            margin-top: 36px;
            .info-title {
                font-weight: bold;
                line-height: 2.5;
            }
        }
        .version-table {
            margin-top: 10px;
            border: 1px solid $borderWeightColor;
            tbody {
                background-color: #fff;
            }
            th {
                height: 42px;
                padding: 2px 10px;
                color: #333C48;
                font-weight: normal;
                &:first-child {
                    padding-left: 20px;
                }
            }
            td {
                height: 42px;
                padding: 2px 10px;
                &:first-child {
                    padding-left: 20px;
                }
                &:last-child {
                    padding-right: 30px;
                }
            }
            .handler-btn {
                span {
                    display: inline-block;
                    margin-right: 20px;
                    color: $primaryColor;
                    cursor: pointer;
                }
            }
            .no-data-row {
                padding: 40px;
                color: $fontColor;
                text-align: center;
            }
        }
    }
</style>
