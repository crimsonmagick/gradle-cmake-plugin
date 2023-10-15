package dev.welbyseely;

import groovy.lang.Closure;
import groovy.lang.MissingMethodException;
import javax.inject.Inject;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;

public class TargetListExtension {

  @Inject
  public TargetListExtension(Project project) {
    targetContainer = project.container(TargetExtension.class, name -> new TargetExtension(project, name));
  }

  private final NamedDomainObjectContainer<TargetExtension> targetContainer;

  public NamedDomainObjectContainer<TargetExtension> getTargetContainer() {
    return targetContainer;
  }

  public Object methodMissing(String name, Object args) {
    if (args instanceof Object[] && ((Object[]) args)[0] instanceof Closure) {
      Closure<?> closure = (Closure<?>) ((Object[]) args)[0];
      return targetContainer.create(name, closure);
    } else {
      final Object[] normalizedArgs;
      normalizedArgs = args instanceof Object[] ? (Object[]) args : new Object[]{args};
      throw new MissingMethodException(name, this.getClass(), normalizedArgs);
    }
  }
}