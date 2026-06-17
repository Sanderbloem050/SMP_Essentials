# Buy-and-Sell Shopkeepers Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Make shopkeepers support both selling items *to* players and buying items *from* players, so player shops become a real two-way economy feature.

**Architecture:** Reuse the existing Shopkeeper + AdminShop GUI flow instead of building a separate system. Extend `ShopTrade` with a trade mode, teach `ShopkeeperEntity` how to process both directions safely against chest stock and owner wallet payout, and expose the mode clearly in the admin UI.

**Tech Stack:** Fabric mod, Java, custom entity/menu/screen stack, wallet-based economy, chest-backed stock.

---

## Why this should be the next step

This is the strongest next feature after the current roadmap state because it is:
- **High impact:** every shopping district instantly gets deeper gameplay
- **Visible in-world:** fits the existing "physical blocks/NPCs" direction better than abstract systems
- **Low-risk leverage:** builds directly on code that already exists (`ShopkeeperEntity`, `AdminShopMenu`, `AdminShopScreen`)
- **Economy-positive:** creates item sinks/sources and encourages player specialisation
- **Closer to done than server-wide events:** events would require a broader game loop, scheduling, balancing, and announcement UX

## Recommendation

Implement **buy-and-sell shops** before server-wide events.

### Scope for v1
- A trade can be either:
  - **SELL**: shop sells item to player for coins/wallet balance
  - **BUY**: shop buys item from player and pays coins/wallet balance to the player
- Use the same admin screen to configure both modes
- Keep one chest as the shop’s backing inventory
  - SELL trades require stock in chest
  - BUY trades deposit purchased items into chest
- For BUY trades, money should come from the **shop owner wallet**
- If chest is full or owner wallet lacks balance, the trade is unavailable

### Out of scope for this step
- Separate input/output chests
- Dynamic pricing
- Transaction history
- Per-trade stock caps beyond chest capacity
- Tax / marketplace fee system
- Server-wide events tied to shop activity

---

## Current context

Relevant existing files and behavior:
- `src/main/java/com/sanderbloem/currencymod/entity/ShopkeeperEntity.java`
  - already supports chest-backed stock
  - already builds merchant offers
  - already handles merchant screen opening
- `src/main/java/com/sanderbloem/currencymod/entity/ShopTrade.java`
  - currently stores only `item` + `priceCopper`
- `src/main/java/com/sanderbloem/currencymod/menu/AdminShopMenu.java`
  - already supports adding/removing trades and setting price
- `src/main/java/com/sanderbloem/currencymod/client/AdminShopScreen.java`
  - already renders admin controls
- `src/main/java/com/sanderbloem/currencymod/commands/ShopkeeperCommand.java`
  - already has `spawn`, `addtrade`, `removetrade`, `setname`, `buy`, `remove`
- `src/main/resources/assets/smpessentials/lang/en_us.json`
  - currently only has core economy strings

Technical reality to preserve:
- Existing SELL shop behavior must remain working
- The owner/admin UX should stay simple and in-world
- Avoid introducing a second entirely separate shop entity/menu if not necessary

---

## Proposed approach

### Core model change
Add a trade mode enum to `ShopTrade`.

Suggested shape:
- `SELL_TO_PLAYER`
- `BUY_FROM_PLAYER`

Each trade remains:
- template item stack
- price in copper
- mode

### Runtime behavior
For **SELL_TO_PLAYER**:
- existing behavior mostly remains
- availability depends on chest stock
- player pays via physical coins + wallet fallback using existing payment logic

For **BUY_FROM_PLAYER**:
- shop checks if player has matching item(s)
- shop checks if owner wallet can afford payout
- shop checks if chest has space to receive the item(s)
- item(s) are removed from player inventory
- payout goes to player wallet
- owner wallet is debited
- item(s) are inserted into the shop chest

### UI behavior
In admin menu:
- owner chooses price
- owner chooses **mode** (`SELL` / `BUY`)
- owner inserts template item
- owner clicks add
- displayed trades show mode visually, not just price

