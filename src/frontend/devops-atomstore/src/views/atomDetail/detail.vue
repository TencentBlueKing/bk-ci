<template>
    <div class="atom-information-wrapper">
        <div class="inner-header">
            <div class="title"> {{ $t('插件详情') }} </div>
            <span @click="goToEditAtom" :class="[{ 'disable': !showEdit }, 'header-edit']" :title="!showEdit && $t('只有处于审核驳回、已发布、上架中止和已下架的状态才允许修改基本信息')"> {{ $t('编辑') }} </span>
        </div>

        <section class="sub-view-port" v-bkloading="{ isLoading: loading }">
            <div class="atom-information-content" v-if="!loading">
                <div class="atom-form">
                    <div class="item-form item-form-left">
                        <div class="detail-form-item multi-item">
                            <div class="detail-form-item">
                                <div class="info-label"> {{ $t('名称：') }} </div>
                                <div class="info-value">{{ atomDetail.name }}</div>
                            </div>
                            <div class="detail-form-item">
                                <div class="info-label"> {{ $t('标识：') }} </div>
                                <div class="info-value">{{ atomDetail.atomCode }}</div>
                            </div>
                        </div>
                        <div class="detail-form-item multi-item">
                            <div class="detail-form-item">
                                <div class="info-label"> {{ $t('范畴：') }} </div>
                                <div class="info-value">{{ categoryMap[atomDetail.category] }}</div>
                            </div>
                            <div class="detail-form-item">
                                <div class="info-label"> {{ $t('分类：') }} </div>
                                <div class="info-value">{{ atomDetail.classifyName }}</div>
                            </div>
                        </div>
                        <div class="detail-form-item multi-item">
                            <div class="detail-form-item">
                                <div class="info-label"> {{ $t('适用Job类型') }}： </div>
                                <!-- <div class="info-value" v-if="atomDetail.os">{{ atomOs(atomDetail.os) }}</div> -->
                                <div class="info-value" v-if="atomDetail.os">{{ jobTypeMap[atomDetail.jobType] }}
                                    <span v-if="atomDetail.jobType === 'AGENT'">（
                                        <i class="bk-icon icon-linux-view" v-if="atomDetail.os.indexOf('LINUX') !== -1"></i>
                                        <i class="bk-icon icon-windows" v-if="atomDetail.os.indexOf('WINDOWS') !== -1"></i>
                                        <i class="bk-icon icon-macos" v-if="atomDetail.os.indexOf('MACOS') !== -1"></i>）
                                    </span>
                                </div>
                            </div>
                            <div class="detail-form-item is-open">
                                <label class="info-label"> {{ $t('是否开源') }}： </label>
                                <div class="info-value">{{ atomDetail.visibilityLevel | levelFilter }}</div>
                            </div>
                        </div>
                        <div class="detail-form-item multi-item">
                            <div class="detail-form-item">
                                <div class="info-label"> {{ $t('功能标签：') }} </div>
                                <div class="info-value feature-label">
                                    <div class="label-card" v-for="(label, index) in atomDetail.labels" :key="index">{{ label }}</div>
                                </div>
                            </div>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label"> {{ $t('简介：') }} </div>
                            <div class="info-value">{{ atomDetail.summary }}</div>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label"> {{ $t('详细描述：') }} </div>
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
                        <div class="toggle-btn" v-if="isOverflow" @click="toggleShow()">{{ isDropdownShow ? $t('收起') : $t('展开') }}
                            <i :class="['bk-icon icon-angle-down', { 'icon-flip': isDropdownShow }]"></i>
                        </div>
                    </div>
                    <div class="item-form item-form-right">
                        <img :src="atomDetail.logoUrl">
                    </div>
                </div>
                <div class="version-content">
                    <div class="version-info-header">
                        <div class="info-title"> {{ $t('版本列表') }} </div>
                        <button class="bk-button bk-primary"
                            type="button"
                            :disabled="upgradeStatus.indexOf(versionList[0].atomStatus) === -1"
                            @click="editAtom('upgradeAtom', versionList[0].atomId)"
                        > {{ $t('新增版本') }} </button>
                    </div>
                    <bk-table style="margin-top: 15px;" :data="versionList">
                        <bk-table-column :label="$t('版本')" prop="version"></bk-table-column>
                        <bk-table-column :label="$t('状态')" prop="atomStatus" :formatter="statusFormatter"></bk-table-column>
                        <bk-table-column :label="$t('创建人')" prop="creator"></bk-table-column>
                        <bk-table-column :label="$t('创建时间')" prop="createTime"></bk-table-column>
                        <bk-table-column :label="$t('操作')" width="120" class-name="handler-btn">
                            <template slot-scope="props">
                                <section v-show="!index">
                                    <span class="update-btn" v-if="props.row.atomStatus === 'INIT'" @click="editAtom('shelfAtom', props.row.atomId)"> {{ $t('上架') }} </span>
                                    <span class="update-btn"
                                        v-if="progressStatus.indexOf(props.row.atomStatus) > -1" @click="routerProgress(props.row.atomId)"> {{ $t('进度') }} </span>
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
    import { mapGetters } from 'vuex'

    export default {
        data () {
            return {
                showContent: false,
                isDropdownShow: false,
                isOverflow: false,
                versionList: [],
                loading: true,
                showEdit: false,
                progressStatus: ['COMMITTING', 'BUILDING', 'BUILD_FAIL', 'TESTING', 'AUDITING'],
                upgradeStatus: ['AUDIT_REJECT', 'RELEASED', 'GROUNDING_SUSPENSION'],
                atomDetail: {
                    visibilityLevel: 'LOGIN_PUBLIC'
                },
                osMap: {
                    'LINUX': 'Linux',
                    'WINDOWS': 'Windows',
                    'MACOS': 'macOS',
                    'NONE': this.$t('无构建环境')
                },
                categoryMap: {
                    'TASK': this.$t('流水线插件'),
                    'TRIGGER': this.$t('流水线触发器')
                },
                jobTypeMap: {
                    'AGENT': this.$t('编译环境'),
                    'AGENT_LESS': this.$t('无编译环境')
                }
            }
        },
        computed: {
            ...mapGetters('store', {
                'currentAtom': 'getCurrentAtom'
            }),
            atomCode () {
                return this.$route.params.atomCode
            },
            atomStatusList () {
                return atomStatusMap
            }
        },

        created () {
            this.initData()
        },

        methods: {
            initData () {
                this.getAtomDetail()
                this.requestVersionList()
            },

            statusFormatter (row, column, cellValue, index) {
                return this.atomStatusList[cellValue]
            },

            getAtomDetail () {
                Object.assign(this.atomDetail, this.currentAtom)
                this.atomDetail.labels = this.currentAtom.labelList.map(item => {
                    return item.labelName
                })
            },

            requestVersionList () {
                this.$store.dispatch('store/requestVersionList', {
                    atomCode: this.atomCode
                }).then((res) => {
                    this.versionList = res.records || []
                    const lastestVersion = this.versionList[0] || {}
                    const lastestStatus = lastestVersion.atomStatus
                    this.showEdit = ['AUDIT_REJECT', 'RELEASED', 'GROUNDING_SUSPENSION', 'UNDERCARRIAGED'].includes(lastestStatus)
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.loading = false
                    this.$nextTick(() => (this.isOverflow = this.$refs.editor.scrollHeight > 180))
                })
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

            goToEditAtom () {
                if (!this.showEdit) return
                this.$router.push({ name: 'edit' })
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

<style lang="scss" scoped>
    @import './../../assets/scss/conf';

    %flex {
        display: flex;
        justify-content: space-between;
    }
    .atom-information-wrapper {
        overflow: auto;
        .inner-header {
            @extend %flex;
            align-items: center;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .title {
                font-size: 16px;
            }
            .header-edit {
                font-size: 16px;
                color: $primaryColor;
                cursor: pointer;
            }
        }
        .atom-information-content {
            height: 100%;
            padding: 20px;
            overflow: auto;
        }
        .atom-form {
            display: flex;
            justify-content: space-between;
            .detail-form-item .markdown-editor-show.info-value {
                /deep/ .v-note-panel {
                    border: none;
                }
                /deep/ .v-show-content {
                    background: #FAFBFD;
                }
            }
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
