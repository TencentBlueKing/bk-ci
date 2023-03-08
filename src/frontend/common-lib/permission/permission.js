import './permission.css'

/**
 * 处理无权限的情况，适用于 vue2
 * @param {*} ui 组件库
 * @param {*} params 接口请求数据
 * @param {*} ajax 发送请求方法
 * @param {*} h createElement 方法
 * @param {*} data 弹框需要的数据，不传就通过接口获取
 */
export const handleNoPermission = (ui, params, ajax, h, data = undefined) => {
    let infoBoxRef = {}
    let refreshBoxRef = {}

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
                    outerBorder: false,
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
                                                    on: {
                                                        click () {
                                                            window.open(info.url, '_blank')
                                                            handleClickLink()
                                                        }
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
                                        )
                                    ]
                                }
                            }
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
                                click () {
                                    window.open(data.groupInfoList[0].url, '_blank')
                                    handleClickLink()
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
                            click () {
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
    const handleClickLink = () => {
        // 关闭现有弹框
        infoBoxRef?.close?.()

        refreshBoxRef = ui.bkInfoBox({
            title: '权限申请单已提交',
            subHeader: h(
                'section',
                [
                    '请在权限管理页填写权限申请单，提交完成后再刷新该页面',
                    h(
                        'section',
                        {
                            class: 'permission-refresh-dialog'
                        },
                        [
                            h(
                                ui.bkButton,
                                {
                                    class: 'mr20',
                                    props: {
                                        theme: 'primary'
                                    },
                                    on: {
                                        click () {
                                            location.reload()
                                        }
                                    }
                                },
                                ['刷新页面']
                            ),
                            h(
                                ui.bkButton,
                                {
                                    on: {
                                        click () {
                                            refreshBoxRef.close?.()
                                        }
                                    }
                                },
                                ['关闭']
                            )
                        ]
                    )
                ]
            ),
            extCls: 'permission-dialog',
            width: 500,
            showFooter: false
        })
    }
    const showDialog = (data) => {
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
    }
    if (data) {
        showDialog(data)
    } else {
        ajax
            .get('/ms/auth/api/user/auth/apply/getRedirectInformation', { params })
            .then((res = {}) => {
                const data = res.data ? res.data : res
                showDialog(data)
            })
    }
}

/**
 * 处理无权限的情况，适用于 vue3
 * @param {*} ui 组件库
 * @param {*} params 接口请求数据
 * @param {*} ajax 发送请求方法
 * @param {*} h createElement 方法
 * @param {*} data 弹框需要的数据，不传就通过接口获取
 */
export const handleNoPermissionV3 = (ui, params, ajax, h, data) => {
    let infoBoxRef = {}
    let refreshBoxRef = {}

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
            ui.Exception,
            {
                type: '403',
                scene: 'part'
            },
            '没有操作权限'
        )
    }
    const renderTable = (data) => {
        return h(
            'section',
            {
                class: 'permission-table-wrapper'
            },
            h(
                ui.Table,
                {
                    border: 'none',
                    data: [data]
                },
                columns.map(column => {
                    return h(
                        ui.TableColumn,
                        {
                            label: column.label,
                            prop: column.prop
                        }
                    )
                })
            )
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
                        ui.Dropdown,
                        {},
                        {
                            content () {
                                return h(
                                    ui.Dropdown.DropdownMenu,
                                    {},
                                    data.groupInfoList.map((info) => {
                                        return h(
                                            ui.Dropdown.DropdownItem,
                                            {
                                                onClick () {
                                                    window.open(info.url, '_blank')
                                                    handleClickLink()
                                                }
                                            },
                                            [info.groupName]
                                        )
                                    })
                                )
                            },
                            default () {
                                return [
                                    h(
                                        ui.Button,
                                        {
                                            theme: 'primary',
                                            class: 'mr10'
                                        },
                                        [
                                            '去申请',
                                            h(
                                                ui.AngleDown,
                                                {
                                                    class: 'icon-angle-down-v3'
                                                }
                                            )
                                        ]
                                    )
                                ]
                            }
                        }
                    )
                    : h(
                        ui.Button,
                        {
                            class: 'mr10',
                            theme: 'primary',
                            onClick () {
                                window.open(data.groupInfoList[0].url, '_blank')
                                handleClickLink()
                            }
                        },
                        ['去申请']
                    ),
                h(
                    ui.Button,
                    {
                        class: 'mr25',
                        onClick () {
                            infoBoxRef?.hide?.()
                        }
                    },
                    [
                        '取消'
                    ]
                )
            ]
        )
    }
    const handleClickLink = () => {
        // 关闭现有弹框
        infoBoxRef?.hide?.()

        refreshBoxRef = ui.InfoBox({
            title: '权限申请单已提交',
            subTitle: h(
                'section',
                [
                    '请在权限管理页填写权限申请单，提交完成后再刷新该页面',
                    h(
                        'section',
                        {
                            class: 'permission-refresh-dialog'
                        },
                        [
                            h(
                                ui.Button,
                                {
                                    class: 'mr20',
                                    theme: 'primary',
                                    onClick () {
                                        location.reload()
                                    }
                                },
                                ['刷新页面']
                            ),
                            h(
                                ui.Button,
                                {
                                    onClick () {
                                        refreshBoxRef.hide?.()
                                    }
                                },
                                ['关闭']
                            )
                        ]
                    )
                ]
            ),
            extCls: 'permission-dialog',
            width: 500,
            dialogType: 'show'
        })
    }
    const showDialog = (data) => {
        infoBoxRef = ui.InfoBox({
            title: '',
            subTitle: h(
                'section',
                [
                    renderException(),
                    renderTable(data),
                    renderFooter(data)
                ]
            ),
            extCls: 'permission-dialog-v3',
            width: 640,
            dialogType: 'show'
        })
    }
    if (data) {
        showDialog(data)
    } else {
        ajax
            .get('/ms/auth/api/user/auth/apply/getRedirectInformation', params)
            .then((res = {}) => {
                const data = res.data ? res.data : res
                showDialog(data)
            })
    }
}
