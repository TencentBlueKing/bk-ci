const bcsMixin = {
    props: {
        elementIndex: Number,
        containerIndex: Number,
        stageIndex: Number,
        element: Object,
        container: Object,
        stage: Object,
        atomPropsModel: Object,
        setAtomValidate: Function
    },
    components: {
    },
    methods: {
        async handleChooseOpType (name, value) {
            this.handleUpdateElement(name, value)
            this.getCategoryList()
            this.isShowInstNum()
            if (this.element.category) {
                this.getInstAppId(this.element.category)
            }
            if (value === 'create') {
                this.handleShowCreate()
            } else if (value === 'rollingupdate') {
                this.handleShowRollingupdate()
            } else if (value === 'scale') {
                this.handleShowOthers()
                this.newModel.bcsInstNum.hidden = false
            } else {
                this.handleShowOthers()
            }
        },
        handleShowCreate () {
            this.newModel.bcsAppInstId.hidden = true
            this.newModel.clusterId.hidden = false
            this.newModel.musterId.hidden = false
            this.newModel.versionId.hidden = false
            this.newModel.showInstanceEntity.hidden = false
            this.newModel.namespaceVar.hidden = false
            this.newModel.instVersionId.hidden = true
            this.newModel.instVar.hidden = true
            this.getCluster()
            this.getMuster()
            // this.getInstanceEntity()
        },
        handleShowRollingupdate () {
            this.newModel.bcsAppInstId.hidden = false
            this.newModel.clusterId.hidden = true
            this.newModel.musterId.hidden = true
            this.newModel.versionId.hidden = true
            this.newModel.showInstanceEntity.hidden = true
            this.newModel.namespaceVar.hidden = true
            this.newModel.instVersionId.hidden = false
            this.newModel.instVar.hidden = false
        },
        handleShowOthers () {
            this.newModel.bcsAppInstId.hidden = false
            this.newModel.clusterId.hidden = true
            this.newModel.musterId.hidden = true
            this.newModel.versionId.hidden = true
            this.newModel.showInstanceEntity.hidden = true
            this.newModel.namespaceVar.hidden = true
            this.newModel.instVersionId.hidden = true
            this.newModel.instVar.hidden = true
        },
        async handleChooseCategory (name, value) {
            this.handleUpdateElement(name, value)
            this.isShowInstNum()
            this.getInstAppId(value)
            this.handleUpdateElement('bcsAppInstId', '')
            this.handleUpdateElement('instVersionId', '')
            this.handleUpdateElement('instVar', [])
            this.newModel.instVersionId.list = []
        },
        handleUpdateBcsAppInstId (name, value) {
            this.handleUpdateElement(name, value)
            this.handleUpdateElement('instVersionId', '')
            this.handleUpdateElement('instVar', [])
            this.newModel.instVersionId.list = []
            this.getInstVersion(value)
        },
        handleUpdateInstVersionId (name, value) {
            this.handleUpdateElement(name, value)
            this.getInstVar(this.element.bcsAppInstId, value)
        },
        handleUpdateInstVar (name, value) {
            this.handleUpdateElement(name, value)
        },
        handleUpdateMusterId (name, value) {
            this.handleUpdateElement(name, value)
            this.handleUpdateElement('versionId', '')
            this.handleUpdateElement('showVersionId', '')
            this.handleUpdateElement('showVersionName', '')
            this.handleUpdateElement('showInstanceEntity', [])
            this.newModel.showInstanceEntity.list = []
            this.newModel.versionId.list = []
            this.getMusterVersion(value)
        },
        handleUpdateVersionId (name, value) {
            this.handleUpdateElement(name, value)
            const curVersion = this.newModel.versionId.list.find(item => item.id === value)
            this.handleUpdateElement('showVersionId', curVersion ? curVersion.show_version_id : '')
            this.handleUpdateElement('showVersionName', curVersion ? curVersion.show_version_name : '')
            this.handleUpdateElement('showInstanceEntity', [])
            this.newModel.showInstanceEntity.list = []
            this.getInstanceEntity(value)
        },
        handleUpdateInstanceEntity (name, value) {
            this.handleUpdateElement(name, value)
        },
        isShowInstNum () {
            if (this.element.opType === 'scale' || (this.element.opType === 'rollingupdate' && this.element.category !== 'Application')) {
                this.newModel.bcsInstNum.hidden = false
            } else {
                this.newModel.bcsInstNum.hidden = true
            }
        },
        async getInstAppId (category) {
            if (!category) {
                return
            }
            try {
                let res = await this.$store.dispatch('common/getBcsProjectInstance', {
                    projectId: this.curProject.projectId,
                    ccId: this.curProject.ccAppId,
                    category
                })
                if (res === null) {
                    res = []
                } else {
                    res = res.map(item => ({
                        id: item.id,
                        name: `${item.name}(${item.namespace})`
                    }))
                }
                this.newModel.bcsAppInstId.list = res
            } catch (err) {
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            }
        },
        async getInstVersion (instId) {
            if (!instId) {
                return
            }
            try {
                let res = await this.$store.dispatch('common/getBcsInstVersion', {
                    projectId: this.curProject.projectId,
                    ccId: this.curProject.ccAppId,
                    instId
                })
                if (res === null) {
                    res = []
                }
                this.newModel.instVersionId.list = res
            } catch (err) {
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            }
        },
        async getInstVar (instId, versionId) {
            if (!instId || !versionId) {
                return
            }
            try {
                const res = await this.$store.dispatch('common/getBcsInstVar', {
                    projectId: this.curProject.projectId,
                    ccId: this.curProject.ccAppId,
                    instId,
                    versionId
                })
                let varArr = []
                if (res && res.variable) {
                    varArr = res.variable
                }
                this.handleUpdateElement('instVar', varArr)
            } catch (err) {
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            }
        },
        async getCluster () {
            try {
                let res = await this.$store.dispatch('common/getBcsCluster', {
                    projectCode: this.projectId || this.curProject.englishName
                })
                if (!res) {
                    res = []
                } else {
                    res = res.results
                    res = res.filter(iitem => !iitem.disabled).map(item => ({
                        id: item.cluster_id,
                        name: item.name
                    }))
                }
                this.newModel.clusterId.list = res || []
            } catch (err) {
                console.log(err.message || err)
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            }
        },
        async getMuster () {
            try {
                let res = await this.$store.dispatch('common/getBcsMuster', {
                    projectId: this.curProject.projectId,
                    ccId: this.curProject.ccAppId
                })
                if (!res) {
                    res = []
                }
                this.newModel.musterId.list = res
            } catch (err) {
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            }
        },
        async getMusterVersion (musterId) {
            if (!musterId) {
                return
            }
            try {
                let res = await this.$store.dispatch('common/getBcsMusterVersion', {
                    projectId: this.curProject.projectId,
                    ccId: this.curProject.ccAppId,
                    musterId
                })
                if (!res) {
                    res = []
                }
                res = res.map(item => ({
                    ...item,
                    name: item.show_version_name
                }))
                this.newModel.versionId.list = res
            } catch (err) {
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            }
        },
        async getInstanceEntity (versionId) {
            if (!versionId) {
                return
            }
            try {
                const resData = await this.$store.dispatch('common/getBcsInstanceEntity', {
                    projectId: this.curProject.projectId,
                    ccId: this.curProject.ccAppId,
                    versionId
                })
                const res = {}
                const allowArr = ['DaemonSet', 'Job', 'Deployment', 'StatefulSet', 'Application']
                if (resData) {
                    for (const obj in resData) {
                        let newKey = ''
                        if (obj.startsWith('K8s') && this.curProject.kind === 1) {
                            newKey = obj.substring(3)
                        } else if (this.curProject.kind === 2) {
                            newKey = obj.substring(0, 1).toUpperCase() + obj.substring(1)
                        } else {
                            newKey = obj
                        }
                        if (allowArr.find(item => item === newKey)) {
                            Object.assign(res, { [newKey]: resData[obj] })
                        }
                    }
                }

                const enitityList = []
                for (const obj in res) {
                    const tmp = Object.assign({}, { name: obj, children: res[obj] })
                    enitityList.push(tmp)
                }
                this.newModel.showInstanceEntity.list = enitityList
                this.newModel.showInstanceEntity.tmpData = res
            } catch (err) {
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            }
        },
        getCategoryList () {
            let arr = []
            if (this.curProject.kind === 1) {
                arr = [
                    {
                        id: 'DaemonSet',
                        name: 'DaemonSet'
                    },
                    {
                        id: 'Job',
                        name: 'Job'
                    },
                    {
                        id: 'Deployment',
                        name: 'Deployment'
                    },
                    {
                        id: 'StatefulSet',
                        name: 'StatefulSet'
                    }
                ]
            } else {
                arr = [
                    {
                        id: 'Deployment',
                        name: 'Deployment'
                    }
                ]
                if (this.element.opType !== 'rollingupdate') {
                    arr.push({ id: 'Application', name: 'Application' })
                }
            }
            this.newModel.category.list = arr
        }
    }
}

export default bcsMixin
