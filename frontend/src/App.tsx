import { useEffect, useState } from "react";
import "./App.css";
import logo from "./assets/nextelis-logo.png";
import Guide from "./Guide";
import {
  ArrowRight,
  CheckIcon,
  DialerIcon,
  GitHubIcon,
  LinkedInIcon,
  LockIcon,
  ServerIcon,
  WarnIcon,
} from "./icons";

const LINKS = {
  repo: "https://github.com/ShowcaseVault/NexTelis",
  org: "https://github.com/ShowcaseVault",
  author: "https://github.com/Page-Vishal",
  linkedin: "https://www.linkedin.com/in/vishalsigdel",
};

/** Mirrors docs/NEXTELIS-V1.md §1 — kept in sync by hand, deliberately short. */
const FEATURES = [
  {
    icon: <DialerIcon />,
    title: "No app to open",
    body: "NexTelis registers as an Android calling account, the same mechanism a SIM uses. You dial from the stock dialer and the native call screen rings.",
  },
  {
    icon: <ServerIcon />,
    title: "You run the infrastructure",
    body: "A FastAPI control plane and an Asterisk telephony engine on your own Linux box. Numbers are issued by you, not leased from a carrier.",
  },
  {
    icon: <LockIcon />,
    title: "Secrets shown once",
    body: "Device tokens and account recovery codes are stored only as SHA-256 hashes. A stolen database yields nothing usable.",
  },
];

const STACK = [
  {
    label: "Android app",
    body: "Kotlin. Telecom ConnectionService for native call handling, Linphone SDK for SIP/RTP media, and a phone-call foreground service so calls survive backgrounding.",
  },
  {
    label: "Control plane",
    body: "FastAPI and Postgres. Users, numbers, device pairing, and call authorization. It never touches audio.",
  },
  {
    label: "Telephony engine",
    body: "Asterisk with PJSIP realtime, reading its SIP endpoints straight out of Postgres. DTMF negotiated as RFC 4733.",
  },
  {
    label: "Separation",
    body: "Control and media travel separate paths. Voice never goes through an HTTP request — the single most important design decision in the project.",
  },
];

const VERIFIED = [
  "Two-way voice calls between two physical Android phones over Wi-Fi, audio in both directions.",
  "NexTelis listed beside the SIM in Settings › Calling accounts, without disrupting carrier service.",
  "Full onboarding on a clean install: register, save recovery code, pair, get a number.",
  "Re-pair after uninstall: same email, recovery-code prompt, same number restored.",
];

const LIMITS = [
  "Verified on a LAN, not the open Internet — NAT traversal is the next hard problem.",
  "No TLS on either the control or media plane. This is a trusted-network system today.",
  "OnePlus/OxygenOS removes the calling-accounts settings screen, so the account can never be enabled there. Samsung One UI is the supported target.",
  "Single-host deployment, sideload-only distribution, and Linphone's AGPLv3 terms to resolve before any closed-source release.",
];

/** Hash routing rather than a router dependency — the site is two pages. */
function useHashRoute() {
  const [hash, setHash] = useState(() => window.location.hash);

  useEffect(() => {
    const onChange = () => setHash(window.location.hash);
    window.addEventListener("hashchange", onChange);
    return () => window.removeEventListener("hashchange", onChange);
  }, []);

  return hash;
}

function App() {
  const hash = useHashRoute();
  const onGuide = hash.startsWith("#/guide");

  // Anchor links within a page shouldn't fight the router's scroll position.
  useEffect(() => {
    if (hash === "#/guide" || hash === "#/") window.scrollTo(0, 0);
  }, [hash]);

  return (
    <div className="shell">
      <header className="site-header">
        <div className="container site-header__inner">
          <a className="brand" href="#/">
            <img className="brand__mark" src={logo} alt="" />
            <span>NexTelis</span>
          </a>
          <nav className="nav">
            <div className="nav__links nav">
              {onGuide ? (
                <>
                  <a href="#/">Overview</a>
                  <a href="#how-it-works">How it works</a>
                </>
              ) : (
                <>
                  <a href="#how">How it works</a>
                  <a href="#stack">Stack</a>
                  <a href="#status">Status</a>
                </>
              )}
              <a href="#/guide">Setup guide</a>
            </div>
            <a
              className="btn btn--ghost"
              href={LINKS.repo}
              target="_blank"
              rel="noreferrer noopener"
            >
              <GitHubIcon />
              GitHub
            </a>
          </nav>
        </div>
      </header>

      {onGuide ? (
        <main id="top">
          <Guide />
        </main>
      ) : (
        <Landing />
      )}

      <footer className="site-footer">
        <div className="container site-footer__inner">
          <span>NexTelis — experimental private Internet telephony.</span>
          <span>
            Built by{" "}
            <a href={LINKS.linkedin} target="_blank" rel="noreferrer noopener">
              Vishal Sigdel
            </a>{" "}
            ·{" "}
            <a href={LINKS.org} target="_blank" rel="noreferrer noopener">
              ShowcaseVault
            </a>
          </span>
        </div>
      </footer>
    </div>
  );
}

