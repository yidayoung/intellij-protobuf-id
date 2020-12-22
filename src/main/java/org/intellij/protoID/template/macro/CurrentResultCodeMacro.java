package org.intellij.protoID.template.macro;

import com.intellij.codeInsight.template.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import idea.plugin.protoeditor.lang.psi.PbSimpleField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurrentResultCodeMacro extends Macro {

  @Override
  public String getName() {
    return "currentResultCode";
  }

  @Override
  public String getPresentableName() {
    return "currentResultCode()";
  }

  @Nullable
  @Override
  public Result calculateResult(@NotNull Expression[] params, ExpressionContext context) {
    if (params.length != 0) return null;
    final int offset = context.getStartOffset();
    final Project project = context.getProject();
    Editor editor = context.getEditor();
    if (editor == null) return null;
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    PsiElement element = file != null ? file.findElementAt(offset) : null;
    if (element == null) return null;
    PbSimpleField pbSimpleField = PsiTreeUtil.getParentOfType(element, PbSimpleField.class);
    pbSimpleField = pbSimpleField == null ? PsiTreeUtil.getNextSiblingOfType(element, PbSimpleField.class):pbSimpleField;
    if (pbSimpleField == null) return null;
    int max = maxCommentID(pbSimpleField.getComments());
    return new TextResult(String.valueOf(max));
  }

  private int maxCommentID(List<PsiComment> comments) {
    int max = 2;
    Pattern pattern = Pattern.compile("(\\d+)-([^-]+)");
    for (PsiComment comment : comments) {
      Matcher matcher = pattern.matcher(comment.getText().replace(" ", ""));
      if (matcher.find()) {
        int id = Integer.parseInt(matcher.group(1));
        if (id >= max) max = id + 1;
      }
    }
    return max;
  }
}
