import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { zodResolver } from '@hookform/resolvers/zod'
import { Eye, EyeOff, Fingerprint, LockKeyhole, ShieldCheck, Vote } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { useAuth } from '../../providers/AuthProvider'
import { readableApiError } from '../../services/apiClient'
import { Button, ErrorNotice } from '../../components/ui'

const loginSchema = z.object({
  username: z.string().trim().min(1, 'Ingresa tu identificador de acceso.'),
  password: z.string().min(1, 'Ingresa tu contraseña.'),
})
type LoginForm = z.infer<typeof loginSchema>

export function LoginPage() {
  const { login, loading, session } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [visible, setVisible] = useState(false)
  const [error, setError] = useState('')
  const { register, handleSubmit, formState: { errors } } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) })

  useEffect(() => { if (session) navigate('/', { replace: true }) }, [session, navigate])
  if (session) return null
  const submit = handleSubmit(async (values) => {
    setError('')
    try {
      await login(values.username, values.password)
      const from = (location.state as { from?: string } | null)?.from || '/'
      navigate(from, { replace: true })
    } catch (reason) {
      setError(readableApiError(reason, 'No se pudo iniciar sesión. Revisa tus credenciales e inténtalo nuevamente.'))
    }
  })

  return <div className="login-screen">
    <section className="login-aside">
      <div className="login-brand"><div className="brand-emblem brand-emblem-light"><Vote size={22} /></div><div><strong>ASOCIACIÓN<br />PROFESIONAL</strong><span>PORTAL ELECTORAL</span></div></div>
      <div className="login-aside-copy"><p className="eyebrow eyebrow-light">PARTICIPACIÓN INSTITUCIONAL</p><h1>Una decisión<br />colectiva, con<br /><em>confianza.</em></h1><p>Un espacio seguro y transparente para participar en las elecciones de nuestra asociación.</p></div>
      <div className="login-aside-foot"><ShieldCheck size={17} /><span>Conexión protegida · Voto secreto</span></div>
      <div className="login-pattern" aria-hidden="true"><span /><span /><span /><span /><span /></div>
    </section>
    <main className="login-main">
      <div className="login-card-wrap">
        <div className="login-mobile-brand"><div className="brand-emblem"><Vote size={20} /></div><strong>Portal electoral</strong></div>
        <div className="login-heading"><p className="eyebrow">ACCESO SEGURO</p><h2>Bienvenido</h2><p>Inicia sesión para acceder a tu espacio institucional.</p></div>
        {error && <ErrorNotice message={error} title="No fue posible acceder" />}
        <form onSubmit={submit} className="form-stack" noValidate>
          <div className="field"><label htmlFor="username">Identificador de acceso</label><div className="input-wrap"><Fingerprint size={17} /><input id="username" autoComplete="username" placeholder="Ingresa tu identificador" aria-invalid={Boolean(errors.username)} {...register('username')} /></div>{errors.username && <small className="field-error">{errors.username.message}</small>}</div>
          <div className="field"><label htmlFor="password">Contraseña</label><div className="input-wrap"><LockKeyhole size={17} /><input id="password" type={visible ? 'text' : 'password'} autoComplete="current-password" placeholder="Ingresa tu contraseña" aria-invalid={Boolean(errors.password)} {...register('password')} /><button type="button" className="reveal-button" aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'} onClick={() => setVisible((value) => !value)}>{visible ? <EyeOff size={17} /> : <Eye size={17} />}</button></div>{errors.password && <small className="field-error">{errors.password.message}</small>}</div>
          <Button type="submit" className="login-submit" disabled={loading}>{loading ? 'Verificando…' : 'Ingresar'}<span aria-hidden="true">→</span></Button>
        </form>
        <div className="login-footnote"><ShieldCheck size={16} /><span>Tus credenciales se validan de forma segura y no se guardan en este dispositivo.</span></div>
        <p className="login-recovery-note">¿Necesitas ayuda para acceder? Comunícate con el área de soporte institucional.</p>
      </div>
      <div className="login-copyright">Asociación Profesional <span>·</span> Sistema de votación segura</div>
    </main>
  </div>
}
