import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router'
import { z } from 'zod'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { candidaturaService } from '../../services/candidaturaService'
import { Button, Card, ErrorNotice, LoadingBlock, PageTitle, StatusBadge } from '../../components/ui'
import { readableApiError } from '../../services/apiClient'
import { useToast } from '../../providers/ToastProvider'

const schema = z.object({ nombre: z.string().min(2), descripcion: z.string().optional(), propuesta: z.string().optional() })
type FormData = z.infer<typeof schema>
export function CandidatesPage() {
  const { electionId = '' } = useParams(); const toast = useToast(); const qc = useQueryClient(); const [editing, setEditing] = useState<string | null>(null)
  const query = useQuery({ queryKey: ['candidates', electionId], queryFn: () => candidaturaService.list(electionId), enabled: !!electionId })
  const form = useForm<FormData>({ resolver: zodResolver(schema), defaultValues: { nombre: '', descripcion: '', propuesta: '' } })
  const mutation = useMutation({ mutationFn: (data: FormData) => editing ? candidaturaService.update(editing, { ...data, activo: query.data?.find(c => c.id === editing)?.activo ?? true }) : candidaturaService.create(electionId, data), onSuccess: () => { toast('Candidatura guardada', 'success'); setEditing(null); form.reset(); qc.invalidateQueries({ queryKey: ['candidates', electionId] }) }, onError: (e) => toast(readableApiError(e), 'error') })
  const toggle = useMutation({ mutationFn: ({ id, active }: { id: string; active: boolean }) => { const c = query.data?.find(x => x.id === id)!; return candidaturaService.update(id, { nombre: c.nombre, descripcion: c.descripcion || '', propuesta: c.propuesta || '', activo: active }) }, onSuccess: () => qc.invalidateQueries({ queryKey: ['candidates', electionId] }), onError: e => toast(readableApiError(e), 'error') })
  return <><PageTitle eyebrow="Gestión electoral" title="Candidaturas" description="Registra y administra las opciones de esta elección." />
    {query.isLoading ? <LoadingBlock /> : query.isError ? <ErrorNotice message={readableApiError(query.error)} /> : <div className="content-grid"><Card><h2>{editing ? 'Editar candidatura' : 'Nueva candidatura'}</h2><form className="form-stack" onSubmit={form.handleSubmit(v => mutation.mutate(v))}><label>Nombre<input {...form.register('nombre')} /></label><label>Descripción<textarea {...form.register('descripcion')} /></label><label>Propuesta<textarea {...form.register('propuesta')} /></label><Button disabled={mutation.isPending}>{editing ? 'Guardar cambios' : 'Crear candidatura'}</Button></form></Card><div className="stack">{query.data?.map(c => <Card key={c.id}><div className="row-between"><h3>{c.nombre}</h3><StatusBadge state={c.activo ? 'ACTIVA' : 'INACTIVA'} /></div><p>{c.descripcion || 'Sin descripción'}</p><p>{c.propuesta || 'Sin propuesta registrada'}</p><div className="button-row"><Button variant="secondary" onClick={() => { setEditing(c.id); form.reset({ nombre: c.nombre, descripcion: c.descripcion || '', propuesta: c.propuesta || '' }) }}>Editar</Button><Button variant="ghost" onClick={() => toggle.mutate({ id: c.id, active: !c.activo })}>{c.activo ? 'Desactivar' : 'Activar'}</Button></div></Card>)}</div></div>}</>
}
