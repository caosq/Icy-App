package plugins.tlecomte.pythonExtractor;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;

/**
 * Annotation used to mark that a plugin contains Python scripts.
 * In turn Icy will extract these files from the jar to to the disk.
 * 
 * @author Timothée Lecomte
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PythonLibraries {

}
