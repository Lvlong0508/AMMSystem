import { regionData } from 'element-china-area-data'

function buildRegionString(values) {
  if (!Array.isArray(values)) return ''
  let current = regionData
  const labels = []
  for (const v of values) {
    const node = current.find(n => n.value === v)
    if (!node) break
    labels.push(node.label)
    current = node.children || []
  }
  return labels.join('')
}

// 假设地址以 element-china-area-data 中的完整行政区划名称开头，例如 "广东省深圳市南山区..."
export function parseAddress(address) {
  if (!address) return { region: [], detail: '' }
  address = address.trim()
  for (const p of regionData) {
    if (address.startsWith(p.label)) {
      let rest = address.slice(p.label.length)
      for (const c of (p.children || [])) {
        if (rest.startsWith(c.label)) {
          rest = rest.slice(c.label.length)
          for (const d of (c.children || [])) {
            if (rest.startsWith(d.label)) {
              return { region: [p.value, c.value, d.value], detail: rest.slice(d.label.length) }
            }
          }
          return { region: [p.value, c.value], detail: rest }
        }
      }
      return { region: [p.value], detail: rest }
    }
  }
  return { region: [], detail: address }
}

export function buildAddressString(values, detail) {
  if (!values || values.length === 0) return detail || ''
  const regionStr = buildRegionString(values)
  return regionStr + (detail || '')
}
