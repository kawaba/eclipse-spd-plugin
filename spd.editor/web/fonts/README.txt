SPD エディタ用フォント PlemolJP HS
====================================

このフォルダには次のファイルがあります。

  PlemolJPHS-Regular.ttf  フォント PlemolJP HS（v3.1.0）
  LICENSE-PlemolJP.txt    フォントのライセンス（SIL Open Font License 1.1）
  spd-font.epf            Eclipse のフォント設定（エディタ・コンソールなどを PlemolJP HS 12pt にする）
  README.txt              この説明


■ SPD エディタの画面

  何もしなくても、SPD エディタの画面はこのフォントで表示されます。
  （このフォルダのフォントを SPD エディタが直接読み込みます。インストールは不要です）


■ Export した SPD が Java エディタなどでずれるとき

  SPD の罫線（─ │ ├ など）は、全角の幅で表示するフォントでないと桁がずれます。
  Java エディタや application.properties に貼り付けた SPD がずれる場合は、
  次の 1・2 を行うと、SPD エディタと同じフォントで表示されます。

  1. フォントを Windows にインストールする
     PlemolJPHS-Regular.ttf を右クリック →「インストール」
     （管理者権限は不要です。すでにインストール済みなら不要です）

  2. Eclipse のフォント設定を取り込む
     (1) Eclipse のメニュー「ファイル → インポート」
     (2)「一般 → 設定」を選んで［次へ］
     (3)「ソース設定ファイル」の［参照］で、このフォルダの spd-font.epf を選ぶ
     (4)［完了］。再起動を求められたら［再開］

  ※ 必ず 1 を先に行ってください。フォントが無いまま 2 を行うと、別のフォントで表示されます。
  ※ 元に戻すには「ウィンドウ → 設定 → 一般 → 外観 → 色とフォント」で
     各フォントを選び［デフォルトに戻す］を押します。
  ※ spd-font.epf は Windows 用です。mac・Linux では「色とフォント」で
     「テキスト・フォント」などを PlemolJP HS に変えてください。


■ フォントについて

  PlemolJP（作者 Yuko OTAWARA 氏）https://github.com/yuru7/PlemolJP
  SIL Open Font License 1.1 に従って、改変せずに同梱しています。
