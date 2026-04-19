import { useState, useCallback } from 'react'
import './App.css'

const DEFAULT_CREATE_PAYLOAD = {
  firstName: 'Mark',
  lastName: 'Cooper',
  personalCode: '60001010007',
  loanPeriodMonths: 36,
  interestMargin: 2.1,
  baseInterestRate: 4.0,
  loanAmount: 12000,
}

const STATUS_LABELS = {
  IN_REVIEW: { label: 'In Review', className: 'pill pill--review' },
  APPROVED: { label: 'Approved', className: 'pill pill--approved' },
  REJECTED: { label: 'Rejected', className: 'pill pill--rejected' },
  PENDING: { label: 'Pending', className: 'pill pill--pending' },
}

async function apiRequest(path, options = {}) {
  const response = await fetch(path, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {}),
    },
    ...options,
  })

  const rawText = await response.text()
  let body = null

  if (rawText) {
    try {
      body = JSON.parse(rawText)
    } catch {
      body = rawText
    }
  }

  return {
    ok: response.ok,
    status: response.status,
    statusText: response.statusText,
    body,
  }
}

function StatusPill({ status }) {
  const config = STATUS_LABELS[status] ?? { label: status, className: 'pill pill--pending' }
  return <span className={config.className}>{config.label}</span>
}

function CopyButton({ value }) {
  const [copied, setCopied] = useState(false)

  const handleCopy = () => {
    navigator.clipboard.writeText(value).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    })
  }

  return (
    <button className="btn-copy" onClick={handleCopy} type="button" title="Copy to clipboard">
      {copied ? '✓' : 'Copy'}
    </button>
  )
}

function formatEuro(value) {
  const amount = Number(value ?? 0)
  return `€${amount.toFixed(2)}`
}

