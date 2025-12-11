# Stress Test Script for OJ System
# Usage: .\stress-test-simple.ps1 -Users 100 -Submissions 5

param(
    [int]$Users = 100,
    [int]$Submissions = 5,
    [string]$Url = "http://localhost:8083"
)

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   OJ Stress Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Users: $Users"
Write-Host "Submissions per user: $Submissions"
$total = $Users * $Submissions
Write-Host "Total submissions: $total"
Write-Host "========================================"
Write-Host ""

# Health check
Write-Host "Checking service health..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$Url/actuator/health" -TimeoutSec 5
    Write-Host "Service status: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "Service unavailable! Please start judge-service first." -ForegroundColor Red
    exit 1
}

# Test code samples - Two Sum problem (problemId = 1)
# All 4 languages: Java, Python, C++, C

$javaCode = 'public class Solution {
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
}'

$pythonCode = 'nums = list(map(int, input().split()))
target = int(input())
for i in range(len(nums)):
    for j in range(i+1, len(nums)):
        if nums[i] + nums[j] == target:
            print(i, j, end="")
            exit()'

$cppCode = '#include <iostream>
#include <sstream>
#include <vector>
using namespace std;
int main() {
    string line;
    getline(cin, line);
    istringstream iss(line);
    vector<int> nums;
    int n;
    while (iss >> n) nums.push_back(n);
    int target;
    cin >> target;
    for (int i = 0; i < nums.size(); i++) {
        for (int j = i + 1; j < nums.size(); j++) {
            if (nums[i] + nums[j] == target) {
                cout << i << " " << j;
                return 0;
            }
        }
    }
    return 0;
}'

$cCode = '#include <stdio.h>
#include <stdlib.h>
#include <string.h>
int main() {
    char line[1000];
    fgets(line, sizeof(line), stdin);
    int nums[100], count = 0;
    char *token = strtok(line, " ");
    while (token != NULL) {
        nums[count++] = atoi(token);
        token = strtok(NULL, " ");
    }
    int target;
    scanf("%d", &target);
    for (int i = 0; i < count; i++) {
        for (int j = i + 1; j < count; j++) {
            if (nums[i] + nums[j] == target) {
                printf("%d %d", i, j);
                return 0;
            }
        }
    }
    return 0;
}'

# All 4 languages
$testCases = @(
    @{ language = "JAVA"; code = $javaCode },
    @{ language = "PYTHON"; code = $pythonCode },
    @{ language = "CPP"; code = $cppCode },
    @{ language = "C"; code = $cCode }
)

# Statistics
$script:success = 0
$script:fail = 0
$script:submissionIds = [System.Collections.ArrayList]::new()
$startTime = Get-Date

Write-Host ""
Write-Host "Starting submissions..." -ForegroundColor Yellow

# Sequential submission
$totalSubmissions = $Users * $Submissions
$completed = 0

for ($u = 1; $u -le $Users; $u++) {
    for ($s = 1; $s -le $Submissions; $s++) {
        $testCase = $testCases | Get-Random
        
        $body = @{
            problemId = 1
            userId = $u
            language = $testCase.language
            code = $testCase.code
        } | ConvertTo-Json -Depth 10
        
        try {
            $response = Invoke-RestMethod -Uri "$Url/judge/submit" -Method Post -Body $body -ContentType "application/json" -TimeoutSec 30
            
            $script:success++
            if ($response.data.submissionId) {
                [void]$script:submissionIds.Add($response.data.submissionId)
            }
            
            $completed++
            $pct = [math]::Round($completed * 100 / $totalSubmissions)
            $statusText = "$completed / $totalSubmissions ($pct pct)"
            Write-Progress -Activity "Submitting" -Status $statusText -PercentComplete $pct
            
        } catch {
            $script:fail++
            Write-Host "Submit failed: $_" -ForegroundColor Red
        }
        
        Start-Sleep -Milliseconds 50
    }
}

Write-Progress -Activity "Submitting" -Completed

$submitEndTime = Get-Date
$submitTime = ($submitEndTime - $startTime).TotalSeconds

Write-Host ""
Write-Host "Submissions done! Time: $([math]::Round($submitTime, 2)) seconds" -ForegroundColor Green
Write-Host "Success: $($script:success), Failed: $($script:fail)"

# Wait for judging
Write-Host ""
Write-Host "Waiting for judging to complete..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Verify results
Write-Host ""
Write-Host "Verifying results..." -ForegroundColor Yellow

$verified = 0
$pending = 0
$accepted = 0
$wrongAnswer = 0
$compileError = 0
$other = 0

foreach ($id in $script:submissionIds) {
    try {
        $result = Invoke-RestMethod -Uri "$Url/judge/result/$id" -TimeoutSec 10
        
        if ($result.data.status) {
            $status = $result.data.status
            
            switch ($status) {
                "PENDING" { $pending++ }
                "JUDGING" { $pending++ }
                "ACCEPTED" { $accepted++; $verified++ }
                "WRONG_ANSWER" { $wrongAnswer++; $verified++ }
                "COMPILE_ERROR" { $compileError++; $verified++ }
                default { $other++; $verified++ }
            }
        }
    } catch {
        # Ignore query errors
    }
}

$endTime = Get-Date
$totalTime = ($endTime - $startTime).TotalSeconds

# Report
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "       Test Report" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "[Submission Stats]"
$totalCount = $script:success + $script:fail
Write-Host "  Total: $totalCount"
Write-Host "  Success: $($script:success)"
Write-Host "  Failed: $($script:fail)"
$successRate = 0
if ($totalCount -gt 0) { 
    $successRate = [math]::Round($script:success * 100 / $totalCount, 2) 
}
Write-Host "  Success Rate: $successRate percent"
Write-Host ""
Write-Host "[Judge Stats]"
Write-Host "  Completed: $verified"
Write-Host "  Pending: $pending"
Write-Host "  AC: $accepted"
Write-Host "  WA: $wrongAnswer"
Write-Host "  CE: $compileError"
Write-Host "  Other: $other"
Write-Host ""
Write-Host "[Performance]"
Write-Host "  Total Time: $([math]::Round($totalTime, 2)) seconds"
Write-Host "  Submit Time: $([math]::Round($submitTime, 2)) seconds"
$throughput = 0
if ($submitTime -gt 0) { 
    $throughput = [math]::Round($script:success / $submitTime, 2) 
}
Write-Host "  Throughput: $throughput submissions/sec"
Write-Host ""
Write-Host "[Conclusion]"

if ($successRate -ge 99) {
    Write-Host "  [PASS] Success rate >= 99 percent" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Success rate: $successRate percent (target >= 99)" -ForegroundColor Red
}

if ($script:fail -eq 0) {
    Write-Host "  [PASS] No service crashes" -ForegroundColor Green
} else {
    Write-Host "  [WARN] $($script:fail) requests failed" -ForegroundColor Yellow
}

if ($pending -eq 0) {
    Write-Host "  [PASS] No data loss" -ForegroundColor Green
} else {
    Write-Host "  [WARN] $pending submissions still pending" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
