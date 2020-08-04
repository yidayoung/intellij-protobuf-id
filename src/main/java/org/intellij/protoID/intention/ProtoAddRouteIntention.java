package org.intellij.protoID.intention;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import idea.plugin.protoeditor.lang.PbLanguage;
import idea.plugin.protoeditor.lang.psi.PbFile;
import idea.plugin.protoeditor.lang.psi.PbMessageDefinition;
import org.intellij.protoID.IDTagUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ProtoAddRouteIntention extends BaseIntentionAction {

  private final String name = "Add route";
  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getFamilyName() {
    return name;
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getText() {
    return name;
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    if (!file.getManager().isInProject(file)) return false;
    if (!file.getLanguage().is(PbLanguage.INSTANCE)) return false;
    PbMessageDefinition definition = findDefinition(file, editor.getCaretModel().getOffset());
    if (definition == null) return false;
    List<String> routes = IDTagUtil.findRouteInComments(definition);
    return routes.isEmpty();
  }

  @Nullable
  private static PbMessageDefinition findDefinition(PsiFile file, int offset) {
    PsiElement element = file.findElementAt(offset);
    if (element == null) return null;
    return PsiTreeUtil.getParentOfType(element, PbMessageDefinition.class);
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    if (!(file instanceof PbFile)) {
      throw new IncorrectOperationException("Only applicable to proto files.");
    }
    PbMessageDefinition definition = findDefinition(file, editor.getCaretModel().getOffset());
    if (definition == null) {
      throw new IncorrectOperationException("Cursor should be placed on Pb definition start.");
    }
    PsiElement identifyingElement = definition.getIdentifyingElement();
    if (identifyingElement == null) return;
    TemplateManager templateManager = TemplateManager.getInstance(project);
    Template template = templateManager.createTemplate("", "");
    template.addTextSegment("// @route ");
    template.addVariable(new ConstantNode(""), true);
    template.addTextSegment("\n");
    int offset = definition.getTextOffset() - identifyingElement.getStartOffsetInParent();
    editor.getCaretModel().moveToOffset(offset);
    TemplateManager.getInstance(project).startTemplate(editor, template);
  }
}
