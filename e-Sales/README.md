# e-Sales

Eigenstaendige Vertriebsanwendung fuer mehrere Produkte. e-Sales verwaltet
Sales-Konten, Produkte, persoenliche Demos und Kunden-Zuordnungen. Jedes Produkt
erstellt und verwaltet seine Demo- und Kundendaten selbst ueber eine private API.

Das Projekt liegt unter `/root/Projekte/e-Sales`, neben `/root/Projekte/e-cole`
und `/root/Projekte/e-cafe`, nicht innerhalb des e-cole-Projekts.
Docker-Compose-Befehle fuer e-Sales aus `/root/Projekte/e-Sales` ausfuehren.

## Aufbau

- `backend`: Spring Boot / Java 17, eigene Anmeldung und PostgreSQL-Datenbank.
- `portal`: eigenstaendiges React-Frontend, standardmaessig auf Port 3090.
- `docs/provider-contract.md`: versionierte Schnittstelle fuer Produktanbieter.
- `docker-compose.yml`: eigene Datenbank, Backend, Portal und Entwicklungs-Mailbox.

`school` ist an e-cole angebunden. Cafe ist an das e-cafe-Backend angebunden,
aber standardmaessig deaktiviert, bis URL, Netzwerk und gemeinsamer Provider-Key
konfiguriert sind. Es werden keine Cafe-Demos simuliert.

## Lokal mit Docker starten

1. In der e-cole-Konfiguration einen zufaelligen `SALES_PROVIDER_API_KEY` setzen.
   Derselbe Wert wird ausschliesslich serverseitig als `SCHOOL_PROVIDER_KEY` in
   e-Sales verwendet. `TENANT_URL_TEMPLATE` auf die oeffentliche URL fuer
   Schul-Tenants einstellen, zum Beispiel `http://{school}.localhost:3080/`.
   Ohne expliziten Wert verwendet Docker Compose den `HTTP_PORT` von e-cole.
2. e-cole mit den neuen Backend- und Portal-Versionen starten:

   ```bash
   cd portal
   npm ci
   REACT_APP_USE_RELATIVE_API=true npm run build
   cd ..
   docker compose up --build -d
   ```

3. In `e-Sales` `.env.example` nach `.env` kopieren. Unabhaengige zufaellige Werte
   fuer `POSTGRES_PASSWORD` und `JWT_SECRET` setzen, den Provider-Key uebernehmen,
   `ADMIN_EMAIL` auf eine gueltige E-Mail-Adresse setzen und
   `ADMIN_INITIAL_PASSWORD` mit mindestens 12 Zeichen festlegen.
   `JWT_SECRET` muss mindestens 32 Bytes lang sein und darf nicht mit dem
   e-cole-JWT-Schluessel identisch sein.
4. `ECOLE_NETWORK` auf den Namen des Docker-Netzwerks von e-cole einstellen.
   Standard: `e-cole_e-cole`. Bei anderem Compose-Projektnamen aendert sich dieser.
5. Das Frontend auf einem Rechner mit funktionierendem npm-Zugriff bauen:

   ```bash
   cd e-Sales/portal
   npm ci --no-audit --no-fund
   npm run build
   cd ..
   docker compose up --build -d
   ```

   Bei einem entfernten Server den kompletten Ordner `portal/build` nach
   `e-Sales/portal/build` auf dem Server uebertragen und dort aus `e-Sales`
   `docker compose up --build -d` ausfuehren. `build` ist nicht in Git enthalten
   und muss nach Frontend-Aenderungen neu erstellt und uebertragen werden.
   Das Portal-Dockerfile kopiert nur die fertigen Dateien in Nginx; es installiert
   keine npm-Pakete auf dem Server. Backend-Build und Docker-Basisimages benoetigen
   weiterhin Zugriff auf ihre jeweiligen Paket- beziehungsweise Image-Quellen.

Portal: `http://localhost:3090`. Entwicklungs-Mailbox:
`http://localhost:8027`. Anmeldung mit `ADMIN_USERNAME` und dem initialen Passwort.
Das initiale Passwort erzeugt einen Administrator nur bei der ersten Einrichtung;
es setzt kein vorhandenes Konto bei einem Neustart zurueck.

Das e-Sales-Backend ist nicht auf einen Host-Port veroeffentlicht. Das Portal
leitet `/api` intern weiter. Die Provider-API von e-cole ist im Schul-Portal
absichtlich gesperrt und nur direkt zwischen Backends erreichbar.

## Entwicklung ohne Docker

Java 17, Maven, Node.js 20 oder neuer und PostgreSQL verwenden.
Eine eigene Datenbank `e_sales` bereitstellen und die Backend-Variablen
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_USERNAME`,
`ADMIN_EMAIL`, `ADMIN_INITIAL_PASSWORD`,
`SCHOOL_PROVIDER_URL` und `SCHOOL_PROVIDER_KEY` setzen. Die Provider-URL muss
direkt das e-cole-Backend erreichen, nicht dessen Portal-Proxy.

```bash
cd e-Sales/backend
mvn spring-boot:run
```

In einem zweiten Terminal:

```bash
cd e-Sales/portal
npm ci
PORT=3090 npm start
```

Das Entwicklungsfrontend leitet `/api` an `http://localhost:8090` weiter.
`SALES_PUBLIC_URL` muss fuer Einladungen der aufgerufenen Frontend-URL entsprechen.

Provider-Aufrufe warten standardmaessig bis zu 120 Sekunden
(`PROVIDER_READ_TIMEOUT_SECONDS`). Der Portal-Proxy wartet 180 Sekunden.
Bei laengeren Produktoperationen beide Werte und den vorgeschalteten
Reverse-Proxy passend einstellen. Beim Loeschen eines Sales-Kontos koennen
Demo-Loeschungen fuer mehrere Produkte nacheinander erfolgen; dafuer muss der
Proxy auch die Summe der jeweiligen Wartezeiten erlauben.

