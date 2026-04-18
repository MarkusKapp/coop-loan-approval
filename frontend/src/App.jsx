import { useState } from 'react'
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

const REJECTION_REASONS = [
  'CUSTOMER_TOO_OLD',
  'HIGH_RISK_PROFILE',
  'FAILED_MANUAL_REVIEW',
  'INCOMPLETE_APPLICATION',
  'OTHER',
]

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

function SummaryCard({ title, application, actions, footer }) {
  return (
    <article className="summary-card">
      <div className="summary-header">
        <strong>{title}</strong>
        <span className="pill">{application.status}</span>
      </div>
      <div className="summary-grid">
        <div>
          <span className="summary-label">Application ID</span>
          <div className="summary-value mono">{application.id}</div>
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
          <div className="summary-value">€{application.loanAmount}</div>
        </div>
        <div>
          <span className="summary-label">Loan period</span>
          <div className="summary-value">{application.loanPeriodMonths} months</div>
        </div>
        <div>
          <span className="summary-label">Payment plan</span>
          <div className="summary-value">
            {Array.isArray(application.paymentSchedule) ? application.paymentSchedule.length : 0} payments
          </div>
        </div>
      </div>
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

  return (
    <SummaryCard title="Created application" application={result.body} />
  )
}

function InReviewApplicationCard({ application, onApprove, onReject, disabled }) {
  const [rejectionReason, setRejectionReason] = useState(REJECTION_REASONS[0])

  return (
    <SummaryCard
      title="In-review application"
      application={application}
      actions={
        <>
          <button disabled={disabled} onClick={() => onApprove(application.id)} type="button">
            Approve
          </button>
          <div className="inline-action-group">
            <select value={rejectionReason} onChange={(event) => setRejectionReason(event.target.value)}>
              {REJECTION_REASONS.map((reason) => (
                <option key={reason} value={reason}>
                  {reason}
                </option>
              ))}
            </select>
            <button disabled={disabled} onClick={() => onReject(application.id, rejectionReason)} type="button">
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

function App() {
  const [createPayload, setCreatePayload] = useState(DEFAULT_CREATE_PAYLOAD)
  const [createResult, setCreateResult] = useState(null)
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
    })
  }

  const refreshInReviewApplications = async () => {
    const result = await apiRequest('/api/loan-applications/in-review')
    setInReviewResult(result)
    return result
  }

  const handleLoadInReview = async () => {
    await withLoading(async () => {
      await refreshInReviewApplications()
    })
  }

  const handleApproveInReview = async (applicationId) => {
    await withLoading(async () => {
      const result = await apiRequest(`/api/loan-applications/${applicationId}/approve`, {
        method: 'POST',
      })
      setDecisionResult(result)
      await refreshInReviewApplications()
    })
  }

  const handleRejectInReview = async (applicationId, reason) => {
    await withLoading(async () => {
      const result = await apiRequest(`/api/loan-applications/${applicationId}/reject`, {
        method: 'POST',
        body: JSON.stringify({ reason }),
      })
      setDecisionResult(result)
      await refreshInReviewApplications()
    })
  }

  return (
    <main className="page">
      <header>
        <h1>Loan API Test Client</h1>
        <p>Use this page to test create, approve, and reject endpoints quickly.</p>
      </header>

      <section className="card">
        <h2>Create Loan Application</h2>
        <form onSubmit={handleCreate}>
          <label>
            First name
            <input
              value={createPayload.firstName}
              onChange={(event) => updatePayloadField('firstName', event.target.value)}
            />
          </label>
          <label>
            Last name
            <input
              value={createPayload.lastName}
              onChange={(event) => updatePayloadField('lastName', event.target.value)}
            />
          </label>
          <label>
            Personal code (11 digits)
            <input
              value={createPayload.personalCode}
              onChange={(event) => updatePayloadField('personalCode', event.target.value)}
            />
          </label>
          <label>
            Loan period (months)
            <input
              type="number"
              min="6"
              max="360"
              value={createPayload.loanPeriodMonths}
              onChange={(event) => updatePayloadField('loanPeriodMonths', event.target.value)}
            />
          </label>
          <label>
            Interest margin (%)
            <input
              type="number"
              step="0.001"
              value={createPayload.interestMargin}
              onChange={(event) => updatePayloadField('interestMargin', event.target.value)}
            />
          </label>
          <label>
            Base interest rate (%)
            <input
              type="number"
              step="0.001"
              value={createPayload.baseInterestRate}
              onChange={(event) => updatePayloadField('baseInterestRate', event.target.value)}
            />
          </label>
          <label>
            Loan amount
            <input
              type="number"
              step="0.01"
              min="5000"
              value={createPayload.loanAmount}
              onChange={(event) => updatePayloadField('loanAmount', event.target.value)}
            />
          </label>
          <button disabled={isLoading} type="submit">
            {isLoading ? 'Sending...' : 'POST /api/loan-applications'}
          </button>
        </form>
        <CreateApplicationResult result={createResult} />
      </section>

      <section className="card">
        <h2>IN_REVIEW Applications</h2>
        <button disabled={isLoading} onClick={handleLoadInReview} type="button">
          {isLoading ? 'Sending...' : 'GET /api/loan-applications/in-review'}
        </button>
        <InReviewApplications
          result={inReviewResult}
          onApprove={handleApproveInReview}
          onReject={handleRejectInReview}
          disabled={isLoading}
        />
        <ResponseCard title="Latest decision response" result={decisionResult} />
      </section>

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

export default App
