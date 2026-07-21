param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$TimerWaitSeconds = 2
)

$ErrorActionPreference = "Stop"

$createdCollaboratorId = $null
$createdProjectId = $null
$createdProgramId = $null
$createdTaskId = $null
$exitCode = 0

function Write-Pass {
    param([string]$Message)
    Write-Host "[PASS] $Message" -ForegroundColor Green
}

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
    Write-Pass $Message
}

function ConvertTo-Utf8JsonBytes {
    param([Parameter(Mandatory)][hashtable]$Payload)
    $json = $Payload | ConvertTo-Json -Compress -Depth 10
    Write-Output -NoEnumerate ([System.Text.Encoding]::UTF8.GetBytes($json))
}

function Assert-HttpError {
    param(
        [Parameter(Mandatory)][string]$Method,
        [Parameter(Mandatory)][string]$Uri,
        [Parameter(Mandatory)][int]$ExpectedStatus,
        [string]$Body,
        [string]$ExpectedMessage,
        [string]$ExpectedValidationField,
        [string]$ExpectedValidationMessage
    )

    $responseFile = Join-Path $env:TEMP ("devflow-response-" + [guid]::NewGuid() + ".json")
    $bodyFile = $null

    try {
        $arguments = @("-sS", "-o", $responseFile, "-w", "%{http_code}", "-X", $Method)

        if (-not [string]::IsNullOrWhiteSpace($Body)) {
            $bodyFile = Join-Path $env:TEMP ("devflow-request-" + [guid]::NewGuid() + ".json")
            [System.IO.File]::WriteAllText($bodyFile, $Body, [System.Text.UTF8Encoding]::new($false))
            $arguments += @("-H", "Content-Type: application/json; charset=utf-8", "--data-binary", "@$bodyFile")
        }

        $arguments += $Uri
        $statusText = & curl.exe @arguments

        if ($LASTEXITCODE -ne 0) {
            throw "curl.exe failed for $Method $Uri with exit code $LASTEXITCODE."
        }

        $responseBody = [System.IO.File]::ReadAllText($responseFile)
        $actualStatus = [int]$statusText

        if ($actualStatus -ne $ExpectedStatus) {
            throw "Expected HTTP $ExpectedStatus but received HTTP $actualStatus from $Method $Uri. Response: $responseBody"
        }

        $responseJson = $null
        if (-not [string]::IsNullOrWhiteSpace($responseBody)) {
            try { $responseJson = $responseBody | ConvertFrom-Json }
            catch { throw "The response from $Method $Uri is not valid JSON. Response: $responseBody" }
        }

        if (-not [string]::IsNullOrWhiteSpace($ExpectedMessage)) {
            if ($null -eq $responseJson -or $responseJson.message -ne $ExpectedMessage) {
                throw "Expected message '$ExpectedMessage' from $Method $Uri. Response: $responseBody"
            }
        }

        if (-not [string]::IsNullOrWhiteSpace($ExpectedValidationField)) {
            if ($null -eq $responseJson -or $null -eq $responseJson.validationErrors) {
                throw "Expected validationErrors from $Method $Uri. Response: $responseBody"
            }

            $actualValidationMessage = $responseJson.validationErrors.$ExpectedValidationField
            if ($actualValidationMessage -ne $ExpectedValidationMessage) {
                throw "Expected validation error '${ExpectedValidationField}: $ExpectedValidationMessage'. Response: $responseBody"
            }
        }

        Write-Pass "$Method $Uri returned HTTP $ExpectedStatus"
    }
    finally {
        Remove-Item $responseFile -Force -ErrorAction SilentlyContinue
        if ($null -ne $bodyFile) {
            Remove-Item $bodyFile -Force -ErrorAction SilentlyContinue
        }
    }
}

