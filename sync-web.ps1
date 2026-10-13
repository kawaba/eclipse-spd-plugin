<#
  元の SPD エディタ（kawaba/spd-editor）から、プラグインの web/ へファイルを複製する。
  web/editor.html などはこのスクリプトで上書きするので、直接編集しない（直すのは元の spd-editor.html）。
  UTF-8 でないファイル（Shift_JIS）は UTF-8 に変換して複製する。
  あわせて、プラグインの版（pom・MANIFEST・feature.xml）を SPD エディタの版（ver. X.Y.Z）にそろえる。

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

# editor.html は UTF-8 のページで、JS も UTF-8 として読む。元が UTF-8 でなければ
# （メモ帳などで Shift_JIS 保存された場合）文字化けしてスケルトンが出なくなるので、Shift_JIS とみなして UTF-8 に変換する
$utf8Strict = New-Object System.Text.UTF8Encoding($false, $true)   # BOM なし・不正なバイトで例外
$sjis = [System.Text.Encoding]::GetEncoding(932)

foreach ($f in $files) {
    $from = Join-Path $Source $f.From
    $to   = Join-Path $web $f.To
    if (-not (Test-Path $from)) { throw "見つかりません: $from" }
    $bytes = [System.IO.File]::ReadAllBytes($from)
    $note = ""
    try {
        $null = $utf8Strict.GetString($bytes)
    } catch [System.Text.DecoderFallbackException] {
        $bytes = $utf8Strict.GetBytes($sjis.GetString($bytes))
        $note = "（Shift_JIS → UTF-8 に変換）"
        Write-Warning ("UTF-8 ではありません。元のファイルも UTF-8 で保存し直してください: " + $from)
    }
    $same = (Test-Path $to) -and ([Convert]::ToBase64String($bytes) -eq [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($to)))
    if ($same) {
        Write-Host ("変更なし  " + $f.To + $note)
    } else {
        [System.IO.File]::WriteAllBytes($to, $bytes)
        Write-Host ("更新      " + $f.To + "  ← " + $f.From + $note)
    }
}

# 表示バージョン（画面右上の ver. X.Y.Z）を知らせる
$m = Select-String -Path (Join-Path $web "editor.html") -Pattern 'class="ver">ver\. ([0-9]+\.[0-9]+\.[0-9]+)<' | Select-Object -First 1
if (-not $m) { throw "editor.html に ver. X.Y.Z が見つかりません" }
$ver = $m.Matches[0].Groups[1].Value
Write-Host ("SPD エディタ ver. " + $ver)

# プラグインの版を SPD エディタの版にそろえる（pom は X.Y.Z-SNAPSHOT、MANIFEST・feature.xml は X.Y.Z.qualifier）。
# qualifier はビルド日時になるので、Java 側だけ直したときも版はそのままで、新しいビルドとして更新が届く
$pom = Join-Path $PSScriptRoot "pom.xml"
$cur = ([xml](Get-Content $pom -Raw -Encoding UTF8)).project.version
$want = "$ver-SNAPSHOT"
if ($cur -eq $want) {
    Write-Host ("プラグインの版  変更なし（$cur）")
} else {
    $tycho = ([xml](Get-Content $pom -Raw -Encoding UTF8)).project.properties.'tycho.version'
    Push-Location $PSScriptRoot
    try {
        & .\mvnw.cmd -q "org.eclipse.tycho:tycho-versions-plugin:${tycho}:set-version" "-DnewVersion=$want"
        if ($LASTEXITCODE -ne 0) { throw "プラグインの版を変えられませんでした" }
    } finally { Pop-Location }
    Write-Host ("プラグインの版  $cur → $want")
}
