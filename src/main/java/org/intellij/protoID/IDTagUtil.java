package org.intellij.protoID;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.QualifiedName;
import com.intellij.util.containers.ContainerUtil;
import idea.plugin.protoeditor.lang.psi.PbFile;
import idea.plugin.protoeditor.lang.psi.PbMessageDefinition;
import idea.plugin.protoeditor.lang.psi.PbSymbol;
import idea.plugin.protoeditor.lang.psi.PbSymbolOwner;
import idea.plugin.protoeditor.lang.psi.util.PbCommentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IDTagUtil {
  static final String TAG_REGEX = "\\s+(\\w+)";

  public static List<Integer> getMessageID(PbMessageDefinition definition) {
    ImmutableMultimap<QualifiedName, List<Integer>> localQualifiedIDMap = getLocalQualifiedIDMap(definition.getPbFile());
    ImmutableCollection<List<Integer>> lists = localQualifiedIDMap.get(definition.getQualifiedName());
    return lists.asList().get(0);
  }

  public static Integer genNewID(PbFile file) {
    Integer moduleID = getProtoModuleID(file);
    if (moduleID == null) return null;
    ImmutableMultimap<QualifiedName, List<Integer>> localQualifiedIDMap = getLocalQualifiedIDMap(file);
    ImmutableCollection<List<Integer>> values = localQualifiedIDMap.values();
    Integer fileIDMax = null;
    for (List<Integer> ids : values) {
      if (ids.isEmpty()) continue;
      Integer cMax = Collections.max(ids);
      if ((cMax != null && isLegalID(moduleID, cMax)) && (fileIDMax == null || cMax > fileIDMax))
        fileIDMax = cMax;
    }
    return genNewID(moduleID, fileIDMax);
  }

  public static List<String> findRouteInComments(PbMessageDefinition definition) {
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

  public static TextRange IndexTagInComment(String commentText, String tag) {
    Pattern pattern = Pattern.compile(tag + TAG_REGEX);
    Matcher matcher = pattern.matcher(commentText);
    if (matcher.find()) return new TextRange(matcher.start(), matcher.end());
    return null;
  }

  public static List<QualifiedName> getIDMarkedMessages(PbFile file, int id) {
    return getLocalQualifiedIDMessageMap(file).get(id).asList().get(0);
  }

  public static boolean isLegalID(int id, PbFile file) {
    Integer protoModuleID = getProtoModuleID(file);
    if (protoModuleID == null) return true;
    return isLegalID(protoModuleID, id);
  }

  private static boolean isLegalID(int moduleID, int id){
    int min = getIDMin(moduleID);
    int max = getIDMax(moduleID);
    return id >= min && id <= max;
  }


  private static ImmutableMultimap<QualifiedName, List<Integer>> getLocalQualifiedIDMap(PbFile file) {
    return CachedValuesManager.getCachedValue(file, () ->
            CachedValueProvider.Result.create(computeLocalQualifiedIDMap(file), PsiModificationTracker.MODIFICATION_COUNT));
  }

  private static ImmutableMultimap<Integer, List<QualifiedName>> getLocalQualifiedIDMessageMap(PbFile file) {
    return CachedValuesManager.getCachedValue(file, () ->
            CachedValueProvider.Result.create(computeLocalQualifiedMessageMap(file), PsiModificationTracker.MODIFICATION_COUNT));
  }

  private static ImmutableMultimap<Integer, List<QualifiedName>> computeLocalQualifiedMessageMap(PbFile file) {
    ImmutableMultimap.Builder<Integer, List<QualifiedName>> builder = ImmutableMultimap.builder();
    ImmutableMultimap<QualifiedName, List<Integer>> localQualifiedIDMap = getLocalQualifiedIDMap(file);
    HashMap<Integer, List<QualifiedName>> result = new HashMap<>();
    localQualifiedIDMap.forEach((qualifiedName, ids) -> ids.forEach(id -> {
      List<QualifiedName> qualifiedNames = result.get(id) == null ? new ArrayList<>() : result.get(id);
      qualifiedNames.add(qualifiedName);
      result.put(id, qualifiedNames);
    }));
    result.forEach(builder::put);
    return builder.build();
  }


  private static ImmutableMultimap<QualifiedName, List<Integer>> computeLocalQualifiedIDMap(PbFile file) {
    ImmutableMultimap.Builder<QualifiedName, List<Integer>> builder = ImmutableMultimap.builder();
    addSymbolsRecursively(file, builder);
    return builder.build();
  }

  private static void addSymbolsRecursively(PbSymbolOwner parent, ImmutableMultimap.Builder<QualifiedName, List<Integer>> builder) {
    for (PbSymbol symbol : parent.getSymbols()) {
      QualifiedName symbolQualifiedName = symbol.getQualifiedName();
      if (symbolQualifiedName != null && symbol instanceof PbMessageDefinition) {
        List<Integer> idInComments = findIDInComments(((PbMessageDefinition) symbol).getComments());
        builder.put(symbolQualifiedName, idInComments);
      }
      if (symbol instanceof PbSymbolOwner) {
        addSymbolsRecursively((PbSymbolOwner) symbol, builder);
      }
    }
  }

  private static ArrayList<Integer> findIDInComments(List<PsiComment> comments) {
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

  private static Pattern getRoutePatten() {
    return Pattern.compile(PbTags.TAG_ROUTE + TAG_REGEX);
  }

  private static Integer getProtoModuleID(PbFile file) {
    String fileName = file.getVirtualFile().getNameWithoutExtension();
    Pattern pattern = Pattern.compile("(\\d+)");
    Matcher matcher = pattern.matcher(fileName);
    if (matcher.find()) return Integer.parseInt(matcher.group(1));
    PsiElement element = file.getFirstChild();
    if (!(element instanceof PsiComment)) return null;
    pattern = Pattern.compile(PbTags.TAG_MODULE_ID + TAG_REGEX);
    matcher = pattern.matcher(element.getText());
    if (matcher.find()) {
      try {
        return Integer.parseInt(matcher.group(1));
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  private static Integer genNewID(Integer moduleID, Integer maxID) {
    int min = getIDMin(moduleID);
    int max = getIDMax(moduleID);
    if (maxID == null) return min;
    int newID = maxID + 1;
    if (newID <= max && newID >= min) return newID;
    return min;
  }

  private static int getIDMax(int moduleID) {
    return moduleID * 100 + 99;
  }
  private static int getIDMin(int moduleID) {
    return moduleID * 100 + 1;
  }

  private static String findTagInComment(String commentText, Pattern pattern) {
    Matcher matcher = pattern.matcher(commentText);
    if (matcher.find()) return matcher.group(1);
    return null;
  }
}
