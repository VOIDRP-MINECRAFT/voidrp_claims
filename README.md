# VoidRP Claims

Anarchy block-anchored land-claim mod for **VoidRP: Abyss** (NeoForge 26.2, Java 25).

Players protect their base by placing a **Claim Core** block. A claim is a set of
**16×16×16 cube cells** (a volume, not a full-height chunk square): placing the core
gives one cube centred on it, and each upgrade adds one more cube in the direction of
the clicked core face — up to **31 cubes** total. Everything outside a claim is pure
anarchy; inside, only the owner and trusted players (and server ops) may
break/place/interact. **PvP stays on everywhere** — claims protect builds, not players.

Claims are stored in and synced with the VoidRP backend (`/api/v1/claims/*`,
`X-Game-Auth-Secret`) and shown in the player cabinet on the site.

## Raiding

Claims are not permanent. **Explosions breach the claim** — TNT and end crystals destroy
claimed blocks, so raiders blast through the walls. The **core itself is blast-proof** and
must be **mined out** (~37 s with a diamond pickaxe); breaking it drops the claim. When a
non-member starts mining the core, or an explosion breaches the claim, the **online owner
gets a throttled raid alert** in chat.

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
| `voidrp.claims.maxLevel` | `31` (cubes, including the core) |
| `voidrp.claims.upgradeItem` | `minecraft:diamond_block` |
| `voidrp.claims.upgradeItemsPerLevel` | `1` |

## Usage & commands

- Place a **Claim Core** to create a claim (level 1 = one 16×16×16 cube).
- Right-click a core **face** with the upgrade item to grow a cube that way; cost rises
  as the claim grows.
- Right-click the core with an **empty hand** (owner/trusted) to toggle a particle
  **grid** of the claim volume.

`/claim info` · `/claim fill` (solidify the volume) · `/claim trust <player>` ·
`/claim untrust <player>` · `/claim remove` · `/claim give` (op)

Break your own core to remove the claim.
