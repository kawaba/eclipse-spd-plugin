# CLAUDE.md — SPD Editor for Eclipse

このリポジトリは、HTML/JavaScript 製の SPD エディタを Eclipse プラグイン化するプロジェクトです。
Claude Code はこのファイルの方針に従って作業してください。

## 基本方針

- **画面（SPDの描画・編集）は HTML/JavaScript のまま使う。** Java に書き直さない。
- Java 側は「Eclipse との連携」だけを担当する（ファイル入出力、保存、ダーティフラグ、メニュー、設定画面など）。
- 既存の SPD エディタのファイルは `spd.editor/web/` に置く。入口は `web/editor.html`。
- CDN は使わない。JS/CSS/フォント以外のライブラリもすべて `web/` 内に同梱する（授業環境はオフラインやプロキシ下の場合がある）。
- コメント・UI 文言・コミットメッセージは日本語。

## 構成

```
spd-eclipse/
├── pom.xml                 親POM（Tycho）
├── spd.editor/             プラグイン本体
│   ├── META-INF/MANIFEST.MF
│   ├── plugin.xml          .spd の関連付け、エディタ・ビュー・ウィザード・メニュー「SPD」・ツールバー
│   ├── icons/spd.png       アイコン（16px と @2x の 32px）
│   ├── src/work/powercampus/spd/editor/
│   │   ├── Activator.java
│   │   ├── SpdBrowser.java Browser で web/editor.html を表示し、約束事の Java 側を持つ（エディタ・ビュー共用）
│   │   ├── SpdView.java    ファイルなしで使う SPD ビュー（主な使い方）。内容は .metadata に自動保管、
│   │   │                   「保存…」で名前を付けて .spd に保存
│   │   ├── SpdEditor.java  .spd ファイルを開いたときのエディタ（保存＝上書き、ダーティ表示）
│   │   └── SpdNewFileWizard.java  新規 .spd 作成ウィザード（言語を選ぶ）
│   └── web/editor.html     既存 SPD エディタ（D:\★SPD\spd-editor\spd-editor.html）＋ Eclipse 連携
├── spd.feature/            Feature（Marketplace 登録に必要）
└── spd.site/               p2 アップデートサイト（target/repository/ を公開する）
```

## Java ⇔ JavaScript の約束事（変更する場合は両側を同時に直す）

| 方向 | 名前 | 説明 |
|---|---|---|
| Java → JS | `loadSpd(text)` | ファイル内容（文字列）を画面に読み込む。読み込み中は変更通知を出さない |
| Java → JS | `getSpdText()` | 保存用に現在の SPD テキストを文字列で返す |
| JS → Java | `spdGetInitialText()` | ファイル内容を取得する（Java の BrowserFunction） |
| JS → Java | `spdNotifyChanged()` | 編集されたら呼ぶ（エディタ：未保存表示。ビュー：自動保管） |
| JS → Java | `spdRequestSave()` | Ctrl+S・「保存」ボタン（エディタ：上書き保存。ビュー：名前を付けて保存） |

- Java から JS へ文字列を渡すときは、`execute("f('" + text + "')")` のような文字列連結をしない。
  エスケープ漏れを避けるため、JS 側から BrowserFunction で取得させる。
- BrowserFunction の処理中に `browser.evaluate()` を呼ばない（Edge では null が返る）。
  保存や画面更新など JS を呼び返す可能性がある処理は `asyncExec` で JS 呼び出しの後に回す。
- Java 側関数は存在チェックしてから呼ぶ（`typeof window.spdXxx === "function"`）。
  既存エディタを単体のブラウザでも動かせるようにしておくため。
- `.spd` ファイルの形式は下の「.spd ファイル形式」を参照。
  `loadSpd` は空文字（新規ファイル）と、JSON でない SPD テキスト（Export 形式）も受け付ける。
