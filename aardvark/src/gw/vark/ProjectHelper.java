package gw.vark;

import gw.lang.reflect.IAnnotationInfo;
import gw.lang.reflect.IMethodInfo;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.gs.IGosuProgram;
import gw.lang.reflect.gs.IProgramInstance;
import gw.vark.annotations.Depends;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;

/**
 * Class description...
 *
 * @author bchang
 */
public class ProjectHelper {

  public static void configureProject(Project project, IGosuProgram gosuProgram) throws BuildException {
    try
    {
      IProgramInstance gosuProgramInstance = gosuProgram.getProgramInstance();
      gosuProgramInstance.evaluate( null );
      addTargets(project, gosuProgram, gosuProgramInstance);
    }
    catch( Exception e )
    {
      throw new BuildException(e);
    }
  }

  private static void addTargets( Project project, final IGosuProgram gosuProgram, final IProgramInstance gosuProgramInstance )
  {
    for ( final IMethodInfo methodInfo : gosuProgram.getTypeInfo().getMethods() )
    {
      if ( methodInfo.isPublic() && methodInfo.getParameters().length == 0 && methodInfo.getOwnersType().equals( gosuProgram ) )
      {
        String rawTargetName = stripParens(methodInfo.getName());
        String hyphenatedTargetName = camelCaseToHyphenated(rawTargetName);

        Target target = new Target() {
          @Override
          public void execute() throws BuildException {
            methodInfo.getCallHandler().handleCall( gosuProgramInstance, new Object[1] );
          }
        };
        target.setProject( project );
        target.setName( hyphenatedTargetName );
        target.setDescription( methodInfo.getDescription() );

        IAnnotationInfo dependsAnnotation = methodInfo.getAnnotation( TypeSystem.get( Depends.class ) );
        if (dependsAnnotation != null) {
          Depends dependsAnnotationValue = (Depends) dependsAnnotation.getInstance();
          String[] dependencies = dependsAnnotationValue.value();
          for ( String dependencyTarget : dependencies ) {
            target.addDependency( camelCaseToHyphenated(dependencyTarget) );
          }
        }

        project.addTarget(target);

        if (!rawTargetName.equals(hyphenatedTargetName)) {
          Target camelcaseTarget = new Target();
          camelcaseTarget.setName(rawTargetName);
          camelcaseTarget.addDependency(hyphenatedTargetName);
          project.addTarget(camelcaseTarget);
        }
      }
    }
  }

  private static String stripParens(String str) {
    if (str.endsWith("()")) {
      return str.substring(0, str.length() - 2);
    }
    else {
      throw new IllegalArgumentException("No no-arg parens in string \"" + str + "\"");
    }
  }

  private static boolean hasUpperCase(String str) {
    for (int i = 0; i < str.length(); i++) {
      if (Character.isUpperCase(str.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  static String camelCaseToHyphenated(String str) {
    if (hasUpperCase(str)) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < str.length(); i++) {
        char c = str.charAt(i);
        if (Character.isUpperCase(c)) {
          sb.append('-');
          sb.append(Character.toLowerCase(c));
        }
        else {
          sb.append(c);
        }
      }
      return sb.toString();
    }
    else {
      return str;
    }
  }
}