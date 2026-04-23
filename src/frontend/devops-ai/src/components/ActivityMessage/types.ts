import type { ActivityType } from "../../constants";

type ActivityTypeValue = (typeof ActivityType)[keyof typeof ActivityType];

export interface DataTableContent {
  title: string;
  summary?: string;
  columns: Array<{
    key: string;
    label: string;
    width?: number;
  }>;
  rows: Array<Record<string, unknown>>;
  pagination?: {
    page: number;
    pageSize?: number;
    total: number;
    /** When set, forces client vs server pagination. Otherwise inferred from total vs rows.length. */
    mode?: "server" | "client";
  };
}

export interface KeyValueContent {
  title: string;
  data?: Record<string, string>;
  items?: Array<Record<string, unknown>>;
}

export interface GroupedListContent {
  title: string;
  groups: Array<{
    label: string;
    items: Array<Record<string, unknown>>;
  }>;
}

export interface OperationResultContent {
  title: string;
  status: "success" | "error" | "partial";
  message: string;
  details?: Array<{
    id: string;
    status: "success" | "error";
    message: string;
  }>;
}

export type ActivityContent =
  | DataTableContent
  | KeyValueContent
  | GroupedListContent
  | OperationResultContent;

export interface ActivityMessage {
  id: string;
  messageId: string;
  role: string;
  activityType: ActivityTypeValue;
  content: ActivityContent;
  status: string;
}
