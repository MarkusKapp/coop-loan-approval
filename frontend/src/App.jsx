import { useState, useCallback } from 'react'
import './App.css'

const DEFAULT_CREATE_PAYLOAD = { firstName: 'Mark', lastName: 'Cooper', personalCode: '60001010007', loanPeriodMonths: 36, interestMargin: 2.1, baseInterestRate: 4.0, loanAmount: 12000 }
const STATUS_LABELS = {
  IN_REVIEW: { label: 'In Review', className: 'pill pill--review' },
  APPROVED: { label: 'Approved', className: 'pill pill--approved' },
  REJECTED: { label: 'Rejected', className: 'pill pill--rejected' },
  PENDING: { label: 'Pending', className: 'pill pill--pending' },
}

const apiRequest = async (path, options = {}) => {
  const res = await fetch(path, { headers: { 'Content-Type': 'application/json', ...options.headers }, ...options })
  const text = await res.text()
  let body = text
  try { body = JSON.parse(text) } catch {}
  return { ok: res.ok, status: res.status, statusText: res.statusText, body }
}



const formatEuro = (v) => `€${Number(v ?? 0).toFixed(2)}`
const StatusPill = ({ status }) => {
  const { label, className } = STATUS_LABELS[status] ?? { label: status, className: 'pill pill--pending' }
  return <span className={className}>{label}</span>
}

const CopyButton = ({ value }) => {
  const [copied, setCopied] = useState(false)
  const handleCopy = () => navigator.clipboard.writeText(value).then(() => { setCopied(true); setTimeout(() => setCopied(false), 1500) })
  return <button className="btn-copy" onClick={handleCopy} type="button">{copied ? '✓' : 'Copy'}</button>
}

const ResponseCard = ({ title, result }) => (
    <section className="response-card">
      <h3>{title}</h3>
      <p><strong>Status:</strong> {result.status} {result.statusText}</p>
      <pre>{JSON.stringify(result.body, null, 2) || 'No response body'}</pre>
    </section>
)

const ResultView = ({ result, children, title = "Response", emptyMsg = "No request sent yet." }) => {
  if (!result) return <p className="muted">{emptyMsg}</p>
  if (!result.ok) return <ResponseCard title={title} result={result} />
  return children
}

const PaymentScheduleTable = ({ schedule }) => {
  const [open, setOpen] = useState(true)
  if (!Array.isArray(schedule) || !schedule.length) return <p className="muted">No payment schedule available.</p>
    const sum = (k) => schedule.reduce((s, r) => s + Number(r[k] ?? 0), 0)
// ja kasutus:
    sum('interest')
    sum('totalPayment')
  return (
      <div className="schedule-wrapper">
        <button className="schedule-toggle" onClick={() => setOpen(!open)} type="button">
          <span>{open ? '▾' : '▸'} Payment schedule</span> <span className="muted">{schedule.length} payments</span>
        </button>
        {open && (
            <div className="schedule-scroll">
              <table className="schedule-table">
                <thead><tr><th>#</th><th>Date</th><th className="num">Principal</th><th className="num">Interest</th><th className="num">Payment</th></tr></thead>
                <tbody>{schedule.map((r, i) => (
                    <tr key={i} className={i === 0 ? 'schedule-row--first' : ''}>
                      <td>{i + 1}</td>
                      <td>{r.paymentDate ? new Date(r.paymentDate).toLocaleDateString(undefined, { day: '2-digit', month: 'short', year: 'numeric' }) : '—'}</td>
                      <td className="num">{formatEuro(r.principal)}</td>
                      <td className="num">{formatEuro(r.interest)}</td>
                      <td className="num">{formatEuro(r.totalPayment)}</td>
                    </tr>
                ))}</tbody>
              </table>
            </div>
        )}
        <div className="schedule-totals">
          <span>Total interest: <strong>{formatEuro(sum('interestAmount', 'interest'))}</strong></span>
          <span>Total paid: <strong>{formatEuro(sum('paymentAmount', 'payment'))}</strong></span>
        </div>
      </div>
  )
}

