package com.habit.client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.text.Font;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

public class TutorialController {

    @FXML
    private TextFlow speechTextFlow;

    @FXML
    private ImageView characterImageView;

    private int currentIndex = 0;
    private final String[] messages = {
        "やあ！僕の名前は「はびりん」！よろしくね！！",
        "「HabitLink」を初めて使う君に、\nこのアプリのルールを教えるよ！",
        "君はここで毎日のタスクを、\nチームメイトと一緒にこなしていくんだ。",
        "勉強、読書、ランニング、睡眠、、、、\n共有したいタスクは人それぞれ。",
        "自分に合ったチームを探して参加してね！\nもちろん自分でチームを作ることもできるよ！",
        "タスクの消化状況はチームメイトに共有されるよ！\nサボってるとばれちゃうからね・・・？",
        "それと1つ注意点！",
        "このアプリにはペナルティポイント制度があるんだ。",
        "タスクを期限内に消化できなかったら\nペナルティポイントが加算されちゃうよ。",
        "ペナルティポイントが一定数たまると\nいろんなペナルティが発生しちゃうからね。",
        "それと、このポイントによって\n僕の見た目やメッセージも変化するんだ！",
        "今でこそこんな感じの口調だけど、\n僕怠惰な人間には興味ないから。",
        "くれぐれも僕をがっかりさせないでね・・・？",
        "ま、せいぜいがんばることだね。",
        "あ、ペナルティポイントはちゃんと毎日タスクをこなしていれば\n減っていくから安心してね！",
        "それじゃあ、素敵な習慣を目指していってらっしゃい！"
    };

    private String[] imagePaths = {
        "/images/TaskCharacterLv5-1.png",
        "/images/TaskCharacterLv5-2.png",
        "/images/TaskCharacterLv5-3.png"
    };

    private final String[] highlightKeywords = {
        "「はびりん」", "それと1つ注意点！", "ペナルティポイント制度"
    };


    @FXML
    public void initialize() {
        updateContent();

        // ラベルをクリックして次のセリフへ
        speechTextFlow.setOnMouseClicked(this::handleLabelClick);
    }

    @FXML
    private void handleSkip() {
        goToHome();
    }

    private void handleLabelClick(MouseEvent event) {
        currentIndex++;
        if (currentIndex < messages.length) {
            updateContent();
        } else {
            goToHome();
        }
    }

    private void updateContent() {
        speechTextFlow.getChildren().clear(); // 古いテキストをクリア
        String message = messages[currentIndex];

        for (String part : splitMessageByKeywords(message)) {
            Text text = new Text(part);
            if (shouldHighlight(part)) {
                text.setStyle("-fx-fill: #d9534f; -fx-font-weight: bold;"); // 赤字＋太字
            } else {
                text.setStyle("-fx-fill: black;");
            }
            text.setFont(Font.font("Arial", 18));
            speechTextFlow.getChildren().add(text);
        }

        // 画像更新
        int imageIndex = currentIndex % imagePaths.length;
        characterImageView.setImage(new Image(getClass().getResourceAsStream(imagePaths[imageIndex])));
    }

    private void goToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/habit/client/gui/Home.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) speechTextFlow.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean shouldHighlight(String word) {
        for (String keyword : highlightKeywords) {
            if (word.equals(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<String> splitMessageByKeywords(String message) {
        List<String> result = new ArrayList<>();
        int i = 0;
        while (i < message.length()) {
            boolean matched = false;
            for (String keyword : highlightKeywords) {
                if (message.startsWith(keyword, i)) {
                    result.add(keyword);
                    i += keyword.length();
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                result.add(String.valueOf(message.charAt(i)));
                i++;
            }
        }
        return result;
    }
}
