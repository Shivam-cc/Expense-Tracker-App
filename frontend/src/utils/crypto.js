/**
 * Hashes a plain-text password using SHA-256 via the browser's native
 * Web Crypto API before it is sent over the network.
 *
 * The backend then BCrypts the received hash, so the actual plain-text
 * password never leaves the browser.
 *
 * Returns a lowercase hex string (64 chars).
 */
export async function hashPassword(plainText) {
  const encoder = new TextEncoder()
  const data = encoder.encode(plainText)
  const hashBuffer = await crypto.subtle.digest('SHA-256', data)
  const hashArray = Array.from(new Uint8Array(hashBuffer))
  return hashArray.map((b) => b.toString(16).padStart(2, '0')).join('')
}
