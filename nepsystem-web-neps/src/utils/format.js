// ---- 监督事件状态 ----
export const STATUS_TEXT = {
  PENDING_REVIEW: '待审核',
  APPROVED: '处理中',
  REJECTED: '已驳回',
  ASSIGNED: '处理中',
  ACCEPTED: '处理中',
  INSPECTING: '处理中',
  INSPECTED: '处理中',
  VERIFIED: '处理中',
  CLOSED: '已完成'
}

export const STATUS_TAG = {
  PENDING_REVIEW: 'warning',
  APPROVED: 'primary',
  REJECTED: 'danger',
  CLOSED: 'success'
}

// ---- 事件类型 ----
export const TYPE_TEXT = {
  POLLUTION: '污染',
  NOISE: '噪声',
  DEVICE_FAULT: '设备故障',
  OTHER: '其他'
}

// ---- 严重程度 ----
export const LEVEL_TEXT = { WARN: '预警', ALARM: '报警' }

// ---- 设备类型 ----
export const DEVICE_TYPE_TEXT = { AIR: '空气', WATER: '水质', NOISE: '噪声' }

export function statusText(s) { return STATUS_TEXT[s] || s || '-' }
export function statusTag(s) { return STATUS_TAG[s] || 'info' }
export function typeText(t) { return TYPE_TEXT[t] || t || '-' }
export function levelText(l) { return LEVEL_TEXT[l] || l || '-' }
export function deviceTypeText(t) { return DEVICE_TYPE_TEXT[t] || t || '-' }

export function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}
