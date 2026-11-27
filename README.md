# MiraRedeem

## Downloads by server version

| Paper target | Java | JAR | Download |
| --- | --- | --- | --- |
| **1.18.2** | 17 | `MiraRedeem_1.18.2.jar` | [Download legacy JAR](https://github.com/FiveSOCE/Mira-Redeem/releases/download/dev-1.18.2/MiraRedeem_1.18.2.jar) |
| **1.21.11–26.2** | Java 21 baseline; newer server may need Java 25 | `MiraRedeem_1.21.11-26.2.jar` | [Download modern JAR](https://github.com/FiveSOCE/Mira-Redeem/releases/download/compat-modern-1.21.11-26.2/MiraRedeem_1.21.11-26.2.jar) |

Both version-named GitHub release assets have passed CI and been verified. **Intermediate Paper 1.19–1.21.10 and live-server feature parity are not yet certified.** Install only one version-specific JAR.

[Legacy source](https://github.com/FiveSOCE/Mira-Redeem/tree/compat/1.18.2) · [Modern source](https://github.com/FiveSOCE/Mira-Redeem) · [All releases](https://github.com/FiveSOCE/Mira-Redeem/releases)

MiraRedeem issues cryptographically signed, single-use physical vouchers for server rewards. It validates the item and signature before running configured console commands and consuming exactly one voucher.

## Dependencies

- **Required:** Matching Paper server and Java runtime.
- **Optional:** LuckPerms for rank voucher validation and promotion. **Rank-type vouchers require LuckPerms to function.**
- **MiraCore:** Not required. The plugin is independently installed.
- Reward commands may depend on other installed plugins. Back up `plugins/MiraRedeem/` and your signing key before any migration.

## Commands and permissions

| Command | Permission | Function |
| --- | --- | --- |
| `/miraredeem give <player> <redeem> [amount]` | `miraredeem.admin` | Generate authenticated vouchers |
| `/miraredeem list` | `miraredeem.admin` | Show configured voucher types |
| `/miraredeem reload` | `miraredeem.admin` | Reload settings and definitions |

Alias: `/mredeem`. `miraredeem.admin` defaults to OP. Players redeem vouchers by right-clicking, without an admin command.

## How MiraRedeem Works

Redeem definitions are configured with their canonical item appearance and one or more console commands. Generated redeem items contain protected persistent metadata and an HMAC signature; the visible name/lore alone does not decide what executes. On right-click, MiraRedeem validates the hidden identity/signature and the entire canonical configured ItemStack before dispatching the reward commands as console.

Configured commands support `%player%`, `%username%` and `%uuid%`. After successful command dispatch, the player receives the configured success feedback and exactly one voucher is consumed. Modified or forged-looking vouchers are rejected without being consumed. Anvil, grindstone, smithing, crafting and enchanting modification paths are blocked for tagged redeem items.

A random signing secret is generated in `config.yml` on first startup. Do not change it after vouchers have been issued or previously issued vouchers will no longer validate. A typical definition can call another Mira plugin, for example dispatching `mitem give %player% pyro_axe` to redeem a MiraItems Pyro Axe.

## Commands

| Command | Permission | What it does |
| --- | --- | --- |
| `/miraredeem give <player> <redeem> [amount]` | `miraredeem.admin` | Gives one or more securely generated redeem items to a player. |
| `/miraredeem list` | `miraredeem.admin` | Lists configured redeem definitions/IDs. |
| `/miraredeem reload` | `miraredeem.admin` | Reloads MiraRedeem configuration and definitions. |

Alias: `/mredeem`.

Voucher redemption itself is performed by right-clicking the physical item and does not require an administration command.

## Permissions

| Permission | Default | What it does |
| --- | --- | --- |
| `miraredeem.admin` | OP | Allows giving, listing and reloading MiraRedeem definitions. |


## LuckPerms Rank Vouchers (0.1.2)

Rank vouchers are a dedicated `RANK` redeem type rather than generic LuckPerms console commands. MiraRedeem reads the configured LuckPerms track through the LuckPerms Java API and compares the player's current direct track position with the target group before changing anything.

A rank voucher is allowed only when the target group is higher than the player's current group on that track.

Blocked without consuming the signed voucher:
- target rank is the same as the current rank
- target rank is lower than the current rank
- LuckPerms is unavailable
- configured track does not exist
- configured target group is not on that track
- the player's LuckPerms user is unavailable
- the player has multiple direct groups on the same configured track

On success, MiraRedeem removes only direct parent groups belonging to that track, adds the target group, updates the primary group when the previous primary group belonged to the track, and saves the LuckPerms user. Unrelated parent groups are left untouched.

Example:

```yaml
rank_legend:
  type: RANK
  material: PAPER
  name: "&6&lLegend Rank Voucher"
  lore:
    - "&7Right-click to redeem."
    - "&7Upgrades your rank to &6Legend&7."
  track: ranks
  target-group: legend
  success-message: "&aYour rank was upgraded to &6Legend&a!"
```
