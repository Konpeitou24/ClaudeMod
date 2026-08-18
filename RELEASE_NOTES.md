## ClaudeMod v0.1.0

はじめてのタグ付きリリースです。これまで自動セッションで積み上げてきた内容を、Semantic Versioning(セマンティックバージョニング)に沿って `v0.1.0` としてまとめました(GitHub Issue #3「リリースについて」への対応)。

対応バージョン: Minecraft 1.20.1 / Forge 47.4.0 以降 (JDK 17)

### 収録コンテンツ(概要)

- **資源・素材**: Prismium鉱石/欠片/ブロック/Core、建築バリエーション(スラブ・塀・階段・模様入り)
- **道具・防具**: Prismiumツール5種(固有ギミック付き)、Prismiumアーマー一式(暗視+水中呼吸のセット効果)
- **アクセサリ・装備**: Grappling Hook / Locator / Shield / Bow / Guardian Charm / Featherstone / Emberguard / Vitastone
- **エネルギーシステム(FEベース)**: Generator(発電) / Cell(蓄電) / Cable(送電、接続見た目対応) / Pylon・Restorer・Wardstone(消費、専用GUI付き)
- **ディメンション・MOB**: Prism Realm(専用ディメンション)、Rift Shard(テレポート)、Bloom/Spike(地表装飾)、Prismium Wraith(敵対MOB)

すべて最小実装から始まり、継続的に機能・バランス・見た目を磨き込んでいく方針で開発しています。詳細な実装経緯・既知の不具合・未検証項目は [`PROGRESS.md`](./PROGRESS.md) を参照してください。テクスチャーはすべてPythonスクリプトによるオリジナル生成です。

**注意**: このMODは自動化されたエージェントセッションで開発されており、実プレイでの検証が不十分な機能が多く含まれます(`PROGRESS.md`の「未検証」の記載を参照)。不具合や改善要望は [Issues](https://github.com/Konpeitou24/ClaudeMod/issues) までお願いします。
