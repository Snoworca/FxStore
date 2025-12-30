$bytes = [System.IO.File]::ReadAllBytes('C:\Work\git\_Snoworca\FxStore\src\main\java\com\snoworca\fxstore\api\CollectionInfo.java')
$hexStr = ($bytes[60..120] | ForEach-Object { $_.ToString('X2') }) -join ' '
Write-Host "Bytes 60-120:"
Write-Host $hexStr

# Check if it looks like UTF-16 (alternating 00 bytes)
$hasAlternatingZeros = ($bytes | Select-Object -Skip 1 -First 10 | Where-Object { $_ -eq 0 }).Count -gt 3
if ($hasAlternatingZeros) {
    Write-Host "Likely UTF-16 encoding"
} else {
    Write-Host "Not UTF-16"
}
