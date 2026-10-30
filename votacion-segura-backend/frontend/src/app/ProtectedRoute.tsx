import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../providers/AuthProvider'
import type { Role } from '../shared/types'

export function ProtectedRoute({ roles }: { roles?: Role[] }) {
  const { session } = useAuth()
  const location = useLocation()
  if (!session) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (roles && !roles.includes(session.role)) return <Navigate to="/acceso-denegado" replace />
  return <Outlet />
}
