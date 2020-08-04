package org.intellij.protoID.inspection;

import com.intellij.codeInspection.*;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.util.ObjectUtils;
import idea.plugin.protoeditor.lang.psi.PbFile;
import idea.plugin.protoeditor.lang.psi.PbVisitor;
import org.jetbrains.annotations.NotNull;

public abstract class ProtoIDInspectionBase extends LocalInspectionTool {
  private static final PsiElementVisitor DUMMY_VISITOR = new PsiElementVisitor() { };
  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly, @NotNull LocalInspectionToolSession session) {
    PbFile pbFile = ObjectUtils.tryCast(session.getFile(), PbFile.class);
    return pbFile!=null ? buildPbVisitor(holder) : DUMMY_VISITOR;
  }

  protected abstract PbVisitor buildPbVisitor(ProblemsHolder holder);


  protected void registerProblem(@NotNull ProblemsHolder holder, @NotNull PsiElement target, @NotNull String text,
                                 LocalQuickFix... fixes) {
    registerProblem(holder, target, null,
            text, ProblemHighlightType.ERROR, fixes);
  }


  protected void registerProblem(@NotNull ProblemsHolder holder, @NotNull PsiElement target, TextRange range,
                                 @NotNull String text, ProblemHighlightType type, LocalQuickFix... fixes) {
    holder.registerProblem(holder.getManager()
            .createProblemDescriptor(target, range, text, type, false, fixes));
  }
}
