# VoidRP Claims

Anarchy block-anchored land-claim mod for **VoidRP: Abyss** (NeoForge 26.2, Java 25).

Players protect their base by placing a **Claim Core** block. Protection is
**level-expandable**: level `L` protects a `(2L-1)²` chunk square around the
core chunk (max level 10). Everything outside a claim is pure anarchy; inside,
only the owner and trusted players (and server ops) may break/place/interact.
**PvP stays on everywhere** — claims protect builds, not players.

Claims are stored in and synced with the VoidRP backend (`/api/v1/claims/*`,
`X-Game-Auth-Secret`) and shown in the player cabinet on the site.

## Build

```bash
./gradlew jar          # -> build/libs/voidrp_claims-<ver>+mc26.2.jar
```

## Config (JVM system properties)

Reuses the auth-bridge secret/backend by default (no extra flags needed):

| Property | Default |
|---|---|
| `voidrp.claims.backend` | `voidrp.auth.backend` → `https://api.void-rp.ru` |
| `voidrp.claims.gameSecret` | `voidrp.auth.gameSecret` |
| `voidrp.claims.serverSlug` | (empty → default server) |
| `voidrp.claims.maxLevel` | `10` |
| `voidrp.claims.upgradeItem` | `minecraft:diamond_block` |
| `voidrp.claims.upgradeItemsPerLevel` | `1` |

## Commands

`/claim info` · `/claim trust <player>` · `/claim untrust <player>` ·
`/claim remove` · `/claim give` (op)

Place a Claim Core to create a claim; right-click it with the upgrade item to
expand; break your own core to remove the claim.
