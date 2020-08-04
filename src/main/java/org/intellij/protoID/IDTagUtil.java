package org.intellij.protoID;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.TextRangeUtil;
import idea.plugin.protoeditor.lang.psi.PbFile;
import idea.plugin.protoeditor.lang.psi.PbMessageDefinition;
import idea.plugin.protoeditor.lang.psi.util.PbCommentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IDTagUtil {
  static final String TAG_REGEX = "\\s+(\\w+)";

  public static ArrayList<Integer> findIDInComments(List<PsiComment> comments) {
    ArrayList<Integer> idTags = new ArrayList<>();
    Pattern pattern = getIDPatten();
    ContainerUtil.process(PbCommentUtil.extractText(comments), text -> {
      String idStr = findTagInComment(text, pattern);
      try {
        if (idStr != null) {
          int id = Integer.parseInt(idStr);
          idTags.add(id);
        }
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
      return true;
    });
    return idTags;
  }

  @NotNull
  private static Pattern getIDPatten() {
    return Pattern.compile(PbTags.TAG_ID + TAG_REGEX);
  }

  public static ArrayList<Integer> findIDInComments(PbMessageDefinition definition){
    return findIDInComments(definition.getComments());
  }

  public static ArrayList<Integer> findIDInPbFile(PbFile file){
    Collection<PbMessageDefinition> messageDefinitions = PsiTreeUtil.findChildrenOfType(file, PbMessageDefinition.class);
    ArrayList<Integer> results = new ArrayList<>();
    ContainerUtil.process(messageDefinitions, definition -> {
              ContainerUtil.addAllNotNull(results, findIDInComments(definition));
              return true;
            });
    return results;
  }

  public static int genNewID(PbFile file){
    return genNewID(findIDInPbFile(file), 0);
  }

  public static int genNewID(ArrayList<Integer> ids, Integer moduleID){
    Integer max = ids.isEmpty() ? moduleID: Collections.max(ids);
    return max + 1;
  }

  public static List<String> findRouteInComments(PbMessageDefinition definition){
    List<PsiComment> comments = definition.getComments();
    Pattern pattern = getRoutePatten();
    ArrayList<String> routes = new ArrayList<>();
    ContainerUtil.process(PbCommentUtil.extractText(comments), text -> {
      String route = findTagInComment(text, pattern);
      if (route != null) routes.add(route);
      return true;
    });
    return routes;
  }

  @NotNull
  private static Pattern getRoutePatten() {
    return Pattern.compile(PbTags.TAG_ROUTE + TAG_REGEX);
  }

  public static String findTagInComment(String commentText, Pattern pattern){
    Matcher matcher = pattern.matcher(commentText);
    if (matcher.find()) return matcher.group(1);
    return null;
  }

  public static TextRange IndexTagInComment(String commentText, String tag){
    Pattern pattern = Pattern.compile(tag + TAG_REGEX);
    Matcher matcher = pattern.matcher(commentText);
    if (matcher.find()) return new TextRange(matcher.start(), matcher.end());
    return null;
  }

}
