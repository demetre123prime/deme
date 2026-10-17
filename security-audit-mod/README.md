# Minecraft Security Audit Mod

A Fabric client mod that logs basic runtime metadata and can optionally send a fixed, non-sensitive audit notice to a Discord webhook.

## Scope
- Does not read browser profiles, passwords, cookies, tokens, or other credentials.
- Sends only a fixed message: "Minecraft security audit completed."
- Reads the webhook URL from the SECURITY_AUDIT_WEBHOOK environment variable; no webhook secret is committed.

## Build
Requires JDK 21 and Gradle. Open this directory as a Gradle project and run `gradle build`. If dependency resolution fails, update the versions in gradle.properties to a mutually compatible Fabric toolchain.

Never commit a webhook URL to source control.