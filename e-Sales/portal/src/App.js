import React, { useCallback, useEffect, useRef, useState } from 'react';
import { api, safeUrl, TOKEN_KEY } from './api';

const segment = encodeURIComponent;

function ErrorMessage({ message }) {
  return message ? <p className="notice error" role="alert">{message}</p> : null;
}

function useResource(loader) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [revision, setRevision] = useState(0);
  const refresh = useCallback(() => setRevision(value => value + 1), []);
  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    setData(null);
    loader().then(value => { if (active) setData(value); })
      .catch(reason => { if (active) setError(reason.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [loader, revision]);
  return { data, error, loading, refresh };
}

function ResourceStatus({ resource }) {
  if (resource.loading) return <p role="status">Chargement des données …</p>;
  if (resource.error) return <><ErrorMessage message={resource.error} /><button onClick={resource.refresh}>Recharger</button></>;
  return null;
}

function Field({ label, id, ...props }) {
  return <label htmlFor={id}>{label}<input id={id} {...props} /></label>;
}

function ExternalLink({ url, children }) {
  const href = safeUrl(url);
  return href ? <a href={href} target="_blank" rel="noopener noreferrer">{children}</a>
    : <span className="notice error">Aucune URL valide disponible.</span>;
}

function InvitationResult({ result }) {
  if (!result) return null;
  return <section className="notice success" aria-label="Résultat de l'invitation">
    <p role="status">{result.invitationEmailSent
      ? "L'e-mail d'invitation a été envoyé."
      : "L'e-mail d'invitation n'a pas été envoyé. Veuillez transmettre le lien manuellement."}</p>
    {result.invitationLink
      ? <><label>Lien d'invitation<input readOnly value={result.invitationLink} onFocus={event => event.target.select()} /></label>
        <ExternalLink url={result.invitationLink}>Ouvrir l'invitation</ExternalLink></>
      : <p>Le serveur n'a fourni aucun lien d'invitation.</p>}
  </section>;
}

function ConfirmDialog({ title, description, exact, onCancel, onConfirm, pending, error }) {
  const [value, setValue] = useState('');
  const dialog = useRef(null);
  useEffect(() => {
    const previous = document.activeElement;
    dialog.current.focus();
    return () => { if (previous?.isConnected) previous.focus(); };
  }, []);
  const keyboard = event => {
    if (event.key === 'Escape' && !pending) onCancel();
    if (event.key !== 'Tab') return;
    const elements = Array.from(dialog.current.querySelectorAll('button:not(:disabled), input:not(:disabled)'));
    const first = elements[0];
    const last = elements[elements.length - 1];
    if (event.shiftKey && (document.activeElement === first || document.activeElement === dialog.current)) {
      event.preventDefault(); last?.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault(); first?.focus();
    }
  };
  return <div className="overlay"><section className="dialog" role="dialog" aria-modal="true"
    aria-labelledby="confirm-title" aria-describedby="confirm-description" tabIndex={-1} ref={dialog} onKeyDown={keyboard}>
    <h2 id="confirm-title">{title}</h2><p id="confirm-description">{description}</p>
    {exact && <Field id="confirm-value" label={`Pour confirmer, saisissez exactement « ${exact} »`}
      value={value} onChange={event => setValue(event.target.value)} autoComplete="off" disabled={pending} />}
    <ErrorMessage message={error} />
    <div className="actions"><button disabled={pending} onClick={onCancel}>Annuler</button>
      <button className="danger" disabled={pending || (exact && value !== exact)}
        onClick={() => onConfirm(value)}>{pending ? 'Opération en cours …' : 'Confirmer'}</button></div>
  </section></div>;
}

function Auth({ onLogin, notice }) {
  const invitationToken = new URLSearchParams(window.location.search).get('token');
  const accepting = window.location.pathname === '/accept-invitation';
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);
  async function submit(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setPending(true); setError('');
    try {
      const result = await api(accepting ? '/auth/invitations/accept' : '/auth/login', {
        method: 'POST',
        body: accepting ? { token: invitationToken, password: form.get('password') }
          : { username: form.get('username'), password: form.get('password') },
      });
      sessionStorage.setItem(TOKEN_KEY, result.token);
      window.history.replaceState(null, '', '/');
      onLogin(result.user);
    } catch (reason) { setError(reason.message); }
    finally { setPending(false); }
  }
  return <main className="auth"><div className="brand">e-Sales <span>Portail commercial</span></div>
    <section className="panel"><h1>{accepting ? "Accepter l'invitation" : 'Bienvenue sur e-Sales'}</h1>
      <p>{accepting ? 'Définissez votre mot de passe personnel.' : 'Un accès centralisé aux produits, aux clients et à vos démonstrations.'}</p>
      {notice && <p className="notice" role="status">{notice}</p>}
      <ErrorMessage message={error || (accepting && !invitationToken ? "Le lien d'invitation ne contient aucun jeton." : '')} />
      <form onSubmit={submit}>
        {!accepting && <Field id="username" label="Nom d'utilisateur" name="username" required autoComplete="username" disabled={pending} />}
        <Field id="password" label={accepting ? 'Nouveau mot de passe' : 'Mot de passe'} name="password" type="password"
          required autoComplete={accepting ? 'new-password' : 'current-password'} disabled={pending} />
        <button className="primary" disabled={pending || (accepting && !invitationToken)}>
          {pending ? 'Veuillez patienter …' : accepting ? "Accepter l'invitation" : 'Se connecter'}</button>
      </form>
      {accepting && <a href="/">Retour à la connexion</a>}
    </section>
  </main>;
}

