import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { auditoriaService } from '../../services/auditoriaService'
import { Card, ErrorNotice, LoadingBlock, PageTitle, Button } from '../../components/ui'
import { readableApiError } from '../../services/apiClient'

export function AuditPage() {
  const [actorId, setActorId] = useState(''); const [page, setPage] = useState(0)
  const query = useQuery({ queryKey: ['audit', actorId, page], queryFn: () => auditoriaService.list({ page, size: 20, actorId: actorId || undefined }) })
  return <><PageTitle eyebrow="Trazabilidad" title="Auditoría" description="Eventos devueltos por el servicio de auditoría." /><Card><label>Filtrar por actor ID<input value={actorId} onChange={e => { setActorId(e.target.value); setPage(0) }} /></label></Card>{query.isLoading ? <LoadingBlock /> : query.isError ? <ErrorNotice message={readableApiError(query.error)} /> : query.data && <><div className="table-wrap"><table><thead><tr><th>Fecha</th><th>Actor</th><th>Rol</th><th>Acción</th><th>Resultado</th><th>Correlación</th></tr></thead><tbody>{query.data.content.map(e => <tr key={e.id}><td>{new Date(e.fecha).toLocaleString()}</td><td>{e.actorId || 'Sistema'}</td><td>{e.rolActor || '—'}</td><td>{e.accion}</td><td>{e.resultado}</td><td>{e.correlationId || '—'}</td></tr>)}</tbody></table></div><div className="button-row"><Button variant="secondary" disabled={query.data.first} onClick={() => setPage(p => p - 1)}>Anterior</Button><span>Página {page + 1} de {query.data.totalPages || 1}</span><Button variant="secondary" disabled={query.data.last} onClick={() => setPage(p => p + 1)}>Siguiente</Button></div><p className="muted-copy">El backend actual solo permite filtrar por actor y paginar; no se ofrecen filtros por fechas, tipo ni exportación.</p></>}</>
}
