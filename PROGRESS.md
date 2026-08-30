# ClaudeMod 開発進捗 (PROGRESS.md)

このファイルは、1時間ごとに自動起動される開発セッション間の**唯一の記憶**です。
新しいセッションを始める前に必ずこのファイル全体を読んでください。会話履歴は引き継がれません。

最終更新: 2026-08-30(こんぺいとう氏との直接チャットセッション、PROGRESS.mdの構成を再整理)

**このファイルの構成(2026-08-30に再整理)**: 以前は「セッションごとに実装内容を長文で追記し続ける」運用で肥大化していたため(ピーク時4000行超)、今回から以下の5分類に固定した。

1. 約束や決まり事 — 必ず遵守する恒久的なルール・技術的な決まり事
2. TODO — 次にやるべきことを優先度順(上ほど高い)に並べたリスト
3. 問題点 — 既知の不具合・未検証事項の箇条書き
4. その他 — 参考情報・議論したい論点・通知状況など
5. MOD構想・ロードマップ — MOD全体の方向性

**今後の運用ルール(重要)**: このファイルにセッションごとの実装経緯を長文で追記し続けない。新しい実装をしたら、TODO/問題点/その他を書き換える(該当項目を完了に更新 or 削除 or 新規追加)ことでこのファイル自体は常にコンパクトに保つ。経緯を残したい詳細な実装ログ・議論の記録は`PROGRESS_ARCHIVE.md`に追記すること。