function Demos({ products }) {
  const loader = useCallback(() => api('/me/demos'), []);
  const resource = useResource(loader);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');
  const [confirmation, setConfirmation] = useState(null);
  async function operate(productId, method) {
    setPending(true); setError('');
    try {
      await api(`/me/demos/${segment(productId)}`, { method });
      setConfirmation(null); resource.refresh();
    } catch (reason) { setError(reason.message); }
    finally { setPending(false); }
  }
  return <section><h1>Mes démonstrations</h1><p>Vos environnements de démonstration sont créés uniquement à votre demande.</p>
    <ResourceStatus resource={resource} /><ErrorMessage message={confirmation ? '' : error} />
    <div className="cards">{products.map(product => {
      const demo = resource.data?.find(item => item.productId === product.id);
      const ready = demo?.status === 'READY';
      const enabled = product.enabled && demo?.enabled !== false;
      return <article className="panel" key={product.id}><h2>{product.name}</h2>
        {!product.enabled ? <span className="badge muted">Non connecté</span>
          : <><span className={`badge ${ready ? 'ready' : 'muted'}`}>{resource.loading ? 'Chargement …'
            : resource.error || !demo ? 'Statut indisponible' : ready ? 'Prêt' : demo.status === 'UNAVAILABLE' ? 'Indisponible' : 'Pas encore créé'}</span>
            {demo?.message && <p>{demo.message}</p>}
            {ready && <><p><ExternalLink url={demo.tenantUrl}>Ouvrir la démonstration ↗</ExternalLink></p>
              <p>Instance : <strong>{demo.tenantId}</strong></p>
              <ul>{demo.logins?.map(login => <li key={`${login.username}-${login.role}`}><strong>{login.username}</strong> · {login.role}</li>)}</ul>
              <label>Mot de passe de démonstration<input readOnly value={demo.password || ''} aria-label={`Mot de passe de démonstration ${product.name}`} /></label></>}
            {enabled && demo && demo.status !== 'UNAVAILABLE' && !resource.loading && !resource.error && <div className="actions">
              <button className="primary" disabled={pending} onClick={() => {
                if (ready) { setError(''); setConfirmation({ product, method: 'PUT' }); }
                else operate(product.id, 'PUT');
              }}>{ready ? 'Réinitialiser la démonstration' : 'Créer une démonstration'}</button>
              {ready && <button className="danger" disabled={pending} onClick={() => {
                setError(''); setConfirmation({ product, method: 'DELETE' });
              }}>Supprimer la démonstration</button>}
            </div>}
          </>}
      </article>;
    })}</div>
    {pending && !confirmation && <p role="status">Préparation de la démonstration. Cela peut prendre un moment …</p>}
    {confirmation && <ConfirmDialog title={confirmation.method === 'PUT' ? 'Réinitialiser la démonstration ?' : 'Supprimer la démonstration ?'}
      description={`Toutes les données actuelles de votre démonstration pour ${confirmation.product.name} seront définitivement supprimées.`}
      pending={pending} error={error} onCancel={() => setConfirmation(null)}
      onConfirm={() => operate(confirmation.product.id, confirmation.method)} />}
  </section>;
}

