import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router'
import { candidaturaService } from '../../services/candidaturaService'
import { eleccionService } from '../../services/eleccionService'
import { participacionService } from '../../services/participacionService'
import { votacionService } from '../../services/votacionService'
import { Button, Card, ErrorNotice, LoadingBlock, PageTitle, Modal } from '../../components/ui'
import { readableApiError } from '../../services/apiClient'
import { useToast } from '../../providers/ToastProvider'

export function VotingPage() {
  const { electionId = '' } = useParams(); const toast = useToast(); const [selected, setSelected] = useState<string | null>(null); const [confirm, setConfirm] = useState(false); const [receipt, setReceipt] = useState(false); const [credential, setCredential] = useState<string | null>(null)
  const election = useQuery({ queryKey: ['elections'], queryFn: () => eleccionService.listOpen(), select: rows => rows.find(e => e.id === electionId) })
  const participation = useQuery({ queryKey: ['participation', electionId], queryFn: () => participacionService.myState(electionId) })
  const candidates = useQuery({ queryKey: ['candidates', electionId], queryFn: () => candidaturaService.list(electionId) })
  const issue = useMutation({ mutationFn: (_candidateId: string) => votacionService.issueCredential(electionId), onSuccess: (value, candidateId) => { setCredential(value); cast.mutate({ token: value, candidateId }) }, onError: e => toast(readableApiError(e), 'error') })
  const cast = useMutation({ mutationFn: ({ token, candidateId }: { token: string; candidateId: string }) => votacionService.cast(token, candidateId), onSuccess: () => { setReceipt(true); setConfirm(false) }, onError: e => toast(readableApiError(e, 'No fue posible confirmar el voto. Consulta tu estado antes de volver a intentarlo.'), 'error') })
  const submit = () => { if (credential && selected) cast.mutate({ token: credential, candidateId: selected }); else if (selected) issue.mutate(selected) }
  if (election.isLoading || participation.isLoading || candidates.isLoading) return <LoadingBlock label="Verificando elección y habilitación" />
  if (election.isError || participation.isError || candidates.isError) return <ErrorNotice message={readableApiError(election.error || participation.error || candidates.error)} />
  if (receipt || participation.data?.participationState === 'VOTADO') return <><PageTitle eyebrow="Participación" title="Voto registrado" /><Card><h2>Tu participación fue registrada</h2><p>Por privacidad, esta pantalla no conserva ni muestra la opción seleccionada.</p></Card></>
  if (!election.data || election.data.estado !== 'ABIERTA') return <><PageTitle title="Votación no disponible" /><Card><p>Esta elección no está abierta para recibir votos.</p></Card></>
  if (!participation.data?.eligible) return <><PageTitle title="No habilitado para votar" /><Card><p>El sistema indica que tu cuenta no está habilitada para esta elección.</p></Card></>
  return <><PageTitle eyebrow="Votación segura" title={election.data.nombre} description="Selecciona una candidatura. Tu selección no se guarda en almacenamiento local ni se incluye en la confirmación." />{participation.data.participationState === 'PENDIENTE' && <ErrorNotice title="Credencial pendiente" message="Ya existe una credencial emitida. Contacta al custodio antes de solicitar otra." />}
    <div className="candidate-grid">{candidates.data?.filter(c => c.activo).map(c => <button className={`candidate-choice ${selected === c.id ? 'selected' : ''}`} key={c.id} onClick={() => setSelected(c.id)} disabled={issue.isPending || cast.isPending} aria-pressed={selected === c.id}><strong>{c.nombre}</strong><span>{c.propuesta || c.descripcion || 'Candidatura'}</span></button>)}</div>
    <Button disabled={!selected || participation.data.participationState !== 'SIN_CREDENCIAL' || issue.isPending || cast.isPending} onClick={() => setConfirm(true)}>Continuar para confirmar</Button>
    {confirm && <Modal title="Confirmar participación" onClose={() => setConfirm(false)}><p>Se emitirá la credencial de un solo uso y se registrará el voto para la opción elegida. La selección no se mostrará en el comprobante.</p><Button disabled={issue.isPending || cast.isPending} onClick={submit}>{issue.isPending || cast.isPending ? 'Registrando…' : 'Confirmar voto'}</Button></Modal>}
  </>
}
