# SPD Editor を Eclipse Marketplace に登録するまでの手順

Eclipse Marketplace（https://marketplace.eclipse.org/）は、プラグインそのものを預かる場所ではありません。
**「p2 アップデートサイトの URL」と説明・画像を登録する掲示板**です。
そのため、作業は大きく次の 3 段階になります。

1. リリース用にビルドする（`spd.site/target/repository/` を作る）
2. そのフォルダをインターネット上に公開する（このリポジトリでは GitHub Pages を使う）
3. Marketplace に公開した URL を登録する

---

## 0. 事前に用意するもの

| もの | 用途 | 備考 |
|---|---|---|
| Eclipse Foundation アカウント | Marketplace へのログイン・登録 | https://accounts.eclipse.org/ で無料作成 |
| GitHub リポジトリ `kawaba/eclipse-spd-plugin` | アップデートサイトの置き場所（GitHub Pages） | 公開（Public）リポジトリであること |
| アイコン画像 | Marketplace の一覧に出るロゴ | 正方形の PNG。`spd.editor/icons/spd@2x.png`（32px）は小さいので、大きめのもの（例：128px 以上）を別に用意する |
| スクリーンショット | 紹介ページ用 | SPD ビュー・エディタで SPD を描いた画面など 1〜3 枚 |
| 紹介文 | 一覧・詳細ページ用 | 短い説明（1 行）と詳しい説明。英語と日本語を併記すると海外の人にも伝わる |

---

## 1. 公開前の確認

### 1.1 中身の確認

- [ ] `web/editor.html`・`spd-config.js`・`spd-patterns.js` が元の SPD エディタの最新になっている
      （`powershell -ExecutionPolicy Bypass -File .\sync-web.ps1` を実行し、すべて「変更なし」になる）
- [ ] 同梱フォントのライセンス文 `web/fonts/LICENSE-PlemolJP.txt` が入っている（SIL OFL 1.1 の条件）
- [ ] `feature.xml` の表示内容を見直す
  - `label`（SPD Editor）、`provider-name`（powercampus.work）、`<description>`、`<copyright>`
  - インストール時の画面に表示される。`<copyright>` の名前の書き方（フルネームにするか等）もここで決める
- [ ] ライセンスが EPL-2.0 でそろっている（`LICENSE`・`feature.xml`・MANIFEST の `Bundle-License`）

### 1.2 動作確認

Marketplace には「対応する Eclipse のバージョン」を書く欄があるので、実際に試したバージョンを控えておきます。

- ビルドのターゲットは親 `pom.xml` の `eclipse.release.url`（現在 2025-09）、Java は 17 以上（MANIFEST の `Bundle-RequiredExecutionEnvironment`）
- 授業で使う Eclipse（2026 系）と、ターゲットの 2025-09 の 2 つで、次を確認する
  - [ ] 「ヘルプ → 新規ソフトウェアのインストール → 追加 → ローカル」で `spd.site/target/repository/` からインストールできる
  - [ ] メニュー「SPD」・ツールバーから SPD ビューが開き、描画・自動保管・「保存…」ができる
  - [ ] 新規 `.spd` ウィザードで作ったファイルを開いて、編集・上書き保存（Ctrl+S）ができる
  - [ ] 「SPD → フォントのフォルダを開く」でフォルダが開く
- 可能なら Windows 以外（mac / Linux）でも表示を確認する（Browser のエンジンが違うため）。
  確認できなかった OS は、Marketplace の対応プラットフォームに入れないか、「未確認」と説明に書く

---

## 2. リリース用のビルド

### 2.1 バージョンを決める

開発中は `0.1.0-SNAPSHOT` ですが、公開するものは `-SNAPSHOT` を外したバージョンにします。

```powershell
# リポジトリの直下で（MANIFEST・feature.xml・各 pom.xml がまとめて変わる）
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=0.1.0
```

