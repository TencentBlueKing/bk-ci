import './permission.css';
import ajax from '../ajax/index';
import { version } from '../utils/vue';
import { t } from '../utils/locale';
/**
 * 处理无权限的情况，适用于 vue2
 * @param {*} ui 组件库
 * @param {*} params 接口请求数据
 * @param {*} h createElement 方法
 * @param {*} data 弹框需要的数据，不传就通过接口获取
 * @param {*} ajaxPrefix 接口请求前缀
 */
export const handleNoPermissionV2 = (ui, params, h, data = undefined, ajaxPrefix = '') => {
  let infoBoxRef = {};
  let refreshBoxRef = {};

  const columns = [
    {
      label: t('需要申请的权限'),
      prop: 'actionName',
    },
    {
      label: t('关联的资源类型'),
      prop: 'resourceTypeName',
    },
    {
      label: t('关联的资源实例'),
      prop: 'resourceName',
    },
  ];
  const renderException = () => h(
    ui.bkException,
    {
      class: 'permission-exception',
      props: {
        type: '403',
        scene: 'part',
      },
    },
    t('没有操作权限'),
  );
  const renderTable = data => h(
    ui.bkTable,
    {
      class: 'permission-table',
      props: {
        outerBorder: false,
        data: [data],
      },
    },
    columns
      .filter(column => data[column.prop])
      .map(column => h(
        ui.bkTableColumn,
        {
          props: {
            showOverflowTooltip: true,
            label: column.label,
            prop: column.prop,
          },
        },
      )),
  );
  const renderFooter = data => h(
    'section',
    {
      class: 'permission-footer',
    },
    [
      data.auth
        ? h(
          ui.bkDropdownMenu,
          {
            class: 'permission-list',
            scopedSlots: {
              'dropdown-content'() {
                return h(
                  'ui',
                  {
                    class: 'bk-dropdown-list',
                  },
                  (data.groupInfoList).map(info => h(
                    'li',
                    {
                      on: {
                        click() {
                          window.open(info.url, '_blank');
                          handleClickLink();
                        },
                      },
                    },
                    [info.groupName],
                  )),
                );
              },
              'dropdown-trigger'() {
                return [
                  h(
                    'span',
                    {
                      class: 'bk-dropdown-list permission-confirm',
                    },
                    [t('去申请')],
                  ),
                  h(
                    'i',
                    {
                      class: 'bk-icon icon-angle-down',
                    },
                  ),
                ];
              },
            },
          },
        )
        : h(
          ui.bkButton,
          {
            class: 'permission-confirm',
            props: {
              theme: 'primary',
            },
            on: {
              click() {
                window.open(data.groupInfoList[0].url, '_blank');
                handleClickLink();
              },
            },
          },
          [t('去申请')],
        ),
      h(
        ui.bkButton,
        {
          class: 'permission-cancel',
          on: {
            click() {
              infoBoxRef?.close?.();
            },
          },
        },
        [
          t('取消'),
        ],
      ),
    ],
  );
  const handleClickLink = () => {
    // 关闭现有弹框
    infoBoxRef?.close?.();

    refreshBoxRef = ui.bkInfoBox({
      title: t('权限申请单已提交'),
      subHeader: h(
        'section',
        [
          t('请在权限管理页填写权限申请单，提交完成后再刷新该页面'),
          h(
            'section',
            {
              class: 'permission-refresh-dialog',
            },
            [
              h(
                ui.bkButton,
                {
                  class: 'mr20',
                  props: {
                    theme: 'primary',
                  },
                  on: {
                    click() {
                      location.reload();
                    },
                  },
                },
                [t('刷新页面')],
              ),
              h(
                ui.bkButton,
                {
                  on: {
                    click() {
                      refreshBoxRef.close?.();
                    },
                  },
                },
                [t('关闭')],
              ),
            ],
          ),
        ],
      ),
      extCls: 'permission-dialog',
      width: 500,
      showFooter: false,
    });
  };
  const showDialog = (data) => {
    infoBoxRef = ui.bkInfoBox({
      subHeader: h(
        'section',
        [
          renderException(),
          renderTable(data),
          renderFooter(data),
        ],
      ),
      extCls: 'permission-dialog',
      width: 640,
      showFooter: false,
    });
  };
  if (data) {
    showDialog(data);
  } else {
    ajax
      .get(`${ajaxPrefix}/ms/auth/api/user/auth/apply/getRedirectInformation`, { params })
      .then((res = {}) => {
        const data = res.data ? res.data : res;
        showDialog(data);
      });
  }
};