function SalesUsers() {
  const loader = useCallback(() => api('/admin/sales-users'), []);
  const resource = useResource(loader);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const [deleting, setDeleting] = useState(null);
  async function create(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const values = Object.fromEntries(new FormData(form));
    setPending(true); setError(''); setResult(null);
    try {
      setResult(await api('/admin/sales-users', { method: 'POST', body: values }));
      form.reset(); resource.refresh();
    } catch (reason) { setError(reason.message); }
    finally { setPending(false); }
  }
  async function userAction(user, invite) {
    setPending(true); setError(''); setResult(null);
    try {
      const response = await api(`/admin/sales-users/${segment(user.id)}${invite ? '/invitation' : ''}`,
        { method: invite ? 'POST' : 'DELETE' });
      if (invite) setResult(response);
      setDeleting(null); resource.refresh();
    } catch (reason) { setError(reason.message); }
    finally { setPending(false); }
  }
  return <section><h1>Équipe commerciale</h1>
    <section className="panel"><h2>Inviter un commercial</h2>
      <p>Le nouveau compte recevra une invitation. Les démonstrations ne sont pas créées automatiquement.</p>
      <form onSubmit={create} className="form-grid">
        <Field id="sales-username" label="Nom d'utilisateur" name="username" required disabled={pending} />
        <Field id="sales-name" label="Nom" name="name" required disabled={pending} />
        <Field id="sales-email" label="E-mail" name="email" type="email" required disabled={pending} />
        <button className="primary" disabled={pending}>{pending ? 'Veuillez patienter …' : 'Créer le compte et inviter'}</button>
      </form>
    </section>
    <ErrorMessage message={deleting ? '' : error} /><InvitationResult result={result} />
    <ResourceStatus resource={resource} />
    {resource.data && <div className="table-wrap"><table><caption>Comptes commerciaux</caption>
      <thead><tr><th>Nom</th><th>Nom d'utilisateur</th><th>E-mail</th><th>Statut / Rôle</th><th>Actions</th></tr></thead>
      <tbody>{resource.data.map(user => <tr key={user.id}><td>{user.name}</td><td>{user.username}</td><td>{user.email}</td>
        <td>{user.active ? 'Actif' : 'Inactif'} · {user.role}</td><td><div className="actions">
          <button disabled={pending} onClick={() => userAction(user, true)}>Renvoyer l'invitation<span className="sr-only">: {user.username}</span></button>
          <button className="danger" disabled={pending} onClick={() => { setError(''); setDeleting(user); }}>Supprimer<span className="sr-only">: {user.username}</span></button>
        </div></td></tr>)}</tbody>
    </table>{resource.data.length === 0 && <p>Aucun compte commercial pour le moment.</p>}</div>}
    {deleting && <ConfirmDialog title="Supprimer le compte commercial ?" description={`Le compte de ${deleting.name} sera supprimé.`}
      exact={deleting.username} pending={pending} error={error} onCancel={() => setDeleting(null)} onConfirm={() => userAction(deleting, false)} />}
  </section>;
}

function OwnerForm({ customer, users, pending, onSave }) {
  const [owner, setOwner] = useState(customer.ownerId || '');
  return <form className="owner-form" onSubmit={event => { event.preventDefault(); onSave(owner || null); }}>
    <label htmlFor={`owner-${customer.tenantId}`} className="sr-only">Responsable de {customer.name}</label>
    <select id={`owner-${customer.tenantId}`} value={owner} disabled={pending} onChange={event => setOwner(event.target.value)}>
      <option value="">Non attribué</option>
      {customer.ownerId && !users.some(user => user.id === customer.ownerId) &&
        <option value={customer.ownerId}>{customer.ownerName || customer.ownerId}</option>}
      {users.map(user => <option key={user.id} value={user.id}>{user.name} ({user.username})</option>)}
    </select><button disabled={pending || owner === (customer.ownerId || '')}>Attribuer<span className="sr-only">: {customer.name}</span></button>
  </form>;
}