- `Bundle-Version: 0.1.0.qualifier` の `qualifier` は、ビルド時に日時（例：`0.1.0.202610091230`）に置き換わる
- 2 回目以降の公開では、必ず前回より大きいバージョンにする（同じだと利用者の Eclipse が更新を見つけない）

### 2.2 ビルド

```powershell
mvn clean verify
```

成果物は `spd.site/target/repository/`（`artifacts.jar`・`content.jar`・`features/`・`plugins/` など）です。
このフォルダの中身が、そのままアップデートサイトになります。

### 2.3 コミットとタグ

```powershell
git add -A
git commit -m "バージョン 0.1.0 を公開する"
git tag v0.1.0
git push origin main --tags
```

公開が終わったら、次の開発用に `-SNAPSHOT` へ戻しておきます。

```powershell
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=0.2.0-SNAPSHOT
git commit -am "開発用のバージョンを 0.2.0-SNAPSHOT にする"
```

---

## 3. アップデートサイトを公開する（GitHub Pages）

ソース（`main` ブランチ）と公開用のファイルを混ぜないように、**`gh-pages` ブランチ**を公開専用にします。

### 3.1 初回だけ：gh-pages ブランチを作る

```powershell
# リポジトリの直下で。main とは別のフォルダ（..\spd-site）に gh-pages を取り出す
git worktree add --orphan -b gh-pages ..\spd-site
```

> `--orphan` が使えない古い Git の場合は、GitHub の画面で空の `gh-pages` ブランチを作ってから
> `git worktree add ..\spd-site gh-pages` とする。

### 3.2 ビルド結果をコピーして push する（公開のたびに行う）

```powershell
# 古い中身を消してから、新しいビルド結果をコピーする（.git は残す）
Get-ChildItem ..\spd-site -Exclude .git | Remove-Item -Recurse -Force
Copy-Item spd.site\target\repository\* ..\spd-site -Recurse
# GitHub Pages の Jekyll 処理を止める（ファイルがそのまま配信されるように）
New-Item -ItemType File ..\spd-site\.nojekyll -Force | Out-Null

git -C ..\spd-site add -A
git -C ..\spd-site commit -m "アップデートサイト 0.1.0"
git -C ..\spd-site push origin gh-pages
```

### 3.3 初回だけ：GitHub Pages を有効にする

1. GitHub のリポジトリ → **Settings → Pages**
2. Source を「Deploy from a branch」、Branch を `gh-pages` / `/(root)` にして保存
3. 数分後、次の URL が使えるようになる

```
https://kawaba.github.io/eclipse-spd-plugin/
```

### 3.4 公開した URL で確認する

ブラウザで `https://kawaba.github.io/eclipse-spd-plugin/p2.index` が表示されることを確かめてから、
**別の（プラグインを入れていない）Eclipse** で次を確認します。

1. 「ヘルプ → 新規ソフトウェアのインストール → 追加」で、上の URL を「ロケーション」に入れる
2. カテゴリ「SPD Tools」の下に「SPD Editor」が出て、インストールできる
3. 途中で「署名されていないコンテンツ」の警告が出る（下の「補足：署名」参照）。「とにかくインストール」で進める

> この URL は Marketplace に登録したあとも変えないこと。変えると、インストール済みの人が更新を受け取れなくなる。

---

## 4. Eclipse Marketplace に登録する

### 4.1 登録画面を開く

1. https://marketplace.eclipse.org/ を開き、右上から Eclipse Foundation アカウントでログイン
2. 「Add Content」（コンテンツの追加）を選び、種類は **Solution**（プラグイン）を選ぶ

> 画面の項目名や並びは変わることがあります。以下は主な項目と、このプラグインで入れる値の目安です。

### 4.2 入力する内容

