import { api, safeUrl, TOKEN_KEY } from './api';

function response(status, data) {
  return { status, ok: status >= 200 && status < 300, json: async () => data };
}

beforeEach(() => {
  sessionStorage.clear();
  global.fetch = jest.fn();
});
afterEach(() => jest.restoreAllMocks());

test('uses only the independent session token and sends JSON', async () => {
  sessionStorage.setItem(TOKEN_KEY, 'sales-token');
  localStorage.setItem('school_token', 'school-token');
  fetch.mockResolvedValue(response(200, { id: 'created' }));
  await expect(api('/customers', { method: 'POST', body: { productId: 'school' } })).resolves.toEqual({ id: 'created' });
  expect(fetch).toHaveBeenCalledWith('/api/customers', expect.objectContaining({
    headers: { Authorization: 'Bearer sales-token', 'Content-Type': 'application/json' },
    body: '{"productId":"school"}',
  }));
  expect(localStorage.getItem('school_token')).toBe('school-token');
  localStorage.clear();
});

test('401 clears only the sales session and requests login', async () => {
  sessionStorage.setItem(TOKEN_KEY, 'expired');
  const listener = jest.fn();
  window.addEventListener('e-sales-unauthorized', listener);
  fetch.mockResolvedValue(response(401, { message: 'Sitzung abgelaufen' }));
  await expect(api('/me')).rejects.toThrow('Sitzung abgelaufen');
  expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull();
  expect(listener).toHaveBeenCalledTimes(1);
  window.removeEventListener('e-sales-unauthorized', listener);
});

test('provider errors retain the session', async () => {
  sessionStorage.setItem(TOKEN_KEY, 'valid');
  fetch.mockResolvedValue(response(503, { message: 'Provider nicht verfügbar' }));
  await expect(api('/me/demos')).rejects.toThrow('Provider nicht verfügbar');
  expect(sessionStorage.getItem(TOKEN_KEY)).toBe('valid');
});

test('204 succeeds without trying to parse JSON', async () => {
  const json = jest.fn();
  fetch.mockResolvedValue({ status: 204, ok: true, json });
  await expect(api('/me/demos/school', { method: 'DELETE' })).resolves.toBeNull();
  expect(json).not.toHaveBeenCalled();
});

test('network and malformed responses show meaningful errors', async () => {
  fetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));
  await expect(api('/products')).rejects.toThrow('inaccessible');
  fetch.mockResolvedValueOnce({ status: 502, ok: false, json: async () => { throw new Error('HTML'); } });
  await expect(api('/products')).rejects.toThrow('HTTP 502');
});

test('a network failure during customer creation warns that the outcome is unknown', async () => {
  fetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));
  await expect(api('/customers', { method: 'POST', body: { productId: 'school' } }))
    .rejects.toThrow("Résultat inconnu. Actualisez et vérifiez dans le produit avant de relancer l'action.");
  expect(fetch).toHaveBeenCalledTimes(1);
});

test('a non-JSON gateway timeout during a mutation warns against blind retry', async () => {
  fetch.mockResolvedValueOnce({ status: 504, ok: false, json: async () => { throw new Error('HTML'); } });
  await expect(api('/customers', { method: 'POST', body: { productId: 'school' } }))
    .rejects.toThrow("Résultat inconnu. Actualisez et vérifiez dans le produit avant de relancer l'action. (HTTP 504)");
  expect(fetch).toHaveBeenCalledTimes(1);
});

test.each(['POST', 'PUT', 'DELETE', 'PATCH'])('%s gateway errors retain the session and report unknown outcome', async method => {
  sessionStorage.setItem(TOKEN_KEY, 'valid');
  fetch.mockResolvedValueOnce(response(502, { message: 'Bad gateway' }));
  await expect(api('/customers/school/customer', { method })).rejects.toThrow('Résultat inconnu');
  expect(sessionStorage.getItem(TOKEN_KEY)).toBe('valid');
});

test.each([null, 'unexpected'])('invalid mutation results report unknown outcome: %p', async data => {
  fetch.mockResolvedValueOnce(response(200, data));
  await expect(api('/customers', { method: 'POST' })).rejects.toThrow('Résultat inconnu');
});

test('malformed JSON after a successful mutation reports unknown outcome', async () => {
  fetch.mockResolvedValueOnce({ status: 200, ok: true, json: async () => { throw new Error('Incomplete JSON'); } });
  await expect(api('/me/demos/school', { method: 'PUT' })).rejects.toThrow('Résultat inconnu');
});

test('rejects executable links', () => {
  expect(safeUrl('javascript:alert(1)')).toBeNull();
  expect(safeUrl('data:text/html,anything')).toBeNull();
  expect(safeUrl('https://demo.example.org')).toBe('https://demo.example.org/');
});