過去の全セッション(#3〜#83)の詳細な実装ログは`PROGRESS_ARCHIVE.md`にある。「なぜこの実装になったか」を調べたい場合はそちらを参照すること。

---

## 1. 約束や決まり事(必ず遵守)

1. **Discord通知にコミット要約を含める**: `.github/workflows/build-and-notify.yml`のNotify Discordステップは、直近1コミットの件名だけでなく、そのpushに含まれる全コミットの件名(自動コミットは除く)を箇条書きにして送る仕様になっている。ワークフローを触る際もこの挙動を壊さないこと。
2. **毎回の状況確認にGitHub Issueの確認を含める**: 公開リポジトリなので`https://github.com/Konpeitou24/ClaudeMod/issues`や個別issueページ(`https://github.com/<owner>/<repo>/issues/<番号>`)を非ログインで`curl`取得すれば見える。取得結果はプロキシキャッシュの影響を受けるため、キャッシュバスティング用クエリ(`?nocache=$(date +%s%N)`)を付けること。コメント本文は`react-app.embeddedData`のJSON(`data['payload']['preloadedQueries'][0]['result']['data']['repository']['issue']`のパス)をパースして読む。このgitトークンにはIssueへのコメント投稿・クローズ権限が無いため、`ISSUES_TO_CLOSE.json`/`PENDING_ISSUES.json`のリレー機構(`.github/workflows/`に整備済み)を使う。
3. **Issue対応ポリシー**: 投稿者が`Konpeitou24`本人ならその場で対応してよい。それ以外の投稿者のIssueは`PENDING_ISSUES.json`に登録して保留する。
4. **音声(サウンド)方針**: バニラの`SoundEvent`を複数レイヤーする(音量・ピッチを変えて`playSound`を複数回呼ぶ)ことを常に第一候補とする。Python合成(numpy等でのサイン波合成)は、バニラに近い音が本当に存在しない場合に限る最終手段。単一のサイン波(ピッチスイープ)はDSPを足してもコミカルな効果音にしかならないという実例があるため、安易に手を出さない。
5. **ローカルビルドは実行不可**: `./gradlew build`はこのサンドボックス内では必ず失敗する(プロキシのallowlist制限で`maven.minecraftforge.net`等に到達不可)。ビルド確認はGitHub Actions経由のみ。`api.github.com`への直接アクセスも不可なため、リポジトリにコミットされる`builds/last_datapack_validation_summary.txt`/`last_ore_verification.txt`/`last_datapack_validation_errors.log`で結果を確認すること。
6. **Discord Webhookへの直接送信もサンドボックスから不可**。通知はGitHub Actions側(`build-and-notify.yml`/`release.yml`)に任せ、無駄なリトライをしないこと。
7. **`git push`はまず素の状態(プロキシ環境変数に手を加えない)で試す**。「access denied by the git proxy」等で失敗した場合にのみ、`https_proxy="" HTTPS_PROXY="" http_proxy="" HTTP_PROXY=""`を付けて再試行する(順序を逆にしない)。
8. **複数セッション同時実行に備え、push前に必ず`git fetch origin main`し、差分があれば`git rebase origin/main`する**。
9. **作業ディレクトリは必ずユニークなパスを使う**(`mktemp -d`、またはセッション専用ディレクトリ配下)。`git config user.name/user.email`は`ClaudeMod Session Agent <claudemod-agent@users.noreply.github.com>`に設定する。
10. **lang(en_us.json/ja_jp.json)等の整形済みJSONを部分編集する際は、`json.load`+`json.dump`による全体再整形をしない**。既存エントリの直後に新規行を文字列置換で挿入する方式を使うこと。
11. **新ブロック追加時は関連タグ(`mineable/pickaxe`、`walls`等)への登録漏れに注意する**(過去に2回発生済み)。
12. **テクスチャーは既存素材をコピーせずPython(Pillow)で自作し、生成後は必ず拡大画像を目視確認する**。このセッション環境のRead/Write/Editツールはリポジトリのgit作業ディレクトリ(Linuxサンドボックス内パス)に直接使えないことが多いため、確認したい画像はいったんWindows側マウントの作業フォルダ(outputs)にコピーしてから`Read`ツールで開くこと。ファイル編集自体は`mcp__workspace__bash`経由のpython/sed/catで行う。
13. **外部API・Minecraft本体の未確認仕様を調べる際は`WebSearch`/`mcp__workspace__web_fetch`を積極的に使う**。`minecraft.wiki`・`mappings.dev`(1.20.1 mojmap javadoc)等の一般サイトには到達できる(bashの`curl`は`api.github.com`等の主要ホストがプロキシで塞がれていて到達不可)。それでも足りなければ`mcp__Claude_Browser__*`ツールで直接リポジトリ・公式mavenを読むこと。
14. **Mixinベースの外部依存MOD(Curios等)を`compileOnly`/`runtimeOnly`で追加する場合、CIの`runGameTestServer`で実際にロードされクラッシュしうる**。`build.gradle`の該当runブロックに`mixin.env.remapRefMap` / `mixin.env.refMapRemappingFile`の設定が必要になる場合がある。
15. **外部MODのスロット/インベントリ拡張機能に対応する際は、「スロット種別への登録・タグ付け」と「エンティティへのスロット配布」が別々の必須ステップであることを確認する**(Curios対応で一度見落とし、実機テストで発覚し後日修正した実例あり)。
16. **セッションがPROGRESS.md更新前に終了する可能性がある**。作業開始時、直近リリースタグ以降のコミットを`git log`で確認し、記録漏れが無いかチェックすること。あれば遡って記録する。
17. `PROGRESS_ARCHIVE.md`に全セッション(#3〜#83)の詳細な実装ログがある。経緯を詳しく調べたい場合はそちらを参照すること。

---

## 2. TODO(優先度順、上ほど優先度が高い)

1. 【超最優先】次に書くリリースノートに、charmスロット配布漏れ修正(コミット`f2a3d99`、v0.31.1に既に含まれているが両バージョンのリリースノートに未記載)についての一文を追記する。文面例:「護符(charm)用Curiosスロットがプレイヤーに配布されていなかった不具合の修正は、実はv0.31.1から既に含まれていました。」追記できたら、この項目と問題点3-Xの記載を消してよい。
2. v0.31.1(蒼白のプリズミウムブロックのテクスチャ、こんぺいとう氏との合作更新)のCIビルド結果を確認する(この更新を行った対話セッションでは`api.github.com`到達不可のため確認できずに終わっている)。
3. プリズミウム・クローラー(v0.31.0で追加した5体目のMOB)が実機で意図通り動作しているか(見た目・自然スポーン・鳴き声)、こんぺいとう氏に確認を依頼する。
4. Issue #18対応(Curios統合: Featherstone/Emberguard/Vitastone/Magnet Charmの4アイテム+Guardian Charm拡張、v0.30.0/v0.30.1)全体が実機で動作しているか確認する。特にCurios GUIでのスロット表示・ドラッグ&ドロップ・右クリック装備・効果発動の4点。動作確認が取れ次第issue #18をクローズする。
5. Issue #21(JEI互換性)への着手方針を検討する。着手する場合は決まり事14(mixin remapRefMap)を思い出すこと。
6. Pale Prismium系(建築バリエーション3種・ランタン)、3機械共通基底クラスへのリファクタリング(v0.27.0〜v0.28.0)の実機フィードバックを確認する。
7. **【2026-08-30、こんぺいとう氏からの指摘で順序変更】Prism Realmにまず「平原」の基本地形を追加する。** 現状はバニラのオーバーワールド設定(地形の起伏はそのまま)+固定バイオームcherry_groveを流用しているだけで、専用の地形が無い。この土台が無いまま巨大山岳地帯のような特徴地形だけを追加すると、「海に急に山岳地帯がある」ような文脈のない不自然な地形になってしまう。
8. 上記7の後に、ユーザー直接要望「Prism Realm巨大山岳地帯+ボス」に着手する。規模が大きいため、地形生成とボスMOBに分割して段階的に進めること。**7より先に着手しないこと**(海のような場所に唐突に山岳があると意味が分からなくなるため)。
9. MOBのカテゴリ拡充を検討する。現状「戦闘」「水中非戦闘」「地上アンビエント」の3種類があり、飛行アンビエントMOB・使い魔的MOB等の新カテゴリを追加する余地がある。

---

## 3. 問題点(既知の不具合・未検証事項)

- Issue #18(CuriosAPI対応)・#21(JEI互換性)は引き続きOPEN。
- **大前提**: このサンドボックスは実機(ゲームクライアント)を起動できないため、MOD内のほぼ全コンテンツが「CIビルドが通ること」以上の検証が一切できていない。具体的にはバランス数値(FE容量・攻撃力・防御力・生成密度・クールダウン等)、装着時テクスチャー・インベントリ表示・GUI表示、FE配電経路が実際に繋がって動くか、全MOBの自然スポーン頻度・AI挙動、worldgen装飾ブロックの生成、全GUIの表示崩れ、サウンド/パーティクル演出のタイミングと音量感が丸ごと未検証。新しいコンテンツを追加するたびに、この一般則で説明できる「未検証」を個別に書き足す必要はない(個別に書く価値があるのは下記のような特殊な課題だけ)。
- charmスロット配布漏れの修正(コミット`f2a3d99`、v0.31.1に含まれる)がリリースノートに未記載(→TODO1参照)。
- Prismium Lanternがバニラの吊り下げ形状ではなく単純な立方体(`cube_all`)のまま(session 4から未着手)。
- `ArmorSetBonusHandler`は`TickEvent.PlayerTickEvent`を毎tick・全プレイヤー分処理する。サーバー側限定ガードはあるが、プレイヤー数が多いサーバーでの負荷は未計測。
- `Monster`クラス限定でMobを走査する実装(Prismium Wardstone等)は`Slime`/`MagmaCube`(`Mob`は継承するが`Monster`は継承しない)を取りこぼす。
- `ResourceLocation`/`FMLJavaModLoadingContext`の非推奨API警告は1.20.1では有効な置き換え先が無い(1.21系に上げない限り対応不能、今後このタスクを申し送りに書かないこと)。
- セッションがPROGRESS.md更新前に終了するリスクが実際に一度発生した(セッション#81、git履歴から遡って復元済み)。今後も作業開始時に必ず確認すること(決まり事16)。

---

## 4. その他

**使えるもの**: JDK 21がプリインストール(JDK 17は`apt-get install openjdk-17-jdk-headless`で追加可能)。システムGradle 8.14.3が`/opt/gradle`にプリインストール済み。Python3 + Pillowはテクスチャ生成に使用可能。`github.com`(`api.github.com`は不可)、`raw.githubusercontent.com`は到達可能。

**議論したい論点・改善案**:
- Guardian Charm・Pulse CharmをCuriosの対象外にした判断への同意が得られるか、あるいは別のアプローチ(Pulse Charmを右クリックでなくCurios経由の常時発動効果に作り替える等)を検討すべきか。
- ポータルフレームの専用ブロック化・6個セット化案。要望があれば着手を検討。
- Prismium Ingot/Alloy Ingotのスミシングアップグレード経路の再検討。
- `PROGRESS_ARCHIVE.md`への分離運用(2026-08-30に本格導入)が今後も機能するか、数セッション後に振り返りたい。

**通知状況**: Discord Webhookはサンドボックスから到達不可のため試みていない。GitHub Actions側(`build-and-notify.yml`・`release.yml`)がpush/タグに対応する通知を送信する(Secret設定済み前提)。

---

## 5. MOD構想・ロードマップ

「てんこ盛り」コンテンツMODとして、以下の柱を段階的に育てていく。優先順位や詳細は毎回のセッションで見直してよい。「完成」を目指さず、常に肉付けし続ける。各要素は最初は最小実装で入れて、後のセッションで機能・バランス・ビジュアルを磨き込む前提。

1. **新資源・素材ライン**: Prismium(プリズミウム) — 最初の資源で、装備・エネルギー・ディメンションの共通テーマ素材。派生として「蒼白のプリズミウム」(Pale Prismium)ファミリーも展開中(装飾ブロック・建築バリエーション・ランタン)。
2. **新エネルギーシステム**: 「Prismium Energy」。FE(Forge Energy)ベース。発電機(Generator)・蓄電ブロック(Cell)・3機械(Pulverizer/Smelter/Compressor、共通基底クラス抽出済み)まで実装済み。ケーブル(離れたブロック間の中継)・GUI連携・複数ブロックにまたがる大規模送電網はまだ無い。
3. **新ディメンション**: 「Prism Realm」。データパック駆動のディメンション、テレポート用アイテム(Prismium Rift Shard)まで実装済み。専用地形・専用鉱石・専用バイオーム・本格的なポータルブロック・巨大山岳地帯+ボスはまだ無い(TODO参照)。
4. **新MOB**: Prismium Wraith/Deep Wraith(戦闘)、Sentinel(戦闘)、Drifter(水中非戦闘)、Crawler(地上アンビエント、5体目)の5体。ボス級はまだ無い。カテゴリ拡充の余地あり(その他参照)。
5. **新装備**: ツール5種・アーマー4種(セット効果: 暗視+水中呼吸)、グラップリングフック、探知アイテム(Locator)、Shield、Bow、Guardian Charm(cheat-death)、Featherstone/Emberguard/Vitastone/Magnet Charm(完全パッシブ系)まで実装済み。一部はCuriosAPI対応済み(Issue #18)。
6. **新ブロック/ギミック**: Prismium Core、Prismium Lantern、蒼白のプリズミウムブロック・ランタン・建築バリエーションまで実装済み。装飾ブロック・罠・ダンジョン用ギミックブロックはさらに拡充の余地がある。
