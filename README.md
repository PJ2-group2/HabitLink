# HabitLink

HabitLink は Java で実装された習慣管理アプリのサンプルです。HTTP サーバと JavaFX ベースのクライアントを備え、チーム単位でタスクを共有できます。

## Obtain the Code:

```bash
git clone https://github.com/PJ2-group2/HabitLink.git
cd HabitLink/
```

## Run the Server:

```bash
mvn exec:java -Dexec.mainClass=com.habit.server.HabitServer
```

サーバは `8080` 番ポートで起動し、HTTP API を提供します。主なエンドポイントは次の通りです。

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


## Run the Clients:

```bash
mvn exec:java -Dexec.mainClass=com.habit.client.gui.HabitClientGUI
```

コマンドラインからサーバにアクセスする `HabitClient` クラスも用意されていますが、上記コマンドでは JavaFX を用いた GUI クライアントを起動します。GUI クライアントでは以下の機能が利用できます。

- 個人ページでローカルタスクの追加・完了
- チームページでチームの作成・参加
- チームページでチームタスクの追加・取得・削除

## Run the Tests:

```bash
mvn test
```


## Generate Class Diagram:

```bash
python tools/diagram.py src out
```

`tools/diagram.py` は `javalang` と `graphviz` を利用してクラス図を生成するスクリプトです。上記コマンドでは `src` 以下の Java ソースを解析し、`out.pdf` を生成します。

---

### ディレクトリ構成

- `src/main/java/com/habit/server` – サーバ本体とチーム・タスク管理クラス
- `src/main/java/com/habit/client` – コマンドラインクライアントおよび JavaFX GUI
- `src/test/java` – JUnit テスト
- `tools/diagram.py` – クラス図生成スクリプト

### 必要環境

- JDK 14 以上
- Maven
- （GUI クライアント用）JavaFX
- （クラス図生成用）Python、javalang、graphviz
