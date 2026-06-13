# Hubitat Forge

This repository contains multiple Hubitat features organized by folder.

## Areas

- `VirtualPresence/` — virtual presence detector app, driver, installation guide, and specification
- `TuyaSNZB02/` — Tuya SNZB-02 temperature and humidity sensor driver and specification

## Start here

Open the README inside the area you want to work on:

- `VirtualPresence/README.md`
- `TuyaSNZB02/README.md`

## Groovy syntax checks

The Hubitat Groovy files are validated with a local syntax-check script instead of the dummy .NET build.

- Prerequisite: Groovy 5 (`groovyc`) installed locally, or use the script with `-InstallGroovy` on Windows
- Script: `scripts/Check-GroovySyntax.ps1`
- CI: `.github/workflows/groovy-syntax-check.yml`

Run it locally from PowerShell:

```powershell
.\scripts\Check-GroovySyntax.ps1
```

If Groovy is not installed and `winget` is available:

```powershell
.\scripts\Check-GroovySyntax.ps1 -InstallGroovy
```

## License

See `LICENSE.txt`