function Remove-TemporaryResource {
    param([string]$Label, [string]$Uri)
    try {
        Invoke-RestMethod -Method Delete -Uri $Uri -ErrorAction Stop | Out-Null
        Write-Host "[CLEANUP] $Label removed."
    }
    catch {
        $response = $_.Exception.Response
        if ($null -eq $response -or [int]$response.StatusCode -ne 404) {
            Write-Warning "Could not remove $Label."
        }
    }
}

try {
    Write-Host ""
    Write-Host "DevFlow Hub PostgreSQL API smoke tests" -ForegroundColor Cyan
    Write-Host "Base URL: $BaseUrl"
    Write-Host "Timer wait: $TimerWaitSeconds second(s)"
    Write-Host ""

    $collaboratorsBefore = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/collaborators"
    $projectsBefore = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/projects"
    $tasksBefore = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/tasks"
    $programsBefore = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/internal-programs"

    Write-Pass "GET /api/collaborators"
    Write-Pass "GET /api/projects"
    Write-Pass "GET /api/tasks"
    Write-Pass "GET /api/internal-programs"

    $initialCollaboratorCount = $collaboratorsBefore.Count
    $initialProjectCount = $projectsBefore.Count
    $initialTaskCount = $tasksBefore.Count
    $initialProgramCount = $programsBefore.Count

    $dashboard = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/dashboard"
    Write-Pass "GET /api/dashboard"

    Assert-True ($dashboard.PSObject.Properties.Name -contains "taskCount") "Dashboard contains taskCount"

    Assert-HttpError -Method "GET" -Uri "$BaseUrl/api/tasks/999999999" -ExpectedStatus 404 -ExpectedMessage "Task not found."

    $timestamp = Get-Date -Format "yyyyMMddHHmmss"

    # Collaborator validation and CRUD
    $invalidCollaborator = @{
        name   = "Invalid API Test $timestamp"
        email  = "invalid.api.$timestamp@devflowhub.pt"
        role   = "QA Tester"
        active = $true
    } | ConvertTo-Json -Compress

    Assert-HttpError -Method "POST" -Uri "$BaseUrl/api/collaborators" -ExpectedStatus 400 -Body $invalidCollaborator -ExpectedMessage "Password is required when creating a collaborator."

    $createdCollaborator = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/collaborators" -ContentType "application/json; charset=utf-8" -Body (ConvertTo-Utf8JsonBytes @{
        name     = "API Smoke Test $timestamp"
        email    = "api.smoke.$timestamp@devflowhub.pt"
        password = "TemporarySmokeTest#2026"
        role     = "QA Tester"
        active   = $true
    })

    $createdCollaboratorId = [long]$createdCollaborator.id
    Assert-True ($createdCollaboratorId -gt 0) "POST /api/collaborators created a collaborator"
    Assert-True (-not ($createdCollaborator.PSObject.Properties.Name -contains "password")) "Password is not exposed in the collaborator response"

    $retrievedCollaborator = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/collaborators/$createdCollaboratorId"
    Assert-True ($retrievedCollaborator.id -eq $createdCollaboratorId) "GET /api/collaborators/{id}"

    $updatedCollaborator = Invoke-RestMethod -Method Put -Uri "$BaseUrl/api/collaborators/$createdCollaboratorId" -ContentType "application/json; charset=utf-8" -Body (ConvertTo-Utf8JsonBytes @{
        name   = $createdCollaborator.name
        email  = $createdCollaborator.email
        role   = "Senior QA Tester"
        active = $false
    })

    Assert-True ($updatedCollaborator.role -eq "Senior QA Tester") "PUT updated the collaborator role"
    Assert-True ($updatedCollaborator.active -eq $false) "PUT updated the collaborator status"

    # Project validation and CRUD
    $createdProject = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/projects" -ContentType "application/json; charset=utf-8" -Body (ConvertTo-Utf8JsonBytes @{
        name        = "API Project Test $timestamp"
        description = "Temporary project created by the PostgreSQL smoke test."
        status      = "PLANNED"
        startDate   = "2026-07-21"
        endDate     = "2026-08-21"
        managerId   = $createdCollaboratorId
    })

    $createdProjectId = [long]$createdProject.id
    Assert-True ($createdProjectId -gt 0) "POST /api/projects created a project"
    Assert-True ($createdProject.managerId -eq $createdCollaboratorId) "Project manager relationship was stored"

    $retrievedProject = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/projects/$createdProjectId"
    Assert-True ($retrievedProject.id -eq $createdProjectId) "GET /api/projects/{id}"

    $updatedProject = Invoke-RestMethod -Method Put -Uri "$BaseUrl/api/projects/$createdProjectId" -ContentType "application/json; charset=utf-8" -Body (ConvertTo-Utf8JsonBytes @{
        name        = $createdProject.name
        description = "Project updated by the PostgreSQL smoke test."
        status      = "IN_PROGRESS"
        startDate   = "2026-07-21"
        endDate     = "2026-09-15"
        managerId   = $createdCollaboratorId
    })

    Assert-True ($updatedProject.status -eq "IN_PROGRESS") "PUT updated the project status"

    Assert-HttpError -Method "POST" -Uri "$BaseUrl/api/projects" -ExpectedStatus 400 -Body (@{
        name = "Invalid Project Status $timestamp"; status = "ARCHIVED"; managerId = $createdCollaboratorId
    } | ConvertTo-Json -Compress) -ExpectedMessage "Project status has an unsupported value."

    Assert-HttpError -Method "POST" -Uri "$BaseUrl/api/projects" -ExpectedStatus 400 -Body (@{
        name = "Invalid Project Dates $timestamp"; status = "PLANNED"; startDate = "2026-09-01"; endDate = "2026-08-01"; managerId = $createdCollaboratorId
    } | ConvertTo-Json -Compress) -ExpectedMessage "Project end date cannot be before its start date."

    Assert-HttpError -Method "POST" -Uri "$BaseUrl/api/projects" -ExpectedStatus 400 -Body (@{
        name = "Invalid Project Manager $timestamp"; status = "PLANNED"; managerId = 999999999
    } | ConvertTo-Json -Compress) -ExpectedMessage "The selected project manager does not exist."

    # Internal program validation and CRUD
    $createdProgram = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/internal-programs" -ContentType "application/json; charset=utf-8" -Body (ConvertTo-Utf8JsonBytes @{
        name        = "API Internal Program Test $timestamp"
        description = "Temporary internal program created by the PostgreSQL smoke test."
        area        = "Engineering"
        status      = "PLANNED"
        startDate   = "2026-07-21"
        endDate     = "2026-08-21"
        managerId   = $createdCollaboratorId
    })

    $createdProgramId = [long]$createdProgram.id
    Assert-True ($createdProgramId -gt 0) "POST /api/internal-programs created a program"
    Assert-True ($createdProgram.managerId -eq $createdCollaboratorId) "Program manager relationship was stored"

    $retrievedProgram = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/internal-programs/$createdProgramId"
    Assert-True ($retrievedProgram.id -eq $createdProgramId) "GET /api/internal-programs/{id}"

    $updatedProgram = Invoke-RestMethod -Method Put -Uri "$BaseUrl/api/internal-programs/$createdProgramId" -ContentType "application/json; charset=utf-8" -Body (ConvertTo-Utf8JsonBytes @{
        name        = $createdProgram.name
        description = "Program updated by the PostgreSQL smoke test."
        area        = "Software Engineering"
        status      = "ACTIVE"
        startDate   = "2026-07-21"
        endDate     = "2026-09-30"
        managerId   = $createdCollaboratorId
    })

    Assert-True ($updatedProgram.status -eq "ACTIVE") "PUT updated the program status"
    Assert-True ($updatedProgram.area -eq "Software Engineering") "PUT updated the program area"

    Assert-HttpError -Method "POST" -Uri "$BaseUrl/api/internal-programs" -ExpectedStatus 400 -Body (@{
        name = "Invalid Program Status $timestamp"; status = "IN_PROGRESS"; managerId = $createdCollaboratorId
    } | ConvertTo-Json -Compress) -ExpectedMessage "Program status has an unsupported value."

    Assert-HttpError -Method "POST" -Uri "$BaseUrl/api/internal-programs" -ExpectedStatus 400 -Body (@{
        name = "Invalid Program Dates $timestamp"; status = "PLANNED"; startDate = "2026-09-01"; endDate = "2026-08-01"; managerId = $createdCollaboratorId
    } | ConvertTo-Json -Compress) -ExpectedMessage "Program end date cannot be before its start date."

    Assert-HttpError -Method "POST" -Uri "$BaseUrl/api/internal-programs" -ExpectedStatus 400 -Body (@{
        name = "Invalid Program Manager $timestamp"; status = "PLANNED"; managerId = 999999999
    } | ConvertTo-Json -Compress) -ExpectedMessage "The selected program manager does not exist."

    Assert-HttpError -Method "POST" -Uri "$BaseUrl/api/internal-programs" -ExpectedStatus 400 -Body (@{
        name = "   "; status = "PLANNED"; managerId = $createdCollaboratorId
    } | ConvertTo-Json -Compress) -ExpectedMessage "The submitted data is invalid." -ExpectedValidationField "name" -ExpectedValidationMessage "Name is required."

    # Task validation, CRUD and timer lifecycle
    $createdTask = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/tasks" -ContentType "application/json; charset=utf-8" -Body (ConvertTo-Utf8JsonBytes @{
        title            = "API Task Test $timestamp"
        description      = "Temporary task created by the PostgreSQL smoke test."
        status           = "PENDING"
        priority         = "HIGH"
        projectId        = $createdProjectId
        assigneeId       = $createdCollaboratorId
        totalTimeSeconds = 999
        timerActive      = $true
        timerStartedAt   = "2026-07-21T10:00:00"
    })

    $createdTaskId = [long]$createdTask.id
    Assert-True ($createdTaskId -gt 0) "POST /api/tasks created a task"
    Assert-True ($createdTask.projectId -eq $createdProjectId) "Task project relationship was stored"
    Assert-True ($createdTask.assigneeId -eq $createdCollaboratorId) "Task assignee relationship was stored"
    Assert-True ($createdTask.totalTimeSeconds -eq 0 -and $createdTask.timerActive -eq $false -and $null -eq $createdTask.timerStartedAt) "Task creation protected the timer state"

    $retrievedTask = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/tasks/$createdTaskId"
    Assert-True ($retrievedTask.id -eq $createdTaskId) "GET /api/tasks/{id}"

    $updatedTask = Invoke-RestMethod -Method Put -Uri "$BaseUrl/api/tasks/$createdTaskId" -ContentType "application/json; charset=utf-8" -Body (ConvertTo-Utf8JsonBytes @{
        title = "Updated API Task $timestamp"; description = "Task updated before starting the timer."; status = "REVIEW"; priority = "LOW"; projectId = $createdProjectId; assigneeId = $createdCollaboratorId
    })

    Assert-True ($updatedTask.status -eq "REVIEW" -and $updatedTask.priority -eq "LOW") "PUT updated a task while its timer was inactive"

    $startedTask = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/tasks/$createdTaskId/start-timer"
    Assert-True ($startedTask.timerActive -eq $true -and $startedTask.status -eq "IN_PROGRESS" -and $null -ne $startedTask.timerStartedAt) "Task timer started"

    $activeUpdatedTask = Invoke-RestMethod -Method Put -Uri "$BaseUrl/api/tasks/$createdTaskId" -ContentType "application/json; charset=utf-8" -Body (ConvertTo-Utf8JsonBytes @{
        title = "Active Timer Task $timestamp"; description = "Task updated while its timer was active."; status = "REVIEW"; priority = "MEDIUM"; projectId = $createdProjectId; assigneeId = $createdCollaboratorId
    })

    Assert-True ($activeUpdatedTask.status -eq "IN_PROGRESS" -and $activeUpdatedTask.priority -eq "MEDIUM") "An active timer protected the task status during PUT"

    Start-Sleep -Seconds $TimerWaitSeconds

    $runningTotal = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/tasks/$createdTaskId/total-time"
    Assert-True ([long]$runningTotal -ge 1) "GET /api/tasks/{id}/total-time included the active session"

    $timerState = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/tasks/$createdTaskId/timer"
    Assert-True ($timerState.timerActive -eq $true) "GET /api/tasks/{id}/timer returned the active timer"

    $pausedTask = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/tasks/$createdTaskId/pause-timer"
    $pausedTotal = [long]$pausedTask.totalTimeSeconds
    Assert-True ($pausedTask.timerActive -eq $false -and $null -eq $pausedTask.timerStartedAt -and $pausedTotal -ge 1) "Task timer paused and stored elapsed time"

    Assert-HttpError -Method "POST" -Uri "$BaseUrl/api/tasks/$createdTaskId/pause-timer" -ExpectedStatus 400 -ExpectedMessage "The timer is not active."

    $resumedTask = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/tasks/$createdTaskId/resume-timer"
    Assert-True ($resumedTask.timerActive -eq $true -and $resumedTask.status -eq "IN_PROGRESS") "Task timer resumed"

    Start-Sleep -Seconds $TimerWaitSeconds

    $completedTask = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/tasks/$createdTaskId/complete"
    Assert-True ($completedTask.status -eq "COMPLETED" -and $completedTask.timerActive -eq $false -and $null -eq $completedTask.timerStartedAt -and [long]$completedTask.totalTimeSeconds -gt $pausedTotal) "Completing a task stopped the timer and stored the second session"

    Assert-HttpError -Method "POST" -Uri "$BaseUrl/api/tasks/$createdTaskId/start-timer" -ExpectedStatus 400 -ExpectedMessage "A completed task cannot restart its timer."

    Assert-HttpError -Method "POST" -Uri "$BaseUrl/api/tasks" -ExpectedStatus 400 -Body (@{
        title = "Invalid Task Status $timestamp"; status = "ARCHIVED"; priority = "MEDIUM"; projectId = $createdProjectId; assigneeId = $createdCollaboratorId
    } | ConvertTo-Json -Compress) -ExpectedMessage "Task status has an unsupported value."

    Assert-HttpError -Method "POST" -Uri "$BaseUrl/api/tasks" -ExpectedStatus 400 -Body (@{
        title = "Invalid Task Priority $timestamp"; status = "PENDING"; priority = "URGENT"; projectId = $createdProjectId; assigneeId = $createdCollaboratorId
    } | ConvertTo-Json -Compress) -ExpectedMessage "Task priority has an unsupported value."

    Assert-HttpError -Method "POST" -Uri "$BaseUrl/api/tasks" -ExpectedStatus 400 -Body (@{
        title = "Invalid Task Project $timestamp"; status = "PENDING"; priority = "MEDIUM"; projectId = 999999999; assigneeId = $createdCollaboratorId
    } | ConvertTo-Json -Compress) -ExpectedMessage "The selected project does not exist."

    Assert-HttpError -Method "POST" -Uri "$BaseUrl/api/tasks" -ExpectedStatus 400 -Body (@{
        title = "Invalid Task Assignee $timestamp"; status = "PENDING"; priority = "MEDIUM"; projectId = $createdProjectId; assigneeId = 999999999
    } | ConvertTo-Json -Compress) -ExpectedMessage "The selected assignee does not exist."

    Assert-HttpError -Method "POST" -Uri "$BaseUrl/api/tasks" -ExpectedStatus 400 -Body (@{
        title = "   "; status = "PENDING"; priority = "MEDIUM"; projectId = $createdProjectId; assigneeId = $createdCollaboratorId
    } | ConvertTo-Json -Compress) -ExpectedMessage "The submitted data is invalid." -ExpectedValidationField "title" -ExpectedValidationMessage "Title is required."

    # Delete temporary resources in dependency order.
    Invoke-RestMethod -Method Delete -Uri "$BaseUrl/api/tasks/$createdTaskId" | Out-Null
    Write-Pass "DELETE /api/tasks/{id}"
    $deletedTaskId = $createdTaskId
    $createdTaskId = $null
    Assert-HttpError -Method "GET" -Uri "$BaseUrl/api/tasks/$deletedTaskId" -ExpectedStatus 404 -ExpectedMessage "Task not found."

    Invoke-RestMethod -Method Delete -Uri "$BaseUrl/api/internal-programs/$createdProgramId" | Out-Null
    Write-Pass "DELETE /api/internal-programs/{id}"
    $deletedProgramId = $createdProgramId
    $createdProgramId = $null
    Assert-HttpError -Method "GET" -Uri "$BaseUrl/api/internal-programs/$deletedProgramId" -ExpectedStatus 404 -ExpectedMessage "Internal program not found."

    Invoke-RestMethod -Method Delete -Uri "$BaseUrl/api/projects/$createdProjectId" | Out-Null
    Write-Pass "DELETE /api/projects/{id}"
    $deletedProjectId = $createdProjectId
    $createdProjectId = $null
    Assert-HttpError -Method "GET" -Uri "$BaseUrl/api/projects/$deletedProjectId" -ExpectedStatus 404 -ExpectedMessage "Project not found."

    Invoke-RestMethod -Method Delete -Uri "$BaseUrl/api/collaborators/$createdCollaboratorId" | Out-Null
    Write-Pass "DELETE /api/collaborators/{id}"
    $deletedCollaboratorId = $createdCollaboratorId
    $createdCollaboratorId = $null
    Assert-HttpError -Method "GET" -Uri "$BaseUrl/api/collaborators/$deletedCollaboratorId" -ExpectedStatus 404 -ExpectedMessage "Collaborator not found."

    $collaboratorsAfter = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/collaborators"
    $projectsAfter = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/projects"
    $tasksAfter = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/tasks"
    $programsAfter = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/internal-programs"

    Assert-True ($collaboratorsAfter.Count -eq $initialCollaboratorCount) "Collaborator count returned to its initial value"
    Assert-True ($projectsAfter.Count -eq $initialProjectCount) "Project count returned to its initial value"
    Assert-True ($tasksAfter.Count -eq $initialTaskCount) "Task count returned to its initial value"
    Assert-True ($programsAfter.Count -eq $initialProgramCount) "Internal program count returned to its initial value"

    Write-Host ""
    Write-Host "All PostgreSQL API smoke tests passed." -ForegroundColor Green
}
catch {
    $exitCode = 1
    Write-Host ""
    Write-Host "[FAIL] $($_.Exception.Message)" -ForegroundColor Red
}
finally {
    if ($null -ne $createdTaskId) {
        Remove-TemporaryResource "temporary task $createdTaskId" "$BaseUrl/api/tasks/$createdTaskId"
    }
    if ($null -ne $createdProgramId) {
        Remove-TemporaryResource "temporary internal program $createdProgramId" "$BaseUrl/api/internal-programs/$createdProgramId"
    }
    if ($null -ne $createdProjectId) {
        Remove-TemporaryResource "temporary project $createdProjectId" "$BaseUrl/api/projects/$createdProjectId"
    }
    if ($null -ne $createdCollaboratorId) {
        Remove-TemporaryResource "temporary collaborator $createdCollaboratorId" "$BaseUrl/api/collaborators/$createdCollaboratorId"
    }
}

exit $exitCode