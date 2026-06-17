# Changelog

Alle noemenswaardige wijzigingen worden hier bijgehouden.
Formaat gebaseerd op [Keep a Changelog](https://keepachangelog.com/nl/1.0.0/).

---

## [Unreleased]

### Toegevoegd
- Server-brede events (gepland)

---

## [1.0.0] — 2026-06-17

### Toegevoegd
- **Valuta**: fysieke koper/zilver/goud-munten (1g = 9s = 81c), crafting, mob-drops, loot-chests
- **Wallet**: `/balance`, `/pay`, `/deposit`, `/withdraw` — persistente opslag via Codec SavedData
- **Bank/ATM-blok**: craftbaar blok voor wallet-beheer via GUI
- **Jobs**: automatische wallet-beloning bij blokken breken en oogsten
- **Shopkeeper-NPC**: spawnt op aangekeken blok, onsterfelijk, kijkt speler aan; admin-GUI via shift+klik; koop- én verkoopmode per artikel; kist-voorraad
- **Veilinghuis**: `/ah sell|buy|mine|cancel` + klikbare chat; `auction_terminal`-blok
- **Loot Crates**: 3 tiers (bronze/silver/gold) elk met eigen sleutel, loot-pool en particles
- **Land-claims**: `/claim|unclaim|claims|trust|untrust`, chunk-bescherming, zichtbare grenzen (`/claim show`); instelbare kosten en max per speler via `/smpadmin claims`
- **Quality of Life**: `/sethome|home|delhome|homes`, `/warp|setwarp|delwarp|warps`, `/spawn`, `/tpa|tpahere|tpaccept|tpdeny`, `/back`, death-chest
- **Bounties**: `/bounty set|list`, betaling bij kill, server broadcast; `bounty_board`-blok
- **NPC-Quests**: Quest Giver (item inleveren voor beloning), Quest Seeker (verstopte NPC), Quest Board (community quests)
- **Admin GUI**: `/smpadmin` — toggle jobs/claims/deathChest/crates/bounties/auction; claim-instellingen

[Unreleased]: https://github.com/Sanderbloem050/SMP_Essentials/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Sanderbloem050/SMP_Essentials/releases/tag/v1.0.0
