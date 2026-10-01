param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
# DEMO / LOCAL DEVELOPMENT ONLY. Never send these public credentials to a remote host.
if (([uri]$BaseUrl).Host -notin @('localhost', '127.0.0.1', '[::1]', '::1')) {
    throw 'This walkthrough is restricted to a loopback API URL.'
}
function Request-Demo {
    param([string]$Method, [string]$Path, [string]$Token, $Body, [int]$Expected = 200)
    $options = @{ Uri = "$BaseUrl$Path"; Method = $Method; UseBasicParsing = $true }
    if ($Token) { $options.Headers = @{ Authorization = "Bearer $Token" } }
    if ($null -ne $Body) { $options.ContentType = 'application/json'; $options.Body = ($Body | ConvertTo-Json -Depth 5 -Compress) }
    try {
        $response = Invoke-WebRequest @options
        $status = [int]$response.StatusCode
        $content = $response.Content
    } catch {
        if ($null -eq $_.Exception.Response) { throw }
        $status = [int]$_.Exception.Response.StatusCode
        $content = $_.ErrorDetails.Message
    }
    if ($status -ne $Expected) { throw "$Method $Path expected $Expected, received $status. $content" }
    Write-Host "[PASS] $Method $Path -> $status"
    if ($content) { return ($content | ConvertFrom-Json) }
}
$tokens = @{}
foreach ($role in @('owner','manager','contributor','viewer','outsider','admin')) {
    $login = Request-Demo POST '/api/auth/login' '' @{email="$role@demo.example";password='DevFlowDemo-2026!'}
    $tokens[$role] = $login.accessToken
}
$projects = @(Request-Demo GET '/api/projects' $tokens.owner $null)
$project = $projects | Where-Object name -eq 'Interview Workspace' | Select-Object -First 1
if (-not $project) { throw 'Load database/demo_seed.sql into devflow_demo first.' }
$createdIds = [System.Collections.Generic.List[long]]::new()
try {
    $payload = @{title='Interview API document';content='Local document authorization demonstration';projectId=$project.id}
    $doc = Request-Demo POST '/api/documents' $tokens.owner $payload 201
    $createdIds.Add($doc.id)
    $null = Request-Demo GET "/api/documents/$($doc.id)" $tokens.viewer $null
    $null = Request-Demo POST '/api/documents' $tokens.viewer $payload 403
    $null = Request-Demo PUT "/api/documents/$($doc.id)" $tokens.contributor $payload 403
    $contribution = Request-Demo POST '/api/documents' $tokens.contributor $payload 201
    $createdIds.Add($contribution.id)
    $null = Request-Demo DELETE "/api/documents/$($contribution.id)" $tokens.contributor $null 403
    foreach ($role in @('outsider','admin')) {
        $null = Request-Demo GET "/api/documents/$($doc.id)" $tokens[$role] $null 404
        $null = Request-Demo GET "/api/projects/$($project.id)/documents" $tokens[$role] $null 404
        $null = Request-Demo POST '/api/documents' $tokens[$role] $payload 404
        $null = Request-Demo PUT "/api/documents/$($doc.id)" $tokens[$role] $payload 404
        $null = Request-Demo DELETE "/api/documents/$($doc.id)" $tokens[$role] $null 404
    }
    $privateProject = @(Request-Demo GET '/api/projects' $tokens.outsider $null) | Where-Object name -eq 'Private Workspace' | Select-Object -First 1
    $payload.projectId = $privateProject.id
    $null = Request-Demo PUT "/api/documents/$($doc.id)" $tokens.owner $payload 404
    $unchanged = Request-Demo GET "/api/documents/$($doc.id)" $tokens.owner $null
    if ($unchanged.projectId -ne $project.id) { throw 'Rejected reassignment changed the document.' }
    $payload.projectId = $project.id
    $payload.title = 'Reviewed by manager'
    $null = Request-Demo PUT "/api/documents/$($doc.id)" $tokens.manager $payload
    $null = Request-Demo GET '/api/documents/9223372036854775807' $tokens.owner $null 404
    Write-Host 'All local document authorization checks passed.'
} finally {
    foreach ($id in $createdIds) { $null = Request-Demo DELETE "/api/documents/$id" $tokens.owner $null 204 }
}
