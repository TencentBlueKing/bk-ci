import './permission.css';

/**
 * 处理无权限的情况
 * @param {*} ui 组件库
 * @param {*} params 接口请求数据
 * @param {*} ajax 发送请求方法
 * @param {*} h createElement 方法
 */
export const handleNoPermission = (ui, params, ajax, h) => {
    let infoBoxRef = {}

    const columns = [
        {
            label: '需要申请的权限',
            prop: 'actionName'
        },
        {
            label: '关联的资源类型',
            prop: 'resourceTypeName'
        },
        {
            label: '关联的资源实例',
            prop: 'resourceName'
        }
    ]
    const renderException = () => {
        return h(
            ui.bkException,
            {
                class: 'permission-exception',
                props: {
                    type: '403',
                    scene: 'part'
                }
            },
            '没有操作权限'
        )
    }
    const renderTable = (data) => {
        return h(
            ui.bkTable,
            {
                class: 'permission-table',
                props: {
                    data: [data]
                }
            },
            columns.map(column => {
                return h(
                    ui.bkTableColumn,
                    {
                        props: {
                            label: column.label,
                            prop: column.prop
                        }
                    }
                )
            })
        )
    }
    const renderFooter = (data) => {
        return h(
            'section',
            {
                class: 'permission-footer'
            },
            [
                data.auth
                    ? h(
                        ui.bkDropdownMenu,
                        {
                            class: 'permission-list',
                            scopedSlots: {
                                'dropdown-content' () {
                                    return h(
                                        'ui',
                                        {
                                            class: 'bk-dropdown-list'
                                        },
                                        (data.groupInfoList).map((info) => {
                                            return h(
                                                'li',
                                                {
                                                    onClick () {
                                                        window.open(info.url, '_blank')
                                                    }
                                                },
                                                [info.groupName]
                                            )
                                        })
                                    )
                                },
                                'dropdown-trigger' () {
                                    return [
                                        h(
                                            'span',
                                            {
                                                class: 'bk-dropdown-list permission-confirm'
                                            },
                                            ['去申请']
                                        ),
                                        h(
                                            'i',
                                            {
                                                class: 'bk-icon icon-angle-down'
                                            }
                                        ),
                                    ]
                                }
                            },
                        }
                    )
                    : h(
                        ui.bkButton,
                        {
                            class: 'permission-confirm',
                            props: {
                                theme: 'primary'
                            },
                            on: {
                                click() {
                                    window.open(data.groupInfoList[0].url, '_blank')
                                }
                            }
                        },
                        ['去申请']
                    ),
                h(
                    ui.bkButton,
                    {
                        class: 'permission-cancel',
                        on: {
                            click() {
                                infoBoxRef?.close?.()
                            }
                        }
                    },
                    [
                        '取消'
                    ]
                )
            ]
        )
    }
    return ajax
        .get('/ms/auth/api/user/auth/apply/getRedirectInformation', { params })
        .then((data = {}) => {
            infoBoxRef = ui.bkInfoBox({
                subHeader: h(
                    'section',
                    [
                        renderException(),
                        renderTable(data),
                        renderFooter(data)
                    ]
                ),
                extCls: 'permission-dialog',
                width: 640,
                showFooter: false
            })
        })
    
}
