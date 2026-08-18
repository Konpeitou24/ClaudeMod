# ClaudeMod

Minecraft Forge (1.20.1) 向けの「てんこ盛り」コンテンツMOD。新素材 **Prismium(プリズミウム)** を軸に、道具・防具・エネルギーシステム・専用ディメンション・MOB・装飾ブロックなどを継続的に追加しています。

コードはすべて自動化されたエージェントセッションによって1時間おきに開発されています。開発の進め方や既知の課題は [`PROGRESS.md`](./PROGRESS.md) に詳しく記録されています。

> An "everything and the kitchen sink" content mod for Minecraft Forge 1.20.1, built around a new material called Prismium. New tools, armor, an FE-based energy system, a custom dimension, a mob, and decorative building blocks are added incrementally by an automated agent every hour. See `PROGRESS.md` for the full development log (Japanese).

## 動作環境

- Minecraft: 1.20.1
- MODローダー: Forge 47.4.0 以降
- ビルド: JDK 17 が必要(Forge MDK)

## ダウンロード

最新のビルド済みjarは [Releases](https://github.com/Konpeitou24/ClaudeMod/releases) から入手できます。リリースが無い場合は `builds/ClaudeMod-latest.jar`(mainブランチに自動コミットされる最新ビルド)も利用できますが、動作確認はReleasesのタグ付きビルドの方が確実です。

## 主なコンテンツ

### 資源・素材
- **Prismium**: 鉱石・欠片・インゴットブロックからなる新資源ライン。MOD内の他コンテンツ全体の共通素材テーマ。
- **Prismium Core**: ダイヤモンド以上のツールでしか採掘できない、常時発光する上位ブロック。Prismium Cellのクラフト素材にもなる。
- **建築バリエーション**: Prismium Blockのスラブ・塀・階段・模様入りブロック(Chiseled)。

### 道具・防具
- Prismiumツール5種(ツルハシ・斧・シャベル・クワ・剣)。ツルハシは鉱石の追加ドロップ、斧・シャベル・クワ・剣にもそれぞれ固有ギミックあり。
- Prismiumアーマー一式(4部位)。フルセット装備で常時暗視+水中呼吸のセット効果。

### アクセサリ・装備
- **Prismium Grappling Hook**: 視線方向のブロックへ引き寄せられるグラップリングフック。
- **Prismium Locator**: 周囲のPrismium鉱石の方角・距離を教えてくれる探知アイテム。
- **Prismium Shield**: 独自実装のブロッキング装備。
- **Prismium Bow**: 全弾に貫通効果を持つ専用の弓。
- **Prismium Guardian Charm**: 致死ダメージを一度だけ防ぐお守り。
- **Prismium Featherstone**: 所持しているだけで落下ダメージを軽減するパッシブアクセサリ。
- **Prismium Emberguard**: 所持しているだけで火・溶岩ダメージを軽減するパッシブアクセサリ。
- **Prismium Vitastone**: 所持しているだけで回復量を増幅するパッシブアクセサリ。

### エネルギーシステム(Forge Energy / FEベース)
- **Prismium Generator**: Prismiumの欠片を燃焼してFEを発電する自動発電機。
- **Prismium Cell**: FEを蓄える電池ブロック。
- **Prismium Cable**: 離れたブロック間でFEを中継する送電網(接続に応じて見た目が変化するマルチパートモデル)。
- **Prismium Pylon**: FEを消費し、周囲のプレイヤーに再生効果を付与する装置(専用GUI付き)。
- **Prismium Restorer**: FEを消費し、手に持ったアイテムの耐久値を回復する装置(専用GUI付き)。
- **Prismium Wardstone**: FEを消費し、周囲の敵Mobを弱体化させる結界装置(専用GUI付き)。

### ディメンション・MOB
- **Prism Realm**: Prismium Rift Shardで行き来できる専用ディメンション。
- **Prismium Bloom / Spike**: Prism Realm特有の地表装飾ブロック。
- **Prismium Wraith**: Prism Realmに生息する敵対MOB。

上記はすべて最小実装から始めて、後続のセッションで機能・バランス・見た目を継続的に磨き込んでいく方針です。詳細な実装経緯・既知の不具合・未検証項目は `PROGRESS.md` を参照してください。

## テクスチャーについて

すべてのブロック・アイテム・MOBのテクスチャーは、既存の外部素材を使わず、Pythonスクリプト(Pillow)でオリジナルに生成しています。生成スクリプトは `scripts/textures/` にあります。

## 不具合報告・要望

[Issues](https://github.com/Konpeitou24/ClaudeMod/issues) からお願いします。内容は次回以降の開発セッションで確認・反映されます。
