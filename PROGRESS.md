# ClaudeMod 開発進捗 (PROGRESS.md)

このファイルは、1時間ごとに自動起動される開発セッション間の**唯一の記憶**です。
新しいセッションを始める前に必ずこのファイル全体を読んでください。会話履歴は引き継がれません。

最終更新: 2026-09-02(定期実行セッション、v0.39.0リリース: TODO10「MOBのカテゴリ拡充」に対応し、MOD第6体目・初の「飛行アンビエント」モブ「プリズミウム・ウィスプ」を追加。PathfinderMob+FlyingMoveControl/FlyingPathNavigation+WaterAvoidingRandomFlyingGoalという、このMOD初の飛行AI構成(全API個別にmappings.devで実在確認してから使用、v0.37.0のcanPlace()事故の教訓を徹底)。モデルはSquidModel(Drifterで実績済みのUV)を流用し、テクスチャーはPrismium Drifterの既存テクスチャーをHSV色相変換して金色〜紫のウィスプ配色に(スクリプト化・自己レビュー済み)。build-and-notify #297(コード実装push)・#298(バージョンbump push)ともActionsページでStatus Success実際に確認済み。**リリース作業で新たな落とし穴を発見**: タグをpushする前に`git pull`で直前のCI自動コミット(`ci: update ... [skip ci]`)まで取り込んでからその最新コミットにタグを打ったところ、GitHub組み込みの「コミットメッセージに`[skip ci]`が含まれるコミットに対してはpush/tagイベントのワークフロー実行自体が一切作成されない」という挙動により、Release workflowがサイレントに一度も起動しなかった(エラーも出ない)。タグを`[skip ci]`を含まない直前のバージョンbumpコミット自体に打ち直して(`git tag -d`→リモート削除→再作成→再push)解決、Release #48がStatus Success(2m33s)、`https://github.com/Konpeitou24/ClaudeMod/releases/tag/v0.39.0`で本文・Assets 3(jar付き)を確認済み。この教訓は下記「1. 約束や決まり事」に恒久ルールとして追記済み。GitHub Issueは`is:issue is:open`で再確認、#15・#21の2件のみで新規issueなし。詳細はTODO・問題点参照)

**このファイルの構成(2026-08-30に再整理)**: 以前は「セッションごとに実装内容を長文で追記し続ける」運用で肥大化していたため(ピーク時4000行超)、今回から以下の5分類に固定した。

1. 約束や決まり事 — 必ず遵守する恒久的なルール・技術的な決まり事
2. TODO — 次にやるべきことを優先度順(上ほど高い)に並べたリスト
3. 問題点 — 既知の不具合・未検証事項の箇条書き
4. その他 — 参考情報・議論したい論点・通知状況など
5. MOD構想・ロードマップ — MOD全体の方向性

**今後の運用ルール(重要)**: このファイルにセッションごとの実装経緯を長文で追記し続けない。新しい実装をしたら、TODO/問題点/その他を書き換える(該当項目を完了に更新 or 削除 or 新規追加)ことでこのファイル自体は常にコンパクトに保つ。経緯を残したい詳細な実装ログ・議論の記録は`PROGRESS_ARCHIVE.md`に追記すること。

過去の全セッション(#3〜#84)の詳細な実装ログは`PROGRESS_ARCHIVE.md`にある。「なぜこの実装になったか」を調べたい場合はそちらを参照すること。

---

## 1. 約束や決まり事(必ず遵守)

* **Git / push運用**
  * 作業ディレクトリは必ずユニークなパスを使う(`mktemp -d`、またはセッション専用ディレクトリ配下)。`git config user.name/user.email`は`ClaudeMod Session Agent <claudemod-agent@users.noreply.github.com>`に設定する。
  * `git push`はまず素の状態(プロキシ環境変数に手を加えない)で試す。「access denied by the git proxy」等で失敗した場合にのみ`https_proxy="" HTTPS_PROXY="" http_proxy="" HTTP_PROXY=""`を付けて再試行する(順序を逆にしない)。2026-09-01セッションでは素の状態のpushが最初から成功した(プロキシ回避策は不要だった)。
  * 複数セッション同時実行に備え、push前に必ず`git fetch origin main`し、差分があれば`git rebase origin/main`する。
  * **【2026-09-02追記・重要な教訓】タグは`[skip ci]`を含まないコミット(実際にコードやgradle.propertiesを変更した通常のコミット)に対して打つこと。** `ci: update ... [skip ci]`という件名の自動コミット(jar/datapack検証/鉱石検証の3つ)は、GitHub Actions組み込みの仕様により「そのコミットに対するpush/tagイベントのワークフロー実行が一切作成されない」(build-and-notify.ymlはそもそも[skip ci]コミット自体もbuildしないので影響が目立たないが、Release.ymlをこの種のコミットが指すタグに対して打つと、エラーも出ないままRelease workflowが永久に起動しない)。バージョンbump+リリースノート追加のコミット(`Bump version to vX.Y.Z, ...`)を作った直後、CI自動コミットが積まれる前にそのコミット自体へタグを打つのが安全。誤って`[skip ci]`コミットにタグを打ってしまった場合は、`git tag -d vX.Y.Z && git push origin :refs/tags/vX.Y.Z`で削除し、正しいコミットに`git tag -a vX.Y.Z -m vX.Y.Z <commit>`で打ち直してから再pushすること。

* **ビルド・CI確認 / 通知**
  * ローカルビルド(`./gradlew build`)はサンドボックス内では必ず失敗する(プロキシのallowlist制限)。ビルド確認はGitHub Actions経由のみ。
  * `api.github.com`への直接アクセスも不可なため、リポジトリにコミットされる`builds/last_datapack_validation_summary.txt`/`last_ore_verification.txt`/`last_datapack_validation_errors.log`で結果を確認する。
  * Discord Webhookへの直接送信もサンドボックスから不可。通知はGitHub Actions側(`build-and-notify.yml`/`release.yml`)に任せ、無駄なリトライをしない。
  * `.github/workflows/build-and-notify.yml`のNotify Discordステップは、pushに含まれる全コミットの件名(自動コミット除く)を箇条書きで送る仕様。ワークフローを触る際もこの挙動を壊さないこと。
  * **【2026-09-01追記・重要な教訓】push/タグpushをした後は、必ずActionsの実際の成否を確認してから「完了」を報告すること。** `builds/last_datapack_validation_summary.txt`等はCIが成功して初めてコミットされる仕組みなので、直前のコミットに対してまだ更新されていない(ファイルが古いコミットのままを指している)場合、それは「まだCIが終わっていない」可能性と「CIが失敗してコミットされなかった」可能性の両方があり、区別が付かない。`api.github.com`が使えない場合の確認手段: `https://github.com/<owner>/<repo>/actions/workflows/<workflow>.yml`(または`?nocache=1`のようなクエリを付けて`mcp__workspace__web_fetch`のセッション内キャッシュを回避)をfetchし、対象コミットの行の直前にある`aria-label="failed: ..."`/`aria-label="success: ..."`のようなSVGアイコンのaria-labelを確認する。個別runの詳細(コンパイルエラー箇所等)は`https://github.com/<owner>/<repo>/actions/runs/<run_id>/job/<job_id>`をfetchすると`## Annotations`セクションにエラー内容がそのままテキストで出る(ログ本文はサインインしないと見えないが、annotationsは非ログインでも見える)。2026-09-01セッションでは、pushした直後に確認せず「push成功=OK」と早合点してPROGRESS.md/HANDOFF.mdに「完了」と記録してしまい、こんぺいとう氏の指摘で実際にはビルドが3回連続失敗していたことが発覚した(原因はJavaの存在しないAPIメソッドをoverrideしていたこと、詳細は下記・問題点参照)。

* **GitHub Issue対応**
  * 毎回の状況確認にGitHub Issueの確認を含める。`https://github.com/Konpeitou24/ClaudeMod/issues`や個別issueページを非ログインで`curl`取得(キャッシュバスティング用クエリ`?nocache=$(date +%s%N)`を付ける)。コメント本文は`react-app.embeddedData`のJSON(`data['payload']['preloadedQueries'][0]['result']['data']['repository']['issue']`のパス)をパースして読む。2026-09-01セッションではこのJSONパスが見つからなかったため、代わりに本文中の`/Konpeitou24/ClaudeMod/issues/(\d+)`正規表現でissue番号一覧だけを抽出する簡易フォールバックを使った(番号の存在確認だけなら十分)。
  * このgitトークンにはIssueへのコメント投稿・クローズ権限が無いため、`ISSUES_TO_CLOSE.json`/`PENDING_ISSUES.json`のリレー機構(`.github/workflows/`に整備済み)を使う。
  * 投稿者が`Konpeitou24`本人ならその場で対応してよい。それ以外の投稿者のIssueは`PENDING_ISSUES.json`に登録して保留する。
  * 【2026-08-31追記・教訓】Issue一覧の確認は「PROGRESS.mdのTODOに載っているissue番号だけ」ではなく、必ず`is:issue is:open`で全件検索すること。

* **コンテンツ制作(テクスチャー・音)**
  * テクスチャーは第三者の作品をコピーしないこと。Python(Pillow)でゼロから自作するのが基本だが、こんぺいとう氏本人が作成・提供したテクスチャーや、Minecraft本体のバニラテクスチャーを土台に調整する手法も選択肢にしてよい(2026-08-30追記)。いずれの手法でも生成・改変後は必ず拡大画像を目視確認する。
  * **【2026-09-01追記・重要な再利用技術】カスタムブロックの形状・当たり判定をvanilla標準ブロック(ランタン、チェーン等)に近づけたい場合、モデルJSONをvanillaの`minecraft:block/template_*`系テンプレート(例: `template_lantern`/`template_hanging_lantern`)に`textures`だけ差し替えてparentする手法が使える。** これによりvanillaの正確な3D形状(element/face/UV)をそのまま流用でき、独自に当たり判定の座標を推測する必要が無くなる。テンプレートの中身は`https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/<version>/assets/minecraft/models/block/<name>.json`(バージョンごとのブランチを持つ公開ミラー、`raw.githubusercontent.com`はサンドボックスから到達可能)で実際のJSONを直接確認できる。ただしテクスチャー側はテンプレートのUVアンラップ(単純なcube_all前提ではない、部位ごとに異なる矩形)に合わせて描き直す必要がある点に注意(`PrismiumLanternBlock`実装時の実例: `gen_prismium_lantern.py`参照)。
  * 音声(サウンド)方針: バニラの`SoundEvent`を複数レイヤーする(音量・ピッチを変えて`playSound`を複数回呼ぶ)ことを常に第一候補とする。Python合成(numpy等)は、バニラに近い音が本当に存在しない場合に限る最終手段。
  * 新ブロック追加時は関連タグ(`mineable/pickaxe`、`walls`等)への登録漏れに注意する(過去に2回発生済み)。

* **コード・ファイル編集の制約**
  * lang(en_us.json/ja_jp.json)等の整形済みJSONを部分編集する際は、`json.load`+`json.dump`による全体再整形をしない。既存エントリの直後に新規行を文字列置換で挿入する方式を使う。
  * このセッション環境のRead/Write/Editツールはリポジトリのgit作業ディレクトリ(Linuxサンドボックス内パス)に直接使えない(Windows側マウントパスしか読み書きできない)。ファイル編集は`mcp__workspace__bash`経由のpython/sed/catで行い、画像確認だけはWindows側マウントの作業フォルダ(outputs)にコピーしてから`Read`ツールで開く。
  * 外部API・Minecraft本体の未確認仕様を調べる際は`WebSearch`/`mcp__workspace__web_fetch`を積極的に使う。`minecraft.wiki`・`mappings.dev`(1.20.1 mojmap javadoc、フィールド一覧の確認に有効)・`raw.githubusercontent.com/InventivetalentDev/minecraft-assets`(バージョン別ブランチのvanilla資産そのもの、モデルJSON等の実物確認に有効、2026-09-01発見)には到達できる(bashの`curl`は`api.github.com`等の主要ホストが到達不可)。それでも足りなければ`mcp__Claude_Browser__*`で直接リポジトリ・公式mavenを読む。

* **未確認のJava API(特に@Override)は必ず出典を確認してから使う(2026-09-01追記・重要な教訓)**
  * v0.37.0の初回pushで、`PrismiumLanternBlock`に`@Override public boolean canPlace(BlockPlaceContext context)`を実装したところ、実際には1.20.1の`Block`クラスにそのようなオーバーライド可能なメソッドが存在せず(`method does not override or implement a method from a supertype`)、3回連続でビルド失敗・リリース失敗する事態になった。「vanillaのBlockItemが内部でこういうチェックをしているはず」という推測だけでコードを書いてしまったことが原因。
  * 教訓: `@Override`を付けるメソッドは、既存のこのMOD内の別クラス(例: `PrismiumSnareBlock`/`PrismBrambleBlock`)で実際にコンパイルが通っている実例をコピーするか、`mappings.dev/<version>/<パッケージ>/<クラス>.html`でそのクラスの実際のメソッド一覧を確認してから使うこと。特に「こういうフックがあったら便利そうだ」という願望ベースで存在を仮定しないこと。座標・シェイプ等の具体的な値が要る場合は`raw.githubusercontent.com/InventivetalentDev/minecraft-assets`(本ファイル内の別項目参照)、メソッド・フィールドの存在確認には`mappings.dev`、が使える。

* **外部MOD連携**
  * Mixinベースの外部依存MOD(Curios等)を`compileOnly`/`runtimeOnly`で追加する場合、CIの`runGameTestServer`で実際にロードされクラッシュしうる。`build.gradle`の該当runブロックに`mixin.env.remapRefMap` / `mixin.env.refMapRemappingFile`の設定が必要になる場合がある。
  * 外部MODのスロット/インベントリ拡張機能に対応する際は、「スロット種別への登録・タグ付け」と「エンティティへのスロット配布」が別々の必須ステップであることを確認する(Curios対応で一度見落とし、実機テストで発覚し後日修正した実例あり)。

* **GUI/Menu実装**
  * このMODのGUI付きブロック(Menuクラスを持つもの)は、クライアント側のMenuコンストラクタで「その場にある本物のブロックエンティティのContainerData/インベントリをそのまま使い回す」実装にしないこと。Forge公式ドキュメント(https://docs.minecraftforge.net/en/1.20.1/gui/menus/)が明記する通り、クライアント側は常に新規のダミー`SimpleContainerData`/空の`ItemStackHandler`を使うのが正しい実装であり、本物のインスタンスを使い回すと(このMODのContainerDataは`set()`を意図的にno-opにしているため)サーバーからの同期パケットの値が握りつぶされ、GUIのバー表示が固まって見える(v0.31.2で発覚・修正、詳細はPROGRESS_ARCHIVE.md参照)。
  * ContainerDataの`get()`はサーバー側で常に生きているフィールドを直接読む実装で問題ないが、`set()`は「クライアント側が同期パケットを受け取った時にしか呼ばれない」ことを忘れないこと。

* **ドキュメント運用(このファイル自体の扱い)**
  * セッションがPROGRESS.md更新前に終了する可能性がある。作業開始時、直近リリースタグ以降のコミットを`git log`で確認し、記録漏れが無いかチェックする。あれば遡って記録する。
  * `PROGRESS_ARCHIVE.md`に全セッションの詳細な実装ログがある。経緯を詳しく調べたい場合はそちらを参照すること。
  * 「今どんな機能が実装済みか」をざっと把握したい場合は`RELEASE_NOTES.md`を確認する手もある(バージョンごとの追加内容がまとまっている)。
  * ディメンション・worldgenなど「今どうなっているか」が重要な話題では、PROGRESS.mdの記述を鵜呑みにせず実際のデータパックJSON・Javaソースを確認してから発言・実装すること(2026-08-30、こんぺいとう氏の指摘で発覚した実例あり、詳細はPROGRESS_ARCHIVE.md参照)。

---

## 2. TODO(優先度順、上ほど優先度が高い)

1. **【v0.34.0で修正・実機確認待ち】プリズミウム・コンペンディウムの右クリックが正しく本を開くか。** `PrismiumCompendiumItem`が自前で`BookViewScreen`を開くよう修正済み。実機で実際に開くか、11ページの内容が正しく表示・ページ送りできるかの確認が必要。
2. **【v0.34.1で本格対応・実機確認待ち】JEIのレシピ画面上でのWキー長押し詳細表示。** JEI本体のPlugin API連携(`JeiCompat`/`ClaudeModJeiPlugin`)に置き換え済み。実機で各種JEI画面での表示内容・マウス座標変換の正確性、JEI未導入環境での動作の確認が必要。
3. **【v0.35.0で対応・実機確認待ち】リフト・シャード / リフト・アンカーの設計整理。** 説明文の修正、レシピ変更(エンダーアイ+コンパス+プリズミウムの欠片x3)、リフトシャードのクールダウンを60秒に変更済み。実機での新レシピクラフト・クールダウン体感・説明文表示の確認が必要。
4. **【v0.34.0で一部対応・実機確認待ち】FEエネルギーシステムのバランス崩壊。** パイロン/修復機/結界石・粉砕機・精錬機・圧縮機の6ブロックで直接充電廃止・最大蓄電量引き下げを実施済み。実機でケーブル経由充電の動作・案内メッセージ表示・ブートストラップ難易度の確認が必要。
5. **【v0.35.0で対応・実機確認待ち】発電機(Generator)の燃料インベントリを改善。** 燃料スロット1→4・プレイヤーインベントリ表示・shift-click対応済み。実機でのスロット表示・クリック動作・GUIレイアウトの確認が必要。
6. **【最重要バグ・未解決】発電→ケーブル→消費ブロックのFE移動アルゴリズムが直感に反する二段階の挙動をしている、という報告(2026-08-31)。** `EnergyPushHelper`・各ブロックエンティティのserverTickを複数セッションで精読したが、単一の発電機→(ケーブル)→単一の消費ブロックという構成で保存則違反や二段階動作の明確な原因はコード上見つけられていない。次回は、(a) 実際にテストで使ったのがPylon/Restorer/Wardstoneのどれか、(b) ケーブル網に複数の消費ブロック/発電機が絡んでいなかったか、(c) 発電側・消費側のGUIを本当に同時に(あるいはごく短い間隔で)見比べたのか、を具体的に確認した上で再調査すること。これはこんぺいとう氏本人からの再現条件の追加情報が無いと自動セッションだけでは前進しづらい。
7. **【v0.31.2で修正・実機未確認】全GUIブロック(Cell/Generator/Pylon/Restorer/Wardstone/Pulverizer/Smelter/Compressor)の画面固まりバグの実機確認がまだ取れていない。**
8. **【v0.38.0で新規・実機確認待ち】Prism Realmの陸地(v0.38.0で実装したPrismiumLandFeature)の見た目確認・チューニング。** 低周波ノイズで陸地/海を判定(閾値LAND_THRESHOLD=0.28、最大高さMAX_LAND_HEIGHT=6)しているが、実際に生成される陸地/海の比率・陸地の形状(島っぽいか大陸っぽいか)・起伏の自然さは未検証。偏りすぎ(陸地だらけ/海だらけ)であれば`PrismiumLandFeature`の定数を調整する。
9. 上記8(陸地の実機確認)の後に、各バイオームに固有のボスを伴うダンジョンが低確率で生成される仕組みを実装する(山岳バイオーム版から着手、平原等へ横展開)。陸地自体はv0.38.0で追加済みのため着手は可能になったが、地形の見た目が実際どう調整されるか分からないうちにダンジョン配置ロジックを組むのは手戻りのリスクがあるため、8の確認を待つのが望ましい。
10. **【v0.39.0で新規・実機確認待ち】プリズミウム・ウィスプ(飛行アンビエントMOB、TODO「MOBのカテゴリ拡充」に対応)の実機確認・チューニング。** `FlyingMoveControl`+`WaterAvoidingRandomFlyingGoal`によるこのMOD初の飛行AI、SquidModel流用の見た目、Prism Realmでの自然スポーン頻度(weight=10)が狙い通りか確認が必要。引っかかる・変な高さに行く等の飛行の不自然さがあれば`PrismiumWispEntity`のAI構成を調整する。MOBのカテゴリ拡充は「戦闘」「水中非戦闘」「地上アンビエント」「飛行アンビエント」の4種類まで到達、残るアイデアは使い魔的MOB(プレイヤーに追従する非戦闘MOB等)。
11. GameTestフレームワークで、ContainerDataの同期を自動検証する仕組みを追加する(CIビルド成功だけでは検知できないGUI関連バグの再発防止)。
12. **【v0.36.0で対応・実機確認待ち】Issue #21(JEI互換性、レシピ表示の話)。** `ClaudeModJeiPlugin`に機械レシピカテゴリ・鉱石高度情報を追加済み。実機でレシピカテゴリの表示・機能・遷移・情報ページ表示・JEIバージョン互換性の確認が必要。
13. プリズミウム・コンペンディウム(TODO1)の、紛失時の再入手レシピ(バニラの本+プリズミウムの欠片等)。専用NBTを持つカスタムレシピの実装が要る。
14. コンペンディウムの内容拡充(現状11ページの概要のみ)。各エネルギー機械の詳細な配線図解、Prism Realmのダンジョン/ボス実装(TODO9)が進んだらその案内ページ追加など。
15. **【v0.37.0で新規・要検討】Prismium Lantern/Pale Prismium Lanternの吊り下げ支持判定は、現状「床/天井が平らな面(isFaceSturdy)であること」のみに対応した簡略版。** vanilla本家のLanternBlockはフェンス・壁・鉄格子・トラップドア・チェーンからの特殊な吊り下げ/据え置きにも対応しているが、今回は未検証な特殊分岐を増やすリスクを避けて実装していない。実機確認(TODO確認後)を踏まえて、需要があれば拡張を検討する。

**朗報**: Issue #18(CuriosAPI対応)はこんぺいとう氏の実機確認で完了済み、ISSUES_TO_CLOSE.jsonからも消化済み(空を確認済み)。プリズミウム・クロノフレイムのUI(v0.33.0)も「素晴らしい、えらい」と高評価済みで対応完了。**Issue #17(羽石の効果がわかりずらい)も2026-09-01時点でこんぺいとう氏によりCLOSED(stateReason: COMPLETED)を確認済み** - v0.36.0のHUDパネル方式(TODO1だった項目)で最終的に解決した模様。

## 3. 問題点(既知の不具合・未検証事項)

- **【v0.37.0で対応・CIビルド成功確認済み(2026-09-01追記)・実機未検証】** Prismium Lantern/Pale Prismium Lanternをcube_allの立方体から、HANGING/WATERLOGGED状態を持つ正式な吊りランタン形状(`PrismiumLanternBlock`)に作り直した(TODO15参照)。**初回実装には実在しないAPIをoverrideするミスがあり3回ビルド失敗していたが、該当箇所(`canPlace(BlockPlaceContext)`)を削除して修正済み、CIのビルド(build-and-notify/release)・データパック検証とも成功を確認済み(commit b6e9464、上記「約束や決まり事」参照)。** 当たり判定・モデル形状はMojang公式`template_lantern`/`template_hanging_lantern`の座標をそのまま採用しているため寸法自体の誤りは無いはずだが、(a) 設置時の吊り下げ/据え置き判定が実際に狙い通り動くか、(b) 当たり判定の感触、(c) 新しいUVアンラップに合わせて描き直したテクスチャーが実際に3D形状へ正しく貼り付くか(サンドボックスでは3Dレンダリングを目視できないため、UV領域を切り出して並べた確認画像でのみレビュー済み)、実機での確認が必要。
- **【v0.37.0で対応・実機未検証】** ケーブルのエネルギーフロー視覚化を、ランダム位置への火花散布から、ケーブル経路に沿って移動する単一パルス(先頭にELECTRIC_SPARK、後方数マスにGLOWの尾)のアニメーションに変更した(v0.37.0で対応、対応するTODO項目は完了により削除済み)。パルスの移動速度(`PULSE_STEP_TICKS`)・トレイル長(`PULSE_TRAIL_LENGTH`)が体感として適切かは実機確認が必要。
- **【v0.37.0で対応・実機未検証】** Prism Realmの海底に2Dフラクタルノイズで高低差(最大2〜3ブロック程度)を追加した(v0.37.0で対応、対応するTODO項目は完了により削除済み)。振幅・地形の見た目が自然に見えるかは実機確認が必要。underground_oresより前のraw_generationステップで実行しているため、海底を掘り下げた一部コラムでは鉱石が置かれなくなる(水没した場所に鉱石は出ない、という自然な副作用)。
- **【v0.38.0で対応・CIビルド成功確認済み・実機未検証】** Prism Realmに陸地(平原地形)を`PrismiumLandFeature`で追加した(TODO8参照)。低周波ノイズで陸地/海を判定し、海面上最大6ブロックの高低差+起伏、表層3ブロックがprismium_soil・それ以外prismium_stoneの構成。build-and-notify #295・Release #47(v0.38.0)ともStatus Successを確認済み、鉱石生成検証(`builds/last_ore_verification.txt`)でもプリズミウム鉱石は引き続き検出されている(この機能によるy=41以上の書き換えがy=40以下のore配置と干渉していないことの傍証)。ただし陸地/海の比率・地形の形状・起伏の自然さは完全に未検証。
- **【v0.39.0で対応・CIビルド成功確認済み・実機未検証】** プリズミウム・ウィスプ(6体目のMOB、初の飛行アンビエント、TODO10参照)を追加した。`FlyingMoveControl`+`FlyingPathNavigation`+`WaterAvoidingRandomFlyingGoal`によるこのMOD初の飛行AI構成で、build-and-notify #297・#298ともStatus Successを確認済み。ただし自然スポーン頻度・飛行の自然さ・SquidModel流用の見た目バランスは完全に未検証。
- **【v0.34.0で修正・実機未検証】** プリズミウム・コンペンディウム(v0.32.0)が右クリックで開けない不具合(issue #7関連)。`PrismiumCompendiumItem`で自前オープンに変更し修正したが、実機で本当に開くかは次回確認が必要(TODO1参照)。issue #7は、この修正の実機確認が取れるまで「未解決」扱いのまま据え置く。
- **【v0.34.1で本格対応・実機未検証】** JEIのレシピ画面上でWキー長押しの詳細表示オーバーレイの表示内容(TODO2参照)。
- **【v0.34.0で一部対応・実機未検証】** FEエネルギーシステムのバランス崩壊(TODO4参照)。
- **【最重要バグ・未解決・再調査要】** 発電→ケーブル→消費ブロックのFE移動アルゴリズムが直感に反する二段階の挙動をしている、という2026-08-31の報告(TODO6参照)。次回はまず具体的な再現条件(使用ブロックの種類、ネットワークの構成、観測方法)を確認すること。
- Issue #15(電力バグ)・#21(JEI互換性)は引き続きOPEN(2026-09-01に`is:issue`全件再確認済み、新規issueは無し)。**Issue #17(羽石)は2026-09-01にこんぺいとう氏によりCLOSED(COMPLETED)を確認済み、以後は対応不要。** #21はコード対応はv0.36.0で完了したが実機確認がまだ取れていない(TODO12参照)。#15はFEバランス部分は一部対応したが、TODO6の移動アルゴリズム自体の再調査はまだ残っている。
- **大前提**: このサンドボックスは実機(ゲームクライアント)を起動できないため、MOD内のほぼ全コンテンツが「CIビルドが通ること」以上の検証が一切できていない。具体的にはバランス数値、装着時テクスチャー・インベントリ表示・GUI表示、FE配電経路が実際に繋がって動くか、全MOBの自然スポーン頻度・AI挙動、worldgen装飾ブロックの生成、全GUIの表示崩れ、サウンド/パーティクル演出のタイミングと音量感が丸ごと未検証。新しいコンテンツを追加するたびに、この一般則で説明できる「未検証」を個別に書き足す必要はない。
- `ArmorSetBonusHandler`は`TickEvent.PlayerTickEvent`を毎tick・全プレイヤー分処理する。サーバー側限定ガードはあるが、プレイヤー数が多いサーバーでの負荷は未計測。
- `ResourceLocation`/`FMLJavaModLoadingContext`の非推奨API警告は1.20.1では有効な置き換え先が無い(1.21系に上げない限り対応不能、今後このタスクを申し送りに書かないこと)。
- **【v0.35.0で対応・実機未検証】** リフト・シャード/リフト・アンカーの説明・レシピ・効果の一貫性の無さ(TODO3参照)。
- **【v0.35.0で対応・実機未検証】** Generatorの燃料スロットが1つのみだった問題(TODO5参照)。

## 4. その他

**使えるもの**: JDK 21がプリインストール(JDK 17は`apt-get install openjdk-17-jdk-headless`で追加可能)。システムGradle 8.14.3が`/opt/gradle`にプリインストール済み。Python3 + Pillowはテクスチャ生成に使用可能。`github.com`(`api.github.com`は不可)、`raw.githubusercontent.com`は到達可能。1.20.1のForge javadocは1.18.2版のミラー(`nekoyue.github.io`)経由で代替確認可能(`BookViewScreen`等のvanilla GUIクラスは1.18〜1.20.1でほぼ変化がない)。**2026-09-01発見**: `raw.githubusercontent.com/InventivetalentDev/minecraft-assets/<version>/assets/minecraft/...`で、バージョン別ブランチのvanilla資産(モデルJSON・blockstates JSON等)そのものを直接取得できる。ブロック形状・UVレイアウトなど「vanillaの実際の値」を推測せずに確認したい場面(今回は`template_lantern.json`/`template_hanging_lantern.json`/`lantern.json`/`blockstates/lantern.json`で活用)で非常に有効。`mappings.dev/<version>/...`はフィールド一覧(例: `ParticleTypes`に`GLOW`が実在するか、`BlockStateProperties`に`HANGING`が実在するか)の確認に有効(ただしフィールドの初期値=具体的な座標などは載っていないので、座標が必要なら上記のminecraft-assetsミラーを使うこと)。

**議論したい論点・改善案**:
- Pulse Charm(右クリックで能動的に使うアイテム)は意図的にCuriosのcharmスロット対象外にしたままだが、この判断への同意が得られるか、あるいは別のアプローチを検討すべきか。
- ポータルフレームの専用ブロック化・6個セット化案。要望があれば着手を検討。
- Prismium Ingot/Alloy Ingotのスミシングアップグレード経路の再検討。
- `PROGRESS_ARCHIVE.md`への分離運用が今後も機能するか、数セッション後に振り返りたい。
- JEIのプラグインAPI(`compileOnly`/`runtimeOnly`依存、`jei_version=15.56.0.204`)を使った`com.claudemod.compat.jei`パッケージ実装済み。JEIの`1.20.1`ブランチと素の`1.20`ブランチでAPI形状が異なる(`1.20`は既に1.20.5+の新しいAPIになっている)ので、今後もJEI API確認時は必ず`1.20.1`ブランチを明示的に指定すること。
- このMOD初のカスタムネットワークチャンネル`com.claudemod.network.ClaudeModNetwork`(`SimpleChannel`、`FeatherstoneReductionMessage`)。今後サーバー専用の計算結果をクライアントのHUD/演出に反映したい場面があれば、ゼロから設計するのではなくこのチャンネルにメッセージ種別を追加していく形にすること。
- **2026-09-01追記**: vanilla標準ブロック(Lantern)のモデル・座標をそのまま流用する手法が有効だと判明したので、今後「vanillaの何かに似た形状にしたいが座標が分からず着手を避けていた」既存TODO(例: 将来的なポータルフレームの専用ブロック化案など)があれば、同じ手法(`minecraft-assets`ミラーでテンプレートJSONを確認→parent流用)を検討する価値がある。

**通知状況**: Discord Webhookはサンドボックスから到達不可のため試みていない。GitHub Actions側(`build-and-notify.yml`・`release.yml`)がpush/タグに対応する通知を送信する(Secret設定済み前提)。

## 5. MOD構想・ロードマップ

「てんこ盛り」コンテンツMODとして、以下の柱を段階的に育てていく。優先順位や詳細は毎回のセッションで見直してよい。「完成」を目指さず、常に肉付けし続ける。各要素は最初は最小実装で入れて、後のセッションで機能・バランス・ビジュアルを磨き込む前提。

1. **新資源・素材ライン**: Prismium(プリズミウム) — 最初の資源で、装備・エネルギー・ディメンションの共通テーマ素材。派生として「蒼白のプリズミウム」(Pale Prismium)ファミリーも展開中(装飾ブロック・建築バリエーション・ランタン)。
2. **新エネルギーシステム**: 「Prismium Energy」。FE(Forge Energy)ベース。発電機(Generator)・蓄電ブロック(Cell)・3機械(Pulverizer/Smelter/Compressor)・ケーブル(離れたブロック間の中継、ネットワーク一括送電・移動パルスの視覚化まで実装済み)まで実装済み。GUI連携・複数ブロックにまたがる大規模送電網はさらに拡張の余地がある。
3. **新ディメンション**: 「Prism Realm」。`minecraft:flat`ジェネレータによる水没ワールド(専用バイオーム`claudemod:prism_realm`、深層岩/石境界・海底の高低差・v0.38.0で追加した陸地(平原地形)は、いずれも自作Perlin/Fractalノイズを使った事後上書きFeatureでまだら化・生成済み)、テレポート用アイテム(Prismium Rift Shard)まで実装済み。プリズミウム鉱石は既にオーバーワールドと共通の鉱石として生成される(Realm側は生成率を通常より高くブースト済み)。陸地は追加されたが実機未確認で比率・見た目のチューニングが要る可能性があり、Realm専用の鉱石種・本格的なポータルブロックもまだ無い。将来的には各バイオームに固有ボスを伴うダンジョンがまれに生成される仕組みを構想中(山岳地帯版から着手予定、TODO参照)。
4. **新MOB**: Prismium Wraith/Deep Wraith(戦闘)、Sentinel(戦闘)、Drifter(水中非戦闘)、Crawler(地上アンビエント)、Wisp(飛行アンビエント、6体目、v0.39.0)の6体。ボス級はまだ無いが、各バイオーム固有のダンジョンボスとして今後複数体追加予定(TODO参照)。カテゴリは「戦闘」「水中非戦闘」「地上アンビエント」「飛行アンビエント」の4種類に到達、残る拡充アイデアは使い魔的MOB。
5. **新装備**: ツール5種・アーマー4種(セット効果: 暗視+水中呼吸)、グラップリングフック、探知アイテム(Locator)、Shield、Bow、Guardian Charm(cheat-death)、Featherstone/Emberguard/Vitastone/Magnet Charm(完全パッシブ系)まで実装済み。一部はCuriosAPI対応済み(Issue #18)。
6. **新ブロック/ギミック**: Prismium Core、Prismium Lantern(v0.37.0でvanilla同等の正式な吊り下げ/据え置き形状に刷新)、蒼白のプリズミウムブロック・ランタン・建築バリエーション、Prismium Snare(罠ギミック)まで実装済み。装飾ブロック・ダンジョン用ギミックブロックはさらに拡充の余地がある。
7. **プレイヤー向けUX/ドキュメンテーション**: issue #7(MODについて何も分からない)への対応として、各エネルギーブロックの使い方ツールチップ、詳細表示オーバーレイ(W長押し)、プリズミウム・コンペンディウム(初回配布されるガイドブック)まで実装済み。今後も新しい系統(Prism Realmのダンジョン/ボス等)を追加するたびに、この系統のドキュメントも一緒に更新していく。
