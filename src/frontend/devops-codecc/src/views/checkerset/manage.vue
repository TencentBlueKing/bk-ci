<template>
    <div class="checkerset-manage">
        <!-- <header>
            <span class="breadcrumb-txt breadcrumb-back" @click="$router.go(-1)">规则集</span>
            <i class="bk-icon icon-angle-right fs12"></i>
            <span class="breadcrumb-txt breadcrumb-name">管理规则集</span>
        </header> -->
        <div class="checkerset-info" v-if="detailInfo.checkerSetId">
            <span class="breadcrumb-txt" v-if="!isFromSettings" @click="back"><i class="bk-icon icon-angle-left"></i>{{$t('返回')}}</span>
            <div class="col-item checkerset-name" :title="detailInfo.checkerSetName">{{detailInfo.checkerSetName}}</div>
            <div class="col-item checkerset-desc" v-if="detailInfo.description" :title="detailInfo.description">{{detailInfo.description}}</div>
            <div class="col-item code-lang">
                <span>语言</span>{{getCodeLang(detailInfo.codeLang)}}
            </div>
            <div class="col-item">
                <span class="checker-tag" v-for="(category, index) in detailInfo.catagories" :key="index">{{category.cnName}}</span>
            </div>
            <span class="codecc-icon icon-edit" @click="edit"></span>
        </div>
        <checker-list
            ref="checkerList"
            v-if="detailInfo.codeLang"
            :is-config="true"
            :has-no-permission="hasNoPermission"
            :checkerset-conf="checkersetConf"
            :handle-select-rules="handleSelectRules"
            :selected-conf="selectedRuleList"
            :save-conf="saveConf"
            :local-rule-param="localRuleParam"
            :update-conf-parame="updateConfParame"
        ></checker-list>
        <create
            :visiable.sync="sliderVisiable"
            :is-edit="true"
            :has-permission="!hasNoPermission"
            :has-detail="true"
            :edit-obj="editObj"
            :refresh-detail="getCheckersetDetail"
        ></create>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import checkerList from '../checker/list'
    import create from './create'

    export default {
        components: { checkerList, create },
        props: {
            checkersetId: {
                type: String,
                default: ''
            },
            version: {
                type: String,
                default: ''
            },
            isFromSettings: {
                type: Boolean,
                default: false
            },
            updateCheckerList: {
                type: Function
            }
        },
        data () {
            return {
                sliderVisiable: false,
                permissionList: [],
                selectedRuleList: [],
                localRuleParam: [],
                detailInfo: {},
                editObj: {}
            }
        },
        computed: {
            ...mapState([
                'toolMeta'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            isPertainProject () {
                return this.detailInfo.projectId && this.detailInfo.projectId === this.projectId
            },
            hasNoPermission () {
                return this.detailInfo.checkerSetId
                    && ((this.detailInfo.legacy && this.detailInfo.codeLangList.length > 1) // 单语言可编辑
                    || !this.isPertainProject
                    || !this.permissionList.some(item => ['CREATOR', 'MANAGER'].includes(item)))
            },
            checkersetConf () {
                if (this.detailInfo.checkerSetId) {
                    const conf = {
                        codeLang: this.detailInfo.codeLangList,
                        checkerSetId: this.detailInfo.checkerSetId,
                        version: this.detailInfo.version,
                        legacy: this.detailInfo.legacy,
                        codeLangList: this.detailInfo.codeLangList,
                        toolList: this.detailInfo.toolList
                    }
                    return conf
                }
                return {}
            }
        },
        mounted () {
            this.checkPermission()
            this.getCheckersetDetail()
        },
        methods: {
            async getCheckersetDetail () {
                const checkersetId = this.checkersetId || this.$route.params.checkersetId
                const version = this.version || this.$route.params.version
                const params = { checkersetId, version }
                const res = await this.$store.dispatch('checkerset/detail', params)
                this.detailInfo = res
                this.selectedRuleList = this.detailInfo.checkerProps ? this.detailInfo.checkerProps : []
                if (this.selectedRuleList.length) {
                    this.detailInfo.checkerProps.forEach(item => {
                        let temp = {}
                        if (item.props) {
                            temp = { ...item, propValue: JSON.parse(item.props)[0].propValue }
                            this.localRuleParam.push(temp)
                        }
                    })
                }
            },
            async checkPermission () {
                const params = {
                    projectId: this.projectId,
                    user: this.$store.state.user.username,
                    checkerSetId: this.checkersetId || this.$route.params.checkersetId
                }
                const res = await this.$store.dispatch('checkerset/permission', params)
                this.permissionList = res.data
            },
            back () {
                this.$router.push({ name: 'checkerset-list', params: { projectId: this.projectId } })
            },
            edit () {
                const catagories = this.detailInfo.catagories.map(category => category.enName)
                this.editObj = { ...this.detailInfo, catagories }
                this.sliderVisiable = true
            },
            getCodeLang (codeLang) {
                const names = this.toolMeta.LANG.map(lang => {
                    if (lang.key & codeLang) {
                        return lang.name
                    }
                }).filter(name => name)
                return names.join('、')
            },
            updateConfParame (list) {
                this.localRuleParam = list
            },
            handleSelectRules (data, isChecked, isBatch) {
                if (isBatch) {
                    const validArr = data.filter(val => val)
                    if (isChecked) {
                        validArr.forEach(val => {
                            this.selectedRuleList.push(val)
                        })
                        const obj = {}
                        this.selectedRuleList = this.selectedRuleList.reduce((cur, next) => {
                            if (!obj[`${next.checkerKey}-${next.toolName}`]) {
                                obj[`${next.checkerKey}-${next.toolName}`] = true
                                cur.push(next)
                            }
                            return cur
                        }, [])
                    } else validArr.forEach(val => this.selectedRuleList.splice(this.selectedRuleList.findIndex(item => item.checkerKey === val.checkerKey && item.toolName === val.toolName), 1))
                } else {
                    if (isChecked) {
                        this.selectedRuleList.push(data)
                    } else this.selectedRuleList.splice(this.selectedRuleList.findIndex(item => item.checkerKey === data.checkerKey && item.toolName === data.toolName), 1)
                }
            },
            async submit () {
                const version = this.version || this.$route.params.version
                const checkersetId = this.checkersetId || this.$route.params.checkersetId
                const params = { checkersetId, version }
                const checkerProps = this.selectedRuleList.map(checker => {
                    const temp = {
                        toolName: checker.toolName,
                        checkerKey: checker.checkerKey
                    }
                    const matchItem = this.localRuleParam.findIndex(item => item.checkerKey === checker.checkerKey)
                    if (matchItem > -1) {
                        temp.props = this.localRuleParam[matchItem].props
                    } else {
                        temp.props = checker.props
                    }
                    return temp
                })
                Object.assign(params, { checkerProps: checkerProps })
                await this.$store.dispatch('checkerset/save', params).then(res => {
                    if (res.code === '0') {
                        const nextVersion = this.detailInfo.initCheckers || this.detailInfo.legacy ? this.detailInfo.version + 1 : this.detailInfo.version
                        this.$bkMessage({ theme: 'success', message: `规则配置已保存至V${nextVersion}版本` })
                        // this.getCheckersetDetail()
                        if (this.isFromSettings) {
                            this.updateCheckerList()
                        } else if (this.detailInfo.initCheckers) {
                            const link = {
                                name: 'checkerset-manage',
                                params: {
                                    projectId: this.projectId,
                                    checkersetId: this.detailInfo.checkerSetId,
                                    version: this.detailInfo.version + 1
                                }
                            }
                            this.$router.push(link)
                        } else {
                            this.getCheckersetDetail()
                        }
                    }
                })
            },
            saveConf () {
                if (this.selectedRuleList.length && !this.hasNoPermission) {
                    const self = this
                    const nextVersion = this.detailInfo.initCheckers || this.detailInfo.legacy ? this.detailInfo.version + 1 : this.detailInfo.version
                    this.$bkInfo({
                        title: `确认`,
                        subTitle: `是否保存规则配置，并创建新的规则集版本V${nextVersion}？`,
                        maskClose: true,
                        confirmFn (name) {
                            self.submit()
                        }
                    })
                }
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .checkerset-manage {
        padding: 0 40px;
        header {
            margin-bottom: 10px;
            padding-bottom: 8px;
            border-bottom: 1px solid #dcdee5;
            font-size: 14px;
            .breadcrumb-back {
                color: #3a84ff;
                cursor: pointer;
            }
        }
        .breadcrumb-txt {
            margin-right: 20px;
            color: #3a84ff;
            cursor: pointer;
            .bk-icon {
                position: relative;
                top: 1px;
                font-size: 20px;
                font-weight: 600;
            }
        }
        .checkerset-info {
            display: flex;
            align-items: center;
            font-size: 14px;
            color: #63656d;
        }
        .checkerset-name {
            margin-right: 14px;
            font-size: 16px;
            color: #333;
            max-width: 196px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .checkerset-desc,
        .code-lang {
            margin-right: 16px;
            padding-right: 16px;
            border-right: 1px solid #dcdee5;
        }
        .checkerset-desc {
            max-width: 196px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .checker-tag {
            background: #c9dffa;
            margin-left: 4px;
            font-size: 12px;
            border-radius: 2px;
            padding: 2px 8px;
            display: inline-block;
        }
        .icon-edit {
            margin-left: 32px;
            font-size: 16px;
            cursor: pointer;
            &:hover {
                color: #3a84ff;
            }
        }
        .disable-edit {
            color: #dcdee5;
            &:hover {
                color: #dcdee5;
            }
        }
        .cc-checkers {
            padding: 12px 0;
            height: calc(100% - 50px);
        }
    }
</style>
