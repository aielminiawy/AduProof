$ErrorActionPreference = 'Stop'
Set-Location (Resolve-Path (Join-Path $PSScriptRoot '..'))
New-Item -ItemType Directory -Force 'out/main','out/test' | Out-Null
$mainSources = @(Get-ChildItem 'src/main/java' -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
$testSources = @(Get-ChildItem 'src/test/java' -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
& javac -encoding UTF-8 -Xlint:all -Werror -d out/main @mainSources
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
Copy-Item 'src/main/resources/*' 'out/main' -Recurse -Force
& javac -encoding UTF-8 -Xlint:all -cp out/main -d out/test @testSources
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& java -ea -cp "out/main;out/test" dev.aduproof.AllTests
if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