### Merchant UX
In the player-facing merchant screen:
- both trade types can appear in the same NPC
- BUY trades should read naturally using the offered item/result arrangement
- if vanilla merchant semantics become too confusing, fallback option is to keep SELL in merchant UI and expose BUY via shift-right-click player menu later — but try the simpler unified route first

---

## Step-by-step plan

### Task 1: Extend the trade model with direction

**Objective:** Add explicit trade direction so the code can distinguish selling versus buying.

**Files:**
- Modify: `src/main/java/com/sanderbloem/currencymod/entity/ShopTrade.java`
- Create: `src/main/java/com/sanderbloem/currencymod/entity/ShopTradeMode.java`

**Steps:**
1. Create `ShopTradeMode` enum with `SELL_TO_PLAYER` and `BUY_FROM_PLAYER`.
2. Extend `ShopTrade` to store `mode`.
3. Update `ShopTrade.CODEC` to serialize/deserialize the new mode.
4. Keep backward compatibility in mind:
   - if practical, default missing old saved trades to `SELL_TO_PLAYER`
   - if codec migration is awkward, document that pre-existing trades may need recreation
5. Add getters needed by UI/entity code.

**Validation:**
- `./gradlew build`
- Confirm no compile failures from changed constructor usage.

---

### Task 2: Update trade creation paths to supply mode

**Objective:** Ensure every place that creates a shop trade sets the direction explicitly.

**Files:**
- Modify: `src/main/java/com/sanderbloem/currencymod/menu/AdminShopMenu.java`
- Modify: `src/main/java/com/sanderbloem/currencymod/commands/ShopkeeperCommand.java`

**Steps:**
1. Update all `new ShopTrade(...)` calls to pass a mode.
2. Preserve existing command behavior by defaulting command-created trades to `SELL_TO_PLAYER`.
3. Optionally add future-friendly command aliases later, but do not expand command scope in this pass unless needed.

**Validation:**
- `./gradlew build`
- Existing `/shopkeeper addtrade <price>` should still create a normal sell trade.

---

### Task 3: Add admin-side mode state to the menu

**Objective:** Let the shop owner choose whether the new trade buys or sells.

**Files:**
- Modify: `src/main/java/com/sanderbloem/currencymod/menu/AdminShopMenu.java`

**Steps:**
1. Add a `DataSlot` for pending trade mode.
2. Add a button id for toggling mode.
3. Expose helper getters for current pending mode.
4. When the owner clicks add:
   - read input item
   - read price
   - read pending mode
   - create trade with mode
5. Keep price reset behavior unchanged after adding a trade.

**Validation:**
- `./gradlew build`
- Menu still opens and existing add/remove behavior still compiles.

---

### Task 4: Show trade mode clearly in the admin screen

**Objective:** Make BUY vs SELL unmistakable in the owner GUI.

**Files:**
- Modify: `src/main/java/com/sanderbloem/currencymod/client/AdminShopScreen.java`

**Steps:**
1. Add a visible toggle button near the price controls.
2. Display current pending mode with strong wording, e.g.:
   - `Mode: SELL TO PLAYER`
   - `Mode: BUY FROM PLAYER`
3. Update rendered listing labels so existing configured trades also indicate mode.
4. Keep spacing readable; do not regress into cramped layout.

**Validation:**
- `./gradlew build`
- Manual in-game check: owner can see and switch mode before clicking add.

---

### Task 5: Refactor shopkeeper trade execution helpers

**Objective:** Separate SELL and BUY execution logic into clear helper methods before touching merchant offer behavior.

**Files:**
- Modify: `src/main/java/com/sanderbloem/currencymod/entity/ShopkeeperEntity.java`
- Possibly modify: `src/main/java/com/sanderbloem/currencymod/entity/PaymentHelper.java`