function Customers({ products, admin }) {
  const [productId, setProductId] = useState(products.find(product => product.enabled)?.id || products[0]?.id || '');
  const product = products.find(item => item.id === productId);
  const enabled = Boolean(product?.enabled);
  const loader = useCallback(() => enabled ? api(`/customers?productId=${segment(productId)}`) : Promise.resolve([]), [enabled, productId]);
  const usersLoader = useCallback(() => admin ? api('/admin/sales-users') : Promise.resolve([]), [admin]);
  const resource = useResource(loader);
  const users = useResource(usersLoader);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [message, setMessage] = useState('');
  async function create(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const values = Object.fromEntries(new FormData(form));
    setPending(true); setError(''); setResult(null); setMessage('');
    try {
      setResult(await api('/customers', { method: 'POST', body: {
        productId, tenantId: values.tenantId, name: values.name, email: values.email,
        admin: { username: values.adminUsername, name: values.adminName, email: values.adminEmail },
      } }));
      form.reset(); resource.refresh();
    } catch (reason) { setError(reason.message); }
    finally { setPending(false); }
  }
  async function change(customer, owner, confirm) {
    setPending(true); setError(''); setMessage('');
    const path = `/customers/${segment(customer.productId)}/${segment(customer.tenantId)}`;
    try {
      await api(confirm !== undefined ? `${path}?confirm=${segment(confirm)}` : `${path}/owner`,
        confirm !== undefined ? { method: 'DELETE' } : { method: 'PUT', body: { salesUserId: owner } });
      setDeleting(null); resource.refresh();
      setMessage(confirm !== undefined ? 'Le client a été supprimé.' : 'Le responsable a été mis à jour.');
    } catch (reason) { setError(reason.message); }
    finally { setPending(false); }
  }
  return <section><h1>Clients</h1><p>{admin ? 'Tous les clients et leurs responsables commerciaux.' : 'Vos clients par produit.'}</p>
    <label htmlFor="customer-product">Produit<select id="customer-product" value={productId} disabled={pending}
      onChange={event => { setProductId(event.target.value); setResult(null); setError(''); setMessage(''); }}>
      {products.map(item => <option key={item.id} value={item.id}>{item.name}{!item.enabled ? ' – Non connecté' : ''}</option>)}
    </select></label>
    {!enabled ? <p className="notice">Non connecté. Aucune action client n'est disponible pour ce produit.</p> : <>
      <section className="panel"><h2>Créer un client · {product.name}</h2>
        <form onSubmit={create}>
          <div className="form-grid">
            <Field id="customer-id" label="Identifiant de l'instance" name="tenantId" required disabled={pending} />
            <Field id="customer-name" label="Nom du client" name="name" required disabled={pending} />
            <Field id="customer-email" label="E-mail du client" name="email" type="email" required disabled={pending} />
          </div>
          <fieldset disabled={pending}><legend>Compte du responsable client (direction)</legend><div className="form-grid">
            <Field id="admin-username" label="Nom d'utilisateur de la direction" name="adminUsername" required />
            <Field id="admin-name" label="Nom de la direction" name="adminName" required />
            <Field id="admin-email" label="E-mail de la direction" name="adminEmail" type="email" required />
          </div></fieldset>
          <button className="primary" disabled={pending}>{pending ? 'Veuillez patienter …' : 'Créer le client et inviter'}</button>
        </form>
      </section>
      <ErrorMessage message={deleting ? '' : error} /><InvitationResult result={result} />
      {message && <p className="notice success" role="status">{message}</p>}
      <ResourceStatus resource={resource} />{admin && <ResourceStatus resource={users} />}
      {resource.data && <div className="table-wrap"><table><caption>Clients · {product.name}</caption>
        <thead><tr><th>Client / Instance</th><th>Date de création</th><th>Utilisateurs</th><th>Responsable</th>{admin && <th>Actions</th>}</tr></thead>
        <tbody>{resource.data.map(customer => <tr key={customer.tenantId}>
          <td><strong>{customer.name}</strong><br /><span>{customer.tenantId}</span></td>
          <td>{customer.createdAt ? new Date(customer.createdAt).toLocaleDateString('fr-FR') : '–'}</td>
          <td>{customer.userCount ?? '–'}</td>
          <td>{customer.ownerName || 'Non attribué'}
            {admin && users.data && <OwnerForm customer={customer} users={users.data} pending={pending} onSave={owner => change(customer, owner)} />}</td>
          {admin && <td>{customer.deletable ? <button className="danger" disabled={pending}
            onClick={() => { setError(''); setDeleting(customer); }}>Supprimer<span className="sr-only">: {customer.name}</span></button>
            : <span>Suppression impossible</span>}</td>}
        </tr>)}</tbody>
      </table>{resource.data.length === 0 && <p>Aucun client pour ce produit pour le moment.</p>}</div>}
    </>}
    {deleting && <ConfirmDialog title="Supprimer définitivement le client ?"
      description={`Toutes les données de ${deleting.name} seront supprimées. Cette action est irréversible.`}
      exact={deleting.tenantId} pending={pending} error={error} onCancel={() => setDeleting(null)}
      onConfirm={confirm => change(deleting, null, confirm)} />}
  </section>;
}

