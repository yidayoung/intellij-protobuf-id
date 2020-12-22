package org.intellij.liveTemplates;

import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class ProtobufContext extends TemplateContextType {
  protected ProtobufContext() {
    super("PROTOBUF", "Protobuf");
  }

  @Override
  public boolean isInContext(@NotNull PsiFile file, int offset) {
    return file.getName().endsWith("proto");
  }
}
