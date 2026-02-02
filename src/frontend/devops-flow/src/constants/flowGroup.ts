/**
 * 创作流组相关的常量定义
 */

/**
 * 一般情况下组类型 groupId
 */
export const FLOW_GROUP_TYPES = {
  /** 未分类创作流 */
  UNCLASSIFIED_FLOWS: 'unclassified',
  /** 最近使用创作流 */
  RECENT_USED_FLOWS: 'recentUse',
  /** 全部创作流 */
  ALL_FLOWS: 'allPipeline',
  /** 我收藏的创作流 */
  MY_FAVORITES: 'collect',
  /** 我创建的创作流 */
  MY_CREATED: 'myPipeline',
  /** 回收站 */
  RECYCLE_BIN: 'recycleBin',
} as const;

/**
 * 获取所有组类型值
 */
export const getAllFlowGroupTypes = () => Object.values(FLOW_GROUP_TYPES);

/**
 * 检查是否为系统预定义组类型
 */
export const isSystemFlowGroupType = (groupId: string): boolean => {
  return Object.values(FLOW_GROUP_TYPES).includes(groupId as any);
};

