import org.jmlspecs.annotation.NonNullByDefault;
import org.jmlspecs.annotation.NullableByDefault;

@NonNullByDefault
class NullityDefault {
    // fine: single default nullity declaration
}

@NonNullByDefault
@NullableByDefault
class BothModifiers {
    // error: both default nullity modifiers at once
}

@NullableByDefault
class SuperClass {
}

class SubClass extends SuperClass {
    // hint: default nullity modifiers are not inherited; effective default is non_null_by_default
}

@NullableByDefault
class Outer {
    class Inner {
        // fine: inherits nullable_by_default from the enclosing class
    }

    @NonNullByDefault
    class InnerWithDefault {
        // fine: own default nullity declaration
    }
}

class Misplaced {
    @NullableByDefault
    void m() {
        // error: default nullity modifiers are only allowed on classes
    }

    @NonNullByDefault
    int f = 0;
}
