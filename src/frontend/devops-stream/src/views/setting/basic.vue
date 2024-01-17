<template>
    <article class="setting-basic-home" v-bkloading="{ isLoading }">
        <h3 class="setting-basic-head">{{$t('setting.general')}}</h3>
        <section class="basic-main">
            <h5 class="main-title">{{$t('setting.configListeningEvents')}}</h5>
            <section class="main-checkbox">
                <bk-checkbox v-model="form.buildPushedBranches" class="basic-item">{{$t('setting.buildPushedBranches')}}</bk-checkbox>
                <bk-checkbox v-model="form.buildPushedPullRequest" class="basic-item">{{$t('setting.buildPushedMergeRequest')}}</bk-checkbox>
            </section>

            <h5 class="main-title">{{$t('setting.configMergeRequest')}}</h5>
            <section class="main-checkbox">
                <bk-checkbox v-model="form.enableMrBlock" class="basic-item">{{$t('setting.lockMrMerge')}}</bk-checkbox>
            </section>
            <bk-button theme="primary" class="basic-btn" @click="saveSetting" :loading="isSaving">{{$t('save')}}</bk-button>
        </section>

        <h3 class="setting-basic-head">{{$t('setting.ciAuthorization')}}</h3>
        <section class="basic-main">
            <h5 class="main-title">{{ $t('setting.authBy', [form.enableUserId])}}</h5>
            <section class="main-checkbox">
                <bk-button @click="resetAuthorization" :loading="isReseting">{{$t('setting.resetAuthorization')}}</bk-button>
            </section>
            <h5 class="main-title">{{ form.enableCi ? $t('setting.disableTips') : $t('setting.enableTips') }}</h5>
            <section class="main-checkbox">
                <bk-button :theme="form.enableCi ? 'danger' : 'primary'" :loading="isToggleEnable" @click="toggleEnable">{{ form.enableCi ? $t('setting.disableCi') : $t('setting.enableCi') }}</bk-button>
            </section>
        </section>

        <h3 class="setting-basic-head">{{$t('setting.mrRunPerm')}}</h3>
        <section class="basic-main">
            <section class="form-item">
                <bk-checkbox v-model="triggerSetting.memberNoNeedApproving" class="basic-item">{{$t('setting.mrNoApproval')}}</bk-checkbox>
                <p class="desc desc-padding">{{$t('setting.mrNoApprovalTips')}}</p>
            </section>
            <section class="form-item">
                <p>{{$t('setting.whiteList')}}</p>
                <bk-input
                    :placeholder="$t('setting.whiteListPlaceholder')"
                    :type="'textarea'"
                    :rows="3"
                    :maxlength="255"
                    v-model="triggerSetting.whitelistStr">
                </bk-input>
                <p class="desc">{{$t('setting.whiteListTips')}}</p>
            </section>
            <section class="main-checkbox">
                <bk-button theme="primary" :loading="isSavingTrigginSetting" @click="saveTriggerSetting">{{ $t('save') }}</bk-button>
            </section>
        </section>
        <h3 class="setting-basic-head">{{$t('setting.organization')}}</h3>
        <section class="basic-main">
            <section class="form-item">
                <p>{{$t('setting.projectOrg')}}</p>
                <div class="project-org-select-area">
                    <bk-cascade
                        v-if="orgs.length > 0"
                        is-remote
                        v-model="projectOrg.dept"
                        :scroll-width="240"
                        :remote-method="loadDepartMents"
                        :list="orgs"
                        @change="loadCenters"
                    >
                    </bk-cascade>
                    <bk-select
                        v-model="projectOrg.centerId"
                    >
                        <bk-option
                            v-for="item in centers"
                            :key="item.id"
                            :id="item.id"
                            :value="item.id"
                            :name="item.name"
                        />
                    </bk-select>
                </div>
            </section>
            <section class="form-item">
                <p>{{$t('setting.projectProduct')}}</p>
                <bk-select
                    searchable
                    v-model="projectOrg.productId"
                    enable-virtual-scroll
                    :list="products"
                >
                </bk-select>
            </section>
            <section class="main-checkbox">
                <bk-button
                    theme="primary"
                    :loading="isSavingProjectOrg"
                    @click="saveProjectOrg">
                    {{ $t('save') }}
                </bk-button>
            </section>
        </section>
    </article>
</template>