function Workspace({ user, onLogout }) {
  const loader = useCallback(() => api('/products'), []);
  const resource = useResource(loader);
  const [page, setPage] = useState('demos');
  const role = (user.role || '').toLowerCase().replace(/_/g, '');
  const admin = role === 'platformadmin';
  return <><header><div className="brand">e-Sales <span>Portail commercial</span></div>
    <div className="account"><span>{user.name || user.username} · {admin ? 'Administration' : 'Commercial'}</span>
      <button onClick={onLogout}>Se déconnecter</button></div></header>
    <nav aria-label="Navigation principale">
      {[['demos', 'Mes démonstrations'], ['customers', 'Clients'], ...(admin ? [['users', 'Équipe commerciale']] : [])].map(([id, name]) =>
        <button key={id} aria-current={page === id ? 'page' : undefined} onClick={() => setPage(id)}>{name}</button>)}
    </nav><main className="workspace">
      <ResourceStatus resource={resource} />
      {resource.data && <>
        {resource.data.length === 0 && <p className="notice">Aucun produit dans le catalogue pour le moment.</p>}
        {page === 'demos' && <Demos products={resource.data} />}
        {page === 'customers' && <Customers products={resource.data} admin={admin} />}
        {page === 'users' && admin && <SalesUsers />}
      </>}
    </main><footer>e-Sales · Gestion commerciale centralisée</footer></>;
}

export default function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(Boolean(sessionStorage.getItem(TOKEN_KEY)) && window.location.pathname !== '/accept-invitation');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    const unauthorized = () => { setUser(null); setLoading(false); setError(''); setNotice('Veuillez vous reconnecter.'); };
    window.addEventListener('e-sales-unauthorized', unauthorized);
    return () => window.removeEventListener('e-sales-unauthorized', unauthorized);
  }, []);
  useEffect(() => {
    let active = true;
    if (sessionStorage.getItem(TOKEN_KEY) && window.location.pathname !== '/accept-invitation') {
      setLoading(true); setError('');
      api('/me').then(value => { if (active) setUser(value); })
        .catch(reason => { if (active && sessionStorage.getItem(TOKEN_KEY)) setError(reason.message); })
        .finally(() => { if (active) setLoading(false); });
    }
    return () => { active = false; };
  }, [revision]);
  const logout = () => { sessionStorage.removeItem(TOKEN_KEY); setUser(null); setError(''); setNotice('Vous avez été déconnecté.'); };
  if (loading) return <main className="auth"><p role="status">Vérification de la session …</p></main>;
  if (error && !user) return <main className="auth"><section className="panel"><h1>Vérifier la session</h1>
    <ErrorMessage message={error} /><div className="actions"><button onClick={() => setRevision(value => value + 1)}>Réessayer</button>
      <button onClick={logout}>Retour à la connexion</button></div></section></main>;
  return user ? <Workspace user={user} onLogout={logout} /> : <Auth onLogin={setUser} notice={notice} />;
}
