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

  const lowerText = sourceText.toLowerCase();
  const lowerKeyword = normalizedKeyword.toLowerCase();
  const keywordLength = lowerKeyword.length;
  const segments = [];
  let cursor = 0;

  while (cursor < sourceText.length) {
    const matchIndex = lowerText.indexOf(lowerKeyword, cursor);
    if (matchIndex === -1) {
      segments.push({ text: sourceText.slice(cursor), matched: false });
      break;
    }

    if (matchIndex > cursor) {
      segments.push({ text: sourceText.slice(cursor, matchIndex), matched: false });
    }

    segments.push({
      text: sourceText.slice(matchIndex, matchIndex + keywordLength),
      matched: true,
    });
    cursor = matchIndex + keywordLength;
  }

  return segments;
}
