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
  - インストール時の画面に表示される。`<copyright>` は `Copyright (c) 2026 Takashi Kawaba`（2026-10-10 決定）
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

**プラグインの版は、SPD エディタの画面右上の版（`ver. X.Y.Z`）と同じにします**（2026-10-10 決定）。
手で変える必要はなく、`sync-web.ps1` が複製のあとに pom・MANIFEST・feature.xml の版をそろえます。

```powershell
powershell -ExecutionPolicy Bypass -File .\sync-web.ps1
# 例：SPD エディタ ver. 1.20.0 / プラグインの版  0.1.0-SNAPSHOT → 1.20.0-SNAPSHOT
```

- pom は `1.20.0-SNAPSHOT`、MANIFEST・feature.xml は `1.20.0.qualifier` のまま公開してよい
  （Tycho の決まりで、`-SNAPSHOT` を外すと `qualifier` も付かなくなる）
- `qualifier` はビルド時に日時（例：`1.20.0.202610101830`）に置き換わる。
  Java 側だけを直して版が変わらないときも、新しいビルドのほうが大きくなるので利用者に更新が届く

### 2.2 ビルド

```powershell
mvn clean verify
```

成果物は `spd.site/target/repository/`（`artifacts.jar`・`content.jar`・`features/`・`plugins/` など）です。
このフォルダの中身が、そのままアップデートサイトになります。

### 2.3 コミットとタグ

タグは `v` ＋ SPD エディタの版にします。Java 側だけを直して出し直すときは、`-2`・`-3` と枝番を付けます。

```powershell
git add -A
git commit -m "SPD エディタ v1.20.0 で公開する"
git tag v1.20.0          # Java 側だけ直して出し直すときは v1.20.0-2 など
git push origin main v1.20.0
```

---

## 3. アップデートサイトを公開する（GitHub Pages）

公開は GitHub Actions（`.github/workflows/pages.yml`）が自動で行います。
**`v` で始まるタグを push すると**、GitHub 上で `mvn clean verify` が走り、
`spd.site/target/repository/` に説明ページ `spd.site/pages/index.html` を加えたものが次の URL に公開されます。

```
https://kawaba.github.io/eclipse-spd-plugin/
```

Marketplace に登録する前でも、この URL を学生に伝えれば同じようにインストール・更新できます
（URL を知っていれば誰でも見られるが、どこにも載せなければ見つかることはまずない）。

### 3.1 初回だけ：GitHub Pages を有効にする

次のどちらかで、公開方式を「GitHub Actions」にします（有効にしただけでは何も公開されない）。

- 画面で：リポジトリ → **Settings → Pages** → 「Build and deployment」の Source を **GitHub Actions** にする
- コマンドで：`gh api -X POST repos/kawaba/eclipse-spd-plugin/pages -f build_type=workflow`

続けて、タグからの公開を許します（初期設定では `main` からしか公開できず、タグの公開が
「environment protection rules」で拒否される）。

- 画面で：**Settings → Environments → github-pages** の「Deployment branches and tags」に、種類 Tag・名前 `v*` を追加
- コマンドで：`gh api -X POST repos/kawaba/eclipse-spd-plugin/environments/github-pages/deployment-branch-policies -f name='v*' -f type=tag`

### 3.2 公開する（公開のたびに行う）

2.3 でタグを push すると、自動で始まります。タグを付けずに今の `main` を公開し直したいときは、
リポジトリの **Actions → 「アップデートサイトを公開」→ Run workflow** で手動実行します
（コマンドなら `gh workflow run pages.yml`）。

進み具合は Actions の画面か `gh run watch` で確認します。失敗したときは、その実行のログを見ます。

### 3.3 インストール時に更新先が登録される

`spd.feature/p2.inf` により、SPD Editor をインストールすると、上の URL が
「使用可能なソフトウェア・サイト」に自動で登録されます。ローカルのフォルダや zip から入れた人にも、
「ヘルプ → 更新の確認」で新しい版が届きます。

### 3.4 公開した URL で確認する

ブラウザで `https://kawaba.github.io/eclipse-spd-plugin/` を開いて説明ページが出ることと、
`https://kawaba.github.io/eclipse-spd-plugin/p2.index` が表示されることを確かめてから、
**別の（プラグインを入れていない）Eclipse** で次を確認します。

1. 「ヘルプ → 新規ソフトウェアのインストール → 追加」で、上の URL を「ロケーション」に入れる
2. カテゴリ「SPD Tools」の下に「SPD Editor」が出て、インストールできる
3. 途中で「署名されていないコンテンツ」の警告が出る（下の「補足：署名」参照）。「とにかくインストール」で進める

> この URL は Marketplace に登録したあとも変えないこと。変えると、インストール済みの人が更新を受け取れなくなる。
> どうしても変えるときは、`spd.feature/p2.inf`・`spd.site/pages/index.html`・この手順書の URL をそろえて直す。

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
| Version | `1.20.0`（SPD エディタの版） |
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

1. 元の SPD エディタを直した場合は `sync-web.ps1` で複製する（プラグインの版もそろう。2.1）
2. `mvn clean verify` → コミット・タグを push（2.2〜2.3）。タグの push で自動的に公開される（3.2）
3. Marketplace の掲載ページを編集し、Version と変更内容を更新する（Update Site URL・Feature ID は変えない）

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

公開のたびに中身を丸ごと入れ替えるので、アップデートサイトには最新版だけが残ります。
古いバージョンも選べるようにしたい場合は、`1.20.0/`・`1.21.0/` のようにフォルダを分けて置き、
直下に「コンポジットリポジトリ」（`compositeContent.xml`・`compositeArtifacts.xml`）を作って、それらをまとめます。
（そのときはワークフローで、過去の版を残したまま新しい版を足すように直す）

-------------------------
次の2点は確かめていないか、手順書の中で判断が必要です。

- Marketplace の画面の項目名：確かめておらず、変わることもあるため、手順書では「目安」と書いています。
- 署名の警告：jar に署名していないので、インストール時に「署名されていないコンテンツ」の警告が出ます。インストールはできるので、授業用ならこのままでも使えます。
