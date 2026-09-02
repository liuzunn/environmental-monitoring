export const STATUS_TEXT = {
  PENDING_REVIEW: '待审核', APPROVED: '待派单', ASSIGNED: '已派单', ACCEPTED: '已接单',
  INSPECTING: '巡检中', INSPECTED: '已巡检', VERIFIED: '已核实', REJECTED: '已驳回', CLOSED: '已完成'
}
export const STATUS_TAG = {
  PENDING_REVIEW: 'warning', APPROVED: 'primary', ASSIGNED: 'primary', ACCEPTED: 'primary',
  INSPECTING: 'primary', INSPECTED: 'success', VERIFIED: 'success', REJECTED: 'danger', CLOSED: 'success'
}
export const TYPE_TEXT = { POLLUTION: '污染', NOISE: '噪声', DEVICE_FAULT: '设备故障', OTHER: '其他' }
export const LEVEL_TEXT = { WARN: '预警', ALARM: '报警' }
export const PRIORITY_TEXT = { LOW: '低', MEDIUM: '中', HIGH: '高' }
export const PRIORITY_TAG = { LOW: 'info', MEDIUM: 'warning', HIGH: 'danger' }
export function statusText(s) { return STATUS_TEXT[s] || s || '-' }
export function statusTag(s) { return STATUS_TAG[s] || 'info' }
export function typeText(t) { return TYPE_TEXT[t] || t || '-' }
export function levelText(l) { return LEVEL_TEXT[l] || l || '-' }
export function priorityText(p) { return PRIORITY_TEXT[p] || p || '-' }
export function priorityTag(p) { return PRIORITY_TAG[p] || 'info' }
export function fmtTime(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }
