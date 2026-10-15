param(
    [Parameter(Mandatory = $true)][string]$PostgresBin,
    [Parameter(Mandatory = $true)][string]$MongoExecutable,
    [string]$MavenCommand = 'mvn',
    [int]$PostgresPort = 54394,
    [int]$MongoPort = 27094
)
$ErrorActionPreference = 'Stop'
$taskApiRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$taskRuntime = Join-Path $taskApiRoot 'target\local-database-tests'
$taskSession = Join-Path $taskRuntime ('session-' + [Guid]::NewGuid().ToString('N'))
$taskPgData = Join-Path $taskSession 'postgres'
$taskMongoData = Join-Path $taskSession 'mongodb'
$taskMongoProcess = $null
$taskPgStarted = $false
$taskExit = 1
$taskEnvironment = @('ORIENTO_TEST_JDBC_URL', 'ORIENTO_TEST_JDBC_USER',
    'ORIENTO_TEST_JDBC_PASSWORD', 'ORIENTO_TEST_MONGO_URI')
$taskPreviousEnvironment = @{}
foreach ($taskName in $taskEnvironment) {
    $taskPreviousEnvironment[$taskName] = [Environment]::GetEnvironmentVariable($taskName, 'Process')
}
foreach ($taskPort in @($PostgresPort, $MongoPort)) {
    $taskListener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $taskPort)
    try { $taskListener.Start() } finally { $taskListener.Stop() }
}
[System.IO.Directory]::CreateDirectory($taskSession) | Out-Null
[System.IO.Directory]::CreateDirectory($taskMongoData) | Out-Null
try {
    & (Join-Path $PostgresBin 'initdb.exe') -D $taskPgData -U postgres -A trust --encoding=UTF8 --locale=C > (Join-Path $taskSession 'initdb.log')
    if ($LASTEXITCODE -ne 0) { throw 'initdb failed' }
    $taskPgProcess = Start-Process -FilePath (Join-Path $PostgresBin 'pg_ctl.exe') -ArgumentList @(
        '-D', ('"' + $taskPgData + '"'), '-l', ('"' + (Join-Path $taskSession 'postgres.log') + '"'),
        '-o', ('"-h 127.0.0.1 -p ' + $PostgresPort + '"'), '-w', 'start'
    ) -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $taskSession 'pg-start.out') -RedirectStandardError (Join-Path $taskSession 'pg-start.err')
    if (!$taskPgProcess.WaitForExit(30000) -or $taskPgProcess.ExitCode -ne 0) {
        throw 'temporary PostgreSQL failed to start'
    }
    $taskPgStarted = $true
    & (Join-Path $PostgresBin 'createdb.exe') -h 127.0.0.1 -p $PostgresPort -U postgres oriento_integration_test
    if ($LASTEXITCODE -ne 0) { throw 'temporary database creation failed' }
    $taskMongoProcess = Start-Process -FilePath $MongoExecutable -ArgumentList @(
        '--dbpath', ('"' + $taskMongoData + '"'), '--bind_ip', '127.0.0.1',
        '--port', $MongoPort, '--logpath', ('"' + (Join-Path $taskSession 'mongo.log') + '"'), '--logappend'
    ) -WindowStyle Hidden -PassThru
    $env:ORIENTO_TEST_JDBC_URL = "jdbc:postgresql://127.0.0.1:$PostgresPort/oriento_integration_test"
    $env:ORIENTO_TEST_JDBC_USER = 'postgres'
    $env:ORIENTO_TEST_JDBC_PASSWORD = ''
    $env:ORIENTO_TEST_MONGO_URI = "mongodb://127.0.0.1:$MongoPort"
    Push-Location $taskApiRoot
    try {
        & $MavenCommand -q test
        $taskExit = $LASTEXITCODE
    } finally { Pop-Location }
} finally {
    if ($taskPgStarted -or (Test-Path -LiteralPath (Join-Path $taskPgData 'postmaster.pid'))) {
        $taskPgStop = Start-Process -FilePath (Join-Path $PostgresBin 'pg_ctl.exe') -ArgumentList @(
            '-D', ('"' + $taskPgData + '"'), '-m', 'fast', '-w', 'stop'
        ) -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $taskSession 'pg-stop.out') -RedirectStandardError (Join-Path $taskSession 'pg-stop.err')
        $taskPgStop.WaitForExit(30000) | Out-Null
    }
    if ($taskMongoProcess -and !$taskMongoProcess.HasExited) {
        Stop-Process -Id $taskMongoProcess.Id
        $taskMongoProcess.WaitForExit(10000) | Out-Null
    }
    foreach ($taskName in $taskEnvironment) {
        [Environment]::SetEnvironmentVariable($taskName, $taskPreviousEnvironment[$taskName], 'Process')
    }
    Write-Output "Temporary database processes stopped. Diagnostics: $taskSession"
}
exit $taskExit
