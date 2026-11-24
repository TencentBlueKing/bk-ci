/**
 * 按项目名称关键字进行不区分大小写的子串模糊过滤
 * @param {Array<{ projectName?: string }>} projects - 项目列表
 * @param {string} [keyword=''] - 搜索关键字
 * @returns {Array} 匹配的项目列表（保持原始顺序）
 */
export function filterProjectsByKeyword(projects, keyword = '') {
  if (!Array.isArray(projects)) {
    return [];
  }

  const normalizedKeyword = typeof keyword === 'string' ? keyword.trim().toLowerCase() : '';
  if (!normalizedKeyword) {
    return projects.slice();
  }

  return projects.filter((project) => {
    const projectName = typeof project?.projectName === 'string' ? project.projectName : '';
    return projectName.toLowerCase().includes(normalizedKeyword);
  });
}

/**
 * 将文本按关键字（不区分大小写的字面量匹配）拆分为高亮片段数组
 * @param {string} text - 原始文本
 * @param {string} [keyword=''] - 搜索关键字
 * @returns {Array<{ text: string, matched: boolean }>} 分段结果
 */
export function splitHighlightSegments(text, keyword = '') {
  const sourceText = typeof text === 'string' ? text : '';
  if (!sourceText) {
    return [];
  }

  const normalizedKeyword = typeof keyword === 'string' ? keyword.trim() : '';
  if (!normalizedKeyword) {
    return [{ text: sourceText, matched: false }];
  }

  const { text: lowerText, offsets } = lowerCaseWithOffsets(sourceText);
  const lowerKeyword = normalizedKeyword.toLowerCase();
  const segments = [];
  let searchCursor = 0;
  let sourceCursor = 0;

  while (searchCursor < lowerText.length) {
    const matchIndex = lowerText.indexOf(lowerKeyword, searchCursor);
    if (matchIndex === -1) {
      break;
    }

    const matchEnd = matchIndex + lowerKeyword.length;
    const sourceStart = Math.max(offsets[matchIndex], sourceCursor);
    const sourceEnd = offsets[matchEnd];

    if (sourceStart > sourceCursor) {
      segments.push({ text: sourceText.slice(sourceCursor, sourceStart), matched: false });
    }

    if (sourceEnd > sourceStart) {
      segments.push({ text: sourceText.slice(sourceStart, sourceEnd), matched: true });
    }
    sourceCursor = Math.max(sourceCursor, sourceEnd);
    searchCursor = matchEnd;
  }

  if (sourceCursor < sourceText.length) {
    segments.push({ text: sourceText.slice(sourceCursor), matched: false });
  }

  return segments;
}

/**
 * 逐字符转小写，并记录小写文本每个位置对应的原始文本位置
 * 小写转换可能改变长度（如 'İ' 转为两个字符），匹配位置需映射回原文
 * @param {string} sourceText - 原始文本
 * @returns {{ text: string, offsets: number[] }} 小写文本及位置映射（长度为小写文本长度 + 1）
 */
function lowerCaseWithOffsets(sourceText) {
  let text = '';
  const offsets = [];
  let sourceIndex = 0;

  for (const char of sourceText) {
    const lowerChar = char.toLowerCase();
    const nextSourceIndex = sourceIndex + char.length;
    for (let i = 0; i < lowerChar.length; i += 1) {
      offsets.push(i === 0 ? sourceIndex : nextSourceIndex);
    }
    text += lowerChar;
    sourceIndex = nextSourceIndex;
  }
  offsets.push(sourceIndex);

  return { text, offsets };
}
