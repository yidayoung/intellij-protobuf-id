package org.intellij.protoID.inspection;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiEditorUtil;
import idea.plugin.protoeditor.lang.psi.PbMessageDefinition;
import idea.plugin.protoeditor.lang.psi.PbVisitor;
import org.intellij.protoID.IDTagUtil;
import org.intellij.protoID.PbTags;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PbReDefinedInspection extends ProtoIDInspectionBase {
  @Override
  protected PbVisitor buildPbVisitor(ProblemsHolder holder) {
    return new PbVisitor(){
      @Override
      public void visitMessageDefinition(@NotNull PbMessageDefinition o) {
        checkMessageComments(o.getComments(), holder);
      }
    };
  }

  private void checkMessageComments(List<PsiComment> comments, ProblemsHolder holder) {
    boolean idExist = false;
    boolean routeExist = false;
    for (PsiComment comment: comments){
      String text = comment.getText();
      TextRange routeRange = IDTagUtil.IndexTagInComment(text, PbTags.TAG_ROUTE);
      if (routeRange != null){
        if (routeExist)
          registerProblem(holder, comment, routeRange, "redefine route", ProblemHighlightType.ERROR, new PbRemoveCommentQuickFix(PbTags.TAG_ROUTE));
        routeExist = true;
      }
      TextRange idRange = IDTagUtil.IndexTagInComment(text, PbTags.TAG_ID);
      if (idRange != null){
        if (idExist)
          registerProblem(holder, comment, idRange, "redefine id", ProblemHighlightType.ERROR, new PbRemoveCommentQuickFix(PbTags.TAG_ID));
        idExist = true;
      }
    }
  }

  private static class PbRemoveCommentQuickFix implements LocalQuickFix {
    private final String myTag;

    public PbRemoveCommentQuickFix(String tag) {
      myTag = tag;
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
      return String.format("Remove %s in Comment",myTag);
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiElement psiElement = descriptor.getPsiElement();
      if (!(psiElement instanceof PsiComment)) return;
      Editor editor = PsiEditorUtil.findEditor(psiElement);
      if (editor == null) return;
      int offset = psiElement.getTextOffset();
      String text = psiElement.getText();
      TextRange routeRange = IDTagUtil.IndexTagInComment(text, myTag);
      if (routeRange == null) return;
      text = String.valueOf(StringUtil.replaceSubSequence(text, routeRange.getStartOffset(), routeRange.getEndOffset(), ""));
      if (text.trim().equals("//")){
        PsiElement nextSibling = psiElement.getNextSibling();
        if (nextSibling instanceof PsiWhiteSpace)
          nextSibling.delete();
        psiElement.delete();
      }
      else{
        text = "// " + StringUtil.trimStart(text, "//").trim();
        editor.getDocument().replaceString(offset, offset + psiElement.getTextLength(), text);
      }

    }

  }
}
