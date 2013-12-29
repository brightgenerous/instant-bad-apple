package com.brightgenerous.instantbadapple;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.SceneBuilder;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.FlowPaneBuilder;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.media.MediaViewBuilder;
import javafx.stage.Stage;
import javafx.stage.StageBuilder;
import javafx.stage.StageStyle;

public class InstantBadAppleApplication extends Application {

    public static void main(String[] args) {
        InstantBadAppleApplication.launch(args);
    }

    @Override
    public void start(final Stage stage) throws Exception {

        Exception ex = null;
        final MediaPlayer player;
        {
            MediaPlayer p = null;
            for (int i = 0; i < 3; i++) {
                try {
                    p = getMediaPlayer();
                } catch (Exception e) {
                    ex = e;
                }
                if (p == null) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                    }
                } else {
                    break;
                }
            }
            player = p;
        }

        if (player == null) {
            String text = (ex == null) ? "Error" : ex.getLocalizedMessage();
            Label label = LabelBuilder.create().text(text).build();
            FlowPane root = FlowPaneBuilder.create().children(label).alignment(Pos.CENTER).build();
            Scene scene = SceneBuilder.create().root(root).build();
            StageBuilder.create().scene(scene).width(300).height(240).title("Instant Bad Apple - Error").applyTo(stage);
        } else {
            MediaView mediaView = MediaViewBuilder.create().mediaPlayer(player).build();

            Pane root = new Pane() {

                @Override
                protected void layoutChildren() {
                    layoutInArea(getChildren().get(0), 0, 0, getWidth(), getHeight(), 0,
                            HPos.CENTER, VPos.CENTER);
                }
            };
            mediaView.fitWidthProperty().bind(root.widthProperty());
            mediaView.fitHeightProperty().bind(root.heightProperty());
            mediaView.setSmooth(true);
            mediaView.setPreserveRatio(true);
            root.getChildren().add(mediaView);
            Scene scene = SceneBuilder.create().root(root).build();
            StageBuilder.create().scene(scene).title("Instant Bad Apple").applyTo(stage);

            root.setPrefWidth(300);
            root.setPrefHeight(240);

            stage.initStyle(StageStyle.UNDECORATED);
            stage.setFullScreen(true);
            stage.setOpacity(0.7);

            player.setOnReady(new Runnable() {

                @Override
                public void run() {
                    player.setVolume(0.3);
                    player.play();
                }
            });
            Runnable finish = new Runnable() {

                @Override
                public void run() {
                    player.dispose();
                    Platform.exit();
                }
            };
            player.setOnEndOfMedia(finish);
            player.setOnError(finish);
            player.setOnStopped(finish);
            mediaView.setOnMouseClicked(new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    player.dispose();
                    Platform.exit();
                }
            });
        }

        stage.show();
    }

    @Override
    public void stop() {
        System.exit(0);
    }

    private MediaPlayer getMediaPlayer() throws Exception {

        URL url = new URL(
                "http://www.youtube.com/results?search_query=bad+apple&search_sort=video_view_count");

        String searchHtml = null;
        {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(),
                    "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                searchHtml = sb.toString();
            }
        }

        String videoUrl = null;
        {
            Pattern pattern = Pattern
                    .compile("<li[^>]*\\sdata-context-item-views\\s*=\\s*\"再生回数\\s*([\\d,]*)\\s*回\"[^>]*>"
                            + ".*?<a[^>]*\\shref\\s*=\\s*\"(/watch[^\"]*)\"[^>]*>.*?</li>");
            Matcher matcher = pattern.matcher(searchHtml);
            int count = Integer.MIN_VALUE;
            Exception _ex = null;
            while (matcher.find()) {
                String cnt = matcher.group(1);
                String vdurl = matcher.group(2);
                Integer _cnt = null;
                try {
                    _cnt = Integer.valueOf(cnt.replace(",", ""));
                } catch (NullPointerException | NumberFormatException e) {
                    _ex = e;
                }
                if (_cnt != null) {
                    int _c = _cnt.intValue();
                    if (count < _c) {
                        videoUrl = vdurl;
                        count = _c;
                    }
                }
            }
            if ((videoUrl == null) && (_ex != null)) {
                throw _ex;
            }
        }

        String actualUrl = YoutubeUtils.extractUrl("http://www.youtube.com" + videoUrl);

        Media media = new Media(actualUrl);

        MediaPlayer mediaPlayer = new MediaPlayer(media);

        return mediaPlayer;
    }
}
