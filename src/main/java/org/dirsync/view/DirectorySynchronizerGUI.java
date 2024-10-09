package org.dirsync.view;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

public class DirectorySynchronizerGUI extends Application {
    
    private Label statusLabel;
    private TextArea logArea;
    private File sourceFolder;
    private File targetFolder;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Directory Synchronizer v1.0");

        // Layout
        BorderPane root = new BorderPane();
        VBox centerPane = new VBox(10);
        centerPane.setPadding(new Insets(20));
        HBox topPane = new HBox(20);
        HBox folderButtonsPane = new HBox(10);
        
        // Folder selectors
        Button sourceButton = new Button();
        Button targetButton = new Button();

        sourceButton.setGraphic(new ImageView(new Image("browse_folder.png")));
        targetButton.setGraphic(new ImageView(new Image("browse_folder.png")));
        
        Label sourceLabel = new Label("Source folder:");
        Label targetLabel = new Label("Target folder:");
        
        Label selectedSource = new Label("No folder selected");
        Label selectedTarget = new Label("No folder selected");

        sourceButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            sourceFolder = directoryChooser.showDialog(primaryStage);
            if (sourceFolder != null) {
                selectedSource.setText(sourceFolder.getAbsolutePath());
            }
        });

        targetButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            targetFolder = directoryChooser.showDialog(primaryStage);
            if (targetFolder != null) {
                selectedTarget.setText(targetFolder.getAbsolutePath());
            }
        });

        folderButtonsPane.getChildren().addAll(sourceButton, targetButton);
        folderButtonsPane.setAlignment(Pos.CENTER);
        
        // Synchronize button
        Button syncButton = new Button();
        syncButton.setGraphic(new ImageView(new Image("play.png")));
        syncButton.setOnAction(e -> synchronizeDirectories());
        
        // Status label
        statusLabel = new Label("Status: Idle");
        
        // Log area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);

        // Layout setup
        topPane.getChildren().addAll(sourceLabel, selectedSource, targetLabel, selectedTarget);
        topPane.setAlignment(Pos.CENTER);
        
        centerPane.getChildren().addAll(folderButtonsPane, topPane, syncButton, statusLabel, logArea);
        centerPane.setAlignment(Pos.CENTER);

        root.setCenter(centerPane);
        
        Scene scene = new Scene(root, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void synchronizeDirectories() {
        // Logic to synchronize directories
        if (sourceFolder == null || targetFolder == null) {
            statusLabel.setText("Status: Please select both source and target folders.");
            return;
        }
        statusLabel.setText("Status: Synchronizing...");
        logArea.appendText("Started synchronizing " + sourceFolder.getName() + " with " + targetFolder.getName() + "\n");

        // Simulate synchronization task
//        new Thread(() -> {
//            try {
//                Thread.sleep(2000);  // Simulate time taken to sync
//                logArea.appendText("Synchronization complete!\n");
//                statusLabel.setText("Status: Complete");
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                logArea.appendText("Synchronization interrupted!\n");
//                statusLabel.setText("Status: Interrupted");
//            }
//        }).start();
    }
}
