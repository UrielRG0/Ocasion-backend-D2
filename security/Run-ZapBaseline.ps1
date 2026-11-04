param(
    [Parameter(Mandatory=$true)][uri]$Target,
    [string]$OutputDirectory = (Join-Path $PWD 'zap-report')
)
$ErrorActionPreference = 'Stop'
if ($Target.Scheme -notin @('http','https') -or $Target.UserInfo) {
    throw 'Usa una URL HTTP(S) del entorno de pruebas autorizado, sin credenciales.'
}
docker info *> $null
if ($LASTEXITCODE -ne 0) { throw 'Inicia Docker Desktop con contenedores Linux antes de continuar.' }
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$reportDirectory = (Resolve-Path -LiteralPath $OutputDirectory).Path
# Spider de un minuto y reglas pasivas. No se habilita el escáner activo.
docker run --rm --mount "type=bind,source=$reportDirectory,target=/zap/wrk" ghcr.io/zaproxy/zaproxy:stable zap-baseline.py -t $Target.AbsoluteUri -m 1 -r informe.html -J informe.json
$result = $LASTEXITCODE
switch ($result) {
    0 { Write-Host 'ZAP finalizó sin alertas para su alcance.' }
    1 { Write-Warning 'ZAP detectó alertas configuradas como FAIL; revisar informe.' }
    2 { Write-Warning 'ZAP detectó advertencias; revisar informe, no significa ausencia de riesgos.' }
    default { Write-Warning 'ZAP no completó correctamente el análisis; no presentar como aprobado.' }
}
exit $result