const SummaryCard = ({ title, application: app, actions, footer }) => {
    const monthly = app.paymentSchedule?.[0]?.totalPayment
  return (
      <article className="summary-card">
        <div className="summary-header"><strong>{title}</strong><StatusPill status={app.status} /></div>
        <div className="summary-grid">
          {[ { l: 'Application ID', v: <div className="id-row"><span>{app.id}</span>{app.id && <CopyButton value={app.id} />}</div>, m: 1 },
            { l: 'Applicant', v: `${app.firstName} ${app.lastName}` },
            { l: 'Personal code', v: app.personalCode, m: 1 },
            { l: 'Loan amount', v: `€${app.loanAmount?.toLocaleString()}` },
            { l: 'Loan period', v: `${app.loanPeriodMonths} months` },
            ...(monthly ? [{ l: 'Monthly payment', v: `€${monthly.toFixed(2)}` }] : [])
          ].map((f, i) => <div key={i}><span className="summary-label">{f.l}</span><div className={`summary-value ${f.m ? 'mono' : ''}`}>{f.v}</div></div>)}
        </div>
        <PaymentScheduleTable schedule={app.paymentSchedule} />
        {actions && <div className="summary-actions">{actions}</div>}
        {footer && <div className="summary-footer">{footer}</div>}
      </article>
  )
}

export default function App() {
  const [createPayload, setPayload] = useState(DEFAULT_CREATE_PAYLOAD)
    const [results, setResults] = useState({ create: null, all: null, review: null, decision: null, regenerate: null, lookup: null })
  const [loading, setLoading] = useState(false)

  const call = async (path, key, opt = {}) => {
    setLoading(true)
    const res = await apiRequest(path, opt)
    setResults(prev => ({ ...prev, [key]: res }))
    setLoading(false)
    return res
  }

    const handleLookup = (id) =>
        call(`/api/loan-applications/${id}`, 'lookup')

    const handleRegenerateSchedule = (id, params) =>
        call(`/api/loan-applications/${id}/regenerate-schedule`, 'regenerate', {
            method: 'PUT',
            body: JSON.stringify(params),
        }).then(refresh)

  const refresh = () => Promise.all([call('/api/loan-applications', 'all'), call('/api/loan-applications/in-review', 'review')])

  const handleCreate = async (e) => {
    e.preventDefault()
    const res = await call('/api/loan-applications', 'create', { method: 'POST', body: JSON.stringify({
        ...createPayload, loanPeriodMonths: +createPayload.loanPeriodMonths, interestMargin: +createPayload.interestMargin,
        baseInterestRate: +createPayload.baseInterestRate, loanAmount: +createPayload.loanAmount
      })})
    if (res.ok) refresh()
  }

  const handleDecision = (id, type, reason) => call(`/api/loan-applications/${id}/${type}`, 'decision', {
    method: 'POST', body: reason ? JSON.stringify({ reason }) : null
  }).then(refresh)

  return (
      <main className="page">
        <header><h1>Loan calculator</h1></header>

        <section className="card">
          <h2>Create Loan Application</h2>
          <form onSubmit={handleCreate}>
            {[ ['First name', 'firstName'], ['Last name', 'lastName'], ['Personal code (11 digits)', 'personalCode'],
              ['Loan period (months)', 'loanPeriodMonths', 'number'], ['Interest margin (%)', 'interestMargin', 'number', '0.05'],
              ['Base interest rate (%)', 'baseInterestRate', 'number', '0.05'], ['Loan amount (€)', 'loanAmount', 'number', '100']
            ].map(([label, key, type, step]) => (
                <label key={key}>{label}<input type={type} step={step} value={createPayload[key]} onChange={e => setPayload({...createPayload, [key]: e.target.value})} /></label>
            ))}
            <button disabled={loading} type="submit">{loading ? 'Sending...' : 'Submit'}</button>
          </form>
          <ResultView result={results.create} title="Create response">
            {!results.create?.body || Array.isArray(results.create.body) ? <ResponseCard title="Create response" result={results.create} /> : <SummaryCard title="Created application" application={results.create.body} />}
          </ResultView>
        </section>

          <section className="card">
              <h2>Find application by ID</h2>
              <LookupById loading={loading} onLookup={handleLookup} />
              <ResultView result={results.lookup} title="Lookup response" emptyMsg="Enter an ID to look up.">
                  {results.lookup?.body && !Array.isArray(results.lookup.body)
                      ? <SummaryCard title="Application" application={results.lookup.body} />
                      : <ResponseCard title="Lookup response" result={results.lookup} />
                  }
              </ResultView>
          </section>

        <section className="card">
          <h2>All Applications</h2>
          <button disabled={loading} onClick={() => call('/api/loan-applications', 'all')}>{loading ? 'Sending...' : 'Load all applications'}</button>
          <ResultView result={results.all} title="All applications response">
            {(!Array.isArray(results.all?.body) || !results.all.body.length) ? <p className="muted">No applications found.</p> : (
                <div className="all-apps-list">{results.all.body.map(app => (
                    <div key={app.id} className="app-row">
                      <div className="app-row-info">
                        <span className="app-row-name">{app.firstName} {app.lastName}</span>
                        <span className="app-row-meta">€{app.loanAmount?.toLocaleString()} · {app.loanPeriodMonths} months</span>
                        {app.status === 'REJECTED' && app.rejectionReason && <span className="app-row-meta">Reason: {app.rejectionReason}</span>}
                        <span className="app-row-id mono">{app.id}</span>
                      </div>
                      <div className="app-row-right"><StatusPill status={app.status} /></div>
                    </div>
                ))}</div>
            )}
          </ResultView>
        </section>

        <section className="card">
          <h2>Applications in review</h2>
          <button disabled={loading} onClick={() => call('/api/loan-applications/in-review', 'review')}>{loading ? 'Sending...' : 'Load applications in review'}</button>
          <ResultView result={results.review} title="In-review response" emptyMsg="No applications are currently in review.">
            <div className="summary-list">{results.review?.body?.map(app => (
                <SummaryCard key={app.id} title="In-review application" application={app} actions={
                    <ReviewActions
                        app={app}
                        loading={loading}
                        onApprove={() => handleDecision(app.id, 'approve')}
                        onReject={r => handleDecision(app.id, 'reject', r)}
                        onRegenerateSchedule={handleRegenerateSchedule}
                    />                }/>
            ))}</div>
          </ResultView>
          <ResultView result={results.decision} title="Decision error" emptyMsg="No decision made yet.">
            <section className="response-card">
              <h3>Latest decision</h3>
              <p><strong>Status:</strong> {results.decision?.body?.status === 'APPROVED' ? 'Approved' : 'Rejected'}</p>
              <p className="mono"><strong>Application ID:</strong> {results.decision?.body?.id}</p>
              {results.decision?.body?.status === 'REJECTED' && <p className="muted"><strong>Reason:</strong> {results.decision?.body?.rejectionReason}</p>}
            </section>
          </ResultView>
        </section>

        <section className="card">
          <h2>Backend docs</h2>
          <p>Swagger UI: <a href="http://localhost:8080/swagger-ui.html">.../swagger-ui.html</a></p>
          <p>OpenAPI JSON: <a href="http://localhost:8080/v3/api-docs">.../v3/api-docs</a></p>
        </section>
      </main>
  )
}

