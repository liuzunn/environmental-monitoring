// WebSocket 封装：连接 /ws/notify，断线自动重连（3 秒）
let ws = null
let reconnectTimer = null
const handlers = new Set()

export function connectWS() {
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return
  const proto = location.protocol === 'https:' ? 'wss://' : 'ws://'
  ws = new WebSocket(proto + location.host + '/ws/notify')

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      handlers.forEach(h => h(msg))
    } catch (e) { /* 忽略非 JSON 消息 */ }
  }

  ws.onclose = () => {
    reconnectTimer = setTimeout(connectWS, 3000)
  }

  ws.onerror = () => {
    try { ws.close() } catch (e) { /* 忽略 */ }
  }
}

export function onWSMessage(handler) {
  handlers.add(handler)
  return () => handlers.delete(handler)
}

export function closeWS() {
  clearTimeout(reconnectTimer)
  handlers.clear()
  if (ws) { try { ws.close() } catch (e) { /* 忽略 */ } ws = null }
}
