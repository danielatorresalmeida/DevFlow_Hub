param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

$createdCollaboratorId = $null
$exitCode = 0

function Write-Pass {
    param([string]$Message)

    Write-Host "[PASS] $Message" -ForegroundColor Green
}

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }

    Write-Pass $Message
}

function Assert-HttpError {
    param(
        [string]$Method,
        [string]$Uri,
        [int]$ExpectedStatus,
        [string]$Body
    )

    try {
        $request = @{
            Method = $Method
            Uri    = $Uri
        }

        if (-not [string]::IsNullOrWhiteSpace($Body)) {
            $request.ContentType = "application/json; charset=utf-8"
            $request.Body = $Body
        }

        Invoke-RestMethod @request | Out-Null

        throw "Expected HTTP $ExpectedStatus from $Method $Uri."
    }
    catch {
        $response = $_.Exception.Response

        if ($null -eq $response) {
            throw
        }

        $actualStatus = [int]$response.StatusCode

        if ($actualStatus -ne $ExpectedStatus) {
            throw "Expected HTTP $ExpectedStatus but received HTTP $actualStatus from $Method $Uri."
        }

        Write-Pass "$Method $Uri returned HTTP $ExpectedStatus"
    }
}

try {
    Write-Host ""
    Write-Host "DevFlow Hub API smoke tests" -ForegroundColor Cyan
    Write-Host "Base URL: $BaseUrl"
    Write-Host ""

    Invoke-RestMethod `
        -Method Get `
        -Uri "$BaseUrl/api/collaborators" |
    Out-Null

    Write-Pass "GET /api/collaborators"

    Invoke-RestMethod `
        -Method Get `
        -Uri "$BaseUrl/api/projects" |
    Out-Null

    Write-Pass "GET /api/projects"

    Invoke-RestMethod `
        -Method Get `
        -Uri "$BaseUrl/api/tasks" |
    Out-Null

    Write-Pass "GET /api/tasks"

    Invoke-RestMethod `
        -Method Get `
        -Uri "$BaseUrl/api/internal-programs" |
    Out-Null

    Write-Pass "GET /api/internal-programs"

    $dashboard = Invoke-RestMethod `
        -Method Get `
        -Uri "$BaseUrl/api/dashboard"

    Write-Pass "GET /api/dashboard"

    Assert-True `
        -Condition ($dashboard.PSObject.Properties.Name -contains "taskCount") `
        -Message "Dashboard contains taskCount"

    Assert-HttpError `
        -Method "Get" `
        -Uri "$BaseUrl/api/tasks/999999" `
        -ExpectedStatus 404

    $timestamp = Get-Date -Format "yyyyMMddHHmmss"

    $invalidCollaborator = @{
        name   = "Invalid API Test $timestamp"
        email  = "invalid.api.$timestamp@devflowhub.pt"
        role   = "QA Tester"
        active = $true
    } | ConvertTo-Json

    Assert-HttpError `
        -Method "Post" `
        -Uri "$BaseUrl/api/collaborators" `
        -ExpectedStatus 400 `
        -Body $invalidCollaborator

    $newCollaborator = @{
        name     = "API Smoke Test $timestamp"
        email    = "api.smoke.$timestamp@devflowhub.pt"
        password = "TemporarySmokeTest#2026"
        role     = "QA Tester"
        active   = $true
    } | ConvertTo-Json

    $createdCollaborator = Invoke-RestMethod `
        -Method Post `
        -Uri "$BaseUrl/api/collaborators" `
        -ContentType "application/json; charset=utf-8" `
        -Body $newCollaborator

    $createdCollaboratorId = $createdCollaborator.id

    Assert-True `
        -Condition ($null -ne $createdCollaboratorId) `
        -Message "POST /api/collaborators created a collaborator"

    Assert-True `
        -Condition (-not (
            $createdCollaborator.PSObject.Properties.Name -contains "password"
        )) `
        -Message "Password is not exposed in the response"

    $retrievedCollaborator = Invoke-RestMethod `
        -Method Get `
        -Uri "$BaseUrl/api/collaborators/$createdCollaboratorId"

    Assert-True `
        -Condition ($retrievedCollaborator.id -eq $createdCollaboratorId) `
        -Message "GET /api/collaborators/{id}"

    $updatedCollaboratorBody = @{
        name   = $createdCollaborator.name
        email  = $createdCollaborator.email
        role   = "Senior QA Tester"
        active = $false
    } | ConvertTo-Json

    $updatedCollaborator = Invoke-RestMethod `
        -Method Put `
        -Uri "$BaseUrl/api/collaborators/$createdCollaboratorId" `
        -ContentType "application/json; charset=utf-8" `
        -Body $updatedCollaboratorBody

    Assert-True `
        -Condition ($updatedCollaborator.role -eq "Senior QA Tester") `
        -Message "PUT updated the collaborator role"

    Assert-True `
        -Condition ($updatedCollaborator.active -eq $false) `
        -Message "PUT updated the collaborator status"

    Invoke-RestMethod `
        -Method Delete `
        -Uri "$BaseUrl/api/collaborators/$createdCollaboratorId" |
    Out-Null

    Write-Pass "DELETE /api/collaborators/{id}"

    $deletedCollaboratorId = $createdCollaboratorId
    $createdCollaboratorId = $null

    Assert-HttpError `
        -Method "Get" `
        -Uri "$BaseUrl/api/collaborators/$deletedCollaboratorId" `
        -ExpectedStatus 404

    Write-Host ""
    Write-Host "All API smoke tests passed." -ForegroundColor Green
}
catch {
    $exitCode = 1

    Write-Host ""
    Write-Host "[FAIL] $($_.Exception.Message)" -ForegroundColor Red
}
finally {
    if ($null -ne $createdCollaboratorId) {
        try {
            Invoke-RestMethod `
                -Method Delete `
                -Uri "$BaseUrl/api/collaborators/$createdCollaboratorId" |
            Out-Null

            Write-Host "[CLEANUP] Temporary collaborator removed."
        }
        catch {
            $response = $_.Exception.Response

            if (
                $null -eq $response -or
                [int]$response.StatusCode -ne 404
            ) {
                Write-Warning "Could not remove temporary collaborator $createdCollaboratorId."
            }
        }
    }
}

exit $exitCode