- Java は `web/editor.html?host=eclipse&mode=editor|view` で開く。JS 側は host（または BrowserFunction の有無）で
  Eclipse 内と判断し、localStorage の自動保存・離脱確認・「開く」ボタンを無効にする。
  mode=view では「保存」ボタンを「保存…」（名前を付けて保存）と表示する。
- Eclipse 2026 では未保存のエディタはタブ名に `*` が付かず、閉じるボタン × が ● になる（既定の設定）。
- `web/spd-config.js`（既定の言語）と `web/spd-patterns.js`（スケルトン）は既存エディタの `2-java/` から複製したもの。

## .spd ファイル形式（2026-10-09 決定）

既存エディタの「保存」と同じ JSON を正式な形式とする（単体ブラウザ版の `.json` と相互に読み書きできる）。

```json
{
  "lang": "java",
  "data": {
    "3,1": "│",
    "3,2": "aa"
  }
}
```

- `lang`：`java` / `thymeleaf` / `python` / `c`（JS の `LANGS` と `SpdNewFileWizard.LANGS` を揃える）
- `data`：キーは `"x,y"`（0 起点の列,行）、値はそのマスの文字（全角1文字、または半角2文字）。空マスは持たない
- 文字コードは UTF-8。人が読む・Git で差分を見る・コード生成の入力にするテキスト形式は、
  必要になったら「書き出し」として別に用意する（ファイル形式は変えない）
- 形式を変えるときは、古いファイルも読めるようにする（`loadSpd` で判別する）

## 作業の進め方（推奨順）

1. `web/editor.html` を既存の SPD エディタに置き換え、上の約束事の関数を追加する
2. 開いて・編集して・保存できることを確認する（下の「動作確認」）
3. 新規 `.spd` ファイル作成ウィザード（`org.eclipse.ui.newWizards`）
4. コード生成機能（方式は未決定：ルールベース変換 / Copilot 連携 / Claude Code CLI 呼び出し）
   - API キーをプラグインに埋め込まない。必要なら設定画面（PreferencePage）で利用者が入力する
5. 「名前を付けて保存」、ワークスペース外ファイル（IURIEditorInput）への対応
6. ヘルプ（アイコンとライセンスは済み。ライセンスは EPL-2.0：`LICENSE`・`feature.xml`・MANIFEST の `Bundle-License`）

## ビルド

```
mvn clean verify
```

- 成果物: `spd.site/target/repository/`（p2 アップデートサイト）
- Eclipse で「ヘルプ → 新規ソフトウェアのインストール → 追加 → ローカル」でこのフォルダを指定するとインストールできる
- Tycho と Eclipse リリース（ターゲット）は親 `pom.xml` の `tycho.version` / `eclipse.release.url` で変更する
- `.mvn/jvm.config` は JDK 24 以降の XML サイズ制限（`jdk.xml.maxGeneralEntitySizeLimit`）を外す設定。
  これが無いと Eclipse リリースの p2 リポジトリ（content.xml）が読めずビルドが失敗する。消さない

## 動作確認（Eclipse 上で開発する場合）

1. Eclipse IDE for RCP and RAP Developers（PDE 入り）で、4つのフォルダを「既存プロジェクトをワークスペースへ」インポート
2. `spd.editor` を右クリック → 実行 → Eclipse アプリケーション
3. 起動した Eclipse で任意のプロジェクトに `test.spd` を作り、開く

## 注意点

- Browser のエンジンは OS ごとに違う（Windows: Edge/WebView2、mac: WebKit、Linux: WebKitGTK）。
  新しい JS 構文を使う場合は 3 環境で動くか確認する。
- `Eclipse-BundleShape: dir` を外さない（外すと web/ 内の CSS・JS が読めなくなる）。
- `Bundle-Version` と `feature.xml` の version、各 `pom.xml` のバージョンは揃えて上げる
  （`mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=0.2.0-SNAPSHOT` で一括変更できる）。
