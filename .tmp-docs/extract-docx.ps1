$ErrorActionPreference = "Stop"
$outDir = "C:\Users\82108\Desktop\Bus_alpha\.tmp-docs"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$files = Get-ChildItem "C:\Users\82108\Desktop\Bus\docs\*.docx"
foreach ($f in $files) {
  $destZip = Join-Path $env:TEMP ("docx_" + [guid]::NewGuid().ToString() + ".zip")
  $extract = Join-Path $env:TEMP ("docx_ex_" + [guid]::NewGuid().ToString())
  Copy-Item $f.FullName $destZip -Force
  New-Item -ItemType Directory -Force -Path $extract | Out-Null
  Expand-Archive -Path $destZip -DestinationPath $extract -Force
  $xmlPath = Join-Path $extract "word\document.xml"
  if (-not (Test-Path $xmlPath)) {
    Write-Host "NO XML: $($f.Name)"
    continue
  }

  [xml]$doc = Get-Content $xmlPath -Encoding UTF8
  $ns = New-Object System.Xml.XmlNamespaceManager($doc.NameTable)
  $ns.AddNamespace("w", "http://schemas.openxmlformats.org/wordprocessingml/2006/main")
  $paras = $doc.SelectNodes("//w:p", $ns)
  $sb = New-Object System.Text.StringBuilder
  foreach ($p in $paras) {
    $texts = $p.SelectNodes(".//w:t", $ns)
    $line = ($texts | ForEach-Object { $_.'#text' }) -join ""
    [void]$sb.AppendLine($line)
  }

  if ($f.Name -like "ON-DA*") {
    $outName = "ON-DA_common_spec_v1.1.txt"
  } elseif ($f.Name -match [regex]::Escape([char]0xAE30) -or $f.Name.Contains("기능")) {
    $outName = "functional_spec_v1.0.txt"
  } elseif ($f.Name.Contains("제안")) {
    $outName = "proposal_v1.0.txt"
  } else {
    # fallback by size / order
    $outName = "doc_" + $f.Length + ".txt"
  }

  # More reliable: map by known byte sizes from earlier listing
  if ($f.Length -eq 62560) { $outName = "ON-DA_common_spec_v1.1.txt" }
  elseif ($f.Length -eq 113477) { $outName = "functional_spec_v1.0.txt" }
  elseif ($f.Length -eq 90853) { $outName = "proposal_v1.0.txt" }

  $outPath = Join-Path $outDir $outName
  [IO.File]::WriteAllText($outPath, $sb.ToString(), [Text.UTF8Encoding]::new($false))
  Write-Host "OK $($f.Length) -> $outName chars=$($sb.Length)"

  Remove-Item $destZip -Force -ErrorAction SilentlyContinue
  Remove-Item $extract -Recurse -Force -ErrorAction SilentlyContinue
}

Copy-Item "C:\Users\82108\Desktop\Bus\docs\mock-data-and-scenarios.md" (Join-Path $outDir "mock-data-and-scenarios.md") -Force
Get-ChildItem $outDir -File | ForEach-Object { Write-Host ("LIST " + $_.Name + " " + $_.Length) }
