# javafx-pdf
- Hacky but working implemntation for displaying PDF document in JavaFX with basic zoom, scroll, and go-to-page functionality
- Highly customizable (you can change button skins, implement additional features, etc)
- No additional dependencies apart from PDFBox `org.apache.pdfbox:pdfbox` for loading of PDF images

# Usage
New "Java Class" file in your project, name it "DocumentView", copy and paste the code.
### Initialize and use like any other JavaFX element
```java
var doc_view = new DocumentView();
doc_view.loadPDF(f);
```
### Getters
```java
doc_view.getZoom();
doc_view.getCurrentPage();
```
### Setters and Actions
```java
doc_view.setZoom(100);
doc_view.gotoPage(5);
```
## Minimal JavaFX Example
```java
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        var root = new BorderPane();

        // Init DocumentView
        var doc_view = new DocumentView();
        doc_view.setPadding(new Insets(20));
        root.setCenter(doc_view);

        // Load PDF button
        var btn = new Button("Load PDF");
        root.setTop(btn);

        // Button binding
        btn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf"));
            fc.setInitialDirectory(new File(System.getProperty("user.home") + "/Desktop"));
            File f = fc.showOpenDialog(stage);
            if (f != null) doc_view.loadPDF(f);
        });
        // window
        stage.setTitle("Bare Minimum JavaFX PDF Viewer Demo");
        stage.setWidth(600); stage.setHeight(400);
        stage.setScene(new Scene(root));
        stage.show();
    }
}
```
### Preview
<img src="https://github.com/woonseah/javafx-pdf/assets/90259138/328e898b-1065-4597-a963-4fc0885b50b6.png" width="600">

# Implementation
PDF pages extracted as BufferedImage, then converted to FXImage and stored as ImageView. VBox, nested inside a ScrollPane, holds all the ImageView. Necessary bindings are implemented and handled in its own code such as scaling and repositioning of the VBox. The structure is as follows:

- DocumentView class (extends BorderPane)
   - Top: MockToolbar
   - Center: PageView

- MockToolbar (HBox)
   - TextField <- current_page
   - "/"
   - Label <- page_count
   - ---------- Seperator ----------
   - Button <- zoom_out
   - TextField <- zoom_level
   - Button <- zoom_in
   - ---------- Seperator ----------
   - Button <- fit to page
   - Button <- rotate

- PageView (ScrollPane)
   - Group
      - Pane
         - VBox
            - ImageView (page 1)
            - ImageView (page 2)
            - ImageView (page 3)
            - ...
# Making it nicer
- Button icons using ikonli
```java
var zoom_btn1 = new Button("", new FontIcon(Feather.MINUS));
var zoom_btn2 = new Button("", new FontIcon(Feather.PLUS));
var fit_btn = new Button("", new FontIcon(Feather.CODE));
var rotate_btn = new Button("", new FontIcon(Feather.ROTATE_CW));
```
- With AtlantaFX base and PrimerDark theme
```java
Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
// add to end of DocumentView.createToolbar() before return
for (Node n : ret.getChildren()) {
    n.getStyleClass().addAll(Styles.FLAT, Styles.BUTTON_CIRCLE, Styles.DENSE, Styles.TEXT_SMALL, Styles.SMALL, Styles.BUTTON_ICON);
    n.setStyle("-fx-padding: 4;");
}
```
### Preview
<img src="https://github.com/woonseah/javafx-pdf/assets/90259138/f7ba66f7-a905-422c-a7c4-34786cfe365d.png" width="600">

# Reason and Ideas
 - Existing libraries are broken or outdated (problems with PDF.js and changes to WebView in JavaFX 17+)
 - You can modify the code and add overlays like Rectangle on each page by adding to the ImageView parent Pane
 - You can bind current_page property on change to load page information or page glyphs from the PDDocument.
