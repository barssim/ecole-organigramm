import React from 'react';
import { act, render, screen, waitFor, within } from '@testing-library/react';
import testingUser from '@testing-library/user-event';
import App from './App';
import { TOKEN_KEY } from './api';

const userEvent = {
  click: (...args) => act(() => { testingUser.click(...args); }),
  type: (...args) => act(() => { testingUser.type(...args); }),
  selectOptions: (...args) => act(() => { testingUser.selectOptions(...args); }),
};

const salesperson = { id: 'u1', username: 'vera', name: 'Vera Vertrieb', email: 'vera@example.org', role: 'sales' };
const administrator = { ...salesperson, role: 'platformadmin' };
const products = [{ id: 'school', name: 'Schulplattform', enabled: true }, { id: 'cafe', name: 'Café', enabled: false }];
const emptyDemo = { productId: 'school', productName: 'Schulplattform', enabled: true, status: 'NOT_CREATED' };
const readyDemo = {
  ...emptyDemo, status: 'READY', tenantId: 'demo-vera', tenantUrl: 'https://demo.example.org',
  password: 'demo-password', logins: [{ username: 'director', role: 'director' }],
};
function response(data, status = 200) {
  return { ok: status >= 200 && status < 300, status, json: async () => data };
}
function mockApi(overrides = {}, user = salesperson) {
  const routes = {
    '/api/me': user,
    '/api/products': products,
    '/api/me/demos': [emptyDemo],
    '/api/customers?productId=school': [],
    '/api/admin/sales-users': [],
    ...overrides,
  };
  global.fetch = jest.fn(async (path, options = {}) => {
    if (!(path in routes)) throw new Error(`Unexpected route: ${path}`);
    const result = routes[path];
    if (typeof result === 'function') return result(options);
    return response(result);
  });
}
function signedIn(overrides, user) {
  sessionStorage.setItem(TOKEN_KEY, 'existing-token');
  mockApi(overrides, user);
  render(<App />);
}
async function openCustomers() {
  userEvent.click(await screen.findByRole('button', { name: 'Clients' }));
  await screen.findByRole('heading', { name: 'Clients' });
}
beforeEach(() => {
  sessionStorage.clear(); localStorage.clear();
  window.history.replaceState(null, '', '/');
});
afterEach(() => jest.restoreAllMocks());

test('login stores an independent session token and sales sees no admin navigation', async () => {
  localStorage.setItem('school_token', 'unchanged');
  mockApi({ '/api/auth/login': options => {
    expect(JSON.parse(options.body)).toEqual({ username: 'vera', password: 'secret' });
    return response({ token: 'new-token', user: salesperson });
  } });
  render(<App />);
  userEvent.type(screen.getByLabelText("Nom d'utilisateur"), 'vera');
  userEvent.type(screen.getByLabelText('Mot de passe'), 'secret');
  userEvent.click(screen.getByRole('button', { name: 'Se connecter' }));
  expect(await screen.findByRole('heading', { name: 'Mes démonstrations' })).toBeInTheDocument();
  expect(sessionStorage.getItem(TOKEN_KEY)).toBe('new-token');
  expect(localStorage.getItem('school_token')).toBe('unchanged');
  expect(screen.queryByRole('button', { name: 'Équipe commerciale' })).not.toBeInTheDocument();
  expect(fetch.mock.calls.some(([, options]) => options.method === 'PUT')).toBe(false);
});

test('backend PLATFORM_ADMIN login exposes the team and customer administration actions', async () => {
  const adminUser = { ...administrator, role: 'PLATFORM_ADMIN' };
  const customer = { productId: 'school', tenantId: 'admin-customer', name: 'Admin-Kunde', ownerId: 'u1', ownerName: 'Vera', deletable: true };
  mockApi({
    '/api/auth/login': { token: 'admin-token', user: adminUser },
    '/api/admin/sales-users': [salesperson],
    '/api/customers?productId=school': [customer],
  });
  render(<App />);
  userEvent.type(screen.getByLabelText("Nom d'utilisateur"), 'admin');
  userEvent.type(screen.getByLabelText('Mot de passe'), 'secret');
  userEvent.click(screen.getByRole('button', { name: 'Se connecter' }));
  const team = await screen.findByRole('button', { name: 'Équipe commerciale' });
  expect(screen.getByText(/Administration/)).toBeInTheDocument();
  userEvent.click(team);
  expect(await screen.findByRole('button', { name: 'Créer le compte et inviter' })).toBeInTheDocument();
  await openCustomers();
  expect(await screen.findByLabelText('Responsable de Admin-Kunde')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /Attribuer.*Admin-Kunde/ })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /Supprimer.*Admin-Kunde/ })).toBeInTheDocument();
});

