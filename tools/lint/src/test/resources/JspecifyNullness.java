import org.jspecify.annotations.NonNullApi;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;

@NullMarked
class NullMarkedClass {
    // fine: single JSpecify default nullness declaration

    class Inner {
        // fine: inherits the null-marked default from the enclosing class
    }
}

@NullMarked
@NullUnmarked
class BothMarked {
    // error: @NullMarked and @NullUnmarked conflict
}

@NonNullApi
@org.jmlspecs.annotation.NullableByDefault
class Mixed {
    // error: JSpecify and JML default nullity declarations conflict
}

@NullMarked
class JSuper {
}

class JSub extends JSuper {
    // hint: default nullity is not inherited; effective default is non_null_by_default
}

@NullMarked
class JOuter {
    @NullUnmarked
    class UnmarkedInner {
        // fine: own declaration, terminates the enclosing null-marked default
    }
}