**Steps:**
1. Extract or add helper methods for:
   - counting matching items in player inventory
   - removing matching items from player inventory
   - checking chest insertion capacity
   - depositing purchased items into chest
   - transferring owner-wallet payout to player wallet
2. Keep helper methods small and side-effect-safe.
3. Use existing coin/wallet utilities where possible; do not duplicate payment logic.

**Validation:**
- `./gradlew build`
- Code remains readable and ready for behavior split.

---

### Task 6: Implement SELL trade availability and behavior cleanly

**Objective:** Lock down the current sell flow before adding buy flow.

**Files:**
- Modify: `src/main/java/com/sanderbloem/currencymod/entity/ShopkeeperEntity.java`

**Steps:**
1. Make the SELL path explicit based on `trade.getMode()`.
2. Preserve chest-stock-based availability.
3. Preserve current player payment behavior.
4. Verify owner payout/storage assumptions still hold.
5. Make sure no regression occurs for existing shops.

**Validation:**
- `./gradlew build`
- Manual in-game test:
  - spawn shopkeeper
  - add SELL trade
  - put stock in chest
  - buy item successfully
  - test out-of-stock case

---

### Task 7: Implement BUY trade behavior

**Objective:** Allow shops to purchase items from players.

**Files:**
- Modify: `src/main/java/com/sanderbloem/currencymod/entity/ShopkeeperEntity.java`
- Possibly modify: `src/main/java/com/sanderbloem/currencymod/data/WalletData.java` only if helper access is missing

**Steps:**
1. For BUY trades, determine required item count from the template stack.
2. Check the player inventory contains enough matching items.
3. Check the owner wallet balance is sufficient.
4. Check chest has room to receive the item stack.
5. Remove items from player inventory.
6. Deposit payout into player wallet.
7. Subtract payout from owner wallet.
8. Insert received items into shop chest.
9. Fail safely if any precondition is not met.

**Validation:**
- `./gradlew build`
- Manual in-game tests:
  - BUY trade succeeds with enough owner balance and chest space
  - fails with full chest
  - fails with insufficient owner balance
  - fails when player lacks enough items

---

### Task 8: Decide how BUY trades appear in the player UI

**Objective:** Make the player-facing interaction understandable, not just technically functional.

**Files:**
- Modify: `src/main/java/com/sanderbloem/currencymod/entity/ShopkeeperEntity.java`
- Possibly modify merchant-offer generation logic there

**Steps:**
1. Try representing BUY trades through merchant offers in a readable way.
2. Verify whether the vanilla merchant screen communicates the direction clearly enough.
3. If confusing, add naming/lore/custom name hints on the trade output/input item.
4. Only if still confusing, define a fallback follow-up plan for a custom buyer UI — but do not scope-creep this implementation unless necessary.

**Validation:**
- Manual in-game UX check:
  - a new player should understand whether the shop buys or sells without being told

---

### Task 9: Improve text, labels, and error messages

**Objective:** Make the new system self-explanatory.

**Files:**
- Modify: `src/main/resources/assets/smpessentials/lang/en_us.json`
- Possibly modify hardcoded `Component.literal(...)` text in:
  - `src/main/java/com/sanderbloem/currencymod/entity/ShopkeeperEntity.java`
  - `src/main/java/com/sanderbloem/currencymod/client/AdminShopScreen.java`
  - `src/main/java/com/sanderbloem/currencymod/commands/ShopkeeperCommand.java`

**Steps:**
1. Add labels for mode, owner-wallet shortage, chest full, missing items, etc.
2. Standardize terminology:
   - `Sell to player`
   - `Buy from player`
   - `Owner wallet`
   - `Shop storage full`
3. Prefer consistent English copy if the repo is currently being prepared for CurseForge.

**Validation:**
- `./gradlew build`
- Manual in-game checks for readable feedback in success/failure cases.

---

### Task 10: Update docs and release-facing copy

**Objective:** Reflect the new capability in the mod’s docs and roadmap.

