package com.ddlab.rnd.ui.dialog;

import com.ddlab.rnd.ui.CodePublishPanelComponent;
import com.ddlab.rnd.ui.util.CodeSharingUtil;
import com.ddlab.rnd.ui.util.CommonUIUtil;
import com.ddlab.rnd.ui.util.UIUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.DocumentAdapter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.io.File;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class CodeSharingDialog extends DialogWrapper {

    private JPanel panel;
    private CodePublishPanelComponent codePublishPanelComponent;
    private Project project;

    public CodeSharingDialog(@Nullable Project project, File selectedRepo, boolean canBeParent) {
        super(project, canBeParent);
        this.project = project;
        setTitle("Share code in Github,Gitlab or Bitbucket ");
        setOKActionEnabled(false);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        panel = createUIAndGetPanel();
//        showMessage();
        attachValidationListener();
        return panel;
    }

    @Override
    protected void doOKAction() {
        saveLastSession();
        close(1);
        shareYourCode();
    }


    // ~~~~~~~~ private methods ~~~~~~~~
    private JPanel createUIAndGetPanel() {
        codePublishPanelComponent = new CodePublishPanelComponent();
        return codePublishPanelComponent.getMainPanel();
    }

    private void saveLastSession() {
        JComboBox hostedGitTypeCombo = codePublishPanelComponent.getHostedGitTypeCombo();
        JComboBox slGitUserNameCombo = codePublishPanelComponent.getSlGitUserNameCombo();
        UIUtil.saveLastSessionSetting(hostedGitTypeCombo, slGitUserNameCombo);
    }

    private void attachValidationListener() {
        JTextArea textArea = codePublishPanelComponent.getTextArea();
        textArea.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                validateInput();
            }
        });
    }

    private void validateInput() {
        JComboBox gitTypeCombo = codePublishPanelComponent.getHostedGitTypeCombo();
        JComboBox slGitUserNameCombo = codePublishPanelComponent.getSlGitUserNameCombo();

        boolean valid = !codePublishPanelComponent.getTextArea().getText().trim().isEmpty()
                && gitTypeCombo.getSelectedItem() != null
                && slGitUserNameCombo.getSelectedItem() != null;

        setOKActionEnabled(valid);
    }

    private void shareYourCode() {
        CompletableFuture<String> future = CodeSharingUtil.performSharing(project,codePublishPanelComponent);
        future.thenAccept(result -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                // Perform the logic
                if (result.equalsIgnoreCase("Success")) {
                    CommonUIUtil.notifyInfo(project, "Codebase hosted successfully");
                }
            });
        }).exceptionally(ex -> {
            log.error("Exception while sharing the code in Hosted Git: {}", ex);
            ApplicationManager.getApplication().invokeLater(() ->
                    Messages.showErrorDialog("Exception while hosting the codebase: " + ex.getMessage(), "Gitifier"));
            return null;
        });
    }

}
