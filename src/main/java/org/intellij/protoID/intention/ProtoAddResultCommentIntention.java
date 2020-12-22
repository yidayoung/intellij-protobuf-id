package org.intellij.protoID.intention;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.TemplateSettings;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import idea.plugin.protoeditor.lang.PbLanguage;
import idea.plugin.protoeditor.lang.psi.PbSimpleField;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProtoAddResultCommentIntention extends BaseIntentionAction {
  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getFamilyName() {
    return "Proto id";
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getText() {
    return "Add result comment";
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    if (!file.getManager().isInProject(file)) return false;
    if (!file.getLanguage().is(PbLanguage.INSTANCE)) return false;
    int offset = editor.getCaretModel().getOffset();
    PbSimpleField simpleField = findSimpleField(file, offset);
    if (simpleField == null) return false;
    PsiElement nameIdentifier = simpleField.getNameIdentifier();
    if (nameIdentifier == null) return false;
    if (!nameIdentifier.getText().startsWith("result")) return false;
    TextRange textRange = nameIdentifier.getTextRange();
    return textRange.contains(offset);
  }

  private PbSimpleField findSimpleField(PsiFile file, int offset) {
    PsiElement element = file.findElementAt(offset);
    if (element == null) return null;
    return PsiTreeUtil.getParentOfType(element, PbSimpleField.class);
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    int offset = editor.getCaretModel().getOffset();
    PbSimpleField simpleField = findSimpleField(file, offset);
    if (simpleField == null) return;
    PsiElement prevSibling = simpleField.getPrevSibling();
    String insert_text;
    if (prevSibling instanceof PsiWhiteSpace){
      offset = prevSibling.getTextRange().getStartOffset();
      insert_text = prevSibling.getText()+"rc";
    }
    else {
      offset = simpleField.getTextRange().getStartOffset();
      insert_text = "rc";
    }
    editor.getDocument().insertString(offset, insert_text);
    editor.getCaretModel().moveToOffset(offset+insert_text.length());
    TemplateManager templateManager = TemplateManager.getInstance(project);
    templateManager.startTemplate(editor, TemplateSettings.TAB_CHAR);
  }
}