test('backend SALES session retains salesperson navigation without administrative customer actions', async () => {
  const customer = { productId: 'school', tenantId: 'sales-customer', name: 'Sales-Kunde', ownerId: 'u1', ownerName: 'Vera', deletable: true };
  signedIn({ '/api/customers?productId=school': [customer] }, { ...salesperson, role: 'SALES' });
  await openCustomers();
  expect(await screen.findByText('Sales-Kunde')).toBeInTheDocument();
  expect(screen.getByText(/· Commercial/)).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Équipe commerciale' })).not.toBeInTheDocument();
  expect(screen.queryByLabelText('Responsable de Sales-Kunde')).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: /Supprimer.*Sales-Kunde/ })).not.toBeInTheDocument();
  expect(fetch.mock.calls.some(([path]) => path === '/api/admin/sales-users')).toBe(false);
});

test('invitation acceptance works even when another session was already stored', async () => {
  window.history.replaceState(null, '', '/accept-invitation?token=invite-value');
  sessionStorage.setItem(TOKEN_KEY, 'older-session');
  mockApi({ '/api/auth/invitations/accept': options => {
    expect(JSON.parse(options.body)).toEqual({ token: 'invite-value', password: 'my-new-password' });
    return response({ token: 'invited-token', user: salesperson });
  } });
  render(<App />);
  userEvent.type(screen.getByLabelText('Nouveau mot de passe'), 'my-new-password');
  userEvent.click(screen.getByRole('button', { name: "Accepter l'invitation" }));
  await screen.findByRole('heading', { name: 'Mes démonstrations' });
  expect(sessionStorage.getItem(TOKEN_KEY)).toBe('invited-token');
  expect(window.location.pathname).toBe('/');
  expect(window.location.search).toBe('');
});

test('disabled products show no operation buttons; creating a demo is on demand', async () => {
  signedIn({ '/api/me/demos/school': () => response(readyDemo) });
  const create = await screen.findByRole('button', { name: 'Créer une démonstration' });
  const cafe = screen.getByRole('heading', { name: 'Café' }).closest('article');
  expect(within(cafe).getByText('Non connecté')).toBeInTheDocument();
  expect(within(cafe).queryByRole('button')).not.toBeInTheDocument();
  expect(fetch.mock.calls.some(([, options]) => options.method === 'PUT')).toBe(false);
  userEvent.click(create);
  await waitFor(() => expect(fetch).toHaveBeenCalledWith('/api/me/demos/school', expect.objectContaining({ method: 'PUT', body: undefined })));
});

test('demo credentials and links are displayed; reset requires confirmation', async () => {
  signedIn({ '/api/me/demos': [readyDemo], '/api/me/demos/school': () => response(readyDemo) });
  const reset = await screen.findByRole('button', { name: 'Réinitialiser la démonstration' });
  expect(screen.getByLabelText('Mot de passe de démonstration Schulplattform')).toHaveValue('demo-password');
  expect(screen.getByRole('link', { name: 'Ouvrir la démonstration ↗' })).toHaveAttribute('href', 'https://demo.example.org/');
  userEvent.click(reset);
  const dialog = screen.getByRole('dialog');
  expect(within(dialog).getByText(/définitivement supprimées/)).toBeInTheDocument();
  expect(fetch.mock.calls.some(([, options]) => options.method === 'PUT')).toBe(false);
  userEvent.click(within(dialog).getByRole('button', { name: 'Confirmer' }));
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  expect(fetch).toHaveBeenCalledWith('/api/me/demos/school', expect.objectContaining({ method: 'PUT' }));
});

