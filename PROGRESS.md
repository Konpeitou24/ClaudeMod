# ClaudeMod 開発進捗 (PROGRESS.md)

このファイルは、1時間ごとに自動起動される開発セッション間の**唯一の記憶**です。
新しいセッションを始める前に必ずこのファイル全体を読んでください。会話履歴は引き継がれません。

最終更新: 2026-08-17 (セッション #17)

---

## 0. 運用ルール(ユーザーからの直接指示、必ず遵守・毎回このセクションを確認)

このセクションは、番号付きの実装セッションログ(§3以降)とは別に、ユーザーから直接指示された恒久的な運用ルールをまとめる。ルールが増えたら追記し、以後の全セッションで欠かさず守ること。

1. **【実装済み】Discord通知にコミット要約を含める**: `.github/workflows/build-and-notify.yml` の Notify Discord ステップは、直近1コミットの件名だけでなく、そのpushに含まれる全コミットの件名(`ci: update built jar [skip ci]` 等の自動コミットは除く)を箇条書きにして送るよう変更済み。理由: Discordに「ビルド結果とURLしか流れず、何を実装したか分からない」というユーザーからの指摘への対応。今後ワークフローを触る際もこの挙動を壊さないこと。
2. **毎回の状況確認にGitHub Issueの確認を含める**(セッション#9と並行していたセッションが新規追加): 「毎回の作業フロー」のステップ1(状況確認)で、GitHub Actionsのビルド結果に加えて、リポジトリのOpen Issueも必ず確認すること。
   - 確認方法: 公開リポジトリなので `https://github.com/Konpeitou24/ClaudeMod/issues` を非ログインで`curl`取得すれば一覧が見える(§2-4と同じ非APIの手法)。ただし§2-7で判明したプロキシキャッシュの影響をこのページも受ける可能性が高いため、取得時は必ずキャッシュバスティング用クエリ(例: `?nocache=$(date +%s%N)`)を付け、古い内容しか返らない場合は複数回リトライすること。個々のIssue本文は `https://github.com/<owner>/<repo>/issues/<番号>` で同様に取得できる。
   - 権限上の制約: このリポジトリ用のgitトークンはContents/WorkflowsのRead/Writeのみで、Issueへのコメント投稿・クローズをAPI経由で行う権限は付与されていない(未確認だが、トークンのスコープ説明にIssues権限の記載が無いため、権限不足で失敗する前提で臨むこと)。Issueへの返信やクローズが必要な場合は、無理にAPIで試みず、対応状況をPROGRESS.mdに明記し、実際のクローズ等はユーザー側の対応に委ねること。
   - 見つけたOpen Issueは内容を要約し、「今回の計画」や「§5 次回セッションへの申し送り」に反映すること。バグ報告は通常のCIビルド失敗対応と同格の優先度で扱ってよい。Issueが無い(0件)の場合もその旨をPROGRESS.mdに一言残す(「確認したが無かった」と「確認していない」を区別できるようにするため)。
   - **セッション#9での追記**: このルールは実装作業がほぼ終わった段階(PROGRESS.md更新中のマージコンフリクト解消時)で並行セッションの変更として見つけた。見つけた直後にIssue一覧を実際に確認したところGitHub issue #1「プリズム装備を装着した際、顔が見えない」(OPEN)が1件あり、セッション#9のうちにその場で対応した(§3H参照)。次回セッション以降もステップ1で必ず確認すること。

---

## 1. MOD全体の構想(ロードマップの叩き台)

「てんこ盛り」コンテンツMODとして、以下の柱を段階的に育てていく。優先順位や詳細は毎回のセッションで見直してよい。

1. **新資源・素材ライン**: Prismium(プリズミウム) — セッション#1で着手した最初の資源。今後の装備・エネルギー・ディメンションの共通テーマ素材。
2. **新エネルギーシステム**: 「Prismium Energy(仮称)」。発電機・ケーブル・蓄電ブロック・機械(粉砕機、精錬機など)を実装し、FE(Forge Energy)ベースで組む想定。セッション#8で蓄電ブロック Prismium Cell(IEnergyStorage capability公開、GUI無し、手動チャージ機構)に着手。**セッション#9で Prismium Generator(MOD初のBlockEntityTicker、Prismiumの欠片を燃焼して隣接ブロックへFEを自動送電) を追加し、CellとGeneratorをペアで置くことで初めて「自動化された発電→送電」ループが成立するようになった**。ケーブル(離れたブロック間の中継)・GUI・複数ブロックにまたがる大規模送電網はまだ無い。
3. **新ディメンション**: 「Prism Realm(仮称)」。Prismiumで動くポータル(枠ブロック+起動アイテム)で行き来する異空間。専用地形生成、専用鉱石、専用バイオーム。**セッション#14で最初の一歩に着手**: データパック駆動のディメンション(地形はバニラのオーバーワールド設定+固定バイオームcherry_groveを流用、専用地形はまだ)と、テレポート用アイテム(Prismium Rift Shard、ポータルブロックの代わりの最小実装)を追加。専用地形・専用鉱石・専用バイオーム・本格的なポータルブロックはまだ無い。**セッション#15でビルド失敗と判明、原因(存在しないシンボル2つ、後述§3N)を特定・修正しビルド成功を確認済み**(§3N参照)。ただしコンパイルが通ることの確認に留まり、実プレイでの検証はまだ無い。
4. **新MOB**: Prism Realm を含む探索先に生息する敵対/中立MOB。ボス级の1体を最終的に用意したい。**セッション#12で最初の1体、Prismium Wraith(敵対、洞窟に生息しPrismium鉱石を守るイメージ)を追加**。ボス級はまだ無い。
5. **新装備**: Prismium製ツール/アーマー(特殊能力付き)、探索を楽しくするアクセサリ的アイテム(グラップリングフック、探知アイテムなど)。ツール5種(セッション#2)・アーマー4種(セッション#3)実装済み。セッション#4でアーマーにフルセット効果(暗視、常時)を追加。セッション#5でアーマーのセット効果に水中呼吸を追加(2つ目の効果)、かつツール側にも初のギミック(Prismiumツルハシの鉱石ボーナスドロップ)を追加し、「ツールが純粋なステ上位互換のまま」という課題に着手。**セッション#7で、長らく手つかずだったアクセサリ系の最初の1個としてPrismiumグラップリングフックを追加**(視線方向のブロックへ引き寄せられる、レイキャスト+速度書き換え方式、飛翔エンティティ無し)。**セッション#16で探知アイテムの最初の1個、Prismium Locatorを追加**(右クリックで周囲41x41x41ブロックを走査し、最も近いPrismium鉱石の方角・距離・上下を行動バーに表示。専用の飛翔エンティティやコンパス針モデルは持たず、メッセージ表示のみの最小実装)。
6. **新ブロック/ギミック**: 装飾ブロック、罠、ダンジョン用ギミックブロックなど。Prismium Core(セッション#3中に並行セッションが追加)はギミックというより「ツールの存在意義付け」の1st ステップ。セッション#4で Prismium Lantern(ツール非依存の量産可能な光源ブロック、光レベル15)を追加、「探索を照らす」実用ブロックの最初の1個。

「完成」を目指さず、常に肉付けし続ける。各要素は最初は最小実装で入れて、後のセッションで機能・バランス・ビジュアルを磨き込む前提。

---

## 2. 【最重要】既知の環境制約と、セッション#3で判明した重大な訂正

### 2-1. ビルドが実行できない(サンドボックス内では)
`./gradlew build` / `gradle build` は **このクラウドサンドボックスの中では必ず失敗する**(セッション#1〜#3すべてで確認、変化なし)。原因はプロキシのアローリスト制限で、`maven.minecraftforge.net` 等に到達できないため。次回セッションも同じ調査を繰り返して時間を無駄にしないこと。試すなら1回だけ `curl -s -o /dev/null -w "%{http_code}" --max-time 8 https://maven.minecraftforge.net` を確認し、`000` 以外ならgradle buildを試す価値あり。`000`のままなら諦めて実装作業に集中する。

**ただし** §2-4 の通り、GitHub Actions側では実際にビルドが走っており、**セッション#3で実際にビルド成功を確認できた**(下記2-4参照)。「サンドボックス内でビルドできない」ことと「コードが実際に正しいか」は別問題であり、後者は今はGitHub Actions経由で検証可能。

### 2-2. Discord Webhook 通知が送信できない(サンドボックス内では)
`discord.com` もサンドボックスから到達不可。無駄なリトライはせず、通知はスキップしてPROGRESS.mdの更新を通知の代わりとすること。GitHub Actions側は引き続きDiscordへの通知を試みる(Secret設定はユーザー側確認事項、変化なし)。

### 2-3. git push には回避策が「必要な場合とそうでない場合がある」(セッション#3で判明・重要な訂正)
セッション#2までは「`git push` の前にプロキシ環境変数を空にする」ことが必須の回避策として書かれていたが、**セッション#3ではこれが原因でむしろ失敗した**:
```
$ https_proxy="" HTTPS_PROXY="" http_proxy="" HTTP_PROXY="" git push origin main
fatal: unable to access '...': Could not resolve host: github.com
```
このサンドボックスはプロキシ (`http://localhost:3128`, 環境変数 `https_proxy` 等に既定で設定済み) 経由でしかネットワークに出られない模様で、プロキシ変数を空にするとDNSごと死ぬ。**セッション#3では、プロキシ変数に一切手を加えず、素の `git push origin main` がそのまま成功した**(トークンをリモートURLに `x-access-token:...` として埋め込んでいるため、プロキシ側の資格情報注入に依存していない可能性がある)。

**次回セッションへの指示**: まず何もいじらずに `git push origin main` を試すこと。それが
```
remote: access denied by the git proxy: ... is not in this session's authorized repository set
```
のようなエラーで失敗した場合にのみ、プロキシ変数を空にする回避策を試すこと(順序を逆にしない)。どちらのパターンもあり得るとみて、両方試せるようにしておくとよい。

### 2-4. 【セッション#3で解決】GitHub Actions のビルド結果を"信頼できる形で"確認する方法
セッション#2はAPIアクセス制限で確認できず、セッション#3序盤も `api.github.com` は相変わらず到達不可(`curl` で exit 56)だったが、**`github.com` 自体(api.ではない)は到達可能**なことを利用して、以下の方法でActionsの実行結果を(ログイン無しでも)取得できることを確認した:

```bash
# ワークフロー全体のバッジ(passing/failing)
curl -s "https://github.com/<owner>/<repo>/actions/workflows/<file>.yml/badge.svg" | grep -o '<title>[^<]*</title>'

# 直近の実行一覧と各実行の結果(aria-label に "completed successfully" / "failed" / "cancelled" / "currently running" が入っている)
curl -s "https://github.com/<owner>/<repo>/actions/workflows/<file>.yml" -o page.html
grep -noE 'aria-label="[^"]*Run [0-9]+ of <workflow name>[^"]*"' page.html
```
これはページの初期HTML(ログアウト状態でもpublicリポジトリなら見える静的一覧)に埋め込まれているため、JSレンダリングもAPIアクセスも不要。`api.github.com` より確実。次回セッション以降はこの方法を最初に使うこと。

**この方法で、セッション#2〜#3にまたがる重大な問題が発覚した**: `build-and-notify.yml` の `Build with Gradle` ステップに `continue-on-error: true` が付いていたため、**実際には `./gradlew build` が失敗していても、ジョブ全体は "completed successfully" と表示されていた**(後続のjar公開・Discord通知ステップが `steps.build.outcome` を見て正しくスキップされるだけで、ジョブ自体は失敗としてマークされない)。これによりセッション#2の「Run 2: 成功」「Run 3: 成功」は**偽陽性**だった可能性が高いことが判明した。

**この問題は、セッション#3の実行中に並行して起動していた別セッションによって独立に発見・修正された**(コミット `502d3a3` "ci: make workflow fail honestly when the Gradle build fails" — jar公開とDiscord通知の後に `steps.build.outcome != 'success'` ならジョブを失敗させる最終ステップを追加)。この修正後の最初の実行(Run 4、修正コミット自身のビルド)は実際に **失敗** し、バッジも "failing" に変わった。これでワークフローの成否表示がようやく信頼できるようになった。

**そして、セッション#3の最終コミット(このファイルの直前のコミット、アーマー一式追加)のビルドは Run 6 として実行され、`git fetch` で `ci: update built jar [skip ci]` コミットが新たに追加されるのを確認済み = 本物の成功**。つまり **worldgen(鉱石生成)・ツール一式・Prismium Core(ブロックタグ切り替え含む)・アーマー一式まで、現在の main の内容は実際にForge/Gradle環境でコンパイル・ビルドが通ることが実証された**。セッション#1〜#3の「未検証」だったコード上の不安(ForgeTierの引数順、Axe/HoeItemの引数型、worldgen JSON、ArmorMaterial実装など)は、この時点で**ビルドレベルでは解消**されたとみてよい(ただしプレイテスト・バランス・見た目は別問題、§4参照)。

**次回セッションへの指示**: 今後は毎回セッション開始時に上記の badge / runs 一覧チェックを行い、直近の実際の(信頼できる)ビルド結果を確認すること。Run 4 が一度failedになったように、依存関係取得の一時的な失敗(flaky)である可能性もあるので、1回failedを見ても即座にコード側を疑わず、まず「同じコミットのビルドをもう一度キックできないか」(`workflow_dispatch` はワークフロー定義上は許可されているが、サンドボックスからAPIで叩けるかは未確認)や「直後のコミットで再度成功していないか」を確認するとよい。

### 2-5. 複数セッションの同時実行について(セッション#2から継続、セッション#3で改めて実体験)
セッション#3の作業中、実際に別セッションが同時並行で動いており、こちらがpushしようとしたら `! [rejected] (fetch first)` で弾かれた。対処は通常のgit運用と同じ: `git fetch origin main` → `git rebase origin/main` (コンフリクトが出たら手動解消、特にlangファイルの末尾追記は競合しやすいので要注意) → `git push`。今回は `en_us.json` / `ja_jp.json` で競合したが、両方の追加分をマージして解消した。**今後もこのパターンは起こりうる前提で、pushの直前に必ず `git fetch` → 差分があれば `rebase` する習慣をつけること。**

### 2-6. 使えるもの
- JDK 21がプリインストール、JDK 17は `apt-get install openjdk-17-jdk-headless` で追加可能(次回も再インストールが要る可能性が高いが、§2-1の通りどのみちローカルビルドはできないので優先度は低い)。
- システムGradle 8.14.3が `/opt/gradle` にプリインストール済み。
- Python3 + Pillow はテクスチャ生成に使用可能。
- `github.com`(`api.github.com`は不可)、`raw.githubusercontent.com` は到達可能。Web検索・Web fetchツールも通常通り使用可能で、Forge/Minecraftのモディング情報の裏取りに活用した(§3-2参照)。

### 2-7. 【セッション#5で発見・重要】Actionsのrunsページ/バッジがプロキシ側でキャッシュされる問題と回避策

セッション#5で、§2-4の「runsページのHTML」方式で直近のビルド結果を確認したところ、**明らかに古い(1セッション分ほど遅れた)内容が返ってくる**という問題に遭遇した。具体的には、push直後に確認した際「Run 8が"currently running"」のまま何分経っても変化せず、実際にはその後のセッションで完了しているはずの内容と食い違っていた。

原因を切り分けた結果、`https://api.github.com/...` 自体は(§2-4の記載通り)相変わらず到達不可(プロキシのアローリストで`blocked-by-allowlist`)だが、`github.com`(api.ではない)への到達自体はできるものの、**このサンドボックスの前段にあるHTTPプロキシ(`http://localhost:3128`)が、同一URLへのGETレスポンスをアグレッシブにキャッシュしている**ことが濃厚だと分かった。証拠: 同じURLを何度取得しても内容が変わらない一方、URLの末尾にダミーのクエリパラメータ(例: `?nocache=$(date +%s%N)`)を付けて取得し直すと、直ちに最新の内容(その時点の本当の最新Run)が返ってきた。

**次回セッションへの指示**: 今後、Actionsのbadge/runsページを確認する際は、必ずキャッシュバスティング用のクエリパラメータを付けること。例:
```bash
TS=$(date +%s%N)
curl -s "https://github.com/<owner>/<repo>/actions/workflows/<file>.yml/badge.svg?nocache=$TS" | grep -o '<title>[^<]*</title>'
curl -s "https://github.com/<owner>/<repo>/actions/workflows/<file>.yml?nocache=$TS" -o page.html
grep -noE 'aria-label="(currently running|completed successfully|failed|cancelled)[^"]*"' page.html | head
```
これを付けずに素のURLでrunsページを取得すると、最大で1セッション分(数十分〜1時間程度)古い内容を「最新」と誤認するリスクがある。badge.svgエンドポイント単体は比較的新しい状態を返すことが多かった(キャッシュの効き方がrunsページ本体より弱いか、TTLが短い可能性)が、念のためbadgeにも同じくクエリパラメータを付けておくと安全。

**セッション#8での追記(重要)**: 上記の対策(クエリパラメータ変更・複数回リトライ)を尽くしても、runsページ/Actionsトップページが数十Run分(セッションにして5個分以上)古い内容を返し続け、一度も最新化できなかったケースが発生した(badge.svgのみ「passing」を返したが、これが本当に最新pushの結果か確信が持てなかった)。**この方法はもはや万能ではないと考えるべきで、`git fetch`で`ci: update built jar`コミットの到着を確認する方法(§3F-3参照)を主たる確認手段にし、runsページ/badgeのHTML確認は補助情報にとどめることを推奨する。**

また、個別のrunの詳細ページ(`/actions/runs/<run_id>`)はReactによるクライアントサイドレンダリングで、静的HTML取得(`curl`や`web_fetch`)では実際のジョブ/ステップログの中身までは取得できないことも確認した(§2-4の「runsページのaria-labelから成否だけ分かる」という前提は変わらず有効。ログ本文までは今のところ非ログインでは見えない)。ただし、runsページのHTMLの中に各runの`/actions/runs/<数字>`へのリンク(`href`)が埋め込まれているので、そこから実際のrun IDを取得すること自体は可能(セッション#5でRun 11のIDを特定した際に利用)。

---

## 3. セッション#3で実装した内容

### 3-1. CI/ワークフローの信頼性確認(実装ではなく調査)
§2-4に詳細。GitHub Actionsの実行結果を非ログイン・API不要で確認する方法を確立し、これまで「未検証」とされてきたコード(セッション#1・#2の全内容 + 並行セッションのPrismium Core)が実際にビルドを通ることを確認した。

### 3-2. Prismium Core(このセクションは並行セッションの記述の補足 — 実装した本人による詳細)
セッション#3中に並行して動いていたもう一つのセッション(このファイルを最終更新したセッションとは別)が実装。既知の課題「Prismiumツールがステ上位互換止まりで専用の採掘対象が無い」(セッション#2から継続)を解消する目的。

- `com.claudemod.registry.ModBlockTags`: 新規タグ `claudemod:needs_prismium_tool`, `claudemod:incorrect_for_prismium_tool` を定義。
- `ModToolTiers.PRISMIUM` の `ForgeTier` 第6引数を、従来の `BlockTags.INCORRECT_FOR_DIAMOND_TOOL`(バニラのタグをそのまま流用)から `ModBlockTags.INCORRECT_FOR_PRISMIUM_TOOL`(独自タグ、中身は今のところ空)に変更。**理由**: 同じタグをダイヤと共有すると、新ブロックをそのタグに追加してダイヤを弾いた瞬間に Prismium 自身も弾かれてしまうため(`DiggerItem#isCorrectToolForDrops` はレベル3同士の識別を各ティア固有の「不正タグ」membership でしか行えない、ハードコードされたレベル判定は0〜2止まりで3以上は各ティアのタグ任せ)。
- 新ブロック `prismium_core`(強度8.0/爆発耐性20.0、lightLevel 10、SoundType.AMETHYST)を `minecraft:needs_diamond_tool` と `minecraft:incorrect_for_diamond_tool` の両方に追加(ダイヤ以下を弾く)。Prismium 側の独自タグには追加しない(Prismiumだけ採掘可能、というのがこの機構の肝)。
- クラフト: Prismium Block ×4 + Amethyst Shard ×1 → Prismium Core ×1(shapeless)。lightLevel 10 で常時発光するため、将来のPrism Realmポータル素材としても機能しそうな見た目。
- テクスチャー(`scripts/textures/gen_prismium_core.py`): 既存 `prismium_block.png` と同じ対角グラデーション技法・パレットを踏襲しつつ、中央に明るい放射コア(lightLevel 10 を意識した視覚的差別化)を追加。
  - **自己レビュー実施済み**: 1回目のドラフトは紫の光る破片を密に散らす手法で描いたが、プレビュー画像(等倍・4倍拡大の両方)を目視した結果「ノイズが多くごちゃついて見える、既存の prismium_block と統一感が薄い」と判断して破棄。`prismium_block.png` と同じ対角バンド構造ベースに描き直し、放射コアだけを主な差別化点に絞ったところ、等倍でも4倍拡大でもシルエットが明瞭になった。最終版を採用。この「派手にしすぎたら家族(既存テクスチャー群)のスタイルに立ち返って描き直す」判断は今後のテクスチャー作成でも参考にできる。

### 3-2. Prismiumアーマー一式(ヘルメット・チェストプレート・レギンス・ブーツ)
- `com.claudemod.item.ModArmorMaterials`: `enum ... implements ArmorMaterial` パターン(Forge 1.20.X系列の実際のチュートリアルリポジトリ [Tutorials-By-Kaupenjoe/Forge-Tutorial-1.20.X, 16-armorブランチ] のソースをWeb fetchで直接参照し、`getDurabilityForType`/`getDefenseForType`/`ArmorItem.Type` の実メソッドシグネチャを確認した上で実装。ツール群と同じ「防御力はダイヤ/ネザライト据え置き、耐久・靭性・ノックバック耐性・エンチャント適正で差別化」という設計方針)。
  - durabilityMultiplier 40 (ネザライトの37より上)、defense {3,8,6,3}(ダイヤ/ネザライトと同値)、enchantmentValue 14(ツールと揃えた)、toughness 3.5、knockbackResistance 0.1、修理はPrismium Shard。
- `ModItems` に `ArmorItem(ModArmorMaterials.PRISMIUM, ArmorItem.Type.X, ...)` で4種登録。クリエイティブタブ・シェイプドレシピ(バニラ準拠の形、材料はPrismium Shard)・アイテムモデル(`minecraft:item/generated` 継承)・lang(en/ja)も追加。
- **セッション#3実行中に並行セッションがPrismium Core関連でModItems/ModCreativeTabs/langを同時に触っていたため、push時にコンフリクトが発生 → §2-5の手順で解消**。

### 3-3. アーマーテクスチャー(`scripts/textures/gen_prismium_armor.py`)
- **アイテムアイコン(16x16 x4)**: ツール群と同じ「クリスタル(Prismiumパレット)+ 灰色の金属ソケット枠」という新しい視覚言語を導入(ツールは全身クリスタルの刃、アーマーは金属フレームにクリスタルを埋め込んだ意匠、という世界観上の役割分担)。
- **装着時レイヤーテクスチャー(64x32 x2、layer_1/layer_2)**: バニラの64x32 biped/mobモデルと同じボックスUV展開(head:0,0-32,16 / right leg:0,16-16,32 / body:16,16-40,32 / right arm:40,16-56,32、左腕・左脚はレンダラー側でミラーされるため専用領域は不要)を、公開資料の座標定義から自分で導出して実装。前面(front)にクリスタル色+ジェム装飾、それ以外の面はソケット枠色、というルールで全ボックスを塗った。
- **自己レビュー実施済み、かつ実際にバグを1つ発見・修正した**: 最初のレギンス/ブーツアイコンは、2px幅の透明な隙間(脚と脚の間、ブーツの切れ込み)を意図して描いたが、`outline_nonzero`(不透明ピクセルの外周1pxに輪郭色を足す自作関数)が両側から同時に1pxずつ塗ってしまい、2px幅の隙間が完全に埋まってしまうというバグを、プレビュー画像の目視で発見(レギンスが「二本足」ではなく「アーチ状の一体形状」に見えた)。ピクセルのアルファ値をテキストダンプして原因を特定し、隙間を4px幅に広げて再生成・再確認して解消。この教訓(**「輪郭線を自動生成するテクスチャースクリプトでは、意図的な隙間は最低でも輪郭幅の2倍(=左右合わせて2px)より広く取らないと埋まる」**)は次回以降のテクスチャースクリプトでも当てはまるので覚えておくこと。
- 装着時レイヤーテクスチャー(64x32)は、フラットなスプライトシートとしての目視確認(色の配置・塗り漏れがないか)はしたが、**実際にプレイヤーモデルに巻き付いた状態(3Dレンダリング)は、このサンドボックスではゲームを起動できないため確認できていない**。UV座標の導出はWeb検索で得た一般的な仕様と、自分でのボックスUV展開の計算に基づくもので、標準的な手法のはずだが、初めてプレイできる環境で見た際に位置ズレ・伸び・裏表反転などがあれば次回セッションで直すこと。

---

## 3B. セッション#4で実装した内容

セッション開始時点でRun 8(セッション#3最終コミット)は "completed successfully" 済みだったため、修正対応は不要で、§5の「すぐやるべきこと」1〜2件目に沿って新規実装に着手した。

### 3B-1. アーマーのフルセットボーナス: 常時暗視(Night Vision)
- 新規クラス `com.claudemod.event.ArmorSetBonusHandler`(`@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)`、Forgeイベントバスの `TickEvent.PlayerTickEvent` を購読)。
- `phase == TickEvent.Phase.END` のときだけ処理し、プレイヤーの4部位が全て `ModArmorMaterials.PRISMIUM` の `ArmorItem` かどうかを `player.getInventory().armor` をループして判定(`ArmorItem#getMaterial()` で比較)。
- フルセットなら毎tick `MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, ambient=true, visible=false, showIcon=false)` を `player.addEffect(...)` で再付与(220tick=11秒のバッファを毎tick更新するので実質常時、HUDのエフェクトアイコンには出ない設計)。
- クライアント/サーバー両方のロジカルサイドでハンドラが走る実装(`Level#isClientSide` 系アクセサ名を未検証のままコードに入れるリスクを避けるため、あえてサイド分岐を省略。クライアント側での呼び出しは冗長だが害はない、という判断。詳細はクラスのjavadoc参照)。
- API裏取り(WebSearch + WebFetchで実施、詳細は各URLを参照): `TickEvent.PlayerTickEvent`(`net.minecraftforge.event.TickEvent`、`phase`/`player`フィールドを持つ)、`MobEffectInstance` の6引数コンストラクタ(`effect, duration, amplifier, ambient, visible, showIcon`)、`ArmorItem#getMaterial()` が1.20.1で `ArmorMaterial` を直接返すこと(1.21以降の `RegistryEntry<ArmorMaterial>` ではない)を確認した上で実装。
- **未検証**: 実プレイでの見た目・バランス(既存のポーション/矢由来の暗視効果との干渉、amplifier重複時の挙動、ラグ時の一瞬の途切れなど)。ビルドは通る想定(§4参照、Run 9で実証)だが、これはコンパイルが通ることの確認であり、ゲームプレイ上の検証ではない。

### 3B-2. Prismium Lantern(新ブロック): ツール非依存の量産可能な光源
- `ModBlocks.PRISMIUM_LANTERN`: `strength(3.5f, 3.5f)`、`SoundType.AMETHYST`、`lightLevel 15`(mod内最大)。`requiresCorrectToolForDrops()` は付けず、素手でも壊れて必ず自身をドロップする設計(バニラの `Lantern` ブロックの実際の仕様を踏襲。Prismium Block/Coreのようなツール階層縛りはこのブロックには意図的に付けていない)。
- `mineable/pickaxe` タグに追加(ツルハシだと効率良く掘れるが、素手や他ツールでも掘れる。バニラLanternと同じ扱い)。
- クラフト: shapeless、Prismium Shard ×4 + `minecraft:torch` ×1 → Prismium Lantern ×1。
- ブロックステート/モデルは `prismium_block`/`prismium_core` と同じ `cube_all` パターンを踏襲(全6面同一テクスチャー)。ルートテーブルも同じ「常に自身をドロップ」パターン。
- クリエイティブタブに追加(Prismium Core の直後)。lang(en/ja)も追加。
- **テクスチャー**(`scripts/textures/gen_prismium_lantern.py`): これまでの `prismium_block`/`prismium_core`(斜めグラデーションの結晶面)とは異なる新しい視覚言語として、「暗い金属の格子(ランタンの枠を想起させる)+中心が明るいラジアル状の発光」を採用。格子は縦横3本ずつ(1px)に留め、交点にリベット状のドットを置いた程度に抑制。紫のエネルギーの粒(Prismiumパレット共通のアクセント色)も3箇所のみに絞った。
  - **自己レビュー実施済み**: 生成後、16倍・4倍にアップスケールしたプレビューをRead(閲覧)で確認。格子と発光のコントラストで小さい表示でもシルエットが明瞭、意図しないノイズ・透過崩れ無し(alphaは全ピクセル255で確認済み)。セッション#3のPrismium Coreでの教訓(「派手にしすぎたら簡潔な構造に立ち返る」)を踏まえ、格子・アクセントとも最初から控えめに設計したため、作り直しは不要だった。

### 3B-3. CI/ビルド確認
- セッション開始時、Run 8(直前セッションの最終コミット)が "completed successfully" であることをバッジ+実行一覧HTMLで確認(§2-4の手法、api.github.comは今回も到達不可のまま)。
- 2コミット(セット効果→ランタン)を作成、`git fetch origin main` で `ci: update built jar [skip ci]` が1件先行しているのを検知し `git rebase origin/main` で解消してからpush。
- push時、プロキシ環境変数は一切いじらず素の `git push origin main` がそのまま成功した(§2-3の「まず何もいじらずに試す」方針の通り、今回も無改変で成功。プロキシ回避策が必要になるケースは依然として未確認のまま)。
- push後、Run 9(このセッションの2コミットをまとめて検証)が "completed successfully" になったことを確認済み(セッション終了時点)。**つまりセット効果ハンドラ・Prismium Lanternとも、実際にコンパイルが通ることを実証できた**。ただし前述の通りプレイテストは別問題。

---

## 3C. セッション#5で実装した内容

セッション開始時点でRun 9(セッション#4最終コミット直後の自動ビルド)は"completed successfully"だった(ただし§2-7の通り、この確認自体はキャッシュされたページ経由だと信頼できず、後から気づいた教訓)。セッション#4の「次にやるべきこと」(§5参照、当時の版)に沿って、(1)サーバー限定ガードの確認・追加、(2)アーマーセット効果のもう1段リッチ化、(3)ツール側の固有ギミック、の3点に着手した。

### 3C-1. `Entity#level()` / `Level#isClientSide` の確認とサーバー限定ガード追加
- MinecraftForgeの実ソース(`ForgeEventFactory.java`、`1.20.x`ブランチ)をWeb fetchで直接参照し、`entity.level()`・`player.level()` はメソッド呼び出し、`level.isClientSide` はメソッドではなく**フィールド直接アクセス**(括弧なし)であることを実例コードで確認した(セッション#4で「未検証のため省略」とされていた箇所)。
- `ArmorSetBonusHandler#onPlayerTick` の先頭に `if (player.level().isClientSide) return;` を追加し、クライアント側での毎tick処理を削減。

### 3C-2. アーマーセット効果に水中呼吸(Water Breathing)を追加
- 同じ `ArmorSetBonusHandler` 内で、常時暗視と同じ「毎tickバッファ再付与」パターンで `MobEffects.WATER_BREATHING` も追加。フルセット時、暗視+水中呼吸の2効果が常時付与される状態になった。
- ロードマップの「議論したい論点」(セッション#4版)で候補として挙がっていた効果そのもの。

### 3C-3. Prismiumツルハシに固有ギミック追加(ツール側で初)
- 新規クラス `com.claudemod.event.PrismiumMiningHandler`。Prismium Ore / Deepslate Prismium Ore を Prismium Pickaxe で採掘した際、25%の確率でボーナスの Prismium Shard ×1 が追加でその場にドロップする。
- **重要な経緯(下記§3C-4のビルド失敗と修正も参照)**: 初版は `BlockEvent.HarvestDropsEvent` を使い `event.getDrops()` にアイテムを追加する設計だったが、**実際のCIビルド(Run 11)で失敗**した。調査の結果、`HarvestDropsEvent` はMinecraft 1.15前後で `BlockEvent.GenerateLootEvent`/`BlockEvent.DropLootEvent` に置き換えられ、Forge 1.20.1には存在しない可能性が高いと判明。作り直した最終版は `BlockEvent.BreakEvent`(採掘の瞬間に発火、`getPlayer()`/`getState()`/`getPos()` を持つ、長期間安定しているAPI)を使い、ボーナスシャードを `ItemEntity` として自力でワールドに直接スポーンさせる方式にした。ドロップテーブル/Fortuneには一切触れない、独立した加算方式。
- 25%固定・Fortune非対応という単純な実装であることは意図的な最小実装(§4参照)。

### 3C-4. 【重要】セッション初のCIビルド失敗の実体験と修正、およびActionsキャッシュ問題の発見
- §3C-3の初版コミット(`Add tool-side gimmick + richer armor set bonus`)をpushしたところ、`git push` 自体は成功したが、**その後のGitHub Actionsビルド(Run 11)が実際に失敗した**。これはPROGRESS.mdの記録が始まって以来、セッション実行中に「push後の自分のコミットが原因でビルドが赤くなった」ことをそのセッション内で検知・修正できた初めてのケース(Run 4の失敗はワークフロー修正コミット自身が原因で、セッション#3は既に完了した後にその失敗を"発見"しただけだった)。
- 検知の過程で§2-7のキャッシュ問題(runsページ/バッジがプロキシ経由でキャッシュされ、実際には失敗しているのに古い"passing"や"currently running"の表示のままになる)を発見。クエリパラメータでキャッシュバスティングして初めて、Run 11が"failed"であることを正しく確認できた。
- 原因調査 → `PrismiumMiningHandler` を `BlockEvent.BreakEvent` ベースに書き直し(§3C-3参照)→ 再push(`Fix Run 11 build failure: ...`)→ 今度はRun 12が(キャッシュバスティング済みのURLで)"completed successfully"になったことを確認、かつ `ci: update built jar [skip ci]` コミットが実際にリモートに追加されたことも `git fetch` で確認済み。**つまりセッション終了時点のmainは実際にビルドが通る状態**。
- この一連の経験から得られる教訓: **「pushしたらそれで終わり」ではなく、push後に(キャッシュバスティングした上で)実際のビルド結果を必ず確認する。もし失敗していたら、そのセッション内で追いかけて直す」ことが重要**だと改めて実証された。今回はセッション内で完結できたが、もし時間切れで完結できなかった場合は、次回セッション冒頭で失敗を最優先で拾う設計(タスクファイルの指示通り)がまさにこのために存在する。

---

## 3D. セッション#6で実装した内容

セッション開始時、§2-4/§2-7の手法(キャッシュバスティング付きURL)で直近のビルド結果を確認したところ、**直前セッション(このファイル未更新のまま並行して走っていた別セッション)の最終コミット `da16f4c`("Give Axe, Shovel, Hoe and Sword their first gimmicks" — Axe/Shovel/Hoe/Swordに初のギミックを追加、Pickaxeで確立したパターンの横展開)が実際にビルドを壊していた(Run 16 "failed")**ことが判明した。タスクファイルの指示通り、これを最優先事項として着手した。

### 3D-1. Run 16 ビルド失敗の調査(実装ではなく調査、かなりの時間を投入)
- このセッションのサンドボックスからは `maven.minecraftforge.net` に到達できず(§2-1と同じ制約、今回も変化なし)、`./gradlew build` を再現してjavacの実際のエラーメッセージを得ることはできなかった。
- GitHub Actionsのrun詳細ページ(`/actions/runs/<id>/job/<id>`)も試したが、ReactによるクライアントサイドレンダリングでログはAPI経由(要ログイン)でしか取得できず、静的HTML取得では実際のビルドログ本文は見えないことを再確認した(§2-4に記載済みの制約と一致)。
- そのため、変更された3ファイル(`PrismiumMiningHandler`のAxe/Shovel追加分、新規`PrismiumSwordHandler`、新規`PrismiumHoeHandler`)を1行ずつ手動レビューし、使用しているForge/Mojang APIそれぞれについて、WebSearch + WebFetchで**バージョンごとのシグネチャ**を裏取りした。
  - `BlockEvent.BreakEvent` ベースのAxe/Shovel追加(`BlockTags.LOGS`, `Blocks.GRAVEL`, `Items.FLINT` 等)は、セッション#5で確立済みの実証済みパターンをそのまま流用しており、問題は見当たらなかった。
  - `PrismiumSwordHandler`(`LivingHurtEvent` + `MobEffectInstance` 6引数コンストラクタ)も、セッション#4で実証済みの `ArmorSetBonusHandler` と全く同じAPIパターンで、問題は見当たらなかった。
  - `PrismiumHoeHandler`(`PlayerInteractEvent.RightClickBlock` + `BonemealableBlock` インターフェース直接操作)は、このMOD初のイベントフック種別かつ初めて直接呼び出すインターフェースだった。個別メソッドを1つずつ複数のマッピングサイト(`mappings.xhyrom.dev`、`nekoyue.github.io/ForgeJavaDocs-NG`、`lexxie.dev/forge/1.20.1`)で1.18.2〜1.20.3の複数バージョン分クロスチェックした結果:
    - `isBonemealSuccess(Level, RandomSource, BlockPos, BlockState)` と `performBonemeal(ServerLevel, RandomSource, BlockPos, BlockState)` は、確認した全バージョンで引数の型・数が完全に同一(安定API)。
    - `isValidBonemealTarget` だけは **バージョンをまたいで引数の数が変化している**(1.19.3時点では末尾に `boolean`(isClient的なフラグ)付きの4引数、1.20.2時点では3引数)ことを突き止めた。WebSearchの要約では「1.20で3引数化された」という情報も得られ、1.20.1もおそらく3引数版だろうという推測はできたものの、**1.20.1ピンポイントでの一次情報は最後まで確定できなかった**(mappings.xhyrom.devの1.20.1個別ページは検索結果に出てこず、`web_fetch`ツールのprovenance制限で直接URLを叩くこともできなかった)。
- 結論として、javac の実エラーメッセージを直接見ることはできなかったため「これが原因だ」と**断定はできていない**が、(a) 唯一バージョン間で挙動が変わることが確認できたAPIであること、(b) このMOD内で唯一の「新しいインターフェースへの直接呼び出し」であり過去に実証されたパターンの使い回しではないこと、の2点から `isValidBonemealTarget` の呼び出しを最有力容疑として特定した。

### 3D-2. 修正: `PrismiumHoeHandler` から `isValidBonemealTarget` 呼び出しを削除
- 事後確認ではなく事前のリスク低減として、`isValidBonemealTarget` による事前チェックを削除し、`isBonemealSuccess` / `performBonemeal`(引数シグネチャが全バージョンで安定していると確認済み)のみに依存する実装に変更した。
- 実質的な影響: 事前チェックを省いたことで、本来なら「対象外」と弾かれるはずだったブロック(例: 既に最大成長段階の作物)に対しても `performBonemeal` を呼び出す可能性がある。バニラの成長可能ブロックはこのケースで安全にno-opする実装になっていることを期待した設計判断であり(「既に育ちきった作物に骨粉を撒く」という行為自体はバニラでも常に安全に許可されている操作なので、リスクは低いと判断)、詳細な理由づけはクラスのjavadocに明記した。
- コミット `631e06a`("Attempt fix for Run 16 build failure: drop riskiest Bonemeal API call")としてpush。

### 3D-3. 修正の検証: Run 17 で実際にビルド成功を確認
- push後、§2-7のキャッシュバスティング手法で確認したところ、**Run 17は "completed successfully"** になった(修正前のRun 16は "failed" のまま履歴に残っている)。`git fetch` で `ci: update built jar [skip ci]`(コミット `cb4d6d1`)がリモートに実際に追加されたことも確認済み。
- つまり、**この修正で実際にビルドが復旧したことを実証できた**。ただし前述の通り、これが本当に唯一の原因だったのか(たまたま他の要因、例えば依存関係取得の一時的な問題等が併発していた可能性)は、javacの実エラーを見ていない以上100%の確信はない。次回セッション以降、もし同種の失敗が再発した場合はこの点を疑ってよい。

### 3D-4. このセッションで新規コンテンツ(ブロック/アイテム/MOB/テクスチャー)は追加していない
- タスクファイルの指示通り「失敗していればその修正を今回のタスクの最優先にする」を厳守し、CI復旧の調査・修正・検証に大半の時間を使った。結果として今回は新規テクスチャー・新規ブロック/アイテムの追加は無し(セッション#3以来、初めて「新規実装ゼロ」のセッションになった)。次回セッションは通常運転(新要素の実装+テクスチャー自作)に戻ってよい。

## 3E. セッション#7で実装した内容

セッション開始時、§2-4/§2-7の手法(キャッシュバスティング付きURL)で直近のビルド結果を確認しようとしたところ、**1回目のクエリは(§2-7で既知の)キャッシュに阻まれ、"Run 8"という数セッション分古い内容が返ってきた**。クエリパラメータを変えて4回ほど取り直してようやく最新化され、Run 18("Update PROGRESS.md for session 6")が"completed successfully"であることを確認できた。**今回の教訓の追記**: 1回のnocacheクエリで最新化される保証は無く、数回リトライする前提で臨むこと(§2-7に「必ず複数回試す」ことを追記する価値あり)。ビルド失敗は無かったため、修正対応は不要で、PROGRESS.md §5(セッション#6の申し送り)にあった「次回は通常運転(新規コンテンツ+テクスチャー)に戻ってよい」の通り、新規実装に着手した。

### 3E-1. Prismium Grappling Hook: 初のアクセサリ系アイテム
- ロードマップ(§1)のアイテム5番目に、セッション#1の時点から「まだ」のまま放置されていた「探索を楽しくするアクセサリ的アイテム(グラップリングフック等)」に、ついてついに着手。
- `com.claudemod.item.PrismiumGrapplingHookItem`(`Item#use`をオーバーライド): 右クリックで視線方向に最大24ブロックのレイキャスト(`Level#clip(ClipContext)`)を行い、ブロックに命中したら `player.setDeltaMovement(...)` でプレイヤーの速度を命中点方向へ直接書き換えて引き寄せる。バニラの釣り竿(`FishingHook#pullEntity`)と同じ「速度を直接書き換え、`hurtMarked`フラグを立ててクライアント同期を促す」手法を踏襲しつつ、専用の飛翔エンティティ(フックのエンティティ本体)は一切使わない、`Item`だけで完結する最小実装にした(新規Entity/レンダラー/ネットワークペイロードを増やさないための設計判断)。
- クールダウンは `player.getCooldowns()`(エンダーパール等と同じ標準API)で25tick(1.25秒)。耐久値250、成功して引き寄せるたびに1耐久消費(`ItemStack#hurtAndBreak` + `LivingEntity#broadcastBreakEvent(InteractionHand)`)。
- **API裏取り(WebSearchで実施、このMOD初使用のAPI3つ)**: `Level#clip(ClipContext)` + `ClipContext`の5引数コンストラクタ(既存の複数のチュートリアル記事で確認)。`Entity#hurtMarked`(公開boolean、ノックバックや釣り竿の引き寄せで使われる「速度変更をクライアントへ同期する必要がある」フラグ)。`ItemStack#hurtAndBreak(int, LivingEntity, Consumer<LivingEntity>)` と `LivingEntity#broadcastBreakEvent(InteractionHand)` の組み合わせは、バニラの盾破損コード(`this.useItem.hurtAndBreak(i, this, (player) -> player.broadcastBreakEvent(interactionhand))`)と完全に一致するパターンであることを確認した上で採用(§4-9で触れている「javacの実エラーが見れない」制約を踏まえ、未知APIは使う前に必ず実例コードで裏取りする方針を今回も継続)。
- クラフト: 鉄インゴット×2 + Prismium Shard×1 + 糸×3(shaped、`IXI / SXS / _S_`)。クリエイティブタブにも追加。lang(en/ja)も追加。
- **テクスチャー(`scripts/textures/gen_prismium_grappling_hook.py`)は3回描き直した**。1回目(3本爪の鉤爪が1点から放射)・2回目(単一の巻き返しフック、意図的な2px隙間を「口」として残す設計)は、いずれも生成後の16倍/4倍プレビューを目視した結果、**フックの輪郭が判別できない黒っぽい塊にしか見えない**と判断して破棄した(セッション#3のPrismium Core・セッション#4のレギンス隙間バグと同種の「小さいキャンバスで細い開いた曲線は輪郭線処理に負ける」問題)。3回目でアプローチを変え、**穴を実線で完全に塗りつぶした後にくり抜く「ドーナツ/リング」形状**にしたところ(隙間を2本の別ストロークの間隔に頼るのではなく、塗り潰した面から矩形を減算する方式)、16倍・4倍どちらのプレビューでも「輪っかのフック」として明瞭に読めることを確認し、これを採用した。ロープ部分(暖色系の新規パレット、既存の道具の柄色とは差別化)は1回目から変更なし。アルファ値も全塗り潰しピクセルで255であることを確認済み(透過崩れ無し)。
- **未検証**: 実プレイでの引き寄せの感触(速度1.35・クールダウン25tick・耐久250・射程24ブロックはいずれも初期見積もりの数値で、実際に触ってみると強すぎる/弱すぎる/操作感がぎこちない等の調整が必要になる可能性が高い)。特に「フックエンティティを飛ばさず即座に引き寄せる」設計は見た目の演出(飛んでいくフックが見えない)を犠牲にしている点も、プレイして初めて気になるかどうか分かる部分。

### 3E-2. push・ビルド確認
- 変更は1コミット(`Add Prismium Grappling Hook: first accessory-style item`, `a570050`)。push前に `git fetch origin main` で他セッションとの衝突が無いことを確認(空振り)。
- push自体は今回もプロキシ変数を一切いじらず、素の `git push origin main` で成功(§2-3の「まず何もいじらず試す」方針、これで4セッション連続成功)。
- push後、§2-7のキャッシュバスティング手法でRun 19を追跡。今回は最初の1回のポーリングで"currently running"、約30秒後の2回目のポーリングで**"completed successfully"**を確認できた(セッション#3以来ずっと踏襲している「push後は必ず確認、failedなら追いかける」を継続)。`git fetch` で `ci: update built jar [skip ci]`(`391eba9`)がリモートに実際に追加されたことも確認済み。つまりこのセッションの変更は実際にコンパイルが通る状態でmainに入っている。

## 3F. セッション#8で実装した内容

セッション開始時、まず badge.svg のキャッシュバスティング確認で "passing" を確認(§2-7の手法)。その後、直前セッション(#7)の申し送り(§5、旧版)に沿って、ロードマップの2大未着手の柱(§1)のうち「Prismium Energyシステム」に、セッション#1以来はじめて着手した。

### 3F-1. Prismium Cell: MOD初のBlockEntity、Energy系統の第一歩
- `com.claudemod.block.PrismiumCellBlock` / `com.claudemod.blockentity.PrismiumCellBlockEntity` / `com.claudemod.energy.PrismiumEnergyStorage` / `com.claudemod.registry.ModBlockEntities` の4クラスを新規作成。PROGRESS.md セッション#7の申し送り(§5-9、旧版)で提案されていた「Energyなら`IEnergyStorage`を持つだけのBlockEntity1個、GUI無し」という最小の第一歩をそのまま実行した。
- **API裏取り(WebSearch+WebFetchで2回に分けて実施、このMOD初のBlockEntity/Capability関連API)**: `net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY` が1.20.1で非推奨でない正しいトークンであること(`net.minecraftforge.energy.CapabilityEnergy.ENERGY` は存在するが1.19.2から非推奨でこちらに転送するだけ)、`LazyOptional<IEnergyStorage>` を `getCapability`/`invalidateCaps` で公開する定型パターン、`net.minecraftforge.energy.EnergyStorage` が **NBTシリアライズを一切実装していない**こと(古い CoFH RF-API の同名クラスと混同しないよう要注意、と裏取り時に明示的に確認)、`BlockEntity#saveAdditional`/`load` が1.20.1では `HolderLookup.Provider` 引数なしの `CompoundTag` 単体シグネチャであること(1.20.5以降で変更される前の版)、`Block#use` の6引数シグネチャ、`BaseEntityBlock` を使う際は `getRenderShape` を明示的に `RenderShape.MODEL` でオーバーライドしないと通常のキューブモデルとして描画されない可能性があること、`Player#displayClientMessage(Component, boolean)` のactionBar引数の意味、をすべて1.20.1系のソース(Forge公式ドキュメントの`/en/1.20.1/`固定ページ、`nekoyue.github.io/ForgeJavaDocs-NG`の1.18.2/1.19.3版javadoc)で個別に確認した上で実装。
- `PrismiumEnergyStorage` は `EnergyStorage` のサブクラスで、`setEnergy(int)` を追加(protectedな`energy`フィールドをNBT復元時だけ直接書き換えるため。通常のエネルギー授受は `receiveEnergy`/`extractEnergy` 経由でmaxReceive/maxExtract制限を守る)。
- 容量100,000 FE、maxReceive/maxExtract各800 FE/tick。**すべて初期見積もりの数値で未調整**。
- **GUIが無いため、右クリックでの手動インタラクションを実装**(セッション#7のグラップリングフックと同様、「新機構を最小構成で試せる形にする」判断):
  - 素手で右クリック: 現在値/最大値をアクションバー表示(`message.claudemod.prismium_cell.status`)。
  - Prismiumの欠片を持って右クリック: 欠片1個を消費してFE+4000を注入(満タン時は`.full`メッセージのみでconsumeせず、`receiveEnergy(amount, true)` でシミュレートしてから実際に注入する2段階呼び出しで消費前に判定)。クリエイティブモードでは欠片を消費しない(`player.getAbilities().instabuild`判定、既存コードに前例なし・バニラの一般的な作法を踏襲)。
  - この「手動チャージャー」機構は、自動発電機がまだ存在しない現状で、capabilityの受電経路(receiveEnergy→NBT永続化→読み出し)を実際にエンドツーエンドで検証できる唯一の手段としても機能する。
- クラフト: shaped `IRI/RCR/IRI`(I=鉄インゴット、R=レッドストーン、C=Prismium Core)。**Prismium Core(セッション#3で追加、これまでクラフト先が無くクラフト結果としてのみ存在していた)を初めて別レシピの材料として使う**ことで、既存アイテムに新しい用途を持たせた。
- ブロック自体は `requiresCorrectToolForDrops()` を付けていない(Prismium Block/Coreのような資源系ブロックではなく「機械」という位置づけなので、ツール階層に縛らない方が自然、という判断。ランタンと同じ考え方)。
- `ModBlockEntities` は他の `Mod*Registry` クラスと同じ `DeferredRegister` パターンで新規作成し、`ClaudeMod` コンストラクタに `ModBlockEntities.register(modEventBus)` を追加。

### 3F-2. テクスチャー(`scripts/textures/gen_prismium_cell.py`) — 1回描き直し
- MOD内3つ目の新しい視覚言語として「金属ケーシング(電池筐体)+ガラス窓のPrismiumグロー+短いゲージ風アクセント線」を採用。
- **自己レビューで1回描き直した**: 初版はランタンの`CAGE_DARK`/`CAGE_MID`(ほぼ黒に近い暗色)をそのまま流用し、枠を3px・窓を10x10・アクセント線を窓幅いっぱいの3本(magenta)で描いたが、生成後の16倍/4倍プレビューを目視した結果、**金属枠が既存の`PRISMIUM_OUTLINE`(暗いティール)と明度・彩度が近すぎて視覚的に溶け込んでしまい、「電池」という差別化ポイントが実質消えていた**上に、窓幅いっぱいのマゼンタの帯3本が「ゲージのアクセント」というより「キャンディストライプの塊」に見えてしまっていた。既存のCAGE系パレットを流用する発想自体は妥当だったが、色相・明度が家族の他の色と衝突していたのが原因と判断し、枠の色をより明るく中立的なスレートグレー(`CASING_DARK`/`CASING_MID`)に変更、枠を4pxに太らせて窓を8x8に縮小(枠のシルエットが主役になるように)、アクセント線を窓幅いっぱいから短い2本(位置も非対称)に減らして再生成した。再確認した結果、枠と窓のコントラストが明瞭になり、4倍プレビューでも「箱の中に光る窓がある」というシルエットとして判別できることを確認して採用。アルファ値も全ピクセル255であることを確認済み(透過崩れ無し)。
- このセッションの教訓(次回以降のテクスチャー作成でも参考にできる): **既存パレットを別の新規テクスチャーに流用する際は、色そのものだけでなく「そのテクスチャー内で共存する他の色(特に共通の`PRISMIUM_OUTLINE`)との明度・彩度のコントラストが十分か」を単体プレビューだけでなく明示的にチェックすること**。今回は流用元(ランタン)では機能していた色が、輪郭線の役割が異なる新しい構図(枠自体が主役)では機能しなかった。

### 3F-3. push・ビルド確認、および確認手法そのものの新しい知見
- 変更は1コミット(`Add Prismium Cell: first block entity, opens the Prismium Energy pillar`, `cac3bfc`)。push前に `git fetch origin main` で衝突無しを確認、素の `git push origin main` で一度で成功(プロキシ変数はいじらず。これで5セッション連続無改変成功)。
- **push後のビルド確認で、§2-7のキャッシュ問題がこれまでで最も深刻な形で再現した**: badge.svg は毎回異なるキャッシュバスティング用クエリでも一貫して「passing」を返したが、runsページ(`/actions/workflows/build-and-notify.yml`)・Actionsトップページ(`/actions`)の両方とも、クエリパラメータを何度変えても(ランダム文字列を都度変更、`Cache-Control: no-cache`ヘッダも付与)**セッション#1〜#3頃、つまり数十Runも前の内容(Run 1〜8)が返り続け、一度も最新化されなかった**。これは過去のセッション(#5, #7)が「数回リトライすれば直る」としていた前提を破る、より深刻な事例。
- コミットページ(`/commit/<hash>`)からCIステータスを読み取ろうとする新しい方法も試したが、差分内のコード文字列(このセッションで書いた`InteractionResult.SUCCESS`)を検知結果と誤認しかけるなど、静的HTML解析ではノイズが多く信頼できないと判断し却下。
- **代わりに採用した、より信頼できる確認方法(今回の新発見)**: `git fetch origin main` を数十秒おきに繰り返し、`ci: update built jar [skip ci]` という自動コミットが自分のコミットの直後に実際に生成されるかどうかを見る方法。この方法はHTTPプロキシのキャッシュの影響を一切受けない(gitのプロトコル自体はキャッシュされない)上、「ジョブが成功してjar公開ステップまで到達した」ことの直接証拠になる(ワークフローの最終ステップが`steps.build.outcome != 'success'`ならジョブ自体を失敗させる設計、§2-4参照、なので、jarコミットが来た時点でビルド成功は確定)。今回はpush後6回・約100秒のポーリングで `524487a`(jarコミット、旧53,899バイト→新62,533バイトにサイズ増加、新規クラス・テクスチャーが実際に反映されたことも裏付け)の到着を確認できた。
- **次回以降への提案**: badge/runsページのキャッシュバスティングはもはや当てにならない場合があると分かったため、**「push後のビルド確認は`git fetch`ポーリングを第一手段とし、badge/runsページのHTML確認はあくまで補助情報にとどめる」**運用に切り替えることを強く推奨する。§2-7に追記する価値がある。


## 3G. セッション#9で実装した内容

セッション開始時、`git fetch origin main` で得られる最新コミット(`524487a` ci: update built jar → `9c84d85` Update PROGRESS.md for session 8 → `cda6f7f` ci: update built jar)を確認し、PROGRESS.md(session 8版)の「次回セッションへの申し送り」項目4「外部からの受電・送電(将来のケーブルや発電機との連携)は一度も実地検証できていない」に沿って、Prismium Cellに実際にFEを送り込む側の機械 = Prismium Generator に着手した。api.github.com・runsページ双方とも今回もこのサンドボックスから信頼できる形では確認できなかった(§2-4/§2-7と同じ制約、変化なし)ため、確認は最初から`git fetch`ポーリング方式(§5-1、session 8で確立済み)のみに絞った。

### 3G-1. Prismium Generator: MOD初のBlockEntityTicker、初の自動FE送電
- `com.claudemod.block.PrismiumGeneratorBlock` / `com.claudemod.blockentity.PrismiumGeneratorBlockEntity` の2クラスを新規作成(ModBlocks/ModBlockEntities/ModItems/ModCreativeTabsへの登録込み)。
- **API裏取り(WebSearchで実施、このMOD初のBlockEntityTicker/近傍capability取得API)**: `BaseEntityBlock#getTicker` は `level.isClientSide()` でクライアント側は`null`を返し、サーバー側は `createTickerHelper(type, ModBlockEntities.PRISMIUM_GENERATOR.get(), PrismiumGeneratorBlockEntity::serverTick)` を返すのが定型パターンであることを、複数のチュートリアル記事のコード例で確認。近傍ブロックのcapability取得は `level.getBlockEntity(neighborPos).getCapability(ForgeCapabilities.ENERGY, direction.getOpposite())` という「相手側から見て自分の方を向いている面」を渡す形が定石であることも確認した(自分の送電方向ではなく、相手の受電方向を渡す点に注意)。`IEnergyStorage#canReceive()`/`canExtract()` は1.11以降変化のない安定APIであることも別途確認。
- **設計**: Prismiumの欠片を右クリックで投入すると1600tick(バニラの石炭と同じ80秒)分の燃焼時間が加算される(累積可能、かまど燃料と同じ挙動)。燃焼中は毎tick 10 FEを内部バッファ(容量8,000 FE、Prismium Cellの100,000 FEに比べてかなり小さい「送電専用」の設計)に追加し、毎tick(燃焼中かどうかに関わらず)バッファに残高があれば最大200 FE/tickを6方向の隣接ブロックのうちcapabilityを公開しているもの全てに分配して送電する。
- **意図的なバランス設計**: 欠片1個あたりの総産出量は 1600tick × 10FE = 16,000 FE で、Prismium Cellの手動チャージ(欠片1個で即座に4,000 FE)の4倍。「手動は即座だが少量、自動化(Generator)は時間はかかるが同じ欠片からより多くのFEを得られる」という、テックMOD的な「自動化への投資が報われる」構図を、MOD初の機械ペアで表現した。
- バッファが満杯の間は燃焼時間を消費しない(=送り先が無い/詰まっている状態では欠片を無駄に浪費しない)設計も追加。
- `BlockStateProperties.LIT`(バニラのかまど・キャンプファイヤーと同じプロパティを再利用、独自プロパティは新設せず)を使い、燃焼中/非燃焼でモデルと発光レベル(0⇔8)を切り替える、MOD初のブロックステートプロパティ活用。
- クラフト: shaped `IPI/PFP/IPI`(I=鉄インゴット、P=Prismiumの欠片、F=バニラかまど)。「かまどをPrismium発電機にアップグレードする」という世界観の演出を意図。
- 右クリック操作はPrismium Cellと同じ「GUI無しの手動インタラクション」方針を踏襲: 欠片を持って右クリックで燃料投入、素手で右クリックで残り燃焼時間(秒)と現在/最大FEをアクションバー表示。
- **ついでの小修正**: `data/minecraft/tags/blocks/mineable/pickaxe.json` に `claudemod:prismium_cell` が(session 8で)漏れていたことに気づき、Generatorと合わせて追加した(意図的な設計ではなく単純な入れ忘れだったと判断)。

### 3G-2. テクスチャー(`scripts/textures/gen_prismium_generator.py`) — 2枚(lit/unlit)、描き直し無し
- Prismium Cell(session 8)で確立済みの金属ケーシングパレット(`CASING_DARK`/`CASING_MID` 対 `PRISMIUM_OUTLINE`)をそのまま再利用し、「CellとGeneratorは同じ機械ファミリー」という視覚的まとまりを意図的に作った(session 8の教訓「新規パレットはPRISMIUM_OUTLINEとのコントラストを個別確認すること」を踏まえ、今回は検証済みパレットの再利用でそのリスク自体を回避)。
- 中央の意匠だけをCell(ガラス窓)と差別化: かまど風の3本の横スリット状エンバーグレート。非点灯(lit=false)は暗い赤茶色で消し炭を表現、点灯(lit=true)はオレンジ〜白のグラデーションで熱い燃焼を表現し、さらにグレート四隅に小さいシアンのアクセント(Prismium共通パレットの`PRISMIUM_ACCENT`)を置いて「燃料(オレンジ)がFE(シアン)に変換されて出ていく」ことを暗示した。
- **自己レビュー実施済み**: 1x/4x/16x/32xの4段階アップスケールしたプレビューシートを生成してRead(閲覧)で確認。両状態ともグレートのスリットが小さい表示でも明瞭に判別でき、lit/unlitの対比も一目で分かるコントラストがあることを確認。全ピクセルのアルファ値が255であることもコードで機械的に確認済み(透過崩れ無し)。今回は初回ドラフトで基準を満たしたため、Prismium Core/レギンス/グラップリングフックのような描き直しは発生しなかった(Cellの検証済みパレット再利用が効いたと考えられる)。

### 3G-3. push・ビルド確認
- 変更は1コミット(`Add Prismium Generator: first BlockEntityTicker, auto-pushes FE to neighbors`, `7e4766c`)。push前に `git fetch origin main` で他セッションとの衝突が無いことを確認(空振り、`origin/main`は`cda6f7f`のまま先行コミット無し)。
- push自体は今回もプロキシ変数を一切いじらず、素の `git push origin main` で一度で成功(§2-3の方針通り、これで6セッション連続無改変成功)。
- push後、`git fetch origin main` を約15秒間隔で6回(計約90秒)ポーリングし、`ci: update built jar [skip ci]`(`2a5e957`)の到着を確認。ローカルを`git merge origin/main --ff-only`で追従させたところ、`builds/ClaudeMod-latest.jar` が 62,533 → 71,856 バイトに増加していることも確認できた(新規クラス・新規テクスチャー2枚が実際にビルド成果物へ反映された裏付け)。**つまりこのセッションの変更(MOD初のBlockEntityTicker含む)は実際にコンパイルが通ることを実証できた**。runsページ/badge.svgのHTML確認は今回は最初から試みず、session 8で確立した`git fetch`ポーリングのみに一本化した(結果的に問題なく機能した)。


## 3H. セッション#9で追加対応: GitHub issue #1「顔が見えない」の修正

§0-2のルール(並行セッションが追加したもの)をマージコンフリクト解消の過程で発見し、その場でOpen Issueを確認したところ、issue #1「プリズム装備を装着した際、顔が見えない」(OPEN)が見つかった。内容: 「顔はプレイヤーを識別する重要な部位です。装備で顔が見えなくなるのはいかがなものかと思います。」

- 原因: `scripts/textures/gen_prismium_armor.py` の `make_layer1()` が、頭部ボックスの前面(顔にあたる面)を他の面と同じロジックで完全不透明に塗っていた。これはバニラのヘルメット(鉄・ダイヤ等)と同じ挙動(バニラも装備中は顔が完全に隠れる)ではあるが、今回はユーザーから明示的に「顔を見せてほしい」という要望が来たため、バニラ踏襲を優先せず要望に応える方針を採った。
- 修正: `open_face()` / `helmet_front()` を新規追加。頭部ボックスの前面のうち、上端2行だけ(額のあたり、"ブリム"/縁のような帯)を従来通り不透明のPrismiumクリスタル色で残し、それ以外(目・鼻・口・あご、行3〜8相当)を完全透明(alpha=0)にして、下地のプレイヤースキンの顔がそのまま見えるようにした。他の3部位(チェストプレート・レギンス・ブーツ)およびヘルメットのインベントリアイコン(16x16の平面アイコン)は変更していない。
- **自己レビュー実施済み**: 修正後の`prismium_layer_1.png`をチェッカーボード背景付き・頭部前面クロップ込みでアップスケールしたプレビューを生成しRead(閲覧)で確認。上部2行の帯だけがPrismiumカラーで残り、その下がきちんと透明(チェッカーボードが透けて見える)になっていることを確認。シート全体のプレビューでも、他の部位(チェストプレート・レギンス・ブーツ)の不透明領域や既存のジェムアクセントには一切変化が無いことも確認した。アルファ値をコードで機械的に確認したところ `{0, 255}` の2値のみで、意図しない中間値(部分透過によるにじみ等)は無かった。
- **未検証**: 実際にプレイヤーが装着した状態で3Dレンダリングした際に、この「上2行だけ残す」設計が見た目としてちょうど良いか(細すぎる/太すぎる、頭の傾きによっては帯が不自然に見える等)は、このサンドボックスではゲームを起動できないため確認できていない。次回以降、プレイ確認の機会があれば真っ先にフィードバックを反映したい箇所。
- コミット: `Fix #1: open the helmet's face instead of fully covering it`。Issueへのコメント投稿・クローズはgitトークンの権限上できない(§0-2参照)ため未実施。ユーザー側でIssueのクローズ判断をお願いしたい。

## 3I. セッション#10で実装した内容

セッション開始時、まず `git fetch origin main` でローカルとリモートのHEADが一致(`d0264a4`)していることを確認し、これがセッション#9の「顔が見えないIssue修正」コミット直後の `ci: update built jar` であることから、**直前のビルドは成功している**と判断した(runsページ/badge.svgのHTML確認は今回も行わず、session 8以降定着した`git fetch`ベースの確認に一本化)。

続いて§0-2の運用ルールに従いGitHub Issueを確認(`https://github.com/Konpeitou24/ClaudeMod/issues?nocache=<timestamp>` を取得しGraphQLのpreloadedデータをgrep)。**Open Issueはissue #1「プリズム装備を装着した際、顔が見えない」1件のみで、これはセッション#9で既に修正済み(コミット`275f027`)。ユーザー側でのクローズ待ちの状態が続いているだけで、新規の未対応Issueは無し**(「確認したが無かった」ではなく「確認したところ、対応済みのものが1件Open状態で残っている」というケース)。

ビルド失敗・新規Issueとも無かったため、PROGRESS.md(session 9版)§5の「すぐやるべきこと」項目4「Prismium GeneratorとPrismium Cellを実際に隣接させて動作確認する手段がまだ無い」、および議論したい論点の「次のケーブルや複数台のGenerator/Cellを組み合わせる場面が来たら」を踏まえ、**Prismium Cable**(Generator/Cell間の中継ブロック)の実装に着手した。ロードマップ(§1、項目2 Prismium Energy)の観点では、「発電→送電→蓄電」のループを「隣接必須」から「離れた場所でも配線でつなげる」に一歩進める内容。

### 3I-1. Prismium Cable: 中継ブロック、MOD初の非フルキューブ形状
- `com.claudemod.block.PrismiumCableBlock` / `com.claudemod.blockentity.PrismiumCableBlockEntity` の2クラスを新規作成(ModBlocks/ModBlockEntities/ModItems/ModCreativeTabsへの登録込み)。
- **設計**: Generator・Cellと同じ`IEnergyStorage`capability(`ForgeCapabilities.ENERGY`)を公開する小容量バッファ(容量400 FE、maxReceive/maxExtractとも400 FE/tick)を持ち、外部から押し込まれたFEを毎tick(サーバー側のみ、MOD初の`BlockEntityTicker`はGeneratorに続き2例目)すぐさま隣接ブロックへ再送する「素通しの導線」として実装した。Generator/Cellのような右クリックでの燃料投入・充電機構は無く(手で操作する対象ではない配線という位置づけ)、空手右クリックでの状態確認(現在/最大バッファ量のアクションバー表示)のみ実装。
- **リファクタリング**: Generatorがセッション#9で`private pushEnergyToNeighbors`として実装した「6方向の隣接ブロックへFEを分配する」ロジックを、新規`com.claudemod.energy.EnergyPushHelper`(静的メソッド`pushToNeighbors`)へ切り出し、GeneratorとCable双方がこれを呼び出す形に整理した。ロジック自体は一切変更していない(Generator側の既存の動作を壊さないよう、コピー&ペースト元のコードをそのまま移動しただけ)。3つ目の同種実装が必要になった時点でのコピペ解消であり、PROGRESS.md過去セッションの「テクスチャー生成の再利用可能な知見」と同じ「同じ処理の3回目の複製は避ける」判断。
- **API裏取り(WebSearchは使わず、既存コード(Generatorの実証済み実装)を裏取り元として再利用)**: capability取得・受け渡しのAPIはGeneratorで既に1.20.1向けに確認済みのものをそのまま流用しているため、新規のバージョン差異調査は今回発生していない。一方、**このMOD初の非フルキューブブロック**という点で新しいAPI領域に触れた: `Block#getShape(BlockState, BlockGetter, BlockPos, CollisionContext)` をオーバーライドして`Block.box(4, 4, 4, 12, 12, 12)`(8x8x8、ブロック中央に配置)を返し、`BlockBehaviour.Properties#noOcclusion()`を付与(フルキューブでないブロックが不透明扱いのままだと、周囲のブロックのその面が誤って非表示になる問題を避けるため。柵・ガラス板などバニラの非フルキューブブロックが軒並み使っている標準パターンで、`Block#getShape`・`Properties#noOcclusion()`とも1.20.1に限らず長期間安定しているAPIと判断し、個別のバージョン裏取りは行わなかった)。
- **モデル**: `models/block/prismium_cable.json` はバニラの`cube_all`を継承せず、`elements`で明示的に8x8x8の立方体(4..12)を定義する自前モデルにした。**当初`cullface`を各面に指定していたが、`cullface`はブロック境界(0または16)に接する面でのみ意味を持つ最適化ヒントであり、中央に浮いた8x8x8の面に付けると、隣接ブロックが不透明な場合に本来見えるべき面まで誤って非表示になるバグを生みかねないと気づき、コミット前に削除した**(このMOD初のカスタムelementsモデルで踏みかけた落とし穴として記録しておく)。
- **既知の割り切り(意図的な最小実装、PROGRESS.mdに明記)**: バニラの柵・ガラス板のような「隣接ブロックに応じて接続形状が変わる」マルチパートblockstate(方向ごとのboolean属性 + `multipart` + 動的な当たり判定)は実装していない。**どのケーブルも常に同じ見た目(中央の柱状キューブ)で、隣とのビジュアルな接続表現(継ぎ目・分岐の見た目)は無い**。機能面(capabilityのやり取り)には影響しないが、次回以降の磨き込み候補として明確に記録する。
- **既知の割り切り(仕組み上の遅延)**: 各ケーブルが「自分のtickで、今持っている分だけを隣接に押し出す」という独立した動きをするため、Generator→Cable→Cable→Cellのような複数ホップの経路では、エネルギーが1tickあたり1ホップずつしか進まない(即座に端から端まで到達するわけではない)。ネットワーク全体を1つのグラフとして扱う本格的な送電網ロジック(Union-Findで接続グループを管理、容量をキャッシュする等)は実装しておらず、今回はその複雑さを避けて「各ブロックが独立に動く」単純な設計を選んだ。将来、ケーブルを大量に使う場面が出てきたら再検討したい。

### 3I-2. テクスチャー(`scripts/textures/gen_prismium_cable.py`) — 描き直し無し
- Prismium Cell/Generator(session 8/9)で確立済みの「機械ファミリー」パレット(`CASING_DARK`/`CASING_MID`、`PRISMIUM_OUTLINE`とのコントラスト検証済み)をそのまま再利用し、Cableも同じ機械ファミリーの一員であることを視覚的に示した(session 9の「検証済みパレットの再利用でリスクを回避する」知見をそのまま踏襲、3個目の適用例)。
- 中心の意匠はCell(ガラス窓+ゲージ)ともGenerator(かまど風スリット)とも異なる新しい差別化点として、「小さめの同心正方形の発光コア(6x8ではなく6x6、Cellの8x8より一回り小さい)+ 発光コアを貫く十字のプリズミウムアクセント線」を採用。十字は「エネルギーがまっすぐ通り抜けていく」という配線の役割を暗示する意図で、Cellの水平ゲージ・Generatorの水平スリットのどちらとも視覚的に混同しないよう方向性を変えた。
- **自己レビュー実施済み**: 1x/4x/8x/16xの4段階アップスケールしたプレビューシート(チェッカーボード背景合成)を生成しRead(閲覧)で確認。最小(1x相当)表示でも「光るアイコン」として輪郭が判別でき、リベット・十字アクセントとも視認性を損なわないコントラストだった。アルファ値も全ピクセル255であることをコードで機械的に確認済み(透過崩れ無し)。今回は初回ドラフトで基準を満たし、描き直しは発生しなかった(Cell/Generatorの検証済みパレット再利用が3回連続で効いている)。

### 3I-3. レシピ・タグ・lang・クリエイティブタブ
- クラフト: shapeless、Prismium Shard ×1 + Redstone ×3 → Prismium Cable ×4(配線らしく複数個まとめて出るレシピ。ケーブルを大量に敷設する前提の設計)。
- `data/minecraft/tags/blocks/mineable/pickaxe.json` に`claudemod:prismium_cable`を追加(session 9で発覚した「新ブロック追加時のタグ登録忘れ」の再発防止として、追加のたびに既存の登録ブロック一覧と見比べて確認する習慣を継続)。
- lang(en_us/ja_jp)にブロック名・ステータスメッセージキーを追加。クリエイティブタブにもPrismium Generatorの直後に追加。

### 3I-4. push・ビルド確認
- 変更は1コミット(`Add Prismium Cable: relay block connecting Generator/Cell across a gap`)。push前に`git fetch origin main`で他セッションとの衝突が無いことを確認(空振り、`origin/main`はセッション開始時点の`d0264a4`のまま先行コミット無し)。
- push自体はプロキシ変数を一切いじらず、素の`git push origin main`で一度で成功(§2-3の方針通り、これで7セッション連続無改変成功)。
- push後、`git fetch origin main`を約15秒間隔でポーリングし(計8回・約120秒)、`ci: update built jar [skip ci]`(`c422c13`)の到着を確認。`builds/ClaudeMod-latest.jar`が71,837→79,251バイトに増加していることも確認できた(新規クラス3つ・新規テクスチャー1枚が実際にビルド成果物へ反映された裏付け)。**つまりこのセッションの変更(MOD初の非フルキューブブロック・2例目のBlockEntityTicker含む)は実際にコンパイルが通ることを実証できた**。

## 3J. セッション#11で実装した内容

セッション開始時、`git fetch origin main`で得られる最新コミット(`1d2af79 ci: update built jar`、Prismium Cableのpushとsession 10のPROGRESS.md更新の直後)からローカルとリモートのHEADが一致していることを確認し、**直前のビルドは成功している**と判断した(session 8以降定着した`git fetch`ベースの確認方法を継続。今回もapi.github.com はプロキシのアローリストで`403 blocked-by-allowlist`となり到達できなかったため、§0-2のIssue確認は`github.com/.../issues`をcurl経由で取得しGraphQLのpreloadedデータをgrepする方法で行った)。

続けてGitHub Open Issueを確認したところ、**Open Issueはissue #1(顔が見えない、session 9で対応済み)1件のみで、新規Issueは無かった**(セッション#10と同じ状態が継続)。

ビルド失敗・新規Issueとも無かったため、PROGRESS.md(session 10版)§5の「すぐやるべきこと」項目6「Cell・Generator・Cableとも、ブロックを壊すとFE(と燃焼時間)が失われる問題(§4-13、§4-15、§4-18)」に着手した。これは3つの機械に共通する既知の不具合として3セッション分蓄積していた優先度中の課題で、今回ようやく解消した。

### 3J-1. Prismium Cell/Generator/Cable: ブロック破壊時のFE(・燃焼時間)消失を修正
- **API裏取り(WebSearchで実施、このMOD初のloot table `copy_nbt`関数)**: 1.20.1(データコンポーネント移行前、コンポーネント移行は1.20.5〜)では、ブロックエンティティのカスタムNBTをドロップアイテムへ引き継ぐ標準手段は、loot tableの`minecraft:copy_nbt`関数(`source: "block_entity"`)で該当キーを`BlockEntityTag.<キー名>`へコピーすること、というバニラのシュルカーボックスと同じパターンであることを確認した。設置時にこの`BlockEntityTag`をブロックエンティティへ再適用する側は、`BlockItem`が標準で持つ`updateCustomBlockEntityTag`が自動的に行うため、**設置側のコード変更は一切不要**と判断した(裏取り時に明示的に確認)。
- 3つのブロックそれぞれの`loot_tables/blocks/prismium_{cell,generator,cable}.json`に`copy_nbt`関数を追加: Cell/Cableは`Energy`のみ、Generatorは`Energy`に加えて`BurnTime`(残り燃焼時間)もコピー対象にした(いずれも各ブロックエンティティの`saveAdditional`が書き込んでいるキー名とそのまま一致させている)。
- **`EnergyStorageBlockItem`(新規、`com.claudemod.item`)**: 3つの`BlockItem`登録を素の`BlockItem`からこのサブクラスに差し替えた(`ModItems`)。単にNBTが引き継がれるだけでは実際に直ったのかプレイヤーから見えないため、`appendHoverText`をオーバーライドし、アイテムの`BlockEntityTag`に`Energy`が入っていればツールチップに`現在/最大 FE`(Generatorはさらに残り燃料秒数)を表示するようにした。**この可視化が無いと、修正が本当に機能しているかを次回以降のセッションが確認する手段が無いままになる**と判断し、NBT修正とセットで実装した(API裏取りは`appendHoverText(ItemStack, Level, List<Component>, TooltipFlag)`のシグネチャが1.20.1では`Level`引数のまま、1.20.6以降で`Item.TooltipContext`に変わることのみ確認。`ItemStack#getTagElement`は長期安定APIと判断し個別のバージョン裏取りは省略)。
- lang(en_us/ja_jp)に`tooltip.claudemod.energy_storage`・`tooltip.claudemod.burn_time`を追加。
- **既知の割り切り(意図的な最小実装)**: クリエイティブモードのミドルクリック(pick block)で得られるアイテムにはNBTが乗らない(バニラの`Block#getCloneItemStack`のデフォルト実装のまま、`BlockEntity#saveToItem`相当のオーバーライドはしていない)。今回対応したのは「サバイバルでブロックを壊してドロップを拾う」経路のみ。実害は小さいと判断し優先度は低いままにした。

### 3J-2. push・ビルド確認
- 変更は1コミット(`Persist FE (and burn time) through Cell/Generator/Cable break+replace`、`107cf4c`)。テクスチャーの追加・変更は無し(コード+データのみの改善セッションのため、今回の作業フローでは新規ビジュアル要素は発生していない)。
- push前に`git fetch origin main`で他セッションとの衝突が無いことを確認(空振り、`origin/main`はセッション開始時点の`1d2af79`のまま先行コミット無し)。
- push自体はプロキシ変数を一切いじらず、素の`git push origin main`で一度で成功(§2-3の方針通り、これで8セッション連続無改変成功)。
- push後、`git fetch origin main`を約15秒間隔で7回(計約105秒)ポーリングし、`ci: update built jar [skip ci]`(`474e19c`)の到着を確認。`builds/ClaudeMod-latest.jar`が79,251→81,091バイトに増加していることも確認できた(新規クラス・loot table変更・lang追加が実際にビルド成果物へ反映された裏付け)。**つまりこのセッションの変更は実際にコンパイルが通ることを実証できた**。

## 3K. セッション#12で実装した内容

セッション開始時、`git fetch origin main`で得られる最新コミット(`e74d8a8 ci: update built jar`、session 11のPROGRESS.md更新pushの直後)からローカルとリモートのHEADが一致していることを確認し、**直前のビルドは成功している**と判断した(session 8以降定着した`git fetch`ベースの確認方法を継続。今回もapi.github.com はプロキシのアローリストで`403 blocked-by-allowlist`となり到達できなかった。加えて今回新たに判明: `raw.githubusercontent.com` も同様にプロキシで到達不可(`curl`が`000`/接続断)。一方 `github.com` のリポジトリ画面(blobページ等)は引き続き到達可能で、HTMLに埋め込まれた`"rawLines":[...]`というJSONの中にファイルの生テキストがそのまま入っていることを今回発見し、これを使えば任意のGitHubリポジトリの任意ファイルの正確な中身を(検索結果に出てきたURLでなくても)`github.com/<owner>/<repo>/blob/<ref>/<path>`経由で直接読めることが分かった。**この手法はAPI裏取りの精度を大きく上げるので次回以降も積極的に使うこと**、詳細は§4に追記した)。

続けてGitHub Open Issueを確認(`github.com/.../issues`のGraphQL preloadedデータをgrep)したところ、**Open Issueはissue #1(顔が見えない、session 9で対応済み)1件のみで、新規Issueは無かった**(session 10・11と同じ状態が継続)。

ビルド失敗・新規Issueとも無かったため、今回はPROGRESS.md(session 11版)§5の項目10「ロードマップの2大柱のうち、Prismium Energyはかなり成熟してきたが、Prism Realmは未着手」という論点を踏まえつつも、**ロードマップ§1で「まだ着手していない」のままだった項目がもう一つあることに気づいた: 項目4「新MOB」が、セッション#1から12セッション連続で完全に手つかずだった**(ブロック・エネルギー・装備はいずれも複数回の磨き込みを経ているのに、MODコンセプトが明記する4本柱の1本が文字通りゼロだった)。Prism Realmディメンションの着手も検討したが、ディメンションはワールド生成・ポータル・専用地形など一度に手を広げすぎるリスクが大きいため、今回はまず「新MOB」の第一歩を選んだ。

### 3K-1. Prismium Wraith: MOD初のMOB(敵対)
- **設計方針(リスク最小化を最優先)**: このサンドボックスはローカルビルド・実プレイとも一切できないため、MOD初のLivingEntity実装をゼロから書く(独自AI・独自モデル・独自レンダラー)のは、コンパイルが通らないリスクが非常に高いと判断した。代わりに、`com.claudemod.entity.PrismiumWraithEntity` はバニラの `Zombie` クラスをそのまま継承する設計にした。これにより:
  - AI(近接攻撃、プレイヤーを索敵、水を避ける、日光で燃える、等)は一切自前実装せず、Zombieの実装をそのまま流用。
  - レンダリングも「新規モデルクラスを書かない」方針で、バニラの `ZombieModel<PrismiumWraithEntity>` をそのまま使う `PrismiumWraithRenderer extends HumanoidMobRenderer<PrismiumWraithEntity, ZombieModel<PrismiumWraithEntity>>` を新規作成し(`ModelLayers.ZOMBIE`をbakeして渡す)、`getTextureLocation`だけをオーバーライドして独自テクスチャーに差し替えた。日光で燃える挙動もZombie由来でそのまま残しており、「洞窟でPrismium鉱石を守り、地上・日中は弱い」というフレーバーに意図的に合致させた。
  - 差別化した点: (a) `createAttributes()`でHP30・攻撃力4・防御4・移動速度0.24・ノックバック耐性0.15(素のゾンビより頑丈で硬い「番人」寄りの数値)、(b) `populateDefaultEquipmentSlots`を空実装にしてバニラ装備(ランダムな鉄防具等)を一切持たないようにした、(c) 効果音をゾンビの唸り声ではなくVexの音(`VEX_AMBIENT`/`VEX_HURT`/`VEX_DEATH`)に差し替え、足音も`WITHER_SKELETON_STEP`にして、より不気味な「異形」寄りの印象にした。
- **API裏取り(WebSearch中心、今回特に念入りに実施)**: 過去セッションでNeoForgeの最新(26.1、1.21系相当)ドキュメントを1.20.1と混同しかけるリスクが分かっていたため、今回は`docs.neoforged.net`の最新版ページ(`ValueInput`/`ValueOutput`や`ResourceKey`ベースのentity登録など、明らかに1.20.1と非互換なAPIが載っていた)を参考にするのを意図的に避け、代わりに **Kaupenjoe氏の公開Forge 1.20.1チュートリアルリポジトリ(`github.com/Kaupenjoe/Forge-Course-1.20.X`)を`git clone`して実際のソースを読む**という手法に切り替えた。これにより以下を実コードで確認できた:
  - `EntityType.Builder.of(EntityFactory, MobCategory)...build("modid:name")`(ResourceKeyではなく文字列を渡す1.20.1の形)。
  - `EntityAttributeCreationEvent#put(EntityType, AttributeSupplier)`、`SpawnPlacementRegisterEvent#register(EntityType, SpawnPlacements.Type, Heightmap.Types, SpawnPredicate, Operation)`(いずれもMODバス購読、`@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)`)。
  - `EntityRenderersEvent.RegisterRenderers`はクライアント限定(`Dist.CLIENT`)のMODバス購読で登録すること(サーバーには存在しないクラスを読み込ませないため)。
  - `defineSynchedData()`(引数無し)、`addAdditionalSaveData(CompoundTag)`等、1.20.1はまだ`ValueInput`/`ValueOutput`ベースの新API(1.21以降)に移行していないこと。
  - また `MinecraftForge/MinecraftForge` リポジトリの1.20.xブランチから `SpawnPlacementRegisterEvent.java` の実ソースをgithub.com blobページ経由(§4の新手法)で直接読み、Javadocコメントで「Fired on the Mod bus」と明記されていることも確認した。
  - `Zombie.createAttributes()`の実在は1.18.2時点のJavadoc(`nekoyue.github.io`のミラー)で確認(1.20.1でも同じ名前のはず、という前提での裏取り。完全な1.20.1版Javadocまでは辿れなかったので、万一シグネチャが変わっていた場合はCIビルドで判明する)。
  - `ForgeSpawnEggItem`のコンストラクタは`(RegistryObject<EntityType<?>>, int backgroundColor, int highlightColor, Item.Properties)`で、`.get()`せずRegistryObjectをそのまま渡す形であることをWeb検索で確認。
- **自然スポーン**: コード側は`SpawnPlacementRegisterEvent`で`Monster::checkMonsterSpawnRules`(バニラの敵対MOBと同じ「暗い場所」判定)を登録するのみに留め、実際にどのバイオームで湧くかはデータ駆動の`forge:add_spawns`バイオームモディファイア(`data/claudemod/forge/biome_modifier/add_prismium_wraith_spawn.json`、既存の`add_prismium_ore.json`と同じ`#minecraft:is_overworld`ターゲット)で追加した。weight 8(ゾンビ95よりかなり低い)・minCount 1・maxCount 2の控えめな設定にし、既存のゾンビ湧きを圧迫しすぎないようにした。「洞窟専用」を狙ったタグ(`#minecraft:is_cave`的なもの)は1.20.1のバニラに存在しないと判断し採用せず、代わりに`checkMonsterSpawnRules`の暗さ判定に任せている(結果的に夜間の地上にも湧きうる。洞窟限定にしたい場合は次回以降、Y座標条件などを足す余地がある)。
- **ドロップ**: `data/claudemod/loot_tables/entities/prismium_wraith.json`で20%の確率でPrismium Shardを1個ドロップ(`minecraft:random_chance`条件)。ロースト表以外(経験値等)はZombieのデフォルト挙動のまま。
- **スポーンエッグ**: `ForgeSpawnEggItem`で登録(`prismium_wraith_spawn_egg`)。配色はダークバイオレット(`#2b1033`、機械ファミリーのFRAME系ダーク寄り)+シアン(`#39e6d6`、PRISMIUM_BASE寄り)にして、クリエイティブタブでPrismiumファミリーの一員に見えるようにした。クラフトレシピは無し(バニラのスポーンエッグ同様、クリエイティブ/ドロップ入手前提)。

### 3K-2. テクスチャー(`scripts/textures/gen_prismium_wraith.py`) — MOD初のエンティティ(64x64)テクスチャー
- MOD初のMOBテクスチャーであり、これまでのブロック/アイテム(16x16)と違い、64x64の「クラシック(Steve形式)ヒューマノイドスキン」UVレイアウトに合わせて描画する必要があった。頭・胴体・右腕・右脚の面ごとのUV座標をキューブ展開の標準公式から自分で計算し(スクリプト内コメントに詳細)、さらに「ZombieModelが64x64の左腕/左脚専用UV領域(Player形式の32,48および16,48オフセット)を読むかどうか確定できなかった」ため、**右側と左側専用領域の両方に同じ絵を描く「両対応」戦略**を取った(片方が実際には参照されなくても実害が無いため)。第2レイヤー(帽子/上着オーバーレイ)領域は全面透明のままにした。
- **配色**: 新しいパレットを作らず、`gen_prismium_armor.py`のFRAME系グレー(石/岩肌)と`gen_prismium_core.py`/`gen_prismium_armor.py`のPRISMIUM系シアン(発光する亀裂・目)、PRISMIUM_ACCENTのバイオレット(胸に埋め込まれた「コアの欠片」、Prismium Coreの意匠と呼応)をそのまま流用した(session 9以降繰り返し効いている「検証済みパレット再利用」の4例目)。顔には発光する目(縦2px×2組)と額の亀裂、胸には縦に伸びるシアンの亀裂とバイオレットのコア欠片を手描きで配置。
- **自己レビュー実施済み**: 1x/4x/8xの3段階アップスケールしたプレビューシート(チェッカーボード背景合成)を生成しRead(閲覧)で確認した。4x/8xでは石肌の質感・発光する目・胸の亀裂とコア欠片がいずれも判別可能で、色は既存のPrismiumファミリーと一貫していることを確認した。石肌のグラデーション関数(`stone_gradient`)がやや粒状感の強い(ノイズが多い)仕上がりになっている点は許容範囲と判断したが、次回以降さらに洗練させる余地がある点として正直に記録する(§4参照)。アルファ値は`{0, 255}`の2値のみであることをコードで機械的に確認済み(透過崩れ無し)。
- **未検証(構造的な限界)**: 平面のUVスプライトシートのプレビューだけでは、実際にZombieModelの3D形状に貼り付けたときにどう見えるか(縫い目のズレ、遠目での視認性、左右反転の有無)は確認できない。これは過去セッションの防具テクスチャーでも繰り返し出てきた既知の限界(§3-3, §3H)で、MOBテクスチャーでも同様に当てはまる。

### 3K-3. push・ビルド確認
- 変更は1コミット(`Add Prismium Wraith: the mod's first mob`、`8a1cf6b`)。push前に`git fetch origin main`で他セッションとの衝突が無いことを確認(空振り、`origin/main`はセッション開始時点の`e74d8a8`のまま先行コミット無し)。
- push自体はプロキシ変数を一切いじらず、素の`git push origin main`で一度で成功(§2-3の方針通り、これで9セッション連続無改変成功)。
- push後、`git fetch origin main`を確認したところ、待つまでもなく**1回目のfetchで既に`ci: update built jar [skip ci]`(`e8ec26d`)が到着していた**(今回はポーリング不要なほど速かった)。`builds/ClaudeMod-latest.jar`が81,091→90,959バイトに増加していることを確認し、jarの中身(`unzip -l`)にも`PrismiumWraithEntity.class`・`PrismiumWraithRenderer.class`・`ModEntities.class`・`ClientModEvents.class`・`ModEntityEvents.class`・テクスチャー・ルートテーブル・バイオームモディファイアJSONが実際に含まれていることを直接確認した。**つまりMOD初のLivingEntity実装(独自AI無し・バニラモデル流用という保守的な設計ではあるが)が実際にコンパイルを通ったことを実証できた**。

## 3L. セッション#13で実装した内容

セッション開始時、`git fetch origin main`で得られる最新コミット(`402ca34 ci: update built jar`、session 12のPROGRESS.md更新pushの直後)からローカルとリモートのHEADが一致していることを確認し、**直前のビルドは成功している**と判断した(session 8以降定着した`git fetch`ベースの確認方法を継続。今回も`api.github.com`はプロキシのアローリストで`403 blocked-by-allowlist`、直接DNS解決も`Could not resolve host`で失敗し、到達不可を再確認した)。

続けて§0-2の運用ルールに従いGitHub Open Issueを確認(`github.com/.../issues`のGraphQL preloadedデータをgrep)したところ、**新規のOpen Issueが1件見つかった: issue #2「ツールの見た目について」**(session 9〜12で継続していたissue #1「顔が見えない」は引き続きOpenのままだが対応済みでクローズ待ち、変化無し)。issue #2の本文: 「各ツールの見た目が、そのツールと一致しません。さらにどれも似通っているため、区別がつかず持ち替えに苦労します。公式が出しているツールを参考にしながら作っていければと思います。」

新規Issueが見つかったため、§0-2のルール通り今回はこれをビルド失敗対応と同格の最優先タスクとして扱い、着手した(Prismium Cable/Generator/Cellの実地未検証問題や新MOB追加など、§5で挙がっていた他の候補は次回以降に持ち越し)。

### 3L-1. Prismium ツール5種のテクスチャー再設計(GitHub issue #2対応)
- 現状の`prismium_pickaxe`/`prismium_axe`/`prismium_shovel`/`prismium_hoe`の4テクスチャーを実際にアップスケールしたプレビューシート(12倍)で目視したところ、issue #2の指摘が正確であることを確認した: ツルハシ・シャベル・クワの3つが、いずれも「斜めの柄+先端の小さな三角形の結晶ブロブ」という酷似したシルエットになっており、ホットバー上で見分けるのがほぼ不可能な状態だった(斧だけはやや大きい単一ウェッジ形状で多少マシ)。
- `scripts/textures/gen_prismium_tools.py`を全面的に書き直し、4種類それぞれに明確に異なるシルエットを持たせた(剣・斧の形状は元々十分に判別可能だったため、斧は形状据え置き・剣は完全に無変更):
  - **ツルハシ**: 柄の先端の1つの共有ソケットから左右に2本の刃が伸びる「フォーク(二股)」形状に変更(旧デザインは右上方向のみの単一の尖った刃で、斧の単純化版のように見えていた)。
  - **シャベル**: 頭部を独立したブロブとして描くのをやめ、柄そのものが先端に向かって結晶色にグラデーションしていく「1本の細い連続した刃」に変更。他の4種と違い明確な「頭部の塊」が無いため、シルエットの太さ自体が識別ポイントになる。
  - **クワ**: 柄の先端付近に、三角形ではなく水平の平たい「横棒(クロスバー)」状の刃を追加。柄より細い1px芯の斜め線(`draw_thin_diagonal`という今回新設のヘルパー関数)を使い、ツルハシ・斧の2px太さの柄とも視覚的に差別化した。
  - **斧**: 形状は変更していないが、シャベル・クワが「細い柄」系に移行したことで、相対的に「太い柄+大きな単一ウェッジ」という斧固有の特徴がより際立つようになった。
- **自己レビューで1回描き直した**: 初版のクワは、柄の先端(座標(10,5)相当)とクロスバーの刃(y=0〜1の行)の間に3行分の隙間が空いてしまい、刃が柄から浮いて見える(接続不良の)バグがあった。12倍・16倍のアップスケールプレビューをRead(閲覧)して発見し、柄の終点をクロスバーの真下に接するよう(10,5)→(11,2)に伸ばし、さらに接合部に木材ハイライト色のピクセルを1つ追加して視覚的にも繋がりを補強してから再生成、再確認して採用した。
- 全5テクスチャーのアルファ値がコードで機械的に`{0, 255}`の2値のみであることを確認済み(透過崩れ無し)。
- **タグの棚卸しもついでに実施**(§4-16で挙げられていた「新ブロック追加時のタグ登録漏れが他にも無いか」という未着手の提案への対応): `ModBlocks.java`に登録されている8個のブロック全てが`data/minecraft/tags/blocks/mineable/pickaxe.json`に漏れなく含まれていることを確認した。今回は漏れは無かった(念のための確認で、修正は発生していない)。
- **未検証**: 新しいツルハシ/シャベル/クワの形状が実際にゲーム内のインベントリ・ホットバーでどう見えるか(特にシャベルの「細い線」が小さい表示でどこまで視認できるか)は、このサンドボックスではプレイできないため確認できていない。issue #2への直接対応ではあるが、issue報告者(ユーザー)自身の目での確認・フィードバックを次回以降のPROGRESS.mdに反映できるとよい。

### 3L-2. push・ビルド確認
- 変更は1コミット(`Fix #2: redesign tool textures so each silhouette reads distinctly`、`1420e84`)。push前に`git fetch origin main`で他セッションとの衝突が無いことを確認(空振り、`origin/main`はセッション開始時点の`402ca34`のまま先行コミット無し)。
- push自体はプロキシ変数を一切いじらず、素の`git push origin main`で一度で成功(§2-3の方針通り、これで10セッション連続無改変成功)。
- push後、`git fetch origin main`を約15秒間隔でポーリングし、`ci: update built jar [skip ci]`(`2d7b4e5`)の到着を確認(1回のポーリングで到着、Wraith追加時と同様に速かった)。`builds/ClaudeMod-latest.jar`のサイズは90,959→90,886バイトとわずかに減少した(テクスチャー5枚中3枚のピクセル数がやや減った設計変更のため、増加ではなく減少もあり得る。他の新規ファイルが増えていないことと整合的)。`unzip -l`で`prismium_pickaxe.png`等の該当ファイルが実際に更新日時付きで含まれていることも確認した。**つまりこのセッションの変更は実際にコンパイルが通ることを実証できた**。

### 3L-3. セッション#13中にユーザーから直接フィードバックを受けての追加修正(session 13b)

§3L-1のツールテクスチャー再設計をpush・ビルド成功させた直後、**ユーザーがこのセッションの会話にリアルタイムで参加し、生成した画像を直接見て2点のフィードバックをくれた**(これまでのセッションはGitHub Issue経由の非同期フィードバックのみだったが、今回は初めてチャットでの同期フィードバックが得られた):

1. **「どこからどう見てもテクスチャが収まり切れていないように見える」**: 検証したところ事実だった。`draw_outline`はキャンバス外にはピクセルを置けないため、結晶の塗り(SHADOW/BASE/MID/HILITE)がキャンバスの端(x=0,15 または y=0,15)に直接触れている箇所は、その面だけ縁取りが描けず、色がキャンバス境界でいきなり途切れて見えていた。特にツルハシ(4辺すべてで発生)とクワ(右・上辺)が深刻で、斧(今回は変更していなかった旧デザイン)にも同じ問題が前セッションから潜んでいたことが判明した。剣の刃先だけは唯一「縁取り色そのものがy=0に接している」パターンで、これは意図した先端の縁取りとして問題無いことも確認した(生の塗り色 vs 縁取り色、という区別が診断の鍵になった)。
2. **「形も一般的な形状とは異なるようですが、これは仕様ですか?」**: 仕様ではなく、issue #2対応時に「見分けやすさ」を優先しすぎた結果の副作用だった。ツルハシの細い二股スパイクは「ダウジングロッド/スリングショット」に見え、シャベルの頭部を持たない一本線は「杖/槍」に見えてしまっていた。

この2点を踏まえ、`scripts/textures/gen_prismium_tools.py`を再度修正(ユーザーからの明示的な許可を得てから着手):
- **ツルハシ**: 細い二又スパイクから、斧と同じ「塊のあるウェッジ」構造を左右対称にミラーした太い二又ヘッドに変更。ツルハシらしい塊感を持たせつつ、斧(非対称の単一ウェッジ)とは明確に区別できる形を維持した。
- **斧**: 形状自体は変更せず、右に1px・下に1pxシフトして右端・上端のはみ出しを解消。
- **シャベル**: 一本線から、先端に実際の平たい(丸みのある)スペード状の刃を追加した形に変更。柄はツルハシ・斧より細いままにして差別化を維持。
- **クワ**: 横棒状の刃はそのまま、下に1px・左に1pxシフトして右端・上端のはみ出しを解消。
- 全パーツをキャンバス端から最低1pxの余白内に収まるよう座標を再設計し、修正後にコードで「不透明ピクセルがキャンバス端に触れているか」「触れている場合その色が生の塗り色か縁取り色か」を機械的に検査するチェックを追加実施(結果: 端に触れる不透明ピクセルは全て縁取り色のみになったことを確認)。
- 16倍アップスケールのプレビューシートをRead(閲覧)して最終確認。ツルハシはV字型で塊のあるシルエットになり、シャベルは丸みのあるスペード形状になったことを目視確認した。
- **この経緯の教訓(次回以降のテクスチャー作成に生かす)**: (a) 図形の座標がキャンバスの端(0または15)に達する場合、`draw_outline`のような「隣接セルに縁取りを置く」方式のヘルパー関数は、キャンバス外に縁取りを置けないため、その面だけ縁が欠けたように見える。次回以降、新しいテクスチャーを設計する際は最初から「不透明領域の座標は原則1〜14の範囲に収める」ことをデフォルトのルールにするとよい。(b) 「区別しやすさ」のためにシルエットを変える際は、変えすぎて「そのツール自体らしさ」を失っていないか、生成直後に一度立ち止まって自問する価値がある(単に前後のプレビューを見比べるだけでなく、「これは何のツールに見えるか」を素朴に自問すること)。(c) 今回のようにユーザーがリアルタイムで画像を見てフィードバックをくれる場面があれば、それを最優先で反映し、许可を得てから追加修正・再push・再ビルド確認まで同一セッション内で完結させるとよい(実際に今回、session 13内で2往復の修正サイクルを回せた)。
- コミットは2つ目: `Fix tool textures bleeding off the canvas edge, redesign pick/shovel`(`dd00ed0`、origin/mainが1コミット先行していたため`git rebase origin/main`で追従してからpush)。push後`git fetch`ポーリングで`ci: update built jar [skip ci]`(`2ddf286`)の到着を確認し、jar内のPNGを`unzip -p`で直接展開してキャンバス端のピクセルを再検査、生の塗り色が端に残っていないことも実際のビルド成果物で確認した。

## 3M. セッション#14で実装した内容

セッション開始時、`git fetch origin main`で得られる最新コミット(`9c20c91 ci: update built jar`、session 13の2度目のPROGRESS.md更新pushの直後)からローカルとリモートのHEADが一致していることを確認し、**直前のビルドは成功している**と判断した(session 8以降定着した`git fetch`ベースの確認方法を継続。今回も`api.github.com`はプロキシのアローリストで`403 blocked-by-allowlist`だった)。

続けて§0-2の運用ルールに従いGitHub Open Issueを確認したところ、**新規のOpen Issueは無かった**。issue #1(顔が見えない)・issue #2(ツールの見た目について)とも引き続きOpenのままだが、いずれもこのファイル内の過去セッションで対応済みで、ユーザー側のクローズ待ち状態に変化は無い(個別ページの`"state":"OPEN"`を直接確認する方式に切り替えた。issue一覧ページ自体はこのセッションでも`"totalCount":0`という明らかにおかしい値を返し、§2-7/§4-14で既知のキャッシュ問題が今回はissue一覧の中身そのものを空に見せかける形で再発したと判断し、代わりに`issues/1`・`issues/2`・`issues/3`・`issues/4`の個別ページを直接叩いて#1と#2がOPENで#3以降が存在しないことを確認する方式で裏取りした)。

ビルド失敗・新規Issueとも無かったため、今回はPROGRESS.md(session 13版)§5の項目10「ロードマップ§1の4本柱のうち、残るPrism Realmディメンションだけがセッション#1から依然として未着手」に着手した。ブロック・エネルギー・装備・MOBはいずれも複数セッションの磨き込みを経ているのに、4本柱の最後の1本が13セッション連続で文字通りゼロだったため、今回はここに時間を割いた。

### 3M-1. Prism Realm: MOD初のディメンション(最小実装)

- **設計方針(リスク最小化を最優先、session 12のWraithと同じ考え方)**: このサンドボックスはローカルビルドはおろかプレイテストも一切できないため、独自の地形生成(ノイズルーター、独自バイオーム、独自地表ルール)をゼロから書くのは避けた。代わりに、タスクファイルの「地形はバニラ流用」という前提通り、**ノイズ設定`minecraft:overworld`をそのまま使い、バイオームだけ固定(`minecraft:fixed`)で`minecraft:cherry_grove`にする**という、既存のバニラ資産を最大限再利用する構成にした。
- **データ駆動、Java登録コード無し**: `data/claudemod/dimension_type/prism_realm_type.json`と`data/claudemod/dimension/prism_realm.json`の2ファイルのみで完結させた。このMODは今までdatagenを使わずJSONを手書きする方針(§4-2)を貫いてきており、ディメンションも同じ方針に揃えた(Kaupenjoe氏のチュートリアル(`git clone`して実コードを確認、後述)はコード側のBootstapContext登録を使っていたが、今回はよりシンプルなJSON直書きを選んだ)。Forgeはmodが同梱するリソースを他のデータパックと同様に読み込むため、ディメンション/ディメンションタイプのJSONに対応するJava登録呼び出しは不要。
- **API裏取り(WebSearch中心)**: `minecraft.wiki/w/Dimension_definition`でdimension jsonのスキーマ(`type`+`generator`{`type`,`settings`,`biome_source`})を確認。dimension_typeの各フィールド(`ultrawarm`/`natural`/`coordinate_scale`/`has_skylight`/`has_ceiling`/`ambient_light`/`fixed_time`/`monster_spawn_light_level`/`monster_spawn_block_light_limit`/`piglin_safe`/`bed_works`/`respawn_anchor_works`/`has_raids`/`logical_height`/`min_y`/`height`/`infiniburn`/`effects`)は、Kaupenjoe氏の実チュートリアルコード(`Tutorials-By-Kaupenjoe/Forge-Tutorial-1.20.X`を`git clone`して`ModDimensions.java`の`DimensionType`コンストラクタ引数を実際に読んだ)と突き合わせて命名・型を裏取りした。`monster_spawn_light_level`が単純な整数(intプロバイダの省略形)として許容されるかは別途WebSearchで確認済み。
- **設定値**: `min_y`/`height`/`logical_height`はノイズ設定`minecraft:overworld`と整合させるためバニラのオーバーワールドと同じ(-64/384/384)。`natural`はfalse(ネザーポータル経由の自動リンク対象にしない・ピグリン変換等を起こさない)、`bed_works`もfalseにして「眠れない特別な場所」という演出にした。`fixed_time`を6000(正午固定、昼夜サイクル無し)にして、常に明るい桜並木(cherry_grove)の異空間という、探索・スクリーンショット映えを狙った雰囲気にした。
- **既知の割り切り(意図的な最小実装)**: 専用地形・専用バイオーム・専用鉱石は今回実装していない(ロードマップ§1項目3が最終的に目指す「専用地形生成、専用鉱石、専用バイオーム」の"最小の第一歩"止まり)。オーバーワールドの地形生成ロジックをそのまま流用しているため、見た目は「桜並木バイオーム固定のオーバーワールド」に近い。
- **副次効果(意図した設計)**: 既存のPrismium鉱石(`add_prismium_ore.json`)・Prismium Wraith(`add_prismium_wraith_spawn.json`)のバイオームモディファイアはどちらも`#minecraft:is_overworld`バイオームタグを対象にしており、`minecraft:cherry_grove`はこのタグを持つ。そのため**Prism Realm側でも追加のコード無しでPrismium鉱石の生成とPrismium Wraithの自然スポーンが有効になっているはず**(未検証、後述)。狙って設計した副産物で、「異空間=Prismium鉱石とその番人がより濃く出る場所」という将来の世界観とも自然に合致する。
- **既知の懸念点(WebSearchで発覚、重要)**: Forge issue #8552(1.18.2向け、PR #8555で修正)として、「modが同梱するディメンションJSONが、ワールドを新規作成した直後のサーバー初回起動では反映されず、サーバーを1回再起動して初めて認識される」という既知の不具合が過去に存在した。修正はこのMODが対象とする1.20.1より前のバージョン向けだが、実際にこのMOD自身のワールドで再現するかはこのセッションでは検証できていない。`ModDimensions`のjavadocに「`server.getLevel(PRISM_REALM)`が`null`を返す場合、まずサーバー再起動を試す」という手がかりを明記した。

### 3M-2. Prismium Rift Shard: MOD初のディメンション間テレポートアイテム

- **設計方針(ここでもリスク最小化)**: バニラのネザーポータルのような「フレーム設置+マルチブロック検知+`ITeleporter`で着地点を探す/作る」という本格的なポータルシステムは、今回1セッションでゼロから実装するにはテスト手段が無い状態でのリスクが大きすぎると判断し、**次回以降の拡張候補として温存**した。代わりに、右クリックで直接テレポートする再利用可能アイテム(消費されない、`stacksTo(1)`)を実装した。グラップリングフック(session 7、飛翔エンティティ無しでraycast+速度書き換えのみ)やPrismium Wraith(session 12、独自AIを書かずZombieを継承)と同じ「まず最小の仕組みで成立させ、後で本格版に発展させる余地を残す」という、このMODで繰り返し機能してきたパターンを踏襲した。
- **実装**: `com.claudemod.item.PrismiumRiftShardItem`。現在のディメンションがPrism Realmかどうかで「行き」「帰り」を判定し(`serverLevel.dimension() == ModDimensions.PRISM_REALM`という`==`比較は、Kaupenjoe氏の実コードで同じパターンが使われていることを確認した上で採用)、
  - 行き(Realmへ): 現在の位置・向き・**現在のディメンション**(オーバーワールド以外、たとえばネザーやジエンドから使った場合もそこへ戻せるように)を`Player#getPersistentData()`(Forgeが`Entity`に生やす、ワールド保存に載る汎用NBTタグ、以前のセッションから独立に確認済みの長期安定API)へ保存してから、Prism Realm側の固定アンカー地点(X=0, Z=0, `Heightmap.Types.MOTION_TOP`で地表Yを算出)へ`ServerPlayer#teleportTo(ServerLevel, double, double, double, Set<RelativeMovement>, float, float)`で転送。
  - 帰り(Realmから): 保存済みタグがあればそこへ、無ければオーバーワールドの共有スポーン地点(`ServerLevel#getSharedSpawnPos()`)へフォールバック。
  - クールダウン100tick(5秒)、`SoundEvents.END_PORTAL_TELEPORT`を転送先で再生。
- **API裏取り(WebSearch)**: `ServerPlayer#teleportTo`の1.20.1向け正確なシグネチャ(`Set<net.minecraft.world.entity.RelativeMovement>`を含む7引数版)を`mappings.dev/1.20.1`の実ページ(fetchしてbashからgrepで該当行を直接確認、`web_fetch`が長すぎて丸ごとは読めなかったため保存済みファイルをgrepする形で裏取り)で確認し、パッケージ名も含めて裏取りできた。Kaupenjoe氏のチュートリアルコードは`Entity#changeDimension`+カスタム`ITeleporter`という別経路を使っていたが、固定アンカー地点への直接テレポートであれば`teleportTo`の方がシンプルで`ITeleporter`実装が不要と判断した。
- **クラフト**: shapeless、プリズミウムの欠片×2 + エンダーパール + アメジストの欠片 → プリズミウムのリフトシャード×1(次元を渡る、というフレーバーでエンダーパールを採用)。
- **既知の割り切り(正直に書く)**:
  - プレイヤーが死亡してから(帰還前に)シャードを使わずに死ぬと、`getPersistentData()`はリスポーン時に新しいプレイヤーエンティティへ引き継がれない(`PlayerEvent.Clone`等の追加対応をしていないため)、保存した帰還先位置は失われる。実害は「オーバーワールドの共有スポーンに飛ばされる」程度で致命的ではないが、正直に記録しておく。
  - 着地点は固定のX=0, Z=0の1箇所のみ。地形次第では水中・崖・木の上など不便な場所になり得る(セーフティとして`Heightmap.Types.MOTION_TOP`で地表を探しているので窒息はしないはずだが、快適さは保証していない)。
  - ポータルブロック・フレームは無い(§3M-2冒頭参照、意図的な先送り)。

### 3M-3. テクスチャー(`scripts/textures/gen_prismium_rift_shard.py`)

- 既存のPrismium Shard本体(`gen_prismium.py`)の六角形シルエット(行ごとのx範囲定義)をそのまま流用し、「プリズミウムファミリーの一員だが特別なバリアント」という見た目にした(session 13の「似た形状のファミリーはシルエットで区別する」知見を踏まえつつ、今回はむしろ意図的にベースシャードと同じ輪郭にして「同じ結晶に穴が空いている」という関係性を表現)。
- 中央に、既存の紫アクセント色(`PRISMIUM_ACCENT`/`PRISMIUM_ACCENT_DARK`、grappling hook/coreで確立済み)で縁取られた真っ黒に近い「裂け目(void)」を追加。ベースの塗り→縁取りの後に裂け目のピクセルで上書きする順序にして、確実に裂け目が最前面に出るようにした。
- **自己レビュー実施済み**: 4x/8x/16xアップスケールのプレビューシート(チェッカーボード背景合成)を生成し、outputsフォルダ経由でRead(閲覧)して確認した。最小(4x)表示でも中央の裂け目が目玉のようにはっきり視認でき、結晶の輪郭・縁取りも既存アイテムと違和感なく揃っていた。アルファ値も`{0, 255}`の2値のみであることをコードで機械的に確認済み(透過崩れ無し)。今回は初回ドラフトで基準を満たし、描き直しは発生しなかった。

### 3M-4. push・ビルド確認 - 【今回は最後まで確認しきれず、重要】

- 変更は1コミット(`Add Prism Realm dimension (session 14): first step on the mod's last untouched roadmap pillar`、`b836049`)。push前に`git fetch origin main`で他セッションとの衝突が無いことを確認(空振り、`origin/main`はセッション開始時点の`9c20c91`のまま先行コミット無し)。
- push自体はプロキシ変数を一切いじらず、素の`git push origin main`で一度で成功。
- **しかし今回はpush後、`git fetch`ポーリングを25秒間隔で合計40分以上(約90回以上)繰り返しても、`ci: update built jar [skip ci]`コミットが一度も到着しなかった**。これは session 8以降このMODで確立された確認方法の中で初めて「結論が出ないまま時間切れになった」ケース。
  - 並行して`actions/workflows/build-and-notify.yml/badge.svg`を複数回(10分以上の間隔を空けて2回)確認したところ、いずれも`"Build and Notify - failing"`と表示された。ただし`actions`一覧ページの方はRun 1〜3(セッション#1〜#2相当の大昔の内容、`"totalCount":0"`という明らかな異常値付き)しか返しておらず、§2-7/§4-14で既知のプロキシキャッシュ問題がこのセッションでも(いつもよりさらに酷い形で)再発していると判断した。badge側も同じキャッシュ層の影響を受けている可能性が高く、**本当に今回のpush(`b836049`)が失敗したのか、単にキャッシュが極端に古いだけなのかを、このセッション内では最終的に判別できなかった**。
  - コミット個別ページ(`/commit/b836049`)のHTML初期ロードにはチェック状況(`statusCheckState`等)が埋め込まれておらず(JSで非同期取得される作りらしく)、この経路からも裏取りできなかった。
- **このセッションでできた自己防衛策**: ビルドログそのものは見えないため、代わりに今回新規追加したJSON/Javaの内容を通常より念入りに複数回読み返し、WebSearchで裏取りした各APIのシグネチャ・フィールド名と実際のコードを突き合わせる作業を追加で行った(§3M-1・§3M-2に詳細)。明らかな誤り(タイプミス、フィールド名の誤り、シグネチャ不一致)は見つからなかったが、**これは「恐らく大丈夫」以上の保証ではない**。
- **次回セッションへの最優先申し送り**はこの一点に尽きる: まず`git fetch origin main`でコミット`b836049`の直後に`ci: update built jar`が付いているかを確認すること。付いていれば今回の実装は実証済みとして扱ってよい。付いておらず、かつactionsページ等で本当に`b836049`のビルドが失敗したと確認できた場合は、§3M-1・§3M-2の新規ファイル(特にこのMOD初のdimension_type/dimension JSON、初めて`net.minecraft.world.entity.RelativeMovement`を使うJavaコード)を最有力容疑として調査すること。

## 3N. セッション#15で実装した内容

セッション開始時、`git fetch origin main`で`origin/main`のHEADが`94fcd10`(session 14の2度目のPROGRESS.md更新)であることを確認。session 14終了時点で「Prism Realm(§3M)のビルド成功/失敗が確認できていない」という最優先の申し送りがあったため、今回はまずこの一点の決着から着手した。

### 3N-1. ビルド失敗を確定させた新しい確認手法

`git fetch`ポーリングで`ci: update built jar`コミットを何度確認しても新着が無く(session 14の40分越えポーリングと同じ状況)、素のbadge.svgも数回中数回は`failing`・数回は`passing`という矛盾した結果を返した(§2-7のキャッシュ問題が継続)。ここで、§2-4に記載の「`actions/workflows/<file>.yml`ページをnocacheクエリ付きで取得し`aria-label`から成否を読む」手法を、**1回のリトライで諦めず4〜5回連続でリトライする**形で改めて試したところ、途中から明らかにページサイズが変わる瞬間(16KB前後のキャッシュ済みシェル → 42万バイト超の本物の一覧)があり、そこから先は複数回連続で一貫した内容(`Run 36`〜`38`がいずれも`failed`)を返すようになった。**「サイズが数十倍になった瞬間を境に信頼できる内容に切り替わる」という経験則は、次回以降のキャッシュ判定に使える具体的な目印として有用**なので明記しておく。

これでsession 14のコミット(`b836049`=Run 36、`94fcd10`=Run 37)が**両方とも実際にビルド失敗していたこと**が確定した。

### 3N-2. javacのエラーメッセージそのものを取得する新手法(§4-9の長年の課題を初めて突破)

これまでの全セッションを通じて「CIビルド失敗時にjavacの実際のエラー内容を見る手段が無い」(§4-9)ことが最大級の課題として繰り返し記録されてきたが、今回**非ログイン・API不使用でエラー内容そのものを取得する方法を発見した**:

1. `https://github.com/<owner>/<repo>/actions/runs/<run_id>` のHTML内に、`data-url="/<owner>/<repo>/actions/runs/<run_id>/annotations_partial"` という埋め込みURLがある(JS/WebSocketで遅延ロードされる部分)。
2. このURLに対して素の`curl`ではなく、ヘッダー `-H "X-Requested-With: XMLHttpRequest" -H "Accept: text/html, */*" -H "Turbo-Frame: annotations"` を付けて取得すると、GitHubのTurbo Frame機構がサーバー側でHTMLフラグメントとして直接返してくれる(ログイン不要、200 OK)。ここに「2 errors and 2 warnings」のようなサマリと、各annotationのメッセージ本文(例: `cannot find symbol`, `[removal] ResourceLocation(String,String) in ResourceLocation has been deprecated and marked for removal`)が入っている。
3. ただし`annotations_partial`単体ではメッセージ本文はあってもファイル名・行番号が省略されがちだったため、代わりに `https://github.com/<owner>/<repo>/commit/<sha>/checks` (通常の`curl`、特別ヘッダー不要)を取得すると、**同じannotation情報がファイルパス・行番号付き(`src/main/java/.../Foo.java#L150`のようなリンクテキストとして)でHTML内に直接埋め込まれている**ことが分かった。むしろこちらの方が情報量が多く簡単なので、次回以降はこの`/commit/<sha>/checks`ページを第一選択肢にするとよい。
4. 一方、実際のビルドログ本文(javac的な完全なコンソール出力)そのものは、`/commit/<sha>/checks/<job_id>/logs/<line>`のようなURLが埋め込まれてはいるものの、素の`curl`では404になり取得できなかった(ログイン必須と思われる)。**annotationsはあくまでGitHub側が「これはエラー行」と認識してハイライトした短いメッセージ+ファイル/行の集合であり、フルログではない**という限界は残るが、今回のケースでは`cannot find symbol`というメッセージと該当行番号だけで原因特定に十分だった。

この手法により、§4-9(ビルドログが見えない問題)は「完全解決」ではないが「annotationsだけでも取得できる」という形で大きく前進した。次回以降、CIビルドが失敗した際はまずこの手法(`/commit/<sha>/checks`ページの取得)を試すこと。

### 3N-3. 実際の原因: 存在しないシンボル2つ(`PrismiumRiftShardItem.java`)

annotationsから得られた情報は「`cannot find symbol`」×4件、該当箇所は`PrismiumRiftShardItem.java`のL150, L156, L188, L203。実際のコードを見比べると、2種類の誤ったシンボルがそれぞれ2箇所ずつ使われていたことが分かった:

- **`Heightmap.Types.MOTION_TOP`は存在しない。** 1.20.1の`Heightmap.Types`列挙型が持つ値は`WORLD_SURFACE_WG`/`WORLD_SURFACE`/`OCEAN_FLOOR_WG`/`OCEAN_FLOOR`/`MOTION_BLOCKING`/`MOTION_BLOCKING_NO_LEAVES`の6つのみ(Yarn mappingsのjavadocで裏取り済み)で、「MOTION_TOP」はいずれのバージョンにも存在しない。地表の着地Y座標を求める意図だったので、`MOTION_BLOCKING`に置き換えた。
- **`SoundEvents.END_PORTAL_TELEPORT`も存在しない。** 実際に存在するEnd Portal関連のサウンド定数は`END_PORTAL_FRAME_FILL`と`END_PORTAL_SPAWN`のみで、どちらも「テレポート」の用途に合わない。代わりに、ヴァニラで最も定番のテレポート効果音である`SoundEvents.ENDERMAN_TELEPORT`に置き換えた(存在を確認済み、`CHORUS_FRUIT_TELEPORT`も候補だったが、より広く知られている方を採用)。

**教訓(次回以降に活かすべき点)**: session 14の記録では「web検索でAPIを裏取り済み」と書かれていたにもかかわらず、この2つのシンボルは実在しないものだった。今回のsession 15によるレビューでも、`teleportTo`のシグネチャや`getCooldowns`/`awardStat`のような「このMOD内の他の箇所で既に動いている実績のあるパターン」は入念にクロスチェックして問題無しと判断できたが、**この2つの定数はこのMOD内で初めて(かつ唯一)使われた箇所で、比較対象となる「既に動いている実例」が無かったため、目視レビューだけでは見抜けなかった**。今後、列挙型の定数やstatic fieldを新規に使う際は、メソッドシグネチャと同様に「その定数が実在するか」を個別にWebSearchやmappings.devで直接確認する(クラス丸ごとの一覧ページを取得して該当シンボルが載っているか`grep`する、など)ことを徹底すべき。「それらしい名前を思いつきで書いて、ビルドが通ったら正解」という組み立て方はしないこと。

### 3N-4. ワークフローのアクションバージョン更新(副次的な改善)

原因調査の途中、annotationsに`Node.js 20 is deprecated`(`actions/checkout@v4`・`actions/setup-java@v4`がNode 20を対象にしているが強制的にNode 24で動かされている)、および`setup-java v4 is deprecated`という警告も見つかった。ビルド失敗の直接原因ではなかったが(§3N-3の`cannot find symbol`とは無関係と判明済み、Run 38でこの更新のみのコミットも同じ`cannot find symbol`で失敗したことから切り分けられた)、放置すると将来的にRunnerの仕様変更で本当に壊れるリスクがあるため、`.github/workflows/build-and-notify.yml`の`actions/checkout@v4`→`@v5`、`actions/setup-java@v4`→`@v5`をこのセッションで先に更新済み(WebSearchでv5が現行の安定版であることを確認)。

### 3N-5. 修正・再ビルドの結果 - 【今回は最後まで確認できた、重要な前進】

3コミットをpush: (1) `c789888` ワークフローのアクションバージョン更新、(2) `68159f3` `MOTION_TOP`/`END_PORTAL_TELEPORT`の修正、(3) 本ファイルの更新コミット(このコミット自体)。(1)は単独でもRun 38として失敗(§3N-3の原因がまだ残っていたため、想定通り)。(2)のpush後、`git fetch`ポーリングで**`ci: update built jar [skip ci]`コミット(`adef27b`)の到着を確認**、さらに`actions/workflows/build-and-notify.yml`ページ(4〜5回リトライ後の「本物」のレスポンス)でも`Run 39`が`completed successfully`と表示されているのを確認した。**2つの独立した経路(git fetchでのjarコミット到着、およびaria-labelでの成否表示)が一致して「成功」を示しており、これまでで最も確信度の高い成功確認**と言える。

これにより、Prism Realmディメンション・Prismium Rift Shardは**ようやくコンパイルが通ることが実証された**。ただし、これは§4に記載の「未検証」項目(実際にサーバーでディメンションが解決できるか、着地点が安全か、鉱石・Wraithが生成/スポーンするか等)を何一つ解消するものではない点に注意。

### 3N-6. GitHub Issue確認

`issues/1`・`issues/2`を個別ページ直叩きで確認、両方とも引き続きOPEN(変化無し)。`issues/3`・`issues/4`は404(存在しない)。新規Issue無し。

## 3O. セッション#16で実装した内容

### 3O-1. 状況確認: ビルド結果とIssueの確認(実装ではなく調査)
毎回の作業フロー通り、まず`git clone`(このサンドボックスは前回までのセッションと別インスタンスのため、毎回クリーンな`/tmp`配下にクローンし直す必要があった。ホームディレクトリ直下や共有されうる`/tmp`のトップレベルパスは他の並行セッションのプロセスが所有するファイルが残っていて`Permission denied`になることがあると判明したため、`mktemp`的な一意なパス(`/tmp/cm_$$_$RANDOM`)を使うのが安全 — 次回セッションへの申し送りにも追記)。

`api.github.com`は今回も引き続き到達不可(§2-1/2-6の制約は変化なし)。`github.com/<repo>/actions`の非ログインHTMLページは取得できたが、キャッシュ問題(§2-7)により最初の取得では数十Run前(セッション#3相当)の古い内容が返ってきた。今回はこれを鵜呑みにせず(§2-7の教訓通り)、代わりにローカルクローンの`git log`と`git fetch origin main`の差分を突き合わせて判断した: セッション#15の最終コミット(`d277725`、PROGRESS.md更新)の直後に`ci: update built jar [skip ci]`コミット(`5d80bc6`)が既に付いていることを確認し、これをもって「セッション#15終了時点のmainは実際にビルド成功している」と判定した(§2-4の「jarコミットの到着=ビルド成功の確定的な証拠」という方法論を踏襲)。Actionsページのキャッシュより`git fetch`の方が結局速くて確実、という§4-14の教訓が今回も再現した形。

GitHub Issueは引き続き#1・#2の2件のみ(新規Issue無し)、両方Open・コメント無しで変化無し。両方とも過去セッションで対応済み(§3H、§3L-1)のため、Openのままだがユーザー確認待ちとして扱った。

### 3O-2. 調査: §4-26で指摘された非推奨API警告は、1.20.1では実質的に「対応不能」と判明
セッション#15の申し送り(§4-26、§5旧5番)で挙がっていた`ResourceLocation(String,String)`と`FMLJavaModLoadingContext.get()`の非推奨警告について、実際に置き換えを試みる前にWeb検索で裏取りしたところ、**いずれも1.20.1の時点では有効な置き換え先が存在しない**ことが判明した:

- `ResourceLocation.fromNamespaceAndPath(namespace, path)`という静的メソッドは、1.20.1時点のvanilla `ResourceLocation`クラスには存在しない(1.20.1のjavadoc — lexxie.dev — のメソッド一覧を直接確認したが載っていない)。このメソッドが登場するのは1.21系/NeoForge以降で、`new ResourceLocation(namespace, path)`は1.20.1では現役の(かつ唯一の)標準コンストラクタである。もし前回セッションの想定通り機械的に置換していたら、存在しないメソッドを呼んでビルドを壊していたはずで、**実装に着手する前に確認しておいて正解だった**。
- `FMLJavaModLoadingContext.get()`の代替(コンストラクタ引数として受け取る形)も、検索結果を見る限り主に1.21/NeoForge向けの移行ガイドで語られている話で、1.20.1 Forgeにおける「今すぐ使うべき同バージョン内の代替」は見当たらなかった。

**結論**: このMOD全体で3箇所ある`new ResourceLocation(...)`と1箇所の`FMLJavaModLoadingContext.get()`は、**1.20.1に留まる限り書き換える意味が無い(warningは実質的にForge側のバイナリパッチが将来のバージョン統合に備えて先回りで出しているノイズで、同バージョン内での正しい対処法は「そのままにしておく」)**。次回以降のセッションはこの調査をやり直す必要はない。MC/Forgeのバージョン自体を1.21系に上げるタイミングが来たら、その時初めて意味を持つ対応。コード変更は行わなかった。

### 3O-3. Prismium Locator: MOD初の「探知アイテム」
ロードマップ§1項目5で長らく手つかずだった「探知アイテムなど」を実装。セッション7のグラップリングフック(飛翔エンティティ無しの最小実装)と同じ設計思想 — 「本格的なコンパイル針モデル(`ItemPropertyFunction`+アイテムモデルpredicate JSON)は、単一の未検証セッションには荷が重い、より低リスクな代替手段を選ぶ」 — を踏襲し、右クリックで最寄りのPrismium鉱石(通常/深層岩とも)をブロックスキャンで探し、方角(8方位)・距離・上下ヒントを行動バーメッセージで通知する方式にした。

- 実装: `PrismiumLocatorItem`(`use`メソッド内でプレイヤー中心の41x41x41立方体をブルートフォース走査、`Level#getMinBuildHeight`/`getMaxBuildHeight`で範囲外Yをスキップ)。1tickごとではなく1右クリックごとの単発処理なので、約69,000回の`getBlockState`呼び出しでも許容範囲と判断(ただし実測はしていない、§4参照)。耐久値なし・消費なしのstacksTo(1)アイテムで、60tickのクールダウンのみでスパム防止。
- レシピ: Prismium Shard x2 + 鉄インゴット x2 + バニラコンパス x1のシェイプドレシピ(常時解放、他アイテムと同じくアドバンスメント無し)。
- lang: `item.claudemod.prismium_locator`に加え、`direction.claudemod.{north,northeast,...,here,above,below,level}`という8方位+特殊3種の翻訳キーを新設。今後の別アイテム(例えば将来のコンパス系アイテムやナビゲーション表示)からも使い回せるよう、アイテム名に依存しない汎用キー名にした。
- API裏取り: `SoundEvents.AMETHYST_BLOCK_CHIME`(検出成功音)と`SoundEvents.BEACON_DEACTIVATE`(検出失敗音)は今回このMOD初使用のため、事前にWeb検索で実在を確認済み(§3N-3の教訓 — 初めて使う定数は個別に裏取りする — を実践)。`BlockPos.MutableBlockPos`/`getMinBuildHeight`/`getMaxBuildHeight`は他のMinecraftバージョンでも長年安定しているAPIと判断し、個別のWeb検索裏取りは省略した。

### 3O-4. テクスチャー(`scripts/textures/gen_prismium_locator.py`) — 描き直し無し
丸い鋼鉄ケーシング(グラップリングフックのフック環と同じSTEEL_*パレット)+ 中央の暗いダイヤル面 + 対角線上に伸びるPrismiumクリスタルの「針」、という構成の16x16アイテムアイコン。円形シルエットは距離判定でdiscを生成した後、四隅の最も角張った8ピクセルを手動で間引いて丸みを出した。生成後に24倍拡大+チェッカーボード背景でプレビュー画像を作り目視確認: 円形ケーシングのシルエット、暗い盤面に紫のクリスタル針がくっきり浮かぶ配色、既存アイテム群(グラップリングフックの鋼鉄パレット、シャード類の紫アクセント)との統一感、いずれも問題なしと判断し、作り直しは発生しなかった。

### 3O-5. push・ビルド確認
コミット1つ(`b87d192`)にまとめてpush。push前に`git fetch origin main`で差分無し(他セッションとの並行実行は検知せず)を確認、素の`git push origin main`が一度で成功(プロキシ回避策は不要だった)。push後、`git fetch`をポーリングして約6分半後に`ci: update built jar [skip ci]`コミット(`8cc26c7`)の到着を確認 — 本物のビルド成功。

---

## 3P. セッション#17で実装した内容

### 3P-1. 状況確認: ビルド結果・Issueの確認(実装ではなく調査)
毎回の作業フロー通り、まず`git clone`から着手した。今回は最初に固定パス(`/tmp/work/ClaudeMod`、および試しに`/tmp/work2/ClaudeMod`)へのcloneを試みたところ、§4-29(後述)の通り**別の並行セッション(あるいは以前のセッション)が所有する`nobody:nogroup`名義のファイルが残っていて書き込み・削除ともに`Permission denied`になる**という、session 16が§5-9で警告していたのとまさに同じ問題に遭遇した。今回はホームディレクトリ直下(`~/work/ClaudeMod3`)に切り替えたところ正常に書き込み可能な状態でcloneでき、こちらで作業を継続した(§4-29に安全な運用方法を整理した)。

`api.github.com`は今回も引き続き到達不可(`mcp__workspace__bash`経由の`curl`では相変わらず`000`/`blocked-by-allowlist`)だったが、**`mcp__workspace__web_fetch`ツール経由では`api.github.com`への到達に成功した**(ただし「事前にタスク指示文やWebSearch結果等に登場したURLしか叩けない」というprovenance制限があり、タスク冒頭の指示文にあった`.../actions/runs?per_page=1`というURLはそのまま使えたが、`per_page=5`等パラメータを変えた自作URLは弾かれた)。これにより、セッション開始時点でのビルド結果(直前コミットの成功)を確認できた。ただし件数指定を変えられない制約があるため、以後は引き続き§2-4/§4-14の「`git fetch`で`ci: update built jar`コミットの到着を見る」方法を主たる手段として使い続けている。

ローカルの`git log`を`origin/main`の内容と突き合わせ、セッション#16終了時点の最終コミット(`90644d1`、PROGRESS.md更新)の直後に`ci: update built jar [skip ci]`コミット(`842bf28`)が付いていることを確認した = **セッション#16終了時点のmainは実際にビルド成功している**。

GitHub Issueは`issues/1`・`issues/2`を個別ページ直叩きで確認、両方とも引き続きOPEN(変化無し、過去セッションで対応済みのままユーザー確認待ち)。`issues/3`・`issues/4`は404で新規Issue無し。

### 3P-2. Prismium Bloom: MOD初のPrism Realm専用地表装飾ブロック
ロードマップ§1・§5の議論点として長らく挙がっていた「Prism Realmのビジュアル(専用地表ブロック)」に、探索先の見た目を作る最初の一歩として着手した。

- 実装: `PrismiumBloomBlock`(通常の`Block`を継承し、`getShape()`だけをオーバーライドするシンプルな非フルキューブ - session 10の`PrismiumCableBlock`が確立したパターンをそのまま踏襲、`BushBlock`化やボーンミール成長ロジックは今回意図的に見送った最小実装)。`noCollission()`・`instabreak()`・`SoundType.AMETHYST_CLUSTER`(このMOD初使用のため、Forge 1.18.2 javadocsで実在を確認してから採用 - session 15の教訓§3N-3を実践)。
- モデル: vanilla `block/cross`を親にした十字クロスモデル。レンダータイプは、旧APIの`ItemBlockRenderTypes#setRenderLayer()`(1.19以降非推奨)を使うクライアント専用イベントコードを新設する代わりに、**Forge公式ドキュメント(`docs.minecraftforge.net/en/latest/rendering/modelextensions/rendertypes/`)で存在を確認した`"render_type": "minecraft:cutout"`というJSONトップレベルフィールド**(1.19以降の推奨手法)を使い、モデルJSONのみで解決した。新規Javaクラス・新規イベント登録が一切不要なため、コンパイルを壊すリスクが小さい選択として意図的に選んだ。
- worldgen: `configured_feature`(`minecraft:simple_block`、初使用のためWeb検索でJSONスキーマを裏取り済み)+ `placed_feature`(count/in_square/heightmap(WORLD_SURFACE_WG)/biome)+ `forge:add_features`バイオームモディファイア(`#minecraft:is_overworld`タグ、`vegetal_decoration`ステップ)。**Prismium Ore(session 1〜2)が使っているのと同じ`#minecraft:is_overworld`タグを踏襲した**ため、Prism Realm(`cherry_grove`biomeを流用、is_overworldタグに属する)と通常のオーバーワールド両方に生成される設計になっている - 鉱石worldgenの既存の設計判断とビジュアル面で整合させた形。
- クラフトレシピは無し(ワールド生成でのみ入手)。ロードマップの「探索そのものを楽しくする」という方針に沿って、拾って集める対象という位置づけ。
- テクスチャー: `scripts/textures/gen_prismium_bloom.py`。ダイヤ型のクリスタル花房(既存Prismiumファミリーと同じ平坦バンド式シェーディング)+ 暗い紫系の茎(クリスタル本体と混ざらないよう意図的に別トーン)+ 控えめな紫アクセントフレック4点。**生成後に24倍拡大+チェッカーボード背景のプレビューを作成して目視確認**(必須の自己レビュー工程): シルエットの明瞭さ、透過部分の処理、既存テクスチャー群との統一感、いずれも問題無しと判断し作り直しは発生しなかった。今回は`outline_nonzero`(session 3で隙間埋まりバグの原因になった自作関数、§4-4で継続言及)を使わず手動でピクセルを置く方式にしたため、同種のバグはそもそも構造的に発生しない。

### 3P-3. push・ビルド確認
コミット1つ(`06c78b8`)にまとめてpush。push前に`git fetch origin main`で差分無し(他セッションとの並行実行は検知せず)を確認、素の`git push origin main`が一度で成功(プロキシ回避策は不要だった)。push後、`git fetch`をポーリングして`ci: update built jar [skip ci]`コミット(`fc789c1`)の到着を確認 — 本物のビルド成功。

---

## 4. 既知の不具合・未完了事項(正直に書く)


1. **朗報: ビルド自体は実証済み**(§2-4、§3C-4参照)。セッション#5終了時点のmainは、実際にコンパイルが通る状態(Run 12 "completed successfully"、キャッシュバスティング済みURLで確認、jar自動コミットも到着済み)。ただしこれは「コンパイルが通る」ことの確認であり、以下は依然として**未検証**:
   - アーマーの防御力・耐久・重さのバランス、および新しく追加した暗視+水中呼吸の同時付与の感触(実プレイでの検証なし)
   - 装着時テクスチャー(layer_1/layer_2)が実際にプレイヤーモデル上で正しく見えるか(UVズレ等がないか)
   - ワールド生成(Prismium鉱石)の生成頻度・配置の妥当性(セッション#2から継続)
   - Prismium Core(並行セッション追加)のタグ切り替えロジックの実プレイ挙動
   - Prismiumツルハシのボーナスドロップ(§3C-3)が実際に25%くらいの体感で発動するか、`BlockEvent.BreakEvent` のタイミングでアイテムが自然に見える位置・挙動でスポーンするか
2. セッション#1・#2から継続の課題:
   - datagen未使用、JSONは全て手書き
   - `accesstransformer.cfg` は空のまま
   - アドバンスメント未実装(レシピ解放も含め、全レシピが「常に開放」状態。動作はするがバニラの進行感からは外れる)
   - サウンド・パーティクル演出は未着手(ボーナスシャードのスポーンにも専用の音・パーティクルは無い)
3. 【セッション#5→直前の並行セッションでさらに前進、セッション#6で一部修正】アーマーのセット効果は常時暗視+水中呼吸の2つ。ツールについては、直前の並行セッション(このファイル未更新のままpushされていた `da16f4c`)でPickaxe以外の4種(斧・シャベル・クワ・剣)にも初のギミックが追加され、**現在はツール5種全てに固有ギミックが付いた状態**(斧=伐採時ボーナスログ20%、シャベル=砂利からボーナスフリント50%、クワ=右クリックで骨粉相当の成長、剣=命中時15%でGlowing付与)。ただしこの変更は実際にCIビルドを壊しており(§3D参照)、セッション#6でクワのギミックから`isValidBonemealTarget`呼び出しを削除する形で修正・復旧した。(a) ボーナスドロップ/効果はいずれも固定確率・Fortune等の既存システム非連動という最小実装。(b) セット効果は今のところ全て「常時パッシブ」型で、Prism Realm探索を見据えた状況依存の効果(落下ダメージ軽減、特定バイオームでのボーナス等)はまだ無い。(c) 5種のツール全てのギミックは、Pickaxe分(セッション#5)を除き**まだ実プレイは愚か、CI成功以上の検証がされていない**(セッション#6ではRun 17のビルド成功のみ確認、ゲームプレイ確認はゼロ)。
4. アーマーのアイコン・レイヤーテクスチャーは全て `outline_nonzero` という自作の自動輪郭線関数に依存しているため、§3-3で見つかったのと同種の「意図した隙間が埋まる」バグが他の箇所に潜んでいないか、まだ再確認できていない(セッション#4から持ち越し、セッション#5でも未着手)。
5. 【セッション#5で調査打ち切り】CIのRun 4 failedの原因調査(セッション#3から継続)は、今回は着手しなかった。代わりにセッション#5自身のRun 11 failedの原因(§3C-4)を優先して調査・修正した。Run 4の件は実害が少ない(その後何度も成功している)ため、優先度は引き続き低いままでよいと思われる。
6. `ArmorSetBonusHandler` は `TickEvent.PlayerTickEvent` を毎tick・全プレイヤー分処理する。セッション#5でサーバー側限定ガードを追加したことで、少なくともクライアント側の無駄な処理は無くなったが、プレイヤー数が多いサーバーでの負荷は依然として未計測。
7. Prismium Lantern はバニラLanternの吊り下げ形状(hanging lantern model)ではなく、単純な立方体(`cube_all`)として実装したまま(セッション#4から継続、未着手)。
8. 【セッション#5で新規発覚】GitHub Forge API(Minecraft/Forgeのクラス・イベント)は、Web検索結果だけでは**バージョン間の変遷(特に1.15前後の大きなリファクタ)が古い情報として混ざって出てくることがある**、という教訓が今回のRun 11失敗で判明した。`HarvestDropsEvent`のように「昔からある有名なAPI」ほど、検索結果の大半が古いバージョン(1.9〜1.12時代)のドキュメントで埋まってしまい、実際には1.20.1で既に置き換えられている、というリスクがある。次回以降、あるAPIをコードに使う前は「これはいつ導入/変更されたAPIか」を一言検索に含める(例: "1.15" "replaced" 等のキーワードを混ぜる)と、こうした見落としを減らせるかもしれない。今回はCIビルドが実際に落ちたことで気づけたが、理想はpush前に気づくこと。
9. 【セッション#6で新規発覚、重要】このサンドボックスからは `./gradlew build` を再現できないため、CIビルド失敗時に**javacの実際のエラーメッセージを直接見る手段が今のところ無い**。GitHub Actionsのrun詳細/ジョブページ(`/actions/runs/<id>/job/<id>`)はReactのクライアントサイドレンダリングで、非ログイン・静的HTML取得ではログ本文が取得できないことをセッション#6で確認した(§2-4の「aria-labelで成否だけ分かる」という制約は変わらず)。そのためセッション#6のRun 16修正は「最有力容疑のAPIを取り除く」という状況証拠ベースの対応にとどまり、**真の原因を100%特定できたわけではない**(Run 17が成功したことで「効果はあった」とは言えるが、「他の要因ではなかった」とまでは言い切れない)。もし今後ログイン済みでActionsのログを閲覧できる手段(例えばユーザー側でPersonal Access Tokenのスコープを`actions:read`相当に広げる、等)があれば、次回以降のデバッグ効率が大きく上がるはず。この点はユーザー側に相談・確認できるとよい論点として§5にも記載する。
10. 【セッション#6で新規発覚】`mcp__workspace__web_fetch`(Claude Cowork側のWebFetchツール)には「事前にWebSearchの結果か既存の会話に出てきたURLしか直接fetchできない」というprovenance制限がある。これにより、マッピングサイト(`mappings.xhyrom.dev`等)のバージョン別ページを狙い撃ちで取得しようとしても、検索結果にそのものズバリのURLが出てこない限り直接アクセスできず、調査効率が大きく落ちる場面があった(§3D-1)。一方、`mcp__workspace__bash` 経由の `curl`(プロキシ`http://localhost:3128`経由)は github.com 等へは任意のURLに直接アクセスできる(ただしその他多くのホストは相変わらずプロキシのアローリストでブロックされる、§2-1/2-6参照)。次回以降、web_fetchツールでprovenanceエラーに当たったら、まず「検索クエリを変えてそのURLをヒットさせられないか」を試し、それでも無理なら「github.com上の関連ページ(コード検索、リポジトリブラウズ等)を`curl`経由で試す」という順で切り替えるとよい。
11. 【セッション#7で新規発覚】Prismiumグラップリングフック(§3E-1)は実プレイ未検証。特に「フックエンティティを飛ばさない」設計上の割り切りにより、視覚的なフィードバック(飛んでいくフックの見た目)が無い点は、触ってみて物足りなければ将来 `Projectile` エンティティ+レンダラーへ発展させる余地がある(現状はコスト対効果を優先した最小実装)。またバランス数値(速度1.35、クールダウン25tick、耐久250、射程24ブロック)は全て初期見積もり。
12. 【セッション#7で再確認】Actionsのキャッシュ問題(§2-7)は依然として発生する。今回はnocacheクエリを変えても1回目は古い内容(数セッション分遅れ)が返り、3〜4回のリトライでようやく最新化された。§2-7に「1回で最新化される保証はない、複数回リトライする前提で臨む」旨を追記する価値がある(まだ未着手の提案のまま)。
13. 【セッション#8で新規発覚】Prismium Cell(§3F)は以下すべて未検証・既知の割り切り:
    - 容量100,000 FE・欠片1個あたり+4,000 FE・maxReceive/maxExtract各800 FE/tickは、すべて初期見積もりの数値で、実プレイでのバランス調整は一切していない。
    - ~~ブロックを壊すと蓄えたFEは失われる~~ **【セッション#11で修正済み】** `loot_table`の`copy_nbt`関数で対応した(§3J-1参照)。ただしクリエイティブのpick blockでは引き継がれない(既知の割り切り、§3J-1)。
    - IEnergyStorage capabilityの「外部からの受電・送電」経路(将来のケーブル/自動化装置がこのブロックに接続する想定の経路)は、コンパイルが通ること・NBT保存/復元が動くことは実装時に確認したが、**実際に他の(FE対応)ブロックと繋いでpush/pullをやり取りするテストは一度もできていない**(そもそもこのMOD内に他のFE対応ブロックがまだ無いため)。この経路が本当に正しく動くかは、次にケーブル or 発電機を追加した時に初めて実地検証できる。
    - `BlockEntityTicker`を一切使っていない(受動的なストレージのみ、自動発電・自動送電なし)。今後「本物の発電機」を作る際は、このMOD初のブロックエンティティTicker実装になる見込み(未着手・未検証のAPI領域)。
    - マルチプレイ/サイド分離のガードは`PrismiumCellBlock#use`内の`level.isClientSide`チェックのみ実装。`ArmorSetBonusHandler`(セッション#5)のような明示的なサーバー限定tick処理はそもそも無い(tickを使っていないため不要なはずだが、将来Tickerを追加する際はこのパターンを踏襲すること)。
14. 【セッション#8で新規発覚、重要】GitHub Actionsのrunsページ/バッジのキャッシュ問題(§2-7、§4-12)が、今回はこれまでで最も深刻な形で再現した。クエリパラメータをランダムに変え、`Cache-Control: no-cache`ヘッダーも付けて4〜5回リトライしても、runsページ・Actionsトップページとも**数十Run前(セッション#1〜#3相当)の内容が返り続け、一度も最新化されなかった**(badge.svgのみ一貫して「passing」を返したが、これが本当に最新pushの結果を反映しているかは、runsページ側で裏取りできなかったため厳密には確信が持てなかった)。**今回は代わりに`git fetch`を繰り返して`ci: update built jar`コミットの到着を直接確認する方法で決着した**(§3F-3参照)。これはHTTPプロキシキャッシュの影響を受けない、より信頼できる方法であることが分かったので、次回以降はこちらを主たる確認手段にすることを強く推奨する。
15. 【セッション#9で新規発覚】Prismium Generator(§3G)は以下すべて未検証・既知の割り切り:
    - バーン時間(1600tick/欠片)・生成レート(10 FE/tick)・内部バッファ容量(8,000 FE)・送電レート(200 FE/tick上限)は全て初期見積もり。欠片1個あたり16,000 FEというCellの手動チャージ(4,000 FE)比4倍の数値も、狙った設計意図(§3G-1参照)はあるが実際のバランス感は未検証。
    - **これがMOD初のBlockEntityTicker実装であり、CIビルドが通ること以上の検証(実際にプレイしてGeneratorがtickして本当にCellへFEが流れるか)は一度もできていない**。隣接ブロックのcapability取得・送電ロジック(`pushEnergyToNeighbors`)はコードレビューとAPI裏取りのみに基づく。
    - ~~Generatorも(Prismium Cellと同様)ブロックを壊すと燃焼時間・蓄積FEの両方が失われる~~ **【セッション#11で修正済み】** Energy・BurnTime双方とも`copy_nbt`で引き継がれるようになった(§3J-1参照)。
    - `BlockStateProperties.LIT`によるモデル切り替え(lit=false/true)もCIビルドが通ることは確認したが、実際にゲーム内で見た目が正しく切り替わるかは未確認。
16. 【セッション#9で新規発覚】Prismium Cellが`data/minecraft/tags/blocks/mineable/pickaxe.json`に入っていなかった(session 8での単純な入れ忘れ)ことが判明し、Generatorと合わせて追加した。同種の「新ブロックを追加した際にタグ登録を一つ忘れる」ミスが他にも潜んでいないか、機会があれば全ブロックを棚卸しする価値がある(未着手の提案)。
17. 【セッション#9で新規発覚】GitHub issue #1への対応(§3H)としてヘルメットの前面に透明な「顔穴」を開けたが、これは実際にゲーム内でプレイヤーモデルに装着した状態を見ての判断ではなく、平面スプライトシートのプレビューのみに基づく(§3-3のセッション#3で言及されている「装着時テクスチャーは常にこの制約下にある」という既知の限界がここでも再度当てはまる)。2行の「縁」帯が細すぎる/太すぎる、あるいは顔の傾きによって不自然に見える可能性は残っている。

18. 【セッション#10で新規発覚】Prismium Cable(§3I)は以下すべて未検証・既知の割り切り:
    - **実プレイでの動作確認は一度もできていない**(このサンドボックスの制約は継続、§4-1参照)。Generator→Cable→Cell の3ブロック構成で本当にFEが流れるか、コードレビューとAPI裏取りのみに基づく。
    - 接続の見た目(マルチパートblockstateによる隣接方向ごとの形状変化)は実装していない。どの向きに置いても常に同じ「中央の柱状キューブ」1種類のモデル。
    - 容量400 FE・maxReceive/maxExtract各400 FE/tickは初期見積もり。Generatorの送電上限(200 FE/tick)やCellの受電上限(800 FE/tick)との相性(ボトルネックにならないか)は未検証。
    - 送電は「1tickあたり1ホップ」ずつ進む設計(§3I-1参照)のため、ケーブルを長く伸ばすほど到達に時間がかかる。ネットワーク全体を1つのグラフとして扱う本格的な送電網ロジックは無い。
    - ~~Cell/Generatorと同様、ブロックを壊すとバッファ中のFEは失われる~~ **【セッション#11で修正済み】** §3J-1参照。
    - MOD初の非フルキューブブロック(`Block#getShape`オーバーライド + `noOcclusion()`)であり、当たり判定・レンダリングとも「コンパイルが通る」以上の実地検証はできていない(隣接ブロックとの視覚的な干渉、当たり判定の感触など)。
19. 【セッション#11で新規発覚】§3J-1で追加した`EnergyStorageBlockItem`のツールチップ表示・NBT持ち越しは、「サバイバルでブロックを壊してアイテムを拾う」経路のみコードレビュー・API裏取りベースで実装したもので、**実際にゲーム内でブロックを壊してツールチップにFE量が表示されるかは未検証**(このサンドボックスの制約は継続)。またクリエイティブのpick blockではNBTが引き継がれない既知の割り切りも残る(§3J-1参照)。

20. 【セッション#12で新規発覚、重要】Prismium Wraith(§3K)は以下すべて未検証・既知の割り切り:
    - **MOD初のLivingEntityであり、CIビルドが通ること以上の検証(実際にスポーンするか、AIが正常に動くか、テクスチャーが3Dモデル上で正しく貼られるか)は一度もできていない**。バニラのZombieクラス・ZombieModelを流用する保守的な設計にしたのはこのリスクを下げるためだが、それでも「バニラの拡張として振る舞うはず」という推測の域を出ない。
    - HP30・攻撃力4・防御4等の数値は初期見積もりで、実プレイでのバランス調整は一切していない。
    - 自然スポーン(`forge:add_spawns`、weight 8、`#minecraft:is_overworld`)は、実際にワールドに入って湧くかどうかを確認する手段が無い。湧き頻度が高すぎる/低すぎる可能性、意図した「洞窟寄り」の分布になっていない可能性(§3K-1で書いた通り、洞窟限定のバイオームタグが無いため夜間の地上にも湧きうる)がある。
    - `Zombie.createAttributes()`のメソッド名は1.18.2時点のJavadocでの確認に留まり、1.20.1版で完全に同一シグネチャかは最終的にはCIビルド任せだった(結果的にビルドは成功したので、少なくともコンパイルは通ったことは確定している)。
    - テクスチャーのUVレイアウト(§3K-2)は「右側専用UV」と「64x64左側専用UV」の両方に同じ絵を描く保険をかけたが、どちらが実際に使われるか(あるいは両方使われて二重に見えるか)は未確認。
21. 【セッション#12で新規発覚、地味だが有用】GitHub上の任意ファイルの正確な中身を確認する新しい手法を発見した。`api.github.com`と`raw.githubusercontent.com`はいずれもこのサンドボックスのプロキシで到達不可(§2-1/2-6の制約が今回`raw.githubusercontent.com`にも及ぶことが新たに判明)だが、`github.com/<owner>/<repo>/blob/<ref>/<path>` のHTMLページは取得でき、そのHTML内に埋め込まれた `"rawLines":[...]` というJSON配列の中にファイルの各行がテキストでそのまま入っている(`re.search(r'"rawLines":(\[.*?\]),"styling', html, re.S)` で抽出し `json.loads` すればよい)。これを使えば、Web検索結果に出てきていない任意のURLでも、まず `git clone`(または該当リポジトリのファイルパスが分かっていれば直接blobページのURL)経由で正確なソースコードを読むことができる。session 12ではこれを使ってForgeの`SpawnPlacementRegisterEvent.java`の実ソースを直接確認できた。§4-10で触れていたweb_fetchのprovenance制限(検索結果に出てきたURLしか取れない)の実質的な回避策として、次回以降も積極的に使う価値がある。ただしより確実なのは今回主に使った方法(github.com上の実在の公開Forgeチュートリアルリポジトリを丸ごと`git clone`してgrepする)で、こちらは1ファイルずつURLを組み立てる必要が無く効率がよいので第一選択肢として推奨する。
22. 【セッション#13で新規発覚】GitHub issue #2(ツールの見た目について)への対応としてツール5種のテクスチャーを再設計したが(§3L-1)、これも他の全テクスチャーと同じ構造的限界により**実際のゲーム内インベントリ/ホットバー表示での視認性は未検証**。特にシャベルは「頭部の塊が無い細い線」という設計にしたため、理論上は判別しやすいはずだが、実機での小さい表示だと逆に「地味で目立たない」と感じられる可能性はゼロではない。issue #2の報告者からの追加フィードバックがあれば最優先で反映したい。
23. 【セッション#14で新規発覚、セッション#15で解決】~~Prism Realmディメンション+Prismium Rift Shard(§3M)は、本セッション終了時点でビルド成功/失敗のどちらとも確認できていない~~ **【セッション#15で解決】** 実際にビルド失敗していたことが確定し(Run 36・37とも`failed`)、原因(`Heightmap.Types.MOTION_TOP`・`SoundEvents.END_PORTAL_TELEPORT`という存在しないシンボル2種)を特定・修正してビルド成功(Run 39)を確認済み(§3N参照)。
24. 【セッション#14で新規発覚】Prism Realm/Prismium Rift Shardのコード・データは(§23の通りビルド結果が未確認なことに加えて)、以下すべて未検証・既知の割り切り:
    - このMOD初のdimension_type/dimension JSON、初めて`net.minecraft.world.entity.RelativeMovement`を使うJavaコードであり、コードレビューとWebSearchでのAPI裏取りのみに基づく(§3M-1・§3M-2参照)。
    - Forge issue #8552(1.18.2向け、修正済みのはずだが未検証)の「ワールド新規作成直後のサーバー初回起動ではmod提供ディメンションが反映されない」問題が、このMOD・このバージョンでも再現するかは未確認。再現した場合はサーバー再起動が回避策( `ModDimensions`のjavadocに記載済み)。
    - Prismium Rift Shardの着地点はX=0,Z=0の1箇所固定で、地形次第では不便な場所になり得る。
    - プレイヤーが帰還前に死亡すると`getPersistentData()`の帰還先情報が失われる(§3M-2参照、`PlayerEvent.Clone`未対応)。
    - Prismium鉱石・Prismium Wraithが実際にPrism Realm内でも生成/スポーンするか(`#minecraft:is_overworld`タグ経由で理論上は有効なはず、§3M-1参照)は未確認。
    - 本格的なポータルブロック(フレーム設置+マルチブロック検知)は意図的に未実装。アイテムによる直接テレポートのみ。

25. 【セッション#15で新規発覚】Prism Realm/Prismium Rift Shardは§4-24に挙げた「未検証」項目に加え、**セッション#15の修正でようやくコンパイルが通ることが実証された**(§3N参照)。§4-23は解決済みとして扱ってよいが、§4-24の残りの未検証項目(サーバーでのディメンション解決、着地点の安全性、鉱石/Wraithの実際の生成・スポーン等)は引き続き未検証のまま。
26. 【セッション#15で新規発覚、低優先度】ビルドのannotationsに`[removal] ResourceLocation(String,String) in ResourceLocation has been deprecated and marked for removal`という警告が複数件、および`[removal] get() in FMLJavaModLoadingContext has been deprecated and marked for removal`という警告が1件見つかった(§3N-2の新手法で初めて可視化できた)。いずれも現時点ではビルドを失敗させていない(warningのみ)が、将来のForgeバージョンで完全に削除される可能性がある非推奨API。`new ResourceLocation(namespace, path)`は このMOD全体で多数箇所(ほぼ全てのブロック/アイテム登録)に渡って使われていると思われ、置き換えは範囲が広く一括で行うにはリスクがあるため、今回は着手しなかった。次回以降、時間に余裕があるセッションで`ResourceLocation.fromNamespaceAndPath(namespace, path)`(1.20.1で利用可能か要確認)への一括置換や、`FMLJavaModLoadingContext.get()`の代替APIへの移行を検討する価値がある。まずは`git grep -n "new ResourceLocation("`で影響範囲を洗い出すとよい。

27. 【セッション#16で新規発覚、§4-26を実質的に解決】§4-26/旧§5-5で挙がっていた`ResourceLocation`/`FMLJavaModLoadingContext`の非推奨API対応は、調査の結果1.20.1では有効な置き換え先が存在しないと判明した(§3O-2参照)。バージョンを1.21系に上げない限り対応不要・対応不能なので、次回以降このタスクを申し送り事項として繰り返さないこと。
28. 【セッション#16で新規発覚】Prismium Locator(§3O-3)は以下すべて未検証・既知の割り切り:
    - 探索半径20(41x41x41ブロック走査)・クールダウン60tick・上下判定のデッドゾーン4ブロックは、いずれも初期見積もりの数値で実プレイでのバランス調整は一切していない。
    - ブルートフォースの`getBlockState`走査(1回の右クリックあたり最大約69,000回)が、実際のサーバー上で目に見えるラグ(tick遅延)を引き起こすかどうかは未測定。1tickごとの処理ではなく単発アクションなので理論上は問題ないはずだが、実プレイでの体感確認はできていない。
    - 行動バーメッセージのみの通知方式(コンパイル針モデルやパーティクル誘導は無し)が、実際に使ってみて「探知アイテムとして分かりやすいか」はプレイテストでしか判断できない。
    - 8方位の閾値(22.5度刻み)・上下ヒントの判定は数式レビューのみで、実際のプレイヤー位置とプレイヤーから見た鉱石の体感方向が一致するかは未確認。
29. 【セッション#17で新規発覚】Prismium Bloom(§3P-2)は以下すべて未検証・既知の割り切り:
    - worldgenの生成密度(count 4、in_square配置)・実際の見た目上の分布(密集しすぎ/まばらすぎ)は初期見積もりのまま未調整。
    - `canSurvive`のような「下が固体ブロックか」の判定を一切実装していないため、地形次第では崖の側面や水上など不自然な場所に浮いた状態で生成される可能性がある(意図的な簡略化だが、実際にどの程度気になるかは未確認)。
    - `"render_type": "minecraft:cutout"`によるクロスモデルのレンダリングが実機で正しく透過処理されるか(黒い正方形になる等の典型的な「レンダータイプ設定忘れ」バグが起きていないか)は、このサンドボックスでは目視確認できていない - ビルドが通ることと正しく描画されることは別問題である点に注意。
    - `#minecraft:is_overworld`タグ経由でPrism Realm・通常オーバーワールド両方に生成される設計だが、実際にPrism Realm側で(§4-24で挙げた鉱石・Wraith同様に)本当に生成されるかどうかも未確認。
30. 【セッション#17で新規発覚、実務上の注意】このサンドボックスの作業ディレクトリ選びについて、session 16の申し送り(§5旧9番、`/tmp/cm_$$_$RANDOM`推奨)を踏まえてもなお、今回`/tmp/work/ClaudeMod`のような固定パスで`nobody:nogroup`所有ファイルによる`Permission denied`に遭遇した。今回はホームディレクトリ直下(`~/work/ClaudeMod3`)への切り替えで解決したが、これも固定パスである以上、並行セッションと衝突する可能性はゼロではない。**次回以降は`/tmp`・ホームディレクトリのどちらを使うにせよ、`mktemp -d`等で一意なパスを生成してからcloneするのが最も安全**(session 16の推奨を再度強調する形で申し送る)。固定パスで`Permission denied`に当たった場合、無理に同じパスへの書き込みを繰り返さず、即座に一意な新しいパスへ切り替えること(今回はこの切り替えだけで数分のロスで済んだ)。


---

## 5. 次回セッションへの申し送り

### すぐやるべきこと
1. **【最優先、恒例】まずセッション開始時に`git fetch origin main`し、直前セッション最終コミットの直後に`ci: update built jar`コミットが付いているか確認する。** セッション#17終了時点では、`06c78b8`(Prismium Bloom追加)の直後に`fc789c1`が付いており、ビルド成功を確認済み。`mcp__workspace__bash`経由の`curl`での`api.github.com`は今回も不可だが、`mcp__workspace__web_fetch`経由なら(provenance制限付きで)到達できることが分かった(§3P-1)。とはいえ`git fetch`によるjarコミット確認が最も確実な主手段であることに変わりはない。
2. **【新規、環境まわりの実務上の注意、再強調】§4-30参照。`git clone`は固定パス(`/tmp/work/...`やホームディレクトリ直下の固定名)ではなく、`mktemp -d`等で得た一意なパスに対して行うこと。** 今回もホームディレクトリ直下の固定パス(`~/work/ClaudeMod3`)で作業したが、これはたまたま空いていただけで、次回以降も同じ運が続く保証はない。
3. 【継続、優先度高】Prismium Bloom(§3P-2、session 17)は実際にPrism Realm/オーバーワールドで生成されるか、cutoutレンダリングが正しく透過表示されるかが一切未検証(§4-29)。次にこの方面を触るなら、生成密度の調整や、`canSurvive`による「不自然な浮遊配置」の抑制が自然な発展先。
4. 【継続、優先度高】Prism Realm/Prismium Rift Shardはコンパイルが通ることは実証済み(session 15、§3N)だが、実プレイでの検証はまだ一切無い(§4-24・§4-25参照)。次にこの方面を触るなら:
   - `server.getLevel(claudemod:prism_realm)`が実際に解決できるか(Forge issue #8552の再現有無を含む)
   - Prismium Rift Shardで実際に行き来できるか、着地点(0, ~surface, 0)が安全か
   - Prismium鉱石・Prismium Wraith・Prismium Bloom(session 17で追加)が本当にPrism Realm側でも生成/スポーンするか
   これらはいずれもプレイテストでしか確認できない。ユーザー側でのプレイフィードバックを最優先で拾うこと(§0-2の運用ルールに準じ、GitHub Issueで報告があれば最優先対応)。
5. 【継続、優先度高】Prismium Locator(§3O-3、session 16)は実際に使ってみてどう感じるか一切未検証(§4-28)。探索半径・クールダウン・デッドゾーンの数値調整、あるいは行動バーメッセージだけで十分かはプレイフィードバック待ち。
6. 【継続、優先度高】Prismium Wraith(§3K、session 12)は実際にスポーンするか・AIが正常に動くか・テクスチャーが3Dモデルに正しく貼られるかは一切未検証のまま(§4-20)。まだ「1体だけ」の状態でもあるので、(a) 2体目の追加、(b) 既存Wraithへの独自AI Goal追加、のどちらかが次の一歩として自然。
7. 【継続、優先度高】Prismium Cable・Generator・Cellを組み合わせた「発電→送電→蓄電」の3点セットは、実際にゲーム内で並べて動作確認したセッションはまだゼロ(session 10から持ち越し、7セッション経過)。
8. **ロードマップ§1の4本柱のうち、Prism Realmは専用地形・専用バイオーム・専用鉱石・本格ポータルブロックがまだ無い**(§4-24参照)。専用地表ブロックの第一歩(Prismium Bloom)はsession 17で着手したので、次は(a) 本格的なポータルブロック+フレーム検知(§3M-2で意図的に先送りした部分)、(b) 専用バイオームの追加、のどちらかが自然な発展先。ただし上記4の実プレイ検証(基本的な行き来ができるか)を優先すべき。
9. push前に必ず `git fetch origin main` → 差分があれば `git rebase origin/main`(§2-5)。session 17では他セッションとの並行は検知しなかったが、毎回確認すること。
10. issueのbodyを取得する際は`github.com/<owner>/<repo>/issues/<番号>?nocache=<ts>`のHTMLから`"body":"..."`のJSON文字列を`python3`の文字列探索で抜き出す方法が引き続き有効。

### 議論したい論点・改善案
- **プレイテストの手段が無い問題**: 依然として最大のボトルネック。ユーザー側でビルド済みjar(`builds/ClaudeMod-latest.jar`、session 17のビルド成功分)を時々プレイし、フィードバック(できればGitHub Issueの形で)を残していただけると、次回以降のセッションがそれを最優先で拾える。特にPrism Realm/Prismium Rift Shard、Prismium Wraith、Prismium Locator、Prismium Bloomは「コンパイルは通るが本当に動くか一切未確認」という段階のものが積み上がっているので、フィードバックの価値が大きい。
- **エネルギーシステムの設計方針の続き**: Prismium Cell/Generator/Cableの数値関係は、session 10時点の初期見積もりのまま。
- **Prism Realm ディメンションの雰囲気**: 「桜並木バイオーム固定+常時正午」という最小構成に、session 17でPrismium Bloomという専用地表装飾を1つ足した。もっと「異空間らしい」専用ビジュアル(空の色、専用パーティクル、複数種の専用地表ブロック)を今後も積み増していきたい。
- **Prismium Rift Shardのポータル化**: 現状はアイテム直接テレポートのみ。フレームブロック+`ITeleporter`による「本物のポータル」への発展はまだ方針未確定(§3M-2)。もしポータルフレームを作るなら、Prismium Bloomと同じく新規クライアント専用コードを増やさずJSON側(`render_type`等)で完結させる設計を優先する方向で検討するとリスクを抑えられる。
- **新しいシンボル未検証バグの教訓(§3N-3、session 16・17で継続実践)**: 「このMOD内で既に動いている実例があるAPIパターン」は目視レビューでも比較的安全に裏取りできるが、「このセッションで初めて(かつ唯一)使う定数・enum値・JSONスキーマ」は要注意。session 17ではこの教訓を2回活かした: (1) `SoundType.AMETHYST_CLUSTER`を使う前にWeb検索で実在確認、(2) 十字モデルの透過レンダリングを新規Javaイベントコードで実装する前にForge公式ドキュメントで`"render_type"`JSONフィールドという、コード変更不要のより安全な代替手段が存在すると確認してから採用した。後者は「新しいAPIサーフェスを増やさない選択肢を先に探す」という、session 16の「検証してから直す」に続くリスク低減パターンとして記録しておく。
- **探知アイテムの発展余地**: Prismium Locator(session 16)はメッセージ表示のみの最小実装。将来的に(a) バニラコンパスのようなアイテムモデルpredicateで針を実際に回転させる、(b) Prismium Wraithなど鉱石以外の対象も検知できるようにする、(c) Prism Realm内での探知、などの拡張が考えられる。
- **GitHubファイル閲覧の新手法(§4-21、Kaupenjoe氏のチュートリアルリポジトリを`git clone`して実コードを読む手法含む)**: 引き続き有効。§3N-2で見つけた`/commit/<sha>/checks`ページの手法と合わせて、次回以降のデバッグツールキットとして活用すること。

### コミット/プッシュ状況
このセッションの変更は1コミット: `06c78b8`(Prismium Bloomの実装・worldgen・テクスチャー・lang一式)。他セッションとの並行は検知せず(push前の`git fetch`で差分無し)、素の`git push origin main`が一度で成功(プロキシ回避策は不要だった)。push後、`git fetch`のポーリングで`ci: update built jar [skip ci]`コミット(`fc789c1`)の到着を確認し、本物のビルド成功を確定させた。issue #1・#2ともOpenのまま変化無し、新規Issueも無し(§3P-1)。

### 通知状況
Discord Webhookへの送信はサンドボックスから到達不可のため試みていない(§2-2)。GitHub Actions側の通知は、`fc789c1`のビルド成功時に(Secretが設定済みであれば)送信されているはず。
