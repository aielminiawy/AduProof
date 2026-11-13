$ErrorActionPreference = 'Stop'
Set-Location (Resolve-Path (Join-Path $PSScriptRoot '..'))
New-Item -ItemType Directory -Force 'out/main','out/test' | Out-Null
$mainSources = @(Get-ChildItem 'src/main/java' -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
& javac -encoding UTF-8 -Xlint:all -Werror -d out/main @mainSources
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
Copy-Item 'src/main/resources/*' 'out/main' -Recurse -Force
& java -cp out/main dev.aduproof.Main
if ($LASTEXITCODE -ne 0) { throw 'Java application failed' }
