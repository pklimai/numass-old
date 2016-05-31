/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.workbench;

import hep.dataforge.context.GlobalContext;
import inr.numass.NumassContext;
import java.io.IOException;
import java.text.ParseException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 *
 * @author Alexander Nozik
 */
public class Workbench extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException, ParseException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NumassWorkbench.fxml"));
        Parent parent = loader.load();

        Scene scene = new Scene(parent, 800, 600);

        NumassWorkbenchController controller = loader.getController();
        controller.setContextFactory(NumassContext::new);

        primaryStage.setTitle("Numass workbench");
        primaryStage.setScene(scene);
        primaryStage.show();

        scene.getWindow().setOnCloseRequest((WindowEvent event) -> {
            controller.getContext().processManager().getRootProcess().cancel(true);
        });
    }

    @Override
    public void stop() throws Exception {
        GlobalContext.instance().close();
        super.stop();
    }
    
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