function formatScheduleDate(value) {
  if (!value) {
    return '—'
  }

  const parsedDate = new Date(value)
  if (Number.isNaN(parsedDate.getTime())) {
    return String(value)
  }

  return parsedDate.toLocaleDateString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function ResponseCard({ title, result }) {
  return (
    <section className="response-card">
      <h3>{title}</h3>
      {!result ? (
        <p className="muted">No request sent yet.</p>
      ) : (
        <>
          <p>
            <strong>Status:</strong> {result.status} {result.statusText}
          </p>
          <pre>{JSON.stringify(result.body, null, 2) || 'No response body'}</pre>
        </>
      )}
    </section>
  )
}

function PaymentScheduleTable({ schedule }) {
  const [open, setOpen] = useState(true)

  if (!Array.isArray(schedule) || schedule.length === 0) {
    return <p className="muted">No payment schedule available.</p>
  }

  const totalInterest = schedule.reduce((sum, row) => sum + Number(row.interestAmount ?? row.interest ?? 0), 0)
  const totalPaid = schedule.reduce((sum, row) => sum + Number(row.paymentAmount ?? row.payment ?? 0), 0)

  return (
    <div className="schedule-wrapper">
      <button
        className="schedule-toggle"
        onClick={() => setOpen((v) => !v)}
        type="button"
      >
        <span>{open ? '▾' : '▸'} Payment schedule</span>
        <span className="muted">{schedule.length} payments</span>
      </button>

      {open && (
        <div className="schedule-scroll">
          <table className="schedule-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Date</th>
                <th className="num">Principal</th>
                <th className="num">Interest</th>
                <th className="num">Payment</th>
                <th className="num">Balance</th>
              </tr>
            </thead>
            <tbody>
              {schedule.map((row, i) => (
                <tr key={i} className={i === 0 ? 'schedule-row--first' : ''}>
                  <td>{i + 1}</td>
                  <td>{formatScheduleDate(row.paymentDate ?? row.date)}</td>
                  <td className="num">{formatEuro(row.principalAmount ?? row.principal)}</td>
                  <td className="num">{formatEuro(row.interestAmount ?? row.interest)}</td>
                  <td className="num">{formatEuro(row.paymentAmount ?? row.payment)}</td>
                  <td className="num">{formatEuro(row.remainingBalance ?? row.balance)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="schedule-totals">
        <span>Total interest: <strong>{formatEuro(totalInterest)}</strong></span>
        <span>Total paid: <strong>{formatEuro(totalPaid)}</strong></span>
      </div>
    </div>
  )
}

function SummaryCard({ title, application, actions, footer }) {
  const monthlyPayment =
    Array.isArray(application.paymentSchedule) && application.paymentSchedule.length > 0
      ? (application.paymentSchedule[0].paymentAmount ?? application.paymentSchedule[0].payment ?? null)
      : null

  return (
    <article className="summary-card">
      <div className="summary-header">
        <strong>{title}</strong>
        <StatusPill status={application.status} />
      </div>

      <div className="summary-grid">
        <div>
          <span className="summary-label">Application ID</span>
          <div className="summary-value mono id-row">
            <span>{application.id}</span>
            {application.id && <CopyButton value={application.id} />}
          </div>
        </div>
        <div>
          <span className="summary-label">Applicant</span>
          <div className="summary-value">
            {application.firstName} {application.lastName}
          </div>
        </div>
        <div>
          <span className="summary-label">Personal code</span>
          <div className="summary-value mono">{application.personalCode}</div>
        </div>
        <div>
          <span className="summary-label">Loan amount</span>
          <div className="summary-value">€{application.loanAmount?.toLocaleString()}</div>
        </div>
        <div>
          <span className="summary-label">Loan period</span>
          <div className="summary-value">{application.loanPeriodMonths} months</div>
        </div>
        {monthlyPayment !== null && (
          <div>
            <span className="summary-label">Monthly payment</span>
            <div className="summary-value">€{monthlyPayment.toFixed(2)}</div>
          </div>
        )}
      </div>

      <PaymentScheduleTable schedule={application.paymentSchedule} />

      {actions ? <div className="summary-actions">{actions}</div> : null}
      {footer ? <div className="summary-footer">{footer}</div> : null}
    </article>
  )
}

function CreateApplicationResult({ result }) {
  if (!result) {
    return <p className="muted">No request sent yet.</p>
  }

  if (!result.ok) {
    return <ResponseCard title="Create response" result={result} />
  }

  if (!result.body || Array.isArray(result.body)) {
    return <ResponseCard title="Create response" result={result} />
  }

  return <SummaryCard title="Created application" application={result.body} />
}

function InReviewApplicationCard({ application, onApprove, onReject, disabled }) {
  const [rejectionReason, setRejectionReason] = useState('')
  const canDeny = !disabled && rejectionReason.trim().length > 0

  return (
    <SummaryCard
      title="In-review application"
      application={application}
      actions={
        <>
          <button disabled={disabled} onClick={() => onApprove(application.id)} type="button" className="btn-approve">
            Approve
          </button>
          <div className="inline-action-group">
            <input
              type="text"
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
              placeholder="Reason for denial"
            />
            <button disabled={!canDeny} onClick={() => onReject(application.id, rejectionReason.trim())} type="button" className="btn-reject">
              Deny
            </button>
          </div>
        </>
      }
    />
  )
}

function InReviewApplications({ result, onApprove, onReject, disabled }) {
  if (!result) {
    return <p className="muted">No request sent yet.</p>
  }

  if (!result.ok) {
    return <ResponseCard title="In-review response" result={result} />
  }

  if (!Array.isArray(result.body) || result.body.length === 0) {
    return <p className="muted">No applications are currently in review.</p>
  }

  return (
    <div className="summary-list">
      {result.body.map((application) => (
        <InReviewApplicationCard
          key={application.id}
          application={application}
          onApprove={onApprove}
          onReject={onReject}
          disabled={disabled}
        />
      ))}
    </div>
  )
}

function AllApplicationsTable({ result }) {
  if (!result) {
    return <p className="muted">No request sent yet.</p>
  }

  if (!result.ok) {
    return <ResponseCard title="All applications response" result={result} />
  }

  if (!Array.isArray(result.body) || result.body.length === 0) {
    return <p className="muted">No applications found.</p>
  }

  return (
    <div className="all-apps-list">
      {result.body.map((app) => (
        <div key={app.id} className="app-row">
          <div className="app-row-info">
            <span className="app-row-name">{app.firstName} {app.lastName}</span>
            <span className="app-row-meta">
              €{app.loanAmount?.toLocaleString()} · {app.loanPeriodMonths} months
            </span>
            {app.status === 'REJECTED' && app.rejectionReason && (
              <span className="app-row-meta">Reason: {app.rejectionReason}</span>
            )}
            <span className="app-row-id mono">{app.id}</span>
          </div>
          <div className="app-row-right">
            <StatusPill status={app.status} />
          </div>
        </div>
      ))}
    </div>
  )
}

export default function App() {
  const [createPayload, setCreatePayload] = useState(DEFAULT_CREATE_PAYLOAD)
  const [createResult, setCreateResult] = useState(null)
  const [allAppsResult, setAllAppsResult] = useState(null)
  const [inReviewResult, setInReviewResult] = useState(null)
  const [decisionResult, setDecisionResult] = useState(null)
  const [isLoading, setIsLoading] = useState(false)

  const updatePayloadField = (field, value) => {
    setCreatePayload((current) => ({ ...current, [field]: value }))
  }

  const withLoading = async (action) => {
    setIsLoading(true)
    try {
      await action()
    } finally {
      setIsLoading(false)
    }
  }

  const refreshAllApplications = useCallback(async () => {
    const result = await apiRequest('/api/loan-applications')
    setAllAppsResult(result)
    return result
  }, [])

  const refreshInReviewApplications = useCallback(async () => {
    const result = await apiRequest('/api/loan-applications/in-review')
    setInReviewResult(result)
    return result
  }, [])

  const handleCreate = async (event) => {
    event.preventDefault()
    await withLoading(async () => {
      const result = await apiRequest('/api/loan-applications', {
        method: 'POST',
        body: JSON.stringify({
          ...createPayload,
          loanPeriodMonths: Number(createPayload.loanPeriodMonths),
          interestMargin: Number(createPayload.interestMargin),
          baseInterestRate: Number(createPayload.baseInterestRate),
          loanAmount: Number(createPayload.loanAmount),
        }),
      })
      setCreateResult(result)

      // Auto-refresh lists after creating
      if (result.ok) {
        await Promise.all([refreshAllApplications(), refreshInReviewApplications()])
      }
    })
  }

  const handleLoadAll = async () => {
    await withLoading(refreshAllApplications)
  }

  const handleLoadInReview = async () => {
    await withLoading(refreshInReviewApplications)
  }

  const handleApproveInReview = async (applicationId) => {
    await withLoading(async () => {
      const result = await apiRequest(`/api/loan-applications/${applicationId}/approve`, {
        method: 'POST',
      })
      setDecisionResult(result)
      await Promise.all([refreshInReviewApplications(), refreshAllApplications()])
    })
  }

  const handleRejectInReview = async (applicationId, reason) => {
    await withLoading(async () => {
      const result = await apiRequest(`/api/loan-applications/${applicationId}/reject`, {
        method: 'POST',
        body: JSON.stringify({ reason }),
      })
      setDecisionResult(result)
      await Promise.all([refreshInReviewApplications(), refreshAllApplications()])
    })
  }

  function DecisionResult({ result }) {
    if (!result) {
      return <p className="muted">No decision made yet.</p>
    }

    if (!result.ok || !result.body) {
      return <ResponseCard title="Decision error" result={result} />
    }

    const { id, status, rejectionReason } = result.body

    return (
        <section className="response-card">
          <h3>Latest decision</h3>

          <p>
            <strong>Status:</strong>{' '}
            {status === 'APPROVED' ? 'Approved' : 'Rejected'}
          </p>

          <p className="mono">
            <strong>Application ID:</strong> {id}
          </p>

          {status === 'REJECTED' && rejectionReason && (
              <p className="muted">
                <strong>Reason:</strong> {rejectionReason}
              </p>
          )}
        </section>
    )
  }

  return (
    <main className="page">
      <header>
        <h1>Loan calculator</h1>
      </header>



      {/* Create */}
      <section className="card">
        <h2>Create Loan Application</h2>
        <form onSubmit={handleCreate}>
          <label>
            First name
            <input
              value={createPayload.firstName}
              onChange={(e) => updatePayloadField('firstName', e.target.value)}
            />
          </label>
          <label>
            Last name
            <input
              value={createPayload.lastName}
              onChange={(e) => updatePayloadField('lastName', e.target.value)}
            />
          </label>
          <label>
            Personal code (11 digits)
            <input
              value={createPayload.personalCode}
              onChange={(e) => updatePayloadField('personalCode', e.target.value)}
            />
          </label>
          <label>
            Loan period (months)
            <input
              type="number"
              min="6"
              max="360"
              value={createPayload.loanPeriodMonths}
              onChange={(e) => updatePayloadField('loanPeriodMonths', e.target.value)}
            />
          </label>
          <label>
            Interest margin (%)
            <input
              type="number"
              step="0.05"
              value={createPayload.interestMargin}
              onChange={(e) => updatePayloadField('interestMargin', e.target.value)}
            />
          </label>
          <label>
            Base interest rate (%)
            <input
              type="number"
              step="0.05"
              value={createPayload.baseInterestRate}
              onChange={(e) => updatePayloadField('baseInterestRate', e.target.value)}
            />
          </label>
          <label>
            Loan amount (€)
            <input
              type="number"
              step="100"
              min="5000"
              value={createPayload.loanAmount}
              onChange={(e) => updatePayloadField('loanAmount', e.target.value)}
            />
          </label>
          <button disabled={isLoading} type="submit">
            {isLoading ? 'Sending...' : 'Submit'}
          </button>
        </form>
        <CreateApplicationResult result={createResult} />
      </section>

      {/* All Applications */}
      <section className="card">
        <h2>All Applications</h2>
        <button disabled={isLoading} onClick={handleLoadAll} type="button">
          {isLoading ? 'Sending...' : 'Load all applications'}
        </button>
        <AllApplicationsTable
            result={allAppsResult}
        />
      </section>

      {/* IN_REVIEW */}
      <section className="card">
        <h2>Applications in review</h2>
        <button disabled={isLoading} onClick={handleLoadInReview} type="button">
          {isLoading ? 'Sending...' : 'Load applications in review'}
        </button>
        <InReviewApplications
          result={inReviewResult}
          onApprove={handleApproveInReview}
          onReject={handleRejectInReview}
          disabled={isLoading}
        />
        <DecisionResult result={decisionResult} />
      </section>

      {/* Docs */}
      <section className="card">
        <h2>Backend docs</h2>
        <p>
          Swagger UI: <a href="http://localhost:8080/swagger-ui.html">http://localhost:8080/swagger-ui.html</a>
        </p>
        <p>
          OpenAPI JSON: <a href="http://localhost:8080/v3/api-docs">http://localhost:8080/v3/api-docs</a>
        </p>
      </section>
    </main>
  )
}