## Manuelle Testkonten

Nur fuer eine Entwicklungs- oder Testdatenbank:
`backend/src/main/resources/db/testdata/sales-test-accounts.sql` in DB Navigator
gegen `e_sales` ausfuehren oder aus dem Verzeichnis `e-Sales` importieren:

```bash
docker compose exec -T sales-postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d e_sales' \
  < backend/src/main/resources/db/testdata/sales-test-accounts.sql
```

Das Schema muss bereits durch den Backend-Start mit Flyway erstellt worden sein.
Das Skript ist keine automatische Migration und wird nicht beim Start ausgefuehrt.

| Benutzername | Rolle | Testpasswort |
|---|---|---|
| `test-admin` | Administrator | `TestSales2026!` |
| `test-alex` | Sales | `TestSales2026!` |
| `test-samira` | Sales | `TestSales2026!` |
| `test-julien` | Sales | `TestSales2026!` |

Alle vier Konten sind aktiv und koennen sich direkt anmelden. Das oeffentlich
bekannte Passwort ist ausschliesslich fuer Tests vorgesehen. Das Skript erzeugt
keine Einladungen, Kunden oder Produkt-Demos. Demos bei Bedarf ueber die
Oberflaeche erstellen. Erneutes Ausfuehren ueberschreibt keine vorhandenen Konten,
Passwoerter oder Rollen; bei bereits belegtem Benutzernamen bleibt das alte
Konto unveraendert.

## Arbeitsablauf

Der Administrator legt Sales-Mitarbeiter an und stellt deren einmaligen
Einladungslink bereit. Mitarbeiter setzen bei Annahme ein eigenes Passwort.
Ein Mitarbeiter waehlt ein Produkt und erstellt bei Bedarf seine persoenliche
Demo; Reset und Loeschen betreffen nur die eigene Demo dieses Produkts.
Demo-Zugangsdaten und die Produkt-URL werden vom Produktanbieter geliefert.

Kunden werden fuer das ausgewaehlte Produkt erstellt. Die Einladung des ersten
Kundenadministrators wird im Produkt angenommen, nicht in e-Sales.
Der e-Sales-Administrator kann Kunden zuordnen und nur nach ausdruecklicher
Bestaetigung loeschen. Nicht erreichbare oder deaktivierte Produkte werden als
nicht verfuegbar angezeigt; fehlgeschlagene Aktionen sind keine Erfolge.

## Weitere Produkte

Das e-cafe-Backend implementiert die [Provider-API](docs/provider-contract.md).
Serverseitig `CAFE_PROVIDER_URL=http://e-cafe-backend-1:8080/api/provider/v1`,
`CAFE_PROVIDER_KEY` und `CAFE_PROVIDER_ENABLED=true` setzen. Derselbe zufaellige
Schluessel (mindestens 32 Bytes) muss in e-cafe als `CAFE_PROVIDER_KEY`
konfiguriert sein. Die URL muss aus dem e-Sales-Backend erreichbar sein. Bei
Docker muessen beide Backends ein gemeinsames Netzwerk verwenden.
Es gibt keine Verbindung von e-Sales zu einer Produktdatenbank.

Produkt-Schluessel duerfen weder in React-Build-Variablen noch in oeffentlichen
Reverse-Proxy-Konfigurationen landen. Fuer getrennte Hosts HTTPS und
eingeschraenkte Netzwerkfreigaben verwenden.

## Umstellung von e-cole

Die bisherigen Platform-/Sales-Funktionen in e-cole bleiben bewusst bestehen.
Es werden keine alten Konten, Einladungen oder Kundendaten geloescht, und kein
SQL-Migrationsschritt verschiebt diese automatisch.

Fuer einen kontrollierten Wechsel:

1. Sales-Konten in e-Sales anlegen und neue Einladungen ausgeben.
   Schul-Benutzerkonten verbleiben in e-cole; alte Sales-Passwoerter werden nicht
   automatisch kopiert.
2. Vorhandene Kunden in e-Sales anzeigen und dem passenden Sales-Konto zuordnen.
   Produktdaten bleiben unveraendert. Neue Zuordnungen werden in der
   e-Sales-Datenbank gespeichert, getrennt von alten Platform-Zuordnungen.
3. Bestehende `demo-<username>`-Tenants bei gleicher Sales-Benennung kontrollieren,
   bevor eine Demo zurueckgesetzt wird. Ein Reset ersetzt Demo-Daten und ist
   keine Migration.
4. Nach erfolgreicher Umstellung die alte Sales-Oberflaeche in einer separaten
   Aenderung stilllegen. Bis dahin nur ein System fuer neue Kunden und
   Demo-Resets verwenden.

## Betrieb

Eigene Datenbank und Produktdatenbanken separat sichern. In Produktion das
Portal hinter TLS betreiben, `SALES_PUBLIC_URL` auf die oeffentliche Adresse
setzen und Mailpit durch SMTP ersetzen. Secrets ueber Umgebungsvariablen oder
die Deployment-Secrets-Verwaltung bereitstellen; `.env` nicht versionieren.

Provider-Aktionen laufen synchron. Eine Netzwerkunterbrechung kann nach einer
bereits erfolgten Produktaktion auftreten. Demo-PUT kann bewusst wiederholt
werden. Kunden-Provisionierung bei unklarem Ergebnis zuerst im Produkt pruefen,
statt automatisch erneut anzulegen. Lokale Datenbank und entfernte Provider
bilden keine gemeinsame Transaktion.
