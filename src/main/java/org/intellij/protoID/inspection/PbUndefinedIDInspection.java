package org.intellij.protoID.inspection;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiEditorUtil;
import idea.plugin.protoeditor.lang.psi.*;
import org.intellij.protoID.IDTagUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PbUndefinedIDInspection extends ProtoIDInspectionBase {
  @Override
  protected PbVisitor buildPbVisitor(ProblemsHolder holder) {
    return new PbVisitor() {
      @Override
      public void visitMessageDefinition(@NotNull PbMessageDefinition o) {
        List<Integer> idInComments = IDTagUtil.findIDInComments(o);
        PsiElement identifyingElement = o.getIdentifyingElement();
        if (idInComments.isEmpty() && identifyingElement != null)
          registerProblem(holder, identifyingElement, "Message " + "'" + identifyingElement.getText() + "' need id Tag!", new PbAddIDQuickFix());
        o.getComments();
      }
    };
  }

  private static class PbAddIDQuickFix implements LocalQuickFix {
    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
      return "Add id for message";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiElement psiElement = descriptor.getPsiElement();
      if (psiElement.getNode().getElementType() != ProtoTokenTypes.IDENTIFIER_LITERAL) return;
      PsiElement parent = psiElement.getParent();
      if (!(parent instanceof PbMessageDefinition)) return;
      PsiFile file = psiElement.getContainingFile();
      if (!(file instanceof PbFile)) return;
      int newID = IDTagUtil.genNewID((PbFile) file);
      Editor editor = PsiEditorUtil.findEditor(psiElement);
      if (editor != null) {
        int offset = psiElement.getTextOffset() - psiElement.getStartOffsetInParent();
        editor.getDocument().insertString(offset, String.format("// @id %d\n", newID));
      }
    }
  }
}
