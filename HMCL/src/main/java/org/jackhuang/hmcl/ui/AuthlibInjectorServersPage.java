package org.jackhuang.hmcl.ui;

import com.jfoenix.controls.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.jackhuang.hmcl.Launcher;
import org.jackhuang.hmcl.auth.authlibinjector.AuthlibInjectorServer;
import org.jackhuang.hmcl.setting.Settings;
import org.jackhuang.hmcl.task.Schedulers;
import org.jackhuang.hmcl.task.Task;
import org.jackhuang.hmcl.ui.animation.ContainerAnimations;
import org.jackhuang.hmcl.ui.animation.TransitionHandler;
import org.jackhuang.hmcl.ui.construct.SpinnerPane;
import org.jackhuang.hmcl.ui.wizard.DecoratorPage;
import org.jackhuang.hmcl.util.NetworkUtils;

import static java.util.stream.Collectors.toList;

import java.io.IOException;

public class AuthlibInjectorServersPage extends StackPane implements DecoratorPage {
    private final StringProperty title = new SimpleStringProperty(this, "title", Launcher.i18n("account.injector.server"));

    @FXML private ScrollPane scrollPane;
    @FXML private StackPane addServerContainer;
    @FXML private Label lblServerUrl;
    @FXML private Label lblServerName;
    @FXML private Label lblCreationWarning;
    @FXML private Label lblServerWarning;
    @FXML private VBox listPane;
    @FXML private JFXTextField txtServerUrl;
    @FXML private JFXDialogLayout addServerPane;
    @FXML private JFXDialogLayout confirmServerPane;
    @FXML private JFXDialog dialog;
    @FXML private StackPane contentPane;
    @FXML private SpinnerPane nextPane;
    @FXML private JFXButton btnAddNext;

    private final TransitionHandler transitionHandler;

    private AuthlibInjectorServer serverBeingAdded;

    {
        FXUtils.loadFXML(this, "/assets/fxml/authlib-injector-servers.fxml");
        FXUtils.smoothScrolling(scrollPane);
        transitionHandler = new TransitionHandler(addServerContainer);

        getChildren().remove(dialog);
        dialog.setDialogContainer(this);

        txtServerUrl.textProperty().addListener((a, b, newValue) ->
                btnAddNext.setDisable(!txtServerUrl.validate()));

        reload();
    }

    private void removeServer(AuthlibInjectorServerItem item) {
        Settings.SETTINGS.authlibInjectorServers.remove(item.getServer());
        reload();
    }

    private void reload() {
        listPane.getChildren().setAll(
                Settings.SETTINGS.authlibInjectorServers.stream()
                        .map(server -> new AuthlibInjectorServerItem(server, this::removeServer))
                        .collect(toList()));
        if (Settings.SETTINGS.authlibInjectorServers.isEmpty()) {
            onAdd();
        }
    }

    @FXML
    private void onAdd() {
        transitionHandler.setContent(addServerPane, ContainerAnimations.NONE.getAnimationProducer());
        txtServerUrl.setText("");
        txtServerUrl.resetValidation();
        lblCreationWarning.setText("");
        addServerPane.setDisable(false);
        nextPane.hideSpinner();
        dialog.show();
    }

    @FXML
    private void onAddCancel() {
        dialog.close();
    }

    @FXML
    private void onAddNext() {
        String url = fixInputUrl(txtServerUrl.getText());

        nextPane.showSpinner();
        addServerPane.setDisable(true);

        Task.of(() -> {
            serverBeingAdded = AuthlibInjectorServer.fetchServerInfo(url);
        }).finalized(Schedulers.javafx(), (variables, isDependentsSucceeded) -> {
            nextPane.hideSpinner();
            addServerPane.setDisable(false);

            if (isDependentsSucceeded) {
                lblServerName.setText(serverBeingAdded.getName());
                lblServerUrl.setText(serverBeingAdded.getUrl());

                lblServerWarning.setVisible("http".equals(NetworkUtils.toURL(serverBeingAdded.getUrl()).getProtocol()));

                transitionHandler.setContent(confirmServerPane, ContainerAnimations.SWIPE_LEFT.getAnimationProducer());
            } else {
                lblCreationWarning.setText(resolveFetchExceptionMessage(variables.<Exception>get("lastException")));
            }
        }).start();

    }

    @FXML
    private void onAddPrev() {
        transitionHandler.setContent(addServerPane, ContainerAnimations.SWIPE_RIGHT.getAnimationProducer());
    }

    @FXML
    private void onAddFinish() {
        if (!Settings.INSTANCE.SETTINGS.authlibInjectorServers.contains(serverBeingAdded)) {
            Settings.INSTANCE.SETTINGS.authlibInjectorServers.add(serverBeingAdded);
        }
        reload();
        dialog.close();
    }

    public String getTitle() {
        return title.get();
    }

    @Override
    public StringProperty titleProperty() {
        return title;
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    private String fixInputUrl(String url) {
        if (!url.endsWith("/")) {
            url += "/";
        }
        return url;
    }

    private String resolveFetchExceptionMessage(Throwable exception) {
        if (exception instanceof IOException) {
            return Launcher.i18n("account.failed.connect_injector_server");
        } else {
            return exception.getClass() + ": " + exception.getLocalizedMessage();
        }
    }
}
