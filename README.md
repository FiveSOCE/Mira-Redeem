# MiraRedeem

MiraRedeem provides secure one-time physical redeem items for the Mira Paper server suite. Each voucher has hidden signed identity and executes one or more configured console commands when a player successfully redeems it.

## Download

[**Download MiraRedeem v0.1.1**](https://github.com/FiveSOCE/Mira-Redeem/releases/download/v0.1.1/MiraRedeem-0.1.1.jar)

## Requirements / Dependencies

- Paper 1.21.11
- Java 21
- No required third-party plugin dependencies
- Any plugin/command referenced by a redeem definition must of course be installed for that reward command to succeed

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
