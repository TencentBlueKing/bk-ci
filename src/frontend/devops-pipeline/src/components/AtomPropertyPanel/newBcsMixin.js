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
            if (this.element.category && this.element.namespace) {
                this.getInstAppName(this.element.namespace)
            }
            if (value === 'create') {
                this.handleShowCreate()
            } else if (value === 'signal') {
                this.handleShowSignal()
            } else if (value === 'rollingupdate') {
                this.handleShowRollingupdate()
            } else if (value === 'command') {
                this.handleShowCommand()
            } else if (value === 'scale') {
                this.handleShowOthers()
                this.newModel.bcsInstNum.hidden = false
            } else {
                this.handleShowOthers()
            }
        },
        handleShowCreate () {
            this.newModel.namespace.hidden = true
            this.newModel.bcsAppInstName.hidden = true
            this.newModel.clusterId.hidden = false
            this.newModel.musterName.hidden = false
            this.newModel.showVersionName.hidden = false
            this.newModel.templateName.hidden = false
            this.newModel.namespaceVar.hidden = false
            this.newModel.instVersionName.hidden = true
            this.newModel.instVar.hidden = true
            this.newModel.processName.hidden = true
            this.newModel.signal.hidden = true

            this.newModel.command.hidden = true
            this.newModel.commandParam.hidden = true
            this.newModel.username.hidden = true
            this.newModel.workDir.hidden = true
            this.newModel.privileged.hidden = true
            this.newModel.reserveTime.hidden = true
            this.newModel.env.hidden = true

            this.getCluster()
            this.getMuster()
        },
        handleShowRollingupdate () {
            this.newModel.namespace.hidden = false
            this.newModel.bcsAppInstName.hidden = false
            this.newModel.clusterId.hidden = true
            this.newModel.musterName.hidden = true
            this.newModel.showVersionName.hidden = true
            this.newModel.templateName.hidden = true
            this.newModel.namespaceVar.hidden = true
            this.newModel.instVersionName.hidden = false
            this.newModel.instVar.hidden = false
            this.newModel.processName.hidden = true
            this.newModel.signal.hidden = true

            this.newModel.command.hidden = true
            this.newModel.commandParam.hidden = true
            this.newModel.username.hidden = true
            this.newModel.workDir.hidden = true
            this.newModel.privileged.hidden = true
            this.newModel.reserveTime.hidden = true
            this.newModel.env.hidden = true
        },
        handleShowSignal () {
            this.newModel.namespace.hidden = false
            this.newModel.bcsAppInstName.hidden = false
            this.newModel.clusterId.hidden = true
            this.newModel.musterName.hidden = true
            this.newModel.showVersionName.hidden = true
            this.newModel.templateName.hidden = true
            this.newModel.namespaceVar.hidden = true
            this.newModel.instVersionName.hidden = true
            this.newModel.instVar.hidden = true
            this.newModel.processName.hidden = false
            this.newModel.signal.hidden = false

            this.newModel.command.hidden = true
            this.newModel.commandParam.hidden = true
            this.newModel.username.hidden = true
            this.newModel.workDir.hidden = true
            this.newModel.privileged.hidden = true
            this.newModel.reserveTime.hidden = true
            this.newModel.env.hidden = true
        },
        handleShowCommand () {
            this.newModel.namespace.hidden = false
            this.newModel.bcsAppInstName.hidden = false
            this.newModel.clusterId.hidden = true
            this.newModel.musterName.hidden = true
            this.newModel.showVersionName.hidden = true
            this.newModel.templateName.hidden = true
            this.newModel.namespaceVar.hidden = true
            this.newModel.instVersionName.hidden = true
            this.newModel.instVar.hidden = true
            this.newModel.processName.hidden = true
            this.newModel.signal.hidden = true

            this.newModel.command.hidden = false
            this.newModel.commandParam.hidden = false
            this.newModel.username.hidden = false
            this.newModel.workDir.hidden = false
            this.newModel.privileged.hidden = false
            this.newModel.reserveTime.hidden = false
            this.newModel.env.hidden = false
        },
        handleShowOthers () {
            this.newModel.namespace.hidden = false
            this.newModel.bcsAppInstName.hidden = false
            this.newModel.clusterId.hidden = true
            this.newModel.musterName.hidden = true
            this.newModel.showVersionName.hidden = true
            this.newModel.templateName.hidden = true
            this.newModel.namespaceVar.hidden = true
            this.newModel.instVersionName.hidden = true
            this.newModel.instVar.hidden = true
            this.newModel.processName.hidden = true
            this.newModel.signal.hidden = true

            this.newModel.command.hidden = true
            this.newModel.commandParam.hidden = true
            this.newModel.username.hidden = true
            this.newModel.workDir.hidden = true
            this.newModel.privileged.hidden = true
            this.newModel.reserveTime.hidden = true
            this.newModel.env.hidden = true
        },
        async handleChooseCategory (name, value) {
            this.handleUpdateElement(name, value)
            this.isShowInstNum()
            this.handleUpdateElement('bcsAppInstName', '')
            this.handleUpdateElement('instVersionName', '')
            this.handleUpdateElement('instVar', [])
            this.handleUpdateElement('templateName', '')
            this.newModel.templateName.list = []
            this.newModel.instVersionName.list = []
            this.getInstAppName(this.element.namespace)
            this.getTemplateName(this.element.showVersionName)
        },
        handleUpdateBcsAppInstName (name, value, isUpdate) {
            this.handleUpdateElement(name, value)
            if (isUpdate) {
                const isOption = this.newModel.bcsAppInstName.list.find(item => item.name === value)
                if (isOption) {
                    this.getInstVersion(isOption.id)
                }
                this.handleUpdateElement('instVersionName', '')
                this.handleUpdateElement('instVar', [])
                this.newModel.instVersionName.list = []
            }
        },
        handleUpdateNamespace (name, value, isUpdate) {
            if (isUpdate) {
                const isOption = this.newModel.namespace.list.find(item => item.name === value)
                if (isOption) {
                    this.getInstAppName(value)
                }
                this.handleUpdateElement('bcsAppInstName', '')
                this.handleUpdateElement('instVersionName', '')
                this.handleUpdateElement('instVar', [])
                this.newModel.bcsAppInstName.list = []
            }
            this.handleUpdateElement(name, value)
        },
        handleUpdateInstVersionName (name, value, isUpdate) {
            if (isUpdate) {
                const isOption = this.newModel.instVersionName.list.find(item => item.name === value)
                if (isOption) {
                    this.getInstVar(this.element.bcsAppInstName, isOption.id)
                }
                this.handleUpdateElement('instVar', [])
            }
            this.handleUpdateElement(name, value)
        },
        handleUpdateInstVar (name, value) {
            this.handleUpdateElement(name, value)
        },
        handleUpdateMusterName (name, value, isUpdate) {
            this.handleUpdateElement(name, value)
            if (isUpdate) {
                const isOption = this.newModel.musterName.list.find(item => item.name === value)
                if (isOption) {
                    this.getMusterVersion(isOption.id)
                }
                this.handleUpdateElement('showVersionName', '')
                this.handleUpdateElement('templateName', '')
                this.newModel.showVersionName.list = []
                this.newModel.templateName.list = []
            }
        },
        handleUpdateVersionName (name, value, isUpdate) {
            this.handleUpdateElement(name, value)
            if (isUpdate) {
                const curVersion = this.newModel.showVersionName.list.find(item => item.name === value)
                if (curVersion) {
                    this.getTemplateName(value)
                }
                this.handleUpdateElement('templateName', '')
                this.newModel.templateName.list = []
            }
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
        async getNamespace () {
            try {
                let res = await this.$store.dispatch('common/getBcsNamespaces', {
                    projectId: this.curProject.project_id,
                    ccId: this.curProject.cc_app_id
                })
                if (!res) {
                    res = []
                }
                this.newModel.namespace.list = res
            } catch (err) {
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            }
        },
        async getInstAppName (namespace) {
            if (!this.element.category || !namespace) {
                return
            }
            const isOption = this.newModel.namespace.list.find(item => item.name === namespace)
            if (!isOption) {
                return
            }
            try {
                let res = await this.$store.dispatch('common/getBcsProjectInstance', {
                    projectId: this.curProject.project_id,
                    ccId: this.curProject.cc_app_id,
                    category: this.element.category,
                    namespace
                })
                if (!res === null) {
                    res = []
                }
                this.newModel.bcsAppInstName.list = res
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
            const isOption = this.newModel.bcsAppInstName.list.find(item => item.id === instId)
            if (!isOption) {
                return
            }
            try {
                let res = await this.$store.dispatch('common/getBcsInstVersion', {
                    projectId: this.curProject.project_id,
                    ccId: this.curProject.cc_app_id,
                    instId
                })
                if (res === null) {
                    res = []
                }
                this.newModel.instVersionName.list = res
            } catch (err) {
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            }
        },
        async getInstVar (instName, versionId) {
            if (!instName || !versionId) {
                return
            }
            let instId = ''
            const isOption = this.newModel.bcsAppInstName.list.find(item => item.name === instName)
            if (isOption) {
                instId = isOption.id
            } else {
                return
            }
            try {
                const res = await this.$store.dispatch('common/getBcsInstVar', {
                    projectId: this.curProject.project_id,
                    ccId: this.curProject.cc_app_id,
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
                    projectCode: this.projectId || this.curProject.english_name
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
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            }
        },
        async getMuster () {
            try {
                let res = await this.$store.dispatch('common/getBcsMuster', {
                    projectId: this.curProject.project_id,
                    ccId: this.curProject.cc_app_id
                })
                if (!res) {
                    res = []
                }
                this.newModel.musterName.list = res
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
            const isOption = this.newModel.musterName.list.find(item => item.id === musterId)
            if (!isOption) {
                return
            }
            try {
                let res = await this.$store.dispatch('common/getBcsMusterVersion', {
                    projectId: this.curProject.project_id,
                    ccId: this.curProject.cc_app_id,
                    musterId
                })
                if (!res) {
                    res = []
                }
                res = res.map(item => ({
                    ...item,
                    name: item.show_version_name
                }))
                this.newModel.showVersionName.list = res
            } catch (err) {
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            }
        },
        async getTemplateName (versionName) {
            if (!versionName || !this.element.category) {
                return
            }
            let versionId = ''
            const isOption = this.newModel.showVersionName.list.find(item => item.name === versionName)
            if (isOption) {
                versionId = isOption.id
            } else {
                return
            }
            try {
                let res = []
                const resData = await this.$store.dispatch('common/getBcsInstanceEntity', {
                    projectId: this.curProject.project_id,
                    ccId: this.curProject.cc_app_id,
                    versionId
                })
                if (resData) {
                    for (const obj in resData) {
                        let newKey = ''
                        if (obj.startsWith('K8s') && (this.curProject.kind === 1 || this.curProject.kind === 3)) {
                            newKey = obj.substring(3)
                        } else if (this.curProject.kind === 2) {
                            newKey = obj.substring(0, 1).toUpperCase() + obj.substring(1)
                        } else {
                            newKey = obj
                        }
                        if (newKey === this.element.category) {
                            res = resData[obj]
                        }
                    }
                }
                this.newModel.templateName.list = res
            } catch (err) {
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            }
        },
        getCategoryList () {
            let arr = []
            if (this.curProject.kind === 1 || this.curProject.kind === 3) {
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
