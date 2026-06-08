/**
 * preparePassword(plainText)
 *
 * Two-layer protection before the password ever leaves the browser:
 *   1. SHA-256 hash  — one-way; the server never sees the original password
 *   2. RSA-OAEP encrypt — the hash is encrypted with the server's public key;
 *      Chrome DevTools shows only an indecipherable Base64 blob.
 *
 * What Chrome DevTools will show in the request payload:
 *   { "password": "Xk9mP2q..." }  ← 344-char encrypted blob, meaningless without private key
 */

function resolveBaseUrl() {
  const raw = import.meta.env.VITE_API_URL
  if (!raw) return 'http://localhost:8080/api'
  const url = raw.startsWith('http://') || raw.startsWith('https://')
    ? raw
    : 'https://' + raw
  return url.replace(/\/$/, '') + '/api'
}

const BASE_URL = resolveBaseUrl()

async function sha256hex(text) {
  const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text))
  return Array.from(new Uint8Array(buf))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}

async function fetchAndImportPublicKey() {
  const res = await fetch(`${BASE_URL}/auth/public-key`)
  if (!res.ok) {
    throw new Error(`Could not reach auth server (status ${res.status}). Is the backend running?`)
  }
  const data = await res.json().catch(() => {
    throw new Error('Auth server returned an unexpected response. Please try again.')
  })
  const { publicKey } = data
  const der = Uint8Array.from(atob(publicKey), (c) => c.charCodeAt(0))
  return crypto.subtle.importKey(
    'spki',
    der,
    { name: 'RSA-OAEP', hash: 'SHA-256' },
    false,
    ['encrypt'],
  )
}

export async function preparePassword(plainText) {
  // Step 1: SHA-256 — server never knows the original password
  const hash = await sha256hex(plainText)

  // Step 2: RSA-OAEP encrypt — hides even the hash from DevTools observers
  const publicKey = await fetchAndImportPublicKey()
  const encrypted = await crypto.subtle.encrypt(
    { name: 'RSA-OAEP' },
    publicKey,
    new TextEncoder().encode(hash),
  )

  // Return as Base64 — this is what appears in the network payload
  return btoa(String.fromCharCode(...new Uint8Array(encrypted)))
}