test('demo deletion can be cancelled before confirming the destructive request', async () => {
  signedIn({ '/api/me/demos': [readyDemo], '/api/me/demos/school': () => response(null, 204) });
  userEvent.click(await screen.findByRole('button', { name: 'Supprimer la démonstration' }));
  userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Annuler' }));
  expect(fetch.mock.calls.some(([, options]) => options.method === 'DELETE')).toBe(false);
  userEvent.click(screen.getByRole('button', { name: 'Supprimer la démonstration' }));
  userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirmer' }));
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  expect(fetch).toHaveBeenCalledWith('/api/me/demos/school', expect.objectContaining({ method: 'DELETE' }));
});

test('customer creation sends selected product and director information and reports email failure', async () => {
  signedIn({ '/api/customers': options => {
    expect(JSON.parse(options.body)).toEqual({
      productId: 'school', tenantId: 'kunde-a', name: 'Kunde A', email: 'office@example.org',
      admin: { username: 'leitung', name: 'Lea Leitung', email: 'lea@example.org' },
    });
    return response({ tenantId: 'kunde-a', invitationLink: 'https://sales.example.org/accept-invitation?token=abc', invitationEmailSent: false });
  } });
  await openCustomers();
  userEvent.type(screen.getByLabelText("Identifiant de l'instance"), 'kunde-a');
  userEvent.type(screen.getByLabelText('Nom du client'), 'Kunde A');
  userEvent.type(screen.getByLabelText('E-mail du client'), 'office@example.org');
  userEvent.type(screen.getByLabelText("Nom d'utilisateur de la direction"), 'leitung');
  userEvent.type(screen.getByLabelText('Nom de la direction'), 'Lea Leitung');
  userEvent.type(screen.getByLabelText('E-mail de la direction'), 'lea@example.org');
  userEvent.click(screen.getByRole('button', { name: 'Créer le client et inviter' }));
  expect(await screen.findByText(/n'a pas été envoyé/)).toBeInTheDocument();
  expect(screen.getByLabelText("Lien d'invitation")).toHaveValue('https://sales.example.org/accept-invitation?token=abc');
  userEvent.selectOptions(screen.getByLabelText('Produit'), 'cafe');
  expect(screen.getByText(/Aucune action client/)).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Créer le client et inviter' })).not.toBeInTheDocument();
  expect(fetch.mock.calls.some(([path]) => path.includes('productId=cafe'))).toBe(false);
});

test('administrator can reassign a customer and deletion requires exact tenant ID', async () => {
  const customer = { productId: 'school', tenantId: 'tenant-123', name: 'Kunde A', ownerId: 'u1', ownerName: 'Vera', deletable: true, userCount: 4 };
  signedIn({
    '/api/customers?productId=school': [customer],
    '/api/admin/sales-users': [salesperson, { ...salesperson, id: 'u2', username: 'tom', name: 'Tom Vertrieb' }],
    '/api/customers/school/tenant-123/owner': options => {
      expect(JSON.parse(options.body)).toEqual({ salesUserId: 'u2' });
      return response(null, 204);
    },
    '/api/customers/school/tenant-123?confirm=tenant-123': () => response(null, 204),
  }, administrator);
  await openCustomers();
  const select = await screen.findByLabelText('Responsable de Kunde A');
  userEvent.selectOptions(select, 'u2');
  userEvent.click(screen.getByRole('button', { name: /Attribuer.*Kunde A/ }));
  await screen.findByText('Le responsable a été mis à jour.');
  userEvent.click(await screen.findByRole('button', { name: /Supprimer.*Kunde A/ }));
  const dialog = screen.getByRole('dialog');
  const confirm = within(dialog).getByRole('button', { name: 'Confirmer' });
  expect(confirm).toBeDisabled();
  userEvent.type(within(dialog).getByLabelText(/Pour confirmer, saisissez exactement/), 'tenant-12');
  expect(confirm).toBeDisabled();
  userEvent.type(within(dialog).getByLabelText(/Pour confirmer, saisissez exactement/), '3');
  userEvent.click(confirm);
  await screen.findByText('Le client a été supprimé.');
  expect(fetch).toHaveBeenCalledWith('/api/customers/school/tenant-123?confirm=tenant-123', expect.objectContaining({ method: 'DELETE' }));
});

test('admin creates a salesperson, sees the invitation result, and can resend an invitation', async () => {
  const invited = { ...salesperson, invitationLink: 'https://sales.example.org/accept-invitation?token=invite', invitationEmailSent: true };
  signedIn({
    '/api/admin/sales-users': options => options.method === 'POST' ? response(invited) : response([salesperson]),
    '/api/admin/sales-users/u1/invitation': () => response(invited),
  }, administrator);
  userEvent.click(await screen.findByRole('button', { name: 'Équipe commerciale' }));
  userEvent.type(await screen.findByLabelText("Nom d'utilisateur"), 'vera');
  userEvent.type(screen.getByLabelText('Nom'), 'Vera Vertrieb');
  userEvent.type(screen.getByLabelText('E-mail'), 'vera@example.org');
  userEvent.click(screen.getByRole('button', { name: 'Créer le compte et inviter' }));
  expect(await screen.findByText("L'e-mail d'invitation a été envoyé.")).toBeInTheDocument();
  expect(fetch).toHaveBeenCalledWith('/api/admin/sales-users', expect.objectContaining({
    method: 'POST', body: JSON.stringify({ username: 'vera', name: 'Vera Vertrieb', email: 'vera@example.org' }),
  }));
  userEvent.click(await screen.findByRole('button', { name: /Renvoyer l'invitation.*vera/ }));
  await waitFor(() => expect(fetch).toHaveBeenCalledWith('/api/admin/sales-users/u1/invitation', expect.objectContaining({ method: 'POST' })));
});

test('deleting a salesperson requires their exact username', async () => {
  signedIn({
    '/api/admin/sales-users': [salesperson],
    '/api/admin/sales-users/u1': () => response(null, 204),
  }, administrator);
  userEvent.click(await screen.findByRole('button', { name: 'Équipe commerciale' }));
  userEvent.click(await screen.findByRole('button', { name: /Supprimer.*vera/ }));
  const dialog = screen.getByRole('dialog');
  expect(within(dialog).getByRole('button', { name: 'Confirmer' })).toBeDisabled();
  userEvent.type(within(dialog).getByLabelText(/Pour confirmer, saisissez exactement/), 'vera');
  userEvent.click(within(dialog).getByRole('button', { name: 'Confirmer' }));
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  expect(fetch).toHaveBeenCalledWith('/api/admin/sales-users/u1', expect.objectContaining({ method: 'DELETE' }));
});

test('an invitation without token cannot be submitted', () => {
  window.history.replaceState(null, '', '/accept-invitation');
  mockApi();
  render(<App />);
  expect(screen.getByRole('alert')).toHaveTextContent('aucun jeton');
  expect(screen.getByRole('button', { name: "Accepter l'invitation" })).toBeDisabled();
  expect(fetch).not.toHaveBeenCalled();
});

test('a provider failure shows an error without logging the user out; retry is offered', async () => {
  signedIn({ '/api/me/demos': () => response({ message: 'Provider ist offline' }, 503) });
  expect(await screen.findByRole('alert')).toHaveTextContent('Provider ist offline');
  expect(sessionStorage.getItem(TOKEN_KEY)).toBe('existing-token');
  expect(screen.getByRole('button', { name: 'Se déconnecter' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Recharger' })).toBeInTheDocument();
});

test('401 from an authenticated request returns to the login screen', async () => {
  signedIn({ '/api/me/demos': () => response({ message: 'Abgelaufen' }, 401) });
  expect(await screen.findByRole('button', { name: 'Se connecter' })).toBeInTheDocument();
  expect(screen.getByText('Veuillez vous reconnecter.')).toBeInTheDocument();
  expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull();
});

test('session validation failure is retryable and does not silently remove credentials', async () => {
  signedIn({ '/api/me': () => response({ message: 'Wartung' }, 503) });
  expect(await screen.findByRole('alert')).toHaveTextContent('Wartung');
  expect(sessionStorage.getItem(TOKEN_KEY)).toBe('existing-token');
  userEvent.click(screen.getByRole('button', { name: 'Retour à la connexion' }));
  expect(screen.getByRole('button', { name: 'Se connecter' })).toBeInTheDocument();
  expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull();
});
