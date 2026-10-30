import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { LoaderCircle, ShieldAlert } from 'lucide-react'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'ghost' | 'danger' }
export function Button({ variant = 'primary', className = '', ...props }: ButtonProps) {
  return <button className={`button button-${variant} ${className}`} {...props} />
}

export function PageTitle({ eyebrow, title, description, actions }: { eyebrow?: string; title: string; description?: string; actions?: ReactNode }) {
  return <div className="page-title-row"><div>{eyebrow && <p className="eyebrow">{eyebrow}</p>}<h1>{title}</h1>{description && <p className="page-description">{description}</p>}</div>{actions && <div className="page-actions">{actions}</div>}</div>
}

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) { return <section className={`card ${className}`}>{children}</section> }

export function LoadingBlock({ label = 'Cargando información' }: { label?: string }) {
  return <div className="loading-block" role="status"><LoaderCircle size={18} className="spin" />{label}</div>
}

export function ErrorNotice({ message, title = 'No se pudo completar' }: { message: string; title?: string }) {
  return <div className="notice notice-danger" role="alert"><ShieldAlert size={18} /><div><strong>{title}</strong><p>{message}</p></div></div>
}

export function EmptyState({ title, detail, action }: { title: string; detail: string; action?: ReactNode }) {
  return <div className="empty-state"><div className="empty-mark"><span /></div><h3>{title}</h3><p>{detail}</p>{action}</div>
}

export function StatusBadge({ state, tone }: { state: string; tone?: 'success' | 'warning' | 'danger' | 'neutral' }) {
  const type = tone || (['ABIERTA', 'HABILITADO', 'PUBLICADA', 'VOTADO'].includes(state) ? 'success' : ['PAUSADA', 'PENDIENTE', 'CIERRE_SOLICITADO', 'PROGRAMADA'].includes(state) ? 'warning' : ['INHABILITADO', 'BAJA'].includes(state) ? 'danger' : 'neutral')
  return <span className={`status-badge badge-${type}`}><i />{state.replaceAll('_', ' ').toLowerCase().replace(/\b\p{L}/gu, (letter) => letter.toUpperCase())}</span>
}

export function Modal({ title, children, onClose }: { title: string; children: ReactNode; onClose: () => void }) {
  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
    <section className="modal" role="dialog" aria-modal="true" aria-label={title}>
      <div className="modal-heading"><h2>{title}</h2><button className="icon-button" aria-label="Cerrar ventana" onClick={onClose}>×</button></div>
      {children}
    </section>
  </div>
}
