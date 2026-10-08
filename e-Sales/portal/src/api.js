export const TOKEN_KEY = 'e_sales_token';

export async function api(path, options = {}) {
  const token = sessionStorage.getItem(TOKEN_KEY);
  const mutation = ['POST', 'PUT', 'DELETE', 'PATCH'].includes((options.method || 'GET').toUpperCase());
  const unknownOutcome = "Résultat inconnu. Actualisez et vérifiez dans le produit avant de relancer l'action.";
  let response;
  try {
    response = await fetch(`/api${path}`, {
      ...options,
      headers: {
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
      body: options.body ? JSON.stringify(options.body) : undefined,
    });
  } catch {
    throw new Error(mutation ? unknownOutcome : 'Le serveur est inaccessible. Veuillez réessayer.');
  }
  if (response.status === 401) {
    sessionStorage.removeItem(TOKEN_KEY);
    window.dispatchEvent(new Event('e-sales-unauthorized'));
  }
  if (response.status === 204) return null;
  if (mutation && [502, 504].includes(response.status)) {
    throw new Error(`${unknownOutcome} (HTTP ${response.status})`);
  }
  let data;
  try {
    data = await response.json();
  } catch {
    throw new Error(mutation ? `${unknownOutcome} (HTTP ${response.status})` : `Réponse du serveur invalide (HTTP ${response.status}).`);
  }
  if (!data || typeof data !== 'object') {
    throw new Error(mutation ? `${unknownOutcome} (HTTP ${response.status})` : `Réponse du serveur invalide (HTTP ${response.status}).`);
  }
  if (!response.ok) {
    throw new Error(data.message || `La requête a échoué (HTTP ${response.status}).`);
  }
  return data;
}

export function safeUrl(value) {
  try {
    const url = new URL(value);
    return ['http:', 'https:'].includes(url.protocol) ? url.href : null;
  } catch {
    return null;
  }
}
