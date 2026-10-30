import { describe, expect, it, beforeEach } from 'vitest'
import { clearMemoryTokens, getMemoryTokens, roleFromAccessToken, setMemoryTokens } from './sessionMemory'

const token = (role: string) => `header.${btoa(JSON.stringify({ role })).replace(/=/g, '')}.sig`
describe('sesión en memoria', () => {
  beforeEach(() => clearMemoryTokens())
  it('reconoce roles válidos del access token para navegación', () => expect(roleFromAccessToken(token('SOCIO_VOTANTE'))).toBe('SOCIO_VOTANTE'))
  it('rechaza claims faltantes, inválidos o tokens malformados', () => {
    expect(roleFromAccessToken(token('SUPERUSER'))).toBeNull()
    expect(roleFromAccessToken('bad-token')).toBeNull()
  })
  it('guarda tokens solo en memoria y permite borrarlos', () => {
    setMemoryTokens({ username: 'demo', role: 'SOCIO_VOTANTE', accessToken: 'a', refreshToken: 'r', tokenType: 'Bearer', expiresInMs: 1000 })
    expect(getMemoryTokens()?.refreshToken).toBe('r')
    clearMemoryTokens()
    expect(getMemoryTokens()).toBeNull()
  })
})
