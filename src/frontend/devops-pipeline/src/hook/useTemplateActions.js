
import { TEMPLATE_MODE } from '@/store/modules/templates/constants'
import { computed, nextTick, ref } from 'vue'
import UseInstance from './useInstance'

export default function useTemplateActions () {
    const { proxy, t, bkMessage, bkInfo, h, validator } = UseInstance()
    const projectId = computed(() => proxy.$route.params.projectId)
    const isInstanceList = computed(() => proxy.$route.params.type === 'instanceList')
    const isTableLoading = ref(false)
    const copyTemp = ref({
        isShow: false,
        title: t('template.copyTemplate'),
        closeIcon: false,
        quickClose: true,
        padding: '0 20px',
        srcTemplateId: '',
        templateName: '',
        isCopySetting: true
    })

    /**
     * 复制
     * @param row
     */
    function copyTemplate (row) {
        if (!row.canEdit) return
        copyTemp.value.templateName = `${row.name}_copy`
        copyTemp.value.isShow = true
        copyTemp.value.srcTemplateId = row.id
    }
    async function copyConfirmHandler () {
        const valid = await validator.validate()
        if (!valid) return
        isTableLoading.value = true
        const templateName = copyTemp.value.templateName || ''
        if (!templateName.trim()) {
            copyTemp.value.nameHasError = true; return
        }

        const postData = {
            projectId: projectId.value,
            params: {
                srcTemplateId: copyTemp.value.srcTemplateId,
                copySetting: copyTemp.value.isCopySetting,
                name: copyTemp.value.templateName
            }
        }

        try {
            const res = await proxy.$store.dispatch('templates/templateCopy', postData)
            copyCancelHandler()
            bkMessage({ message: t('template.copySuc'), theme: 'success' })
            goTemplateOverview(res)
        } catch (error) {
            const message = error.message || error
            bkMessage({ message, theme: 'error' })
        } finally {
            isTableLoading.value = false
        }
    }
    function copyCancelHandler () {
        copyTemp.value.isShow = false
        copyTemp.value.templateName = ''
        copyTemp.value.pipelineId = ''
        copyTemp.value.nameHasError = false
        copyTemp.value.isCopySetting = true
    }

    /**
     * 导出模板
     * @param row
     */
    async function exportTemplate (row) {
        try {
            const params = {
                projectId: projectId.value,
                templateId: row.id
            }
            const res = await proxy.$store.dispatch('templates/exportYamlTemplate', params, {
                responseType: 'blob'
            })
            const blob = new Blob([res], { type: 'application/x-yaml' })
            const url = window.URL || window.webkitURL || window.moxURL

            const a = document.createElement('a')
            a.href = url.createObjectURL(blob)
            a.download = `${row.name}.yaml`
            a.click()
            window.URL.revokeObjectURL(url)
        } catch (error) {
            bkMessage({
                message: error.message || error,
                theme: 'error'
            })
        }
    }

    /**
     * 删除模板
     * @param row
     */
    function deleteTemplate (row, fetchTableData) {
        if (!row.canEdit) return
        const title = row.mode === TEMPLATE_MODE.CONSTRAINT ? t('template.deleteStore') : t('template.deleteCustom')

        bkInfo({
            title,
            okText: t('delete'),
            extCls: 'delete_template',
            subHeader: h('div', [
                h('p', {
                    class: 'template-title-delete',
                    directives: [
                        {
                            name: 'bk-tooltips',
                            value: row.name
                        }
                    ]
                }, [
                    h('span', `${t('templateName')} : `),
                    h('span', { class: 'template-name-info' }, row.name)
                ])
            ]),
            confirmLoading: true,
            confirmFn: () => {
                confirmDeleteTemplate(row, fetchTableData)
            }
        })
    }
    async function confirmDeleteTemplate (row, fetchTableData) {
        isTableLoading.value = true
        try {
            await proxy.$store.dispatch('templates/deleteTemplate', {
                projectId: projectId.value,
                templateId: row.id
            })
            bkMessage({ message: t('template.deleteSuc'), theme: 'success' })
            if (typeof fetchTableData === 'function') {
                fetchTableData()
            }
        } catch (err) {
            bkMessage({
                message: err.message || err,
                theme: 'error'
            })
        } finally {
            isTableLoading.value = false
        }
    }

    function goTemplateOverview (data) {
        if (data?.canView === false) return
        if (!isInstanceList.value) {
            proxy.$router.push({
                name: 'TemplateOverview',
                params: {
                    templateId: data.templateId,
                    version: data.version,
                    type: 'instanceList'
                }
            })
        }
    }

    /**
     * 上架研发商店-关联商店
     * @param row
     */
    function toRelativeStore (row, storeStatus) {
        if (!row.canEdit) return
        let href = `${WEB_URL_PREFIX}/store/editTemplate/${row.id}?hasSourceInfo=true`
        if (storeStatus === 'NEVER_PUBLISHED') {
            href += `&projectCode=${encodeURIComponent(row.projectId)}`
        }
        window.open(href, '_blank')
    }

    /**
     * 到研发商店查看模板
     * @param row
     */
    function toStoreTemplateDetail (id) {
        const href = `${WEB_URL_PREFIX}/store/manage/template/${id}/releaseManage`
        window.open(href, '_blank')
    }
    /**
   * 转为自定义
   * @param row
   */
    async function convertToCustom (row, fetchTableData) {
        if (!row.canEdit) return
        nextTick(() => {
            bkInfo({
                width: 480,
                title: t('template.templateToCustom'),
                extCls: 'custom_template',
                subHeader: h('div', [
                    h('p', {
                        class: 'template-title',
                        directives: [
                            {
                                name: 'bk-tooltips',
                                value: row.name
                            }
                        ]
                    }, [
                        h('span', `${t('templateName')} : `),
                        h('span', { class: 'template-name-info' }, row.name)
                    ]),
                    h('div', { class: 'custom-tip' }, t('template.customTip'))
                ]),
                confirmLoading: true,
                confirmFn: async () => {
                    isTableLoading.value = true
                    try {
                        await proxy.$store.dispatch('templates/transformTemplateToCustom', {
                            projectId: projectId.value,
                            templateId: row.id
                        })
                        bkMessage({ message: t('template.deleteSuc'), theme: 'success' })
                        if (typeof fetchTableData === 'function') {
                            fetchTableData()
                        }
                    } catch (err) {
                        bkMessage({
                            message: err.message || err,
                            theme: 'error'
                        })
                    } finally {
                        isTableLoading.value = false
                    }
                }
            })
        })
    }

    return {
        copyTemp,
        isTableLoading,
        copyTemplate,
        exportTemplate,
        deleteTemplate,
        copyConfirmHandler,
        copyCancelHandler,
        goTemplateOverview,
        toStoreTemplateDetail,
        toRelativeStore,
        convertToCustom
    }
}
