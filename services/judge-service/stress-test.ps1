# OJ 系统压力测试脚本
# 使用 PowerShell 执行

param(
    [int]$ConcurrentUsers = 100,
    [int]$SubmissionsPerUser = 5,
    [string]$BaseUrl = "http://localhost:8083"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "       OJ 系统压力测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "并发用户数: $ConcurrentUsers"
Write-Host "每用户提交次数: $SubmissionsPerUser"
Write-Host "目标服务: $BaseUrl"
Write-Host "========================================`n"

# 统计变量
$script:successCount = 0
$script:failCount = 0
$script:totalResponseTime = 0
$script:submissionIds = [System.Collections.Concurrent.ConcurrentBag[long]]::new()

# 测试代码
$testCases = @(
    @{
        language = "JAVA"
        code = @"
public class Solution {
    public static void main(String[] args) {
        java.util.Scanner sc = new java.util.Scanner(System.in);
        int[] nums = java.util.Arrays.stream(sc.nextLine().split(" ")).mapToInt(Integer::parseInt).toArray();
        int target = sc.nextInt();
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[i] + nums[j] == target) {
                    System.out.print(i + " " + j);
                    return;
                }
            }
        }
    }
}
"@
        expected = "ACCEPTED"
    },
    @{
        language = "JAVA"
        code = @"
public class Solution {
    public static void main(String[] args) {
        System.out.print("0 1");
    }
}
"@
        expected = "WRONG_ANSWER"
    },
    @{
        language = "PYTHON"
        code = @"
nums = list(map(int, input().split()))
target = int(input())
for i in range(len(nums)):
    for j in range(i+1, len(nums)):
        if nums[i] + nums[j] == target:
            print(i, j, end='')
            exit()
"@
        expected = "ACCEPTED"
    }
)

# 健康检查
function Test-ServiceHealth {
    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -Method Get -TimeoutSec 5
        Write-Host "服务健康检查: $($response.status)" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "服务健康检查失败: $_" -ForegroundColor Red
        return $false
    }
}

# 提交代码
function Submit-Code {
    param(
        [int]$UserId,
        [hashtable]$TestCase
    )
    
    $body = @{
        problemId = 1
        userId = $UserId
        language = $TestCase.language
        code = $TestCase.code
    } | ConvertTo-Json -Depth 10
    
    $startTime = Get-Date
    
    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl/judge/submit" `
            -Method Post `
            -Body $body `
            -ContentType "application/json" `
            -TimeoutSec 60
        
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        [System.Threading.Interlocked]::Increment([ref]$script:successCount) | Out-Null
        [System.Threading.Interlocked]::Add([ref]$script:totalResponseTime, [long]$responseTime) | Out-Null
        
        if ($response.data.submissionId) {
            $script:submissionIds.Add($response.data.submissionId)
            Write-Host "[用户$($UserId.ToString('D3'))] 提交成功 ID=$($response.data.submissionId) 耗时=$([int]$responseTime)ms" -ForegroundColor Green
        }
        
        return $response
    } catch {
        [System.Threading.Interlocked]::Increment([ref]$script:failCount) | Out-Null
        Write-Host "[用户$($UserId.ToString('D3'))] 提交失败: $_" -ForegroundColor Red
        return $null
    }
}

# 主测试流程
if (-not (Test-ServiceHealth)) {
    Write-Host "`n请确保 judge-service 已启动！" -ForegroundColor Red
    exit 1
}

Write-Host "`n开始压力测试...`n" -ForegroundColor Yellow

$startTime = Get-Date

# 使用 RunspacePool 实现并发
$runspacePool = [runspacefactory]::CreateRunspacePool(1, $ConcurrentUsers)
$runspacePool.Open()

$jobs = @()

for ($user = 1; $user -le $ConcurrentUsers; $user++) {
    $powershell = [powershell]::Create().AddScript({
        param($UserId, $BaseUrl, $TestCases, $SubmissionsPerUser)
        
        for ($i = 0; $i -lt $SubmissionsPerUser; $i++) {
            $testCase = $TestCases | Get-Random
            
            $body = @{
                problemId = 1
                userId = $UserId
                language = $testCase.language
                code = $testCase.code
            } | ConvertTo-Json -Depth 10
            
            try {
                $response = Invoke-RestMethod -Uri "$BaseUrl/judge/submit" `
                    -Method Post `
                    -Body $body `
                    -ContentType "application/json" `
                    -TimeoutSec 60
                
                @{
                    Success = $true
                    SubmissionId = $response.data.submissionId
                    UserId = $UserId
                }
            } catch {
                @{
                    Success = $false
                    Error = $_.Exception.Message
                    UserId = $UserId
                }
            }
            
            Start-Sleep -Milliseconds (Get-Random -Minimum 100 -Maximum 500)
        }
    }).AddArgument($user).AddArgument($BaseUrl).AddArgument($testCases).AddArgument($SubmissionsPerUser)
    
    $powershell.RunspacePool = $runspacePool
    
    $jobs += @{
        PowerShell = $powershell
        Handle = $powershell.BeginInvoke()
    }
}

# 等待所有任务完成
$results = @()
foreach ($job in $jobs) {
    $result = $job.PowerShell.EndInvoke($job.Handle)
    $results += $result
    $job.PowerShell.Dispose()
}

$runspacePool.Close()
$runspacePool.Dispose()

$endTime = Get-Date
$totalTime = ($endTime - $startTime).TotalMilliseconds

# 统计结果
$successResults = $results | Where-Object { $_.Success -eq $true }
$failResults = $results | Where-Object { $_.Success -eq $false }

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "       压力测试报告" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "【提交统计】"
Write-Host "  总提交数: $($results.Count)"
Write-Host "  成功提交: $($successResults.Count)"
Write-Host "  失败提交: $($failResults.Count)"
$successRate = if ($results.Count -gt 0) { [math]::Round($successResults.Count * 100.0 / $results.Count, 2) } else { 0 }
Write-Host "  提交成功率: $successRate%"
Write-Host ""
Write-Host "【性能统计】"
Write-Host "  总耗时: $([int]$totalTime) ms"
$throughput = if ($totalTime -gt 0) { [math]::Round($results.Count * 1000.0 / $totalTime, 2) } else { 0 }
Write-Host "  吞吐量: $throughput 请求/秒"
Write-Host ""
Write-Host "【测试结论】"

if ($successRate -ge 99) {
    Write-Host "  ✅ 提交成功率达标 (>= 99%)" -ForegroundColor Green
} else {
    Write-Host "  ❌ 提交成功率低于99%" -ForegroundColor Red
}

if ($failResults.Count -eq 0) {
    Write-Host "  ✅ 无失败提交" -ForegroundColor Green
} else {
    Write-Host "  ⚠️ 存在 $($failResults.Count) 个失败提交" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($successRate -ge 99 -and $failResults.Count -eq 0) {
    Write-Host "       测试通过 ✅" -ForegroundColor Green
} else {
    Write-Host "       测试需要关注 ⚠️" -ForegroundColor Yellow
}
Write-Host "========================================" -ForegroundColor Cyan