| 項目 | 入れる値（例） |
|---|---|
| Name（名前） | `SPD Editor` |
| Short description（短い説明） | `Editor for SPD (Structured Programming Diagrams) / SPD（構造化プログラム図）エディタ` |
| Description（説明） | 何ができるか、画面の使い方（メニュー「SPD」・SPD ビュー・`.spd` ファイル）、フォントについて（README.txt の内容の要約）、対応言語（Java / Thymeleaf / Python / C） |
| Logo（ロゴ） | 用意した正方形のアイコン |
| Screenshots | 用意したスクリーンショット |
| Categories / Markets | Tools、Editor、Education など近いもの |
| License | `EPL 2.0`（Free / Open Source） |
| Organization / Company | `powercampus.work`（`feature.xml` の provider-name と合わせる） |
| Website / Source / Support | `https://github.com/kawaba/eclipse-spd-plugin`（問い合わせ先は Issues） |
| **Update Site URL** | `https://kawaba.github.io/eclipse-spd-plugin/` |
| **Feature ID** | `work.powercampus.spd.feature`（`feature.xml` の `id`。Marketplace はこの ID を使ってインストールする） |
| Version | `0.1.0` |
| Supported Eclipse versions | 1.2 で確認したリリース（例：2025-09 以降） |
| Supported platforms | 確認した OS（Windows など） |
| Java version | 17 以上 |

### 4.3 公開と確認

1. 保存すると掲載ページができる。公開前に運営側の確認が入る場合があるので、公開されるまで待つ
2. 公開されたら、プラグインを入れていない Eclipse で **「ヘルプ → Eclipse マーケットプレース」** を開き、
   「SPD」で検索して、見つかる・インストールできることを確認する
3. 掲載ページにある「Install」ボタン（ドラッグ＆ドロップでインストールできるリンク）は、
   GitHub の README や授業資料に貼ると便利

---

## 5. 2 回目以降の更新

1. 元の SPD エディタを直した場合は `sync-web.ps1` で複製する
2. バージョンを上げる（2.1。必ず前回より大きく）
3. `mvn clean verify` → コミット・タグ（2.2〜2.3）
4. `gh-pages` に新しいビルド結果をコピーして push（3.2）
5. Marketplace の掲載ページを編集し、Version と変更内容を更新する（Update Site URL・Feature ID は変えない）

利用者の Eclipse では「ヘルプ → 更新の確認」で新しいバージョンが入ります。
（開発機のように更新の確認が効かない環境では、いったんアンインストールして入れ直す）

---

## 補足

### 署名（任意）

今のビルドは jar に署名していないため、インストール時に「署名されていないコンテンツ」の警告が出ます。
インストールはできるので、授業用ならこのままでも使えます。警告を消したい場合は次のどちらかを検討します。

- コード署名証明書（有料）を取得し、`jarsigner` でプラグインと Feature の jar に署名する
- Tycho の `tycho-gpg-plugin` で PGP 署名を付ける（新しい Eclipse は PGP 署名を受け付け、初回に鍵を信頼するか尋ねる）

どちらも鍵や証明書をリポジトリに入れないこと。

### 古いバージョンも残したい場合

3.2 の手順は毎回中身を入れ替えるので、アップデートサイトには最新版だけが残ります。
古いバージョンも選べるようにしたい場合は、`gh-pages` に `0.1.0/`・`0.2.0/` のようにフォルダを分けて置き、
直下に「コンポジットリポジトリ」（`compositeContent.xml`・`compositeArtifacts.xml`）を作って、それらをまとめます。

### 公開を自動にしたい場合

GitHub Actions で「タグを push したら `mvn clean verify` して `gh-pages` に置く」ようにできます。
手作業の公開に慣れてから検討すれば十分です。

-------------------------
次の3点は確かめていないか、手順書の中で判断が必要です。

- Marketplace の画面の項目名：確かめておらず、変わることもあるため、手順書では「目安」と書いています。
- feature.xml の <copyright>：今は Copyright (c) 2026 Takashi です。インストール時の画面に表示されるので、フルネームにするかどうかなどを公開前に決めてください。
- 署名の警告：jar に署名していないので、インストール時に「署名されていないコンテンツ」の警告が出ます。インストールはできるので、授業用ならこのままでも使えます。
