Get-ChildItem -Path "C:\Work" -Directory -Recurse -Depth 2 -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -like '*FxStore*' -or $_.Name -like '*fxstore*' } |
    Select-Object FullName
