package OOP.Solution;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(OOPParents.class)
public @interface OOPParent {
        Class<?> parent() ;
        boolean isVirtual() default false;
}

