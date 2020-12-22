package org.intellij.protoID.inspection;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiEditorUtil;
import com.intellij.psi.util.QualifiedName;
import idea.plugin.protoeditor.lang.psi.PbFile;
import idea.plugin.protoeditor.lang.psi.PbMessageDefinition;
import idea.plugin.protoeditor.lang.psi.PbVisitor;
import idea.plugin.protoeditor.lang.psi.ProtoTokenTypes;
import org.intellij.protoID.IDTagUtil;
import org.intellij.protoID.PbTags;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PbUndefinedIDInspection extends ProtoIDInspectionBase {
  @Override
  protected PbVisitor buildPbVisitor(ProblemsHolder holder) {
    return new PbVisitor() {
      @Override
      public void visitMessageDefinition(@NotNull PbMessageDefinition o) {
        PsiElement identifyingElement = o.getIdentifyingElement();
        if (identifyingElement == null) return;
        List<Integer> messageIDList = IDTagUtil.getMessageID(o);
        if (messageIDList.isEmpty())
          registerProblem(holder, identifyingElement, "Message " + "'" + identifyingElement.getText() + "' need id Tag!", new PbAddIDQuickFix());
        if (messageIDList.size() == 1){
          int id = messageIDList.get(0);
          if(!IDTagUtil.isLegalID(id, o.getPbFile()))
            registerProblem(holder, identifyingElement,
                    String.format("Message %s has bad id %d", identifyingElement.getText(), id), new PbFixIDQuickFix());
          List<QualifiedName> messages = IDTagUtil.getIDMarkedMessages(o.getPbFile(), id);
          if (messages.size() > 1)
            registerProblem(holder, identifyingElement,
                    String.format("Message %s has same id %d", messages.toString(), id), new PbFixIDQuickFix());
        }
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
      Integer newID = IDTagUtil.genNewID((PbFile) file);
      if (newID == null) {
        Messages.showInfoMessage("can't gen new ID, file need @module_id tag or file name contain number", "Proto Gen ID");
        return;
      }
      Editor editor = PsiEditorUtil.findEditor(psiElement);
      if (editor != null) {
        int offset = psiElement.getTextOffset() - psiElement.getStartOffsetInParent();
        editor.getDocument().insertString(offset, String.format("// @id %d\n", newID));
      }
    }
  }

  private static class PbFixIDQuickFix implements LocalQuickFix{

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
      return "Fix id for message";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiElement psiElement = descriptor.getPsiElement();
      if (psiElement.getNode().getElementType() != ProtoTokenTypes.IDENTIFIER_LITERAL) return;
      PsiElement parent = psiElement.getParent();
      if (!(parent instanceof PbMessageDefinition)) return;
      PsiFile file = psiElement.getContainingFile();
      if (!(file instanceof PbFile)) return;
      Integer newID = IDTagUtil.genNewID((PbFile) file);
      if (newID == null) {
        Messages.showErrorDialog("can't gen new ID, file need @module_id tag or file name contain number", "Proto Gen ID");
        return;
      }
      List<PsiComment> comments = ((PbMessageDefinition) parent).getComments();
      Editor editor = PsiEditorUtil.findEditor(psiElement);
      for (PsiComment comment : comments){
        TextRange textRange = IDTagUtil.IndexTagInComment(comment.getText(), PbTags.TAG_ID);
        int textOffset = comment.getTextOffset();
        if (textRange != null){
          if (editor != null) {
            editor.getDocument().replaceString(textOffset + textRange.getStartOffset(),
                    textOffset + textRange.getEndOffset(), String.format("@id %d", newID));
          }
        }
      }
    }
  }
}
