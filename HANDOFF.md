# HANDOFF.md (直前セッションからの申し送り、直近1回分のみ)

## 今回やったこと(2026-08-31、こんぺいとう氏との直接チャットセッション、v0.34.1リリース)

直前の定期実行セッションが実装したJEIオーバーレイの簡易対応(JEIのレシピ画面等で「カーソル下の素材」の代わりに「手に持っているアイテム」を表示するフォールバック)について、こんぺいとう氏本人から「ＪＥＩの修正は混乱を招くからちゃんと修正してくれ」と直接の指摘を受けた。これに応え、JEI本体のPlugin APIを使った本格的な修正を実施した。

具体的には:
- `com.claudemod.compat.jei.JeiCompat`/`ClaudeModJeiPlugin`を新規実装。JEIの`IJeiRuntime#getScreenHelper()#getClickableIngredientUnderMouse(Screen, double, double)`(JEI公式1.20.1ブランチのソースで仕様を確認済み)を使い、レシピ画面・素材一覧・ブックマークなどJEIが把握するあらゆる画面上で、実際にカーソル下にある素材そのものを取得するようにした。
- `ItemDetailsOverlay`のフォールバック処理を、上記`JeiCompat`経由の実際の素材検出に置き換えた。JEI未導入・カーソル下に何も無い場合は何も表示しない(誤情報を出さないことを優先)。
- `build.gradle`/`gradle.properties`/`mods.toml`にJEIへの`compileOnly`/`runtimeOnly`ソフト依存(`jei_version=15.56.0.204`)を追加。CuriosCompatと全く同じ「JEI未導入でも本体は問題なく動く」設計パターンを踏襲。
- CIビルド成功を確認済み(commit 50111ab、`status=ok`)。以前から懸念していた「JEIをruntimeOnlyに追加するとヘッドレスの`runGameTestServer`がクラッシュするのでは」というリスクも、実際には問題なく通ることを確認できた。
- v0.34.1としてバージョンアップ・リリースノート追加・タグ付けまで実施。

## 次回最優先でやるべきこと

- 上記JEI連携の実機確認(こんぺいとう氏に依頼): JEIのレシピ画面・素材一覧・ブックマーク画面それぞれでWキー長押しが正しい素材を表示するか、JEI未導入環境でMOD本体が問題なく動くか。
- PROGRESS.mdの「2. TODO」に残る他の項目(特にTODO1羽石再確認・TODO7 FE移動アルゴリズムの最重要バグ)に引き続き対応すること。TODO7は「具体的な再現条件」の確認がまだ取れていない。
- Issue #21(JEI互換性、このMOD自身のレシピをJEI一覧に出す件)は今回のJEI連携基盤(`ClaudeModJeiPlugin`)を土台に着手しやすくなっている。TODO16参照。

## 注意点

- タグ付きリリース済み: v0.34.1。RELEASE_NOTES.mdの最新セクションがGitHub Releaseの本文になる。
- JEI関連の新規ファイルは全て`com.claudemod.compat.jei`パッケージ内(`JeiCompat.java`/`ClaudeModJeiPlugin.java`)。これ以外のファイルは`mezz.jei.*`を一切importしていない(ソフト依存の原則を維持)。