function Landing() {
  return (
    <main id="top">
        <section className="hero">
          <div className="container hero__inner">
            <div>
              <span className="eyebrow">
                <span className="eyebrow__dot" />
                v1 — Private Telephony MVP
              </span>
              <h1>
                A phone network that doesn&apos;t need a <em>phone company</em>.
              </h1>
              <p className="hero__lede">
                We rebuilt chat a hundred times but never rebuilt calling.
                NexTelis is a private, self-hosted telephony system: run the
                server, hand out your own numbers, and call people through the
                dialer that&apos;s already on the phone.
              </p>
              <div className="hero__actions">
                <a
                  className="btn btn--primary"
                  href={LINKS.repo}
                  target="_blank"
                  rel="noreferrer noopener"
                >
                  View the source
                  <ArrowRight />
                </a>
                <a className="btn btn--ghost" href="#/guide">
                  Setup guide
                </a>
              </div>
            </div>
            <div className="hero__art">
              <img
                className="hero__logo"
                src={logo}
                alt="NexTelis logo"
                width={300}
                height={300}
              />
            </div>
          </div>
        </section>

        <section className="section" id="how">
          <div className="container">
            <div className="section__head">
              <h2>Calls ride the native dialer</h2>
              <p>
                Not another calling app with its own contact list and in-call
                screen. NexTelis registers as a call provider, so it coexists
                with your real SIM instead of replacing it.
              </p>
            </div>

            <div className="flow">
              <span className="flow__node">Native dialer</span>
              <span className="flow__arrow">→</span>
              <span className="flow__node">Android Telecom</span>
              <span className="flow__arrow">→</span>
              <span className="flow__node flow__node--accent">NexTelis</span>
              <span className="flow__arrow">→</span>
              <span className="flow__node">Asterisk</span>
              <span className="flow__arrow">→</span>
              <span className="flow__node">Callee</span>
            </div>

            <div className="grid grid--3" style={{ marginTop: 28 }}>
              {FEATURES.map((f) => (
                <article className="card" key={f.title}>
                  <div className="card__icon">{f.icon}</div>
                  <h3>{f.title}</h3>
                  <p>{f.body}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="section" id="stack">
          <div className="container">
            <div className="section__head">
              <h2>What&apos;s under it</h2>
              <p>
                Existing, mature components wired together rather than telephony
                infrastructure written from scratch.
              </p>
            </div>
            <div className="stack">
              {STACK.map((s) => (
                <div className="stack__row" key={s.label}>
                  <div className="stack__label">{s.label}</div>
                  <div className="stack__body">{s.body}</div>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="section" id="status">
          <div className="container">
            <div className="section__head">
              <h2>Where v1 actually stands</h2>
              <p>
                The point of a first version is knowing exactly what you&apos;ve
                proven — and what you haven&apos;t.
              </p>
            </div>

            <div className="grid grid--2">
              <div className="card">
                <h3 style={{ marginBottom: 16 }}>Verified on real hardware</h3>
                <ul className="status-list">
                  {VERIFIED.map((v) => (
                    <li key={v}>
                      <span className="status-list__icon status-list__icon--good">
                        <CheckIcon />
                      </span>
                      <span>{v}</span>
                    </li>
                  ))}
                </ul>
              </div>

              <div className="callout">
                <h3>
                  <WarnIcon /> Known limitations
                </h3>
                <ul className="status-list">
                  {LIMITS.map((l) => (
                    <li key={l}>
                      <span className="status-list__icon status-list__icon--warn">
                        <WarnIcon />
                      </span>
                      <span>{l}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </div>
        </section>

        <section className="section" id="links">
          <div className="container">
            <div className="section__head">
              <h2>Project &amp; author</h2>
              <p>
                NexTelis is built in the open. Full architecture notes,
                experimental findings, and the v1 record live in the repo.
              </p>
            </div>
            <div className="linkrow">
              <a
                className="linkcard"
                href={LINKS.repo}
                target="_blank"
                rel="noreferrer noopener"
              >
                <span className="linkcard__icon">
                  <GitHubIcon />
                </span>
                <span className="linkcard__text">
                  <div className="linkcard__title">ShowcaseVault/NexTelis</div>
                  <div className="linkcard__sub">
                    Source, docs, and release APKs
                  </div>
                </span>
                <span className="linkcard__arrow">
                  <ArrowRight />
                </span>
              </a>

              <a
                className="linkcard"
                href={LINKS.org}
                target="_blank"
                rel="noreferrer noopener"
              >
                <span className="linkcard__icon">
                  <GitHubIcon />
                </span>
                <span className="linkcard__text">
                  <div className="linkcard__title">ShowcaseVault</div>
                  <div className="linkcard__sub">The organization</div>
                </span>
                <span className="linkcard__arrow">
                  <ArrowRight />
                </span>
              </a>

              <a
                className="linkcard"
                href={LINKS.author}
                target="_blank"
                rel="noreferrer noopener"
              >
                <span className="linkcard__icon">
                  <GitHubIcon />
                </span>
                <span className="linkcard__text">
                  <div className="linkcard__title">Page-Vishal</div>
                  <div className="linkcard__sub">Author on GitHub</div>
                </span>
                <span className="linkcard__arrow">
                  <ArrowRight />
                </span>
              </a>

              <a
                className="linkcard"
                href={LINKS.linkedin}
                target="_blank"
                rel="noreferrer noopener"
              >
                <span className="linkcard__icon">
                  <LinkedInIcon />
                </span>
                <span className="linkcard__text">
                  <div className="linkcard__title">Vishal Sigdel</div>
                  <div className="linkcard__sub">LinkedIn</div>
                </span>
                <span className="linkcard__arrow">
                  <ArrowRight />
                </span>
              </a>
            </div>
          </div>
        </section>
    </main>
  );
}

export default App;