**Files:**
- Modify: `README.md`
- Modify: `docs/index.html`
- Modify: `wiki/index.html`

**Steps:**
1. Update feature list from one-way shopkeeper to buy-and-sell shopkeeper.
2. Mark roadmap item `Koop-én-verkoop-shops` as done if implementation is complete.
3. Add one concise explanation of how BUY shops differ from SELL shops.
4. Add one screenshot TODO note if screenshots are not ready yet.

**Validation:**
- Read rendered text for clarity
- Verify no stale roadmap wording remains

---

## Files likely to change

### Core logic
- `src/main/java/com/sanderbloem/currencymod/entity/ShopTrade.java`
- `src/main/java/com/sanderbloem/currencymod/entity/ShopTradeMode.java` *(new)*
- `src/main/java/com/sanderbloem/currencymod/entity/ShopkeeperEntity.java`
- `src/main/java/com/sanderbloem/currencymod/entity/PaymentHelper.java` *(possibly)*

### Admin UI / config flow
- `src/main/java/com/sanderbloem/currencymod/menu/AdminShopMenu.java`
- `src/main/java/com/sanderbloem/currencymod/client/AdminShopScreen.java`
- `src/main/java/com/sanderbloem/currencymod/commands/ShopkeeperCommand.java`

### Copy / docs
- `src/main/resources/assets/smpessentials/lang/en_us.json`
- `README.md`
- `docs/index.html`
- `wiki/index.html`

---

## Validation checklist

### Build validation
- Run: `./gradlew build`
- Expected: `BUILD SUCCESSFUL`

### Manual gameplay validation
1. Spawn a shopkeeper on/above a chest.
2. Create one SELL trade.
3. Create one BUY trade.
4. Confirm SELL trade:
   - works with chest stock
   - fails when stock is gone
5. Confirm BUY trade:
   - works when player has the item
   - pays from owner wallet
   - deposits item into chest
   - fails when chest is full
   - fails when owner wallet is too low
6. Confirm mixed-mode NPC still feels understandable.
7. Confirm existing old SELL-only shops still work after reload.

### Regression checks
- Shift-right-click admin still opens correctly
- Remove-trade flow still works
- Shop name setting still works
- No dupe bug from failed BUY transaction ordering
- No item loss when chest insertion fails

---

## Risks and tradeoffs

### Risk 1: Vanilla merchant UI may be ambiguous for BUY trades
**Mitigation:** start with strong mode labels/custom item names; only escalate to custom buyer UI if truly necessary.

### Risk 2: Owner wallet payout introduces cross-player economy side effects
**Mitigation:** precheck owner balance before removing player items; perform operations in safe order.

### Risk 3: Inventory/chest insertion edge cases can cause dupes or losses
**Mitigation:** verify all conditions first, then mutate in a deterministic order.

### Risk 4: Save-data migration for `ShopTrade` may affect old shops
**Mitigation:** default legacy trades to `SELL_TO_PLAYER` if codec can support it; otherwise note migration expectation clearly.

---

## Open questions

1. Should BUY trades pay to **wallet only**, or also try to give physical coins if desired?
   - Recommendation: wallet only for v1.
2. Should BUY trades use the same chest for storage as SELL trades?
   - Recommendation: yes, for simplicity.
3. Should shop owners be able to reorder trades?
   - Not needed in this step.
4. Should command-based `/shopkeeper addtrade` gain a mode argument?
   - Not required for v1; GUI can lead.

---

## After this step

Once buy-and-sell shops are stable, the next best follow-up is:
- **server-wide events**, especially rotating merchants / timed bounty waves / treasure announcements

But do that **after** the economy loop is stronger, because events will land better on top of a richer player market.

---

## Suggested execution order summary

1. Add trade mode model
2. Feed mode through menu/command creation
3. Add admin UI toggle + labels
4. Preserve SELL behavior explicitly
5. Add BUY behavior with wallet + chest checks
6. Polish messages
7. Update docs/roadmap
8. Manual in-game validation
