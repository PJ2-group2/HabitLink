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

サーバは `8080` 番ポートで起動し、以下の簡易 API を提供します。

- `/hello` – 動作確認用メッセージを返します。
- `/createTeam?id=<ID>` – 新しいチームを作成します。
- `/joinTeam?id=<ID>` – 既存チームに参加します。
- `/addTask?id=<ID>&task=<TASK>` – 指定チームにタスクを追加します。
- `/getTasks?id=<ID>` – チーム内のタスク一覧を取得します。


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
