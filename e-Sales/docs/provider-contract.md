# Product provider API v1

e-Sales owns sales accounts, product selection, demo ownership and customer ownership.
Products own their tenant data and provisioning. Provider credentials stay on the
e-Sales server and are never sent to the browser.

The configured provider base URL ends in `/api/provider/v1`. All requests require
`X-Provider-Key`. An empty configured key disables access. Providers return JSON
errors as `{ "message": "..." }` with an appropriate non-2xx status.

## Demos

`GET /capabilities` returns:

```json
{ "product": "school", "contractVersion": 1, "features": ["demos", "customers"] }
```

`ownerKey` is a stable sales username matching `[a-z0-9][a-z0-9-]{0,47}`.
`GET /demos/{ownerKey}` returns the following shape, with `ready: false` for a
demo that has not yet been created:

```json
{
  "tenantId": "demo-alex",
  "ready": true,
  "tenantUrl": "http://demo-alex.localhost:3080/",
  "password": "333333",
  "logins": [{ "username": "Hamid", "role": "teacher" }]
}
```

`PUT /demos/{ownerKey}` takes `{ "ownerName": "Alex" }`, creates or resets only
that owner's demo, and returns the same shape. `DELETE /demos/{ownerKey}` deletes
only that demo and returns 204. Product adapters must never derive a customer
tenant deletion from a demo operation.

## Customers

`GET /customers` returns an array:

```json
[
  {
    "tenantId": "school-one",
    "name": "School One",
    "createdAt": "2026-10-07T12:00:00Z",
    "userCount": 1,
    "deletable": true,
    "ownerRef": "alex"
  }
]
```

`POST /customers` takes:

```json
{
  "tenantId": "school-one",
  "name": "School One",
  "email": "office@example.test",
  "admin": { "username": "director", "name": "Director", "email": "director@example.test" },
  "ownerRef": "alex"
}
```

It returns 201:

```json
{
  "tenantId": "school-one",
  "name": "School One",
  "invitationLink": "http://school-one.localhost:3080/accept-school-invitation?token=...",
  "invitationEmailSent": true
}
```

Duplicate tenant IDs return 409. School administrator invitations are accepted
in the product, not in e-Sales.

`DELETE /customers/{tenantId}?confirm={tenantId}` returns 204 after checking the
confirmation and product-specific protection rules. Protected/system/demo
tenants must not be deletable through this endpoint.

## Failure semantics

Operations are synchronous. A provider failure must be surfaced; e-Sales must
not claim a successful reset, creation or deletion. e-Sales records ownership
only after successful provisioning. A retry of demo PUT is safe; customer POST
may return 409 after an uncertain network result and must not be retried silently.

Cafe is a configured, disabled provider until a Cafe deployment implements this
contract. e-Sales must not generate simulated Cafe demos or share product databases.