/**
 * 处理无权限的情况，适用于 vue3
 * @param {*} ui 组件库
 * @param {*} params 接口请求数据
 * @param {*} h createElement 方法
 * @param {*} data 弹框需要的数据，不传就通过接口获取
 * @param {*} ajaxPrefix 接口请求前缀
 */
export const handleNoPermissionV3 = (ui, params, h, data, ajaxPrefix = '') => {
  let infoBoxRef = {};
  let refreshBoxRef = {};

  const columns = [
    {
      label: t('需要申请的权限'),
      prop: 'actionName',
    },
    {
      label: t('关联的资源类型'),
      prop: 'resourceTypeName',
    },
    {
      label: t('关联的资源实例'),
      prop: 'resourceName',
    },
  ];
  const renderException = () => h(
    ui.Exception,
    {
      type: '403',
      scene: 'part',
    },
    t('没有操作权限'),
  );
  const renderTable = data => h(
    'section',
    {
      class: 'permission-table-wrapper',
    },
    h(
      ui.Table,
      {
        border: 'none',
        data: [data],
      },
      columns
        .filter(column => data[column.prop])
        .map(column => h(
          ui.TableColumn,
          {
            showOverflowTooltip: true,
            label: column.label,
            prop: column.prop,
          },
        )),
    ),
  );
  const renderFooter = data => h(
    'section',
    {
      class: 'permission-footer',
    },
    [
      data.auth
        ? h(
          ui.Dropdown,
          {},
          {
            content() {
              return h(
                ui.Dropdown.DropdownMenu,
                {},
                data.groupInfoList.map(info => h(
                  ui.Dropdown.DropdownItem,
                  {
                    onClick() {
                      window.open(info.url, '_blank');
                      handleClickLink();
                    },
                  },
                  [info.groupName],
                )),
              );
            },
            default() {
              return [
                h(
                  ui.Button,
                  {
                    theme: 'primary',
                    class: 'mr10',
                  },
                  [
                    t('去申请'),
                    h(
                      ui.AngleDown,
                      {
                        class: 'icon-angle-down-v3',
                      },
                    ),
                  ],
                ),
              ];
            },
          },
        )
        : h(
          ui.Button,
          {
            class: 'mr10',
            theme: 'primary',
            onClick() {
              window.open(data.groupInfoList[0].url, '_blank');
              handleClickLink();
            },
          },
          [t('去申请')],
        ),
      h(
        ui.Button,
        {
          class: 'mr25',
          onClick() {
            infoBoxRef?.hide?.();
          },
        },
        [
          t('取消'),
        ],
      ),
    ],
  );
  const handleClickLink = () => {
    // 关闭现有弹框
    infoBoxRef?.hide?.();

    refreshBoxRef = ui.InfoBox({
      title: t('权限申请单已提交'),
      subTitle: h(
        'section',
        [
          t('请在权限管理页填写权限申请单，提交完成后再刷新该页面'),
          h(
            'section',
            {
              class: 'permission-refresh-dialog',
            },
            [
              h(
                ui.Button,
                {
                  class: 'mr20',
                  theme: 'primary',
                  onClick() {
                    location.reload();
                  },
                },
                [t('刷新页面')],
              ),
              h(
                ui.Button,
                {
                  onClick() {
                    refreshBoxRef.hide?.();
                  },
                },
                [t('关闭')],
              ),
            ],
          ),
        ],
      ),
      extCls: 'permission-dialog',
      width: 500,
      dialogType: 'show',
    });
  };
  const showDialog = (data) => {
    infoBoxRef = ui.InfoBox({
      title: '',
      subTitle: h(
        'section',
        [
          renderException(),
          renderTable(data),
          renderFooter(data),
        ],
      ),
      extCls: 'permission-dialog-v3',
      width: 640,
      dialogType: 'show',
    });
  };
  if (data) {
    showDialog(data);
  } else {
    ajax
      .get(`${ajaxPrefix}/ms/auth/api/user/auth/apply/getRedirectInformation`, { params })
      .then((res = {}) => {
        const data = res.data ? res.data : res;
        showDialog(data);
      });
  }
};

export default version === 2 ? handleNoPermissionV2 : handleNoPermissionV3
