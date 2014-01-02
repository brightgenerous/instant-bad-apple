package com.brightgenerous.instantbadapple;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.animation.FadeTransition;
import javafx.animation.FadeTransitionBuilder;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.animation.TranslateTransitionBuilder;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.ImageCursor;
import javafx.scene.Scene;
import javafx.scene.SceneBuilder;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.effect.DropShadowBuilder;
import javafx.scene.effect.Effect;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.FlowPaneBuilder;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaMarkerEvent;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.media.MediaViewBuilder;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextBuilder;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageBuilder;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class InstantBadAppleApplication extends Application {

    public static void main(String[] args) {
        InstantBadAppleApplication.launch(args);
    }

    double[] whiteBlacks = new double[] { 1.40, 14.76, 27.33, 42.06, 56.16, 57.96, 74.53, 83.56,
            91.40, 94.50, 110.73, 120.80, 121.66, 122.53, 126.50, 142.03, 154.43, 181.03, 204.60,
            209.93, 215.60, 217.13 };

    Timeline timeline;

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
            StageBuilder.create().scene(scene).width(300).height(240)
                    .title("Instant Bad Apple - Error").applyTo(stage);
        } else {
            final Pane mediaPane = new Pane() {

                @Override
                protected void layoutChildren() {
                    layoutInArea(getChildren().get(0), 0, 0, getWidth(), getHeight(), 0,
                            HPos.CENTER, VPos.CENTER);
                }
            };
            final MediaView mediaView = MediaViewBuilder.create().mediaPlayer(player).build();
            mediaView.fitWidthProperty().bind(mediaPane.widthProperty());
            mediaView.fitHeightProperty().bind(mediaPane.heightProperty());
            mediaView.setSmooth(true);
            mediaView.setPreserveRatio(true);
            mediaPane.getChildren().add(mediaView);
            mediaPane.prefWidthProperty().bind(stage.widthProperty());
            mediaPane.prefHeightProperty().bind(stage.heightProperty());

            final ImageCursor iconW = new ImageCursor(new Image(
                    InstantBadAppleApplication.class.getResourceAsStream("icon_w.png")));
            final ImageCursor iconB = new ImageCursor(new Image(
                    InstantBadAppleApplication.class.getResourceAsStream("icon_b.png")));

            mediaPane.setCursor(iconB);
            mediaPane.setStyle("-fx-background-color:black;");

            for (int i = 0; i < whiteBlacks.length; i++) {
                double wb = whiteBlacks[i];
                String key = ((i % 2) == 0) ? String.format("%02d-white", Integer.valueOf(i))
                        : String.format("%02d-black", Integer.valueOf(i));
                player.getMedia().getMarkers().put(key, Duration.seconds(wb));
            }

            final Group group = new Group();
            group.getChildren().add(mediaPane);

            final ObjectProperty<Color> textFill = new SimpleObjectProperty<>(Color.BLACK);
            final Effect effectWhite = DropShadowBuilder.create().color(Color.BLACK).build();
            final Effect effectBlack = DropShadowBuilder.create().color(Color.WHITE).build();
            final ObjectProperty<Effect> effect = new SimpleObjectProperty<>(effectBlack);

            player.setOnMarker(new EventHandler<MediaMarkerEvent>() {

                @Override
                public void handle(MediaMarkerEvent event) {
                    if (event.getMarker().getKey().endsWith("white")) {
                        mediaPane.styleProperty().set("-fx-background-color:white;");
                        mediaPane.cursorProperty().setValue(iconW);
                        textFill.setValue(Color.WHITE);
                        effect.setValue(effectWhite);
                    } else {
                        mediaPane.styleProperty().set("-fx-background-color:black;");
                        mediaPane.cursorProperty().setValue(iconB);
                        textFill.setValue(Color.BLACK);
                        effect.setValue(effectBlack);
                    }
                }
            });
            player.setOnReady(new Runnable() {

                @Override
                public void run() {
                    timeline = setupAnime(group, stage, textFill, effect);
                    player.setVolume(0.3);
                    player.play();
                }
            });
            player.setOnPlaying(new Runnable() {

                @Override
                public void run() {
                    timeline.play();
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

            mediaPane.setOnMouseClicked(new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    player.dispose();
                    Platform.exit();
                }
            });

            Scene scene = SceneBuilder.create().root(group).build();
            StageBuilder.create().scene(scene).title("Instant Bad Apple").applyTo(stage);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setMinWidth(300);
            stage.setMinHeight(240);
            stage.setOpacity(0.7);
        }

        boolean full = false;
        {
            String os = System.getProperty("os.name");
            full = (os != null) && os.toLowerCase().contains("windows");
        }
        if (full) {
            stage.setFullScreen(true);
        } else {
            Rectangle2D d = Screen.getPrimary().getVisualBounds();
            double w = d.getWidth();
            double h = d.getHeight();
            stage.setX(w * 0.05);
            stage.setY(h * 0.05);
            stage.setWidth(w * 0.9);
            stage.setHeight(h * 0.9);
        }
        stage.show();
    }

    private Timeline setupAnime(Group group, Stage stage, ObjectProperty<Color> textFill,
            ObjectProperty<Effect> effect) {

        double width = stage.getWidth();
        double height = stage.getHeight();
        double fontSize = height / 30;

        double delay = 0;

        List<KeyFrame> keyFrames = new ArrayList<>();

        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 3000, 3000,
                "画面をクリックすると終了します", fontSize, 0, -1, 1000, textFill, effect));

        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 1500, 20000, "少女祈祷中．．．",
                fontSize * 2, -1, -1, 1000, textFill, effect));

        keyFrames.add(createKeyFrameFlow(width, height, group, delay + 2000, 6000, "2014年　謹賀新年",
                fontSize * 5, 50, textFill, effect));
        keyFrames.add(createKeyFrameFlow(width, height, group, delay + 3000, 6000,
                "今年もよろしく！！＠ぶらいじぇん", fontSize * 3, 50 + (fontSize * 5), textFill, effect));

        keyFrames.add(createKeyFrameFlow(width, height, group, delay + 28000, 4000, "流れてく　時の中ででも",
                fontSize * 1.5, 50, textFill, effect));

        keyFrames.add(createKeyFrameFlow(width, height, group, delay + 30500, 3000, "け",
                fontSize * 1.5, height / 2, textFill, effect));
        keyFrames.add(createKeyFrameFlow(width, height, group, delay + 30500, 3100, "だ",
                fontSize * 1.5, (height / 2) + fontSize, textFill, effect));
        keyFrames.add(createKeyFrameFlow(width, height, group, delay + 30500, 3200, "る",
                fontSize * 1.5, (height / 2) + (fontSize * 2), textFill, effect));
        keyFrames.add(createKeyFrameFlow(width, height, group, delay + 30500, 3300, "さ",
                fontSize * 1.5, (height / 2) + (fontSize * 2), textFill, effect));
        keyFrames.add(createKeyFrameFlow(width, height, group, delay + 30500, 3400, "が",
                fontSize * 1.5, (height / 2) + fontSize, textFill, effect));

        keyFrames.add(createKeyFrameFlow(width, height, group, delay + 31500, 4500, "ほら", fontSize,
                (height / 2) - (fontSize * 3), textFill, effect));

        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 33700, 2100, "グ",
                fontSize * 2, (width / 2) - (fontSize * 6), (height / 2) - (fontSize * 6), 1500,
                textFill, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 33900, 2000, "ル",
                fontSize * 2, (width / 2) - (fontSize * 3.5), (height / 2) - (fontSize * 8), 1500,
                textFill, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 34100, 1900, "グ",
                fontSize * 2, (width / 2) - (fontSize * 0.5), (height / 2) - (fontSize * 6), 2000,
                textFill, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 34300, 1500, "ル",
                fontSize * 2, width / 2, (height / 2) - (fontSize * 4), 2000, textFill, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 34500, 1700, "廻",
                fontSize * 2, (width / 2) - (fontSize * 3.5), (height / 2) - (fontSize * 2), 1500,
                textFill, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 34700, 1600, "っ",
                fontSize * 2, (width / 2) - (fontSize * 5), (height / 2) - (fontSize * 4), 1500,
                textFill, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 34900, 1500, "て",
                fontSize * 2, (width / 2) - (fontSize * 3.5), (height / 2) - (fontSize * 4.5),
                1500, textFill, effect));

        keyFrames.add(createKeyFrameFlow(width, height, group, delay + 35500, 4000, "私から　離れる心も",
                fontSize * 3, (height / 2) + (fontSize * 2), textFill, effect));

        keyFrames.add(createKeyFrameFlow(width, height, group, delay + 38500, 4000, "見えないわ",
                fontSize * 2, height / 2, textFill, effect));
        keyFrames.add(createKeyFrameFlow(width, height, group, delay + 38800, 3500, "そ",
                fontSize * 4, (height / 2) - (fontSize * 4), textFill, effect));
        keyFrames.add(createKeyFrameFlow(width, height, group, delay + 39300, 3000, "う",
                fontSize * 4, (height / 2) + (fontSize * 2), textFill, effect));

        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 40800, 2700, "知",
                fontSize * 10, 0, 0, 0, textFill, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 41200, 2300, "ら",
                fontSize * 10, -1, 0, 0, textFill, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 41600, 1900, "な",
                fontSize * 10, -1, -1, 0, textFill, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 42000, 1500, "い",
                fontSize * 10, 0, -1, 0, textFill, effect));
        keyFrames.add(createKeyFrameFlow(width, height, group, delay + 41800, 2000, "？",
                fontSize * 10, (height / 2) - (fontSize * 3.5), textFill, effect));

        ObjectBinding<Color> textFillRev = Bindings.when(textFill.isEqualTo(Color.WHITE))
                .then(Color.BLACK).otherwise(Color.WHITE);

        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 44500, 5000, "こ",
                fontSize * 3, 0, 0, 1000, textFillRev, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 44500, 5000, "こ",
                fontSize * 3, 0, fontSize * 3, 1000, textFillRev, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 44500, 5000, "ま",
                fontSize * 3, 0, fontSize * 6, 1000, textFillRev, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 44500, 5000, "で",
                fontSize * 3, 0, fontSize * 9, 1000, textFillRev, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 44500, 5000, "し",
                fontSize * 3, 0, fontSize * 12, 1000, textFillRev, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 44500, 5000, "か",
                fontSize * 3, 0, fontSize * 15, 1000, textFillRev, effect));

        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 45000, 5000, "文",
                fontSize * 3, -1, 0, 1000, textFillRev, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 45000, 5000, "字",
                fontSize * 3, -1, fontSize * 3, 1000, textFillRev, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 45000, 5000, "打",
                fontSize * 3, -1, fontSize * 6, 1000, textFillRev, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 45000, 5000, "っ",
                fontSize * 3, -1, fontSize * 9, 1000, textFillRev, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 45000, 5000, "て",
                fontSize * 3, -1, fontSize * 12, 1000, textFillRev, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 45000, 5000, "ま",
                fontSize * 3, -1, fontSize * 15, 1000, textFillRev, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 45000, 5000, "せ",
                fontSize * 3, -1, fontSize * 18, 1000, textFillRev, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 45000, 5000, "ん",
                fontSize * 3, -1, fontSize * 21, 1000, textFillRev, effect));

        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 45000, 164000, "(´・ω・`)",
                fontSize, 0, -1, 0, textFill, effect));
        keyFrames.addAll(createKeyFrameFix(width, height, group, delay + 209000, 1000, "(´・ω:;.:…",
                fontSize, 0, -1, 3000, textFill, effect));

        keyFrames.add(createKeyFrameFlow(width, height, group, delay + 45000, 4000, "サーセンｗｗｗｗｗｗｗ",
                fontSize * 5, 50, textFill, effect));

        keyFrames.add(createKeyFrameFlow(width, height, group, delay + 50000, 4000,
                "以後、通常の動画をお楽しみください", fontSize * 3, -50, textFill, effect));

        return new Timeline(keyFrames.toArray(new KeyFrame[keyFrames.size()]));
    }

    private KeyFrame createKeyFrameFlow(double width, double height, final Group group,
            double start, double span, String str, double size, double top,
            ObservableValue<Color> textFill, ObservableValue<Effect> effect) {
        double _top = (0 <= top) ? top + (size * 0.8) : height + top;
        final Text label = TextBuilder.create().text(str).translateX(9999).translateY(_top)
                .focusTraversable(false).style("-fx-font-size:" + size).build();
        label.effectProperty().bind(effect);
        label.fillProperty().bind(textFill);

        final TranslateTransition translate = TranslateTransitionBuilder.create()
                .duration(Duration.millis(span)).node(label).fromX(width)
                .toX(0 - (str.length() * size * 0.8)).onFinished(new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {
                        group.getChildren().remove(label);
                    }
                }).build();
        return new KeyFrame(Duration.millis(start), new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                group.getChildren().add(label);
                translate.play();
            }
        });
    }

    private List<KeyFrame> createKeyFrameFix(double width, double height, final Group group,
            double start, double span, String str, double size, double left, double top,
            double fade, ObservableValue<Color> textFill, ObservableValue<Effect> effect) {
        double _left = (0 <= left) ? left : width - (str.length() * size * 0.8);
        double _top = (0 <= top) ? top + (size * 0.8) : height + top;
        final Text label = TextBuilder.create().text(str).translateX(_left).translateY(_top)
                .focusTraversable(false).style("-fx-font-size:" + size).build();
        label.effectProperty().bind(effect);
        label.fillProperty().bind(textFill);

        final FadeTransition fadeIn = FadeTransitionBuilder.create()
                .duration(Duration.millis(fade)).node(label).toValue(1.0).build();
        final FadeTransition fadeOut = FadeTransitionBuilder.create()
                .duration(Duration.millis(fade)).node(label).toValue(0)
                .onFinished(new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {
                        group.getChildren().remove(label);
                    }
                }).build();
        return Arrays.asList(new KeyFrame(Duration.millis(start), new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                group.getChildren().add(label);
                fadeIn.play();
            }
        }), new KeyFrame(Duration.millis(start + span), new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                fadeOut.play();
            }
        }));
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
                    .compile("<li[^>]*\\sdata-context-item-views\\s*=\\s*\"(?:再生回数)?\\s*([\\d,]*)\\s*(?:回)?\"[^>]*>"
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
