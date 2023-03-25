import './authority-directive.scss';
import ajax from '../ajax/index';
import { version } from '../utils/vue';

// vue2 和 vue3 使用的弹框不一样，使用变量接收传入的值
let handleShowDialog;

const DEFAULT_OPTIONS = {
  hasPermission: false,
  offset: [12, 0],
  cls: 'bk-permission-cursor-element',
  disablePermissionApi: false,
  tooltips: '',
};

/**
 * 初始化
 * @param {*} el
 * @param {*} data
 * @param {*} vNode
 * @returns
 */
function init(el, data, vNode) {
  // 节点被替换过时需要还原回来
  if (el.originEl) {
    el = destroy(el, vNode);
  }
  const parent = el.parentNode;
  const options = Object.assign({}, DEFAULT_OPTIONS, data);
  if (options.hasPermission || !parent) return;

  if (!el.cloneEl) {
    el.cloneEl = el.cloneNode(true);
  }
  const { cloneEl } = el;
  // 保留原始节点
  cloneEl.originEl = el;
  // 替换当前节点（为了移除节点的所有事件）
  parent?.replaceChild(cloneEl, el);
  Reflect.has(vNode, 'elm')
    ? vNode.elm = cloneEl
    : vNode.el = cloneEl;

  cloneEl.classList.add('bk-permission-disable');
  // 添加提示
  if (options.tooltips) {
    cloneEl.classList.add('bk-permission-tooltips');
    cloneEl.setAttribute('tooltips', options.tooltips);
  }
  cloneEl.mouseEnterHandler = function () {
    const element = document.createElement('div');
    element.id = 'directive-ele';
    element.style.position = 'absolute';
    element.style.zIndex = '9999';
    cloneEl.element = element;
    document.body.appendChild(element);

    element.classList.add(options.cls || DEFAULT_OPTIONS.cls);
    cloneEl.addEventListener('mousemove', cloneEl.mouseMoveHandler);
  };
  cloneEl.mouseMoveHandler = function (event) {
    const { pageX, pageY } = event;
    const elLeft = pageX + DEFAULT_OPTIONS.offset[0];
    const elTop = pageY + DEFAULT_OPTIONS.offset[1];
    cloneEl.element.style.left = `${elLeft}px`;
    cloneEl.element.style.top = `${elTop}px`;
  };
  cloneEl.mouseLeaveHandler = function () {
    cloneEl.element?.remove();
    cloneEl.element = null;
    cloneEl.removeEventListener('mousemove', cloneEl.mouseMoveHandler);
  };
  cloneEl.clickHandler = async (e) => {
    e.stopPropagation();
    // 点击弹框
    handleShowDialog(data.permissionData);
  };

  cloneEl.addEventListener('mouseenter', cloneEl.mouseEnterHandler);
  cloneEl.addEventListener('mouseleave', cloneEl.mouseLeaveHandler);
  cloneEl.addEventListener('click', cloneEl.clickHandler);
}

/**
 * 销毁元素
 * @param {*} cloneEl
 * @param {*} vNode
 * @returns
 */
function destroy(cloneEl, vNode) {
  const el = cloneEl.originEl;
  if (!el) return;

  // 还原原始节点
  const parent = cloneEl.parentNode;
  parent?.replaceChild(el, el.cloneEl);
  Reflect.has(vNode, 'elm')
    ? vNode.elm = el
    : vNode.el = el;
  cloneEl.removeEventListener('mouseenter', cloneEl.mouseEnterHandler);
  cloneEl.removeEventListener('mousemove', cloneEl.mouseMoveHandler);
  cloneEl.removeEventListener('mouseleave', cloneEl.mouseLeaveHandler);
  cloneEl.removeEventListener('click', cloneEl.clickHandler);
  cloneEl.element?.remove();
  cloneEl.element = null;
  delete el.cloneEl;
  return el;
}

/**
 * 判断是否有权限
 * @param {*} data 权限数据包
 */
