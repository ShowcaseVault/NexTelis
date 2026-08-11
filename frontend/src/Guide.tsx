import { ArrowRight, CheckIcon, WarnIcon } from "./icons";

/**
 * Setup + usage guide. Commands here are the real ones from the Makefile and
 * scripts/build-release.sh — if those change, change these.
 */

type Step = {
  title: string;
  body: string;
  code?: string;
  note?: string;
};

const SERVER_STEPS: Step[] = [
  {
    title: "Clone and configure",
    body: "Copy the example environment file and fill in the secrets. SECRETS_ENCRYPTION_KEY encrypts SIP passwords at rest, so generate a real one rather than reusing a placeholder.",
    code: `git clone https://github.com/ShowcaseVault/NexTelis.git
cd NexTelis
cp .env.example .env

# generate the at-rest encryption key
python -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())"`,
  },
  {
    title: "Start everything",
    body: "One command brings up Postgres, Asterisk, the FastAPI backend, and this site. Database migrations run automatically as the backend starts.",
    code: "make up-all",
    note: "First run builds the images and takes a few minutes, then seconds after that. For development, make up starts only Postgres and Asterisk so you can run the backend on the host with make dev.",
  },
  {
    title: "Check it came up",
    body: "The site answers on port 8080 and proxies the API. Asterisk should report its endpoints.",
    code: `curl -s -o /dev/null -w "%{http_code}\\n" http://localhost:8080/
make check`,
  },
];

const PHONE_STEPS: Step[] = [
  {
    title: "Install the app",
    body: "Grab the signed APK from the repo releases, or build it yourself. Both phones need it, and both must reach the server — usually the same Wi-Fi network.",
    code: "bash scripts/build-release.sh --release",
  },
  {
    title: "Point it at your server",
    body: "On first launch the app asks for the server host and port. There is no hardcoded address, so the same APK works against any deployment. Enter your machine's LAN IP and port 8000.",
    note: "Not localhost — that resolves to the phone itself. Use the server's actual LAN address, e.g. 192.168.1.7.",
  },
  {
    title: "Register and save the recovery code",
    body: "Enter a name and email. The app shows a recovery code exactly once. Save it — it is the only way to move your number to a new phone later, and it is never shown again.",
  },
  {
    title: "Enable NexTelis as a calling account",
    body: "Grant the requested permissions, then open Settings › Calling accounts and switch NexTelis on. Android deliberately provides no way for an app to enable this itself.",
    note: "On OnePlus/OxygenOS this screen has no toggle and NexTelis cannot be enabled. Samsung One UI is the supported target.",
  },
  {
    title: "Call",
    body: "You are assigned a number. Dial another NexTelis number from the normal phone dialer and pick NexTelis when asked which account to call with. The native call screen handles the rest.",
  },
];

function StepList({ steps, start = 1 }: { steps: Step[]; start?: number }) {
  return (
    <ol className="steps">
      {steps.map((s, i) => (
        <li className="step" key={s.title}>
          <div className="step__num">{start + i}</div>
          <div className="step__content">
            <h3>{s.title}</h3>
            <p>{s.body}</p>
            {s.code && (
              <pre className="code">
                <code>{s.code}</code>
              </pre>
            )}
            {s.note && (
              <p className="step__note">
                <WarnIcon /> {s.note}
              </p>
            )}
          </div>
        </li>
      ))}
    </ol>
  );
}

export default function Guide() {
  return (
    <>
      <section className="section section--first">
        <div className="container">
          <div className="section__head">
            <span className="eyebrow">
              <span className="eyebrow__dot" />
              Setup guide
            </span>
            <h2 style={{ marginTop: 20 }}>Run your own NexTelis</h2>
            <p>
              You need a Linux machine for the server and at least two Android
              phones on the same network. No SIM, no carrier, no cellular
              hardware.
            </p>
          </div>

          <h3 className="subhead">On the server</h3>
          <StepList steps={SERVER_STEPS} />

          <h3 className="subhead" style={{ marginTop: 48 }}>
            On each phone
          </h3>
          <StepList steps={PHONE_STEPS} />
        </div>
      </section>

      <section className="section" id="how-it-works">
        <div className="container">
          <div className="section__head">
            <h2>How a call actually travels</h2>
            <p>
              Two separate paths. Control decisions go over HTTP to the backend;
              audio never does.
            </p>
          </div>

          <div className="grid grid--2">
            <div className="card">
              <h3 style={{ marginBottom: 14 }}>Placing a call</h3>
              <ul className="status-list">
                {[
                  "You dial a NexTelis number in the stock Android dialer.",
                  "Android Telecom routes it to the NexTelis ConnectionService.",
                  "The app asks the backend whether the destination is callable.",
                  "Linphone sends a SIP INVITE to Asterisk.",
                  "Asterisk finds the callee's registered device and rings it.",
                  "Audio flows as RTP between the phones via Asterisk.",
                ].map((t) => (
                  <li key={t}>
                    <span className="status-list__icon status-list__icon--good">
                      <ArrowRight />
                    </span>
                    <span>{t}</span>
                  </li>
                ))}
              </ul>
            </div>

            <div className="card">
              <h3 style={{ marginBottom: 14 }}>Receiving one</h3>
              <ul className="status-list">
                {[
                  "Your phone keeps a SIP registration alive with Asterisk.",
                  "Asterisk delivers the INVITE to that registration.",
                  "The app hands the call to Android Telecom.",
                  "The native incoming-call screen rings, as it would for a SIM call.",
                  "Unknown numbers are resolved to names via the NexTelis directory.",
                ].map((t) => (
                  <li key={t}>
                    <span className="status-list__icon status-list__icon--good">
                      <ArrowRight />
                    </span>
                    <span>{t}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="section__head">
            <h2>Useful commands</h2>
            <p>Everything is wrapped in the Makefile.</p>
          </div>
          <div className="stack">
            {[
              ["make help", "List every target with a one-line description."],
              ["make up", "Start Postgres and Asterisk only."],
              ["make up-all", "Start those plus the backend and this site."],
              ["make down", "Stop everything."],
              [
                "make check",
                "SIP endpoints, live registrations, channels, and uptime.",
              ],
              ["make logs s=backend", "Follow one service's logs."],
              ["make rebuild s=frontend", "Rebuild and restart one service."],
              ["make psql", "Open a database shell."],
              ["make cli", "Drop into the Asterisk console."],
            ].map(([cmd, desc]) => (
              <div className="stack__row" key={cmd}>
                <div className="stack__label">
                  <code>{cmd}</code>
                </div>
                <div className="stack__body">{desc}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="callout">
            <h3>
              <WarnIcon /> Before you rely on this
            </h3>
            <ul className="status-list">
              {[
                "There is no TLS. Traffic is unencrypted, so keep this on a network you trust.",
                "Calls are verified on a LAN. NAT traversal across the open Internet is not solved yet.",
                "Losing your recovery code means losing the account — there is no self-service reset.",
                "OnePlus/OxygenOS cannot enable the calling account at all.",
              ].map((t) => (
                <li key={t}>
                  <span className="status-list__icon status-list__icon--warn">
                    <CheckIcon />
                  </span>
                  <span>{t}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>
    </>
  );
}
