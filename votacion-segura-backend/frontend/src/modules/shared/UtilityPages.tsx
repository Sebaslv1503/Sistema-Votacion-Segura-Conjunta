import { Link } from 'react-router'
import { Card, PageTitle } from '../../components/ui'
export function AccessDeniedPage() { return <><PageTitle title="Acceso restringido" /><Card><p>Tu rol no tiene acceso a esta sección.</p><Link to="/">Volver al inicio</Link></Card></> }
export function NotFoundPage() { return <><PageTitle title="Página no encontrada" /><Card><p>La ruta solicitada no existe.</p><Link to="/">Ir al inicio</Link></Card></> }
