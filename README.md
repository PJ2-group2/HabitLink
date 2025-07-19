# HabitLink

HabitLink は Java で実装された習慣管理アプリのサンプルです。HTTP サーバと JavaFX ベースのクライアントを備え、チーム単位でタスクを共有できます。

このプロジェクトは、`habit-server`、`habit-client`、`habit-domain` の3つのMavenモジュールで構成されています。

- `habit-server`: サーバサイドのアプリケーションロジック
- `habit-client`: JavaFXベースのクライアントアプリケーション
- `habit-domain`: サーバとクライアントで共有されるドメインオブジェクト

## 実行方法

### 1. プロジェクトのビルド

まず、プロジェクトのルートディレクトリで以下のコマンドを実行し、すべてのモジュールをビルドしてローカルのMavenリポジトリにインストールします。

```bash
mvn clean install
```

これにより、`habit-client/target` と `habit-server/target` ディレクトリに、それぞれ実行可能なJARファイル (`HabitLinkClient.jar`, `HabitLinkServer.jar`) が生成されます。

### 2. サーバーの実行

以下のコマンドでサーバーを起動します。

```bash
java -jar habit-server/target/HabitLinkServer.jar
```

サーバーは `8080` 番ポートで起動します。

### 3. クライアントの実行

以下のコマンドでクライアントGUIを起動します。

```bash
java -jar habit-client/target/HabitLinkClient.jar
```

## APIエンドポイント

サーバは以下のHTTP APIを提供します。

- `GET /hello` – 動作確認メッセージ
- `POST /login` – ログインして `SESSION_ID` を取得
- `POST /register` – 新規ユーザー登録
- `POST /createTeam` – チームの新規作成
- `GET  /joinTeam?teamName=<NAME>` – 既存チームへ参加
- `GET  /publicTeams` – 公開チーム名一覧取得
- `GET  /findTeamByPasscode?passcode=<WORD>` – 合言葉からチーム名検索
- `GET  /getTeamIdByPasscode?passcode=<WORD>` – 合言葉からチームID取得
- `GET  /getTeamName?teamID=<ID>` – チーム名取得
- `GET  /getTeamMembers?teamID=<ID>` – チームメンバー一覧
- `GET  /getTeamTasks?teamID=<ID>` – チームタスク一覧
- `GET  /getTaskIdNameMap?id=<ID>` – タスクIDと名前のマッピング
- `GET  /getUserTeamTasks?teamID=<ID>` – 自分のタスクのみ取得（`SESSION_ID` 使用）
- `POST /saveTask` – タスク情報を保存
- `POST /saveUserTaskStatus` – タスク状態を保存
- `POST /completeUserTask` – タスク完了登録
- `GET  /getUserIncompleteTasks?teamID=<ID>&date=<YYYY-MM-DD>` – 未完了タスク取得
- `GET  /getTeamTaskStatusList?teamID=<ID>&date=<YYYY-MM-DD>&days=<N>` – チームのタスク進捗
- `GET  /getUserTaskStatusList?teamID=<ID>&date=<YYYY-MM-DD>` – 自分のタスク状態一覧
- `GET  /getUserTaskIds` – 自分のタスクID一覧
- `POST /sendChatMessage` – チャットメッセージ送信
- `GET  /getChatLog?teamID=<ID>&limit=<N>` – チャット履歴取得
- `GET  /getJoinedTeamInfo` – 参加中チーム情報取得

ユーザー認証が必要な API では、`SESSION_ID` ヘッダにログイン時に得られた値を設定してアクセスします。データは `habit.db` に保存されます。

## テストの実行

```bash
mvn test
```

## クラス図の生成

```bash
python tools/diagram.py src out
```

`tools/diagram.py` は `javalang` と `graphviz` を利用してクラス図を生成するスクリプトです。上記コマンドでは `src` 以下の Java ソースを解析し、`out.pdf` を生成します。

## 必要環境

- JDK 14 以上
- Maven
- （GUI クライアント用）JavaFX
- （クラス図生成用）Python、javalang、graphviz