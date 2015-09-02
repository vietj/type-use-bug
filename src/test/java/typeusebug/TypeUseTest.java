package typeusebug;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.junit.Test;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TypeUseTest {

  @Test
  public void testTypeUse() {
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    File f = new File("src/test/java/typeusebug/TheSource.java");
    assertTrue(f.exists());
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ENGLISH, Charset.defaultCharset());
    Iterable<? extends JavaFileObject> sources = fileManager.getJavaFileObjects(f);
    JavaCompiler.CompilationTask task = compiler.getTask(new StringWriter(), fileManager, diagnostics, new ArrayList<>(), new ArrayList<>(), sources);
    task.setProcessors(Collections.singletonList(new AbstractProcessor() {
      @Override
      public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
      }
      @Override
      public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element rootElt : roundEnv.getRootElements()) {
          if (rootElt.getKind() == ElementKind.CLASS) {
            TypeElement typeElt = (TypeElement) rootElt;
            ExecutableElement methodElt = (ExecutableElement) typeElt.getEnclosedElements().stream().filter(elt -> elt.getKind() == ElementKind.METHOD).findFirst().get();
            DeclaredType returnType = (DeclaredType) methodElt.getReturnType();
            List<? extends AnnotationMirror> annotationMirrors = returnType.getAnnotationMirrors();
            assertEquals(1, annotationMirrors.size());
            assertEquals(Nullable.class.getName(), ((TypeElement) annotationMirrors.get(0).getAnnotationType().asElement()).getQualifiedName().toString());
            TypeMirror typeArg = returnType.getTypeArguments().get(0);

            // Should contain Nullable but does not
            System.out.println("typeArg.getAnnotationMirrors().size() = " + typeArg.getAnnotationMirrors().size());

            // Now with compiler api
            Trees trees = Trees.instance(processingEnv);
            TreePath path = trees.getPath(methodElt);
            MethodTree leaf = (MethodTree) path.getLeaf();
            ParameterizedTypeTree returnTypeTree = (ParameterizedTypeTree) leaf.getReturnType();
            JCTree.JCAnnotatedType typeArgTree = (JCTree.JCAnnotatedType) returnTypeTree.getTypeArguments().get(0);
            Type.AnnotatedType annotatedType = (Type.AnnotatedType) typeArgTree.type;
            assertEquals(1, annotatedType.getAnnotationMirrors().size());
          }
        }
        return false;
      }
    }));
    assertEquals(Boolean.TRUE, task.call());
  }

}
