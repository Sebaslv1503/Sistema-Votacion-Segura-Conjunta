import { useQuery } from '@tanstack/react-query'
import { systemService } from '../../services/systemService'
import { Card, ErrorNotice, LoadingBlock, PageTitle, StatusBadge } from '../../components/ui'
import { readableApiError } from '../../services/apiClient'
export function AdminPage() { const q = useQuery({ queryKey: ['health'], queryFn: systemService.health, refetchInterval: 30_000 }); return <><PageTitle eyebrow="Administración técnica" title="Estado del servicio" description="Información disponible en el endpoint público de salud." />{q.isLoading ? <LoadingBlock /> : q.isError ? <ErrorNotice message={readableApiError(q.error)} /> : q.data && <Card><StatusBadge state={q.data.status} /><pre>{JSON.stringify(q.data.components || {}, null, 2)}</pre></Card>}<Card><p>La API actual no expone gestión de usuarios, roles ni estado detallado del padrón desde administración.</p></Card></> }
