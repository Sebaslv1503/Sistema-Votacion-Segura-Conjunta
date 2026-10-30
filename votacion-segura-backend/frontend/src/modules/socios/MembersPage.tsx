import { useDeferredValue, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { FileUp, Pencil, Plus, Search, UserRoundCheck, Users } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { Button, Card, EmptyState, ErrorNotice, LoadingBlock, Modal, PageTitle, StatusBadge } from '../../components/ui'
import { useToast } from '../../providers/ToastProvider'
import { readableApiError } from '../../services/apiClient'
import { socioService, type CreateMember, type UpdateMember } from '../../services/socioService'
import type { Member } from '../../shared/types'
import { formatNumber } from '../../shared/format'

const memberSchema = z.object({
  numeroSocio: z.string().trim().min(1, 'Ingresa el número de socio.'),
  nombres: z.string().trim().min(1, 'Ingresa los nombres.'),
  apellidos: z.string().trim().min(1, 'Ingresa los apellidos.'),
  correoElectronico: z.union([z.literal(''), z.email('Ingresa un correo válido.')]).optional(),
})
type MemberFormData = z.infer<typeof memberSchema>

export function MembersPage() {
  const [term, setTerm] = useState('')
  const search = useDeferredValue(term.trim())
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<Member | 'new' | null>(null)
  const [importOpen, setImportOpen] = useState(false)
  const queryClient = useQueryClient()
  const toast = useToast()
  const query = useQuery({ queryKey: ['members', page, search], queryFn: () => socioService.list({ page, size: 10, q: search || undefined }), gcTime: 0 })

  const save = useMutation({
    mutationFn: (data: MemberFormData) => editing === 'new'
      ? socioService.create({ ...data, correoElectronico: data.correoElectronico || undefined } satisfies CreateMember)
      : socioService.update(editing!.id, { nombres: data.nombres, apellidos: data.apellidos, correoElectronico: data.correoElectronico || undefined } satisfies UpdateMember),
    onSuccess: () => { toast(editing === 'new' ? 'Socio registrado.' : 'Datos del socio actualizados.', 'success'); setEditing(null); void queryClient.invalidateQueries({ queryKey: ['members'] }) },
  })
  const changeStatus = useMutation({
    mutationFn: ({ member, eligible }: { member: Member; eligible: boolean }) => socioService.setStatus(member.id, eligible ? 'INHABILITADO' : 'HABILITADO', true),
    onSuccess: (_data, vars) => { toast(vars.eligible ? 'Habilitación retirada.' : 'Socio habilitado.', 'success'); void queryClient.invalidateQueries({ queryKey: ['members'] }) },
  })
  const importMembers = useMutation({
    mutationFn: (file: File) => socioService.importCsv(file),
    onSuccess: (result) => { toast(`${formatNumber(result.sociosImportados)} socios importados.`, 'success'); setImportOpen(false); void queryClient.invalidateQueries({ queryKey: ['members'] }) },
  })

  return <>
    <PageTitle eyebrow="GESTIÓN DEL PADRÓN" title="Socios" description="Administra los datos mínimos del padrón autorizado y el estado de habilitación electoral."
      actions={<><Button variant="secondary" onClick={() => setImportOpen(true)}><FileUp size={15} /> Importar CSV</Button><Button onClick={() => setEditing('new')}><Plus size={16} /> Registrar socio</Button></>} />
    <div className="notice notice-info" style={{ marginBottom: 16 }}><Users size={17} /><span>El padrón almacena identificador, nombre, correo opcional y habilitación. No solicita datos personales innecesarios.</span></div>
    <Card>
      <div className="toolbar"><div><strong style={{ color: '#314b68', fontSize: 12 }}>Padrón electoral</strong><div className="table-secondary">{query.isSuccess ? `${formatNumber(query.data.totalElements)} registros` : 'Resultados desde el backend'}</div></div><div className="toolbar-right"><label className="search-box"><Search size={15} /><input className="text-input" type="search" value={term} onChange={(event) => { setTerm(event.target.value); setPage(0) }} placeholder="Buscar por socio o nombre" aria-label="Buscar socios" /></label></div></div>
      {query.isPending ? <LoadingBlock label="Consultando padrón" /> : query.isError ? <ErrorNotice message={readableApiError(query.error)} /> : query.data.content.length === 0 ? <EmptyState title="No hay coincidencias" detail="Prueba con otro número de socio o nombre, o registra un socio nuevo." /> : <>
        <div className="data-table-wrap"><table><thead><tr><th>Número de socio</th><th>Nombre completo</th><th>Correo</th><th>Habilitación</th><th>Registro</th><th>Acciones</th></tr></thead><tbody>
          {query.data.content.map((member) => {
            const eligible = member.estadoHabilitacion === 'HABILITADO' && member.activo
            return <tr key={member.id}><td><strong>{member.numeroSocio}</strong></td><td>{member.nombres} {member.apellidos}</td><td>{member.correoElectronico || '—'}</td><td><StatusBadge state={member.estadoHabilitacion} tone={eligible ? 'success' : member.estadoHabilitacion === 'INHABILITADO' ? 'danger' : 'warning'} /></td><td><StatusBadge state={member.activo ? 'Activo' : 'Inactivo'} tone={member.activo ? 'neutral' : 'danger'} /></td><td><div className="table-actions"><button className="table-action" onClick={() => setEditing(member)}><Pencil size={13} /> Editar</button><button className="table-action" disabled={changeStatus.isPending} onClick={() => changeStatus.mutate({ member, eligible })}><UserRoundCheck size={13} /> {eligible ? 'Inhabilitar' : 'Habilitar'}</button></div></td></tr>
          })}
        </tbody></table></div>
        <div className="pagination"><span>Página {query.data.number + 1} de {Math.max(query.data.totalPages, 1)}</span><div className="pagination-controls"><Button variant="secondary" disabled={query.data.first || query.isFetching} onClick={() => setPage((value) => Math.max(0, value - 1))}>Anterior</Button><Button variant="secondary" disabled={query.data.last || query.isFetching} onClick={() => setPage((value) => value + 1)}>Siguiente</Button></div></div>
      </>}
      {changeStatus.isError && <div style={{ marginTop: 12 }}><ErrorNotice message={readableApiError(changeStatus.error)} /></div>}
    </Card>

    {editing && <Modal title={editing === 'new' ? 'Registrar socio' : 'Editar información permitida'} onClose={() => { setEditing(null); save.reset() }}>
      <MemberForm member={editing === 'new' ? undefined : editing} submitting={save.isPending} error={save.isError ? readableApiError(save.error) : ''} onCancel={() => setEditing(null)} onSave={(data) => save.mutate(data)} />
    </Modal>}
    {importOpen && <CsvImportModal busy={importMembers.isPending} error={importMembers.isError ? readableApiError(importMembers.error, 'No se importó el archivo. El backend procesa el lote como una sola transacción.') : ''} onClose={() => { setImportOpen(false); importMembers.reset() }} onImport={(file) => importMembers.mutate(file)} />}
  </>
}

function MemberForm({ member, submitting, error, onCancel, onSave }: { member?: Member; submitting: boolean; error: string; onCancel: () => void; onSave: (data: MemberFormData) => void }) {
  const { register, handleSubmit, formState: { errors } } = useForm<MemberFormData>({ resolver: zodResolver(memberSchema), defaultValues: { numeroSocio: member?.numeroSocio || '', nombres: member?.nombres || '', apellidos: member?.apellidos || '', correoElectronico: member?.correoElectronico || '' } })
  return <form className="form-stack" noValidate onSubmit={handleSubmit(onSave)}>
    <div className="form-grid">
      <div className="field"><label htmlFor="member-number">Número de socio</label><input className="text-input" id="member-number" disabled={Boolean(member)} {...register('numeroSocio')} />{errors.numeroSocio && <small className="field-error">{errors.numeroSocio.message}</small>}{member && <small className="field-hint">El número de socio es inmutable.</small>}</div>
      <div className="field"><label htmlFor="member-email">Correo electrónico (opcional)</label><input className="text-input" type="email" id="member-email" autoComplete="email" {...register('correoElectronico')} />{errors.correoElectronico && <small className="field-error">{errors.correoElectronico.message}</small>}</div>
      <div className="field"><label htmlFor="member-first">Nombres</label><input className="text-input" id="member-first" autoComplete="given-name" {...register('nombres')} />{errors.nombres && <small className="field-error">{errors.nombres.message}</small>}</div>
      <div className="field"><label htmlFor="member-last">Apellidos</label><input className="text-input" id="member-last" autoComplete="family-name" {...register('apellidos')} />{errors.apellidos && <small className="field-error">{errors.apellidos.message}</small>}</div>
    </div>
    {error && <ErrorNotice message={error} />}
    <div className="form-actions"><Button type="button" variant="ghost" onClick={onCancel}>Cancelar</Button><Button type="submit" disabled={submitting}>{submitting ? 'Guardando…' : member ? 'Guardar cambios' : 'Registrar socio'}</Button></div>
  </form>
}

function CsvImportModal({ busy, error, onClose, onImport }: { busy: boolean; error: string; onClose: () => void; onImport: (file: File) => void }) {
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<string[][]>([])
  const [previewError, setPreviewError] = useState('')
  const readFile = async (selected: File | null) => {
    setFile(selected); setPreview([]); setPreviewError('')
    if (!selected) return
    if (!selected.name.toLowerCase().endsWith('.csv')) { setPreviewError('Selecciona un archivo .csv.'); setFile(null); return }
    try {
      const text = await selected.text()
      const rows = text.split(/\r?\n/).filter((line) => line.trim()).slice(0, 6).map((line) => parsePreviewLine(line))
      if (rows.length && rows[0][0]?.replace(/^\uFEFF/, '').toLowerCase() !== 'numerosocio') rows.unshift(['numeroSocio', 'nombres', 'apellidos', 'correoElectronico'])
      setPreview(rows)
    } catch { setPreviewError('No se pudo leer el archivo seleccionado.'); setFile(null) }
  }
  return <Modal title="Importar socios desde CSV" onClose={onClose}>
    <div className="form-stack">
      <p className="muted-copy" style={{ margin: 0 }}>Formato esperado: <code>numeroSocio,nombres,apellidos,correoElectronico</code>. Si un registro duplicado invalida el lote, el backend lo rechaza completo.</p>
      <div className="field"><label htmlFor="csv-file">Archivo CSV</label><input className="text-input" id="csv-file" type="file" accept=".csv,text/csv" onChange={(event) => void readFile(event.target.files?.[0] || null)} /></div>
      {previewError && <ErrorNotice title="Archivo no válido" message={previewError} />}
      {preview.length > 0 && <div><strong style={{ color: '#53667d', fontSize: 10 }}>Vista previa · hasta 5 filas</strong><div className="data-table-wrap" style={{ marginTop: 8 }}><table><thead><tr>{preview[0].map((name, index) => <th key={index}>{name}</th>)}</tr></thead><tbody>{preview.slice(1).map((row, index) => <tr key={index}>{[0, 1, 2, 3].map((column) => <td key={column}>{row[column] || '—'}</td>)}</tr>)}</tbody></table></div></div>}
      {error && <ErrorNotice title="Importación no completada" message={error} />}
      <div className="form-actions"><Button variant="ghost" onClick={onClose}>Cancelar</Button><Button disabled={!file || busy || previewError !== ''} onClick={() => file && onImport(file)}><FileUp size={14} />{busy ? 'Importando…' : 'Confirmar importación'}</Button></div>
    </div>
  </Modal>
}

function parsePreviewLine(line: string) {
  const result: string[] = []; let value = ''; let quote = false
  for (let i = 0; i < line.length; i++) {
    const current = line[i]
    if (current === '"') {
      if (quote && line[i + 1] === '"') { value += '"'; i++ } else quote = !quote
    } else if (current === ',' && !quote) { result.push(value.trim()); value = '' } else value += current
  }
  result.push(value.trim()); return result
}
