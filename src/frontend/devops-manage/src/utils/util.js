function prezero (num) {
    num = Number(num)

    if (num < 10) {
        return '0' + num
    }

    return num
}

export function convertTime (ms) {
    if (!ms) return '--'
    const time = new Date(ms)

    return `${time.getFullYear()}-${prezero(time.getMonth() + 1)}-${prezero(time.getDate())} ${prezero(time.getHours())}:${prezero(time.getMinutes())}:${prezero(time.getSeconds())}`
}

export function deepEqual(obj1, obj2) {
    if (obj1 === obj2) return true;

    if (typeof obj1 !== 'object' || typeof obj2 !== 'object' || obj1 === null || obj2 === null) {
        return false;
    }

    const keys1 = Object.keys(obj1);
    const keys2 = Object.keys(obj2);

    if (keys1.length !== keys2.length) return false;

    const keys2Set = new Set(keys2);

    for (const key of keys1) {
        if (!keys2Set.has(key) || !deepEqual(obj1[key], obj2[key])) {
        return false;
        }
    }

    return true;
}

export async function copyToClipboard(text) {
    if (navigator.clipboard?.writeText) {
      try {
        await navigator.clipboard.writeText(text);
      } catch (err) {
        throw err;
      }
    } else {
      const textArea = document.createElement('textarea');
      textArea.value = text;
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand('Copy');
      document.body.removeChild(textArea);
    }
  }
  
export function validProjectCode(code) {
    if (typeof code !== 'string') return false;
    return /^[a-z0-9-]+$/.test(code);
  }