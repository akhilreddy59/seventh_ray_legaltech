import { useEffect, useRef, useState } from 'react';
import { supabase } from './supabase.js';

const API = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '');
const nextStatus = { DRAFT: 'REVIEW', REVIEW: 'APPROVED' };

async function request(path, options = {}) {
  if (!API) throw new Error('Set VITE_API_BASE_URL to the Spring Boot API URL.');
  if (!supabase) throw new Error('Set VITE_SUPABASE_URL and SUPABASE_ANON_KEY to enable sign-in.');
  const { data: { session } } = await supabase.auth.getSession();
  if (!session) throw new Error('Sign in to access your contracts.');
  const send = (token) => {
    const headers = new Headers(options.headers || {});
    headers.set('Authorization', `Bearer ${token}`);
    return fetch(`${API}${path}`, { ...options, headers });
  };
  let response = await send(session.access_token);
  if (response.status === 401) {
    const { data, error } = await supabase.auth.refreshSession();
    if (error || !data.session) {
      await supabase.auth.signOut();
      throw new Error('Your session expired. Sign in again.');
    }
    response = await send(data.session.access_token);
  }
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.error || 'Something went wrong. Please try again.');
  return body;
}

function date(value) { return new Date(value).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' }); }

export default function App() {
  const [session, setSession] = useState(null);
  const [authReady, setAuthReady] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [authBusy, setAuthBusy] = useState(false);
  const [authError, setAuthError] = useState('');
  const [contracts, setContracts] = useState([]);
  const [selected, setSelected] = useState(null);
  const [title, setTitle] = useState('');
  const [file, setFile] = useState(null);
  const fileInput = useRef(null);
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  async function loadContracts(keepId) {
    const data = await request('/contracts');
    setContracts(data.contracts);
    if (keepId) await openContract(keepId);
  }

  async function openContract(id) {
    const data = await request(`/contracts/${id}`);
    setSelected(data.contract);
    setAnswer('');
    setQuestion('');
  }

  useEffect(() => {
    if (!supabase) { setAuthReady(true); return undefined; }
    supabase.auth.getSession().then(({ data, error: sessionError }) => {
      if (sessionError) setAuthError(sessionError.message);
      setSession(data.session);
    }).catch((sessionError) => setAuthError(sessionError.message)).finally(() => setAuthReady(true));
    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, nextSession) => {
      setSession(nextSession);
      setAuthReady(true);
      if (!nextSession) { setContracts([]); setSelected(null); }
    });
    return () => subscription.unsubscribe();
  }, []);

  useEffect(() => {
    if (session?.user?.id) loadContracts().catch((err) => setError(err.message));
  }, [session?.user?.id]);

  async function signIn(event) {
    event.preventDefault();
    if (!supabase) return setAuthError('Sign-in is not configured yet. Add the Supabase URL and public key to the client environment.');
    setAuthBusy(true); setAuthError('');
    try {
      const { data, error: signInError } = await supabase.auth.signInWithPassword({ email: email.trim(), password });
      if (signInError) setAuthError(signInError.message);
      else { setSession(data.session); setPassword(''); }
    } catch (signInError) {
      setAuthError(signInError.message || 'Could not reach Supabase Auth. Try again.');
    } finally { setAuthBusy(false); }
  }

  async function signOut() {
    if (supabase) await supabase.auth.signOut();
    setSession(null);
    setContracts([]);
    setSelected(null);
  }

  async function upload(event) {
    event.preventDefault();
    if (!file) return setError('Choose a PDF or DOCX file to upload.');
    setBusy(true); setError(''); setNotice('');
    const form = new FormData(); form.append('file', file); if (title.trim()) form.append('title', title.trim());
    try {
      const data = await request('/contracts', { method: 'POST', body: form });
      setTitle(''); setFile(null);
      if (fileInput.current) fileInput.current.value = '';
      await loadContracts(data.contract.id);
      setNotice('Contract added to your register.');
    } catch (err) { setError(err.message); }
    finally { setBusy(false); }
  }

  async function advanceStatus() {
    setBusy(true); setError('');
    try { await request(`/contracts/${selected.id}/status`, { method: 'PATCH' }); await loadContracts(selected.id); }
    catch (err) { setError(err.message); }
    finally { setBusy(false); }
  }

  async function ask(event) {
    event.preventDefault(); if (!question.trim()) return;
    setBusy(true); setError(''); setAnswer('');
    try { const data = await request(`/contracts/${selected.id}/questions`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ question }) }); setAnswer(data.answer); }
    catch (err) { setError(err.message); }
    finally { setBusy(false); }
  }

  if (!authReady) return <main className="page auth-page"><div className="topbar"><a className="wordmark" href="#top">CM<span>.</span></a><div className="topbar-date">CONTRACT REGISTER</div></div><p className="auth-loading">Preparing your private workspace…</p></main>;

  if (!session) return <main className="page auth-page">
    <header className="topbar"><a className="wordmark" href="#top">CM<span>.</span></a><div className="topbar-note">A quiet place for your agreements</div><div className="topbar-date">PRIVATE CONTRACT REGISTER</div></header>
    <section className="auth-card">
      <div className="eyebrow">YOUR PRIVATE WORKSPACE</div>
      <h1>Welcome back.</h1>
      <p>Sign in to see and manage your contracts.</p>
      <form onSubmit={signIn}>
        <label className="field-label" htmlFor="login-email">Account email</label>
        <input id="login-email" type="email" autoComplete="username" required value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" />
        <label className="field-label" htmlFor="login-password">Password</label>
        <input id="login-password" type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Your password" />
        <button className="button button-dark auth-submit" disabled={authBusy || !supabase}>{authBusy ? 'Signing in…' : 'Sign in'} <span aria-hidden="true">↗</span></button>
      </form>
      {!supabase && <div className="auth-message">Sign-in is not configured. Add the Supabase URL and public key to the Vercel client environment, then redeploy.</div>}
      {authError && <div className="auth-message auth-message-error" role="alert">{authError}</div>}
      <div className="auth-note">Accounts are provided by your administrator.</div>
    </section>
    <footer className="footer"><span>CONTRACT MANAGER</span><span>PRIVATE, BY DESIGN.</span></footer>
  </main>;

  return <main className="page">
      <header className="topbar"><a className="wordmark" href="#top">CM<span>.</span></a><div className="topbar-note">A quiet place for your agreements</div><div className="account-menu"><span>{session?.user?.email}</span><button onClick={signOut}>Sign out</button></div></header>
    <section className="intro" id="top"><div className="eyebrow">YOUR DOCUMENTS, IN ORDER</div><h1>Contract Manager</h1><p>Keep the important details close, and the paperwork in its place.</p></section>
    <div className="workspace">
      <aside className="register">
        <div className="section-heading"><div><div className="eyebrow">THE REGISTER</div><h2>All contracts <span className="count">{contracts.length.toString().padStart(2, '0')}</span></h2></div></div>
        <form className="upload-card" onSubmit={upload}>
          <label className="field-label" htmlFor="contract-title">Add a contract</label>
          <input id="contract-title" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Title (optional)" maxLength="200" />
          <label className="file-picker"><span>{file ? file.name : 'Choose a PDF or DOCX'}</span><input ref={fileInput} type="file" accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document" onChange={(e) => setFile(e.target.files?.[0] || null)} /></label>
          <button className="button button-dark upload-button" disabled={busy}>{busy ? 'Please wait…' : 'Upload contract'} <span aria-hidden="true">↗</span></button>
          <div className="hint">PDF or DOCX · up to 10 MB</div>
        </form>
        <div className="contract-list">
          {contracts.length === 0 ? <div className="empty-list"><span className="empty-mark">—</span><p>Your register is empty.</p><small>Your first agreement can go here.</small></div> : contracts.map((item) => <button key={item.id} className={`contract-row ${selected?.id === item.id ? 'selected' : ''}`} onClick={() => openContract(item.id).catch((err) => setError(err.message))}>
            <span className="row-icon" aria-hidden="true">⌑</span><span className="row-copy"><strong>{item.title}</strong><small>{item.file_name}</small></span><span className={`status status-${item.status.toLowerCase()}`}>{item.status}</span>
          </button>)}
        </div>
        <div className="register-foot">{contracts.length} {contracts.length === 1 ? 'document' : 'documents'} on file</div>
      </aside>
      <section className="detail">
        {selected ? <>
          <div className="detail-head"><div><div className="eyebrow">CONTRACT DETAILS</div><h2>{selected.title}</h2><p className="file-meta">{selected.file_name}<span>·</span> Added {date(selected.created_at)}</p></div><span className={`status status-${selected.status.toLowerCase()}`}>{selected.status}</span></div>
          <div className="rule" />
          <section className="document-section"><div className="subhead"><h3>Document text</h3><span>EXTRACTED PREVIEW</span></div><div className="document-preview">{selected.extracted_text ? selected.extracted_text.slice(0, 5000) : 'No readable text could be extracted from this document.'}{selected.extracted_text?.length > 5000 ? '…' : ''}</div></section>
          <section className="workflow"><div><div className="eyebrow">NEXT STEP</div><h3>{nextStatus[selected.status] ? `Move to ${nextStatus[selected.status].toLowerCase()}` : 'Workflow complete'}</h3><p>{selected.status === 'DRAFT' ? 'Ready for a first review?' : selected.status === 'REVIEW' ? 'Has the review been completed?' : 'This contract has been approved.'}</p></div><button className="button button-outline" onClick={advanceStatus} disabled={busy || !nextStatus[selected.status]}>{nextStatus[selected.status] ? `Advance to ${nextStatus[selected.status]}` : 'Approved'} <span aria-hidden="true">→</span></button></section>
          <section className="qa"><div className="subhead"><div><div className="eyebrow">A CLOSER LOOK</div><h3>Ask this contract</h3></div><span>KEYWORD SEARCH</span></div><form className="question-form" onSubmit={ask}><input value={question} onChange={(e) => setQuestion(e.target.value)} placeholder="e.g. When does the agreement begin?" aria-label="Question about this contract" /><button className="button button-dark" disabled={busy || !question.trim()}>Ask <span aria-hidden="true">↗</span></button></form>{answer && <div className="answer"><div className="eyebrow">FROM THE DOCUMENT</div><p>{answer}</p></div>}</section>
        </> : <div className="welcome"><div className="welcome-icon">⌑</div><div className="eyebrow">A CLEARER VIEW OF WHAT MATTERS</div><h2>Your agreements,<br />at a glance.</h2><p>Select a contract from the register to read its details, move it through review, or ask a question about its contents.</p><div className="welcome-rule" /><small>Private by design. Originals are kept in Supabase Storage.</small></div>}
      </section>
    </div>
    {(error || notice) && <div className={`toast ${error ? 'toast-error' : ''}`} role="status">{error || notice}<button aria-label="Dismiss message" onClick={() => { setError(''); setNotice(''); }}>×</button></div>}
    <footer className="footer"><span>CONTRACT MANAGER</span><span>ORDER, WITHOUT THE ORNAMENT.</span></footer>
  </main>;
}