function validatePermission(data, ajaxPrefix) {
  return new Promise((resolve) => {
    if (!data) return resolve(true);
    const { projectId, resourceType, resourceCode, action } = data;
    // 通过下面三个数据确定发送请求
    const key = projectId + resourceType + resourceCode;

    // 构造组合请求的数据包
    let postData = validatePermission
      .postData
      ?.find(data => (data.projectId + data.resourceType + data.resourceCode) === key);
    if (!postData) {
      ajax;
      postData = { projectId, resourceType, resourceCode, actionList: [] };
      validatePermission.postData = [...validatePermission.postData || [], postData];
    }
    postData.actionList.push(action);

    // 接口执行完以后的回调
    let callBackMap = validatePermission.callBackMaps?.[key];
    if (!callBackMap) {
      callBackMap = [];
      validatePermission.callBackMaps = {
        ...validatePermission.callBackMaps,
        [key]: callBackMap,
      };
    }
    callBackMap.push(detail => resolve(detail[action]));

    // 使用节流的方式防止发送太多请求，对请求进行组合
    clearTimeout(validatePermission[key]);
    validatePermission[key] = setTimeout(() => {
      const { projectId, ...others } = postData;
      // 真正发送请求
      ajax
        .post(`${ajaxPrefix}/auth/api/user/auth/permission/batch/validate`, others, { headers: { 'X-DEVOPS-PROJECT-ID': projectId } })
        .then((res) => {
          const detail = res.data || res;
          validatePermission.callBackMaps[key].map(callBack => callBack(detail));
        })
        .catch((err) => {
          validatePermission.callBackMaps[key].map(callBack => callBack({}));
          console.error(err.message || err);
        });
    }, 0);
  });
}

// 通过接口判断有无权限
async function updatePerms(el, data, vNode, ajaxPrefix) {
  const hasPermission = await validatePermission(data.permissionData, ajaxPrefix);
  const cloneData = JSON.parse(JSON.stringify(data));
  cloneData.hasPermission = hasPermission;
  init(el, cloneData, vNode);
}

// vue2 使用的权限指令
function AuthorityDirectiveV2(handleNoPermission, ajaxPrefix = '') {
  handleShowDialog = handleNoPermission;
  return class {
    static install(Vue) {
      Vue.directive('perm', {
        bind(el, binding, vNode) {
          if (!vNode.key) {
            vNode.key = new Date().getTime();
          }
        },
        inserted(el, binding, vNode) {
          const { disablePermissionApi } = binding.value;
          if (!disablePermissionApi) {
            updatePerms(el, binding.value, vNode, ajaxPrefix);
          } else {
            init(el, binding.value, vNode);
          }
        },
        update(el, binding, vNode) {
          const { value, oldValue } = binding;
          if (JSON.stringify(value) === JSON.stringify(oldValue)) return;
          init(el, binding.value, vNode);
        },
        unbind(el, binding, vNode) {
          destroy(el, vNode);
        },
      });
    }
  };
}

// vue3 使用的权限指令
function AuthorityDirectiveV3(handleNoPermission, ajaxPrefix = '') {
  handleShowDialog = handleNoPermission;
  return class {
    static install(app) {
      app.directive('perm', {
        created(el, binding, vNode) {
          if (!vNode.key) {
            vNode.key = new Date().getTime();
          }
        },
        mounted(el, binding, vNode) {
          const { disablePermissionApi } = binding.value;
          if (!disablePermissionApi) {
            updatePerms(el, binding.value, vNode, ajaxPrefix);
          } else {
            init(el, binding.value, vNode);
          }
        },
        updated(el, binding, vNode) {
          const { value, oldValue } = binding;
          if (JSON.stringify(value) === JSON.stringify(oldValue)) return;
          init(el, binding.value, vNode);
        },
        beforeUnmount(el, binding, vNode) {
          destroy(el, vNode);
        },
      });
    }
  };
}

export default version === 2 ? AuthorityDirectiveV2 : AuthorityDirectiveV3
