<#
  元の SPD エディタ（kawaba/spd-editor）から、プラグインの web/ へファイルを複製する。
  web/editor.html などはこのスクリプトで上書きするので、直接編集しない（直すのは元の spd-editor.html）。

    元の spd-editor.html        → spd.editor/web/editor.html
    元の 2-java/spd-config.js   → spd.editor/web/spd-config.js
    元の 2-java/spd-patterns.js → spd.editor/web/spd-patterns.js

  使い方（リポジトリの直下で）:
    powershell -ExecutionPolicy Bypass -File .\sync-web.ps1
    powershell -ExecutionPolicy Bypass -File .\sync-web.ps1 -Source D:\work\spd-editor
#>
param(
    # 元の SPD エディタのフォルダ
    [string]$Source = "D:\★SPD\spd-editor"
)

$ErrorActionPreference = "Stop"
$web = Join-Path $PSScriptRoot "spd.editor\web"

$files = @(
    @{ From = "spd-editor.html";        To = "editor.html" },
    @{ From = "2-java\spd-config.js";   To = "spd-config.js" },
    @{ From = "2-java\spd-patterns.js"; To = "spd-patterns.js" }
)

foreach ($f in $files) {
    $from = Join-Path $Source $f.From
    $to   = Join-Path $web $f.To
    if (-not (Test-Path $from)) { throw "見つかりません: $from" }
    $same = (Test-Path $to) -and ((Get-FileHash $from).Hash -eq (Get-FileHash $to).Hash)
    if ($same) {
        Write-Host ("変更なし  " + $f.To)
    } else {
        Copy-Item $from $to -Force
        Write-Host ("更新      " + $f.To + "  ← " + $f.From)
    }
}

# 表示バージョン（画面右上の ver. X.Y.Z）を知らせる
$m = Select-String -Path (Join-Path $web "editor.html") -Pattern 'class="ver">ver\. ([0-9.]+)<' | Select-Object -First 1
if ($m) { Write-Host ("SPD エディタ ver. " + $m.Matches[0].Groups[1].Value) }
