<#
.SYNOPSIS
    Connects the app to a Supabase project in one step (SETUP.md, section 6).

.DESCRIPTION
    Writes SUPABASE_URL and SUPABASE_ANON_KEY into local.properties and puts the project ref
    into firestore.rules, so post, eco and story URLs from that project pass the rules.
    Run it again with other values to point the app at a different project.

.EXAMPLE
    ./scripts/configure-supabase.ps1 -Url https://abcdefghijklmnopqrst.supabase.co -AnonKey eyJhbGciOi...
#>
param(
    [Parameter(Mandatory = $true)] [string] $Url,
    [Parameter(Mandatory = $true)] [string] $AnonKey
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

$Url = $Url.Trim().TrimEnd('/')
if ($Url -notmatch '^https://([a-z0-9]{20})\.supabase\.co$') {
    throw "La URL debe verse como https://xxxxxxxxxxxxxxxxxxxx.supabase.co (Project Settings > API)."
}
$ref = $Matches[1]

$AnonKey = $AnonKey.Trim()
if ($AnonKey.Length -lt 20 -or $AnonKey -match '\s') {
    throw "La anon key no parece valida: copiala completa desde Project Settings > API."
}
if ($AnonKey -match 'service_role' -or $AnonKey.StartsWith('sb_secret_')) {
    throw "Esa es la clave secreta (service_role). Usa la anon / publishable key: la secreta nunca va en la app."
}

# local.properties: replace the keys if they exist, append them otherwise
$props = Join-Path $root 'local.properties'
$lines = if (Test-Path $props) { @(Get-Content $props) } else { @() }
foreach ($entry in @(@('SUPABASE_URL', $Url), @('SUPABASE_ANON_KEY', $AnonKey))) {
    $key = $entry[0]
    $line = "$key=$($entry[1])"
    $index = [Array]::FindIndex([string[]]$lines, [Predicate[string]] { param($l) $l -match "^\s*$key\s*=" })
    if ($index -ge 0) { $lines[$index] = $line } else { $lines += $line }
}
# No BOM: Gradle would read it as part of the first key
$utf8 = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllText($props, (($lines -join "`n") + "`n"), $utf8)
Write-Host "local.properties actualizado (SUPABASE_URL y SUPABASE_ANON_KEY)."

# firestore.rules: the media URL prefix names the project
$rulesPath = Join-Path $root 'firestore.rules'
$rules = Get-Content $rulesPath -Raw
$pattern = "\^https://(SUPABASE_PROJECT_REF|[a-z0-9]{20})\\\\\.supabase"
if ($rules -notmatch $pattern) {
    throw "No encontre el prefijo de Supabase en firestore.rules."
}
$rules = [regex]::Replace($rules, $pattern, "^https://$ref\\.supabase")
[IO.File]::WriteAllText($rulesPath, $rules, $utf8)
Write-Host "firestore.rules apunta al proyecto $ref."

Write-Host ""
Write-Host "Faltan estos pasos en la consola (SETUP.md, seccion 6):"
Write-Host "  1. Supabase > Authentication > Third-party Auth > Firebase, Project ID iso-social"
Write-Host "  2. Supabase > SQL Editor: ejecutar supabase/storage-setup.sql"
Write-Host "  3. firebase deploy --only firestore:rules --project iso-social"
Write-Host "  4. Recompilar la app (Build > Rebuild o ./gradlew assembleDebug)"
