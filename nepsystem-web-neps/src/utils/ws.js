// WebSocket 封装（复用 NEPV /ws/notify 单端点，带身份参数，断线 3s 重连）
let ws = null
let reconnectTimer = null
const handlers = new Set()

export function connectWS(query = '') {
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return
  const proto = location.protocol === 'https:' ? 'wss://' : 'ws://'
  ws = new WebSocket(proto + location.host + '/ws/notify' + (query ? '?' + query : ''))

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      handlers.forEach(h => h(msg))
    } catch (e) { /* 忽略非 JSON 消息 */ }
  }

  ws.onclose = () => {
    ws = null
    reconnectTimer = setTimeout(() => connectWS(query), 3000)
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
