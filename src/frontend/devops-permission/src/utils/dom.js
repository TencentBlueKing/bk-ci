const dom = {
  nodeContains(root, el) {
    if (root.compareDocumentPosition) {
      return root === el || !!(root.compareDocumentPosition(el) & 16);
    }
    if (root.contains && el.nodeType === 1) {
      return root.contains(el) && root !== el;
    }
    let node = el.parentNode;
    while (node) {
      if (node === root) return true;
      node = node.parentNode;
    }
    return false;
  },
  parentClsContains(cls, el) {
    if (el.classList.contains(cls)) {
      return true;
    }
    let node = el.parentNode;
    while (node) {
      if (node.classList?.contains(cls)) {
        return true;
      };
      node = node.parentNode;
    }
    return false;
  },
  getElementScrollCoords(element) {
    let actualLeft = element.offsetLeft;
    let actualTop = element.offsetTop;
    let current = element.offsetParent;
    while (current !== null) {
      // 注意要加上边界宽度
      actualLeft += (current.offsetLeft + current.clientLeft);
      actualTop += (current.offsetTop + current.clientTop);
      current = current.offsetParent;
    }
    return { x: actualLeft, y: actualTop };
  },
  setPageTabIcon(path) {
    const link = document.querySelector("link[rel*='icon']") || document.createElement('link');
    link.type = 'image/x-icon';
    link.rel = 'shortcut icon';
    link.href = path;
    document.getElementsByTagName('head')[0].appendChild(link);
  },
};

export default dom;
