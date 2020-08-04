package org.intellij.protoID.highlighter;

import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import idea.plugin.protoeditor.lang.psi.PbVisitor;
import org.intellij.protoID.PbTags;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class PbHighlightingAnnotator  implements Annotator {
  public static final Set<String> PB_TAGS = ContainerUtil.set(PbTags.TAG_ID, PbTags.TAG_MODULE_ID, PbTags.TAG_ROUTE);
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    element.accept(new PbVisitor(){
      @Override
      public void visitComment(@NotNull PsiComment comment) {
        annotateComment(comment, holder);
      }
    });
  }

  private void annotateComment(PsiComment comment, AnnotationHolder holder) {
    String commentText = comment.getText();
    List<Pair<String, Integer>> wordsWithOffset = StringUtil.getWordsWithOffset(commentText);
    for (Pair<String, Integer> pair : wordsWithOffset) {
      Integer offset = pair.second;
      String tag = pair.first;
      if (PB_TAGS.contains(tag)) {
        TextRange range = TextRange.from(comment.getTextOffset() + offset, tag.length());
        holder.newAnnotation(HighlightInfoType.SYMBOL_TYPE_SEVERITY, "").range(range)
                .textAttributes(DefaultLanguageHighlighterColors.DOC_COMMENT_TAG)
                .create();
      }
    }
  }
}
