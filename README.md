# CIVENTRA

CIVENTRA is a Java 17, Spring Boot 3.2, Vaadin 24 civic issue application using Oracle XE and JDBC.

## Run from VS Code on Windows

Open the project folder in VS Code, then run **Terminal → Run Task… → CIVENTRA: Build and run**. This task uses the same credential-loading launcher as the PowerShell workflow below. Press `Ctrl+C` in its terminal to stop the app. Open <http://localhost:8081> after startup.

If the saved Oracle password cannot be decrypted, recreate the local credential in the same Windows account that runs VS Code:

```powershell
Read-Host "Oracle password" -AsSecureString | ConvertFrom-SecureString | Set-Content -NoNewline .civentra-local\oracle-password.secure
```

## Run on the configured Windows development machine

1. Start Oracle Database XE and make sure the `XEPDB1` pluggable database is open and listening on port `1521`.
2. From PowerShell in this project directory, build and launch:

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\build-and-run.ps1
   ```

   This helper stops an already-running CIVENTRA JAR before Maven rebuilds it, avoiding Windows' "Unable to rename ...jar" lock error. It compiles and packages the application without compiling or running tests, then starts the app in the same terminal. Press `Ctrl+C` in that terminal to stop it. Run tests separately with `mvn test`. To build manually, stop CIVENTRA first. Run `mvn -s target/maven-settings.xml '-Dmaven.test.skip=true' package` when that local settings file exists; otherwise use `mvn '-Dmaven.test.skip=true' package`. Then launch with `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\run-civentra.ps1`.

3. Open <http://localhost:8081>.

`run-civentra.ps1` loads the locally DPAPI-encrypted Oracle password from `.civentra-local/oracle-password.secure`, sets connection settings for the app process, then clears the plaintext environment variable on exit. Run it using the same Windows account that configured that encrypted credential. The credential file is git-ignored. Port 8081 is the launcher's default. If you launch the script manually, use `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\run-civentra.ps1` so the bypass applies only to that PowerShell process.

## Database schema

From the project root, you can inspect the schema through Oracle's local Windows administrator authentication (no database password entered):

```sql
sqlplus / as sysdba
ALTER SESSION SET CONTAINER = XEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = CIVENTRA_USER;
SELECT COUNT(*) AS USER_COUNT FROM USERS;
EXIT
```

The configured Oracle schema must already contain the core `USERS`, `ISSUE_CATEGORIES`, and `COMMUNITY_ISSUES` tables. The application does not create tables at startup. Connect as above, set the current schema, and apply the additive migrations in order:

```sql
@sql/migrations/V1_1_add_issue_evidence_path.sql
@sql/migrations/V1_2_issue_status_history.sql
@sql/migrations/V1_3_service_workflows.sql
@sql/migrations/V1_4_service_request_verification.sql
@sql/migrations/V1_5_unique_normalized_user_email.sql
@sql/migrations/V1_6_service_estimates_and_invoice_link.sql
```

These migrations are designed to be rerun safely. They add evidence storage, issue history and assignment fields, service workflow tables, persistent notifications, auditable service completion review, a unique case-insensitive email index, provider estimate review, and service invoice linkage. `V1_5` stops with an explanatory error if existing users already share a normalized email; resolve those accounts before rerunning it. `V1_6` creates the invoice tables if they are not present, links final invoices to service requests, and persists service invoice line items. Setting `CURRENT_SCHEMA` is essential because migration table names are unqualified. Back up the database before applying schema changes.

## Registration

Open <http://localhost:8081/register> or click **Create a citizen account** on the login page. It saves the account to Oracle and stores a BCrypt password hash. Public registration assigns only the configured `CITIZEN` role; it does not create demo users or grant staff roles. The form requires an Indian 10-digit mobile number and a password of at least 8 characters.

## Storage and integrations

Issue photos are stored outside Oracle in the configured upload directory, validated as PNG/JPEG/WebP, limited to 5 MB, and referenced by `EVIDENCE_PATH`. The same-origin `/uploads/` endpoint checks the signed-in user's report, assignment, or administrator role before returning evidence.

The Community Map uses Leaflet with a configurable XYZ tile URL and plots only saved latitude/longitude values; reports without coordinates remain in the nearby list. Configure `CIVENTRA_MAP_TILE_URL` and `CIVENTRA_MAP_ATTRIBUTION` as needed. Its **My location** control uses the browser geolocation permission to center the map; coordinates are not saved or sent to the server. The report form can check for open reports with the same title from the last 30 days; when both coordinates are provided, matches must also be within 0.01 degrees on each coordinate axis. This check is advisory and never blocks submission. No geocoding or weather provider is configured.

The AI assistant requires `CIVENTRA_AI_ENABLED=true` and `CIVENTRA_AI_API_KEY` in the server process environment; the default model is `gpt-4o-mini`. The key stays server-side. Report text is sent only when a user asks the assistant to process it. If AI is not configured or unavailable, the assistant reports that clearly. Deterministic category suggestions in the report workflow are local heuristics and are not presented as AI responses.

Personal service requests require provider acceptance, a provider-entered estimate, citizen approval, provider completion, and administrator verification. Administrator approval creates one final invoice linked uniquely to that request and copies the approved estimate amount and tax; civic issue reports do not create paid-service invoices. Payment processing is not configured, so the app does not mark invoices paid or claim to charge a payment method. Provider and officer accounts must be provisioned by an administrator; public registration creates citizen accounts only. Demo credentials are not seeded by the application.

External map tiles must have appropriate attribution and provider policy compliance. The public OpenStreetMap tile service is best-effort and has no SLA ([tile usage policy](https://operations.osmfoundation.org/policies/tiles/)).
