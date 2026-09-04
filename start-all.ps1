$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaHome = "C:\Users\THE FLASH\.jdks\openjdk-21.0.2"

if (-not (Test-Path $javaHome)) {
    throw "Java 17+ was not found at $javaHome. Update the JAVA_HOME path in start-all.ps1."
}

$env:JAVA_HOME = $javaHome
Set-Location $projectRoot

Write-Host "Starting Docker infrastructure..."
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    throw "Docker Compose failed to start."
}

$services = @(
    "identity-service",
    "organization-service",
    "project-service",
    "work-service",
    "bug-service",
    "recruitment-service",
    "notification-service",
    "payment-service",
    "api-gateway"
)

foreach ($service in $services) {
    Write-Host "Starting $service..."
    Start-Process powershell.exe -ArgumentList @(
        "-NoExit",
        "-Command",
        "Set-Location '$projectRoot'; `$env:JAVA_HOME='$javaHome'; mvn spring-boot:run -pl $service"
    )
    Start-Sleep -Seconds 2
}

Write-Host ""
Write-Host "All applications are starting."
Write-Host "Swagger: http://localhost:8080/gateway/swagger-ui.html"
