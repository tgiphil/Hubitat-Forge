[CmdletBinding()]
param(
	[string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot),
	[switch]$InstallGroovy
)

$ErrorActionPreference = 'Stop'

function Get-GroovyCompiler {
	$command = Get-Command groovyc -ErrorAction SilentlyContinue
	if ($command) {
		return $command.Source
	}

	$command = Get-Command groovyc.bat -ErrorAction SilentlyContinue
	if ($command) {
		return $command.Source
	}

	$candidatePaths = @(
		'C:\Program Files\Groovy\bin\groovyc.bat',
		'C:\Program Files (x86)\Groovy\bin\groovyc.bat'
	)

	foreach ($candidate in $candidatePaths) {
		if (Test-Path $candidate) {
			return $candidate
		}
	}

	return $null
}

function Ensure-GroovyInstalled {
	if (Get-GroovyCompiler) {
		return
	}

	if (-not $InstallGroovy) {
		throw "groovyc was not found. Install Groovy 5 or rerun with -InstallGroovy on Windows."
	}

	$winget = Get-Command winget -ErrorAction SilentlyContinue
	if (-not $winget) {
		throw "groovyc was not found and winget is unavailable. Install Groovy manually, then rerun this script."
	}

	& $winget.Source install --id Apache.Groovy.5 --silent --accept-package-agreements --accept-source-agreements | Out-Host
	if (-not (Get-GroovyCompiler)) {
		throw "Groovy installation completed, but groovyc is still not available."
	}
}

Ensure-GroovyInstalled

$groovyc = Get-GroovyCompiler
if (-not $groovyc) {
	throw "groovyc was not found."
}

$files = @(
	(Join-Path $RepositoryRoot 'TuyaSNZB02\Drivers\TuyaSnzb02TemperatureHumidityDriver.groovy'),
	(Join-Path $RepositoryRoot 'VirtualPresence\Apps\VirtualPresenceDetectorApp.groovy'),
	(Join-Path $RepositoryRoot 'VirtualPresence\Drivers\VirtualPresenceDetectorDriver.groovy')
)

foreach ($file in $files) {
	if (-not (Test-Path $file)) {
		throw "Source file not found: $file"
	}
}

$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) 'Hubitat-Forge-Groovy'
$stubRoot = Join-Path $tempRoot 'hubitat\zigbee\zcl'
$outputRoot = Join-Path $tempRoot 'classes'

New-Item -ItemType Directory -Force -Path $stubRoot | Out-Null
New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null

$stubFile = Join-Path $stubRoot 'DataType.groovy'
$stubContent = @'
package hubitat.zigbee.zcl

class DataType {
	static final int INT16 = 0
	static final int UINT16 = 0
	static final int UINT8 = 0
}
'@

[System.IO.File]::WriteAllText($stubFile, $stubContent, [System.Text.UTF8Encoding]::new($false))

try {
	& $groovyc -d $outputRoot $stubFile $files
	if ($LASTEXITCODE -ne 0) {
		throw "groovyc exited with code $LASTEXITCODE."
	}
	Write-Host "Groovy syntax check passed."
}
finally {
	if (Test-Path $tempRoot) {
		Remove-Item $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
	}
}