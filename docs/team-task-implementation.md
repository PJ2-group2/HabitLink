# チーム共通タスクの自動管理実装

## 概要
チーム共通タスクを作成時やチーム参加時に全メンバーと自動的に紐づける機能を実装しました。

## データ構造の変更

### 1. Taskエンティティの拡張
- `teamId`フィールドを追加
- チーム共通タスクには必須、個人タスクはnull

### 2. UserTaskStatusエンティティの拡張
- `teamId`フィールドを追加
- チーム共通タスクの場合は対応するチームIDを保持

### 3. 新しいサービスクラス
- `TeamTaskService`を作成
- チーム共通タスクの自動管理機能を提供

## 主要機能

### チーム共通タスク作成時の自動紐づけ
```java
TeamTaskService teamTaskService = new TeamTaskService(taskRepository, teamRepository, userTaskStatusRepository);

// チーム共通タスクを作成（teamIdを指定）
Task teamTask = new Task("task001", "チーム習慣", "説明", 30, 
    Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), 
    true, "team001", LocalTime.of(9, 0), "daily");

// 自動的に全チームメンバーにUserTaskStatusを生成
Task savedTask = teamTaskService.createTeamTask(teamTask);
```

### チーム参加時の既存タスク紐づけ
```java
// 新しいメンバーがチームに参加した際
teamTaskService.createUserTaskStatusForNewMember("team001", "newUserId");
```

### チーム進捗状況の取得
```java
// チーム全体の完了率を取得
double completionRate = teamTaskService.getTeamTaskCompletionRate(
    "team001", "task001", LocalDate.now());

// ユーザーのチーム共通タスク一覧を取得
List<UserTaskStatus> teamTasks = teamTaskService.getUserTeamTaskStatuses(
    "userId", LocalDate.now());
```

## データベース変更

### tasksテーブル
- `teamID`カラムが既に存在（変更なし）

### user_task_statusesテーブル
- `teamId`カラムを追加
- 既存データとの互換性を保持

## 使用方法

### 1. タスク作成時
従来の個人タスク作成に加えて、チーム共通タスクの場合：
```java
// teamIdを指定してTaskを作成
Task teamTask = new Task(taskId, taskName, description, estimatedMinutes, 
    repeatDays, true, teamId, dueTime, cycleType);

// TeamTaskServiceを使用して保存
teamTaskService.createTeamTask(teamTask);
```

### 2. チーム参加処理
```java
// 従来のチーム参加処理に加えて
team.addMember(newUserId);
teamRepository.save(team, ...);

// 既存チーム共通タスクを新メンバーに紐づけ
teamTaskService.createUserTaskStatusForNewMember(teamId, newUserId);
```

### 3. 画面表示
個人画面とチーム画面で、それぞれ適切なタスクのみを表示：

**個人画面**：
```java
// 個人タスク（teamId = null）とチーム共通タスク（teamId != null）の両方
List<UserTaskStatus> allTasks = userTaskStatusRepository.findByUserId(userId);
```

**チーム画面**：
```java
// 特定チームのタスクのみ
List<UserTaskStatus> teamTasks = userTaskStatusRepository.findByUserIdAndTeamIdAndDate(
    userId, teamId, date);
```

## 利点

1. **自動化**: チーム共通タスクの作成時に手動でメンバー紐づけが不要
2. **一貫性**: 新メンバー参加時に既存タスクも自動的に紐づけ
3. **個別管理**: 各メンバーの進捗状況を個別に追跡可能
4. **統計機能**: チーム全体の完了率など統計情報を簡単に取得
5. **後方互換性**: 既存の個人タスク機能に影響なし

## 今後の拡張可能性

- チーム共通タスクの編集権限管理
- チームメンバー退出時のタスク削除
- チーム統計ダッシュボード
- タスクテンプレート機能