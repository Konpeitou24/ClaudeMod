# ClaudeMod 開発進捗 (PROGRESS.md)

このファイルは、1時間ごとに自動起動される開発セッション間の**唯一の記憶**です。
新しいセッションを始める前に必ずこのファイル全体を読んでください。会話履歴は引き継がれません。

最終更新: 2026-08-16 (セッション #1、初回セットアップ)

---

## 1. MOD全体の構想(ロードマップの叩き台)

「てんこ盛り」コンテンツMODとして、以下の柱を段階的に育てていく。優先順位や詳細は毎回のセッションで見直してよい。

1. **新資源・素材ライン**: Prismium(プリズミウム) — セッション#1で着手した最初の資源。今後の装備・エネルギー・ディメンションの共通テーマ素材。
2. **新エネルギーシステム**: 「Prismium Energy(仮称)」。発電機・ケーブル・蓄電ブロック・機械(粉砕機、精錬機など)を実装し、FE(Forge Energy)ベースで組む想定。
3. **新ディメンション**: 「Prism Realm(仮称)」。Prismiumで動くポータル(枠ブロック+起動アイテム)で行き来する異空間。専用地形生成、専用鉱石、専用バイオーム。
4. **新MOB**: Prism Realm を含む探索先に生息する敵対/中立MOB。ボス级の1体を最終的に用意したい。
5. **新装備**: Prismium製ツール/アーマー(特殊能力付き)、探索を楽しくするアクセサリ的アイテム(グラップリングフック、探知アイテムなど)。
6. **新ブロック/ギミック**: 装飾ブロック、罠、ダンジョン用ギミックブロックなど。

「完成」を目指さず、常に肉付けし続ける。各要素は最初は最小実装で入れて、後のセッションで機能・バランス・ビジュアルを磨き込む前提。

---

## 2. 【最重要】既知の環境制約(次回セッションは必ず読むこと)

このセッションの実行環境(クラウドサンドボックス)には**ネットワークのアウトバウンド制限**があり、以下が判明した:

### 2-1. ビルドが実行できない
`./gradlew build` / `gradle build` は **必ず失敗する**。原因はプロキシのアローリスト制限で、以下のホストに到達できないため:
- `maven.minecraftforge.net` (ForgeGradle プラグイン自体・Forge本体の取得元)
- `files.minecraftforge.net`
- Mojang 系 (`piston-meta.mojang.com`, `libraries.minecraft.net` など、バニラMinecraftのライブラリ・マッピング取得元)
- `repo.maven.apache.org` / `repo1.maven.org` (Maven Central)
- `plugins.gradle.org` / `services.gradle.org` (Gradle Plugin Portal・Gradle配布)

到達確認できた(＝使える)ホストは `github.com`, `raw.githubusercontent.com`, `objects.githubusercontent.com`, `registry.npmjs.org`, `pypi.org` など極めて限定的。

実際に `gradle build` を実行して確認したエラー(セッション#1時点):
```
Plugin [id: 'net.minecraftforge.gradle', version: '[6.0,6.2)'] was not found in any of the following sources:
...
    Gradle Central Plugin Repository
    maven(https://maven.minecraftforge.net/)
```
プラグイン解決の時点で止まる。ここを突破できても、次はMinecraft本体・Forgeライブラリのダウンロードで同様に失敗するはず。

**次回セッションへの指示**: 同じ調査を繰り返して時間を無駄にしないこと。まずこの制約が解消されているか軽く再確認(例: `curl -s -o /dev/null -w "%{http_code}" https://maven.minecraftforge.net` が `000` 以外を返すか)し、直っていなければビルド確認はスキップして実装作業に集中する。もし直っていたら、この節を更新して以後は通常通りビルド確認を行うこと。

この制約により、**コードは目視レビューと知識に基づく慎重な記述で書いているが、実機コンパイル未検証**。構文ミス・API不整合が残っている可能性がある。ビルドが通る環境(ユーザーのローカルPC等)で最初に検証されるまでは、その前提で読むこと。

### 2-2. Discord Webhook 通知が送信できない
`discord.com` / `discordapp.com` も同じ理由で到達不可(`curl` が `exit 56` / `000` で失敗)。指示された開始・完了通知の curl コマンドは**実行を試みたが送信できていない**。次回セッションも同様に失敗する可能性が高いので、無駄なリトライはせず、この事実だけ確認して(1回で十分)、通知はスキップしてPROGRESS.mdの更新を通知の代わりとすること。ユーザーには別チャネル(Push通知)で一度知らせた。

### 2-3. 使えるもの
- JDK 21がプリインストール、JDK 17は `apt-get install openjdk-17-jdk-headless` で追加済み(このセッションで導入。次回セッションでは再度必要になる可能性が高い — コンテナが使い捨てのため)。
- システムGradle 8.14.3が `/opt/gradle` にプリインストール済み(`gradle` コマンドで直接使える。wrapperのダウンロードは不要)。
- Python3 + Pillow はテクスチャ生成に使用可能・導入済み確認。
- `github.com` へのgit push/pull、`raw.githubusercontent.com` からのファイル取得は可能(gradle-wrapper.jar等はここから取得した)。

---

## 3. セッション#1で実装した内容

### コード
- Forge 1.20.1 MDK の標準プロジェクト構成一式を手動で構築(`settings.gradle`, `build.gradle`, `gradle.properties`, `gradlew`/`gradlew.bat` + wrapper jar(GitHub上のgradleリポジトリから取得)、`.gitignore`)。
  - `forge_version=47.4.0`, `mapping_channel=official` / `mapping_version=1.20.1`
- `com.claudemod.ClaudeMod`: メインMODクラス(`@Mod("claudemod")`)。DeferredRegister登録をまとめるだけの薄い構成。
- `com.claudemod.registry.ModBlocks`: `prismium_ore`, `deepslate_prismium_ore`, `prismium_block` を登録。
- `com.claudemod.registry.ModItems`: `prismium_shard`(素材アイテム)+ 上記3ブロックのBlockItem。
- `com.claudemod.registry.ModCreativeTabs`: 専用クリエイティブタブ「ClaudeMod」。
- ルートテーブル(鉱石は通常ドロップ+フォーチュン対応、シルクタッチでブロック自体、Prismiumブロックは自己ドロップ)、ブロックタグ(`mineable/pickaxe`, `needs_iron_tool`)、レシピ(shard⇔blockの9個相互変換)、blockstate/model json、英語(`en_us`)・日本語(`ja_jp`)言語ファイル。

### テクスチャー(すべて自作、16x16)
生成スクリプト: `scripts/textures/gen_prismium.py` (Pillow使用、シード固定で再現可能)。
- `textures/block/prismium_ore.png`: バニラ石ベース+シアン系結晶の斑点
- `textures/block/deepslate_prismium_ore.png`: 深層岩ベース+同結晶
- `textures/block/prismium_block.png`: シアン〜ティール系のグラデーション+斜めファセットライン+微量の紫エネルギー粒(将来のエネルギー系への伏線)
- `textures/item/prismium_shard.png`: 縦長六角形の結晶シルエット、透過背景、outline/shadow/base/mid/highlight+紫アクセント1px

**自己レビュー実施済み**: 4枚を12倍拡大したプレビュー画像を生成しRead(目視確認)した。石/深層岩ベースの視認性、結晶の輪郭のはっきりしたシルエット、バニラのダイアモンド/エメラルドと被らない配色であることを確認済み。ブロック外周のアウトラインがやや強めなので、次回微調整の余地あり(§5参照)。

---

## 4. 既知の不具合・未完了事項(正直に書く)

1. **最重要: ビルド未検証**(§2-1参照)。javac/ForgeGradleでの実コンパイルが一度もできていない。特に以下は自己レビューだけでは見落としがちなので次回コンパイルできた際に最優先で確認すること:
   - import文の過不足(例: `net.minecraft.world.level.block.state.BlockBehaviour` のパッケージパスが1.20.1で正しいか)
   - `CreativeModeTab.builder()...title(...)` のメソッドチェーンの引数型・戻り値
   - `BlockBehaviour.Properties.of().lightLevel(state -> 3)` のラムダのシグネチャ(`ToIntFunction<BlockState>` のはず)
   - mods.toml のプレースホルダ置換(`${...}`)が `processResources` で正しく展開されるか
2. データ生成(datagen)を使わず、loot table・tags・recipe・modelをすべて手書きJSONにした。将来的にはDataProvider(`GatherDataEvent`)化してハードコードを減らしたい。
3. `accesstransformer.cfg` は空ファイルのまま(現状ATが不要なため)。将来的に不要なら`build.gradle`から`accessTransformer`設定ごと外すことも検討。
4. アドバンスメント(実績)は未実装。
5. `prismium_ore` の生成(ワールド生成/OreFeature配置)は未実装。現状は入手手段がクリエイティブのみ。次回の最優先候補。
6. サウンド、パーティクル演出などのポリッシュは未着手。

---

## 5. 次回セッションへの申し送り

### すぐやるべきこと
1. まず §2-1 の環境制約が解消されているか軽く確認(1回だけ)。解消されていれば `gradle build` を実行し、上記4節のチェックリストを潰す。解消されていなければビルドは諦めて実装続行。
2. `prismium_ore` / `deepslate_prismium_ore` のワールド生成(ConfiguredFeature/PlacedFeature + biome modifier json、またはOreFeaturesのdatagen)を追加し、サバイバルで実際に入手できるようにする。
3. Prismium系の最初のツール/アーマー1〜2種を検討開始(例: Prismium Pickaxe)。Tierを作るならTier定義(採掘レベル、耐久、速度、エンチャント適正、修理素材)を書く。

### 議論したい論点・改善案
- **エネルギーシステムの設計方針**: Forge Energy (FE) をそのまま使うか、独自単位にするか。将来性を考えるとFE互換にして他MODとの連携も視野に入れたい。
- **Prism Realm ディメンションの雰囲気**: 「探索が楽しい」を体現するため、単なる新バイオームの寄せ集めでなく、縦方向の探索(空中島、深い縦穴)や視認性の良いランドマーク配置を検討したい。
- **ブロックのアウトライン表現**: `prismium_block.png` の外周1pxアウトラインがやや強く出ている。バニラの鉄/金ブロックのような「フレーム」演出として意図的だが、実際のインゲーム表示(複数設置時の見た目)は未確認。次回、複数並べた際の見た目をシミュレート(タイル状にPillowで並べて確認)してから微調整するとよい。
- **難易度・入手難度のバランス**: Prismium鉱石の生成頻度・深度をまだ決めていない。ダイアモンドより希少にするか、序盤〜中盤向けの汎用素材にするか、方向性を決めたい。

### コミット/プッシュ状況
このセッションの変更はコミット・プッシュ済み(コミットメッセージで内容確認可能)。`git log` で履歴を追えるので、変更点の詳細はコミット差分を参照。
