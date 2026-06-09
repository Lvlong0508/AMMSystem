import { regionData } from 'element-china-area-data'

function getRegionLabels(values) {
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

export function parseAddress(address) {
  if (!address) return { region: [], detail: '' }
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
  const regionStr = getRegionLabels(values)
  return regionStr + (detail || '')
}
