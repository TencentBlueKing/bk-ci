<template>
    <div class="publish-dept">
        <section class="publish-dept-row">
            <label>
                {{ $t('store.归属信息') }}
            </label>
          
            <form
                class="dept-editing-form"
                v-if="editing"
            >
                <div class="bk-dropdown-box">
                    <bk-cascade
                        :list="orgTree"
                        v-model="orgValue"
                        clearable
                        is-remote
                        :scroll-width="300"
                        :check-any-level="true"
                        change-immediately
                        :placeholder="this.$t('store.请选择部门')"
                        :remote-method="getDepartment"
                        @change="handleChangeDept"
                    />
                </div>
                <div class="bk-dropdown-box">
                    <bk-select
                        v-model="centerId"
                        :placeholder="this.$t('store.请选择中心')"
                        :loading="centerLoading"
                        searchable
                        clearable
                        id-key="id"
                        @change="handleChangeCenter"
                    >
                        <bk-option
                            v-for="center in centerList"
                            :key="center.id"
                            :id="center.id"
                            :name="center.name"
                        />
                    </bk-select>
                </div>
                <bk-button
                    text
                    @click="handleDeptInfoChange"
                    theme="primary"
                >
                    {{ $t('confirm') }}
                </bk-button>
                <bk-button
                    text
                    @click="cancelEditing"
                >
                    {{ $t('cancel') }}
                </bk-button>
            </form>
            <p
                v-else
                class="publish-dept-detail"
                v-bkloading="{ isLoading: orgLoading, size: 'mini' }"
            >
                <span>{{ deptInfo.deptPath || '' }}</span>
                <span class="ml6">{{ deptInfo.centerName || '' }}</span>
                <i
                    class="bk-icon icon-edit2"
                    @click="editDeptInfo"
                />
            </p>
        </section>
    </div>
</template>