function ReviewActions({ app, loading, onApprove, onReject, onRegenerateSchedule }) {
    const [reason, setReason] = useState('')
    const [editing, setEditing] = useState(false)

    if (editing) return (
        <RegenerateForm
            app={app}
            loading={loading}
            onSave={async (id, params) => {
                await onRegenerateSchedule(id, params)
                setEditing(false)
            }}
            onCancel={() => setEditing(false)}
        />
    )

    return (
        <>
            <button disabled={loading} onClick={onApprove} className="btn-approve">Approve</button>
            <div className="inline-action-group">
                <input value={reason} onChange={e => setReason(e.target.value)} placeholder="Reason for denial" />
                <button disabled={loading || !reason.trim()} onClick={() => onReject(reason.trim())} className="btn-reject">Deny</button>
            </div>
            <button disabled={loading} onClick={() => setEditing(true)} className="btn-secondary">Edit schedule</button>
        </>
    )
}

function RegenerateForm({ app, loading, onSave, onCancel }) {
    const [params, setParams] = useState({
        interestMargin: app.interestMargin,
        baseInterestRate: app.baseInterestRate,
        loanPeriodMonths: app.loanPeriodMonths,
    })

    const handleSave = () => onSave(app.id, params)

    return (
        <div className="regenerate-form">
            {[
                ['Interest margin (%)', 'interestMargin', '0.05'],
                ['Base interest rate (%)', 'baseInterestRate', '0.05'],
                ['Loan period (months)', 'loanPeriodMonths', '1'],
            ].map(([label, key, step]) => (
                <label key={key}>{label}
                    <input
                        type="number"
                        step={step}
                        value={params[key]}
                        onChange={e => setParams({ ...params, [key]: +e.target.value })}
                    />
                </label>
            ))}
            <div className="regenerate-actions">
                <button disabled={loading} onClick={handleSave} className="btn-approve">
                    {loading ? 'Saving...' : 'Save & regenerate'}
                </button>
                <button onClick={onCancel} className="btn-secondary">Cancel</button>
            </div>
        </div>
    )

}

function LookupById({ loading, onLookup }) {
    const [id, setId] = useState('')

    return (
        <div className="inline-action-group">
            <input
                value={id}
                onChange={e => setId(e.target.value)}
                placeholder="Application UUID"
                className="mono"
            />
            <button
                disabled={loading || !id.trim()}
                onClick={() => onLookup(id.trim())}
            >
                {loading ? 'Loading...' : 'Look up'}
            </button>
        </div>
    )
}
