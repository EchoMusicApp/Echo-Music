import test from 'node:test'
import assert from 'node:assert'

function isAllowedStreamingDomain(urlStr: string): boolean {
  try {
    const parsed = new URL(urlStr)
    const host = parsed.hostname.toLowerCase()
    return (
      host === 'youtube.com' ||
      host.endsWith('.youtube.com') ||
      host === 'googlevideo.com' ||
      host.endsWith('.googlevideo.com')
    )
  } catch {
    return false
  }
}

function isAllowedAppleDomain(urlStr: string): boolean {
  try {
    const parsed = new URL(urlStr)
    const host = parsed.hostname.toLowerCase()
    return (
      host === 'music.apple.com' ||
      host === 'beta.music.apple.com' ||
      host.endsWith('.apple.com') ||
      host.endsWith('.mzstatic.com') ||
      host.endsWith('.apple-dns.net')
    )
  } catch {
    return false
  }
}

function isAllowedTidalDomain(urlStr: string): boolean {
  try {
    const parsed = new URL(urlStr)
    const host = parsed.hostname.toLowerCase()
    return host === 'tidal.com' || host.endsWith('.tidal.com')
  } catch {
    return false
  }
}

test('Security: isAllowedStreamingDomain strictly validates YouTube & GoogleVideo domains and blocks SSRF targets', () => {
  // Allowed
  assert.strictEqual(isAllowedStreamingDomain('https://rr1---sn-uxa-i54e.googlevideo.com/videoplayback?id=123'), true)
  assert.strictEqual(isAllowedStreamingDomain('https://music.youtube.com/watch?v=123'), true)
  assert.strictEqual(isAllowedStreamingDomain('https://youtube.com/watch?v=123'), true)

  // Blocked SSRF & internal targets
  assert.strictEqual(isAllowedStreamingDomain('http://localhost:8080/admin'), false)
  assert.strictEqual(isAllowedStreamingDomain('http://127.0.0.1/metadata'), false)
  assert.strictEqual(isAllowedStreamingDomain('http://169.254.169.254/latest/meta-data'), false)
  assert.strictEqual(isAllowedStreamingDomain('https://evil-googlevideo.com/payload'), false)
  assert.strictEqual(isAllowedStreamingDomain('https://googlevideo.com.attacker.com/payload'), false)
  assert.strictEqual(isAllowedStreamingDomain('not-a-url'), false)
})

test('Security: isAllowedAppleDomain blocks spoofed subdomains and unapproved endpoints', () => {
  assert.strictEqual(isAllowedAppleDomain('https://music.apple.com/us/album/test/123'), true)
  assert.strictEqual(isAllowedAppleDomain('https://is1-ssl.mzstatic.com/image/thumb/123.jpg'), true)

  // Blocked
  assert.strictEqual(isAllowedAppleDomain('https://apple.com.attacker.com/test'), false)
  assert.strictEqual(isAllowedAppleDomain('https://evil-apple.com/test'), false)
  assert.strictEqual(isAllowedAppleDomain('http://127.0.0.1:3000'), false)
})

test('Security: isAllowedTidalDomain blocks non-Tidal origins', () => {
  assert.strictEqual(isAllowedTidalDomain('https://listen.tidal.com/album/123'), true)
  assert.strictEqual(isAllowedTidalDomain('https://api.tidal.com/v1/tracks/123'), true)

  // Blocked
  assert.strictEqual(isAllowedTidalDomain('https://tidal.com.phishing.net'), false)
  assert.strictEqual(isAllowedTidalDomain('https://evil-tidal.com'), false)
})