<script>
    import { mapActions } from 'vuex'

    export default {

        data () {
            return {
                editing: false,
                orgValue: [],
                orgTree: [],
                centerList: [],
                deptMap: new Map(),
                centerId: '',
                centerLoading: false,
                orgLoading: false,
                initOrgValue: [],
                initCenterId: ''
            }
        },
        computed: {
            deptInfo () {
                const deptPath = this.orgValue
                    .map(id => this.deptMap.get(id)?.name)
                    .filter(Boolean)
                    .join(' / ')
                const centerName = this.centerList.find(center => center.id === this.centerId)?.name
                return { deptPath, centerName }
            }
        },

        async mounted () {
            await this.getOrgList()
            this.requestList()
        },

        methods: {
            ...mapActions('store', ['requestOrganizations', 'getDeptCodes', 'updateDeptInfo']),
            async getOrgList () {
                try {
                    const res = await this.requestOrganizations({
                        type: 'dept',
                        id: 0,
                        excludeBelowTheDept: true
                    })
                    res.forEach(item => {
                        if (this.deptMap.has(item.id)) return
                        this.deptMap.set(item.id, item)
                    })
                    this.orgTree = res
                } catch (error) {
                    console.log(error)
                }
            },
            async getDepartment (item, resolve) {
                try {
                    if (item.isLoading === false) {
                        resolve(item)
                    } else {
                        this.$set(item, 'isLoading', true)
                        const res = await this.requestOrganizations({
                            type: ['businessLine', 'bg'].includes(item.type) ? 'dept' : item.type,
                            id: item.id,
                            ...(['businessLine', 'dept', 'bg'].includes(item.type) ? { excludeBelowTheDept: true } : {})
                        })
                        res.forEach(i => {
                            if (this.deptMap.has(i.id)) return
                            this.deptMap.set(i.id, i)

                            i.leaf && (i.isLoading = !i.leaf)
                        })
                        this.$set(item, 'children', res)
                        this.$set(item, 'isLoading', false)
                        resolve(item)
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            async requestList () {
                const { code: storeCode, type } = this.$route.params
                try {
                    this.orgLoading = true
                    const typeMap = {
                        atom: 'ATOM'
                    }
                    const res = await this.getDeptCodes({
                        storeCode,
                        storeType: typeMap[type]
                    })
                    const info = res.storeDeptInfo
                
                    const tempOrgValue = []
                    if (info.bgId) tempOrgValue.push(info.bgId)
                    if (info.businessLineId) tempOrgValue.push(info.businessLineId)
                    if (info.deptId) tempOrgValue.push(info.deptId)

                    if (tempOrgValue.length > 0) {
                        await this.loadCascadeNodes(tempOrgValue)
                        const lastNodeId = tempOrgValue[tempOrgValue.length - 1]
                        await this.fetchCenterList(lastNodeId)
                        this.centerId = info.centerId
                    }
                    this.$nextTick(() => {
                        this.orgValue = tempOrgValue
                    })
                } catch (error) {
                    console.log(error)
                } finally {
                    this.orgLoading = false
                }
            },
            async loadCascadeNodes (pathIds) {
                if (pathIds.length < 2) return
    
                for (let i = 0; i < pathIds.length - 1; i++) {
                    const id = pathIds[i]

                    const currentNode = i === 0
                        ? this.orgTree.find(node => node.id === id)
                        : this.deptMap.get(id)

                    if (currentNode && (!currentNode.children || currentNode.children.length === 0)) {
                        await new Promise(resolve => {
                            this.getDepartment(currentNode, resolve)
                        })
                        this.deptMap.set(id, currentNode)
                    }
                }
            },

            async fetchCenterList (deptId) {
                this.centerId = ''
                if (deptId) {
                    this.centerLoading = true
                    try {
                        const res = await this.requestOrganizations({
                            type: 'center',
                            id: deptId
                        })
                        this.centerList = res
                    } catch (err) {
                        this.$bkMessage({
                            message: err.message || err,
                            theme: 'error'
                        })
                    } finally {
                        this.centerLoading = false
                    }
                }
            },
            editDeptInfo () {
                this.editing = true
                this.initOrgValue = [...this.orgValue]
                this.initCenterId = this.centerId
            },
            handleChangeDept (valueList) {
                this.orgValue = valueList
                this.fetchCenterList(valueList[valueList.length - 1])
            },
            handleChangeCenter (value) {
                this.centerId = value
            },
            cancelEditing () {
                this.editing = false
                this.orgValue = [...this.initOrgValue]
                this.centerId = this.initCenterId
            },
            async handleDeptInfoChange () {
                if (this.orgValue.length === 0) {
                    this.$bkMessage({
                        message: this.$t('store.请选择归属部门'),
                        theme: 'error'
                    })
                    return
                }
                try {
                    const params = {
                        storeCode: this.$route.params.code,
                        storeType: 'ATOM',
                        storeDeptInfo: {
                            bgId: this.orgValue[0],
                            bgName: this.deptMap.get(this.orgValue[0])?.name,
                            centerId: this.centerId,
                            centerName: this.centerList.find(center => center.id === this.centerId)?.name
                        }
                    }
                    if (this.orgValue.length > 1) {
                        this.orgValue.slice(1).forEach(id => {
                            const node = this.deptMap.get(id)
                            if (!node) return
                            
                            if (node.type === 'businessLine') {
                                params.storeDeptInfo.businessLineId = id
                                params.storeDeptInfo.businessLineName = node.name
                            } else if (node.type === 'dept') {
                                params.storeDeptInfo.deptId = id
                                params.storeDeptInfo.deptName = node.name
                            }
                        })
                    }

                    const res = await this.updateDeptInfo(params)
                    if (res) {
                        this.$bkMessage({
                            message: this.$t('store.操作成功'),
                            theme: 'success'
                        })
                        this.editing = false
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            }
        }
    }
</script>

<style lang="scss" scope>
.publish-dept {
    height: 80px;
    display: flex;
    background-color: white;
    padding: 32px 24px;
    .publish-dept-row {
        width: 100%;
        height: 32px;
        line-height: 32px;
        display: flex;
        grid-gap: 8px;
        font-size: 12px;
        align-items: center;
        > label {
            color: #979BA5;
        }

        .dept-editing-form {
            display: flex;
            align-items: center;
            grid-gap: 8px;
            .publish-dept-select {
                width: 360px;
            }
        }

        .publish-dept-detail {
            display: flex;
            flex: 1;
            align-items: center;
            grid-gap: 6px;
            line-height: 32px;
            .bk-icon.icon-edit2{
                color: #979ba5;
                font-size: 24px;
                cursor: pointer;
                &:hover {
                    color: #3a84ff;
                }
            }
        }
    }
    .bk-dropdown-box {
        width: 260px;
    }
}
</style>
