# Security Policy

Semantica shows texts written by translators and administrators together with values given by programs, such as a player's name. A flaw matters when it lets a value be read as markup or as a message (such as a player making others run a command through a MiniMessage click), when a translation can make a server spend unbounded memory or time, or when bundles fetched over HTTP are used without matching their manifest.

## Supported versions

Security fixes go into the next release built from `dev`. Only the latest release receives them; update to it before reporting.

## Reporting a vulnerability

Report vulnerabilities privately through [GitHub's private vulnerability reporting](https://github.com/whereareiam/semantica/security/advisories/new). Do not open a public issue, pull request, or Discord message with the details.

Include:

- the Semantica version, the project it runs in, and the server or bot it runs on
- what a player, a translator or an attacker can do, and what they need for it
- steps to reproduce, with the messages involved

You receive an answer on the report. The fix is released before the details are published, and the release notes name it as a security fix.
