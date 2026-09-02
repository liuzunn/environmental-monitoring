export const STATUS_TEXT = {
  ASSIGNED: '待接收', ACCEPTED: '已接收', INSPECTING: '检测中',
  INSPECTED: '已完成', CLOSED: '已关闭', PENDING_REVIEW: '待审核', APPROVED: '待派单'
}
export const STATUS_TAG = {
  ASSIGNED: 'warning', ACCEPTED: 'primary', INSPECTING: 'primary', INSPECTED: 'success', CLOSED: 'success'
}
export const TYPE_TEXT = { POLLUTION: '污染', NOISE: '噪声', DEVICE_FAULT: '设备故障', OTHER: '其他' }
export const PRIORITY_TEXT = { LOW: '低', MEDIUM: '中', HIGH: '高' }
export const PRIORITY_TAG = { LOW: 'info', MEDIUM: 'warning', HIGH: 'danger' }
export function statusText(s) { return STATUS_TEXT[s] || s || '-' }
export function statusTag(s) { return STATUS_TAG[s] || 'info' }
export function typeText(t) { return TYPE_TEXT[t] || t || '-' }
export function priorityText(p) { return PRIORITY_TEXT[p] || p || '-' }
export function priorityTag(p) { return PRIORITY_TAG[p] || 'info' }
export function fmtTime(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : '-' }
export function fmtDateTime(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }

// ---- AQI 前端实时计算（HJ633-2012 1小时均值分段表，与后端 calcAQI 一致） ----
const BP = {
  pm25: [[0,35,0,50],[35,75,50,100],[75,115,100,150],[115,150,150,200],[150,250,200,300],[250,350,300,400],[350,500,400,500]],
  pm10: [[0,50,0,50],[50,150,50,100],[150,250,100,150],[250,350,150,200],[350,420,200,300],[420,500,300,400],[500,600,400,500]],
  so2:  [[0,50,0,50],[50,150,50,100],[150,475,100,150],[475,800,150,200],[800,1600,200,300],[1600,2100,300,400],[2100,2620,400,500]],
  no2:  [[0,40,0,50],[40,80,50,100],[80,180,100,150],[180,280,150,200],[280,565,200,300],[565,750,300,400],[750,940,400,500]],
  co:   [[0,5,0,50],[5,10,50,100],[10,35,100,150],[35,60,150,200],[60,90,200,300],[90,120,300,400],[120,150,400,500]],
  o3:   [[0,160,0,50],[160,200,50,100],[200,300,100,150],[300,400,150,200],[400,800,200,300],[800,1000,300,400],[1000,1200,400,500]]
}
function iaqi(c, segs) {
  if (c == null || c === '' || isNaN(c)) return null
  c = Number(c)
  for (const s of segs) {
    if (c <= s[1]) return Math.round((s[3] - s[2]) / (s[1] - s[0]) * (c - s[0]) + s[2])
  }
  return segs[segs.length - 1][3]
}
export function calcAQI(values) {
  let max = -1
  for (const key of ['pm25', 'pm10', 'so2', 'no2', 'co', 'o3']) {
    const v = iaqi(values[key], BP[key])
    if (v !== null) max = Math.max(max, v)
  }
  return max < 0 ? null : max
}
