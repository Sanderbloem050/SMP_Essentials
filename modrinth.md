# SMP Essentials — Modrinth publicatie checklist

Gebruik dit bestand als je de mod op Modrinth wilt zetten.

## Eenmalige setup

1. Maak een account op https://modrinth.com
2. Maak een nieuw project aan: **Mod** → naam: `SMP Essentials`
3. Vul in:
   - **Summary**: Een complete SMP-toolkit voor Fabric — valuta, winkels, jobs, claims, quests en QoL.
   - **Description**: kopieer van README.md (markdown werkt)
   - **Categories**: `utility`, `economy`, `game-mechanics`
   - **License**: MIT
   - **Links**: homepage, source, issues (zie fabric.mod.json)
   - **Icon**: het 512×512 icon.png

## Per release

1. Bump versie in `gradle.properties` → `mod_version`
2. Update `CHANGELOG.md` (verplaats Unreleased naar nieuwe versienummer)
3. Commit + tag: `git tag v1.x.x && git push --tags`
4. GitHub Actions bouwt automatisch en maakt een GitHub Release aan
5. Upload dezelfde jar handmatig op Modrinth (of gebruik de Modrinth GitHub Action — zie optie hieronder)

## Automatisch uploaden via GitHub Actions (optioneel)

Voeg toe aan `.github/workflows/release.yml` na de GitHub Release stap:

```yaml
      - name: Publish to Modrinth
        uses: Kir-Antipov/mc-publish@v3.3
        with:
          modrinth-id: <jouw-modrinth-project-id>
          modrinth-token: ${{ secrets.MODRINTH_TOKEN }}
          files: build/libs/smpessentials-*.jar
          name: SMP Essentials v${{ steps.version.outputs.VERSION }}
          version: ${{ steps.version.outputs.VERSION }}
          version-type: release
          game-versions: 26.1.2
          loaders: fabric
          changelog-file: CHANGELOG.md
```

Sla je Modrinth API-token op als GitHub Secret: `MODRINTH_TOKEN`
(Modrinth → instellingen → Personal access tokens)
