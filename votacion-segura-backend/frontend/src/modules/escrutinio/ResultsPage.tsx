import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { escrutinioService } from '../../services/escrutinioService'
import { Button, Card, ErrorNotice, LoadingBlock, PageTitle, Modal } from '../../components/ui'
import { readableApiError } from '../../services/apiClient'
import { useAuth } from '../../providers/AuthProvider'
import { useToast } from '../../providers/ToastProvider'

export function ResultsPage() {
  const [params, setParams] = useSearchParams(); const [id, setId] = useState(params.get('eleccion') || ''); const [confirm, setConfirm] = useState<'generate'|'approve'|'publish'|null>(null)
  const { session } = useAuth(); const toast = useToast(); const canManage = ['CUSTODIO_ELECTORAL','ADMINISTRADOR_TECNICO'].includes(session?.role || '')
  useEffect(() => { setId(params.get('eleccion') || '') }, [params])
  const query = useQuery({ queryKey: ['result', id], queryFn: () => escrutinioService.getPublished(id), enabled: !!id })
  const mutation = useMutation({ mutationFn: async (op: 'generate'|'approve'|'publish') => op === 'generate' ? escrutinioService.generate(id) : op === 'approve' ? escrutinioService.approve(id) : escrutinioService.publish(id), onSuccess: () => { toast('Operación de escrutinio completada', 'success'); setConfirm(null); query.refetch() }, onError: e => toast(readableApiError(e), 'error') })
  let chartData: Array<{ name: string; votos: number }> = []
  if (query.data) { try { const parsed = JSON.parse(query.data.conteosJson) as Record<string, number>; chartData = Object.entries(parsed).map(([name, votos]) => ({ name, votos })) } catch { chartData = [] } }
  return <><PageTitle eyebrow="Escrutinio oficial" title="Resultados electorales" description="Los resultados solo se consultan desde el endpoint oficial publicado." /><Card><form className="button-row" onSubmit={e => { e.preventDefault(); setParams({ eleccion: id }) }}><label>ID de elección<input value={id} onChange={e => setId(e.target.value)} required /></label><Button type="submit">Consultar publicación</Button></form></Card>
    {canManage && id && <div className="button-row">{(['generate','approve','publish'] as const).map(op => <Button key={op} variant={op === 'publish' ? 'danger' : 'secondary'} onClick={() => setConfirm(op)}>{op === 'generate' ? 'Generar escrutinio' : op === 'approve' ? 'Aprobar' : 'Publicar'}</Button>)}</div>}
    {!id ? <Card><p>Ingresa el identificador de una elección. El backend no tiene un endpoint para listar elecciones finalizadas.</p></Card> : query.isLoading ? <LoadingBlock /> : query.isError ? <ErrorNotice message={readableApiError(query.error, 'Aún no hay resultados publicados para esta elección.')} /> : query.data && <><div className="metric-grid"><Card><span>Votos contabilizados</span><strong>{query.data.totalVotos}</strong></Card><Card><span>Fecha de cierre</span><strong>{new Date(query.data.fechaCierre).toLocaleString()}</strong></Card><Card><span>Estado</span><strong>{query.data.estado}</strong></Card></div><Card><h2>Votos por candidatura</h2>{chartData.length ? <div className="chart-box"><ResponsiveContainer width="100%" height="100%"><BarChart data={chartData}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="name" /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="votos" fill="#356d62" radius={[7,7,0,0]} /></BarChart></ResponsiveContainer></div> : <p>El escrutinio no contiene un conteo JSON compatible para visualizar.</p>}<p>Hash: <code>{query.data.hash}</code></p></Card></>}
    {confirm && <Modal title="Confirmar acción de escrutinio" onClose={() => setConfirm(null)}><p>Esta acción será procesada por el backend para la elección {id}. Confirma que corresponde al paso vigente.</p><Button disabled={mutation.isPending} onClick={() => mutation.mutate(confirm)}>{mutation.isPending ? 'Procesando…' : 'Confirmar'}</Button></Modal>}
  </>
}
