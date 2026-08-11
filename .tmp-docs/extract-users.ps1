$ErrorActionPreference = "Continue"
$paths = @(
  "C:\Users\82108\.cursor\projects\c-Users-82108-Desktop-Bus\agent-transcripts\736f35bd-22bb-4c8c-bb0a-611359e064c4\736f35bd-22bb-4c8c-bb0a-611359e064c4.jsonl",
  "C:\Users\82108\.cursor\projects\c-Users-82108-Desktop-yoonho\agent-transcripts\5c82628c-53b6-4019-ba47-427973a27116\5c82628c-53b6-4019-ba47-427973a27116.jsonl",
  "C:\Users\82108\.cursor\projects\c-Users-82108-Desktop-yoonho\agent-transcripts\6ec01dd5-9dee-4393-b3d6-56a5c7ea30de\6ec01dd5-9dee-4393-b3d6-56a5c7ea30de.jsonl"
)

foreach ($p in $paths) {
  Write-Host "==== FILE ===="
  Write-Host $p
  $lines = Get-Content $p -Encoding UTF8
  Write-Host ("lines=" + $lines.Count)
  $idx = 0
  foreach ($line in $lines) {
    $idx++
    if ($line -notmatch '"role"\s*:\s*"user"') { continue }
    try {
      $j = $line | ConvertFrom-Json
      $text = $null
      $content = $null
      if ($j.message -and $j.message.content) { $content = $j.message.content }
      elseif ($j.content) { $content = $j.content }

      if ($content -is [string]) {
        $text = $content
      } elseif ($content) {
        $parts = @()
        foreach ($c in $content) {
          if ($c.type -eq "text" -and $c.text) { $parts += [string]$c.text }
          elseif ($c.text) { $parts += [string]$c.text }
        }
        $text = $parts -join " "
      }

      if (-not $text) { continue }
      # skip huge tool dumps / system noise
      if ($text -match "^(<|\[)" -and $text.Length -gt 500) { continue }
      $flat = ($text -replace "[\r\n]+", " ").Trim()
      if ($flat.Length -gt 300) { $flat = $flat.Substring(0, 300) + "..." }
      Write-Host ("USER#" + $idx + " " + $flat)
    } catch {
      Write-Host ("USER#" + $idx + " parse-fail")
    }
  }
}
