import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { AuthProvider } from '../../providers/AuthProvider'
import { LoginPage } from './LoginPage'

const { loginMock } = vi.hoisted(() => ({ loginMock: vi.fn() }))
vi.mock('../../services/authService', () => ({ authService: { login: (...args: unknown[]) => loginMock(...args), logout: vi.fn() } }))

const makeToken = (role: string) => `header.${btoa(JSON.stringify({ role })).replace(/=/g, '')}.sig`
function setup() { return render(<MemoryRouter initialEntries={['/login']}><AuthProvider><Routes><Route path="/login" element={<LoginPage />} /><Route path="/" element={<h1>Inicio autenticado</h1>} /></Routes></AuthProvider></MemoryRouter>) }

describe('inicio de sesión', () => {
  afterEach(() => cleanup())
  beforeEach(() => { loginMock.mockReset() })
  it('informa un error de autenticación sin revelar credenciales', async () => {
    loginMock.mockRejectedValue(new Error('invalid credentials'))
    setup()
    fireEvent.change(screen.getByLabelText('Identificador de acceso'), { target: { value: 'socio1' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'incorrecta' } })
    fireEvent.click(screen.getByRole('button', { name: /Ingresar/ }))
    expect(await screen.findByText('No se pudo iniciar sesión. Revisa tus credenciales e inténtalo nuevamente.')).toBeInTheDocument()
    expect(screen.queryByText('invalid credentials')).not.toBeInTheDocument()
  })
  it('acepta login y redirige tras recibir un token con rol válido', async () => {
    loginMock.mockResolvedValue({ accessToken: makeToken('SOCIO_VOTANTE'), refreshToken: 'refresh', tokenType: 'Bearer', expiresInMs: 60_000 })
    setup()
    fireEvent.change(screen.getByLabelText('Identificador de acceso'), { target: { value: 'socio1' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'correcta' } })
    fireEvent.click(screen.getByRole('button', { name: /Ingresar/ }))
    await waitFor(() => expect(screen.getByText('Inicio autenticado')).toBeInTheDocument())
  })
})
