<#
  日本語の .properties（*_ja.properties）の ASCII 以外の文字を \uXXXX に変換する。
  Eclipse の NLS（Messages.java）は .properties を ISO-8859-1 として読むため、日本語をそのまま書くと文字化けする。

  使い方（リポジトリの直下で）:
    1. *_ja.properties の行を日本語のまま書き換える（例： SpdNewFileWizard_lang=言語）
    2. powershell -ExecutionPolicy Bypass -File .\escape-ja.ps1
  変換した行のすぐ上に、読めるように「# キー=日本語」のコメントを付ける（前からあれば置き換える）。
  変換済みの行は ASCII だけなので、何度実行してもよい。
#>
param(
    [string[]]$Path = @(Get-ChildItem -Path $PSScriptRoot -Recurse -Filter "*_ja.properties" |
        Where-Object { $_.FullName -notmatch '\\(target|bin)\\' } | ForEach-Object FullName)
)

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding $false

foreach ($p in $Path) {
    $lines = [System.IO.File]::ReadAllLines($p, $utf8)
    $out = New-Object System.Collections.Generic.List[string]
    $changed = 0
    foreach ($line in $lines) {
        $isComment = $line -match '^\s*[#!]'
        if (-not $isComment -and $line -match '[^\x00-\x7F]') {
            $key = ($line -split '=', 2)[0].Trim()
            # 直前に同じキーのコメントがあれば置き換える
            if ($out.Count -gt 0 -and $out[$out.Count - 1] -match ('^# ' + [regex]::Escape($key) + '=')) {
                $out.RemoveAt($out.Count - 1)
            }
            $out.Add("# " + $line.Trim())
            $sb = New-Object System.Text.StringBuilder
            foreach ($ch in $line.ToCharArray()) {
                if ([int]$ch -lt 0x80) { [void]$sb.Append($ch) } else { [void]$sb.AppendFormat('\u{0:x4}', [int]$ch) }
            }
            $out.Add($sb.ToString())
            $changed++
        } else {
            $out.Add($line)
        }
    }
    if ($changed) {
        [System.IO.File]::WriteAllText($p, (($out -join "`n") + "`n"), $utf8)
        Write-Host ("変換 {0} 行  {1}" -f $changed, $p)
    } else {
        Write-Host ("変更なし  " + $p)
    }
}
