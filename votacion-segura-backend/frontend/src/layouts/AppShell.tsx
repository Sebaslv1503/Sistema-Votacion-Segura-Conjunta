import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router'
import {
  Activity, BookOpenCheck, Building2, ChevronRight, ClipboardList, FileCheck2,
  LayoutDashboard, LifeBuoy, LogOut, Menu, ShieldCheck, Users, Vote, X,
} from 'lucide-react'
import { useAuth } from '../providers/AuthProvider'
import { roleLabels } from '../shared/types'
import { initials } from '../shared/format'

const navItems = [
  { label: 'Resumen', to: '/', icon: LayoutDashboard, roles: null },
  { label: 'Elecciones', to: '/elecciones', icon: Vote, roles: null },
  { label: 'Padrón electoral', to: '/socios', icon: Users, roles: ['GESTOR_PADRON', 'ADMINISTRADOR_TECNICO'] },
  { label: 'Auditoría', to: '/auditoria', icon: ClipboardList, roles: ['AUDITOR_INDEPENDIENTE', 'ADMINISTRADOR_TECNICO'] },
  { label: 'Resultados oficiales', to: '/resultados', icon: FileCheck2, roles: ['SOCIO_VOTANTE', 'CUSTODIO_ELECTORAL', 'AUDITOR_INDEPENDIENTE', 'ADMINISTRADOR_TECNICO'] },
  { label: 'Administración técnica', to: '/administracion', icon: Activity, roles: ['ADMINISTRADOR_TECNICO'] },
  { label: 'Centro de ayuda', to: '/soporte', icon: LifeBuoy, roles: ['SOPORTE'] },
] as const

export function AppShell() {
  const { session, logout } = useAuth()
  const navigate = useNavigate()
  const [mobileOpen, setMobileOpen] = useState(false)
  if (!session) return null

  const filtered = navItems.filter((item) => item.roles === null || item.roles.includes(session.role as never))
  const handleLogout = async () => {
    try { await logout() } finally { navigate('/login', { replace: true }) }
  }

  return <div className="app-frame">
    {mobileOpen && <button className="mobile-scrim" aria-label="Cerrar menú" onClick={() => setMobileOpen(false)} />}
    <aside className={`sidebar ${mobileOpen ? 'sidebar-open' : ''}`}>
      <div className="brand-lockup">
        <div className="brand-emblem"><Building2 size={20} strokeWidth={1.8} /></div>
        <div><strong>Asociación<br />Profesional</strong><span>PORTAL ELECTORAL</span></div>
        <button className="icon-button sidebar-close" aria-label="Cerrar menú" onClick={() => setMobileOpen(false)}><X size={17} /></button>
      </div>

      <div className="sidebar-caption">MENÚ PRINCIPAL</div>
      <nav className="sidebar-nav" aria-label="Navegación principal">
        {filtered.map(({ label, to, icon: Icon }) => <NavLink key={to} end={to === '/'} to={to} onClick={() => setMobileOpen(false)} className={({ isActive }) => `nav-link ${isActive ? 'nav-link-active' : ''}`}>
          <Icon size={18} strokeWidth={1.8} /><span>{label}</span>{to === '/' && <ChevronRight className="nav-chevron" size={15} />}
        </NavLink>)}
      </nav>

      <div className="sidebar-privacy">
        <div className="privacy-icon"><ShieldCheck size={18} /></div>
        <strong>Privacidad electoral</strong>
        <p>Tu participación se registra por separado de la selección del voto.</p>
      </div>
      <div className="sidebar-foot"><span className="secure-dot" />Canal seguro <span>•</span> Sesión activa</div>
    </aside>

    <div className="main-column">
      <header className="topbar">
        <div className="topbar-left"><button className="icon-button mobile-menu" aria-label="Abrir menú" onClick={() => setMobileOpen(true)}><Menu size={20} /></button>
          <div className="breadcrumbs"><span>Portal electoral</span><ChevronRight size={14} /><strong>{navItems.find((item) => item.to === window.location.pathname)?.label || 'Resumen'}</strong></div>
        </div>
        <div className="topbar-right"><span className="topbar-date"><BookOpenCheck size={15} /> Sistema institucional</span>
          <div className="user-chip"><div className="user-avatar">{initials(session.username)}</div><div className="user-info"><strong>{session.username}</strong><span>{roleLabels[session.role]}</span></div><button className="icon-button logout-button" title="Cerrar sesión" aria-label="Cerrar sesión" onClick={handleLogout}><LogOut size={17} /></button></div>
        </div>
      </header>
      <main className="page-content"><Outlet /></main>
      <footer className="app-footer"><span>© 2026 Asociación Profesional</span><span className="footer-separator" /> <span>Votación electrónica segura</span></footer>
    </div>
  </div>
}
