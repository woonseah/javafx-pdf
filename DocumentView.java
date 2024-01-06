import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

public class DocumentView extends BorderPane {
    private static final int MIN_ZOOM = 25, MAX_ZOOM = 500;
    private static final int[] zoom_steps = new int[] { 25, 33, 50, 67, 75, 80, 90, 100, 110, 125, 150, 175, 200, 250, 300, 400, 500 };
    private final VBox pages = new VBox(20);
    private final ScrollPane page_view;
    private final IntegerProperty page_count = new SimpleIntegerProperty(1);
    private final IntegerProperty current_page = new SimpleIntegerProperty(1);
    private final IntegerProperty zoom_level = new SimpleIntegerProperty(100);

    public DocumentView() {
        this.page_view = createPageView();
        this.setTop(createMockToolbar());
        this.setCenter(page_view);
        //clipChildren(page_view);
    }
    public void loadPDF(File f) {
        this.pages.getChildren().clear();
        current_page.set(1); zoom_level.set(100);

        Thread th = new Thread(() -> {
            try (PDDocument document = Loader.loadPDF(f)) {
                Platform.runLater(() -> { page_count.set(document.getNumberOfPages()); });
                PDFRenderer renderer = new PDFRenderer(document);
                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    var img_view = new ImageView(fxImageConvert(renderer.renderImage(i)));
                    Platform.runLater(() -> pages.getChildren().add(new Pane(img_view)));
                }
            } catch (IOException e) { throw new RuntimeException(e); }
        });
        th.setDaemon(true);
        th.start();
    }
    public void setZoom(int z) {
        updateZoom(z);
    }
    public int getZoom() { return this.zoom_level.get(); }
    public int getCurrentPage() { return this.current_page.get(); }
    public void gotoPage(int n) {
        n = Integer.max(1, Integer.min(page_count.get(), n));
        double val = 0;
        for (int i = 0; i < page_count.get(); i++) {
            if (i+1 == n) {
                page_view.setVvalue(val / (pages.getHeight() - getHeight()));
                current_page.set(n);
                return;
            } else val += ((Pane)pages.getChildren().get(i)).getHeight() + pages.getSpacing();
        }
    }
    private void updateZoom(int val) {
        // keep zoom pivot on mouse
        double tmp_x = page_view.getHvalue();
        double tmp_y = page_view.getVvalue();

        this.zoom_level.set(Integer.max(MIN_ZOOM, Integer.min(MAX_ZOOM, val)));
        this.pages.setScaleX(this.zoom_level.get()/100.0);
        this.pages.setScaleY(this.zoom_level.get()/100.0);
        page_view.setHvalue(tmp_x); page_view.setVvalue(tmp_y);
    }
    private HBox createMockToolbar() {
        var page_tf = new TextField("1");
        page_tf.setMaxWidth(30);
        page_tf.setTextFormatter(getFormatter("[0-9]*"));
        current_page.addListener((o, oldVal, newVal) -> page_tf.setText(newVal.toString()));
        page_tf.setOnAction(e -> { // on ENTER
            var s = page_tf.getText().replaceAll("[^0-9]*", "");
            if (!s.isBlank()) gotoPage(Integer.parseInt(s));
            page_tf.setText(String.valueOf(current_page.get()));
        });
        page_tf.focusedProperty().addListener((o, oldVal, newVal) -> {
            if (newVal) return; // on LOST_FOCUSED only
            var s = page_tf.getText().replaceAll("[^0-9]*", "");
            if (!s.isBlank()) gotoPage(Integer.parseInt(s));
            page_tf.setText(String.valueOf(current_page.get()));
        });

        var page_label = new Label();
        page_label.textProperty().bind(page_count.asString());
        //////////////////////////////////////////////////////////////////

        var zoom_tf = new TextField("100%");
        zoom_tf.setMaxWidth(50);
        zoom_tf.setTextFormatter(getFormatter("[0-9%]*"));
        zoom_level.addListener((o) -> zoom_tf.setText(zoom_level.get() + "%"));
        zoom_tf.setOnAction(e -> { // on ENTER
            var s = zoom_tf.getText().replaceAll("[^0-9]*", "");
            if (!s.isBlank()) setZoom(Integer.parseInt(s));
            zoom_tf.setText(zoom_level.get() + "%");
        });
        zoom_tf.focusedProperty().addListener((o, oldVal, newVal) -> {
            if (newVal) return; // on LOST_FOCUSED only
            var s = zoom_tf.getText().replaceAll("[^0-9]*", "");
            if (!s.isBlank()) setZoom(Integer.parseInt(s));
            zoom_tf.setText(zoom_level.get() + "%");
        });

        //var zoom_btn1 = new Button("-");
        var zoom_btn1 = new Button("", new FontIcon(Feather.MINUS));
        zoom_btn1.setOnAction(e -> { // Zoom Out - get step before
            for (int i = zoom_steps.length-1; i > 0; i--) {
                if (zoom_steps[i] >= zoom_level.get()) continue;
                setZoom(zoom_steps[i]);
                zoom_tf.setText(zoom_level.get() + "%");
                return;
            }
            setZoom(MIN_ZOOM);
        });
        //var zoom_btn2 = new Button("+");
        var zoom_btn2 = new Button("", new FontIcon(Feather.PLUS));
        zoom_btn2.setOnAction(e -> { // Zoom In - get step after
            for (int zoomStep : zoom_steps) {
                if (zoomStep <= zoom_level.get()) continue;
                setZoom(zoomStep);
                zoom_tf.setText(zoom_level.get() + "%");
                return;
            }
            setZoom(MAX_ZOOM);
        });
        //////////////////////////////////////////////////////////////////
        //var fit_btn = new Button("Fit");
        var fit_btn = new Button("", new FontIcon(Feather.CODE));
        fit_btn.setOnAction(e -> {
            if (pages.getChildren().isEmpty()) return;
            double w = ((Pane)pages.getChildren().getFirst()).getWidth();
            setZoom((int) (100 * (getWidth() - 100) / w));
            zoom_tf.setText(zoom_level.get() + "%");
        });
        //var rotate_btn = new Button("Rotate");
        var rotate_btn = new Button("", new FontIcon(Feather.ROTATE_CW));
        rotate_btn.setOnAction(e -> {
            // not really implemented yet but a hacky way of doing it
            for (Node n : pages.getChildren()) n.setRotate(n.getRotate() + 90);
        });

        HBox ret =new HBox(10);
        ret.setAlignment(Pos.CENTER);
        ret.setPadding(new Insets(5));
        ret.getChildren().setAll(
                page_tf, new Label("/"), page_label,
                new Separator(Orientation.VERTICAL),
                zoom_btn1, zoom_tf, zoom_btn2,
                new Separator(Orientation.VERTICAL),
                fit_btn, rotate_btn
        );

        for (Node n : ret.getChildren()) {
            n.getStyleClass().addAll(Styles.FLAT, Styles.BUTTON_CIRCLE, Styles.DENSE, Styles.TEXT_SMALL, Styles.SMALL, Styles.BUTTON_ICON);
            n.setStyle("-fx-padding: 4;");
        }
        return ret;
    }

    private ScrollPane createPageView() {
        ScrollPane ret = new ScrollPane();
        ret.setBackground(Background.fill(Color.GREY));
        pages.setPadding(new Insets(20));
        pages.layoutXProperty().bind(ret.widthProperty().subtract(pages.widthProperty()).divide(2));
        pages.layoutYProperty().bind(pages.heightProperty().multiply(pages.scaleYProperty().subtract(1).divide(2)));
        var pane = new Pane(pages); // need pane to offset VBox in Group
        pane.setPrefSize(0, 0); // don't resize scrollbars for this
        ret.setContent(new Group(pane)); // group to "reset" layout coordinates

        // update current_page on scroll
        ret.vvalueProperty().addListener(e -> {
            if (pages.getChildren().isEmpty()) { current_page.set(1); return; }
            final double scale_y = pages.getScaleY(), spacing = pages.getSpacing();
            double val = (scale_y * pages.getHeight() - ret.getHeight()) * ret.getVvalue() + ret.getHeight()/2;

            double c = 0;
            for (Node p : pages.getChildren()) {
                c += scale_y * ( ((Pane) p).getHeight() + spacing );
                if (c > val) { current_page.set(pages.getChildren().indexOf(p)+1); return; }
            }
            current_page.set(pages.getChildren().size());
        });

        // adding zoom functionality (hope it works for you too)
        //  - events recorded when doing zoom gestures on my laptop's touchpad
        //  - without shift: (x=0, y=32) or (x=0, y=-32)
        //  - with shift: (x=32, y=0) or (x=-32, y=0)
        addEventFilter(ScrollEvent.SCROLL, e -> {
            double x = e.getDeltaX(); double y = e.getDeltaY();
            if ((x == 0 && y == 32) || (x == 32 && y == 0)) { // zoom in
                var m = (zoom_level.get() >= 200) ? 25 : (zoom_level.get() >= 80) ? 10 : 5; // m = 25 at 200, zoom in
                setZoom(zoom_level.get() + m); e.consume();
            } else if ((x == 0 && y == -32) || (x == -32 && y == 0)) { // zoom out
                var m = (zoom_level.get() > 200) ? 25 : (zoom_level.get() >= 80) ? 10 : 5; // m = 10 at 200
                setZoom(zoom_level.get() - m); e.consume();
            }
        });

        return ret;
    }
    private static TextFormatter<String> getFormatter(String s) { return new TextFormatter<>(c -> c.getText().matches(s) ? c : null); }
    // https://news.kynosarges.org/2016/11/03/javafx-pane-clipping/
    private static void clipChildren(Region region) {
        final Rectangle outputClip = new Rectangle();
        region.setClip(outputClip);

        region.layoutBoundsProperty().addListener((ov, oldValue, newValue) -> {
            outputClip.setWidth(newValue.getWidth());
            outputClip.setHeight(newValue.getHeight());
        });
    }

    // https://stackoverflow.com/questions/30970005/bufferedimage-to-javafx-image
    private Image fxImageConvert(BufferedImage img) {
        //converting to a good type, read about types here: https://openjfx.io/javadoc/13/javafx.graphics/javafx/scene/image/PixelBuffer.html
        BufferedImage newImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        newImg.createGraphics().drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
        //converting the BufferedImage to an IntBuffer
        int[] type_int_agrb = ((DataBufferInt) newImg.getRaster().getDataBuffer()).getData();
        IntBuffer buffer = IntBuffer.wrap(type_int_agrb);
        //converting the IntBuffer to an Image, read more about it here: https://openjfx.io/javadoc/13/javafx.graphics/javafx/scene/image/PixelBuffer.html
        PixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbPreInstance();
        PixelBuffer<IntBuffer> pixelBuffer = new PixelBuffer(newImg.getWidth(), newImg.getHeight(), buffer, pixelFormat);
        return new WritableImage(pixelBuffer);
    }
}