<script>
    import { setting } from '@/http'
    import { mapActions, mapState } from 'vuex'

    export default {
        data () {
            return {
                form: {
                    buildPushedBranches: false,
                    buildPushedPullRequest: false,
                    enableMrBlock: false
                },
                triggerSetting: {
                    memberNoNeedApproving: true,
                    whitelistStr: ''
                },
                projectOrg: {
                    dept: [],
                    centerId: '',
                    productId: ''
                },
                deptMap: {},
                orgs: [],
                centers: [],
                products: [],
                isSaving: false,
                isLoading: false,
                isToggleEnable: false,
                isSavingTrigginSetting: false,
                isSavingProjectOrg: false,
                isReseting: false
            }
        },

        computed: {
            ...mapState(['projectId', 'projectInfo'])
        },

        created () {
            this.getSetting()
        },

        methods: {
            ...mapActions(['setProjectSetting']),
            async getSetting () {
                try {
                    this.isLoading = true
                    const [res, projectInfo, products] = await Promise.all([
                        setting.getSetting(this.projectId),
                        setting.getProjectInfo(this.projectId),
                        setting.getProducts()
                    ])
                    const dept = ['bgId', 'businessLineId', 'deptId'].map(key => projectInfo[key]).filter(item => item)
                    this.initOrgs(dept)
                    
                    this.projectOrg = {
                        dept,
                        centerId: projectInfo.centerId,
                        productId: projectInfo.productId
                    }
                    console.log(dept, this.projectOrg)
                    this.products = products.map(item => ({
                        id: item.ProductId,
                        name: item.ProductName
                    }))
                    Object.assign(this.form, res)
                    this.triggerSetting = {
                        memberNoNeedApproving: res.triggerReviewSetting?.memberNoNeedApproving !== undefined ? res.triggerReviewSetting?.memberNoNeedApproving : true,
                        whitelistStr: (res.triggerReviewSetting?.whitelist || []).join(',') || ''
                    }
                    this.setProjectSetting(res)
                } catch (err) {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                } finally {
                    this.isLoading = false
                }
            },

            async initOrgs (dept) {
                const depts = await Promise.all(['0', ...dept].map((id, index) => {
                    const type = index === dept.length ? 'center' : 'dept'
                    return setting.getDepartmentList(type, id)
                }))
                // 去掉最后的center
                this.centers = depts.pop()
                this.deptMap = depts.flat().reduce((prev, cur) => {
                    prev[cur.id] = cur
                    if (cur.type === 'dept') {
                        prev[cur.id].isLoading = false
                    }
                    if (cur.parentId && prev[cur.parentId]) {
                        prev[cur.parentId].children = prev[cur.parentId].children || []
                        prev[cur.parentId].children.push(cur)
                    }
                    return prev
                }, {})
                this.orgs = Object.values(this.deptMap).filter(item => item.parentId === '0')
            },

            saveSetting () {
                this.isSaving = true
                setting.saveSetting(this.projectId, this.form).then(() => {
                    this.$bkMessage({ theme: 'success', message: 'Saved successfully' })
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isSaving = false
                })
            },

            saveTriggerSetting () {
                this.isSavingTrigginSetting = true
                const whitelist = this.triggerSetting.whitelistStr.trim().split(',').map(item => item.trim())
                const data = {
                    memberNoNeedApproving: this.triggerSetting.memberNoNeedApproving,
                    whitelist
                }
                setting.saveTriggerSetting(this.projectId, data).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isSavingTrigginSetting = false
                })
            },

            toggleEnable () {
                this.isToggleEnable = true
                setting.toggleEnableCi(!this.form.enableCi, this.projectInfo).then(() => {
                    this.getSetting()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isToggleEnable = false
                })
            },

            resetAuthorization () {
                this.isReseting = true
                setting.resetAuthorization(this.projectInfo.id).then(() => {
                    this.getSetting()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isReseting = false
                })
            },
            async loadDepartMents (item, resolve) {
                try {
                    if (item.type === 'dept' || item.isLoading === false) {
                        resolve(item)
                        return
                    }
                    const res = await setting.getDepartmentList('dept', item.id)
                    item.children = res.map(item => {
                        if (item.type === 'dept') {
                            item.isLoading = false
                        }
                        if (!this.deptMap[item.id]) {
                            this.deptMap[item.id] = item
                        }
                        return item
                    })
                    resolve(item)
                } catch (err) {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }
            },
            async loadCenters (dept) {
                try {
                    this.projectOrg.centerId = ''
                    
                    const last = dept[dept.length - 1]
                    const res = await setting.getDepartmentList('center', last)
                    this.centers = res
                } catch (err) {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }
            },
            async saveProjectOrg () {
                this.isSavingProjectOrg = true
                try {
                    const { dept, productId, centerId } = this.projectOrg
                    
                    await setting.saveProjectInfo(this.projectId, {
                        ...this.generateProjectOrgParam(dept),
                        centerId,
                        centerName: this.centers.find(item => item.id === this.projectOrg.centerId)?.name ?? ''
                    }, {
                        productId,
                        productName: this.products.find(item => item.id === this.projectOrg.productId)?.name ?? ''
                    })
                    this.$bkMessage({ theme: 'success', message: 'Saved successfully' })
                } catch (err) {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                } finally {
                    this.isSavingProjectOrg = false
                }
            },
            generateProjectOrgParam () {
                try {
                    const { dept } = this.projectOrg
                    const deptParam = dept.reduce((acc, id) => {
                        const item = this.deptMap[id]
                        Object.assign(acc, {
                            [`${item.type}Name`]: item.name,
                            [`${item.type}Id`]: id
                        })
                        return acc
                    }, {
                        businessLineId: '',
                        businessLineName: ''
                    })
                    return deptParam
                } catch (error) {
                    console.error(error)
                    return {}
                }
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .setting-basic-home {
        margin: 16px;
        padding: 24px;
        background: #fff;
        overflow: auto;
    }
    .setting-basic-head {
        font-size: 16px;
        color: #313328;
    }
    .basic-main {
        margin: 10px 0 30px;
        border: 1px solid #f0f1f5;
        padding: 20px;
        .main-title {
            margin-bottom: 20px;
        }
        .main-checkbox {
            display: flex;
            margin-bottom: 20px;
            &:last-child {
                margin-bottom: 0;
            }
        }
        .form-item {
            margin-bottom: 20px;
            &:last-child {
                margin-bottom: 0;
            }
            p {
                line-height: 24px;
            }
            .desc {
                color: #979BA5;
                font-size: 12px;
            }
            .desc-padding {
                padding-left: 22px;
            }
            .project-org-select-area {
                display: grid;
                grid-gap: 12px;
                grid-auto-flow: column;
                grid-auto-columns: 1fr 240px;
            }
        }
        .basic-item {
            margin-right: 100px;
        }
    }
    .basic-btn {
        width: 88px;
    }
</style>
